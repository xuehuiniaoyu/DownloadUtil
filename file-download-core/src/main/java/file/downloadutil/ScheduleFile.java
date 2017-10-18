package file.downloadutil;

import android.support.annotation.NonNull;
import android.util.Log;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.URI;

/**
 * Created by Administrator on 2017/10/18 0018.
 * 存储按顺序
 * 0. 下载线程数
 * 1. 下载总大小
 * 2. 下载状态
 * 3. 第一次写入时间
 * 4. 最后一次写入时间
 * 5. 下载地址
 */

public class ScheduleFile extends File {

    public final static String TAG = ScheduleFile.class.getSimpleName();
    /**
     * 下载对象
     */
    private long firstWriteTime;
    private long lastWriteTime;

    public ScheduleFile(@NonNull String pathname) {
        super(pathname);
    }

    public ScheduleFile(String parent, @NonNull String child) {
        super(parent, child);
    }

    public ScheduleFile(File parent, @NonNull String child) {
        super(parent, child);
    }

    public ScheduleFile(@NonNull URI uri) {
        super(uri);
    }


    public boolean set(DownloadInfo downloadInfo) {
        if(downloadInfo.getUrl() != null) {
            try {
                RandomAccessFile raf = new RandomAccessFile(this, "rw");
                raf.seek(0);
                raf.writeInt(downloadInfo.getSplitCount());
                raf.writeLong(downloadInfo.getTotal());
                raf.writeInt(downloadInfo.getState());
                if (firstWriteTime == 0) {
                    firstWriteTime = System.currentTimeMillis();
                }
                raf.writeLong(firstWriteTime);
                raf.writeLong(lastWriteTime = System.currentTimeMillis());
                raf.writeBytes(downloadInfo.getUrl());
                raf.close();
                return true;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return false;
    }

    public boolean get(DownloadInfo downloadInfo) {
        try {
            RandomAccessFile raf = new RandomAccessFile(this, "rw");
            raf.seek(0);
            int splitCount = raf.readInt();
            long total = raf.readLong();
            int state = raf.readInt();
            firstWriteTime = raf.readLong();
            lastWriteTime = raf.readLong();
            String url = raf.readLine();
            raf.close();
            downloadInfo.splitCount = splitCount;
            downloadInfo.url = url;
            downloadInfo.total = total;
            downloadInfo.state = state;
            return true;
        }
        catch (IOException e) {
            Log.i(TAG, "已经到结尾");
        }
        return false;
    }

    public long getFirstWriteTime() {
        return firstWriteTime;
    }

    public long getLastWriteTime() {
        return lastWriteTime;
    }
}
