package com.nhn.android.me2day.sample.api;

import java.io.InputStream;
import java.net.URLEncoder;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.http.HttpResponse;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import com.nhn.android.me2day.sample.base.Me2dayInfo;
import com.nhn.android.me2day.sample.base.Utility;

public class GetFullAuthTokenWorker {

	static final String TAG = "GetFullAuthTokenWorker";
	private static GetFullAuthTokenWorker instance;

	private String returnToken;
	private String userId;
	private boolean httpok = false;

	public boolean ishttpok() {
		return httpok;
	}

	public static GetFullAuthTokenWorker getInstance() {
		if (instance == null) {
			synchronized (GetFullAuthTokenWorker.class) {
				if (instance == null) {
					instance = new GetFullAuthTokenWorker();
				}
			}
		}
		return instance;
	}

	public String getReturnToken() {
		return returnToken;
	}

	public String getReturnUserId() {
		return userId;
	}
	
	public void onSuccess(HttpResponse response, InputStream in)
			throws Exception {
		Utility.d(TAG, "Called onSuccess()");
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		DocumentBuilder builder = factory.newDocumentBuilder();
		Document document = builder.parse(in);
		if (document != null) {
			NodeList items = document.getElementsByTagName("auth_token");
			if (items != null) {
				Element item = (Element) items.item(0);
				returnToken = Utility.getElementValue(item.getElementsByTagName("full_auth_token").item(0));
				userId = Utility.getElementValue(item.getElementsByTagName("user_id").item(0));
				Me2dayInfo.setLoginId(userId);
				httpok = true;
			}
		}
	}

	public void onError(HttpResponse response, InputStream in) throws Exception {
		Utility.d(TAG, "Called onError()");
		int responseCode = response.getStatusLine().getStatusCode();
		Utility.d(TAG, String.format("Response Code = %d", responseCode));
		returnToken = null;

	}
	
	static public String get_full_auth_token(String authToken) {
		StringBuffer url = new StringBuffer();
		url.append(Me2dayInfo.host).append("/api/get_full_auth_token.xml?");
		url.append("token=").append(authToken);
		Utility.appendSigUrl(url, true);
		
		String urlText = url.toString();
		Utility.d(TAG, "get_full_auth_token(), urlText=" + urlText);
		return urlText;
	}
	
}
