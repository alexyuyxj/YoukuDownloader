package m.flvcd.downloader;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

public class FileListDownloader {
	private String taskTitle;
	private String cacheFolder;
	private int downloadThreadCount;
	private int retryCount;
	private int cacheSize;
	private int connectionTimeout;
	private int readTimeout;
	private OnDownloadProgressListener onDownloadProgressListener;
	private int workingThreadCount;
	
	public FileListDownloader(String title) {
		taskTitle = title;
	}
	
	public void setCacheFolder(String path) {
		cacheFolder = path;
	}
	
	public void setDownloadThreadCount(int count) {
		downloadThreadCount = count;
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
	
	public HashMap<String, String> start(String[] fileList, String localFolder) 
			throws Throwable {
		if (cacheFolder == null || downloadThreadCount <= 0 || cacheSize <= 0) {
			return null;
		}
		
		if (fileList == null || localFolder == null || fileList.length <= 0) {
			return null;
		}
		
		File file = new File(cacheFolder, localFolder);
		file = new File(file, "list");
		if (!file.exists()) {
			file.mkdirs();
		}
		
		final HashMap<String, String> resMap = new HashMap<String, String>();
		ArrayList<String> list = new ArrayList<String>();
		list.addAll(Arrays.asList(fileList));
		final Object lock = new Object();
		final int fileCount = list.size();
		workingThreadCount = downloadThreadCount;
		OnDownloadFinishListener listener = new OnDownloadFinishListener() {
			public void onFinish() {
				synchronized (lock) {
					workingThreadCount--;
					if (workingThreadCount == 0) {
						lock.notifyAll();
					}
				}
			}
		};
		
		synchronized (lock) {
			for (int i = 0; i < downloadThreadCount; i++) {
				String title = taskTitle;
				if (title.length() > 5) {
					title = title.substring(0, 5) + "…";
				}
				DownloaderThread thread = new DownloaderThread(title + " (t-" + (i + 1) + ")");
				thread.setLocalFolder(file.getAbsolutePath());
				thread.setTaskList(list);
				thread.setRetryCount(retryCount);
				thread.setCacheSize(cacheSize);
				thread.setConnectionTimeout(connectionTimeout);
				thread.setReadTimeout(readTimeout);
				thread.setOnDownloadProgressListener(new OnDownloadProgressListener() {
					public void onDownload(String file, String url, int progress) {
						synchronized (resMap) {
							resMap.put(url, "list/" + file);
						}
						if (onDownloadProgressListener != null) {
							onDownloadProgressListener.onDownload(file, url, 100 * resMap.size() / fileCount);
						}
					}
				});
				thread.setOnDownloadFinishListener(listener);
				thread.start();
			}
			
			lock.wait();
			System.out.println("finish!!");
		}
		
		return resMap;
	}
	
}
