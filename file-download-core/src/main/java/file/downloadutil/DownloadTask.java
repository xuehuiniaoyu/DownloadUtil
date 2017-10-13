package file.downloadutil;

import android.os.SystemClock;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;
import java.util.Observable;
import java.util.Vector;

/**
 * Created by Administrator on 2017/10/10 0010.
 */

public class DownloadTask extends Observable {

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

    /**
     * 设置状态通知间隔时间
     * @param notifyIntervalTime
     */
    public void setNotifyIntervalTime(long notifyIntervalTime) {
        this.notifyIntervalTime = notifyIntervalTime;
    }

    final class ProgressMonitorThread implements Runnable {

        private boolean alive;
        private DownloadInfo downloadInfo;
        private ArrayList<DownloadSplit> downloadSplits;

        public ProgressMonitorThread(DownloadInfo downloadInfo) {
            this.downloadInfo = downloadInfo;
            alive = true;
            downloadSplits = new ArrayList<>(DownloadTask.this.downloadSplits);
        }
        @Override
        public void run() {
            while (alive && working) {
//                LogInfo.d(this + ", "+alive + ", "+working+" , "+downloadInfo);
                long progress = 0;
                for (DownloadSplit split : downloadSplits) {
                    progress += split.getCursor();
                }
                downloadInfo.setProgress(progress);
                send(downloadInfo);
                if(notifyIntervalTime > 0) {
                    SystemClock.sleep(notifyIntervalTime);
                }
            }
            downloadSplits.clear();
            downloadSplits = null;
            downloadInfo = null;
            LogInfo.d("结束" + working + ", "+alive);
        }
    }

    /**
     * 扬声器开关，如果打开就可以通知消息，关闭则收不到任何通知。
     */
    public boolean loudspeakerIsOpen = true;

    public DownloadInfo getDownloadInfo() {
        return downloadInfo;
    }

    void setDownloadInfo(DownloadInfo downloadInfo) {
        loudspeakerIsOpen = true;
        send(this.downloadInfo = downloadInfo);
    }

    private void reset() {
        LogInfo.d("reset");
        progressMonitor.alive = false;
        progressMonitor = null;
        send(downloadInfo);
        downloadInfo = null;
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
            }
        }
        if(downloadSplits.size() == 0) {
            if(downloadInfo.getState() == Status.STATE_DOWNLOAD) {
                downloadInfo.setState(Status.STATE_SUCCESS);
                downloadInfo.setProgress(downloadInfo.getTotal());
            }
            reset();
        }
    }

    /**
     * 通知状态
     * @param entity
     */
    void send(ObserverEntity entity) {
        if(loudspeakerIsOpen) {
            setChanged();
            notifyObservers(entity);
        }
    }

    public void send(DownloadInfo downloadInfo) {
        ObserverEntity observerEntity  = new ObserverEntity();
        observerEntity.downloadInfo = downloadInfo;
        observerEntity.downloadTask = this;
        send(observerEntity);
    }

    public boolean isWorking() {
        return working;
    }

    void setWorking(boolean working) {
        this.working = working;
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
