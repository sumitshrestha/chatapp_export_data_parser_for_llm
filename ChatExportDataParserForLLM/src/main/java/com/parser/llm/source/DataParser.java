package com.parser.llm.source;

import com.parser.llm.chat.export.process.ChatMessage;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

public interface DataParser {
    public ArrayList<ChatMessage> readFileIntoChatMessage(File file) throws IOException;
}
