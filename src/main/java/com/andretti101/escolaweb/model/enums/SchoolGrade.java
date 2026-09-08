package com.andretti101.escolaweb.model.enums;

import lombok.Getter;

@Getter
public enum SchoolGrade {

    PRE_1("Pré-Escola 1"),
    PRE_2("Pré-Escola 2"),
    PRE_3("Pré-Escola 3"),
    FUNDAMENTAL_1("1º Ano Fundamental"),
    FUNDAMENTAL_2("2º Ano Fundamental"),
    FUNDAMENTAL_3("3º Ano Fundamental"),
    FUNDAMENTAL_4("4º Ano Fundamental"),
    FUNDAMENTAL_5("5º Ano Fundamental"),
    FUNDAMENTAL_6("6º Ano Fundamental"),
    FUNDAMENTAL_7("7º Ano Fundamental"),
    FUNDAMENTAL_8("8º Ano Fundamental"),
    FUNDAMENTAL_9("9º Ano Fundamental"),
    MEDIO_1("1º Ano Médio"),
    MEDIO_2("2º Ano Médio"),
    MEDIO_3("3º Ano Médio");

    private final String label;

    SchoolGrade(String label) {
        this.label = label;
    }
}
