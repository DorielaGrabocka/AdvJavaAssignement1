/*
 * Copyright (c) 16/12/2020 . Author @Doriela Grabocka
 */

package com.examples;


import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class LanguageModel {
    private final String language;
    private final int nGramSize;
    private Map<String, Integer> languageHistogram = new HashMap<>();//this is the shared location among thraeds

    public LanguageModel(String language, int nGramSize) {
        this.language = language;
        if(nGramSize<1)
            this.nGramSize=2;
        else
            this.nGramSize=nGramSize;
    }

    /**Static method to create the histogram of the language model and any file
     * @param linesOfText - are the lines of text that will correspond to the histogram
     * @param histogram - this is a HashMap data structure that will correspond to the
     *                    histogram
     * @param nGramSize - is the maximum substring length that will correspond to the n of n-gram
     * */
    protected static void createHistogram(List<String> linesOfText, Map<String, Integer> histogram, int nGramSize){
       List<String> lines = clearText(linesOfText);//clearText
       //tokenize
       List<String> tokens = lines.stream()
                .flatMap(line-> Arrays.asList(line.split(" ")).stream())
                .flatMap(word->getTokens(word, nGramSize).stream())
                .collect(Collectors.toList());
       //fill the histogram
       synchronized (histogram){
           tokens.stream()
                 .forEach(token->{
                     if (histogram.containsKey(token)) {//token already in dictionary
                        histogram.put(token, histogram.get(token)+1);
                     }
                     else{//token not in dictionary
                         histogram.put(token,1);
                     }
                     });//this part is synchronized
       }
    }

    /**Method to compute the resemblance of a file to the current language model.
     * It return the cosine of the angle of the two vectors, the language histogram
     * and the file histogram.
     * @param mysteryHistogram - is the text of the file to be classified
     * @return is the cosine of the angle of the histograms in an n-dimensional space
     * */
    protected double computeModelResemblance(Map<String, Integer> mysteryHistogram){
        HashMap<String, Integer> numerator = new HashMap<>();
        //filter  only  the common values of the two histograms
        numerator.putAll(mysteryHistogram);
        //here we compute the first step: sum a_i *b_i
        int value = numerator.entrySet().stream()
                .map(entry-> entry.getValue()*languageHistogram.getOrDefault(entry.getKey(),0))
                .reduce(0,(l,m)->l+m);
        //here we find sum a_i^2
        int sum1 = mysteryHistogram.values().stream()
                .reduce( 0, (a, b)->a+b*b);
        //here we find sum b_i^2
        int sum2 = languageHistogram.values().stream()
                .reduce(0, (a,b)->a+b*b);
        //here we apply the cosine similarity formula: sum a_i*b_i/sqrt(sum a_i^2)*sqrt(sum b_i^2)
        double cosineOfAngle = Math.round((value/(Math.sqrt(sum1)*Math.sqrt(sum2)))*10000.0)/10000.0;//for rounding to 4 dec. places

        return cosineOfAngle;
    }


    /**Method to return the dictionary of this language model
     * @return the Map<String, Integer> containing the histogram of the model*/
    public Map<String, Integer> getLanguageHistogram() {
        return languageHistogram;
    }

    /**Method to return the n-gram size of this language model
     * @return the int containing the histogram of the model*/
    public int getnGramSize() { return nGramSize; }

    /**Method to return the language of this model
     * @return a string containing the language of this model*/
    public String getLanguage() {
        return language;
    }

    /**Method to clear the text that it is supplied. It converts everything to
     * lowercase and removes all non-alpha-numeric characters
     * @param lines- a List of string that will undergo processing
     * @return List<String> the same list of lines but cleared
     * */
    private static List<String> clearText(List<String> lines){
        return lines.stream()
                .map(String::toLowerCase)
                .map(line->line.replaceAll("[!\"#$%&'()*+,-./:;<=>?@\\[\\]^_`\\{|}~0-9]",""))
                .collect(Collectors.toList());
    }

    /**Method used to return all tokens from the word supplied as string
     * @param string - the word to be tokenized
     * @param nGramSize - the maximum size of a token
     * @return List<String> with all tokens from a word
     * */
    private static List<String> getTokens(String string, int nGramSize){
        List<String> allTokens = new ArrayList<>();
        //here we check if the string has more letters than n of the n-gram
        if(string.length()>=nGramSize){
            allTokens = IntStream.range(0,string.length()-nGramSize+1)
                    .mapToObj(number->string.substring(number, number+nGramSize))
                    .collect(Collectors.toList());
        }
        else{//this means that the string is less than ngram size so we just add it
            allTokens.add(string);
        }
        return allTokens;
    }
}
