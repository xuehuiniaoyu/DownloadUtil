package file.downloadutil.example;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.support.v4.content.FileProvider;
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
    public static void installBySystem(Context context, File apkFile) {

        /*if (context == null || path == null)
            return;
        Log.i("PackageUtil", "install apk file path:" + path);

        File f = new File(path);
        System.out.println("length=" + f.length());
        Uri mApkURI = Uri.fromFile(f);

        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.setDataAndType(mApkURI,
                "application/vnd.android.package-archive");
        context.startActivity(intent);*/


        Intent intent = new Intent(Intent.ACTION_VIEW);
        Uri data;
        // 判断版本大于等于7.0
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            // "net.csdn.blog.ruancoder.fileprovider"即是在清单文件中配置的authorities
            data = FileProvider.getUriForFile(context, "file.downloadutil.example.fileprovider", apkFile);
            // 给目标应用一个临时授权
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        } else {
            data = Uri.fromFile(apkFile);
        }
        intent.setDataAndType(data, "application/vnd.android.package-archive");
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
