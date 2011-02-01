package com.nhn.android.me2day.sample.base;

public class Me2dayInfo {
	
	public static final String APP_KEY = "4e19bc31dfeb694da5b8b8f3138d63ec";
	
	public static String USER_AGENT = "me2dayLoginModule/x.x (Android OS x.x;Unknown Device)";
	public static String NONCE = "ffffffff";

	public static final String completeAuthUrl = "http://me2day.net/api/auth_by_user_id_complete";
	
	// HTTP Timeout
	public static int TIMEOUT = 30 * 1000; //seconds
	
	//Gzip을 사용여부를 컨트롤하기 위해 for 디버깅
	public static boolean flagUseGzip = true;
	public static final String ENC_TYPE = "gzip";
	
	public static final String host = "http://me2day.net";
	
	public static String loginId = null;
	public static String userAgent = "me2day/(Android OS)";  //will be set later
	
	public static String getLoginId() {
		return Me2dayInfo.loginId;
	}
	public static void setLoginId(String loginId) {
		Me2dayInfo.loginId = loginId;
	}
	
	public static String getUerAgent(){
		return Me2dayInfo.userAgent;
	}
	public static void setUserAgent(String param){
		Me2dayInfo.userAgent = param;
	}
	
}
