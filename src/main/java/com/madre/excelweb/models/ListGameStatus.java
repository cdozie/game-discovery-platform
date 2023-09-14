package com.madre.excelweb.models;

public enum ListGameStatus {
    COMPLETED("Completed"),
    PLAYING("Playing"),
    DROPPED("Dropped"),
    PLAN_TO_PLAY("Plan to Play"),
    ON_HOLD("On Hold"),
    REPLAYING("Replaying"),
    WATCHING("Watching"),
    WANTED("Wanted");


    private final String name;


    ListGameStatus(String name) {
        this.name = name;
    }
}
