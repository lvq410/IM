package com.lvt4j.im.common;

/**
 * 含有错误码的异常
 * @author LV
 */
public class Err extends RuntimeException{

    private static final long serialVersionUID = 1L;

    /** 错误码：成功 */
    public static final int Success = 0;
    /** 错误码：默认失败 */
    public static final int DefFail = 1;
    
    private int errCode;
    
    public Err() {
        super();
        errCode = DefFail;
    }
    
    public Err(int errCode) {
        super();
        this.errCode = errCode;
    }
    
    public Err(String msg) {
        super(msg);
        errCode = DefFail;
    }
    
    public Err(int errCode, String msg) {
        super(msg);
        this.errCode = errCode;
    }
    
    public Err(String msg, Throwable e) {
        super(msg, e);
        errCode = DefFail;
    }
    
    public Err(int errCode, String msg, Throwable e) {
        super(msg, e);
        this.errCode = errCode;
    }
    
    public int errCode() {
        return errCode;
    }
    
}
