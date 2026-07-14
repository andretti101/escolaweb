package com.andretti101.escolaweb.model.enums;

public enum AttendanceStatus {
    PRESENT("Presente"),
    ABSENT("Falta"),
    JUSTIFIED_ABSENCE("Falta Justificada");

    private final String label;

    AttendanceStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
