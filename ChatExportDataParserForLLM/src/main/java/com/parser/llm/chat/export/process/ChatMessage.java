package com.parser.llm.chat.export.process;

import lombok.extern.log4j.Log4j2;
import org.json.JSONObject;

import java.time.DateTimeException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.Period;
import java.time.format.DateTimeFormatter;

@Log4j2
public class ChatMessage {

    private String sender;
    private String message;
    private LocalDateTime timestamp;

    private ChatMessage(String sender, String message, LocalDateTime timestamp) {
        this.sender = sender;
        this.message = message;
        this.timestamp = timestamp;
    }

    public String getSender() {
        return sender;
    }

    public String getMessage() {
        return message;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public String appendMessage(String text) {
        this.message += "\n" + text;
        return this.message;
    }

    public void mergeMessage(ChatMessage message) {
//        this.message += '\n' + message.getMessage();
        this.message += ". " + message.getMessage();
        Period period = getPeriod(this.timestamp, message.timestamp);

        // Calculate the time (hours, minutes, seconds) between the two dates
        long[] time = getTime(this.timestamp, message.timestamp);

        // Calculate the average period and time
        int totalSeconds = (int) (time[0] * 3600 + time[1] * 60 + time[2]);
        int averageSeconds = totalSeconds / 2;

        Period averagePeriod = period.plusDays(averageSeconds / (24 * 3600));
        Duration averageDuration = Duration.ofSeconds(averageSeconds % (24 * 3600));

        // Calculate the average LocalDateTime
        this.timestamp = this.timestamp.plus(averagePeriod).plus(averageDuration);
    }

    private static Period getPeriod(LocalDateTime dateTime1, LocalDateTime dateTime2) {
        return Period.between(dateTime1.toLocalDate(), dateTime2.toLocalDate());
    }

    private static long[] getTime(LocalDateTime dateTime1, LocalDateTime dateTime2) {
        LocalDateTime today = LocalDateTime.of(
                dateTime2.getYear(), dateTime2.getMonthValue(), dateTime2.getDayOfMonth(),
                dateTime1.getHour(), dateTime1.getMinute(), dateTime1.getSecond());

        Duration duration = Duration.between(today, dateTime2);
        long seconds = duration.getSeconds();
        long hours = seconds / 3600;
        long minutes = (seconds % 3600) / 60;
        long secs = seconds % 60;

        return new long[]{hours, minutes, secs};
    }

    public JSONObject getJson() {
        return new JSONObject().put("sender", sender).put("message", message).put("time", timestamp.format(DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy")));
    }

    public static class ChatMessageBuilder {
        private String sender;
        private String message;
        private int month;
        private int day;
        private int year;
        private int hour;
        private int minute;
        private int second;
        private boolean isPM = false;
        private LocalDateTime time;

        private ChatMessageBuilder() {
        }

        public static ChatMessageBuilder get() {
            return new ChatMessageBuilder();
        }

        public ChatMessageBuilder sender(String sender) {
            this.sender = sender;
            return this;
        }

        public ChatMessageBuilder message(String message) {
            this.message = message;
            return this;
        }

        public ChatMessageBuilder month(String month) {
            this.month = Integer.parseInt(month);
            return this;
        }

        public ChatMessageBuilder day(String day) {
            this.day = Integer.parseInt(day);
            return this;
        }

        public ChatMessageBuilder year(String year) {
            this.year = Integer.parseInt(year);
            return this;
        }

        public ChatMessageBuilder hour(String hour) {
            this.hour = Integer.parseInt(hour);
            if (isPM) {
                this.hour += 12;
            }
            return this;
        }

        public ChatMessageBuilder minute(String minute) {
            this.minute = Integer.parseInt(minute);
            return this;
        }

        public ChatMessageBuilder second(String second) {
            this.second = Integer.parseInt(second);
            return this;
        }

        public ChatMessageBuilder isDay(String daynight) {
            this.isPM = "PM".equalsIgnoreCase(daynight);
            this.hour = convertTimeTo24HourFormat(this.hour, daynight);
            return this;
        }

        public ChatMessageBuilder time(LocalDateTime time) {
            this.time = time;
            return this;
        }

        public int convertTimeTo24HourFormat(int hour12, String amPm) {
            int hour24 = 0;
            if ("am".equalsIgnoreCase(amPm)) {
                if (hour12 == 12) {
                    hour24 = 0; // Midnight
                } else {
                    hour24 = hour12;
                }
            } else { // assume its pm
                if (hour12 == 12) {
                    hour24 = 12; // Noon
                } else {
                    hour24 = hour12 + 12;
                }
            }

            return hour24;
        }

        public ChatMessage build() {
            if (time == null) {
                try {
                    time = LocalDateTime.of(year, month, day, hour, minute, second);
                } catch (DateTimeException e) {
                    log.error(e);
                }
            }

            return new ChatMessage(this.sender, this.message, time);
        }
    }
}