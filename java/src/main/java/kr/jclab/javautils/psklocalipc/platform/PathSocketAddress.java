package kr.jclab.javautils.psklocalipc.platform;

import java.net.SocketAddress;

public class PathSocketAddress extends SocketAddress {
    private final String path;

    public PathSocketAddress(String path) {
        this.path = path;
    }

    public String getPath() {
        return path;
    }
}
