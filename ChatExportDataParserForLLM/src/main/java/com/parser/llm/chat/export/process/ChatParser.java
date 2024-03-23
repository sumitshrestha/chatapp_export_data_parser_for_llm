package com.parser.llm.chat.export.process;

import lombok.extern.log4j.Log4j2;
import org.json.JSONArray;
import org.json.JSONObject;
import com.parser.llm.source.DataParser;
import com.parser.llm.source.TelegramDataParser;
import com.parser.llm.source.WhatsappDataParser;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

@Log4j2
public class ChatParser {
    private static final int CRITERIA_CONVERSATION_SEPARATOR = 10;
    private static final int CRITERIA_CONVERSATION_MERGE = 20;
    public static final int MAX_LIMIT_MESSAGE_MERGE = 10;
    private final int MAX_BUCKET_SIZE;
    private final int MIN_BUCKET_SIZE;

    private final int MAX_TRAIN_DATA_SIZE;
    private final int TRAIN_DATA_COUNT;
    private final int TEST_DATA_COUNT;

    private final Map<String, DataParser> dataParserMap = Map.of("whatsapp", new WhatsappDataParser(), "telegram", new TelegramDataParser());

    private final String targetUser;
    private String folderPath = "input";

    private final Map<List<ChatMessage>, HashSet<ChatMessage>> map = new HashMap<>();

    public ChatParser(String targetUser, int TRAIN_DATA_COUNT, int TEST_DATA_COUNT, int MIN_BUCKET_SIZE, int MAX_BUCKET_SIZE, int max_train_data_size) {
        this.targetUser = targetUser;
        this.MAX_BUCKET_SIZE = MAX_BUCKET_SIZE;
        this.MIN_BUCKET_SIZE = MIN_BUCKET_SIZE;
        this.TRAIN_DATA_COUNT = TRAIN_DATA_COUNT;
        this.TEST_DATA_COUNT = TEST_DATA_COUNT;
        this.MAX_TRAIN_DATA_SIZE = max_train_data_size;
    }

    public void process() throws IOException {
        File folder = new File(folderPath);
        LinkedList<Conversation> allConversations = new LinkedList<>();
        if (folder.isDirectory()) {
            File[] directories = folder.listFiles();
            if (directories != null) {
                for (File directory : directories) {
                    if (directory.isDirectory()) {
                        String source = directory.getName();
                        log.info("Reading files off : " + source);
                        if (dataParserMap.containsKey(source)) {
                            DataParser parser = dataParserMap.get(source);
                            File[] files = directory.listFiles();
                            if (files != null) {
                                for (File file : files) {
                                    if (file.isFile()) {
                                        ArrayList<ChatMessage> chatMessages = parser.readFileIntoChatMessage(file);
                                        LinkedList<Conversation> conversations = processChat(chatMessages);
                                        log.info("size of conversation : {}", conversations.size());
                                        allConversations.addAll(conversations);
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                log.info("No files found in the folder.");
            }
        } else {
            log.info("{} is not a directory", folderPath);
            return;
        }
        if (allConversations.isEmpty()) {
            log.error("no input file to process or input file is empty");
            return;
        }
        int totalConversation = allConversations.size();
        log.info(String.format("total for difference of %d hour: %d", CRITERIA_CONVERSATION_MERGE, totalConversation));

        writeIntoFile(allConversations, "merged_conversation.txt");
        // create train and test data
        List<List<ChatMessage>> trainList = new LinkedList<>();
        Random random = new Random();
        int trainCounter = 0;
        while (trainList.size() < TRAIN_DATA_COUNT) {
            Conversation randomConversation = allConversations.get(random.nextInt(totalConversation));
            log.debug("1");
            List<ChatMessage> randomBlockOfMessage = randomConversation.getRandomBlockOfMessage();
            if (!randomBlockOfMessage.isEmpty() && isMultiplePeopleChat(randomBlockOfMessage) && jsonify(randomBlockOfMessage).length() < MAX_TRAIN_DATA_SIZE) {
                log.debug("2");
                if (doesNotContainsSameSubset(trainList, randomBlockOfMessage)) {
                    trainList.add(randomBlockOfMessage);
                    if ((++trainCounter) % 50 == 0) {
                        log.info("this is train data at {}", trainCounter);
                    }
                }
            }
        }

        List<List<ChatMessage>> testList = new LinkedList<>();
        int testCounter = 0;
        while (testList.size() < TEST_DATA_COUNT) {
            Conversation randomConversation = allConversations.get(random.nextInt(totalConversation));
            List<ChatMessage> randomBlockOfMessage = randomConversation.getRandomBlockOfMessage();
            if (!randomBlockOfMessage.isEmpty() && isMultiplePeopleChat(randomBlockOfMessage) && jsonify(randomBlockOfMessage).length() < MAX_TRAIN_DATA_SIZE) {
                if (doesNotContainsSameSubset(testList, randomBlockOfMessage)) {
                    testList.add(randomBlockOfMessage);
                    if ((++testCounter) % 50 == 0) {
                        log.info("this is train data at {}", testCounter);
                    }
                }
            }
        }

        writeIntoJsonLineFile(trainList, "train_conversation.json");
        writeIntoJsonLineFile(testList, "test_conversation.json");
    }

    private boolean doesNotContainsSameSubset(List<List<ChatMessage>> trainList, List<ChatMessage> randomBlockOfMessage) {
        for (List<ChatMessage> train : trainList) {
            if (isSubset(train, randomBlockOfMessage)) {
                return false;
            }
        }
        return true;
    }

    private boolean isMultiplePeopleChat(List<ChatMessage> randomBlockOfMessage) {
        Set<String> senderSet = new HashSet<>();
        for (ChatMessage msg : randomBlockOfMessage) {
            senderSet.add(msg.getSender());
        }
        return senderSet.size() > 1;
    }

    public boolean isSubset(List<ChatMessage> list1, List<ChatMessage> list2) {
        HashSet<ChatMessage> m1 = getCachedMessage(list1);
        HashSet<ChatMessage> m2 = getCachedMessage(list2);
        return m1.containsAll(list2) || m2.containsAll(list1);
    }

    private HashSet<ChatMessage> getCachedMessage(List<ChatMessage> list) {
        HashSet<ChatMessage> set;
        if (map.containsKey(list)) {
            set = map.get(list);
        } else {
            set = new HashSet<>(list);
            map.put(list, set);
        }
        return set;
    }

    private LinkedList<Conversation> processChat(ArrayList<ChatMessage> chatMessages) {
        ChatMessage previousMessage = null;
        boolean skipUpdate = false;
        ListIterator<ChatMessage> itr = chatMessages.listIterator();
        while (itr.hasNext()) {
            ChatMessage currentMessage = itr.next();
            if (previousMessage != null) {
                Duration duration = Duration.between(previousMessage.getTimestamp(), currentMessage.getTimestamp());
                if (duration.toMinutes() < MAX_LIMIT_MESSAGE_MERGE && targetUser.equals(previousMessage.getSender()) && targetUser.equals(currentMessage.getSender())) {
                    previousMessage.mergeMessage(currentMessage);
                    itr.remove();
                    skipUpdate = true;
                }
            }
            if (!skipUpdate) {
                previousMessage = currentMessage;
            } else skipUpdate = false;
        }

//        for (ChatMessage currentMessage : chatMessages) {
//            if (previousMessage != null) {
//                Duration duration = Duration.between(previousMessage.getTimestamp(), currentMessage.getTimestamp());
//                if (duration.toMinutes() < 10 && targetUser.equals(previousMessage.getSender()) && targetUser.equals(currentMessage.getSender())) {
//                    previousMessage.mergeMessage(currentMessage);
//                    if (chatMessages.remove(currentMessage))
//                        log.info("removing message");
//                    skipUpdate = true;
//                }
//            }
//            if (!skipUpdate) {
//                previousMessage = currentMessage;
//            } else skipUpdate = false;
//        }

        LinkedList<Conversation> conversationList = new LinkedList<>();
        Conversation previousConversation = null;
        for (ChatMessage currentMessage : chatMessages) {
            if (previousConversation == null) {
                previousConversation = new Conversation(MIN_BUCKET_SIZE, MAX_BUCKET_SIZE, targetUser);
                previousConversation.addChatMessage(currentMessage);
                conversationList.add(previousConversation);
            } else {
                ChatMessage lastMessage = previousConversation.getLastMessage();
                if (lastMessage == null) {
                    previousConversation.addChatMessage(currentMessage);
                } else {
                    Duration duration = Duration.between(lastMessage.getTimestamp(), currentMessage.getTimestamp());
                    long hours = duration.toHours();
                    if (hours < CRITERIA_CONVERSATION_SEPARATOR) {
                        previousConversation.addChatMessage(currentMessage);
                    } else { // separate from previous
//                        System.out.printf("separating due to difference of %d hours", hours);
                        previousConversation = new Conversation(MIN_BUCKET_SIZE, MAX_BUCKET_SIZE, targetUser);
                        previousConversation.addChatMessage(currentMessage);
                        conversationList.add(previousConversation);
                    }
                }
            }
        }

        LinkedList<Conversation> mergedList = new LinkedList<>();
        // merge conversations that are either just one person with nearest conversation
        Conversation conversation1 = conversationList.peekFirst();

        for (Conversation conversation2 : conversationList.subList(1, conversationList.size() - 1)) {
            boolean skip = false;
            if (conversation1.getSenderList().size() < 2 || conversation2.getSenderList().size() < 2) { // single person conversation. what are you doing just alone? come join the party
                Duration duration = Duration.between(conversation1.getLastMessage().getTimestamp(), conversation2.getFirstMessage().getTimestamp());
                if (duration.toHours() < CRITERIA_CONVERSATION_MERGE) { // look at the max time
                    conversation2.mergeConversation(conversation1);
                    skip = true;
                }
            }
            if (!skip)
                mergedList.add(conversation1);
            conversation1 = conversation2;
        }
        mergedList.add(conversationList.peekLast());
        return mergedList;
    }

    private void writeIntoFile(LinkedList<Conversation> conversationList, String fileName) throws IOException {
        if (createDestinationFolderIfNotExists()) {
            FileWriter writer = new FileWriter(getOutputFolderName() + fileName);
            writer.write(conversationList.stream().map(Conversation::getJsonArray).toList().toString());
            writer.close();
        } else log.info("not writing because output directory creation failed");
    }

    private void writeIntoFile(List<List<ChatMessage>> conversationList, String fileName) throws IOException {
        if (createDestinationFolderIfNotExists()) {
            FileWriter writer = new FileWriter(getOutputFolderName() + fileName);
            JSONArray array = new JSONArray();
            for (List<ChatMessage> chatMessages : conversationList) {
                array.put(new JSONArray(chatMessages.stream().map(ChatMessage::getJson).collect(Collectors.toList())));
            }
            writer.write(array.toString());
            writer.close();
        } else log.info("not writing because output directory creation failed");
    }

    private void writeIntoJsonLineFile(List<List<ChatMessage>> conversationList, String fileName) throws IOException {
        if (createDestinationFolderIfNotExists()) {
            FileWriter writer = new FileWriter(getOutputFolderName() + fileName);
            List<Integer> lengthDataList = new ArrayList<>();
            long totalLength = 0;
            for (List<ChatMessage> conversation : conversationList) {
                String llmData = jsonify(conversation);
                totalLength += llmData.length();
                lengthDataList.add(llmData.length());
                writer.write(llmData);
                writer.append('\n');
            }
            writer.close();
            // run some stats
            log.info("stats for {} of length {}", fileName, conversationList.size());
            log.info("average : {}", totalLength / conversationList.size());
            Collections.sort(lengthDataList);
            log.info("median : {}", getMedian(lengthDataList));
            log.info("min length data : {}", lengthDataList.stream().findFirst().orElse(-1));
            log.info("max length data : {}", lengthDataList.stream().skip(lengthDataList.size() - 1).findFirst().orElse(-1));
        } else log.info("not writing because output directory creation failed");
    }

    private String jsonify(List<ChatMessage> conversation) {
        JSONObject data = new JSONObject();
        ChatMessage lastResponse = conversation.get(conversation.size() - 1);
        data.put("output", lastResponse.getSender() + ": " + lastResponse.getMessage());
        data.put("input", stringify(conversation.stream().limit(conversation.size() - 1).toList())); // get all the messages serially skipping last one which becomes target's response
        String personTalkingToTarget = conversation.stream().findFirst().orElse(Conversation.EMPTY_CHAT_MESSAGE).getSender();
//        data.put("instruction", "Respond based on context in chat snippet input");
        data.put("instruction", String.format("Generate a possible response from %s to %s's last message in the given chat snippet, ensuring it aligns with the context of their conversation.", targetUser, personTalkingToTarget));
        return data.toString();
    }

    private int getMedian(List<Integer> sortedList) {
        int median;
        int midIndex = sortedList.size() / 2;
        if (sortedList.size() % 2 == 0) {
            // Even-sized array
            median = (sortedList.get(midIndex - 1) + sortedList.get(midIndex)) / 2;
        } else {
            // Odd-sized array
            median = sortedList.get(midIndex);
        }

        return median;
    }

    private String getOutputFolderName() {
        return "output_" + this.targetUser + "/";
    }

    private String stringify(List<ChatMessage> chatMessages) {
        StringBuilder builder = new StringBuilder();
        for (ChatMessage msg : chatMessages) {
            builder.append('\n').append(msg.getSender()).append(": ").append(msg.getMessage());
        }

        return builder.toString();
    }

    private boolean createDestinationFolderIfNotExists() {
        File file = new File(getOutputFolderName());
        if (file.exists()) {
            if (!file.isDirectory()) {
                log.info("output exists but not as directory");
                return false;
            }
        }
        if (!file.exists()) {
            return file.mkdir();
        }
        return true;
    }

}