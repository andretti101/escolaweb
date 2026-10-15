package com.andretti101.escolaweb.model.enums;

public enum AcademicPeriodType {
    BIMESTER("Bimestre"),
    TRIMESTER("Trimestre"),
    SEMESTER("Semestre");

    private final String label;

    AcademicPeriodType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    public int getPeriodsCount() {
        return switch (this) {
            case BIMESTER -> 4;
            case TRIMESTER -> 3;
            case SEMESTER -> 2;
        };
    }

    public int getDivisor() {
        return getPeriodsCount();
    }
}
