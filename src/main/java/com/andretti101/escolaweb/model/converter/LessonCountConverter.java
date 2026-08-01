package com.andretti101.escolaweb.model.converter;

import com.andretti101.escolaweb.model.enums.LessonCount;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class LessonCountConverter implements AttributeConverter<LessonCount, Integer> {

    @Override
    public Integer convertToDatabaseColumn(LessonCount attribute) {
        return attribute != null ? attribute.getValue() : null;
    }

    @Override
    public LessonCount convertToEntityAttribute(Integer dbData) {
        return dbData != null ? LessonCount.fromValue(dbData) : null;
    }
}
