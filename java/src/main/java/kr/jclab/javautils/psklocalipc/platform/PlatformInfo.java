package kr.jclab.javautils.psklocalipc.platform;

public class PlatformInfo {
    public final static boolean IS_WINDOWS;
    public final static String PATH_SEP;
    static {
        IS_WINDOWS = System.getProperty("os.name").toLowerCase().startsWith("windows");
        if (IS_WINDOWS) {
            PATH_SEP = "\\";
        } else {
            PATH_SEP = "/";
        }
    }
}
