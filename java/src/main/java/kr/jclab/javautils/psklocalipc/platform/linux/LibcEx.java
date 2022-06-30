package kr.jclab.javautils.psklocalipc.platform.linux;

import com.sun.jna.LastErrorException;
import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.Structure;
import com.sun.jna.platform.unix.LibCAPI;

import java.util.Arrays;

public interface LibcEx extends LibCAPI, Library {
    int AF_UNIX = 1;
    int SOCK_STREAM = 1;
    int PROTOCOL = 0;

    String NAME = "c";

    static LibcEx createInstance() {
        return Native.load(NAME, LibcEx.class);
    }

    /**
     * creates an endpoint for communication and returns a file descriptor that refers to that
     * endpoint. see https://man7.org/linux/man-pages/man2/socket.2.html
     *
     * @param domain domain
     * @param type type
     * @param protocol protocol
     * @return file descriptor
     * @throws LastErrorException if any error occurs
     */
    int socket(int domain, int type, int protocol) throws LastErrorException;

    /**
     * Connect socket
     *
     * @param sockfd file descriptor
     * @param sockaddr socket address
     * @param addrlen address length
     * @return zero on success. -1 on error
     * @throws LastErrorException if error occurs
     */
    int connect(int sockfd, SockAddr sockaddr, int addrlen)
            throws LastErrorException;

    /**
     * Receive a message from a socket
     *
     * @param fd file descriptor
     * @param buffer buffer
     * @param count length
     * @param flags flag. see https://man7.org/linux/man-pages/man2/recvmsg.2.html
     * @return zero on success. -1 on error
     * @throws LastErrorException if error occurs
     */
    int recv(int fd, byte[] buffer, int count, int flags)
            throws LastErrorException;

    /**
     * Send a message to a socket
     *
     * @param fd file descriptor
     * @param buffer buffer
     * @param count length
     * @param flags flag. see https://man7.org/linux/man-pages/man2/sendmsg.2.html
     * @return zero on success. -1 on error
     * @throws LastErrorException if error occurs
     */
    int send(int fd, byte[] buffer, int count, int flags)
            throws LastErrorException;

    /**
     * Close socket
     *
     * @param fd file descriptor
     * @return zero on success. -1 on error
     * @throws LastErrorException if error occurs
     */
    int close(int fd) throws LastErrorException;

    /**
     * return a description of the error code passed in the argument errnum.
     *
     * @param errno error pointer
     * @return error description
     */
    String strerror(int errno);

    /** Socket address */
    class SockAddr extends Structure {
        /** socket family */
        public short sun_family = AF_UNIX;
        /** pathname */
        public byte[] sun_path;

        /**
         * Constructor.
         *
         * @param sunPath path
         */
        public SockAddr(String sunPath) {
            byte[] arr = sunPath.getBytes();
            sun_path = new byte[arr.length + 1];
            System.arraycopy(arr, 0, sun_path, 0, Math.min(sun_path.length - 1, arr.length));
            allocateMemory();
        }

        @Override
        protected java.util.List<String> getFieldOrder() {
            return Arrays.asList("sun_family", "sun_path");
        }
    }
}
