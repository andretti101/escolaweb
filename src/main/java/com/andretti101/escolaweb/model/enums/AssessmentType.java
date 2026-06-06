package com.andretti101.escolaweb.model.enums;

public enum AssessmentType {
    EXAM("Prova"),
    ASSIGNMENT("Trabalho"),
    ACTIVITY("Atividade");
    private final String label;

    AssessmentType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}