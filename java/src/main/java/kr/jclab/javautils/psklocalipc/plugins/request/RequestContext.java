package kr.jclab.javautils.psklocalipc.plugins.request;

import kr.jclab.javautils.psklocalipc.IpcChannel;
import lombok.Builder;
import lombok.Getter;

import java.util.UUID;

@Getter
@Builder
public class RequestContext {
    private final IpcChannel ipcChannel;
    private final UUID requestId;
    private final String method;
    private final byte[] userdata;

    public void resolve(byte[] userdata) {

    }

    public void reject(String message, byte[] userdata) {

    }
}
