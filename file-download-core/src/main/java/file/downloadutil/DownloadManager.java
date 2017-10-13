package file.downloadutil;

import android.util.Log;

import java.io.File;
import java.io.IOException;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Observable;
import java.util.Observer;

/**
 * Created by Administrator on 2017/10/10 0010.
 */

public class DownloadManager implements DownloadTask.OnResetListener {

    private File workspace;

    // 文件类型
    private String fileType;

    /** 这是一个参考分片数量，如果已经下载过的任务分片数是可知的。 **/
    private int defaultSplitCount = 3;

    /**
     * 同时支持的下载任务
     */
    private List<DownloadTask> downloadTasks = new ArrayList<>();

    /**
     * 等待队列
     */
    private List<DownloadInfo> waitQueue = new ArrayList<>();

    /**
     * 下载队列
     */
    private HashMap<String, DownloadInfo> queue = new HashMap<>();

    public DownloadManager(File workspace) {
        this(workspace, 1000);
    }

    public DownloadManager(File workspace, long notifyIntervalTime) {
        this(workspace, ".source", notifyIntervalTime);
    }

    public DownloadManager(File workspace, String fileType) {
        this(workspace, fileType, 1000);
    }

    public DownloadManager(File workspace, String fileType, long notifyIntervalTime) {
        this.workspace = workspace;
        if(!this.workspace.exists()) {
            this.workspace.mkdirs();
        }
        this.fileType = fileType;
        addDownloadTask(notifyIntervalTime);
    }


    public String getFileType() {
        return fileType;
    }

    public void setFileType(String fileType) {
        this.fileType = fileType;
    }

    public int getDefaultSplitCount() {
        return defaultSplitCount;
    }

    public void setDefaultSplitCount(int defaultSplitCount) {
        this.defaultSplitCount = defaultSplitCount;
    }

    public void addDownloadTask(long notifyIntervalTime) {
        DownloadTask downloadTask = new DownloadTask();
        downloadTask.setNotifyIntervalTime(notifyIntervalTime);
        downloadTask.setOnResetListener(this);
        downloadTasks.add(downloadTask);
    }

    public void start(String url) {
        DownloadInfo downloadInfo = getDownloadInfo(url);
        start(downloadInfo);
    }

    public void start(DownloadInfo downloadInfo) {
        if(!queue.containsKey(downloadInfo.getUrl())) {
            queue.put(downloadInfo.getName(), downloadInfo);
        }
        DownloadTask freeDownloadTask = getFreeDownloadTask();
        if(freeDownloadTask != null) {
            freeDownloadTask.setWorking(true);
            new Thread(new TaskRunner(downloadInfo, Status.START).setDownloadTask(freeDownloadTask)).start();
        }
        else {
            LogInfo.d(downloadInfo + " add to wait queue");
            downloadInfo.setState(Status.START);
            waitQueue.add(downloadInfo);
        }
    }

    public void resume(String url) {
        String name = string_md5(url);
        if(queue.containsKey(name)) {
            DownloadInfo downloadInfo = queue.get(name);
            DownloadTask freeDownloadTask = getFreeDownloadTask();
            if(freeDownloadTask != null) {
                freeDownloadTask.setWorking(true);
                new Thread(new TaskRunner(downloadInfo, Status.RESUME).setDownloadTask(freeDownloadTask)).start();
            }
            else {
                downloadInfo.setState(Status.RESUME);
                waitQueue.add(downloadInfo);
            }
        }
    }

    public void pause(String url) {
        String name = string_md5(url);
        if(queue.containsKey(name)) {
            DownloadInfo downloadInfo = queue.get(name);
            DownloadTask task = getDownloadTask(downloadInfo);
            if(task != null) {
                new Thread(new TaskRunner(downloadInfo, Status.PAUSE)).start();
            }
        }
    }

    public void cancel(String url) {
        String name = string_md5(url);
        if(queue.containsKey(name)) {
            DownloadInfo downloadInfo = queue.get(name);
            new Thread(new TaskRunner(downloadInfo, Status.CANCEL)).start();
            // 如果是已经暂停的下载是收不到CANCEL状态的，所以必须通知一次STATE_CANCEL
            downloadInfo.over();
            downloadInfo.setState(Status.STATE_CANCEL);
            DownloadTask task0 = downloadTasks.get(0);
            task0.send(downloadInfo);
        }
    }

    public String string_md5(String val) {
        try {
            MessageDigest md5 = MessageDigest.getInstance("MD5");
            md5.update(val.getBytes());
            BigInteger mBInteger = new BigInteger(1, md5.digest());
            return mBInteger.toString(16);
        } catch (NoSuchAlgorithmException e) {
            return null;
        }
    }

    private class TaskRunner implements Runnable {
        private DownloadInfo downloadInfo;
        private int state;
        private DownloadTask downloadTask;

        public TaskRunner setDownloadTask(DownloadTask downloadTask) {
            this.downloadTask = downloadTask;
            return this;
        }

        public TaskRunner(DownloadInfo downloadInfo, int state) {
            (this.downloadInfo = downloadInfo).setState(this.state = state);
        }

        @Override
        public void run() {
            switch (state) {
                case Status.START: {
                    downloadInfo.setState(state = Status.STATE_START);
                    File localFile = downloadInfo.getLocalFile();
                    if(!localFile.exists()) {
                        if(!downloadInfo.getWorkspace().exists()) {
                            downloadInfo.getWorkspace().mkdirs();
                        }
                        // 清理目录下的所有文件
                        File parentFile = localFile.getParentFile();
                        for(File file : parentFile.listFiles()) {
                            file.delete();
                        }
                        // 创建新文件
                        try {
                            localFile.createNewFile();
                            LogInfo.d("创建下载文件");
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        if(initTask(downloadInfo, downloadTask)) {
                            downloadInfo.setState(state = Status.STATE_DOWNLOAD);
                            downloadTask.start();
                        }
                        break;
                    }
                }
                case Status.RESUME: {
                    downloadInfo.setState(state = Status.STATE_RESUME);
                    if(initTask(downloadInfo, downloadTask)) {
                        downloadInfo.setState(state = Status.STATE_DOWNLOAD);
                        downloadTask.start();
                    }
                    break;
                }
                case Status.PAUSE: {
                    downloadTask = getDownloadTask(downloadInfo);
                    if(downloadTask != null) {
                        downloadTask.getDownloadInfo().setState(state = Status.STATE_PAUSE);
                    }
                    break;
                }
                case Status.CANCEL: {
                    downloadTask = getDownloadTask(downloadInfo);
                    if(downloadTask != null) {
                        downloadTask.loudspeakerIsOpen = false;
                        downloadTask.getDownloadInfo().setState(state = Status.STATE_CANCEL);
                    }
                    removeAllFiles(downloadInfo);
                    break;
                }
            }
        }
    }

    synchronized DownloadTask getFreeDownloadTask() {
        for (DownloadTask task : downloadTasks) {
            if (!task.isWorking()) {
                return task;
            }
        }
        return null;
    }

    synchronized DownloadTask getDownloadTask(DownloadInfo downloadInfo) {
        for(DownloadTask task : downloadTasks) {
            if(downloadInfo.equals(task.getDownloadInfo())) {
                return task;
            }
        }
        return null;
    }

    /**
     * 删除所有文件
     * @param url
     */
    public void removeAllFiles(String url) {
        String name = string_md5(url);
        removeAllFiles(queue.remove(name));
    }

    /**
     * 删除所有文件
     * @param downloadInfo
     */
    public void removeAllFiles(DownloadInfo downloadInfo) {
        if(downloadInfo != null && queue.containsKey(downloadInfo.getName())) {
            try {
                for (File file : downloadInfo.getWorkspace().listFiles()) {
                    file.delete();
                }
                downloadInfo.getWorkspace().delete();
                LogInfo.d("删除所有文件！");
            } catch (Exception e) {

            }
        }
    }

    boolean initTask(DownloadInfo downloadInfo, DownloadTask downloadTask) {
        LogInfo.d("闲置任务："+downloadTask + " 下载："+downloadInfo);
        downloadTask.setDownloadInfo(downloadInfo);
        long length = 0;
        try { length = HttpUtil.getLength(downloadInfo.getUrl()); } catch (IOException e) {
            // e.printStackTrace();
            downloadInfo.setState(Status.STATE_ERROR);
            downloadInfo.setException(e);
        }
        if(length > 0) {
            downloadInfo.setTotal(length);
            // 获取分片数量
            int splitCount = downloadInfo.getSplitCount(defaultSplitCount);
            LogInfo.d("当前下载被分为：" + splitCount + "片段进行下载");
            for (int i = 0; i < splitCount; i++) {
                long splitSize = length / splitCount;
                long startPosition = splitSize * i;
                long endPosition;
                if (i == splitCount - 1) {
                    endPosition = length;
                } else {
                    endPosition = splitSize * (i + 1) - 1;
                }
                DownloadSplit downloadSplit = new DownloadSplit(i, startPosition, endPosition);
                downloadSplit.setDownloadInfo(downloadInfo);
                downloadSplit.setWorkspace(downloadInfo.getWorkspace());
                downloadSplit.setDownloadTask(downloadTask);
            }
            return true;
        }
        else {
            downloadTask.send(downloadInfo);
            downloadTask.setWorking(false);
            return false;
        }
    }

    @Override
    public void onReset(DownloadTask downloadTask) {
        if(waitQueue.size() > 0) {
            DownloadInfo wait0 = waitQueue.remove(0);
            LogInfo.d("下载等待队列中的第一个:" + wait0);
            downloadTask.setWorking(true);
            new Thread(new TaskRunner(wait0, wait0.getState()).setDownloadTask(downloadTask)).start();
            LogInfo.d("等待队列中的剩余数量为："+waitQueue.size());
        }
        else {
            downloadTask.setWorking(false);
        }
    }

    public static abstract class OnStateChangeListener implements Observer {
        public abstract void onStateChange(DownloadInfo downloadInfo, DownloadTask task);
        @Override
        public void update(Observable o, Object arg) {
            ObserverEntity observerEntity = (ObserverEntity) arg;
            onStateChange(observerEntity.downloadInfo, observerEntity.downloadTask);
        }
    }

    public void registerOnStateChangeListener (OnStateChangeListener onStateChangeListener){
        for(DownloadTask task : downloadTasks) {
            task.addObserver(onStateChangeListener);
        }
    }

    public void unregisterOnStateChangeListener(OnStateChangeListener onStateChangeListener) {
        for(DownloadTask task : downloadTasks) {
            task.deleteObserver(onStateChangeListener);
        }
    }

    /**
     * 是否存在
     * @param url
     * @return
     */
    public boolean has(String url) {
        return queue.containsKey(string_md5(url));
    }

    public DownloadInfo getDownloadInfo(String url) {
        String name = string_md5(url);
        if(queue.containsKey(name)) {
            return queue.get(name);
        }
        DownloadInfo downloadInfo = new DownloadInfo(workspace, name, url);
        downloadInfo.setFileType(fileType);
        return downloadInfo;
    }

    public void addDownloadInfo(DownloadInfo downlaodInfo) {
        if(!queue.containsKey(downlaodInfo)) {
            queue.put(downlaodInfo.getName(), downlaodInfo);
        }
    }
}
