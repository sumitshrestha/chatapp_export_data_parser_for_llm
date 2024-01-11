package parser.llm;

import lombok.extern.log4j.Log4j2;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;
import java.util.Scanner;

@Log4j2
public class Main {

    public static void main(String[] args) {
        Properties prop = new Properties();

        try {
            FileInputStream fis = new FileInputStream("config.properties"); // Specify file path
            prop.load(fis);
            fis.close();
        } catch (IOException e) {
            log.error(e);
        }

        String targetUser = prop.getProperty("target.user", "");
        String trainDataCountStr = prop.getProperty("train.data.count", "");
        String testDataCountStr = prop.getProperty("test.data.count", "");
        String chatMinSizeStr = prop.getProperty("chat.min.size", "");
        String chatMaxSizeStr = prop.getProperty("chat.max.size", "");

        if (targetUser.isEmpty()) {
            do {
                System.out.println("enter user to be simulated");
                targetUser = readFromConsole();
            } while (targetUser.isEmpty());
        }
        int trainDataCount = getIntValue(trainDataCountStr, 1000, "enter train data count. enter newline to default to 1000", "train data in config is invalid. enter train data. enter newline to default to 1000");
        int testDataCount = getIntValue(testDataCountStr, 160, "enter test data count. enter newline to default to 160", "test data in config is invalid. enter test data. enter newline to default to 160");
        int chatMinSize = getIntValue(chatMinSizeStr, 3, "enter minimum block size to be selected. enter newline to default to 3", "enter minimum block size in config is invalid. enter minimum block size to be selected. enter newline to default to 3");
        int chatMaxSize = getIntValue(chatMaxSizeStr, 10, "enter maximum block size to be selected. enter newline to default to 10", "enter maximum block size in config is invalid. enter maximum block size to be selected. enter newline to default to 10");
        try {
            log.info(String.format("targetUser %s, trainDataCount %d, testDataCount %d, chatMinSize %d, chatMaxSize %d", targetUser, trainDataCount, testDataCount, chatMinSize, chatMaxSize));
            new WhatsAppChatParser(targetUser, trainDataCount, testDataCount, chatMinSize, chatMaxSize).process();
        } catch (IOException e) {
            log.error(e);
        }
    }

    private static int getIntValue(String trainDataCountStr, final int defaultValue, final String msg1, final String msg2) {
        int trainDataCount;
        if (trainDataCountStr.isEmpty()) {
            do {
                try {
                    System.out.println(msg1);
                    String k = readFromConsole();
                    if (k.isEmpty()) trainDataCount = defaultValue;
                    else trainDataCount = Integer.parseInt(k);
                } catch (NumberFormatException e) {
                    trainDataCount = -1;
                }
            } while (trainDataCount < 0);
        } else {
            try {
                trainDataCount = Integer.parseInt(trainDataCountStr);
            } catch (NumberFormatException e) {
                do {
                    try {
                        System.out.println(msg2);
                        String k = readFromConsole();
                        if (k.isEmpty()) trainDataCount = defaultValue;
                        else trainDataCount = Integer.parseInt(k);
                    } catch (NumberFormatException e1) {
                        trainDataCount = -1;
                    }
                } while (trainDataCount < 0);
            }
        }

        return trainDataCount;
    }

    private static String readFromConsole() {
        Scanner scanner = new Scanner(System.in);
        return scanner.nextLine();
    }

}