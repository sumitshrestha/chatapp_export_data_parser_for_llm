package com.parser.llm;

import lombok.extern.log4j.Log4j2;

import java.io.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Log4j2
public class WordCount {
    private static final Pattern wordPattern = Pattern.compile("[^a-z]*(?<word>[a-z]+)[^a-z]*");

    public static void main(String[] a) {
        List<Character> blacklistedCharacterInWord = Arrays.asList('@', '"', '&', ';', '#', '$', '%', '&', '\'', ':');
        String path = a[0];
        File folder = new File(path);
        File[] files = folder.listFiles();
        Map<String, Integer> map = new TreeMap<>();
        if (files != null) {
            for (File f : files) {
                if (f.isFile() && f.getName().endsWith("csv")) {
                    try (BufferedReader br = new BufferedReader(new FileReader(f))) {
                        String line;
                        while ((line = br.readLine()) != null) {
                            StringTokenizer tokenizer = new StringTokenizer(line.toLowerCase());
                            while (tokenizer.hasMoreTokens()) {
                                String token = tokenizer.nextToken();
                                Matcher matcher = wordPattern.matcher(token);
                                if (matcher.matches()) {
                                    token = matcher.group("word");
                                    boolean found = false;
                                    for (Character c : blacklistedCharacterInWord) {
                                        if (token.indexOf(c) > -1) {
                                            found = true;
                                            break;
                                        }
                                    }
                                    if (!found && token.length() > 1) {
                                        if (map.containsKey(token)) {
                                            map.put(token, map.get(token) + 1);
                                        } else map.put(token, 1);
                                    }
                                }
                            }
                        }
                    } catch (IOException e) {
                        log.error(e);
                    }
                }
            }
        }
        log.info("total words {}", map.size());
        String finalPath = folder.getAbsolutePath() + '\\' + "wordcount.tsv";
        log.info("writing into {}", finalPath);
        try {
            FileWriter writer = new FileWriter(finalPath);
            for (Map.Entry<String, Integer> e : map.entrySet()) {
                if (e.getValue() > 1) {
                    writer.write(String.format("%s;%d", e.getKey(), e.getValue()));
                    writer.write('\n');
                }
            }
            writer.close();
        } catch (IOException e) {
            log.error(e);
        }
    }
}
