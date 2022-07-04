package kr.jclab.javautils.psklocalipc.platform.windows;

import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.platform.win32.Kernel32;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.win32.W32APIOptions;

import java.nio.ByteBuffer;

public interface Kernel32Ex extends Kernel32 {
    Kernel32Ex INSTANCE = Native.load("kernel32", Kernel32Ex.class, W32APIOptions.DEFAULT_OPTIONS);

    boolean GetOverlappedResult(
            HANDLE hFile,
            Pointer lpOverlapped,
            IntByReference lpNumberOfBytesTransferred,
            boolean wait
    );
    boolean ReadFile(
            HANDLE hFile,
            Pointer lpBuffer,
            int nNumberOfBytesToRead,
            IntByReference lpNumberOfBytesRead,
            Pointer lpOverlapped
    );

    boolean WriteFile(
            HANDLE hFile,
            ByteBuffer lpBuffer,
            int nNumberOfBytesToWrite,
            IntByReference lpNumberOfBytesWritten,
            Pointer lpOverlapped
    );
}
