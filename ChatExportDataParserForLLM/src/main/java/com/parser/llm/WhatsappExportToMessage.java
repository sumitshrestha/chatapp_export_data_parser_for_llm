package com.parser.llm;

import com.parser.llm.chat.export.process.ChatMessage;
import lombok.extern.log4j.Log4j2;
import com.parser.llm.source.WhatsappDataParser;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

@Log4j2
public class WhatsappExportToMessage {
    public static void main(String[] a) {
        WhatsappDataParser parser = new WhatsappDataParser();
        String path = a[0];
        String skipUser = a[1];
        File folder = new File(path);
        File[] files = folder.listFiles();
        Set<String> utterances = new HashSet<>();
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
                                if (!msg.getSender().equalsIgnoreCase(skipUser) && !utterances.contains(message)) {
                                    writer.write(message + '\n');
                                    utterances.add(message);
                                }
                            }
                            writer.close();
                        }
                    } catch (IOException e) {
                        log.error(e);
                    }
                }
            }
        }
    }
}
