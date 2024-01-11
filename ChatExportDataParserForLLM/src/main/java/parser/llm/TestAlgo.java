package parser.llm;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TestAlgo {
    public static void main(String[] a) {
        Conversation conversation = new Conversation(1, 1, "cherrYk");
        conversation.addChatMessage(ChatMessage.ChatMessageBuilder.get().sender("apple").build());
        conversation.addChatMessage(ChatMessage.ChatMessageBuilder.get().sender("banana").build());
        conversation.addChatMessage(ChatMessage.ChatMessageBuilder.get().sender("apple").build());
        conversation.addChatMessage(ChatMessage.ChatMessageBuilder.get().sender("cherry").build());
        conversation.addChatMessage(ChatMessage.ChatMessageBuilder.get().sender("banana").build());
        conversation.addChatMessage(ChatMessage.ChatMessageBuilder.get().sender("apple").build());
        conversation.addChatMessage(ChatMessage.ChatMessageBuilder.get().sender("orange").build());
        conversation.addChatMessage(ChatMessage.ChatMessageBuilder.get().sender("banana").build());
        conversation.addChatMessage(ChatMessage.ChatMessageBuilder.get().sender("orange").build());
        conversation.addChatMessage(ChatMessage.ChatMessageBuilder.get().sender("mango").build());
        conversation.addChatMessage(ChatMessage.ChatMessageBuilder.get().sender("cherry").build());
        conversation.addChatMessage(ChatMessage.ChatMessageBuilder.get().sender("banana").build());
        conversation.addChatMessage(ChatMessage.ChatMessageBuilder.get().sender("orange").build());
        conversation.addChatMessage(ChatMessage.ChatMessageBuilder.get().sender("mango").build());
        conversation.addChatMessage(ChatMessage.ChatMessageBuilder.get().sender("cherry").build());
        conversation.addChatMessage(ChatMessage.ChatMessageBuilder.get().sender("banana").build());
        Map<Integer, Integer> map = new HashMap<>();
        for (int i = 0; i < 1000; i++) {
            List<ChatMessage> list = conversation.getRandomBlockOfMessage();
            int size = list.size();
            System.out.print("count " + size + "::");
            for (ChatMessage m : list) {
                System.out.print(m.getSender() + ",");
            }
            System.out.println();
            if (map.containsKey(size)) {
                map.put(size, map.get(size) + 1);
            } else
                map.put(size, 1);
        }
        System.out.println("this is final stats: " + map);
    }
}
