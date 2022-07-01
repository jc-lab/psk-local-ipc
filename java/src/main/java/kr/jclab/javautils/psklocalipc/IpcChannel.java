package kr.jclab.javautils.psklocalipc;

import java.io.IOException;

public interface IpcChannel {
    void write(int msgType, byte[] data) throws IOException;
}
