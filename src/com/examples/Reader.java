/*
 * Copyright (c) 17/12/2020 . Author @Doriela Grabocka
 */

package com.examples;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;

public class Reader implements Runnable{
    private LanguageModel languageModel;//this is the shared object among the threads
    private String fileName;
    public Reader(LanguageModel languageModel, String fileName) {
        this.languageModel = languageModel;
        this.fileName=fileName;
    }

    @Override
    public void run() {
        try{
            LanguageModel.createHistogram(getTextLines(fileName), languageModel.getLanguageHistogram(), languageModel.getnGramSize());
        }catch (IOException e){
            System.err.println("An error has occurred! Possibly the file does not exist!");
        }
    }

     public static List<String> getTextLines(String fileName) throws IOException {
        return Files.lines(Paths.get(fileName))
                .collect(Collectors.toList());
    }
}
