package org.example;

import com.robrua.nlp.bert.Bert;
import edu.stanford.nlp.util.Triple;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.similarities.LMDirichletSimilarity;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.*;

/**
 * Get answers for all the Jeopardy questions and measure the performance
 */
public class JeopardyQuery {

    private final String jeopardyQuestionsFilePath;
    private final String indexPath;
    private final TextProcessingOption textProcessingOption;
    private final Map<String, Triple<Integer, Integer, Double>> categoriesAnswers = new HashMap<>();

    public JeopardyQuery(String jeopardyQuestionsFilePath, TextProcessingOption textProcessingOption) {
        this.jeopardyQuestionsFilePath = jeopardyQuestionsFilePath;
        this.textProcessingOption = textProcessingOption;
        this.indexPath = Utils.getIndexPathBasedOnTextProcessingOption(textProcessingOption);
    }

    // Parse the Jeopardy questions and run the queries
    public void startQuery(boolean useBert) {
        try (BufferedReader reader = new BufferedReader(new FileReader(jeopardyQuestionsFilePath))) {

            String category = "";
            String clue = "";
            String answer = "";

            int lineNumber = 0;
            String line;

            int correctAnswers = 0;

            Bert bert = null;
            if (useBert) {
                bert = Bert.load("com/robrua/nlp/easy-bert/bert-uncased-L-12-H-768-A-12");
            }

            // Parse the questions
            while ((line = reader.readLine()) != null) {
                switch (lineNumber % 4) {
                    case 0 -> category = line.trim();
                    case 1 -> clue = line.trim();
                    case 2 -> answer = line.trim();
                    case 3 -> {
                        // Create the query
                        String query = Utils.applyTextProcessing(category + " " + clue, textProcessingOption);
                        JeopardyQueryResult result = runQuery(query, bert);

                        boolean isFirstAnswerCorrect = false;

                        if (result.getTitle().equalsIgnoreCase(answer)) {
                            isFirstAnswerCorrect = true;
                            correctAnswers += 1;
                        }

                        // Update the categories
                        addToCategoriesAnswers(category, isFirstAnswerCorrect);

                        System.out.println();
                        System.out.println("Category: " + category);
                        System.out.println("Question: " + clue);
                        System.out.println("Expected answer: " + answer);
                        System.out.println("Actual answer: " + result.getTitle());
                        System.out.println("Is correct: " + isFirstAnswerCorrect);
                    }
                }
                lineNumber += 1;
            }

            double precisionAt1 = correctAnswers / 100.0;
            System.out.println("\nFor index: " + indexPath);
            System.out.println("Correct answers: " + correctAnswers + " /100");
            System.out.println("P@1: " + precisionAt1);
            System.out.println();

            // Print categories information
            categoriesAnswers.entrySet().stream()
                    .sorted(Comparator.comparingDouble(entry -> {
                        Triple<Integer, Integer, Double> triple = entry.getValue();
                        double total = triple.first + triple.second;
                        double precision = (triple.first * 100.0) / total;
                        triple.third = precision;
                        return -precision;
                    })).forEach(entry -> {
                        String key = entry.getKey();
                        Triple<Integer, Integer, Double> triple = entry.getValue();
                        System.out.println(key + ": Correct: " + triple.first + ", Incorrect: " + triple.second + ", Precision: " + triple.third);
                    });
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    // Search using the IndexSearcher
    private JeopardyQueryResult runQuery(String query, Bert bert) {
        List<JeopardyQueryResult> queryResults = new ArrayList<>();

        try {
            StandardAnalyzer standardAnalyzer = new StandardAnalyzer();
            Query currentQuery = new QueryParser("content", standardAnalyzer).parse(QueryParser.escape(query));

            Directory index = FSDirectory.open(new File(indexPath).toPath());
            IndexReader indexReader = DirectoryReader.open(index);
            IndexSearcher indexSearcher = new IndexSearcher(indexReader);
            indexSearcher.setSimilarity(new LMDirichletSimilarity());

            TopDocs topDocs = indexSearcher.search(currentQuery, 2);
            ScoreDoc[] searchResults = topDocs.scoreDocs;
            for (ScoreDoc searchResult : searchResults) {
                int docId = searchResult.doc;
                Document document = indexSearcher.doc(docId);
                queryResults.add(new JeopardyQueryResult(document, searchResult.score));
            }

            indexReader.close();

        } catch (Exception e) {
            e.printStackTrace();
        }

        return reRankDocuments(queryResults, query, bert);
    }

    // Update the categories map with the answers
    private void addToCategoriesAnswers(String category, boolean correctAnswer) {
        categoriesAnswers.compute(category, (key, existingPair) -> {
            if (existingPair == null) {
                if (correctAnswer) {
                    return new Triple<>(1, 0, 0.0);
                } else {
                    return new Triple<>(0, 1, 0.0);
                }
            } else {
                if (correctAnswer) {
                    return new Triple<>(existingPair.first + 1, existingPair.second, 0.0);
                } else {
                    return new Triple<>(existingPair.first, existingPair.second + 1, 0.0);
                }
            }
        });
    }

    // Chose between the results when the score is lower than 1, using bert
    private JeopardyQueryResult reRankDocuments(List<JeopardyQueryResult> results, String query, Bert bert) {
        JeopardyQueryResult firstResult = results.get(0);
        if (bert == null) {
            return firstResult;
        }

        JeopardyQueryResult secondResult = results.get(0);

        double scoreDifference = firstResult.documentScore() - secondResult.documentScore();
        if (scoreDifference < 1) {
            float[] queryEmbedding = bert.embedSequence(query);
            float[] firstResultContentEmbedding = bert.embedSequence(firstResult.getContent());
            float[] secondResultContentEmbedding = bert.embedSequence(secondResult.getContent());

            if (cosineSimilarity(queryEmbedding, firstResultContentEmbedding) > cosineSimilarity(queryEmbedding, secondResultContentEmbedding)) {
                return firstResult;
            }
            return secondResult;
        }

        return firstResult;
    }

    // Calculate the cosine similarity of two vectors
    private static double cosineSimilarity(float[] vector1, float[] vector2) {
        if (vector1.length != vector2.length) {
            throw new IllegalArgumentException("Vector dimensions must be the same");
        }

        double dotProduct = 0.0;
        double magnitude1 = 0.0;
        double magnitude2 = 0.0;

        for (int i = 0; i < vector1.length; i++) {
            dotProduct += vector1[i] * vector2[i];
            magnitude1 += Math.pow(vector1[i], 2);
            magnitude2 += Math.pow(vector2[i], 2);
        }

        if (magnitude1 == 0.0 || magnitude2 == 0.0) {
            return 0.0; // Avoid division by zero
        }

        return dotProduct / (Math.sqrt(magnitude1) * Math.sqrt(magnitude2));
    }
}
