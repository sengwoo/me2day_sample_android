package com.nhn.android.me2day.sample.base;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import org.w3c.dom.Node;

import android.util.Log;

public class Utility {
	
	private static Boolean logFlag = true;
	
	static public void d(String tag, String msg) {
		if ( getLogFlag() == true ) {
			Log.d(tag, msg);
		}
	}
	public static void setLogFlag(Boolean flag){
		Utility.logFlag = flag;
	}
	public static Boolean getLogFlag(){
		return Utility.logFlag;
	}
	
	static public void appendSigUrl(StringBuffer url, boolean appAmp) {
		if (appAmp == true) {
			url.append("&");
		}
		url.append("akey=");
		url.append(Me2dayInfo.APP_KEY);
	}
	
	static public String convertStreamToString(InputStream is) {
		BufferedReader reader = new BufferedReader(new InputStreamReader(is));
		StringBuilder sb = new StringBuilder();
		 
		String line = null;
		try {
			while ((line = reader.readLine()) != null) {
				sb.append(line + "\n");
			}
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			
		}		 
		return sb.toString();
	}
	
	static final public String getElementValue(Node element) {		
		if (element == null) 
			return null;
		
		if (element.getNodeType() != Node.ELEMENT_NODE) 
			return null;
		
		String value = element.getNodeValue();
		if (value == null) {
			Node child = element.getFirstChild();
			while (child != null) {
				
				switch (child.getNodeType()) {
				case Node.CDATA_SECTION_NODE:
				case Node.TEXT_NODE:
					value = child.getNodeValue();
					return value;
				}				
				child = child.getNextSibling();
			}
		}
		return value;
	}
}
