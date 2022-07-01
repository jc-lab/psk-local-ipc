package kr.jclab.javautils.psklocalipc.plugins.request;

import java.util.UUID;

public class RejectedMessage extends Exception {
    private final UUID requestId;
    private final byte[] userdata;

    public RejectedMessage(UUID requestId, String message, byte[] userdata) {
        super(message);
        this.requestId = requestId;
        this.userdata = userdata;
    }
}
