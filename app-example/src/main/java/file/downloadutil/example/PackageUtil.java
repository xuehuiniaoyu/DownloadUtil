package file.downloadutil.example;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;

import java.io.File;

/**
 * Created by Administrator on 2017/10/12 0012.
 */

public class PackageUtil {
    /**
     * @Description 系统源生安装应用
     * @param
     * @return
     */
    public static void installBySystem(Context context, String path) {

        if (context == null || path == null)
            return;

        chmodPath("777", context.getCacheDir().getAbsolutePath());

        Log.i("PackageUtil", "install apk file path:" + path);

        File f = new File(path);
        System.out.println("length=" + f.length());
        Uri mApkURI = Uri.fromFile(f);

        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.setDataAndType(mApkURI,
                "application/vnd.android.package-archive");
        context.startActivity(intent);
    }

    /** 文件及所在目录加载权限 */
    public static void chmodPath(String permission, String path) {
        try {
            Runtime.getRuntime().exec("chmod -R " + permission +" " + path);
        } catch (Exception e) {
            e.printStackTrace();
            Log.i("PackageUtil", "chmodPath fault1 msg=" + e);
        }
    }
}
