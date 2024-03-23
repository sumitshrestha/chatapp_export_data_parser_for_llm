package com.parser.llm;

import com.parser.llm.chat.export.process.ChatMessage;
import lombok.extern.log4j.Log4j2;
import com.parser.llm.source.DiscordDataParser;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

@Log4j2
public class DiscordExportToMessage {
    public static void main(String[] a) {
        long startTime = System.nanoTime();
        DiscordDataParser parser = new DiscordDataParser();
        String path = a[0];

        File folder = new File(path);
        File[] files = folder.listFiles();
        if (files != null) {
            for (File f : files) {
                if (f.isFile() && f.getName().endsWith("txt")) {
                    try {
                        ArrayList<ChatMessage> messages = parser.readFileIntoChatMessage(f);
                        String filePath = f.getAbsolutePath();
                        log.info("this is file name {}", filePath);
                        int indexOf = filePath.lastIndexOf('.');
                        if (indexOf > 0) { // there is file extension
                            String finalPath = filePath.substring(0, indexOf) + "_msg.csv";
                            log.info("writing into {}", finalPath);
                            FileWriter writer = new FileWriter(finalPath);
                            for (ChatMessage msg : messages) {
                                String message = msg.getMessage();
                                writer.write(message);
//                                System.out.println(message);
                            }
                            writer.close();
                        }
                    } catch (IOException e) {
                        log.error(e);
                    }
                }
            }
        }

        long durationInNano = (System.nanoTime() - startTime);
        double durationInSeconds = durationInNano / 1_000_000_000.0;
        log.info("total time {} seconds", durationInSeconds);
    }
}
