package org.example;

import edu.stanford.nlp.simple.Sentence;
import org.apache.lucene.analysis.CharArraySet;
import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.tartarus.snowball.ext.PorterStemmer;

import java.util.List;
import java.util.stream.Collectors;

public final class Utils {
    private static final CharArraySet stopWords = EnglishAnalyzer.getDefaultStopSet();

    private static final String INDEX_PATH_NONE = "src\\main\\resources\\Index\\IndexNONE";
    private static final String INDEX_PATH_STOP_WORDS = "src\\main\\resources\\Index\\IndexSTOPWORDS";
    private static final String INDEX_PATH_STEMMING = "src\\main\\resources\\Index\\IndexSTEMMING";
    private static final String INDEX_PATH_STOP_WORDS_STEMMING = "src\\main\\resources\\Index\\IndexSTOPWORDSSTEMMING";
    private static final String INDEX_PATH_LEMMATIZATION = "src\\main\\resources\\Index\\IndexLEMMATIZATION";

    public static final String INDEX_PATH = "src\\main\\resources\\Index";
    public static final String WIKIPEDIA_DATASET_DIRECTORY_PATH = "src\\main\\resources\\WikiPages";
    public static final String JEOPARDY_QUESTIONS_PATH = "src\\main\\resources\\Jeopardy\\questions.txt";

    public static String applyTextProcessing(String inputText, TextProcessingOption textProcessingOption) {
        switch (textProcessingOption) {
            case NONE -> {
                return inputText;
            }
            case STOP_WORDS -> {
                return Utils.removeStopWords(inputText);
            }
            case STEMMING -> {
                return Utils.stemText(inputText);
            }
            case LEMMATIZATION -> {
                return Utils.lemmatizeText(inputText);
            }
            case STOP_WORDS_STEMMING -> {
                return Utils.removeStopWordsAndStemText(inputText);
            }
        }
        return inputText;
    }

    public static String removeStopWords(String inputText) {
        return String.join(" ", removeStopWords(new Sentence(inputText).words()));
    }

    public static String stemText(String inputText) {
        PorterStemmer stemmer = new PorterStemmer();
        return String.join(" ", stemWords(stemmer, new Sentence(inputText).words()));
    }

    public static String lemmatizeText(String inputText) {
        return String.join(" ", new Sentence(inputText).lemmas());
    }

    public static String removeStopWordsAndStemText(String inputText) {
        PorterStemmer stemmer = new PorterStemmer();
        return String.join(" ", stemWords(stemmer, removeStopWords(new Sentence(inputText).words())));
    }

    public static List<String> removeStopWords(List<String> words) {
        return words.stream()
                .map(String::toLowerCase)
                .filter(word -> !stopWords.contains(word))
                .collect(Collectors.toList());
    }

    public static List<String> stemWords(PorterStemmer stemmer, List<String> words) {
        return words.stream()
                .map(word -> stemWord(stemmer, word))
                .collect(Collectors.toList());
    }

    public static String stemWord(PorterStemmer stemmer, String word) {
        stemmer.setCurrent(word);
        stemmer.stem();
        return stemmer.getCurrent();
    }

    public static String getIndexPathBasedOnTextProcessingOption(TextProcessingOption textProcessingOption) {
        switch (textProcessingOption) {
            case NONE -> {
                return INDEX_PATH_NONE;
            }
            case STOP_WORDS -> {
                return INDEX_PATH_STOP_WORDS;
            }
            case STEMMING -> {
                return INDEX_PATH_STEMMING;
            }
            case LEMMATIZATION -> {
                return INDEX_PATH_LEMMATIZATION;
            }
            case STOP_WORDS_STEMMING -> {
                return INDEX_PATH_STOP_WORDS_STEMMING;
            }
        }
        return "";
    }
}
