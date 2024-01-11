package parser.llm;

import lombok.extern.log4j.Log4j2;
import org.json.JSONArray;

import java.util.*;
import java.util.stream.Collectors;

@Log4j2
public class Conversation {
    public final int MAX_BUCKET_SIZE;
    public final int MIN_BUCKET_SIZE;
    public static final ChatMessage EMPTY_CHAT_MESSAGE = ChatMessage.ChatMessageBuilder.get().build();
    private final String targetUser;

    private static final Random random = new Random();
    private final LinkedList<ChatMessage> messageList = new LinkedList<>();
    private LinkedList<Integer> targetUserIndices;
    private int countAtTargetIndexCalculation = -1;

    public Conversation(int MIN_BUCKET_SIZE, int MAX_BUCKET_SIZE, String targetUser) {
        this.MAX_BUCKET_SIZE = MAX_BUCKET_SIZE;
        this.MIN_BUCKET_SIZE = MIN_BUCKET_SIZE;
        this.targetUser = targetUser;
    }

    public void addChatMessage(ChatMessage message) {
        this.messageList.add(message);
    }

    public ChatMessage getLastMessage() {
        return this.messageList.peekLast();
    }

    public ChatMessage getFirstMessage() {
        return this.messageList.peekFirst();
    }

    public JSONArray getJsonArray() {
        return new JSONArray(messageList.stream().map(ChatMessage::getJson).collect(Collectors.toList()));
    }

    public Set<String> getSenderList() {
        return messageList.stream().map(ChatMessage::getSender).collect(Collectors.toSet());
    }

    public void mergeConversation(Conversation conversation) {
        this.messageList.addAll(conversation.messageList);
    }

    public List<ChatMessage> getRandomBlockOfMessage() {
        if (this.messageList.size() < MIN_BUCKET_SIZE)
            return new ArrayList<>(); // Return an empty list

        LinkedList<Integer> indices = this.calculateTargetUserIndices();
        if (!indices.isEmpty() && indices.peekLast() < MIN_BUCKET_SIZE)
            return new ArrayList<>(); // Return an empty list

        int targetIndex = -1;
        int sublistSize = -1;
        do {
            // Find a random occurrence of the target item
            targetIndex = findRandomOccurrenceOfTarget();
            if (targetIndex == -1) {
                // Target item not found
                return new ArrayList<>(); // Return an empty list
            }

            // Generate random sublist size between 2 and 10, ensuring it doesn't exceed available space
            sublistSize = random.nextInt(MIN_BUCKET_SIZE, MAX_BUCKET_SIZE + 1);
        } while (hasRightBlockOfMessageNotFound(targetIndex, sublistSize));

        // Create the sublist starting from the target item's index and extending backwards
        return this.messageList.subList(targetIndex - sublistSize + 1, targetIndex + 1);
    }

    /**
     * first condition: size of random block is greater than the actual size that it will help pluck off from the message list
     * second condition: the block that is pluck off when first condition is not met, does the first message is from the target user
     *
     * @param targetIndex
     * @param sublistSize
     * @return
     */
    private boolean hasRightBlockOfMessageNotFound(int targetIndex, int sublistSize) {
        return sublistSize > (targetIndex + 1) || this.targetUser.equalsIgnoreCase(this.messageList.subList(targetIndex - sublistSize + 1, targetIndex + 1).stream().findFirst().orElse(EMPTY_CHAT_MESSAGE).getSender());
    }

    private int findRandomOccurrenceOfTarget() {
        List<Integer> indices = this.calculateTargetUserIndices();
        if (indices.isEmpty()) {
            return -1; // Item not found
        }

        return indices.get(random.nextInt(indices.size())); // Return a random index from the list of occurrences
    }

    private LinkedList<Integer> calculateTargetUserIndices() {
        if (targetUserIndices == null)
            targetUserIndices = new LinkedList<>();

        final int messageSize = this.messageList.size();
        if (this.countAtTargetIndexCalculation == messageSize)
            return this.targetUserIndices;

        for (int i = 0; i < messageSize; i++) {
            if (Optional.of(targetUser).orElse("").equalsIgnoreCase(this.messageList.get(i).getSender())) {
                targetUserIndices.add(i);
            }
        }

        this.countAtTargetIndexCalculation = messageSize;
        return this.targetUserIndices;
    }
}
