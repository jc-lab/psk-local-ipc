package kr.jclab.javautils.psklocalipc;

import lombok.Getter;

@Getter
@lombok.Builder
public class Message {
    private final int msgType;
    private final byte[] data;
}
