package org.example;


import java.io.File;

public class Main {

    public static void main(String[] args) {

        boolean useBert = false;
        boolean all = false;

        for (String arg : args) {
            if ("bert".equalsIgnoreCase(arg)) {
                useBert = true;
            }
            if ("all".equalsIgnoreCase(arg)) {
                all = true;
            }

        }

        if (all) {
            buildIndex(TextProcessingOption.NONE);
            buildIndex(TextProcessingOption.STOP_WORDS);
            buildIndex(TextProcessingOption.STEMMING);
            buildIndex(TextProcessingOption.STOP_WORDS_STEMMING);
            startQuery(TextProcessingOption.NONE, useBert);
            startQuery(TextProcessingOption.STOP_WORDS, useBert);
            startQuery(TextProcessingOption.STEMMING, useBert);
            startQuery(TextProcessingOption.STOP_WORDS_STEMMING, useBert);
        } else {
            buildIndex(TextProcessingOption.NONE);
            startQuery(TextProcessingOption.NONE, false);
        }
    }

    /**
     * Run the Jeopardy questions for the specified index
     */
    private static void startQuery(TextProcessingOption textProcessingOption, boolean useBert) {
        try {
            JeopardyQuery jeopardyQuery = new JeopardyQuery(Utils.JEOPARDY_QUESTIONS_PATH, textProcessingOption);
            jeopardyQuery.startQuery(useBert);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Build the index with the specified text processing option
     */
    private static void buildIndex(TextProcessingOption textProcessingOption) {
        String indexPath = Utils.getIndexPathBasedOnTextProcessingOption(textProcessingOption);
        System.out.println("Start build index for: " + indexPath);
        if (checkIfIndexIsAlreadyBuild(indexPath)) {
            System.out.println("Index is already built!");
            return;
        }

        WikipediaIndexer wikipediaIndexer = new WikipediaIndexer(
                Utils.WIKIPEDIA_DATASET_DIRECTORY_PATH,
                indexPath,
                textProcessingOption
        );

        wikipediaIndexer.buildIndex();
        System.out.println("End build index for: " + indexPath);
    }

    /**
     * Check if the folder where the index should be build is empty or not
     */
    private static boolean checkIfIndexIsAlreadyBuild(String indexPath) {
        File directory = new File(indexPath);
        if (directory.isDirectory()) {
            String[] files = directory.list();
            return files != null && files.length > 0;
        }
        return false;
    }
}