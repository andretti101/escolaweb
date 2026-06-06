package com.andretti101.escolaweb.model.enums;

public enum Shift {
    MORNING("Matutino"),
    AFTERNOON("Vespertino"),
    EVENING("Noturno"),
    FULL_TIME("Integral");

    private final String label;

    Shift(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
