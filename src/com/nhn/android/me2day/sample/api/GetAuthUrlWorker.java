package com.nhn.android.me2day.sample.api;

import java.io.InputStream;
import java.net.URLEncoder;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.http.HttpResponse;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import android.util.Log;

import com.nhn.android.me2day.sample.base.Utility;
import com.nhn.android.me2day.sample.base.Me2dayInfo;

public class GetAuthUrlWorker {

	static final String TAG = "GetAuthUrlWorker";
	private String returnUrl = null;
	private String returnToken = null;

	private static GetAuthUrlWorker instance;

	public static GetAuthUrlWorker getInstance() {
		if (instance == null) {
			synchronized (GetAuthUrlWorker.class) {
				if (instance == null) {
					instance = new GetAuthUrlWorker();
				}
			}
		}
		return instance;
	}

	public String getReturnUrl() {
		return returnUrl;
	}

	public String getReturnToken() {
		return returnToken;
	}

	public void onSuccess(HttpResponse response, InputStream in)
			throws Exception {
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		DocumentBuilder builder = factory.newDocumentBuilder();
		Document document = builder.parse(in);
		if (document != null) {
			NodeList items = document.getElementsByTagName("auth_token");
			if (items != null) {
				Element item = (Element) items.item(0);
				returnUrl = Utility.getElementValue(item.getElementsByTagName("url").item(0));
				returnToken = Utility.getElementValue(item.getElementsByTagName("token").item(0));
			}
		}

	}

	public void onError(HttpResponse response, InputStream in) throws Exception {
		Utility.d(TAG, "Called onError()");
		int responseCode = response.getStatusLine().getStatusCode();
		Utility.d(TAG, String.format("Response Code = %d", responseCode));
		returnUrl = null;
		returnToken = null;
	}
	
	/**
	 * get_auth_url api
	 * 미투데이 Id에 대해 로그인할 페이지를 얻는다.
	 * @param userId - 미투데이 Id
	 */
	static public String get_auth_url(String userId) {
		Utility.d(TAG, "Called get_auth_url(), userId=" + userId);
		StringBuffer url = new StringBuffer();
		url.append(Me2dayInfo.host).append("/api/get_auth_url.xml?");
		Utility.appendSigUrl(url, false);
		String urlText = url.toString();
		Utility.d(TAG, "get_auth_url(), urlText=" + urlText);
		return urlText;
	}
}
