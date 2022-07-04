package kr.jclab.javautils.psklocalipc.platform.windows;

import com.sun.jna.LastErrorException;
import com.sun.jna.Memory;
import com.sun.jna.platform.win32.Kernel32;
import com.sun.jna.platform.win32.WinBase;
import com.sun.jna.platform.win32.WinNT;
import com.sun.jna.ptr.IntByReference;
import kr.jclab.javautils.psklocalipc.platform.PathSocketAddress;
import kr.jclab.javautils.psklocalipc.platform.SystemIOException;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicBoolean;

public class NamedPipeSocket extends Socket {
    private final Kernel32Ex kernel32 = Kernel32Ex.INSTANCE;

    private final AtomicBoolean closeLock = new AtomicBoolean();
    private String path = null;
    private WinNT.HANDLE handle = WinNT.INVALID_HANDLE_VALUE;
    private WinNT.HANDLE readerWaitable = null;
    private WinNT.HANDLE writerWaitable = null;

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
            try {
                readerWaitable = kernel32.CreateEvent(null, true, false, null);
                if (readerWaitable == null) {
                    throw new IOException("CreateEvent() failed ");
                }

                writerWaitable = kernel32.CreateEvent(null, true, false, null);
                if (writerWaitable == null) {
                    throw new IOException("CreateEvent() failed ");
                }

                handle = kernel32.CreateFile(
                        pathSocketAddress.getPath(),
                        Kernel32.GENERIC_READ | Kernel32.GENERIC_WRITE,
                        0,
                        null,
                        Kernel32.OPEN_EXISTING,
                        Kernel32.FILE_FLAG_OVERLAPPED,
                        null
                );
                if (WinNT.INVALID_HANDLE_VALUE.equals(handle)) {
                    int code = kernel32.GetLastError();
                    throw new SystemIOException(code, "error " + code);
                }
                connected = true;
            } catch (LastErrorException lee) {
                throw new IOException("native connect() failed : " + formatError(lee));
            }
            is = new WindowsFileInputStream();
            os = new WindowsFileOutputStream();
        } catch (IOException e) {
            if (WinNT.INVALID_HANDLE_VALUE.equals(handle)) {
                kernel32.CloseHandle(handle);
                handle = WinNT.INVALID_HANDLE_VALUE;
            }
            if (readerWaitable != null) {
                kernel32.CloseHandle(readerWaitable);
                readerWaitable = null;
            }
            if (writerWaitable != null) {
                kernel32.CloseHandle(writerWaitable);
                writerWaitable = null;
            }
            throw e;
        }
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
            Memory readBuffer = new Memory(len);

            WinBase.OVERLAPPED olap = new WinBase.OVERLAPPED();
            olap.hEvent = readerWaitable;
            olap.write();

            boolean immediate = kernel32.ReadFile(handle, readBuffer, len, null, olap.getPointer());
            if (!immediate) {
                int lastError = kernel32.GetLastError();
                if (lastError != WinNT.ERROR_IO_PENDING) {
                    throw new SystemIOException(lastError, "ReadFile() failed: " + lastError);
                }
            }

            IntByReference readBytes = new IntByReference();
            if (!kernel32.GetOverlappedResult(handle, olap.getPointer(), readBytes, true)) {
                int lastError = kernel32.GetLastError();
                throw new SystemIOException(lastError, "GetOverlappedResult() failed for read operation: " + lastError);
            }
            int actualLen = readBytes.getValue();

            readBuffer.read(0, bytesEntry, off, actualLen);

            return actualLen;
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
            ByteBuffer writeBuffer = ByteBuffer.wrap(bytesEntry, off, len);

            WinBase.OVERLAPPED olap = new WinBase.OVERLAPPED();
            olap.hEvent = writerWaitable;
            olap.write();

            boolean immediate = kernel32.WriteFile(handle, writeBuffer, len, null, olap.getPointer());
            if (!immediate) {
                int lastError = kernel32.GetLastError();
                if (lastError != WinNT.ERROR_IO_PENDING) {
                    throw new SystemIOException(lastError, "WriteFile() failed: " + lastError);
                }
            }
            IntByReference writtenBytes = new IntByReference();
            if (!kernel32.GetOverlappedResult(handle, olap.getPointer(), writtenBytes, true)) {
                int lastError = kernel32.GetLastError();
                throw new SystemIOException(lastError, "GetOverlappedResult() failed for write operation: " + lastError);
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
