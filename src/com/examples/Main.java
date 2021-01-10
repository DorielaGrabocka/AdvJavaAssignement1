/*
 * Copyright (c) 17/12/2020 . Author @Doriela Grabocka
 */

package com.examples;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class Main {

    private static Map<String, Double> resemblanceResults = new HashMap<>();
    private static Map<String, String> languagePrefixNames = new HashMap<>();
    private static List<LanguageModel> languageModelsList = new ArrayList<>();

    public static void main(String[] args) throws InterruptedException{
        int nGram=0;
        String fileName="";
        if(args.length==2) {
            try {
                nGram = Integer.parseInt(args[1]);
                if(nGram<1) throw new Exception();//throw exception if n-gram is 0 or less
                fileName = "com/examples/folder/".concat(args[0]);
            } catch (Exception e) {
                System.out.println("Wrong number of inputs!The first should be the filename " +
                        "and the second the nGram size(greater than 0).");
            }
        }
        else if(args.length==1){
            nGram=2;
            fileName = "com/examples/folder/".concat(args[0]);
        }
        else{
            System.out.println("Wrong input!");
            System.exit(-1);
        }

        languagePrefixNames.put("al", "Albanian");
        languagePrefixNames.put("de", "German");
        languagePrefixNames.put("en", "English");
        languagePrefixNames.put("fr", "French");
        languagePrefixNames.put("gr", "Greek");
        languagePrefixNames.put("it", "Italian");
        startProcessingFolders(nGram);
        computeResemblance(languageModelsList, fileName, nGram);

    }

    /**Method to start processing the folders. It will initialize a thread per folder to process them concurrently
     * and will call method startProcessingFiles to concurrently process even the files in the folder.
     * @param nGramSize - this is the maximum  number of the token.*/
    private static void startProcessingFolders(int nGramSize){
        File directory = new File("com/examples/folder");
        Arrays.stream(directory.listFiles())//.parallel()
                    .filter(File::isDirectory)//filter only the directories
                    .forEach(dir->{
                        ExecutorService executorService = Executors.newCachedThreadPool();
                        executorService.execute(()->startProcessingFiles(dir, nGramSize));
                        executorService.shutdown();
                        try{
                            executorService.awaitTermination(1, TimeUnit.MINUTES);
                        }
                        catch(InterruptedException e){
                            System.out.println("External Thread was interrupted!");
                        }
                    });
    }

    /**This method is used to initialize the threads for each file, inside the thread that is processing the
     * folder. It will create the language models, as well.
     * @param dir - this is the directory that is being processed.
     * @param nGramSize - this is the maximum size of a token in the model.*/
    private static void startProcessingFiles(File dir, int nGramSize) {
        ExecutorService executorService = Executors.newCachedThreadPool();
        String modelName = getModelName(dir.getName());
        LanguageModel languageModel = new LanguageModel(modelName, nGramSize);//language model of folder
        languageModelsList.add(languageModel);
        Predicate<File> isTextFile = f-> f.isFile() && f.getName().endsWith(".txt");//predicate to filter .txt files
        Arrays.stream(dir.listFiles())//.parallel()//get all files/folders in each directory
                .filter(isTextFile)//filter only the text files
                .forEach(file->executorService.execute(//a new thread reads each file
                        new Reader(languageModel, "com/examples/folder/".concat(dir.getName()).concat("/")
                                                                        .concat(file.getName())//filepath
                        )));
        executorService.shutdown();
        try{
            executorService.awaitTermination(1, TimeUnit.MINUTES);
        }
        catch(InterruptedException e){
            System.out.println("Thread was interrupted!");
        }
    }

    /**Method to give a name to the languageModel being created in startProcessingFileModel.
     * @param folderName is the name of the folder that is being processed by the thread.
     * @return the name of the file taken from the global map languagePrefixNames. */
    private static String getModelName(String folderName) {
        return languagePrefixNames.containsKey(folderName)?languagePrefixNames.get(folderName):"Other ".concat(folderName.toUpperCase());
    }//end of getModelName

    /**Method to calculate the resemblance of a file to each of the models.
     * The resemblance is computed concurrently. Each thread computes the resemblance to a model.
     * @param nGramSize - the maximum size of the token
     * @param fileName - is the file whose language we want to find
     * @param allModels - is the list of all models created.
     * */
    private static void computeResemblance(List<LanguageModel> allModels, String fileName, int nGramSize){
        Map<String, Integer> histogramOfFile = new HashMap<>();
        ExecutorService executorService = Executors.newCachedThreadPool();
        try{
            LanguageModel.createHistogram(Reader.getTextLines(fileName), histogramOfFile, nGramSize);
            allModels.forEach(model->
                    executorService.execute(()->resemblanceResults.put(model.getLanguage(),
                                                                       model.computeModelResemblance(histogramOfFile))));
            executorService.shutdown();
            executorService.awaitTermination(1, TimeUnit.MINUTES);

            showResults();
        }catch (IOException e){
            System.out.println("File not found!");
        }
        catch (InterruptedException e){
            System.out.println("Thread interrupted!");
        }

    }//end of computeResemblance

    /**Method to print the results of the program.*/
    private static void showResults() {
        List<Map.Entry<String, Double>> sortedResults = resemblanceResults.entrySet().stream()
                .sorted((o1, o2) -> {
                    if(o1.getValue()>o2.getValue())return -1;
                    if(o1.getValue()<o2.getValue()) return 1;
                    return 0;
                })
                .collect(Collectors.toList());

        sortedResults.stream()
                .forEach(entry->System.out.printf("Model:%-15sCosine:%-10.4fAngle:%-10.3f \n", entry.getKey(),
                        entry.getValue(),
                        Math.acos(entry.getValue())*180/Math.PI));

        String bestModel = sortedResults.get(0).getKey();
        double angle = Math.acos(sortedResults.get(0).getValue())*180/Math.PI;
        if(angle==90.0){
            System.out.println("There is no suitable model for this file!");
        }
        else{
            System.out.printf("The best model is %s with an angle of %.3f degrees.", bestModel, angle);
        }
    }//end of showResults
}
