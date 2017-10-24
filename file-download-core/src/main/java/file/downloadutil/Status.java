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

    /** 系统暂停，非人为的，主要是给被优先下载的任务让位 **/
    public static final int STATE_PAUSE_BY_SYS = 13;

    /** 等待 **/
    public static final int STATE_WAIT = 100;
}
