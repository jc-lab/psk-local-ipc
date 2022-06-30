package kr.jclab.javautils.psklocalipc.platform.linux;

import com.sun.jna.LastErrorException;
import com.sun.jna.Native;
import com.sun.jna.Platform;
import kr.jclab.javautils.psklocalipc.platform.PathSocketAddress;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.SocketAddress;
import java.util.concurrent.atomic.AtomicBoolean;

public class UnixDomainSocket extends Socket {
    private final LibcEx libc = LibcEx.createInstance();

    private static final int AF_UNIX = 1;
    private static final int SOCK_STREAM = 1;
    private static final int PROTOCOL = 0;

    private final AtomicBoolean closeLock = new AtomicBoolean();
    private LibcEx.SockAddr sockaddr = null;
    private final int fd;
    private InputStream is;
    private OutputStream os;
    private boolean connected;

    /**
     * Constructor
     *
     * @throws IOException if any error occurs
     */
    public UnixDomainSocket() throws IOException {
        closeLock.set(false);
        try {
            fd = libc.socket(AF_UNIX, SOCK_STREAM, PROTOCOL);
        } catch (LastErrorException lee) {
            throw new IOException("native socket() failed : " + formatError(lee));
        }
    }

    private String formatError(LastErrorException lee) {
        try {
            return libc.strerror(lee.getErrorCode());
        } catch (Throwable t) {
            return lee.getMessage();
        }
    }

    @Override
    public boolean isConnected() {
        return connected;
    }

    @Override
    public void close() throws IOException {
        if (!closeLock.getAndSet(true)) {
            try {
                libc.close(fd);
            } catch (LastErrorException lee) {
                throw new IOException("native close() failed : " + formatError(lee));
            }
            connected = false;
        }
    }

    @Override
    public void connect(SocketAddress endpoint, int timeout) throws IOException {
        PathSocketAddress pathSocketAddress = (PathSocketAddress) endpoint;
        LibcEx.SockAddr sockAddr = new LibcEx.SockAddr(pathSocketAddress.getPath());
        this.sockaddr = sockAddr;
        try {
            int ret = libc.connect(fd, sockAddr, sockAddr.size());
            if (ret != 0) {
                throw new IOException(libc.strerror(Native.getLastError()));
            }
            connected = true;
        } catch (LastErrorException lee) {
            throw new IOException("native connect() failed : " + formatError(lee));
        }
        is = new UnixSocketInputStream();
        os = new UnixSocketOutputStream();
    }

    @Override
    public InputStream getInputStream() {
        return is;
    }

    @Override
    public OutputStream getOutputStream() {
        return os;
    }

    @Override
    public void setTcpNoDelay(boolean b) {
        // do nothing
    }

    @Override
    public void setKeepAlive(boolean b) {
        // do nothing
    }

    @Override
    public void setSoLinger(boolean b, int i) {
        // do nothing
    }

    @Override
    public void setSoTimeout(int timeout) {
        // do nothing
    }

    @Override
    public void shutdownInput() {
        // do nothing
    }

    @Override
    public void shutdownOutput() {
        // do nothing
    }

    class UnixSocketInputStream extends InputStream {

        @Override
        public int read(byte[] bytesEntry, int off, int len) throws IOException {
            try {
                return libc.recv(fd, bytesEntry, len, 0);
            } catch (LastErrorException lee) {
                throw new IOException("native read() failed : " + formatError(lee));
            }
        }

        @Override
        public int read() throws IOException {
            byte[] bytes = new byte[1];
            int bytesRead = read(bytes);
            if (bytesRead == 0) {
                return -1;
            }
            return bytes[0] & 0xff;
        }

        @Override
        public int read(byte[] bytes) throws IOException {
            return read(bytes, 0, bytes.length);
        }
    }

    class UnixSocketOutputStream extends OutputStream {

        @Override
        public void write(byte[] bytesEntry, int off, int len) throws IOException {
            int bytes;
            try {
                bytes = libc.send(fd, bytesEntry, len, 0);

                if (bytes != len) {
                    throw new IOException("can't write " + len + "bytes");
                }
            } catch (LastErrorException lee) {
                throw new IOException("native write() failed : " + formatError(lee));
            }
        }

        @Override
        public void write(int value) throws IOException {}
    }
}
