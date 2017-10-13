package file.downloadutil;

import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Created by Administrator on 2017/10/10 0010.
 */

public class HttpUtil {
    static String TAG = HttpUtil.class.getSimpleName();
    static InputStream getDownLoadFileIo(String path, long progress, long size) throws IOException {
        URL url = new URL(path);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setConnectTimeout(25000);
        conn.setReadTimeout(25000);
        conn.setRequestMethod("GET");
        conn.setRequestProperty("Charset", "UTF-8");
        conn.setRequestProperty("Referer", url.toString());
        conn.setRequestProperty("Range", "bytes=" + progress + "-" + size);
        conn.setRequestProperty("Connection", "Keep-Alive");
        return conn.getInputStream();
    }

    static long getLength(String httpFileUrl) throws IOException {
        URL url = new URL(httpFileUrl);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setReadTimeout(25 * 1000);
        String length = connection.getHeaderField("Content-Length"); // 获取文件长度

        LogInfo.i("下载APK前，获取文件真实大小 ResponseCode "+connection.getResponseCode());

        if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
            LogInfo.i("下载APK前，获取文件真实大小失败...");
            throw new IOException("content-length is null");
        }

        if(length == null)
            throw new IOException("content-length is null");
        long len = Long.valueOf(length);
        if(len == 0)
            throw new IOException("content-length is null");
        return len;
    }
}
