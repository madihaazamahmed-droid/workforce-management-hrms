package com.deepthought.workforce.exception;

public class WorkforceException extends RuntimeException {
    private final String errorCode;
    private final int httpStatus;

    public WorkforceException(String errorCode, String message, int httpStatus) {
        super(message);
        this.errorCode = errorCode;
        this.httpStatus = httpStatus;
    }

    public String getErrorCode() { return errorCode; }
    public int getHttpStatus() { return httpStatus; }

    // 400
    public static WorkforceException badRequest(String errorCode, String message) {
        return new WorkforceException(errorCode, message, 400);
    }

    // 404
    public static WorkforceException notFound(String errorCode, String message) {
        return new WorkforceException(errorCode, message, 404);
    }

    // 409
    public static WorkforceException conflict(String errorCode, String message) {
        return new WorkforceException(errorCode, message, 409);
    }
}
