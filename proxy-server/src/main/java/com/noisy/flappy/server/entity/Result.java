package com.noisy.flappy.server.entity;

/**
 * @author lei.X
 * @date 2019/8/28
 */
public class Result {

    //响应码
    private int code;

    public Result(int code) {
        this.code = code;
    }

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

}
