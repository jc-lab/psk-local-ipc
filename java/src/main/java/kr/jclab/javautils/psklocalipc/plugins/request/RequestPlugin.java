package kr.jclab.javautils.psklocalipc.plugins.request;

import kr.jclab.javautils.psklocalipc.IpcChannel;
import kr.jclab.javautils.psklocalipc.Message;
import kr.jclab.javautils.psklocalipc.plugins.Plugin;

import java.io.IOException;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class RequestPlugin implements Plugin {
    private static final int cMsgTypeRequest  = 0x80010001;
    private static final int cMsgTypeResolved = 0x80010002;
    private static final int cMsgTypeRejected = 0x80010003;

    private final ScheduledExecutorService scheduledExecutorService;
    private final RequestHandler requestHandler;

    private final ConcurrentHashMap<UUID, RequesterContext> contextMap = new ConcurrentHashMap<>();

    public RequestPlugin(ScheduledExecutorService scheduledExecutorService, RequestHandler requestHandler) {
        this.scheduledExecutorService = scheduledExecutorService;
        this.requestHandler = requestHandler;
    }

    public RequestFuture request(IpcChannel ipcChannel, String method, byte[] userdata, int timeout, TimeUnit timeUnit) throws IOException {
        UUID requestId = UUID.randomUUID();
        RequesterContext.RequesterContextBuilder requesterContextBuilder = RequesterContext.builder()
                .requestId(requestId)
                .ipcChannel(ipcChannel);
        RequesterContext requesterContext = requesterContextBuilder.build();

        byte[] bMethod = method.getBytes(StandardCharsets.UTF_8);
        ByteBuffer payloadBuffer = ByteBuffer.allocate(18 + bMethod.length + userdata.length)
                .order(ByteOrder.BIG_ENDIAN);
        writeUUID(payloadBuffer, requestId);
        payloadBuffer.putShort((short) bMethod.length);
        payloadBuffer.put(bMethod);
        payloadBuffer.put(userdata);
        ((Buffer)payloadBuffer).flip();

        contextMap.put(requestId, requesterContext);
        this.scheduledExecutorService.schedule(() -> {
            contextMap.remove(requestId);
            requesterContext.cancel(false);
        }, timeout, timeUnit);

        ipcChannel.write(cMsgTypeRequest, payloadBuffer.array());

        return requesterContext;
    }

    @Override
    public boolean handleMessage(IpcChannel ipcChannel, Message message) {
        int msgType = message.getMsgType();

        if ((msgType & 0xffff0000) != 0x80010000) {
            return false;
        }

        ByteBuffer byteBuffer = ByteBuffer.wrap(message.getData())
                .order(ByteOrder.BIG_ENDIAN);

        byte[] requestIdBytes = new byte[16];
        byteBuffer.get(requestIdBytes);
        UUID requestId = convertBytesToUUID(requestIdBytes);

        switch (msgType) {
            case cMsgTypeRequest:
                this.handleRequest(ipcChannel, requestId, byteBuffer);
                break;
            case cMsgTypeResolved:
                this.handleResolved(ipcChannel, requestId, byteBuffer);
                break;
            case cMsgTypeRejected:
                this.handleRejected(ipcChannel, requestId, byteBuffer);
                break;
            default:
                return false;
        }
        return true;
    }

    private void handleRequest(IpcChannel ipcChannel, UUID requestId, ByteBuffer reader) {
        RequestContext.RequestContextBuilder requestContextBuilder = RequestContext.builder()
                .requestId(requestId)
                .ipcChannel(ipcChannel);

        int methodLength = reader.getShort();
        byte[] buffer;

        buffer = new byte[methodLength];
        reader.get(methodLength);
        requestContextBuilder.method(new String(buffer));

        buffer = new byte[reader.remaining()];
        reader.get(buffer);
        requestContextBuilder.userdata(buffer);
    }

    private void handleResolved(IpcChannel ipcChannel, UUID requestId, ByteBuffer reader) {
        byte[] userdata = new byte[reader.remaining()];
        reader.get(userdata);

        RequesterContext requesterContext = contextMap.remove(requestId);
        if (requesterContext != null) {
            requesterContext.complete(new ResolvedMessage(requestId, userdata));
        }
    }

    private void handleRejected(IpcChannel ipcChannel, UUID requestId, ByteBuffer reader) {
        short messageLength = reader.getShort();
        byte[] message = new byte[messageLength];
        if (messageLength > 0) {
            reader.get(message);
        }
        byte[] userdata = new byte[reader.remaining()];
        reader.get(userdata);

        RequesterContext requesterContext = contextMap.remove(requestId);
        if (requesterContext != null) {
            requesterContext.completeExceptionally(new RejectedMessage(requestId, new String(message), userdata));
        }
    }

    public static UUID convertBytesToUUID(byte[] bytes) {
        ByteBuffer byteBuffer = ByteBuffer.wrap(bytes);
        long high = byteBuffer.getLong();
        long low = byteBuffer.getLong();
        return new UUID(high, low);
    }

    public static void writeUUID(ByteBuffer buffer, UUID uuid) {
        buffer.putLong(uuid.getMostSignificantBits());
        buffer.putLong(uuid.getLeastSignificantBits());
    }
}
