package file.downloadutil.example;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import java.io.File;

import file.downloadutil.DownloadInfo;
import file.downloadutil.DownloadManager;
import file.downloadutil.DownloadTask;
import file.downloadutil.Status;

public class MainActivity extends Activity {
    DownloadManager downloadManager;
    private DownloadManager.OnStateChangeListener onStateChangeListener;
    DownloadInfo downloadInfo;

    private Button btn;


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
        btn = (Button) findViewById(R.id.btn);
        findViewById(R.id.cancel).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                downloadManager.cancel(url);
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
                        downloadManager.resume(downloadInfo.getUrl());
                        break;
                    }
                    case Status.STATE_ERROR: {
                        downloadManager.removeAllFiles(downloadInfo);
                        downloadManager.start(downloadInfo);
                        break;
                    }
                    case Status.STATE_SUCCESS: {
                        PackageUtil.installBySystem(MainActivity.this, downloadInfo.getLocalFile().getAbsolutePath());
                        break;
                    }
                    case Status.STATE_DOWNLOAD: {
                        downloadManager.pause(downloadInfo.getUrl());
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
//        downloadManager.setFileType(".cha");
        // 得到一个已经存在或者不存在的DownloadInfo对象
        downloadInfo = downloadManager.getDownloadInfo(url);
        downloadInfo.setFileType(".apk");


        /****  *****  *****  ****/
        // 我们模拟数据库中存在该下载对象，那么在初始化的时候就要把下载加入到队列中
        downloadManager.addDownloadInfo(downloadInfo);


        // 根据状态显示文字
        mHandler.sendMessage(mHandler.obtainMessage(0, downloadInfo));

        /**
         * 注册下载状态监听器
         */
        downloadManager.registerOnStateChangeListener(onStateChangeListener=new DownloadManager.OnStateChangeListener() {
            @Override
            public void onStateChange(DownloadInfo downloadInfo, DownloadTask task) {
                Log.i("APP_INFO", downloadInfo.getName() + " -> "+downloadInfo.getState() +" e:"+downloadInfo.getException());
                if(downloadInfo.equals(MainActivity.this.downloadInfo)) {
                    if (downloadInfo.getState() == Status.STATE_SUCCESS) {
                        File file = downloadInfo.getLocalFile();
                        Log.i("APP_INFO", "md5 is " + MD5Util.getMd5ByFile(file));
                    }
                    mHandler.sendMessage(mHandler.obtainMessage(0, downloadInfo));
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
                btn.setText("安装");
                break;
            }
            case Status.STATE_START:
            case Status.STATE_RESUME:
            case Status.STATE_DOWNLOAD: {
                btn.setText("暂停");
                break;
            }
            default: {
                btn.setText("下载");
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        downloadManager.unregisterOnStateChangeListener(onStateChangeListener);
    }
}
