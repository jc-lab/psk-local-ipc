package kr.jclab.javautils.psklocalipc.plugins.request;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;

import java.util.UUID;

@RequiredArgsConstructor
@Getter
@ToString
public class ResolvedMessage {
    private final UUID requestId;
    private final byte[] userdata;
}
