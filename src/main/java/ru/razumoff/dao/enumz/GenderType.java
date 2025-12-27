package ru.razumoff.dao.enumz;

import lombok.Getter;

@Getter
public enum GenderType {
    MALE("Мужской"),
    FEMALE("Женский"),
    UNKNOWN("Не указан");

    private final String ru_name;

    GenderType(String ru_name) {
        this.ru_name = ru_name;
    }
}
