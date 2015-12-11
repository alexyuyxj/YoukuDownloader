package m.flvcd;

import java.util.ArrayList;

import m.flvcd.downloader.OnDownloadProgressListener;
import m.flvcd.downloader.VideoDownloader;
import android.app.Service;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Handler.Callback;
import android.widget.Toast;

public class DownloadService extends Service implements Callback, OnDownloadProgressListener {
	private static final int MSG_STARTING = 1;
	private static final int MSG_DOWNLOAD = 2;
	private static final int MSG_FINISH = 3;
	
	private Handler handler;
	private ArrayList<String> taskList;
	
	public void onCreate() {
		handler = new Handler(this);
		taskList = new ArrayList<String>();
	}
	
	public IBinder onBind(Intent intent) {
		return null;
	}
	
	public int onStartCommand(Intent intent, int flags, int startId) {
		String title = intent.getStringExtra("title");
		String url = intent.getStringExtra("url");
		newTask(title, url);
		return START_STICKY;
	}

	private void newTask(final String title, final String url) {
		new Thread(title) {
			public void run() {
				synchronized (taskList) {
					taskList.add(title);
				}
				handler.sendEmptyMessage(MSG_STARTING);
				VideoDownloader.download(title, url, DownloadService.this);
				handler.sendEmptyMessage(MSG_FINISH);
				synchronized (taskList) {
					taskList.remove(title);
					if (taskList.isEmpty()) {
						stopSelf();
					}
				}
			}
		}.start();
	}

	public void onDownload(final String file, String url, int progress) {
		Message msg = new Message();
		msg.what = MSG_DOWNLOAD;
		msg.arg1 = progress;
		msg.obj = file;
		handler.sendMessage(msg);
	}
	
	public boolean handleMessage(Message msg) {
		switch (msg.what) {
			case MSG_STARTING: {
				Toast.makeText(this, "starting...", Toast.LENGTH_SHORT).show();
			} break;
			case MSG_FINISH: {
				Toast.makeText(this, "finished!", Toast.LENGTH_SHORT).show();
			} break;
			case MSG_DOWNLOAD: {
				Toast.makeText(this, msg.obj + " (" + msg.arg1 + "%)", Toast.LENGTH_SHORT).show();
			} break;
		}
		return false;
	}
	
}
