package com.andretti101.escolaweb.model.enums;

public enum UserRole {
    STUDENT("Aluno"),
    TEACHER("Professor"),
    SECRETARY("Secretaria"),
    PRINCIPAL("Direção");

    private final String label;

    UserRole(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
