package m.flvcd;

import java.util.Calendar;

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
import android.widget.EditText;

public class MainActivity extends Activity implements OnClickListener {
	private static final String FIREFOX_UA = "Mozilla/5.0 (Windows NT 6.1; WOW64; rv:43.0) Gecko/20100101 Firefox/43.0";
	private WebView wvBody;
	
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		findViewById(R.id.tvClose).setOnClickListener(this);
		findViewById(R.id.ivMenu).setOnClickListener(this);
		findViewById(R.id.llMenu).setOnClickListener(this);
		findViewById(R.id.tvParse).setOnClickListener(this);
		findViewById(R.id.tvParseUrl).setOnClickListener(this);
		findViewById(R.id.rlLoadUrl).setOnClickListener(this);
		findViewById(R.id.btnLoad).setOnClickListener(this);
		findViewById(R.id.tvSwitchUA).setOnClickListener(this);
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
	
	@SuppressWarnings("deprecation")
	public void onClick(View v) {
		switch (v.getId()) {
			case R.id.tvClose: {
				View llMenu = findViewById(R.id.llMenu);
				View rlLoadUrl = findViewById(R.id.rlLoadUrl);
				if (llMenu.getVisibility() == View.VISIBLE) {
					llMenu.setVisibility(View.GONE);
				} else if (rlLoadUrl.getVisibility() == View.VISIBLE) {
					rlLoadUrl.setVisibility(View.GONE);
				} else {
					finish();
				}
			} break;
			case R.id.ivMenu: {
				View llMenu = findViewById(R.id.llMenu);
				if (llMenu.getVisibility() == View.VISIBLE) {
					llMenu.setVisibility(View.GONE);
				} else {
					llMenu.setVisibility(View.VISIBLE);
				}
			} break;
			case R.id.llMenu: {
				v.setVisibility(View.GONE);
			} break;
			case R.id.tvParse: {
				Intent i = new Intent(this, DownloadService.class);
				i.putExtra("title", wvBody.getTitle());
				i.putExtra("url", wvBody.getUrl());
				startService(i);
				findViewById(R.id.llMenu).setVisibility(View.GONE);
			} break;
			case R.id.tvParseUrl: {
				findViewById(R.id.rlLoadUrl).setVisibility(View.VISIBLE);
				findViewById(R.id.llMenu).setVisibility(View.GONE);
			} break;
			case R.id.rlLoadUrl: {
				v.setVisibility(View.GONE);
			} break;
			case R.id.btnLoad: {
				EditText etUrl = (EditText) findViewById(R.id.etUrl);
				String url = etUrl.getText().toString().trim();
				Intent i = new Intent(this, DownloadService.class);
				Calendar cal = Calendar.getInstance();
				cal.setTimeInMillis(System.currentTimeMillis());
				i.putExtra("title", cal.getTime().toLocaleString());
				i.putExtra("url", url);
				startService(i);
				findViewById(R.id.rlLoadUrl).setVisibility(View.GONE);
			}
			case R.id.tvSwitchUA: {
				wvBody.getSettings().setUserAgentString(FIREFOX_UA);
				wvBody.reload();
				findViewById(R.id.llMenu).setVisibility(View.GONE);
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
