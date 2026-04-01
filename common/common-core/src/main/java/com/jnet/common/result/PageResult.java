package com.jnet.common.result;

import lombok.Data;

import java.io.Serializable;

@Data
public class PageResult<T> implements Serializable {

    private static final long serialVersionUID = 1L;

    private java.util.List<T> records;

    private long total;

    private long size;

    private long current;

    public PageResult() {
    }

    public PageResult(java.util.List<T> records, long total, long size, long current) {
        this.records = records;
        this.total = total;
        this.size = size;
        this.current = current;
    }

    public static <T> PageResult<T> of(java.util.List<T> records, long total, long size, long current) {
        return new PageResult<>(records, total, size, current);
    }

}
