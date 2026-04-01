package com.jnet.common.exception;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class ServiceException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private Integer code;

    private String message;

    public ServiceException(String message) {
        this(message, 500);
    }

    public ServiceException(String message, Integer code) {
        super(message);
        this.code = code;
        this.message = message;
    }

}
