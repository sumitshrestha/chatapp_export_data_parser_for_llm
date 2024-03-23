package com.parser.llm;

import lombok.extern.log4j.Log4j2;

import java.io.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Log4j2
public class ChatProcessor {
    private static final Pattern wordPattern = Pattern.compile("[^a-z]*(?<word>[a-z]+)[^a-z]*");
    private static final List<Character> blacklistedCharacterInWord = Arrays.asList('@', '"', '&', ';', '#', '$', '%', '&', '\'', ':');

    public static void main(String[] a) {
        String path = a[0];
        File folder = new File(path);
        File[] files = folder.listFiles();
        FileWriter writer = null;
        try {
            String finalPath = folder.getAbsolutePath() + '\\' + "marged";
            new File(finalPath).mkdir();
            writer = new FileWriter(finalPath + '\\' + "all.txt");
            if (files != null) {
                for (File f : files) {
                    if (f.isFile() && f.getName().endsWith("csv")) {
                        try (BufferedReader br = new BufferedReader(new FileReader(f))) {
                            String line;
                            StringBuilder scrubbedString = new StringBuilder();
                            while ((line = br.readLine()) != null) {
                                StringTokenizer tokenizer = new StringTokenizer(line.toLowerCase());
                                while (tokenizer.hasMoreTokens()) {
                                    String token = tokenizer.nextToken();
                                    Matcher matcher = wordPattern.matcher(token);
                                    boolean flag = false;
                                    if (matcher.matches()) {
                                        token = matcher.group("word");
                                        for (Character c : blacklistedCharacterInWord) {
                                            if (token.indexOf(c) > -1) {
                                                flag = true;
                                                break;
                                            }
                                        }
                                        if (!flag && token.length() > 1) {
                                            scrubbedString.append(token).append(' ');
                                        }
                                    }
                                }
                            }
                            writer.write(scrubbedString.toString());
                            writer.write('\n');
                        }
                    }
                }
            }

        } catch (IOException e) {
            log.error(e);
        } finally {
            if (writer != null) try {
                writer.close();
            } catch (IOException e) {
                log.error(e);
            }
        }
    }
}
