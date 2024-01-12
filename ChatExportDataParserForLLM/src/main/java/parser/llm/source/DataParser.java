package parser.llm.source;

import parser.llm.ChatMessage;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.regex.Matcher;

public interface DataParser {
    public ArrayList<ChatMessage> readFileIntoChatMessage(File file) throws IOException;
}
