package com.nhn.android.me2day.sample;

import java.io.InputStream;
import java.io.StringBufferInputStream;
import java.net.HttpURLConnection;
import java.net.SocketException;
import java.net.SocketTimeoutException;
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
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.DialogInterface.OnClickListener;
import android.content.DialogInterface.OnKeyListener;
import android.content.res.Configuration;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.RelativeLayout;

import com.nhn.android.me2day.sample.R;
import com.nhn.android.me2day.sample.base.Me2dayInfo;
import com.nhn.android.me2day.sample.base.Utility;

import com.nhn.android.me2day.sample.api.GetAuthUrlWorker;
import com.nhn.android.me2day.sample.api.GetFullAuthTokenWorker;;


/**
 * 미투데이 로그인.
 * 어플리케이션 시작 Activity
 * 1. get_auth api를 통해 어느 웹페이지에서 로그인 하는지 알아낸다.
 * 2. LoginWebActity를 통해 웹 로그인을 수행한다.
 */
public class SplashActivity extends Activity {

	public static final String category = "SplashActivity";
	public static final String PARAM_FORWARD_CLASS = "forwardClass";
	public static final String LOGOUT_ID ="com.nhn.android.me2day.Logout";

	public static final int NOTIFY_RECALL = 1000;
	public static final int NOTIFY_MAILET = 2000;
	public static final int LOGOUT_VALUE = 1000;

	private static final int request_loginweb = 1;
	private static final int request_joinweb = 2;
	private static final int request_idpwdapi = 3;
	private static final int request_main_activity = 100;
	private static final int request_writepost_activity = 200;

	RelativeLayout openidLayout = null;
	Button btn_openid_login = null;

	/**
	 * 로그인 성공 후 실행할 Activity
	 */
	String forwardClassName = WritePostActivity.class.getName();
	ProgressDialog progDialog;

	GetAuthUrlWorker getAuthUrlWorker = null;
	GetFullAuthTokenWorker getFullAuthTokenWorker = null;

    @Override
    public void onCreate(Bundle savedInstanceState) {
    	Utility.d(category, "Called onCreate()");
    	super.onCreate(savedInstanceState);
    	requestWindowFeature(Window.FEATURE_NO_TITLE);
        initLoginLayout();
    }

    @Override
    protected void onPause() {
    	Utility.d(category, "Called onPause()");
        super.onPause();
        if(progDialog != null)
        	progDialog.dismiss();

        progDialog = null;
    }

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if(keyCode == KeyEvent.KEYCODE_SEARCH){
			return true;
		} else if(keyCode == KeyEvent.KEYCODE_BACK){
			finish();
		}
		return false;
		//return super.onKeyDown(keyCode, event);
	}

	@Override
	public boolean onSearchRequested()	{
		return false;
	}

    private void initLoginLayout(){
    	Utility.d(category, "Called initLoginLayout()");
    	updateLoginUI();
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

    @Override
	protected void onDestroy() {
    	Utility.d(category, "Called onDestroy()");
    	if(progDialog != null)
    		progDialog.dismiss();

    	progDialog = null;
    	super.onDestroy();
	}

	@Override
	protected void onRestart() {
		Utility.d(category, "Called onRestart()");
		super.onRestart();
		updateLoginUI();
	}

	private void updateLoginUI() {
		Utility.d(category, "Called updateLoginUI()");
		setContentView(R.layout.login);

		btn_openid_login = (Button)this.findViewById(R.id.login_openid_button);
		openidLayout = (RelativeLayout)this.findViewById(R.id.logintab_openid_body);

        //OPEN ID //////////////////////////////////////////////////////

        if (btn_openid_login != null) {
        	btn_openid_login.setOnClickListener(new View.OnClickListener() {
	        	public void onClick(View button) {

	            	////////////////////////////////////////////////
	            	//Get Login Start URL
	            	boolean bAuthLoginStart = onClickLoginUsingOpenid();
	        		if(bAuthLoginStart == false){
	        			Utility.d(category, "ERROR, fail to get login start url");
	        			return;
	        		} else {
	        			Utility.d(category, "SUCCESS to get login start url="
	        					+ getAuthUrlWorker.getReturnUrl());
	        		}
	        		////////////////////////////////////////////////
	            	//Get Full-Auth Token
	        		boolean bFullAuthToken = openLoginWeb();
	        		if(bFullAuthToken == false){
	        			Utility.d(category, "ERROR, fail to get full auth token");
	        			return;
	        		}
	        		////////////////////////////////////////////////
	        	}
	        });
        }
	}

    private void showProgressDialog() {
    	progDialog = new ProgressDialog(this);
    	progDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
    	progDialog.setMessage(this.getText(R.string.logining));
    	progDialog.setOnKeyListener(new OnKeyListener(){

			public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {

				if(keyCode == KeyEvent.KEYCODE_SEARCH)
					return true;

				else if(keyCode == KeyEvent.KEYCODE_BACK)	{
					return false;
				}
				return false;
			}
		});
    	progDialog.show();
    }

    private boolean onClickLoginUsingOpenid() {
    	Utility.d(category, "Called onClickLoginUsingOpenid()");

    	// Network Available check.
    	if(checkNetworkAvailable() == false)
    		return false;

		showProgressDialog();
		getAuthUrlWorker = GetAuthUrlWorker.getInstance();

		try {
			HttpParams params = new BasicHttpParams();
			HttpConnectionParams.setConnectionTimeout(params, Me2dayInfo.TIMEOUT);
			HttpConnectionParams.setSoTimeout(params, Me2dayInfo.TIMEOUT);

			DefaultHttpClient httpClient = new DefaultHttpClient(params);
			HttpRequestBase method = createHttpMehtod(GetAuthUrlWorker.get_auth_url());

			// configure http header
			settingHttpClient(method, httpClient);

			HttpResponse response = httpClient.execute(method);

			if (progDialog != null) {
				progDialog.hide();
			}

			InputStream in = response.getEntity().getContent();

			String xmlMessage = Utility.convertStreamToString(in);
			in = new StringBufferInputStream(xmlMessage);

			int responseCode = response.getStatusLine().getStatusCode();
			Utility.d(category, String.format("Response Code = %d", responseCode));

			//Http Status Code가 200일때만 성공으로 취급한다.
			if (responseCode != HttpURLConnection.HTTP_OK) {
				Utility.d(category, String.format("onClickLoginUsingOpenid(), Error(%d, %s) message(%s)",
						responseCode, response.getStatusLine().getReasonPhrase(), xmlMessage));
				getAuthUrlWorker.onError(response, in);
				return false;
			}

			Utility.d(category, xmlMessage);
			getAuthUrlWorker.onSuccess( response, in );
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

    private boolean openLoginWeb() {
    	Utility.d(category, "Called openLoginWeb()");

    	String url = getAuthUrlWorker.getReturnUrl();
		if (url != null) {
			Intent intent = new Intent(this, LoginWebActivity.class);
			intent.putExtra(LoginWebActivity.PARAM_URL, url);
			intent.putExtra(LoginWebActivity.PARAM_TOKEN, getAuthUrlWorker.getReturnToken());
			this.startActivityForResult(intent, request_loginweb);
		}	else {
			Dialog dialog = new AlertDialog.Builder(this).setTitle("미투데이")
			.setMessage("로그인에 실패하였습니다.").setPositiveButton("확인", new OnClickListener(){
				public void onClick(DialogInterface dialog, int which) {
					dialog.cancel();
					if(progDialog != null)
						progDialog.dismiss();
				}

			}).setOnKeyListener(new OnKeyListener(){
				public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {
					if(keyCode == KeyEvent.KEYCODE_SEARCH)
						return true;
					if(progDialog != null)
						progDialog.dismiss();
					return false;
				}
			}).create();
			dialog.show();
		}
		return true;
	}

	@Override
	protected void onNewIntent(Intent intent) {
		Utility.d(category, ">>> Called onNewIntent()");
		super.onNewIntent(intent);
		int logoutValue = intent.getIntExtra(LOGOUT_ID, -1);
		if (logoutValue == LOGOUT_VALUE) {
			Intent newIntent = new Intent(this, SplashActivity.class);
			this.startActivity(newIntent);
			intent.removeExtra(LOGOUT_ID);
			intent.putExtra(LOGOUT_ID, -1);
		}
		Utility.d("Me2dayLogin", "Me2dayIntent onNewIntent");
	}
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		Utility.d(category, "Called onActivityResult()");
		super.onActivityResult(requestCode, resultCode, data);
		Utility.d("Me2dayLogin::onActivityResult", String.format("requestCode(%d) resultCode(%d)", requestCode, resultCode));

		switch (requestCode) {
		case request_loginweb:
			if (resultCode == LoginWebActivity.LOGIN_SUCCESS) {
				forward();
			}
			break;
		case request_joinweb:
			this.finish();
			break;

		case request_main_activity:
			this.finish();
			break;
		case request_writepost_activity:
		    this.finish();
            break;
		}
	}

	private void forward() {
		Utility.d("Me2dayLogin", forwardClassName);
	    Intent intent = new Intent(this, WritePostActivity.class);
        this.startActivityForResult(intent, request_writepost_activity);
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
}