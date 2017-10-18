package file.downloadutil;

import java.util.Observable;

/**
 * Created by Administrator on 2017/10/18 0018.
 */

public class LoudspeakerObserable extends Observable {
    /**
     * 扬声器开关，如果打开就可以通知消息，关闭则收不到任何通知。
     */
    public boolean loudspeakerIsOpen = true;

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

    void send(DownloadInfo downloadInfo, DownloadManager downloadManager) {
        if(downloadInfo != null) {
            ObserverEntity observerEntity = new ObserverEntity();
            observerEntity.downloadInfo = downloadInfo;
            observerEntity.manager = downloadManager;
            send(observerEntity);
        }
    }
}
