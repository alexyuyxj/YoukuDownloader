package m.flvcd.downloader;

import java.io.File;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;

public class DownloaderThread extends Thread {
	private String localFolder;
	private List<String> taskList;
	private int retryCount;
	private int cacheSize;
	private int connectionTimeout;
	private int readTimeout;
	private OnDownloadProgressListener onDownloadProgressListener;
	private OnDownloadFinishListener onDownloadFinishListener;

	public DownloaderThread(String name) {
		super(name);
	}
	
	public void setLocalFolder(String path) {
		localFolder = path;
	}
	
	public void setTaskList(List<String> list) {
		taskList = list;
	}
	
	public void setRetryCount(int count) {
		retryCount = count;
	}
	
	public void setCacheSize(int size) {
		cacheSize = size;
	}
	
	public void setConnectionTimeout(int timeoutSec) {
		connectionTimeout = timeoutSec;
	}
	
	public void setReadTimeout(int timeoutSec) {
		readTimeout = timeoutSec;
	}
	
	public void setOnDownloadProgressListener(OnDownloadProgressListener listener) {
		onDownloadProgressListener = listener;
	}
	
	public void setOnDownloadFinishListener(OnDownloadFinishListener listener) {
		onDownloadFinishListener = listener;
	}
	
	public void run() {
		while (true) {
			String url = null;
			synchronized (taskList) {
				if (taskList.isEmpty()) {
					onDownloadFinishListener.onFinish();
					break;
				} else {
					url = taskList.remove(0);
				}
			}
			
			if (url != null) {
				String fileName = System.currentTimeMillis() + ".ts";
				int curRetryLeft = retryCount >= 0 ? (retryCount + 1) : Integer.MAX_VALUE;
				boolean comp = false;
				while (curRetryLeft > 0) {
					try {
						download(url, fileName);
						if (onDownloadProgressListener != null) {
							onDownloadProgressListener.onDownload(fileName, url);
						}
						comp = true;
						System.out.println("retry count: " + (retryCount - curRetryLeft + 1));
						break;
					} catch (Throwable t) {
						t.printStackTrace();
						curRetryLeft--;
					}
				}
				
				if (!comp) {
					System.err.println("download failed!!");
				}
			}
		}
	}
	
	private void download(String url, String fileName) throws Throwable {
		File file = new File(localFolder, fileName);
		long range = 0;
		if (file.exists()) {
			range = file.length();
		}
		
		HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
		if (connectionTimeout > 0) {
			conn.setConnectTimeout(connectionTimeout * 1000);
		}
		if (readTimeout > 0) {
			conn.setReadTimeout(readTimeout * 1000);
		}
		if (range > 0) {
			conn.setRequestProperty("RANGE", "bytes=" + range);
		}
		conn.connect();
		
		RandomAccessFile raf = null;
		if (conn.getResponseCode() == HttpURLConnection.HTTP_OK) {
			raf = new RandomAccessFile(file, "rw");
		} else if (conn.getResponseCode() == HttpURLConnection.HTTP_PARTIAL) {
			raf = new RandomAccessFile(file, "rw");
			raf.seek(range);
		} else {
			String msg = "Response Code = " + conn.getResponseCode();
			conn.disconnect();
			throw new RuntimeException(msg);
		}
		
		InputStream is = conn.getInputStream();
		byte[] buf = new byte[cacheSize];
		int len = is.read(buf);
		while (len > 0) {
			raf.write(buf, 0, len);
			len = is.read(buf);
		}
		is.close();
		raf.close();
		conn.disconnect();
	}
	
}
