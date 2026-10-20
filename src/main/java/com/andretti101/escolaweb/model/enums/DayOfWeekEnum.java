package com.andretti101.escolaweb.model.enums;

import java.time.DayOfWeek;

public enum DayOfWeekEnum {
    SEGUNDA("Segunda-feira", DayOfWeek.MONDAY),
    TERCA("Terça-feira", DayOfWeek.TUESDAY),
    QUARTA("Quarta-feira", DayOfWeek.WEDNESDAY),
    QUINTA("Quinta-feira", DayOfWeek.THURSDAY),
    SEXTA("Sexta-feira", DayOfWeek.FRIDAY),
    SABADO("Sábado", DayOfWeek.SATURDAY);

    private final String label;
    private final DayOfWeek javaDayOfWeek;

    DayOfWeekEnum(String label, DayOfWeek javaDayOfWeek) {
        this.label = label;
        this.javaDayOfWeek = javaDayOfWeek;
    }

    public String getLabel() {
        return label;
    }

    public DayOfWeek getJavaDayOfWeek() {
        return javaDayOfWeek;
    }

    public static DayOfWeekEnum fromJavaDayOfWeek(DayOfWeek dow) {
        if (dow == null) return null;
        for (DayOfWeekEnum day : values()) {
            if (day.getJavaDayOfWeek() == dow) {
                return day;
            }
        }
        return null;
    }
}
