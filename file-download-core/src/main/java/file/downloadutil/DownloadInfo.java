package file.downloadutil;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.lang.reflect.Field;
import java.text.MessageFormat;

/**
 * Created by Administrator on 2017/10/10 0010.
 */

public class DownloadInfo {
    int state = Status.STATE_DEFAULT;

    private String name;
    /** 下载地址 **/
    String url;

    /** 下载工作空间 **/
    private File workspace;

    /** 本地源文件 **/
    private File localFile;

    // 进度
    private long progress;
    // 总大小
    long total;

    // 下载线程数
    int splitCount;

    private Exception exception;

    // 进度文件
    private ScheduleFile scheduleFile;

    /**
     * 文件类型
     */
    private String fileType;

    public DownloadInfo() {
    }

    public DownloadInfo(File workspace, String name) {
        this(workspace, name, null);
    }

    public DownloadInfo(File workspace, String name, String url) {
        this.workspace = new File(workspace, this.name = name);
        if(!this.workspace.exists()) {
            this.workspace.mkdirs();
        }
        this.url = url;
        init();
    }

    void init() {
        // 从本地加载进度
        loadProgress(this.workspace);
        LogInfo.i("progress="+getProgress());
        if(scheduleFile == null || !scheduleFile.exists()) {
            scheduleFile = new ScheduleFile(this.workspace, "______file.sch");
        }
        scheduleFile.get(this);
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

    public String getName() {
        return name;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public int getState() {
        return state;
    }

    public void setState(int state) {
        this.state = state;
        scheduleFile.set(this);
    }

    public ScheduleFile getScheduleFile() {
        return scheduleFile;
    }

    public void setScheduleFile(ScheduleFile scheduleFile) {
        this.scheduleFile = scheduleFile;
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
        scheduleFile.set(this);
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

    public void loadProgress(File workspace) {
        long progress = 0;
        if(workspace.exists()) {
            for (File file : workspace.listFiles()) {
                if (file.getName().contains(".mk")) {
                    RandomAccessFile mkRaf = null;
                    try {
                        mkRaf = new RandomAccessFile(file, "rw");
                        mkRaf.seek(0);
                        progress += mkRaf.readLong();
                    } catch (Exception e) {
                    } finally {
                        if (mkRaf != null) {
                            try {
                                mkRaf.close();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                }
            }
        }
        else {
            workspace.mkdirs();
        }
        this.progress = progress;
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
            splitCount = -1;
        }
        if(splitCount != this.splitCount) {
            for (File file : workspace.listFiles()) {
                if (file.getName().contains(".mk")) {
                    splitCount += 1;
                }
            }
            splitCount = defaultSplitCount;
        }
        return this.splitCount = splitCount;
    }

    public int getSplitCount() {
        return splitCount;
    }
}
