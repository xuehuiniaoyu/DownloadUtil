package file.downloadutil.example;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.nostra13.universalimageloader.cache.disc.naming.Md5FileNameGenerator;
import com.nostra13.universalimageloader.cache.memory.impl.WeakMemoryCache;
import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.ImageLoaderConfiguration;
import com.nostra13.universalimageloader.core.assist.QueueProcessingType;
import com.nostra13.universalimageloader.core.listener.SimpleImageLoadingListener;

import java.io.File;

import file.downloadutil.DownloadInfo;
import file.downloadutil.DownloadManager;
import file.downloadutil.DownloadTask;
import file.downloadutil.Status;

/**
 * Created by Administrator on 2017/10/11 0011.
 */

public class PicDownloadActivity extends Activity/* implements AdapterView.OnItemClickListener*/ {
    private CommonAdapter<Pic> adapter;
    private GridView gridView;

    private DownloadManager.OnStateChangeListener onStateChangeListener;
    private DownloadManager downloadManager;
    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            adapter.notifyDataSetChanged();
        }
    };

    private static DisplayImageOptions options;

    // 初始化imageLoader
    public static void initImageLoader(Context context) {
//		int maxWH = ResolutionUtil.dip2px(context, 400);
        ImageLoaderConfiguration config = new ImageLoaderConfiguration.Builder(context)
                .threadPriority(Thread.NORM_PRIORITY - 2)
                .denyCacheImageMultipleSizesInMemory()
                .diskCacheFileNameGenerator(new Md5FileNameGenerator())
                .diskCacheSize(50 * 1024 * 1024) // 50 Mb
                .diskCacheFileCount(100)
                .memoryCache(new WeakMemoryCache())
                .memoryCacheSize(2 * 1024 * 1024)
                .tasksProcessingOrder(QueueProcessingType.LIFO)
//				.memoryCacheExtraOptions(maxWH, maxWH)
//				.writeDebugLogs() // Remove for release app
                .build();
        // Initialize ImageLoader with configuration.
        ImageLoader.getInstance().init(config);

        options = new DisplayImageOptions.Builder()
                .showImageOnLoading(R.drawable.loading)
                .showImageForEmptyUri(R.drawable.empty)
                .showImageOnFail(R.drawable.fail).build();
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.pic_download_layout);
        initImageLoader(this);

        downloadManager = new DownloadManager(new File(getCacheDir(), "pics"), 0);
        downloadManager.setDefaultSplitCount(2);
        downloadManager.registerOnStateChangeListener(onStateChangeListener = new DownloadManager.OnStateChangeListener() {
            @Override
            public void onStateChange(DownloadInfo downloadInfo, DownloadTask task) {
                Log.i(PicDownloadActivity.class.getSimpleName(), "正在下载："+downloadInfo);
                if(downloadInfo.getState() == Status.STATE_SUCCESS) {
                    Log.i(PicDownloadActivity.class.getSimpleName(), "完成下载："+downloadInfo);
                }
                mHandler.sendEmptyMessage(0);
            }
        });

        gridView = (GridView) findViewById(R.id.gridView);
        adapter = new CommonAdapter<Pic>(this) {
            class Holder {
                ImageView icon;
                TextView title;
                ProgressBar seekBar;
            }
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                Holder holder;
                if(convertView == null) {
                    convertView = View.inflate(context, R.layout.pic_download_item_layout, null);
                    holder = new Holder();
                    holder.icon = (ImageView) convertView.findViewById(R.id.icon);
                    holder.title = (TextView) convertView.findViewById(R.id.title);
                    holder.seekBar = (ProgressBar) convertView.findViewById(R.id.seek);
                    convertView.setTag(holder);
                }
                else {
                    holder = (Holder) convertView.getTag();
                }
                final ImageView icon = holder.icon;
                TextView title = holder.title;
                final ProgressBar seekBar = holder.seekBar;
                final Pic pic = getItem(position);
                title.setText(pic.getName());
                if(pic.getBitmap() == null) {
                    if (pic.getState() == Status.STATE_SUCCESS) {
                        Log.i("Adapter", "下载成功:" + pic.getUrl() + "  md5:" + MD5Util.getMd5ByFile(pic.getLocalFile()));

                        ImageLoader.getInstance().displayImage(Uri.fromFile(pic.getLocalFile()).toString(), icon, options, new SimpleImageLoadingListener() {
                            @Override
                            public void onLoadingComplete(String imageUri, View view, Bitmap loadedImage) {
                                if(loadedImage.getWidth() > 0 && loadedImage.getHeight() > 0) {
                                    pic.setBitmap(loadedImage);
                                }
                            }
                        });

                        /*new AsyncTask<String, Void, Bitmap>(){
                            @Override
                            protected Bitmap doInBackground(String... params) {
                                Bitmap bitmap = BitmapFactory.decodeFile(pic.getLocalFile().getAbsolutePath());
                                if(bitmap.getWidth() > 0 && bitmap.getHeight() > 0) {
                                    return bitmap;
                                }
                                return null;
                            }

                            @Override
                            protected void onPostExecute(Bitmap bitmap) {
                                if(bitmap != null) {
                                    pic.setBitmap(bitmap);
                                    icon.setImageBitmap(bitmap);
                                }
                            }
                        }.execute(pic.getLocalFile().getAbsolutePath());*/
                    }
                }
                seekBar.setMax((int) pic.getTotal());
                seekBar.setProgress((int) pic.getProgress());
                return convertView;
            }
        };
        adapter.append(new Pic("https://timgsa.baidu.com/timg?image&quality=80&size=b9999_10000&sec=1507723403902&di=d2e3fdb4ac333beba319c62f849a1b85&imgtype=0&src=http%3A%2F%2Fpic23.nipic.com%2F20120803%2F4756481_192647407130_2.jpg", downloadManager));
        adapter.append(new Pic("https://timgsa.baidu.com/timg?image&quality=80&size=b9999_10000&sec=1507723403901&di=bfde778c2c509c508a82ad0af6f3dcae&imgtype=0&src=http%3A%2F%2Fdown1.sucaitianxia.com%2Fpsd02%2Fpsd170%2Fpsds32766.jpg", downloadManager));
        adapter.append(new Pic("https://timgsa.baidu.com/timg?image&quality=80&size=b9999_10000&sec=1507723403901&di=caeb6c17bff77bef21ca013211b9c90d&imgtype=0&src=http%3A%2F%2Fscimg.jb51.net%2Fallimg%2F140516%2F11-140516155P0264.jpg", downloadManager));
        adapter.append(new Pic("https://timgsa.baidu.com/timg?image&quality=80&size=b9999_10000&sec=1507723403900&di=1c306894722aca0085bbcfca32ff77fc&imgtype=0&src=http%3A%2F%2Fdown1.sucaitianxia.com%2Fpsds2%2Fp66%2Fpsds9521.jpg", downloadManager));
        adapter.append(new Pic("https://timgsa.baidu.com/timg?image&quality=80&size=b9999_10000&sec=1507723403900&di=7a53d97cfb61f5a0195f8658a270f0f9&imgtype=0&src=http%3A%2F%2Fdown1.sucaitianxia.com%2Fpsd02%2Fpsd172%2Fpsds33271.jpg", downloadManager));
        adapter.append(new Pic("https://timgsa.baidu.com/timg?image&quality=80&size=b9999_10000&sec=1507723403899&di=4091b20e58cd8c9ff2cfc94e65ed6840&imgtype=0&src=http%3A%2F%2Fscimg.jb51.net%2Fallimg%2F140328%2F11-14032Q20353342.jpg", downloadManager));
        adapter.append(new Pic("https://timgsa.baidu.com/timg?image&quality=80&size=b9999_10000&sec=1507723403897&di=78ffe759408c6196cfa6ee65341074da&imgtype=0&src=http%3A%2F%2Fdown1.sucaitianxia.com%2Fpsd02%2Fpsd171%2Fpsds33086.jpg", downloadManager));
        adapter.append(new Pic("https://timgsa.baidu.com/timg?image&quality=80&size=b9999_10000&sec=1507723403897&di=480c777f298f99acd44fb5b5bba218e0&imgtype=0&src=http%3A%2F%2Fdown1.sucaitianxia.com%2Fpsd01%2Fpsd120%2Fpsds17272.jpg", downloadManager));
        adapter.append(new Pic("https://timgsa.baidu.com/timg?image&quality=80&size=b9999_10000&sec=1507723403897&di=cae553b0f7d2eedddf8cd6a34cb9cf82&imgtype=0&src=http%3A%2F%2Fscimg.jb51.net%2Fallimg%2F140125%2F2-140125233334L7.jpg", downloadManager));
        adapter.append(new Pic("https://timgsa.baidu.com/timg?image&quality=80&size=b9999_10000&sec=1507723403896&di=fc6340ce9c6e094a42f4c7f25a5de587&imgtype=0&src=http%3A%2F%2Fimg1.cache.netease.com%2Fcatchpic%2FE%2FEC%2FECE77F7F43F9CA9F961E75B0B6815EE7.jpg", downloadManager));
        adapter.append(new Pic("https://timgsa.baidu.com/timg?image&quality=80&size=b9999_10000&sec=1507723403895&di=5b304bcae98b65e9a6eb9e53e651c048&imgtype=0&src=http%3A%2F%2Fimg.sccnn.com%2Fbimg%2F337%2F18656.jpg", downloadManager));
        adapter.append(new Pic("https://timgsa.baidu.com/timg?image&quality=80&size=b9999_10000&sec=1507723403895&di=984dac180acd278e8388534ae1f0f14f&imgtype=0&src=http%3A%2F%2Fpic.58pic.com%2F58pic%2F12%2F56%2F82%2F00258PIChrS.jpg", downloadManager));
        adapter.append(new Pic("https://timgsa.baidu.com/timg?image&quality=80&size=b9999_10000&sec=1507723403894&di=8b960d2e4af97f5495eaa655c16e8359&imgtype=0&src=http%3A%2F%2Fimg14.3lian.com%2F201509%2F09%2Fd999e9547f0a3aded455e740d4cdda3d.jpg", downloadManager));
        adapter.append(new Pic("https://timgsa.baidu.com/timg?image&quality=80&size=b9999_10000&sec=1507723403894&di=15ea5af4f798c84328936d59998faa8e&imgtype=0&src=http%3A%2F%2Fpic29.nipic.com%2F20130531%2F5402938_173908549153_2.jpg", downloadManager));
        adapter.append(new Pic("https://timgsa.baidu.com/timg?image&quality=80&size=b9999_10000&sec=1507723403893&di=5e49eb7a7755e253887474069af131f5&imgtype=0&src=http%3A%2F%2Fpic13.nipic.com%2F20110303%2F479029_092405618197_2.jpg", downloadManager));
        adapter.append(new Pic("https://timgsa.baidu.com/timg?image&quality=80&size=b9999_10000&sec=1507875891123&di=5743eb0309992e33fdbe05617fb7b89a&imgtype=jpg&src=http%3A%2F%2Fimg3.imgtn.bdimg.com%2Fit%2Fu%3D441386944%2C4100867386%26fm%3D214%26gp%3D0.jpg", downloadManager));
        adapter.append(new Pic("https://timgsa.baidu.com/timg?image&quality=80&size=b9999_10000&sec=1507875889836&di=c3667f4b0d0dbe43471ad478f127c857&imgtype=0&src=http%3A%2F%2Fpic.duowan.com%2Fipad%2F1301%2F222045155181%2F222045191992.jpg", downloadManager));
        adapter.append(new Pic("https://timgsa.baidu.com/timg?image&quality=80&size=b9999_10000&sec=1507876033749&di=42aef3504d48829a954dbaabb6909dd8&imgtype=jpg&src=http%3A%2F%2Fimg3.imgtn.bdimg.com%2Fit%2Fu%3D2963700601%2C474660118%26fm%3D214%26gp%3D0.jpg", downloadManager));
        adapter.append(new Pic("https://timgsa.baidu.com/timg?image&quality=80&size=b9999_10000&sec=1507876135149&di=5785c5233db528dde498dfcb15e86cdd&imgtype=0&src=http%3A%2F%2Fpic11.nipic.com%2F20101118%2F67374_112556005445_2.jpg", downloadManager));
        gridView.setAdapter(adapter);

//        gridView.setOnItemClickListener(this);

        for(final Pic pic : adapter.getData()) {
            if(!downloadManager.has(pic.getUrl())) {
                downloadManager.start(pic);
            }
        }

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        downloadManager.unregisterOnStateChangeListener(onStateChangeListener);
        onStateChangeListener = null;
    }

    /*@Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        Pic pic = adapter.getItem(position);
        if(!downloadManager.has(pic.getUrl())) {
            DownloadInfo downloadInfo = downloadManager.getDownloadInfo(pic.getUrl());
            downloadInfo.setFileType(pic.getFileType());
            pic.setDownloadInfo(downloadInfo);
            downloadManager.start(downloadInfo);
        }
    }*/
}
