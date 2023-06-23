package com.juanmuscaria.duskers;

public class ReportedException extends Exception {
    private final String header;

    public ReportedException(String header, String message) {
        super(message);
        this.header = header;
    }

    public ReportedException(String header, String message, Throwable cause) {
        super(message, cause);
        this.header = header;
    }

    public ReportedException(String header, String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
        this.header = header;
    }

    public String getHeader() {
        return header;
    }
}
