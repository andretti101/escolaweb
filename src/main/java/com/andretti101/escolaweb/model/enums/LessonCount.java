package com.andretti101.escolaweb.model.enums;

public enum LessonCount {

    ONE(1),
    TWO(2),
    THREE(3),
    FOUR(4),
    FIVE(5),
    SIX(6);

    private final int value;

    LessonCount(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }
    public static LessonCount fromValue(int value) {
        for (LessonCount lc : values()) {
            if (lc.value == value) {
                return lc;
            }
        }
        throw new IllegalArgumentException(
                "No LessonCount constant mapped to database value: " + value
                        + ". Valid range is 1–6.");
    }
}
