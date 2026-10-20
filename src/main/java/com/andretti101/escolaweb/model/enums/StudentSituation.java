package com.andretti101.escolaweb.model.enums;

public enum StudentSituation {
    APPROVED("Aprovado"),
    FAILED("Reprovado"),
    PENDING("Pendente");

    private final String label;

    StudentSituation(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
