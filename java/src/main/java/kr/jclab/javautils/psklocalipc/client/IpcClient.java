package kr.jclab.javautils.psklocalipc.client;

import kr.jclab.javautils.psklocalipc.IpcChannel;
import kr.jclab.javautils.psklocalipc.Message;
import kr.jclab.javautils.psklocalipc.platform.PathSocketAddress;
import kr.jclab.javautils.psklocalipc.platform.PlatformInfo;
import kr.jclab.javautils.psklocalipc.platform.SystemSocketProvider;
import kr.jclab.javautils.psklocalipc.plugins.Plugin;
import org.bouncycastle.tls.PSKTlsClient;
import org.bouncycastle.tls.TlsClient;
import org.bouncycastle.tls.TlsClientProtocol;
import org.bouncycastle.tls.TlsPSKIdentity;
import org.bouncycastle.tls.crypto.impl.bc.BcTlsCrypto;

import java.io.Closeable;
import java.io.IOException;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.security.*;
import java.util.ArrayList;
import java.util.List;

public class IpcClient implements Closeable, IpcChannel {
    public static byte VERSION = 2;

    private final String path;
    private final Socket socket;
    private final List<Plugin> plugins;

    TlsClientProtocol tlsClientProtocol;
    TlsClient tlsClient;

    private int maxMsgSize = 0;

    public IpcClient(
            String path,
            TlsPSKIdentity pskIdentity,
            List<Plugin> plugins
    ) throws IOException {
        this.path = path;
        this.plugins = plugins;
        this.socket = SystemSocketProvider.newInstance();
        this.socket.connect(new PathSocketAddress(path));

        BcTlsCrypto tlsCrypto = new BcTlsCrypto(new SecureRandom());
        this.tlsClient = new PSKTlsClient(tlsCrypto, pskIdentity);
        this.tlsClientProtocol = new TlsClientProtocol(this.socket.getInputStream(), this.socket.getOutputStream());
        this.tlsClientProtocol.connect(this.tlsClient);

        this.handshake();
    }

    @Override
    public void close() throws IOException {
        this.tlsClientProtocol.close();
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String path;
        private String socketDirectory;
        private String name;
        private TlsPSKIdentity pskIdentity;
        private List<Plugin> plugins = new ArrayList<>();

        public Builder path(String path) {
            this.path = path;
            return this;
        }

        public Builder socketDirectory(String socketDirectory) {
            this.socketDirectory = socketDirectory;
            return this;
        }

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder pskIdentity(TlsPSKIdentity pskIdentity) {
            this.pskIdentity = pskIdentity;
            return this;
        }

        public Builder addPlugin(Plugin plugin) {
            this.plugins.add(plugin);
            return this;
        }

        public IpcClient build() throws IOException {
            String path = this.path;
            if (this.path == null) {
                String socketDirectory = this.socketDirectory;
                String suffix = PlatformInfo.IS_WINDOWS ? "" : ".sock";
                if (socketDirectory == null) {
                    socketDirectory = PlatformInfo.IS_WINDOWS ?
                            "\\\\.\\pipe\\" :
                            "/tmp/";
                }
                if (!socketDirectory.endsWith(PlatformInfo.PATH_SEP)) {
                    socketDirectory += PlatformInfo.PATH_SEP;
                }

                path = socketDirectory + name + suffix;
            }
            return new IpcClient(
                    path,
                    this.pskIdentity,
                    this.plugins
            );
        }
    }

    @Override
    public void write(int msgType, byte[] data) throws IOException {
        ByteBuffer sendBuffer = ByteBuffer.allocate(4 + 4 + data.length)
                .order(ByteOrder.BIG_ENDIAN)
                .putInt(4 + data.length)
                .putInt(msgType)
                .put(data);
        sendBuffer.flip();
        tlsClientProtocol.writeApplicationData(sendBuffer.array(), sendBuffer.arrayOffset(), sendBuffer.remaining());
    }

    public Message read() throws IOException {
        byte[] buffer = new byte[4];
        tlsClientProtocol.readApplicationData(buffer, 0, buffer.length);

        int msgLength = ByteBuffer.wrap(buffer)
                .order(ByteOrder.BIG_ENDIAN)
                .getInt();

        buffer = new byte[msgLength];
        tlsClientProtocol.readApplicationData(buffer, 0, buffer.length);

        ByteBuffer receiveBuffer = ByteBuffer.wrap(buffer)
                .order(ByteOrder.BIG_ENDIAN);

        Message.MessageBuilder builder = Message.builder()
                .msgType(receiveBuffer.getInt());

        byte[] data = new byte[receiveBuffer.remaining()];
        receiveBuffer.get(data);
        builder.data(data);

        Message message = builder.build();
        boolean handled = false;
        for (Plugin plugin : plugins) {
            handled = plugin.handleMessage(this, message);
            if (handled) break;
        }

        if (handled) return null;
        return message;
    }

    public <T extends Plugin> T getPlugin(Class<T> clazz) {
        return (T) this.plugins.stream()
                .filter(v -> clazz.isAssignableFrom(v.getClass()))
                .findFirst()
                .get();
    }

    private void handshake() throws IOException {
        byte[] buffer = new byte[8];
        tlsClientProtocol.readApplicationData(buffer, 0, buffer.length);
        ByteBuffer receiveBuffer = ByteBuffer.wrap(buffer)
                .order(ByteOrder.BIG_ENDIAN);

        byte version = receiveBuffer.get();
        receiveBuffer.get(); receiveBuffer.get(); receiveBuffer.get();
        this.maxMsgSize = receiveBuffer.getInt();

        if (version != VERSION) {
            handshakeSendReply(1);
            throw new IOException("server has sent a different version number: " + version);
        }

        handshakeSendReply(0);
    }

    private void handshakeSendReply(int result) throws IOException {
        ByteBuffer buffer = ByteBuffer.allocate(1);
        buffer.put((byte) result);
        buffer.flip();
        tlsClientProtocol.writeApplicationData(buffer.array(), 0, buffer.remaining());
    }
}
