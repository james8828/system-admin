package com.jnet.common.result;

import lombok.Data;

import java.io.Serializable;

@Data
public class PageQuery implements Serializable {

    private static final long serialVersionUID = 1L;

    private int pageNum = 1;

    private int pageSize = 10;

    public PageQuery() {
    }

    public PageQuery(int pageNum, int pageSize) {
        this.pageNum = pageNum;
        this.pageSize = pageSize;
    }

}
