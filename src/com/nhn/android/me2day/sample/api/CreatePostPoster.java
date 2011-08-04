package com.nhn.android.me2day.sample.api;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.ArrayList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.AbstractHttpMessage;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import com.nhn.android.me2day.sample.base.Me2dayInfo;
import com.nhn.android.me2day.sample.base.BASE64;
import com.nhn.android.me2day.sample.base.Utility;
import com.nhn.android.me2day.sample.multipart.FilePart;
import com.nhn.android.me2day.sample.multipart.MultipartEntity;
import com.nhn.android.me2day.sample.multipart.Part;
import com.nhn.android.me2day.sample.multipart.StringPart;

/**
 * 포스트 보내기. MultiPart 형식을 사용한다.
 * 
 * @author telltale
 * 
 */
public class CreatePostPoster {

	String body;
	String tag;
	int icon = 0;
	String longitude;
	String latitude;
	String spotId;
	String location;
	String attachment;
	// byte[] byte_attachment;
	FilePart filePart;
	boolean isCloseComment = false;
	static private int RETRY_COUNT = 1;
	private String headerFileName = null;
	public int retrycount = RETRY_COUNT;

	public CreatePostPoster() {
		// 포스트 쓰기는 두 번이상 Retry 하지 않도록 한다.
		// 글이 두 개 이상 올라가는 문제가 발생할 수 있음
		this.setRetryCount(RETRY_COUNT);
	}

	/**
	 * retrycount 설정
	 */
	public void setRetryCount(int iRetrycount) {
		retrycount = iRetrycount;
	}

	public void setTag(String tag) {
		this.tag = tag;
	}

	public void setIcon(int icon) {
		this.icon = icon;
	}

	public void setLongitude(String longitude) {
		this.longitude = longitude;
	}

	public void setLatitude(String latitude) {
		this.latitude = latitude;
	}

	public void setSpotId(String spotId) {
		this.spotId = spotId;
	}

	public void setLocation(String location) {
		this.location = location;
	}

	public void setAttachment(String attachment) {
		this.attachment = attachment;
	}


	public void setBody(String body) {
		this.body = body;
	}

	public void setCloseComment(boolean isCloseComment) {
		this.isCloseComment = isCloseComment;
	}

	/**
	 * Multipart로 보내기 위해 POST를 사용한다.
	 */
	public HttpRequestBase createHttpMehtod(String url) {
		return new HttpPost(url);
	}

	/**
	 * Multipart를 구성한다.
	 */
	public void settingHttpClient(AbstractHttpMessage methodPost, DefaultHttpClient httpClient) {
		StringBuffer buffer = new StringBuffer();
		String loginId = Me2dayInfo.getLoginId();
		String fullAuthToken = GetFullAuthTokenWorker.getInstance().getReturnToken();
		
		//MUST be appended "full_auth_token " string between ":" and received fullAuthToken
		buffer.append(loginId).append(":").append("full_auth_token ").append(fullAuthToken); 
		BASE64 base64 = new BASE64(false);
		String encodeValue = base64.encode(buffer.toString());
		buffer.setLength(0);
		buffer.append("Basic ").append(encodeValue);
		String authorization = buffer.toString();

		methodPost.setHeader("Authorization", authorization);
		if (Me2dayInfo.flagUseGzip)
			methodPost.setHeader("Accept-Encoding", Me2dayInfo.ENC_TYPE);
		else
			methodPost.setHeader("Accept-Encoding", "");

		methodPost.setHeader("User-Agent", Me2dayInfo.getUerAgent());

		Utility.d("Me2dayDataWorker", String.format(
				"loginId(%s), fullAuthToken(%s), auth(%s)", loginId,
				fullAuthToken, authorization));

		ArrayList<Part> partArray = new ArrayList<Part>();

		Utility.d("CreatePostPoster", String.format(
				"settingHttpClient body(%s), attachment(%s)", this.body, this.attachment));

		if (this.body != null) {
			partArray.add(new StringPart("post[body]", this.body, "UTF8"));
		}
		if (this.tag != null) {
			partArray.add(new StringPart("post[tags]", this.tag, "UTF8"));
		}
		if (this.icon > 0) {
			partArray.add(new StringPart("post[icon]", String.format("%d", this.icon), "UTF8"));
		}
		if (this.attachment != null) {
			try {
				// if (byte_attachment == null)
				filePart = new FilePart("attachment", new File(attachment));
				// else
				// filePart = new FilePart("attachment", attachment,
				// byte_attachment);
				if (headerFileName != null)
					filePart.setHeaderFileName(headerFileName);
				partArray.add(filePart);
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			}
		}
		if (this.longitude != null) {
			partArray.add(new StringPart("longitude", this.longitude, "UTF8"));
		}
		if (this.latitude != null) {
			partArray.add(new StringPart("latitude", this.latitude, "UTF8"));
		}
		if (this.spotId != null && this.spotId.length() > 0) {
			partArray.add(new StringPart("domain", "me2spot", "UTF8"));
			partArray.add(new StringPart("key", this.spotId, "UTF8"));
			Utility.d("CreatePostPoster", ">>>>>> key=" + this.spotId + ", domain=me2post <<<<<<");
		}
		if (this.location != null) {
			partArray.add(new StringPart("location", this.location, "UTF8"));
		}
		if (this.isCloseComment == true) {
			partArray.add(new StringPart("close_comment", "true", "UTF8"));
		}
		int count = partArray.size();
		Part[] parts = new Part[count];
		for (int i = 0; i < count; i++) {
			parts[i] = partArray.get(i);
		}
		MultipartEntity entity = new MultipartEntity(parts);
		HttpPost httpPost = (HttpPost) methodPost;
		httpPost.setEntity(entity);
	}

	public long getSendigFileLength() {
		if (filePart == null)
			return 0;
		else {
			return filePart.getSendingDataLength();
		}
	}

	public long getTotalFileLength() {
		if (filePart == null)
			return 0;
		else {
			return filePart.getTotalLength();
		}
	}

	public void setHeaderFileName(String fileName) {
		this.headerFileName = fileName;
	}

	/**
	 * create_post api 포스트 보내기에 사용한다.
	 */
	static public String create_post(String bandId) {
		StringBuffer url = new StringBuffer();
		url.append(Me2dayInfo.host).append("/api/create_post/");

		if (bandId == null) {
			String loginId = Me2dayInfo.getLoginId();
			if (loginId != null) {
				url.append(loginId).append(".xml?");
			}
		} else {
			url.append(bandId).append(".xml?");
		}

		Utility.appendSigUrl(url, false);

		String urlText = url.toString();
		Utility.d("CreatePostPoster", "****** create_post(), urlText=" + urlText);
		return urlText;
	}

	public void onSuccess(HttpResponse response, InputStream in)
			throws Exception {
		
		Utility.d("CreatePostPoster", "Called onSuccess()");
//		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
//		DocumentBuilder builder = factory.newDocumentBuilder();
//		Document document = builder.parse(in);
//		if (document != null) {
//			NodeList items = document.getElementsByTagName("auth_token");
//			if (items != null) {
//				Element item = (Element) items.item(0);
//				returnUrl = Utility.getElementValue(item.getElementsByTagName(
//						"url").item(0));
//				returnToken = Utility.getElementValue(item
//						.getElementsByTagName("token").item(0));
//			}
//		}

	}

	public void onError(HttpResponse response, InputStream in) throws Exception {
		Utility.d("CreatePostPoster", "Called onError()");
		int responseCode = response.getStatusLine().getStatusCode();
		Utility.d("CreatePostPoster", String.format("Response Code = %d", responseCode));
	}
}