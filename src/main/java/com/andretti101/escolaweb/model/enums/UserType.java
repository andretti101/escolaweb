package com.andretti101.escolaweb.model.enums;

public enum UserType {
    ADMINISTRATION("Secretaria"),
    MANAGEMENT("Direção"),
    TEACHER("Professor"),
    STUDENT("Aluno");

    private final String label;

    UserType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
