package com.andretti101.escolaweb.model.enums;

public enum PeriodNumber {
    ONE("1"),
    TWO("2"),
    THREE("3"),
    FOUR("4");

    private final String value;

    PeriodNumber(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
