package com.andretti101.escolaweb.model.enums;

public enum AcademicPeriodType {
    BIMESTER("Bimestre"),
    TRIMESTER("Trimestre"),
    QUADRIMESTER("Quadrimestre"),
    SEMESTER("Semestre"),
    ANNUAL("Anual");

    private final String label;

    AcademicPeriodType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
