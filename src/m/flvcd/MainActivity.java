package m.flvcd;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.webkit.WebSettings;
import android.webkit.WebSettings.RenderPriority;
import android.webkit.WebView;
import android.webkit.WebViewClient;

public class MainActivity extends Activity implements OnClickListener {
	private WebView wvBody;
	
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		findViewById(R.id.tvClose).setOnClickListener(this);
		findViewById(R.id.tvParse).setOnClickListener(this);
		wvBody = (WebView) findViewById(R.id.wvBody);
		WebSettings setting = wvBody.getSettings();
		setting.setJavaScriptEnabled(true);
		setting.setRenderPriority(RenderPriority.HIGH);
		setting.setSupportZoom(true);
		setting.setBuiltInZoomControls(true);
		setting.setDisplayZoomControls(false);
		setting.setCacheMode(WebSettings.LOAD_CACHE_ELSE_NETWORK);
		setting.setDomStorageEnabled(true);
		setting.setDatabaseEnabled(true);
		setting.setAppCacheEnabled(true);
		wvBody.setWebViewClient(new WebViewClient() {
			public boolean shouldOverrideUrlLoading(WebView view, String url) {
				view.loadUrl(url);
				return true;
			}
		});
		wvBody.loadUrl("http://www.youku.com");
	}
	
	public void onClick(View v) {
		switch (v.getId()) {
			case R.id.tvClose: {
				finish();
			} break;
			case R.id.tvParse: {
				Intent i = new Intent(this, DownloadService.class);
				i.putExtra("title", wvBody.getTitle());
				i.putExtra("url", wvBody.getUrl());
				startService(i);
			} break;
		}
	}
	
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_BACK && wvBody.canGoBack()) {
			wvBody.goBack();
			return true;
		}
		return super.onKeyDown(keyCode, event);
	}
	
}
