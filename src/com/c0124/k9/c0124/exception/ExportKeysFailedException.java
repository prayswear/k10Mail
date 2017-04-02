package com.c0124.k9.c0124.exception;

/**
 * Created by xinqian on 9/13/15.
 */
public class ExportKeysFailedException extends Exception {
    public static final long serialVersionUID = -1;
    final public Exception m_reason;
    public ExportKeysFailedException(String message, Exception reason) {
        super(message);
        m_reason = reason;
    }
}
