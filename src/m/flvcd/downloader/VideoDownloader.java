package m.flvcd.downloader;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

import org.json.JSONObject;

import android.util.Base64;

public class VideoDownloader {
	private static final byte[] TRANSFORM_TABLE = {
		63, 121, -44, 54, 86, -68, 114, 15, 108, 94, 77, -15, 89, 46, -81, 4, -114, 69, 
		-88, -79, -26, 91, 50, -19, -37, 38, 27, -80, 7, 32, -64, 127, -41, 27, -49, 
		-89, 3, 42, 52, 29, 86, 122, 6, -35, -110, -1, -57, 41, 52, -13, -73, 10, 48, 
		49, 92, 117, 67, 72, 45, 121, 93, -63, 101, -90, 73, 108, -29, -91, 7, 46, -110, 
		85, 0, 81, 67, 83, 113, 67, 9, -57, 116, -102, -26, 15, 92, -14, -91, 90, 56, 
		-76, 18, 1, 57, 95, -1, 83, 67, -84, 52, 117, -93, 86, 116, -58, 120, -112, 70, 
		-88, -123, -45, -122, 10, 38, 39, -10, -60, -114, 93, 31, 25, 1, -120, -121, 
		-66, -40, 74, -69, 83, 101, -86, 107, 121, -6, 109, 50, 111, -33, 62, 27, -63, 
		-33, 1, 52, 81, 83, 109, -59, 122, 11, -57, -75, 34, 58, 38, -75, -115, 62, -46, 
		7, -114, -60, -20, 55, 4, -107, -110, -62, 103, -21, 40, 56, -62, -110, -91, 
		-64, 53, -69, 123, -87, 66, -67, 57, 91, 74, 82, 13, 14, 109, -77, -108, -28, 
		-78, 103, -85, -37, -47, -33, -33, 97, -103, 102, -96, -78, -116, 57, 55, 91, 
		20, 80, -66, -82, -77, -78, 39, -63, 19, 12, -2, 93, -32, 65, -25, 89, 104, 
		-51, -102, 76, -68, -86, -90, 121, 39, -83, -118, -102, 110, 113, -3, -23, 52, 
		-71, -16, -21, 72, -99, -86, -120, -16, 2, 114, 72, -50, 56, 73, -56, 117
	};
	
	public static String download(String title, String pageUrl, OnDownloadProgressListener listener) {
		try {
			String parseRes = parsePage(pageUrl);
			String xml = decryptResult(parseRes);
			String[] mediaInfo = parseXml(xml);
			URL m3u8Url = new URL(mediaInfo[1]);
			String host = m3u8Url.getProtocol() + "://" + m3u8Url.getHost();
			
			if (host.contains("youku") || host.contains("tudou")) {
				String m3u8 = downloadText(mediaInfo[1]);
				String[] videoList = parseMeu8(m3u8);
				HashMap<String, String> localVideos = downloadVideos(title, videoList, mediaInfo[0], listener);
				return genM3u8(m3u8, localVideos, mediaInfo[0]);
			} else if (host.contains("cntv")) {
				String mainM3u8 = downloadText(mediaInfo[1]);
				String m3u8 = downloadCNTVM3u8(host, mainM3u8);
				String[] videoList = parseMeu8(m3u8);
				HashMap<String, String> localVideos = downloadVideos(title, videoList, mediaInfo[0], listener);
				return genM3u8(m3u8, localVideos, mediaInfo[0]);
			} else if (host.contains("iask")) {
				String[] videoList = new String[] {mediaInfo[1]};
				downloadVideos(title, videoList, mediaInfo[0], listener);
				return (new File("/sdcard/FLVCD/", mediaInfo[0])).getAbsolutePath();
			} else if (host.contains("letv")) {
				String jsonStr = downloadText(mediaInfo[1]);
				JSONObject json = new JSONObject(jsonStr);
				String[] videoList = new String[] {json.optString("location")};
				downloadVideos(title, videoList, mediaInfo[0], listener);
				return (new File("/sdcard/FLVCD/", mediaInfo[0])).getAbsolutePath();
			}
		} catch (Throwable t) {
			t.printStackTrace();
		}
		return null;
	}
	
	private static String parsePage(String pageUrl) throws Throwable {
		String enPageUrl = URLEncoder.encode(pageUrl, "UTF-8");
		String url = "http://m.flvcd.com/parse_m3u8.php?url=" + enPageUrl + "&format=super";
		HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
		conn.connect();
		
		String resp = null;
		if (conn.getResponseCode() == HttpURLConnection.HTTP_OK) {
			InputStream is = conn.getInputStream();
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			byte[] buf = new byte[1024];
			int len = is.read(buf);
			while (len > 0) {
				baos.write(buf, 0, len);
				len = is.read(buf);
			}
			baos.close();
			is.close();
			resp = new String(baos.toByteArray(), "UTF-8");
		}
		conn.disconnect();
		
		return resp;
	}
	
	private static String decryptResult(String parseRes) throws Throwable {
		String resp = null;
		if (parseRes != null) {
			byte[] tmp = new byte[parseRes.length() / 2];
			for (int i = 0; i < tmp.length; i++) {
				int c1 = Character.digit(parseRes.charAt(i * 2), 16) << 4;
				int c2 = Character.digit(parseRes.charAt(i * 2 + 1), 16);
				tmp[i] = (byte) ((c1 | c2) & 0xff);
			}
			
			byte[] base64 = new byte[tmp.length];
			for (int i = 0; i < base64.length; i++) {
				base64[i] = (byte) (TRANSFORM_TABLE[i % 256] ^ tmp[i]);
			}
			resp = new String(Base64.decode(base64, Base64.DEFAULT), "UTF-8");
			System.out.println(resp);
		}
		
		return resp;
	}
	
	private static String[] parseXml(String xml) throws Throwable {
		String[] resp = null;
		if (xml != null) {
			String[] lines = xml.split("\n");
			String name = null;
			String url = null;
			for (String line : lines) {
				if (line.startsWith("<title><![CDATA[") && line.endsWith("]]></title>")) {
					line = line.substring("<title><![CDATA[".length());
					name = line.substring(0, line.length() - "]]></title>".length());
				} else if (line.startsWith("<U><![CDATA[") && line.endsWith("]]></U>")) {
					line = line.substring("<U><![CDATA[".length());
					url = line.substring(0, line.length() - "]]></U>".length());
				}
			}
			
			if (name != null && url != null) {
				resp = new String[] {name, url};
			}
		}
		return resp;
	}
	
	private static String downloadText(String url) throws Throwable {
		String resp = null;
		if (url != null) {
			HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
			conn.connect();
			if (conn.getResponseCode() == HttpURLConnection.HTTP_OK) {
				InputStream is = conn.getInputStream();
				ByteArrayOutputStream baos = new ByteArrayOutputStream();
				byte[] buf = new byte[1024];
				int len = is.read(buf);
				while (len > 0) {
					baos.write(buf, 0, len);
					len = is.read(buf);
				}
				baos.close();
				is.close();
				resp = new String(baos.toByteArray(), "UTF-8");
				System.out.println(resp);
			}
			conn.disconnect();
		}
		return resp;
	}
	
	private static String downloadCNTVM3u8(String host, String m3u8) {
		String[] lines = m3u8.split("\n");
		ArrayList<Integer> list = new ArrayList<Integer>();
		HashMap<Integer, Integer> map = new HashMap<Integer, Integer>();
		for (int i = 0; i < lines.length; i++) {
			if (lines[i].startsWith("#EXT-X-STREAM-INF") && lines[i].contains("RESOLUTION=")) {
				int ind = lines[i].indexOf("RESOLUTION=") + "RESOLUTION=".length();
				String resolution = lines[i].substring(ind);
				String[] ps = resolution.split("x");
				try {
					int iResolution = Integer.parseInt(ps[0]) * Integer.parseInt(ps[1]);
					list.add(iResolution);
					map.put(iResolution, i);
				} catch (Throwable t) {}
			}
		}
		Collections.sort(list);
		int maxRes = list.get(list.size() - 1);
		int index = map.get(maxRes) + 1;
		String m3u8Url = host + lines[index];
		
		String resp = null;
		try {
			HttpURLConnection conn = (HttpURLConnection) new URL(m3u8Url).openConnection();
			conn.connect();
			if (conn.getResponseCode() == HttpURLConnection.HTTP_OK) {
				InputStream is = conn.getInputStream();
				ByteArrayOutputStream baos = new ByteArrayOutputStream();
				byte[] buf = new byte[1024];
				int len = is.read(buf);
				while (len > 0) {
					baos.write(buf, 0, len);
					len = is.read(buf);
				}
				baos.close();
				is.close();
				conn.disconnect();
				resp = new String(baos.toByteArray(), "UTF-8");
			}
		} catch (Throwable t) {
			t.printStackTrace();
			resp = null;
		}

		if (resp != null) {
			lines = resp.split("\n");
			int ind = m3u8Url.lastIndexOf("/") + 1;
			String pref = m3u8Url.substring(0, ind);
			StringBuilder sb = new StringBuilder();
			for (String line : lines) {
				sb.append('\n');
				if (!line.startsWith("#")) {
					sb.append(pref);
				}
				sb.append(line);
			}
			sb.deleteCharAt(0);
			resp = sb.toString();
			System.out.println(resp);
		}
	
		return resp;
	}
	
	private static String[] parseMeu8(String m3u8) throws Throwable {
		String[] resp = null;
		if (m3u8 != null) {
			String[] lines = m3u8.split("\n");
			ArrayList<String> list = new ArrayList<String>();
			for (String line : lines) {
				if (!line.startsWith("#")) {
					list.add(line);
				}
			}
			
			if (list.size() > 0) {
				resp = new String[list.size()];
				for (int i = 0; i < resp.length; i++) {
					resp[i] = list.get(i);
				}
			}
		}
		return resp;
	}
	
	private static HashMap<String, String> downloadVideos(String title, String[] videoList, 
			String name, OnDownloadProgressListener listener) throws Throwable {
		HashMap<String, String> resp = null;
		if (videoList != null) {
			FileListDownloader downloader = new FileListDownloader(title);
			downloader.setCacheFolder("/sdcard/FLVCD");
			downloader.setDownloadThreadCount(10);
			downloader.setRetryCount(20);
			downloader.setCacheSize(1024 * 128);
			downloader.setConnectionTimeout(10);
			downloader.setReadTimeout(60);
			downloader.setOnDownloadProgressListener(listener);
			resp = downloader.start(videoList, name);
		}
		return resp;
	}
	
	private static String genM3u8(String m3u8, HashMap<String, String> localVideos, String name) 
			throws Throwable {
		String resp = null;
		if (localVideos != null) {
			File folder = new File("/sdcard/FLVCD/", name);
			if (!folder.exists()) {
				folder.mkdirs();
			}
			
			FileOutputStream fos = new FileOutputStream(new File(folder, name + ".m3u"));
			OutputStreamWriter osw = new OutputStreamWriter(fos, Charset.forName("UTF-8"));
			String[] lines = m3u8.split("\n");
			for (String line : lines) {
				if (!line.startsWith("#")) {
					line = localVideos.get(line);
				}
				osw.append(line).append('\n');
			}
			osw.flush();
			osw.close();
			
			resp = folder.getAbsolutePath();
		}
		
		return resp;
	}
	
}
