package file.downloadutil;

/**
 * Created by Administrator on 2017/10/10 0010.
 */

public class Status {
    public static final int STATE_DEFAULT = 0;
    public static final int START = 1;
    public static final int PAUSE = 2;
    public static final int RESUME = 3;
    public static final int CANCEL = 4;

    public static final int STATE_START = 6;
    public static final int STATE_DOWNLOAD = 7;
    public static final int STATE_PAUSE = 8;
    public static final int STATE_CANCEL = 9;
    public static final int STATE_RESUME = 10;
    public static final int STATE_ERROR = 11;
    public static final int STATE_SUCCESS = 12;
}
