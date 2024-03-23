package com.parser.llm.source;

import com.parser.llm.chat.export.process.ChatMessage;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class WhatsappDataParser implements DataParser {
    private static final Pattern iphoneMessagePattern = Pattern.compile("\\[(?<month>\\d{1,2})/(?<day>\\d{1,2})/(?<year>\\d{1,2}), (?<hour>\\d{1,2}):(?<minute>\\d{1,2}):(?<second>\\d{1,2}) (?<daynight>[AP]M)\\] (?<personName>[\\w ]+): (?<message>.*)");
    private static final Pattern androidMessagePattern = Pattern.compile("(?<month>\\d{2})/(?<day>\\d{2})/(?<year>\\d{2}), (?<hour>\\d{1,2}):(?<minute>\\d{1,2}) (?<daynight>[AP]M) - (?<personName>[\\w ]+): (?<message>.*)");
    private static final char wierdCharacter = ' ';
    private final String[] blacklistedMessages = {
            "Messages and calls are end-to-end encrypted. No one outside of this chat, not even WhatsApp, can read or listen to them.".toLowerCase(),
            "<Media omitted>".toLowerCase(),
            "image omitted".toLowerCase(),
            "video omitted".toLowerCase(),
            "You deleted this message.".toLowerCase()
    };

    public ArrayList<ChatMessage> readFileIntoChatMessage(File file) throws IOException {
        FileReader fileReader = new FileReader(file);
        // Read the WhatsApp chat export file
        BufferedReader bufferedReader = new BufferedReader(fileReader);

        // Initialize a list to store the chat messages
        ArrayList<ChatMessage> chatMessages = new ArrayList<>();

        // Iterate over the chat messages
        String line;
        while ((line = bufferedReader.readLine()) != null) {
            line = line.replace(WhatsappDataParser.wierdCharacter, ' ');
            boolean isLineToBeSkipped = false;
            for (String b : blacklistedMessages) {
                if (line.toLowerCase().endsWith(b)) {
                    isLineToBeSkipped = true;
                    break;
                }
            }
            if (isLineToBeSkipped) continue;

            Matcher matcher = WhatsappDataParser.iphoneMessagePattern.matcher(line);
            if (matcher.matches()) {
                ChatMessage chatMessage = ChatMessage.ChatMessageBuilder.get()
                        .sender(matcher.group("personName")).message(matcher.group("message"))
                        .month(matcher.group("month")).day(matcher.group("day")).year(matcher.group("year"))
                        .hour(matcher.group("hour")).minute(matcher.group("minute")).second(matcher.group("second")).isDay(matcher.group("daynight"))
                        .build();
                // Add the ChatMessage object to the list
                chatMessages.add(chatMessage);
            } else {
                matcher = WhatsappDataParser.androidMessagePattern.matcher(line);
                if (matcher.matches()) {
                    ChatMessage chatMessage = ChatMessage.ChatMessageBuilder.get()
                            .sender(matcher.group("personName")).message(matcher.group("message"))
                            .month(matcher.group("month")).day(matcher.group("day")).year(matcher.group("year"))
                            .hour(matcher.group("hour")).minute(matcher.group("minute")).isDay(matcher.group("daynight"))
                            .build();
                    // Add the ChatMessage object to the list
                    chatMessages.add(chatMessage);
                } else if (!chatMessages.isEmpty()) { // regular text that continues
                    ChatMessage message = chatMessages.get(chatMessages.size() - 1);
                    message.appendMessage(line);
                }
            }
        }
        // Close the file reader
        bufferedReader.close();
        return chatMessages;
    }
}
