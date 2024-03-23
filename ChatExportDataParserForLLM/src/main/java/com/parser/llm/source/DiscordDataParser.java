package com.parser.llm.source;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.stream.JsonReader;
import com.parser.llm.chat.export.process.ChatMessage;
import com.parser.llm.source.pojo.DiscordMessage;
import lombok.extern.log4j.Log4j2;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

@Log4j2
public class DiscordDataParser implements DataParser {
    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss[.][SSS][SS][S]XXX");
    private final Set<String> allowedTypes = new HashSet<>(Arrays.asList("Default", "ChannelPinnedMessage", "Reply"));

    @Override
    public ArrayList<ChatMessage> readFileIntoChatMessage(File file) throws IOException {
        log.info("date format {}", formatter.toString());
        ArrayList<ChatMessage> messages = new ArrayList<>();
        Gson gson = new GsonBuilder().create();
        Set<String> uniqueTypes = new HashSet<>();
        try (InputStream is = new FileInputStream(file);
             InputStreamReader inputStreamReader = new InputStreamReader(is, StandardCharsets.UTF_8);
             JsonReader reader = new JsonReader(inputStreamReader)) {
            reader.beginObject();
            log.info(processJson(reader));
            log.info(processJson(reader));
            log.info(processJson(reader));
            reader.skipValue();
            reader.skipValue();
            String msgKey = reader.nextName();
            if ("messages".equalsIgnoreCase(msgKey)) {
                reader.beginArray();
                while (reader.hasNext()) {
                    DiscordMessage msg = gson.fromJson(reader, DiscordMessage.class);
                    uniqueTypes.add(msg.getType());
//                    if (allowedTypes.contains(msg.getType())) {
                        OffsetDateTime odt = OffsetDateTime.parse(msg.getTimestamp(), formatter);
                        LocalDateTime time = odt.toLocalDateTime();
                        messages.add(ChatMessage.ChatMessageBuilder.get().message(msg.getContent()).sender(msg.getAuthor().getName()).time(time).build());
//                    }
                }
            } else {
                log.info("no match {}", msgKey);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        log.info("these are the types {}", uniqueTypes.toString());
        return messages;
    }

    private String processJson(JsonReader reader) throws IOException {
        String s = reader.nextName();
        reader.beginObject();
        while (reader.hasNext()) {
            reader.skipValue();
        }
        reader.endObject();
        return s;
    }
}
