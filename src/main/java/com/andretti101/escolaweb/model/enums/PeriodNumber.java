package com.andretti101.escolaweb.model.enums;

public enum PeriodNumber {
    ONE("1º Período"),
    TWO("2º Período"),
    THREE("3º Período"),
    FOUR("4º Período");

    private final String label;

    PeriodNumber(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}