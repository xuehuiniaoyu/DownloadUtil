package file.downloadutil.example;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;

import java.io.File;
import java.util.Collection;

import file.downloadutil.DownloadInfo;
import file.downloadutil.DownloadManager;
import file.downloadutil.Status;

public class MainActivity extends Activity {
    DownloadManager downloadManager;
    private DownloadManager.OnStateChangeListener onStateChangeListener;
    DownloadInfo downloadInfo;

    private Button btn;
    private ProgressBar seek;


    final String url = "https://github.com/xuehuiniaoyu/DownloadUtil/raw/master/app_1506583562917.apk";

    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            if(msg.what == 0) {
                DownloadInfo downloadInfo = (DownloadInfo) msg.obj;
                showState(downloadInfo);
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        seek = (ProgressBar) findViewById(R.id.seek);
        btn = (Button) findViewById(R.id.btn);
        findViewById(R.id.cancel).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                downloadManager.cancel(downloadInfo.getName());
            }
        });
        findViewById(R.id.jump).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, PicDownloadActivity.class));
            }
        });


        /**
         * 根据不同状态执行不同的点击事件
         */
        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switch (downloadInfo.getState()) {
                    case Status.STATE_PAUSE: {
                        downloadManager.resume(downloadInfo.getName());
                        break;
                    }
                    case Status.STATE_ERROR: {
                        downloadManager.start(downloadInfo);
                        break;
                    }
                    case Status.STATE_SUCCESS: {
                        PackageUtil.chmodPath("777", downloadInfo.getWorkspace().getAbsolutePath());
                        PackageUtil.installBySystem(MainActivity.this, downloadInfo.getLocalFile());
                        break;
                    }
                    case Status.STATE_DOWNLOAD: {
                        downloadManager.pause(downloadInfo.getName());
                        break;
                    }
                    default: {
                        downloadManager.start(downloadInfo);
                    }
                }
            }
        });


        ///////////////////////////////////////////////////////////////////////////////////////////////////////////
        // 下载初始化部分

        // 创建下载管理对象，并指定下载文件目录workspace
        downloadManager = new DownloadManager(new File(getCacheDir(), "download"));
        downloadManager.setFileType(".apk");
        // 初始化状态

        Collection<DownloadInfo> downloadInfos = downloadManager.loadAllDownloadInfos();
        for(DownloadInfo downloadInfo : downloadInfos) {
            if(downloadInfo.getState() == Status.STATE_DOWNLOAD) {
                downloadInfo.setState(Status.STATE_PAUSE);
            }
//            downloadManager.addDownloadInfo(downloadInfo);
        }

//        downloadManager.setFileType(".cha");
        // 得到一个已经存在或者不存在的DownloadInfo对象
        downloadInfo = downloadManager.getDownloadInfo("download-id", url);
        downloadInfo.setFileType(".apk");
        showState(downloadInfo);


        /****  *****  *****  ****/
        // 我们模拟数据库中存在该下载对象，那么在初始化的时候就要把下载加入到队列中
//        downloadManager.addDownloadInfo(downloadInfo);


        // 根据状态显示文字
        mHandler.sendMessage(mHandler.obtainMessage(0, downloadInfo));

        /**
         * 注册下载状态监听器
         */
        downloadManager.registerOnStateChangeListener(onStateChangeListener=new DownloadManager.OnStateChangeListener() {
            @Override
            public void onStateChange(DownloadInfo downloadInfo, DownloadManager manager) {
                Log.i("APP_INFO", downloadInfo.getName() + " -> "+downloadInfo.getState() +" e:"+downloadInfo.getException());
                if(downloadInfo.equals(MainActivity.this.downloadInfo)) {
                    if (downloadInfo.getState() == Status.STATE_SUCCESS) {
                        File file = downloadInfo.getLocalFile();
                        Log.i("APP_INFO", "md5 is " + MD5Util.getMd5ByFile(file));
                    }
                    mHandler.sendMessage(mHandler.obtainMessage(0, downloadInfo));
                    Log.i("APP_INFO", manager.toString());
                }
            }
        });

        ///////////////////////////////////////////////////////////////////////////////////////////////////////////
    }

    /**
     * 根据不同状态显示文字
     * @param downloadInfo
     */
    private void showState(DownloadInfo downloadInfo) {
        switch (downloadInfo.getState()) {
            case Status.STATE_PAUSE: {
                btn.setText("继续下载");
                break;
            }
            case Status.STATE_ERROR: {
                btn.setText("重新下载");
                break;
            }
            case Status.STATE_SUCCESS: {
                if("bc415853dbb22a307a2fb566890b5abe".equals(MD5Util.getMd5ByFile(downloadInfo.getLocalFile()))) {
                    btn.setText("安装");
                }
                else{
                    btn.setText("MD5校验失败");
                }
                break;
            }
            case Status.STATE_START:
            case Status.STATE_RESUME: {
                btn.setText("等待下载...");
                break;
            }
            case Status.STATE_DOWNLOAD: {
                btn.setText("暂停");
                break;
            }

            case Status.PAUSE: {
                btn.setText("暂停中...");
                break;
            }
            case Status.CANCEL: {
                btn.setText("取消中...");
                break;
            }

            default: {
                btn.setText("下载");
            }
        }
        seek.setProgress((int) downloadInfo.getProgress());
        seek.setMax((int) downloadInfo.getTotal());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        downloadManager.unregisterOnStateChangeListener(onStateChangeListener);
    }
}
