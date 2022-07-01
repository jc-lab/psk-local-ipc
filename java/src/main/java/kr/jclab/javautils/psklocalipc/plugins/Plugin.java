package kr.jclab.javautils.psklocalipc.plugins;

import kr.jclab.javautils.psklocalipc.IpcChannel;
import kr.jclab.javautils.psklocalipc.Message;

public interface Plugin {
    /**
     * process message
     *
     * @param ipcChannel ipcChannel
     * @param message message
     * @return handled
     */
    boolean handleMessage(IpcChannel ipcChannel, Message message);
}
