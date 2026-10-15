package com.andretti101.escolaweb.dto.response;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class PagedResponse<T> extends PageImpl<T> {

    public PagedResponse(List<T> content, Pageable pageable, long total) {
        super(content != null ? content : Collections.emptyList(), pageable != null ? pageable : Pageable.unpaged(), total);
    }

    public PagedResponse(List<T> content) {
        super(content != null ? content : Collections.emptyList());
    }

    public static <T> PagedResponse<T> of(Page<T> page) {
        return new PagedResponse<>(page.getContent(), page.getPageable(), page.getTotalElements());
    }

    public static <T> PagedResponse<T> of(List<T> content, Pageable pageable, long total) {
        return new PagedResponse<>(content, pageable, total);
    }

    @JsonAnyGetter
    public Map<String, T> getIndexedElements() {
        Map<String, T> map = new LinkedHashMap<>();
        List<T> list = getContent();
        for (int i = 0; i < list.size(); i++) {
            map.put(String.valueOf(i), list.get(i));
        }
        return map;
    }
}
