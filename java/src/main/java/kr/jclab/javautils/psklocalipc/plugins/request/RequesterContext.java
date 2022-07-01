package kr.jclab.javautils.psklocalipc.plugins.request;

import kr.jclab.javautils.psklocalipc.IpcChannel;
import lombok.Builder;
import lombok.Getter;

import java.util.UUID;

@Builder
@Getter
class RequesterContext extends RequestFuture {
    private final UUID requestId;
    private final IpcChannel ipcChannel;

}
