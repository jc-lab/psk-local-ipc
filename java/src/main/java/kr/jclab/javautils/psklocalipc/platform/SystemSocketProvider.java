package kr.jclab.javautils.psklocalipc.platform;

import kr.jclab.javautils.psklocalipc.platform.linux.UnixDomainSocket;
import kr.jclab.javautils.psklocalipc.platform.windows.NamedPipeSocket;

import java.io.IOException;
import java.net.Socket;

public abstract class SystemSocketProvider {
    public static Socket newInstance() throws IOException {
        if (PlatformInfo.IS_WINDOWS) {
            return new NamedPipeSocket();
        } else {
            return new UnixDomainSocket();
        }
    }
}
