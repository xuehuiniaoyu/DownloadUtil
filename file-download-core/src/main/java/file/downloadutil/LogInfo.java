package file.downloadutil;

import android.util.Log;

/**
 * Created by Administrator on 2017/10/13 0013.
 */

public class LogInfo {
    public static final String LOG_TAG = "LogInfo";
    /**
     * 是否打印开关
     */
    public static boolean debug = true;

    public static final void i(Object log) {
        if(debug) {
            Log.i(LOG_TAG, log + "");
        }
    }

    public static final void d(Object log) {
        if(debug) {
            Log.d(LOG_TAG, log + "");
        }
    }

    public static final void e(Object log) {
        if(debug) {
            Log.e(LOG_TAG, log + "");
        }
    }
}
