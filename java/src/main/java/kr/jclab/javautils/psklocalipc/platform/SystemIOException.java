package kr.jclab.javautils.psklocalipc.platform;

import java.io.IOException;

public class SystemIOException extends IOException {
    private final int code;

    public SystemIOException(int code) {
        this.code = code;
    }

    public SystemIOException(int code, String message) {
        super(message);
        this.code = code;
    }

    public SystemIOException(int code, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
    }

    public SystemIOException(int code, Throwable cause) {
        super(cause);
        this.code = code;
    }
}
