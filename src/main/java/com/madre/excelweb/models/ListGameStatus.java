package com.madre.excelweb.models;

import lombok.Getter;

public enum ListGameStatus {
    COMPLETED("Completed"),
    PLAYING("Playing"),
    DROPPED("Dropped"),
    PLAN_TO_PLAY("Plan to Play"),
    ON_HOLD("On Hold"),
    REPLAYING("Replaying"),
    WATCHING("Watching"),
    WANTED("Wanted");


    @Getter
    private final String name;


    ListGameStatus(String name) {
        this.name = name;
    }


    public static ListGameStatus fromString(String text) {
        for (ListGameStatus b : ListGameStatus.values()) {
            if (b.name.equalsIgnoreCase(text)) {
                return b;
            }
        }
        return null;
    }

    public static ListGameStatus fromStringSwitch(String text) {
        return switch (text.toLowerCase()) {
            case "completed" -> COMPLETED;
            case "playing" -> PLAYING;
            case "dropped" -> DROPPED;
            case "plan to play" -> PLAN_TO_PLAY;
            case "on hold" -> ON_HOLD;
            case "replaying" -> REPLAYING;
            case "watching" -> WATCHING;
            case "wanted" -> WANTED;
            default -> null;
        };

    }

    @Override
    public String toString() {
        return name;
    }
}
