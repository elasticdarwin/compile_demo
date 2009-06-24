package org.world.hello.apps.exceptions;

import java.util.concurrent.atomic.AtomicLong;
import org.world.hello.apps.DynamicRuntime;


/**
 * The super class for all Play! exceptions
 */
public abstract class DynamicRuntimeException extends RuntimeException {

    static AtomicLong atomicLong = new AtomicLong(System.currentTimeMillis());
    String id;

    public DynamicRuntimeException() {
        super();
        setId();
    }

    public DynamicRuntimeException(String message) {
        super(message);
        setId();
    }

    public DynamicRuntimeException(String message, Throwable cause) {
        super(message, cause);
        setId();
    }

    void setId() {
        long nid = atomicLong.incrementAndGet();
        id = Long.toString(nid, 26);
    }

    public abstract String getErrorTitle();

    public abstract String getErrorDescription();

    public boolean isSourceAvailable() {
        return this instanceof SourceAttachment;
    }

    public Integer getLineNumber() {
        return -1;
    }

    public String getSourceFile() {
        return "";
    }

    public String getId() {
        return id;
    }

    public static StackTraceElement getInterestingStrackTraceElement(Throwable cause) {
        for (StackTraceElement stackTraceElement : cause.getStackTrace()) {
            if (stackTraceElement.getLineNumber() > 0 && DynamicRuntime.classes.hasClass(stackTraceElement.getClassName())) {
                return stackTraceElement;
            }
        }
        return null;
    }
}