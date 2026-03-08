package com.tower.game.common.exception;

import lombok.Getter;

/**
 * 业务异常，由业务层抛出，由全局异常处理器统一返回 ApiResponse
 */
@Getter
public class BusinessException extends RuntimeException {

    private final int code;

    public BusinessException(String message) {
        super(message);
        this.code = 400;
    }

    public BusinessException(int code, String message) {
        super(message);
        this.code = code;
    }
}
