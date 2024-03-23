package com.parser.llm.source.pojo;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class DiscordMessage {

    private String id;
    private String type;
    private String timestamp;
    private String timestampEdited;
    private String callEndedTimestamp;
    private boolean isPinned;
    private String content;
    private Author author;
    private List<Object> attachments;
    private List<Object> embeds;
    private List<Object> stickers;
    private List<Object> reactions;
    private List<User> mentions;
    private Reference reference;

    @Getter
    @Setter
    public static class Author {
        private String id;
        private String name;
        private String discriminator;
        private String nickname;
        private String color;
        private boolean isBot;
        private List<Role> roles;
        private String avatarUrl;
    }

    @Getter
    @Setter
    public static class User {
        private String id;
        private String name;
        private String discriminator;
        private String nickname;
        private String color;
        private boolean isBot;
        private List<Role> roles;
        private String avatarUrl;
    }

    @Getter
    @Setter
    public static class Reference {
        private String messageId;
        private String channelId;
        private String guildId;

    }

    @Getter
    @Setter
    public static class Role {
        private String id;
        private String name;
        private String color;
        private int position;
    }
}
