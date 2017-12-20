package file.downloadutil;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;

/**
 * Created by Administrator on 2017/10/10 0010.
 */

class DownloadSplit implements Runnable {

    /** 被维护的下载对象 **/
    private DownloadInfo downloadInfo;

    /** id 唯一编号，也是分片的下标 **/
    private int id;

    /** 开始读取的位置 **/
    private long startPosition;

    /** 结束读取的位置 **/
    private long endPosition;

    /** 需要下载的长度 **/
    private long length;

    /** 进度游标 **/
    private long cursor;

    /** 工作空间, 文件夹，分片创建的一些文件都被放在该文件夹下。 **/
    private File workspace;

    /** 所属任务 **/
    private DownloadTask downloadTask;

    /**
     * 发生的错误/异常
     */
    private Exception exception;

    /**
     * 下载完成的标记
     */
    private boolean success = false;

    public DownloadSplit(int id, long startPosition, long endPosition) {
        this.id = id;
        this.startPosition = startPosition;
        this.endPosition = endPosition;
        this.length = this.endPosition - this.startPosition;
        LogInfo.d("split"+id+": "+startPosition+" - "+endPosition);
    }

    @Override
    public void run() {
        LogInfo.d(downloadInfo+" 分段"+id+"开始下载");
        File mkFile = new File(workspace, "task-"+id+".mk");
        RandomAccessFile localRaf = null;
        RandomAccessFile mkRaf = null;
        try {
            mkRaf = new RandomAccessFile(mkFile, "rw");
            mkRaf.seek(0);
            cursor = mkRaf.readLong();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
        }
        // 准备下载
        long realPosition = startPosition + cursor;
        LogInfo.d(downloadInfo+ " start:"+startPosition+" cursor:"+cursor+" end:"+endPosition);
        if (realPosition < endPosition) {
            try {
                InputStream ins = HttpUtil.getDownLoadFileIo(downloadInfo.getUrl(), realPosition, endPosition);
                localRaf = new RandomAccessFile(downloadInfo.getLocalFile(), "rw");
                localRaf.seek(realPosition);
                byte[] b = new byte[1024];
                int len;
                boolean alive = true;
                exception = null;
                while (downloadTask.aliveInfo.alive && alive && (len = ins.read(b)) != -1) {
                    if (downloadInfo.getState() == Status.PAUSE || downloadInfo.getState() == Status.CANCEL
                            || downloadInfo.getState() == Status.STATE_PAUSE_BY_SYS) {
                        exception = new PauseException();
                        break;
                    }
                    localRaf.write(b, 0, len);
                    cursor += len;
                    mkRaf.seek(0);
                    mkRaf.writeLong(cursor);
//                    LogInfo.d(">>>>>> " + len+" >>> "+cursor+"/"+length);
                }
                if(exception != null) {
                    LogInfo.d("exception is " + exception.getClass());
                }
                if(exception == null) {
                    success = true;
                }
            } catch (Exception e) {
                downloadInfo.setException(exception = e);
            }
        }
        else {
            success = true;
        }
        if(mkRaf != null) {
            try {
                mkRaf.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        if(localRaf != null) {
            try {
                localRaf.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        // 注销
        LogInfo.d("注销:" + downloadInfo);
        downloadTask.removeDownloadSplit(this);
    }

    public void setWorkspace(File workspace) {
        this.workspace = workspace;
    }

    public void setDownloadInfo(DownloadInfo downloadInfo) {
        this.downloadInfo = downloadInfo;
    }

    public long getCursor() {
        return cursor;
    }

    public void setDownloadTask(DownloadTask downloadTask) {
        if(downloadTask == null) {
            this.downloadTask = null;
        }
        else {
            (this.downloadTask = downloadTask).addDownloadSplit(this);
            LogInfo.d("add to task :" +downloadInfo);
        }
    }

    /**
     * 当前分片在下载中没有出现任何异常并完整的完成了所有下载。
     * @return
     */
    public boolean isSuccess() {
        return success && exception == null;
    }

    public Exception getException() {
        return exception;
    }

    /** 暂停或取消是抛出的异常，用来阻止成功 **/
    public static class PauseException extends Exception {
        @Override
        public String toString() {
            return "user pause or cancel!";
        }
    }
}
