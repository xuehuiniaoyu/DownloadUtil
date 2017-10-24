package file.downloadutil.example;

import android.graphics.Bitmap;
import android.util.Log;

import file.downloadutil.DownloadInfo;
import file.downloadutil.DownloadManager;

/**
 * Created by Administrator on 2017/10/11 0011.
 */

public class Pic extends DownloadInfo {
    private Bitmap bitmap;

    public Pic(String url, DownloadManager dm) {
        super();
        DownloadInfo downloadInfo = dm.getDownloadInfo(MD5Util.getMd5ByFile(url), url);
        this.loadFrom(downloadInfo);
        setFileType(url.substring(url.lastIndexOf(".")));
        Log.i("Pic", "fileType is "+getFileType());
    }

    public Bitmap getBitmap() {
        return bitmap;
    }

    public void setBitmap(Bitmap bitmap) {
        this.bitmap = bitmap;
    }
}
