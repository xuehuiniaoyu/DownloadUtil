package file.downloadutil;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.lang.reflect.Field;
import java.text.MessageFormat;

/**
 * Created by Administrator on 2017/10/10 0010.
 */

public class DownloadInfo {
    private int state = Status.STATE_DEFAULT;

    private String name;
    /** 下载地址 **/
    private String url;

    /** 下载工作空间 **/
    private File workspace;

    /** 本地源文件 **/
    private File localFile;

    /** 状态文件 **/
    private File localStateFile;

    // 进度
    private long progress;
    // 总大小
    private long total;

    private Exception exception;

    private RandomAccessFile localStateRandomAccessFile;
    private boolean over;

    /**
     * 文件类型
     */
    private String fileType;

    public DownloadInfo() {
    }

    public DownloadInfo(File workspace, String name, String url) {
        this.workspace = new File(workspace, this.name = name);
        if(!this.workspace.exists()) {
            this.workspace.mkdirs();
        }
        this.url = url;
        try {
            localStateRandomAccessFile = new RandomAccessFile(getLocalStateFile(), "rw");
            state = localStateRandomAccessFile.readInt();
        } catch (FileNotFoundException e) {
            // 文件不存在
        } catch (IOException e) {
            // 可能是第一次创建
        }
    }

    public DownloadInfo(DownloadInfo downloadInfo) {
        loadFrom(downloadInfo);
    }

    public void loadFrom(DownloadInfo downloadInfo) {
        for(Field field : DownloadInfo.class.getDeclaredFields()) {
            field.setAccessible(true);
            try {
                field.set(this, field.get(downloadInfo));
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        }
    }

    public File getLocalFile() {
        if(localFile != null)
            return localFile;
        return localFile = new File(workspace, "______file" + fileType);
    }

    public File getLocalStateFile() {
        if(localStateFile != null)
            return localStateFile;
        return localStateFile = new File(workspace, "______file.state");
    }

    public String getName() {
        return name;
    }

    public String getUrl() {
        return url;
    }

    public int getState() {
        return state;
    }

    public void setState(int state) {
        this.state = state;

        // 保存状态
        if(localStateRandomAccessFile == null) {
            try {
                localStateRandomAccessFile = new RandomAccessFile(getLocalStateFile(), "rw");
            } catch (FileNotFoundException e) {
                // 文件不存在
            }
        }

        try {
            localStateRandomAccessFile.seek(0);
            localStateRandomAccessFile.writeInt(this.state);
        } catch (Exception e) {

        } finally {
            if(over) {
                try {
                    localStateRandomAccessFile.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            localStateRandomAccessFile = null;
        }
    }

    public File getWorkspace() {
        return workspace;
    }

    public long getProgress() {
        return progress;
    }

    public void setProgress(long progress) {
        this.progress = progress;
    }

    public long getTotal() {
        return total;
    }

    public void setTotal(long total) {
        this.total = total;
    }

    public Exception getException() {
        return exception;
    }

    public void setException(Exception exception) {
        this.exception = exception;
    }

    public String getFileType() {
        return fileType;
    }

    public void setFileType(String fileType) {
        this.fileType = fileType;
    }

    @Override
    public String toString() {
        return MessageFormat.format("[{0} - {1} - {2} ------- {3}]", name, progress, total, state);
    }

    /**
     * 生命历程的结束
     */
    public void over() {
        over = true;
    }

    public int getSplitCount(int defaultSplitCount) {
        int splitCount = 0;
        try {
            for (File file : workspace.listFiles()) {
                if (file.getName().contains(".mk")) {
                    splitCount += 1;
                }
            }
        } catch (Exception e) {} if (splitCount == 0) {
            splitCount = defaultSplitCount;
        }
        return splitCount;
    }

    public interface OnStateChangeListener {
        void onStateChange(DownloadInfo downloadInfo);
    }
}
