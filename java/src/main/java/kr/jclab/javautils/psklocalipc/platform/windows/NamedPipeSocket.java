package kr.jclab.javautils.psklocalipc.platform.windows;

import com.sun.jna.LastErrorException;
import com.sun.jna.platform.win32.Kernel32;
import com.sun.jna.platform.win32.WinNT;
import com.sun.jna.ptr.IntByReference;
import kr.jclab.javautils.psklocalipc.platform.PathSocketAddress;
import kr.jclab.javautils.psklocalipc.platform.SystemIOException;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.SocketAddress;
import java.util.concurrent.atomic.AtomicBoolean;

public class NamedPipeSocket extends Socket {
    private final Kernel32 kernel32 = Kernel32.INSTANCE;

    private final AtomicBoolean closeLock = new AtomicBoolean();
    private String path = null;
    private WinNT.HANDLE handle = WinNT.INVALID_HANDLE_VALUE;
    private InputStream is;
    private OutputStream os;
    private boolean connected;

    /**
     * Constructor
     *
     * @throws IOException if any error occurs
     */
    public NamedPipeSocket() throws IOException {
        closeLock.set(false);
    }

    private String formatError(LastErrorException lee) {
        try {
            return lee.toString();
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
                kernel32.CloseHandle(handle);
                handle = Kernel32.INVALID_HANDLE_VALUE;
            } catch (LastErrorException lee) {
                throw new IOException("native close() failed : " + formatError(lee));
            }
            connected = false;
        }
    }

    @Override
    public void connect(SocketAddress endpoint, int timeout) throws IOException {
        PathSocketAddress pathSocketAddress = (PathSocketAddress) endpoint;
        this.path = pathSocketAddress.getPath();
        try {
            this.handle = kernel32.CreateFile(
                    pathSocketAddress.getPath(),
                    Kernel32.GENERIC_READ | Kernel32.GENERIC_WRITE,
                    0,
                    null,
                    Kernel32.OPEN_EXISTING,
                    0,
                    null
            );
            if (WinNT.INVALID_HANDLE_VALUE.equals(this.handle)) {
                int code = kernel32.GetLastError();
                throw new SystemIOException(code, "error " + code);
            }
            connected = true;
        } catch (LastErrorException lee) {
            throw new IOException("native connect() failed : " + formatError(lee));
        }
        is = new WindowsFileInputStream();
        os = new WindowsFileOutputStream();
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

    class WindowsFileInputStream extends InputStream {
        @Override
        public int read(byte[] bytesEntry, int off, int len) throws IOException {
            IntByReference readBytes = new IntByReference();
            boolean result;
            if (off > 0) {
                byte[] tempBuffer = new byte[len];
                result = kernel32.ReadFile(handle, tempBuffer, len, readBytes, null);
                if (result) {
                    System.arraycopy(tempBuffer, 0, bytesEntry, off, readBytes.getValue());
                }
            } else {
                result = kernel32.ReadFile(handle, bytesEntry, len, readBytes, null);
            }
            if (!result) {
                int code = kernel32.GetLastError();
                throw new SystemIOException(code, "error " + code);
            }
            return readBytes.getValue();
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

    class WindowsFileOutputStream extends OutputStream {
        @Override
        public void write(byte[] bytesEntry, int off, int len) throws IOException {
            IntByReference writtenBytes = new IntByReference();
            boolean result;
            if (off > 0) {
                byte[] tempBuffer = new byte[len];
                System.arraycopy(bytesEntry, off, tempBuffer, 0, len);
                result = kernel32.WriteFile(handle, tempBuffer, len, writtenBytes, null);
            } else {
                result = kernel32.WriteFile(handle, bytesEntry, len, writtenBytes, null);
            }
            if (writtenBytes.getValue() != len) {
                throw new IOException("can't write " + len + "bytes (written=" + writtenBytes.getValue() + " bytes)");
            }
        }

        @Override
        public void write(int value) throws IOException {
            byte[] buffer = new byte[1];
            buffer[0] = (byte)(value & 0xff);
            this.write(buffer, 0, 1);
        }
    }
}
