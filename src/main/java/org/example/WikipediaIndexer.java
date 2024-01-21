package org.example;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.FSDirectory;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Create an index for the Wikipedia documents
 */
public class WikipediaIndexer {

    private static final String CATEGORIES_LINE_START = "CATEGORIES:";

    private final String wikipediaDatasetDirectoryPath;
    private final String indexPath;
    private final TextProcessingOption textProcessingOption;


    public WikipediaIndexer(String wikipediaDatasetDirectoryPath, String indexPath, TextProcessingOption textProcessingOption) {
        this.wikipediaDatasetDirectoryPath = wikipediaDatasetDirectoryPath;
        this.indexPath = indexPath;
        this.textProcessingOption = textProcessingOption;
    }

    /**
     * Build the index for the Wikipedia documents
     */
    public void buildIndex() {
        try (IndexWriter indexWriter = new IndexWriter(
                FSDirectory.open(new File(indexPath).toPath()),
                new IndexWriterConfig(new StandardAnalyzer())
            )
        ) {
            List<File> wikipediaFiles = getWikipediaFiles(wikipediaDatasetDirectoryPath);
            for (File wikipediaFile : wikipediaFiles) {
                System.out.println("Indexing document: " + wikipediaFile.getName());
                parseWikipediaPage(wikipediaFile, indexWriter);
            }

            indexWriter.commit();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

    /**
     * Parse a Wikipedia page based on its structure
     */
    private void parseWikipediaPage(File wikipediaFile, IndexWriter indexWriter) {
        String documentTitle = "";
        String documentCategories = "";
        List<String> documentContent = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(new FileReader(wikipediaFile))) {
            String line;
            while ((line = reader.readLine()) != null) {

                if (isTitleLine(line)) {

                    if (!"".equals(documentTitle)) {
                        addDocumentToIndex(indexWriter, documentTitle, documentCategories.trim(), concatenateDocumentContent(documentContent));
                    }

                    documentTitle = line.substring(2, line.length() - 2);
                    documentContent = new ArrayList<>();

                } else if (isCategoriesLine(line)) {
                    documentCategories = line.substring(CATEGORIES_LINE_START.length() + 1);
                } else if (isHeaderLine(line)) {
                    line = line.replace("=", "");
                    documentContent.add(line);
                } else {
                    documentContent.add(line);
                }
            }

            addDocumentToIndex(indexWriter, documentTitle, documentCategories.trim(), concatenateDocumentContent(documentContent));


        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Create new Wikipedia document
     */
    private void addDocumentToIndex(IndexWriter indexWriter, String docTitle, String docCategories, String docContent) throws IOException {
        Document document = new Document();
        document.add(new StringField("title", docTitle, Field.Store.YES));
        document.add(new TextField("categories", docCategories, Field.Store.YES));

        String all = docTitle + " " + String.join(" ", docCategories) + " " + docContent;
        document.add(new TextField("content", Utils.applyTextProcessing(all, textProcessingOption), Field.Store.YES));

        indexWriter.addDocument(document);
    }

    private List<File> getWikipediaFiles(String wikipediaDatasetDirectoryPath) {
        List<File> wikipediaFiles = new ArrayList<>();
        File wikipediaDirectory = new File(wikipediaDatasetDirectoryPath);

        if (wikipediaDirectory.isDirectory()) {
            File[] files = wikipediaDirectory.listFiles();
            if (files == null) {
                return wikipediaFiles;
            }

            for (File file : files) {
                if (file.isFile()) {
                    wikipediaFiles.add(file);
                }
            }
        }

        return wikipediaFiles;
    }

    /**
     * Check if the line is a title line
     */
    private boolean isTitleLine(String line) {
        return line.startsWith("[[") && line.endsWith("]]") && line.length() > 4;
    }

    /**
     * Check if the line is a category line
     */
    private boolean isCategoriesLine(String line) {
        return line.startsWith(CATEGORIES_LINE_START);
    }

    /**
     * Check if the line is a header line
     */
    private boolean isHeaderLine(String line) {
        return line.startsWith("=") && line.endsWith("=") && line.length() > 2;
    }

    private String concatenateDocumentContent(List<String> content) {
        return String.join(" ", content);
    }
}
