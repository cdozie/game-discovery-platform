package com.madre.excelweb.models;


import lombok.Getter;

public enum Role {

    ADMIN,
    USER,
    MODERATOR,
    GUEST;

    @Getter
    private final String name;

    Role() {
        this.name = "ROLE_" + this.name();
    }
}
