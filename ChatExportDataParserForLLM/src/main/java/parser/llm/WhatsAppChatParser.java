package parser.llm;

import lombok.extern.log4j.Log4j2;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.*;
import java.time.Duration;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Log4j2
public class WhatsAppChatParser {
    private static final int CRITERIA_CONVERSATION_SEPARATOR = 10;
    private static final int CRITERIA_CONVERSATION_MERGE = 20;
    private final int MAX_BUCKET_SIZE;
    private final int MIN_BUCKET_SIZE;
    private final int TRAIN_DATA_COUNT;
    private final int TEST_DATA_COUNT;
    private static final Pattern iphoneMessagePattern = Pattern.compile("\\[(?<month>\\d{1,2})/(?<day>\\d{1,2})/(?<year>\\d{1,2}), (?<hour>\\d{1,2}):(?<minute>\\d{1,2}):(?<second>\\d{1,2}) (?<daynight>[AP]M)\\] (?<personName>[\\w ]+): (?<message>.*)");
    private static final Pattern androidMessagePattern = Pattern.compile("(?<month>\\d{2})/(?<day>\\d{2})/(?<year>\\d{2}), (?<hour>\\d{1,2}):(?<minute>\\d{1,2}) (?<daynight>[AP]M) - (?<personName>[\\w ]+): (?<message>.*)");
    private static final char wierdCharacter = ' ';

    private final String targetUser;
    private String folderPath = "input";

    private final Map<List<ChatMessage>, HashSet<ChatMessage>> map = new HashMap<>();

    private final String[] blacklistedMessages = {
            "Messages and calls are end-to-end encrypted. No one outside of this chat, not even WhatsApp, can read or listen to them.".toLowerCase(),
            "<Media omitted>".toLowerCase(),
            "image omitted".toLowerCase(),
            "video omitted".toLowerCase(),
            "You deleted this message.".toLowerCase()
    };

    public WhatsAppChatParser(String targetUser, int TRAIN_DATA_COUNT, int TEST_DATA_COUNT, int MIN_BUCKET_SIZE, int MAX_BUCKET_SIZE) {
        this.targetUser = targetUser;
        this.MAX_BUCKET_SIZE = MAX_BUCKET_SIZE;
        this.MIN_BUCKET_SIZE = MIN_BUCKET_SIZE;
        this.TRAIN_DATA_COUNT = TRAIN_DATA_COUNT;
        this.TEST_DATA_COUNT = TEST_DATA_COUNT;
    }

    public void process() throws IOException {
        File folder = new File(folderPath);
        LinkedList<Conversation> allConversations = new LinkedList<>();
        if (folder.isDirectory()) {
            File[] files = folder.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isFile()) { // only read files. dont care for any subdirectories under
                        String fileName = file.getName();
                        log.info("File: " + fileName);
                        LinkedList<Conversation> conversations = processChat(targetUser, file);
                        log.info("size of conversation :" + conversations.size());
                        allConversations.addAll(conversations);
                    }
                }
            } else {
                log.info("No files found in the folder.");
            }
        } else {
            log.info(folderPath + " is not a directory.");
            return;
        }

        int totalConversation = allConversations.size();
        log.info(String.format("total for difference of %d hour: %d", CRITERIA_CONVERSATION_MERGE, totalConversation));

        writeIntoFile(allConversations, "merged_conversation.txt");
        // create train and test data
        List<List<ChatMessage>> trainList = new LinkedList<>();
        Random random = new Random();
        int trainCounter = 0;
        int i = 1;
        while (trainList.size() < TRAIN_DATA_COUNT) {
            Conversation randomConversation = allConversations.get(random.nextInt(totalConversation));
            log.debug("1");
            List<ChatMessage> randomBlockOfMessage = randomConversation.getRandomBlockOfMessage();
            if (!randomBlockOfMessage.isEmpty()) {
                log.debug("2");
                if (doesNotContainsSameSubset(trainList, randomBlockOfMessage)) {
                    trainList.add(randomBlockOfMessage);
                    if ((++trainCounter) % 50 == 0) {
                        log.info("this is train data at " + trainCounter);
                    }
                }
            }
        }

        List<List<ChatMessage>> testList = new LinkedList<>();
        int testCounter = 0;
        while (testList.size() < TEST_DATA_COUNT) {
            Conversation randomConversation = allConversations.get(random.nextInt(totalConversation));
            List<ChatMessage> randomBlockOfMessage = randomConversation.getRandomBlockOfMessage();
            if (!randomBlockOfMessage.isEmpty()) {
                if (doesNotContainsSameSubset(testList, randomBlockOfMessage)) {
                    testList.add(randomBlockOfMessage);
                    if ((++testCounter) % 50 == 0) {
                        log.info("this is train data at " + testCounter);
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

    private LinkedList<Conversation> processChat(String target, File file) throws IOException {
        FileReader fileReader = new FileReader(file);
        // Read the WhatsApp chat export file
        BufferedReader bufferedReader = new BufferedReader(fileReader);

        // Initialize a list to store the chat messages
        ArrayList<ChatMessage> chatMessages = new ArrayList<>();

        // Iterate over the chat messages
        String line;
        while ((line = bufferedReader.readLine()) != null) {
            line = line.replace(wierdCharacter, ' ');
            boolean isLineToBeSkipped = false;
            for (String b : blacklistedMessages) {
                if (line.toLowerCase().endsWith(b)) {
                    isLineToBeSkipped = true;
                    break;
                }
            }
            if (isLineToBeSkipped) continue;

            Matcher matcher = iphoneMessagePattern.matcher(line);
            if (matcher.matches()) {
                ChatMessage chatMessage = ChatMessage.ChatMessageBuilder.get()
                        .sender(matcher.group("personName")).message(matcher.group("message"))
                        .month(matcher.group("month")).day(matcher.group("day")).year(matcher.group("year"))
                        .hour(matcher.group("hour")).minute(matcher.group("minute")).second(matcher.group("second")).isDay(matcher.group("daynight"))
                        .build();
                // Add the ChatMessage object to the list
                chatMessages.add(chatMessage);
            } else {
                matcher = androidMessagePattern.matcher(line);
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

        LinkedList<Conversation> conversationList = new LinkedList<>();
        Conversation previousConversation = null;
        for (ChatMessage currentMessage : chatMessages) {
            if (previousConversation == null) {
                previousConversation = new Conversation(MIN_BUCKET_SIZE, MAX_BUCKET_SIZE, target);
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
                        previousConversation = new Conversation(MIN_BUCKET_SIZE, MAX_BUCKET_SIZE, target);
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
            for (List<ChatMessage> conversation : conversationList) {
                JSONObject data = new JSONObject();
                ChatMessage lastResponse = conversation.get(conversation.size() - 1);
                data.put("output", lastResponse.getSender() + ": " + lastResponse.getMessage());
                data.put("input", stringify(conversation.stream().limit(conversation.size() - 1).toList())); // get all the messages serially skipping last one which becomes target's response
                String personTalkingToTarget = conversation.stream().findFirst().orElse(Conversation.EMPTY_CHAT_MESSAGE).getSender();
                data.put("instruction", String.format("Generate a possible response from %s to %s's last message in the given chat snippet, ensuring it aligns with the context of their conversation.", targetUser, personTalkingToTarget));
                writer.write(data.toString());
                writer.append('\n');
            }
            writer.close();
        } else log.info("not writing because output directory creation failed");
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