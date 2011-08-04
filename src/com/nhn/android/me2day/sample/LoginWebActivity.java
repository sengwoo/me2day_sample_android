package com.nhn.android.me2day.sample;

import java.io.InputStream;
import java.io.StringBufferInputStream;
import java.net.HttpURLConnection;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.Timer;
import java.util.TimerTask;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.AbstractHttpMessage;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.DialogInterface.OnClickListener;
import android.content.DialogInterface.OnKeyListener;
import android.content.res.Configuration;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.Window;
import android.webkit.CookieManager;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.LinearLayout;
import android.widget.Toast;
import android.graphics.Bitmap;

import com.nhn.android.me2day.sample.base.Me2dayInfo;
import com.nhn.android.me2day.sample.base.Utility;
import com.nhn.android.me2day.sample.api.GetFullAuthTokenWorker;
import com.nhn.android.me2day.sample.R;

/**
 * 미투데이 웹페이지 로그인.
 * 로그인이 성공하면 full token을 받는다.
 * @author telltale
 *
 */
public class LoginWebActivity extends Activity {

	public static final int LOGIN_SUCCESS = 1;
	public static final int LOGIN_FAIL = 2;

	public static final String category = "LoginWebActivity";	//DANIEL
	public static final String PARAM_URL = "url";
	public static final String PARAM_TOKEN = "token";
	public static final String PARAM_ASIG = "asig";

	private WebView loginWebview;
	private LinearLayout completeView;
	ProgressDialog progressDialog;

	private boolean isLoginComplete;
	private String currentURL;
	private int oldOrientation = -1;

	private GetFullAuthTokenWorker workerGetFullAuthToken;
	private static int webViewFailCount = 0;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		Utility.d(category, ">>> Called onCreate()");
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		super.onCreate(savedInstanceState);
		setContentView(R.layout.loginweb);
		isLoginComplete = false;
		loginWebview = (WebView)this.findViewById(R.id.login_web);
		loginWebview.setVerticalScrollbarOverlay(true);
		completeView = (LinearLayout)this.findViewById(R.id.complete_view);
		completeView.setVisibility(View.GONE);
		loginWebview.getSettings().setJavaScriptEnabled(true);
		loginWebview.setWebViewClient(new WebViewClient() {

			@Override
			public void onPageFinished(WebView view, String url) {
				Utility.d(category, ">>> Called onPageFinished(), url=" + url);
				//super.onPageFinished(view, url);
				onPageFinishLogin(url);
			}

			@Override
			public boolean shouldOverrideUrlLoading(WebView view, String url) {
				Utility.d(category, ">>> Called shouldOverrideUrlLoading(), url=" + url);

				//http://me2day.net/api/auth_by_user_id_complete
				//if (url.startsWith(Me2dayInfo.completeAuthUrl) == true) {
				if (url.startsWith("http://me2day.net/api/auth_submit_popup") == true) {
					//DANIEL : 2010-11-22
					//Galaxy S 2.2 Froyo에서는 아래의 코드가 에러 유발
					//상기의 onPageFinished() override 함수에서 아래 코드에 해당하는 내용을 처리함으로
					//아래의 코드는 중복수행에 따른 주석처리함.
					completeAuth(url);

				}	else {
					view.loadUrl(url);
				}
				return true;
			}

			//@Override
			public void onPageStarted(WebView view, String url, Bitmap favicon)	{
				Utility.d(category, ">>> Called onPageStarted(), url=" + url);
				super.onPageStarted(view, url, favicon);
				currentURL = url;
				if(url.startsWith(Me2dayInfo.completeAuthUrl) == true)	{
					isLoginComplete = true;
					updateLoginUI();
				}
			}


			@Override
			public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
				Utility.d(category, ">>> Called onReceivedError(), errorCode=" + errorCode);
				Utility.d(category, ">>> Called onReceivedError(), failingUrl=" + failingUrl);
				super.onReceivedError(view, errorCode, description, failingUrl);
				// http오류가 발생했을 때 로그인 중 창이 계속 떠있고,
				// Back Key처리가 안되는 현상 처리
				if (progressDialog != null) {
					progressDialog.setCancelable(true);
				} else {
					Utility.d(category, ">>> onReceivedError(), webViewFailCount=" + webViewFailCount);
					if (webViewFailCount >= 2 ){
						Utility.d(category, ">>> onReceivedError(), Fail to connect server");

						//3번이상 Connection Error가 발생하였을 경우, 아이디 입력화면으로 전환.
						webViewFailCount = 0;
						view.goBack();
					}else{
						webViewFailCount++;
						Utility.d(category, ">>> onReceivedError(), Retry(" + webViewFailCount + ") to connect server" );
						loginWebview.loadUrl(failingUrl);
					}
				}
			}
		});

		Intent intent = this.getIntent();
		String url = intent.getStringExtra(LoginWebActivity.PARAM_URL);
		Utility.d("LoginWebActivity", String.format("loginUrl %s", url.toString()));
		currentURL = url.toString();
		loginWebview.loadUrl(currentURL);
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		Utility.d(category, ">>> Called onConfigurationChanged()");
		if (oldOrientation == newConfig.orientation) {
			super.onConfigurationChanged(newConfig);
			return;
		}
		oldOrientation = newConfig.orientation;
    	super.onConfigurationChanged(newConfig);
    	if(isLoginComplete)
    		updateLoginUI();
    }

	private void updateLoginUI()	{
		Utility.d(category, ">>> Called updateLoginUI()");
		setContentView(R.layout.loginweb);

		loginWebview = (WebView)this.findViewById(R.id.login_web);
		loginWebview.setVerticalScrollbarOverlay(true);
		completeView = (LinearLayout)this.findViewById(R.id.complete_view);

		if(isLoginComplete)	{
			loginWebview.setVisibility(View.GONE);
			completeView.setVisibility(View.VISIBLE);

		}	else	{
			loginWebview.setVisibility(View.VISIBLE);
			completeView.setVisibility(View.GONE);
		}

		if(currentURL != null && currentURL.length() > 0)	{
			loginWebview.loadUrl(currentURL);
		}

	}

	@Override
	protected void onDestroy() {
		Utility.d(category, ">>> Called onDestroy()");
		if (progressDialog != null) {
			progressDialog.dismiss();
			progressDialog = null;
		}
		if(loginWebview != null)	{
			loginWebview.destroy();
			loginWebview = null;
		}
		if(completeView !=null)	{
			completeView.removeAllViews();
			completeView = null;
		}
		super.onDestroy();
	}
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		Utility.d(category, ">>> Called onKeyDown()");
		if(keyCode == KeyEvent.KEYCODE_SEARCH)
			return true;
		return super.onKeyDown(keyCode, event);
	}
	@Override
	public boolean onSearchRequested()	{
		return false;
	}


	/**
	 * 로그인웹페이지 이외에는 프로그래스바를 활성화 시킨다.
	 */
	private void onPageFinishLogin(String url) {
		Utility.d(category, ">>> Called onPageFinishLogin(), url=" + url);
		Utility.d("LoginWebActivity", String.format("onPageFinished url(%s)", url));
		//if (url.startsWith(Me2dayInfo.completeAuthUrl) == true) {
		if (url.startsWith("http://me2day.net/api/auth_submit_popup") == true) {
			completeAuth(url);
		}
	}

    /**
     * Bearer Type별 네트워크 상태 정보 체크
     * @return Bearer Type별 네트워크 상태 정보
     */
    public boolean checkNetworkAvailable(){
    	Utility.d("BaseActivity", ">>> Called checkNetworkAvailable()");
    	ConnectivityManager conn_manager = (ConnectivityManager)this.getSystemService(Context.CONNECTIVITY_SERVICE);
    	boolean isNetworkAvailable = false;
        try{
            NetworkInfo network_info = conn_manager.getActiveNetworkInfo();
            if ( network_info != null && network_info.isConnected() ) {
                if (network_info.getType() == ConnectivityManager.TYPE_WIFI ){
                    // do some staff with WiFi connection
                	isNetworkAvailable = true;
                }	else if (network_info.getType() == ConnectivityManager.TYPE_MOBILE ){
                    // do something with Carrier connection
                	isNetworkAvailable = true;
                }	else if (network_info.getType() == ConnectivityManager.TYPE_WIMAX ){
                    // do something with Wibro/Wimax connection
                	isNetworkAvailable = true;
                }
            }
        }catch(Exception e){
            e.printStackTrace();
        }
        if(isNetworkAvailable == false)
        	showNetworkAlert();

        return isNetworkAvailable;
    }

    /**
     * 네트워크 상태가 이용 불가능일 경우, Alert Dialog 띄워줌.
     */
    private void showNetworkAlert()	{
    	Utility.d("BaseActivity", ">>> Called showNetworkAlert()");
    	AlertDialog.Builder adb = new AlertDialog.Builder(this);
		adb.setTitle("미투데이");
		adb.setIcon(android.R.drawable.ic_dialog_alert);
		adb.setMessage("네트워크 연결을 확인해 주세요.");
		adb.setPositiveButton("확인", new OnClickListener(){
			public void onClick(DialogInterface dialog, int which) {
				dialog.dismiss();
			}
		});
		adb.setOnKeyListener(new OnKeyListener(){
			public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {
				if(keyCode == KeyEvent.KEYCODE_SEARCH)
					return true;
				return false;
			}
		});
		adb.show();
    }

	/**
	 * 로그인 완료페이지가 성공이면 full token을 얻는다.
	 */
	private void completeAuth(String url) {
		Utility.d(category, ">>> Called completeAuth(), url=" + url);

    	// Network Available check.
    	if(checkNetworkAvailable() == false){
			if (progressDialog != null) {
				progressDialog.hide();
			}
    		return;
    	}

	    getFullAuthToken();
	}

	private boolean getFullAuthToken() {
		Utility.d(category, ">>> Called getFullAuthToken()");

		boolean loginsuccess = false;
		Intent intent = this.getIntent();
		String token = intent.getStringExtra(LoginWebActivity.PARAM_TOKEN);
		workerGetFullAuthToken = GetFullAuthTokenWorker.getInstance();

		try {
			HttpParams params = new BasicHttpParams();
			HttpConnectionParams.setConnectionTimeout(params, Me2dayInfo.TIMEOUT);
			HttpConnectionParams.setSoTimeout(params, Me2dayInfo.TIMEOUT);

			DefaultHttpClient httpClient = new DefaultHttpClient(params);
			HttpRequestBase method = createHttpMehtod(GetFullAuthTokenWorker.get_full_auth_token(token));

			// configure http header
			settingHttpClient(method, httpClient);

			HttpResponse response = httpClient.execute(method);
			InputStream in = response.getEntity().getContent();

			String xmlMessage = Utility.convertStreamToString(in);
			in = new StringBufferInputStream(xmlMessage);

			int responseCode = response.getStatusLine().getStatusCode();
			Utility.d(category, String.format("Response Code = %d", responseCode));

			//Http Status Code가 200일때만 성공으로 취급한다.
			if (responseCode != HttpURLConnection.HTTP_OK) {
				Utility.d(category, String.format("onClickLoginUsingOpenid(), Error(%d, %s) message(%s)",
						responseCode, response.getStatusLine().getReasonPhrase(), xmlMessage));
				workerGetFullAuthToken.onError(response, in);
				return false;
			}

			Utility.d(category, xmlMessage);
			workerGetFullAuthToken.onSuccess( response, in );

		    if ( workerGetFullAuthToken.ishttpok()==true ) {
		        String returnToken = workerGetFullAuthToken.getReturnToken();

				if (returnToken != null && returnToken.length()>0) {
					loginsuccess = true;
					loginSuccess(returnToken);
				} else {
					loginsuccess = false;
				}
			}

			if (loginsuccess == false) {
				Toast.makeText(LoginWebActivity.this, LoginWebActivity.this.getString(R.string.message_unknown_error), Toast.LENGTH_SHORT).show();
				LoginWebActivity.this.setResult(LoginWebActivity.LOGIN_FAIL);
				LoginWebActivity.this.finish();
			}
		}
		catch (SocketTimeoutException timeoutEx) {
			timeoutEx.printStackTrace();
			return false;
		}
		catch (SocketException e) {
			e.printStackTrace();
			return false;
		}
		catch (Exception e) {
			e.printStackTrace();
			return false;
		}
		return true;
	}

	/**
	 * HttpMethod(Get, Post)를 선택한다.
	 */
	private HttpRequestBase createHttpMehtod(String url) {
		Utility.d(category, "Called createHttpMehtod(), url=" + url);
		return new HttpGet(url);
	}

	/*
	 * HTTP Request Header Parameter를 설정한다.
	 */
	private void settingHttpClient(AbstractHttpMessage methodPost, DefaultHttpClient httpClient) {
		String aKey = Me2dayInfo.APP_KEY;
		methodPost.setHeader("Me2_application_key", aKey);

//		String aSig = Utility.getAppSig();
//		methodPost.setHeader("Me2_asig", aSig);
//		methodPost.setHeader("User_Agent", Me2dayInfo.getUerAgent());
	}

	private void loginSuccess(String fullAuthtoken) {
		Utility.d(category, ">>> Called loginSuccess()");
		this.setResult(LOGIN_SUCCESS);
		LoginWebActivity.this.finish();
	}
}
