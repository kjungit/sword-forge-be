package com.codex.swordgrowth.domain.weapon;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Locale;

public enum Grade {
    NORMAL,
    RARE,
    EPIC,
    LEGENDARY;

    @JsonCreator
    public static Grade from(String value) {
        return Grade.valueOf(value.trim().toUpperCase(Locale.ROOT));
    }

    @JsonValue
    public String code() {
        return name().toLowerCase();
    }
}
