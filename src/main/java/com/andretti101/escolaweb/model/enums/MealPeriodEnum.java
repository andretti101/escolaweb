package com.andretti101.escolaweb.model.enums;

public enum MealPeriodEnum {
    MORNING("Manhã", "Café da Manhã / Lanche"),
    LUNCH("Almoço", "Refeição Principal"),
    AFTERNOON("Tarde", "Lanche da Tarde");

    private final String label;
    private final String description;

    MealPeriodEnum(String label, String description) {
        this.label = label;
        this.description = description;
    }

    public String getLabel() {
        return label;
    }

    public String getDescription() {
        return description;
    }
}
