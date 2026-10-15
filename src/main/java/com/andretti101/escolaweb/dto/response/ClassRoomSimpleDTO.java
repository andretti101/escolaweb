package com.andretti101.escolaweb.dto.response;

import com.andretti101.escolaweb.model.enums.AcademicPeriodType;

public record ClassRoomSimpleDTO(
        Integer id,
        String name,
        AcademicPeriodType periodType,
        String periodTypeLabel
) {
    public ClassRoomSimpleDTO(Integer id, String name) {
        this(id, name, null, null);
    }
}
