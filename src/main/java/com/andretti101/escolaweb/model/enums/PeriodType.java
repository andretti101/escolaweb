package com.andretti101.escolaweb.model.enums;

public enum PeriodType {
    BIMESTER("Bimestre"),
    TRIMESTER("Trimestre"),
    SEMESTER("Semestre");

    private final String label;

    PeriodType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
