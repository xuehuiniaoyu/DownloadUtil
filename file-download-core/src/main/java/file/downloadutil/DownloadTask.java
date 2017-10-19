package file.downloadutil;

import android.os.SystemClock;

import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

/**
 * Created by Administrator on 2017/10/10 0010.
 */

class DownloadTask extends LoudspeakerObserable {

    private DownloadManager downloadManager;

    /** 分片集合：一个下载被拆分为n部分去下载，这样做的好处是经可能的使用带宽，这些分片都被记录在 downloadSplits 集合中。 **/
    private List<DownloadSplit> downloadSplits = new Vector<>();
    /** 下载对象 **/
    private DownloadInfo downloadInfo;
    /** 是否在下载中 **/
    private boolean working;
    /** 当下载完成以后，当前DownloadTask对象被闲置，为了能够被再次使用必须调用reset方法进行初始化， onResetListener 就是在初始化时做的一些初始化动作。 **/
    private OnResetListener onResetListener;
    /** 进度监听器，进度监听器主要工作是通知状态。 **/
    private ProgressMonitorThread progressMonitor;

    /** 通知状态间隔时间 **/
    private long notifyIntervalTime = 1000;

    class AliveInfo {
        boolean alive;
    } AliveInfo aliveInfo = new AliveInfo();

    /**
     * 设置状态通知间隔时间
     * @param notifyIntervalTime
     */
    void setNotifyIntervalTime(long notifyIntervalTime) {
        this.notifyIntervalTime = notifyIntervalTime;
    }

    final class ProgressMonitorThread implements Runnable {

        private boolean alive;
        private DownloadInfo downloadInfo;
        private ArrayList<DownloadSplit> downloadSplits;

        ProgressMonitorThread(DownloadInfo downloadInfo) {
            this.downloadInfo = downloadInfo;
            alive = true;
            downloadSplits = new ArrayList<>(DownloadTask.this.downloadSplits);
        }
        @Override
        public void run() {
            while (alive && working) {
                long progress = 0;
                for (DownloadSplit split : downloadSplits) {
                    progress += split.getCursor();
                }
                if(progress > 0) {
                    downloadInfo.setProgress(progress);
                }
                send(downloadInfo, downloadManager);
                if(notifyIntervalTime > 0) {
                    SystemClock.sleep(notifyIntervalTime);
                }
            }
            downloadSplits.clear();
            downloadSplits = null;
            downloadInfo = null;
            LogInfo.d("通知器结束工作" + working + ", "+alive);
        }
    }

    DownloadInfo getDownloadInfo() {
        return downloadInfo;
    }

    void setDownloadInfo(DownloadInfo downloadInfo) {
        loudspeakerIsOpen = true;
        send(this.downloadInfo = downloadInfo, downloadManager);
    }

    public void setDownloadManager(DownloadManager downloadManager) {
        this.downloadManager = downloadManager;
    }

    private void reset() {
        LogInfo.d("reset");
        progressMonitor.alive = false;
        progressMonitor = null;
        send(downloadInfo, downloadManager);
        onResetListener.onReset(this);
    }

    void start() {
        if (progressMonitor == null) {
            new Thread(progressMonitor = new ProgressMonitorThread(downloadInfo)).start();
            LogInfo.d(downloadInfo + " start split" + downloadSplits.size());
            for (DownloadSplit downloadSplit : new ArrayList<>(downloadSplits)) {
                new Thread(downloadSplit).start();
            }
        }
    }

    /**
     * 添加分片
     * @param downloadSplit
     */
    synchronized void addDownloadSplit(DownloadSplit downloadSplit) {
        downloadSplits.add(downloadSplit);
    }

    /**
     * 卸载分片
     * @param downloadSplit
     */
    synchronized void removeDownloadSplit(DownloadSplit downloadSplit) {
        downloadSplit.setDownloadTask(null);
        downloadSplit.setDownloadInfo(null);
        downloadSplits.remove(downloadSplit);
        LogInfo.d("剩余分片数："+downloadSplits.size());
        if(downloadInfo.getState() == Status.STATE_DOWNLOAD) {
            if(!downloadSplit.isSuccess()) {
                downloadInfo.setState(Status.STATE_ERROR);
                aliveInfo.alive = false;
            }
        }
        if(downloadSplits.size() == 0) {
            if(downloadInfo.getState() == Status.STATE_DOWNLOAD) {
                downloadInfo.setState(Status.STATE_SUCCESS);
                downloadInfo.setProgress(downloadInfo.getTotal());
            }
            else if(downloadInfo.getState() == Status.PAUSE) {
                downloadInfo.setState(Status.STATE_PAUSE);
            }
            else if(downloadInfo.getState() == Status.CANCEL) {
                downloadInfo.setState(Status.STATE_CANCEL);
                downloadInfo.setProgress(0);
            }
            reset();
        }
    }

    boolean isWorking() {
        return working;
    }

    void setWorking(boolean working) {
        aliveInfo.alive = (this.working = working);
    }

    /**
     * 初始化时想做的一些事情
     * @param onResetListener
     */
    void setOnResetListener(OnResetListener onResetListener) {
        this.onResetListener = onResetListener;
    }

    interface OnResetListener {
        void onReset(DownloadTask downloadTask);
    }
}
