package com.parser.llm.source;

import com.parser.llm.chat.export.process.ChatMessage;
import lombok.extern.log4j.Log4j2;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Log4j2
public class TelegramDataParser implements DataParser {

    @Override
    public ArrayList<ChatMessage> readFileIntoChatMessage(File file) throws IOException {
        List<String> fileLines = Files.readAllLines(file.toPath());
        StringBuilder builder = new StringBuilder();
        for (String line : fileLines)
            builder.append(line);
        JSONObject telegram = new JSONObject(builder.toString());
        ArrayList<ChatMessage> messages = new ArrayList<>();
        if (!telegram.isEmpty() && telegram.has("messages")) {
            JSONArray array = telegram.getJSONArray("messages");
            for (Object nxt : array) {
                if (nxt instanceof JSONObject m) {
                    Object textObj = m.get("text");
                    String text;
                    if (textObj instanceof JSONArray) {
                        StringBuilder b = new StringBuilder();
                        for (Object txt : (JSONArray) textObj) {
                            b.append(txt);
                        }
                        text = b.toString();
                    } else text = (String) textObj;
                    String from = "";
                    if (m.has("from")) {
                        from = m.getString("from");
                        LocalDateTime date = LocalDateTime.parse(m.getString("date"));
                        ChatMessage msg = ChatMessage.ChatMessageBuilder.get()
                                .sender(from).message(text)
                                .time(date).build();
                        messages.add(msg);
                    }
                }
            }
        }

        return messages;
    }
}
