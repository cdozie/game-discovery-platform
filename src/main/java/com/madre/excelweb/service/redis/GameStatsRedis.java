package com.madre.excelweb.service.redis;

import lombok.Getter;

public enum GameStatsRedis {

    INTERACTIONS("interactions"),
    WISHLISTS("wishlists"),

    COMMENTS("comments"),
    POSTS("posts");


    @Getter
    private final String name;

    GameStatsRedis(String name) {
        this.name = "game:" + name + ":";
    }
}
