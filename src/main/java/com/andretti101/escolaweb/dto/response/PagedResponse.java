package com.andretti101.escolaweb.dto.response;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.Collections;
import java.util.List;

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
}
