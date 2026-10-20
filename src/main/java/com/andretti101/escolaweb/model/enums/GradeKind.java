package com.andretti101.escolaweb.model.enums;

public enum GradeKind {
    ORIGINAL("Nota Original"),
    RECOVERY("Recuperação");

    private final String label;

    GradeKind(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
