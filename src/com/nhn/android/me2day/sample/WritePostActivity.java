package com.nhn.android.me2day.sample;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringBufferInputStream;
import java.net.HttpURLConnection;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.impl.client.DefaultHttpClient;
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
import android.content.DialogInterface.OnKeyListener;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Rect;
import android.hardware.Camera.Size;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
//import android.media.ExifInterface;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.provider.MediaStore;
import android.test.TouchUtils;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.SoundEffectConstants;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.CompoundButton.OnCheckedChangeListener;

import com.nhn.android.me2day.sample.R;
import com.nhn.android.me2day.sample.base.Me2dayInfo;
import com.nhn.android.me2day.sample.base.Utility;
import com.nhn.android.me2day.sample.api.CreatePostPoster;

/**
 * 포스트 쓰기 Activity
 */
public class WritePostActivity extends Activity implements TextWatcher {
	
	public static final String category = "WritePostActivity";
	
	public static final String PARAM_TEXT = "text";
	public static final String PARAM_MEDIA = "sended_media";
	public static final String PARAM_SENDED_FILE_ERROR = "sended_file_error";
	public static final String PARAM_RETURN_POST = "returnPost";
	
	private static final String PHOTO_FILEPATH = "writepostactivity.photo_filepath";
	private static final String TEMP_CAMERA_IMAGE_PATH = "writepostactivity.temp_camera_image_path";
	
	private static final String PHOTO_CAMERA_FILENAME = "me2day_camera.jpg";;
	private static final String PHOTO_RESIZE_FILENAME = "me2day_resized.jpg";
	
	private static final int IMAGE_ALLOW_WIDTH = 480;//480;
	private static final int IMAGE_ALLOW_HEIGHT = 800;//800;
	
	public static final int request_album = 1;
	public static final int request_photo_camera = 2;
	public static final int request_video_camera = 3;
	public static final int request_gps = 4;
	public static final int request_icon = 5;
	
	public static final int resultCodeSuccess = 1;
	
	static final int PROGRESS_DIALOG = 0;
	static final int SPINNER_PROGRESS_DIALOG = 1;
	static final int FINDING_DIALOG = 2;
	
	private static final int NONE_TYPE = -1;
	private static final int IMAGE_TYPE = 0;
	private static final int VIDEO_TYPE = 1; 
	
	private EditText edit;
	private TextView editcount;
	
	private String messageText;

	/**
	 * 포스트 보내기 Worker
	 */
	private CreatePostPoster postPoster;

	// 선택된 Image
	private String selectedMediaFile;
	private int mediaType = NONE_TYPE;

	
	// 카메라 임시 저장소
	private String tempCameraPath;
	
	private Handler handler = new Handler();
	private boolean isSendCompleted;
	private ProgressDialog progressDialog;
	
	private Button sendButton;
	private boolean auto_open_SoftInput = false;

	private Dialog customProgressDialog;
	private int compressCount;
	private int initCursorPos = 0;
	private ProgressDialog loadingDialog = null;
	public static int THROW_ERROR = 0;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		Utility.d(category, "Called onCreate()");
		THROW_ERROR = 1;
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		super.onCreate(savedInstanceState);
		this.setContentView(R.layout.write_post);
		
		if (savedInstanceState != null) {
			String imageFilePath = savedInstanceState.getString(PHOTO_FILEPATH);
			if (imageFilePath != null)
				setSelectedMediaFile(savedInstanceState.getString(PHOTO_FILEPATH));
			
			this.tempCameraPath = savedInstanceState.getString(TEMP_CAMERA_IMAGE_PATH);
		}

		Intent intent = this.getIntent();
		if (intent != null) {
			boolean senedFileError = intent.getBooleanExtra(PARAM_SENDED_FILE_ERROR, false);
			if (senedFileError) {
				popupErrorDialog(this);
			}
		}
		updateUI(true);
	}

	static public void popupErrorDialog(Context context) {
		AlertDialog.Builder dialog = new AlertDialog.Builder(context);
		dialog.setIcon(android.R.drawable.ic_dialog_alert);
		dialog.setTitle(context.getString(R.string.app_name));
		dialog.setMessage(context.getString(R.string.message_sended_file_error));
		dialog.setPositiveButton(context.getString(R.string.ok), null);
		dialog.setOnKeyListener(new DialogInterface.OnKeyListener(){
			public boolean onKey(DialogInterface dialog, int keyCode,
					KeyEvent event) {
				if(keyCode == KeyEvent.KEYCODE_SEARCH)
					return true;
				return false;
			}
        	
        });
    	dialog.show();
	}
	
	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		Utility.d(category, "Called onConfigurationChanged()");
		super.onConfigurationChanged(newConfig);
		
		String contentText = edit.getText().toString();
        
        int focusPos = 0;
        View focusView = this.getCurrentFocus();
        if (focusView == edit) {
        	focusPos = 1;
        }
        
		this.setContentView(R.layout.write_post);
        updateUI(false);
        edit.setText(contentText);
        setMessageText();
        updatePhotoButton();
        
        if (focusPos == 1) {
        	edit.requestFocus();
        	setWriteCount();
        }
	}
	
    @Override
    protected void onSaveInstanceState(Bundle outState) {
    	Utility.d(category, "Called onSaveInstanceState()");
        super.onSaveInstanceState(outState);
        outState.putString(PHOTO_FILEPATH, selectedMediaFile);
        outState.putString(TEMP_CAMERA_IMAGE_PATH, tempCameraPath);
    }
    
	@Override
	protected void onRestart() {
		Utility.d(category, "Called onRestart()"); 
		super.onRestart();
	}
	
    @Override
    protected void onResume() {
    	Utility.d(category, "Called onResume()");
    	super.onResume();
    }
    
    @Override
    protected void onPause() {
    	Utility.d(category, "Called onPause()");
    	super.onPause();
    	hideKeyboard();
    }
     
    @Override
    protected void onDestroy() {
    	Utility.d(category, "Called onDestroy()");
    	super.onDestroy();
    }
    
    private void hideKeyboard() {
		InputMethodManager manager = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
		if (manager.isActive())
			manager.hideSoftInputFromWindow(edit.getWindowToken(), 0);
    }
    
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK
                && event.getRepeatCount() == 0 && isEditing() == true) {

			AlertDialog.Builder dialog = new AlertDialog.Builder(this);
			dialog.setIcon(android.R.drawable.ic_dialog_info);
			dialog.setTitle(this.getString(R.string.title_cancle_editpost));
			dialog.setMessage(this.getString(R.string.message_cancel_editpost));
			dialog.setPositiveButton(this.getString(R.string.yes), new DialogInterface.OnClickListener() {
				
				public void onClick(DialogInterface dialog, int which) {
					// TODO Auto-generated method stub
					onClickCancelButton();
				}
			});
			dialog.setNegativeButton(this.getString(R.string.no), null);
			/**
	         * author daesoo,kim
	         * date 2010.3.16
	         */
			dialog.setOnKeyListener(new DialogInterface.OnKeyListener(){
				public boolean onKey(DialogInterface dialog, int keyCode,
						KeyEvent event) {
					if(keyCode == KeyEvent.KEYCODE_SEARCH)
						return true;
					return false;
				}
	        	
	        });
        	dialog.show();
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }    
  
	private void updateUI(boolean bInit) {
		Utility.d(category, "Called updateUI()");
		Intent intent = this.getIntent();

		editcount = (TextView)this.findViewById(R.id.editcount);
		edit = (EditText)this.findViewById(R.id.edit);
		if (edit != null) {
			setMessageText();
			edit.addTextChangedListener(this);

			String initText = intent.getStringExtra(PARAM_TEXT);
			if (initText != null && bInit == true) {
				if (initText.startsWith("http://") || initText.startsWith("https://")) {
					edit.setText(String.format("\"\":%s ", initText));
					initCursorPos = 1;
				}else {
					edit.setText(initText);
				}
				setMessageText();
			}	
			String mediaPath = intent.getStringExtra(PARAM_MEDIA);
			if (mediaPath != null && bInit == true) {
				setSelectedMediaFile(mediaPath);
			}
			
			edit.setOnFocusChangeListener(new View.OnFocusChangeListener() {
				public void onFocusChange(View v, boolean hasFocus) {
					if (hasFocus) {
						setWriteCount();
						if (initCursorPos == 0)
							edit.setSelection(edit.getText().length(), edit.getText().length());
						else
							edit.setSelection(initCursorPos, initCursorPos);
						initCursorPos = 0;
					}
				}
			});
			edit.setOnClickListener(new View.OnClickListener() {
				public void onClick(View v) {
					// TODO Auto-generated method stub
					setWriteCount();
				}
			});
		}
		
		
		sendButton = (Button)this.findViewById(R.id.okbutton);
		if (sendButton != null) {
			sendButton.setOnClickListener(new View.OnClickListener() {
				public void onClick(View v) {
					onClickSendButton();
				}
			});
		}
		setSendButtonState();
		
		ImageButton imagebutton = (ImageButton)this.findViewById(R.id.imagebutton);
		if (imagebutton != null) {
			imagebutton.setOnClickListener(new View.OnClickListener() {
				public void onClick(View v) {
					onClickImageButton(v);
				}
			});
		}		
		
		if (edit != null && bInit == true) {
			edit.requestFocus();
		}
		setWriteCount();
	}
	

	public void setSelectedMediaFile(String selectedMediaFile) {
		this.selectedMediaFile = selectedMediaFile;
		
		if (selectedMediaFile == null) {
			String msg = this.getString(R.string.message_delete_attchedphoto);
			Toast.makeText(WritePostActivity.this, msg, Toast.LENGTH_SHORT).show();
			mediaType = NONE_TYPE;
		}
		else {
			mediaType = NONE_TYPE;
			String [] imageType = {".jpg", ".png", ".jpeg"};
			String fileName = selectedMediaFile.toLowerCase();
			boolean isPhotoMedia = false;
			for (int i = 0; i < imageType.length; i++) {
				isPhotoMedia = fileName.endsWith(imageType[i]);
				if (isPhotoMedia == true)
					break;
			}
			
			if (isPhotoMedia)
				mediaType = IMAGE_TYPE;
			else {
				boolean isVideoMedia = false;
				
				String [] videoType = {".avi", ".wmv", ".mpg", ".mpeg", ".mov", ".asf", ".mp4", ".skm", ".k3g", ".3gp"};
				for (int i = 0; i < videoType.length; i++) {
					isVideoMedia = fileName.endsWith(videoType[i]);
					if (isVideoMedia == true) {
						break;					
					}
				}
				if (isVideoMedia)
					mediaType = VIDEO_TYPE;
			}
			if (mediaType == NONE_TYPE) {
				popupErrorDialog(this);
				this.selectedMediaFile = null;
				return;
			}
			
		}
		updatePhotoButton();
		
	}
	public String getSelectedMediaFile() {
		return selectedMediaFile;
	}
	public void updatePhotoButton() {
		ImageButton button = (ImageButton)this.findViewById(R.id.imagebutton);
		if (selectedMediaFile != null)
			button.setSelected(true);
		else 
			button.setSelected(false);
	}

	private void setSendButtonState() {
		if (sendButton==null) {
			return;
		}
		String editText = messageText.trim();//edit.getText().toString().trim();
		if (editText.length() == 0 || editText.length() > 150 ) {
			sendButton.setEnabled(false);
		}
		else {
			sendButton.setEnabled(true);
		}		
	}	
	
	static public String getPlainText(String srcText) {
		String linkText = srcText.toString();
		Pattern p = Pattern.compile("\"([^\"]*)\":(http|https)://([\\S]*)");
		Pattern p2 = Pattern.compile("\"([^\"]*)\":(http|https)://([\\S]*)[\\s]");
		
		Matcher m = p.matcher(linkText);
//		boolean bMatch = false;
		while (m.find() && m.groupCount() > 1 ) {
			Matcher m2 = p2.matcher(linkText);
			if (m2.find() && m2.groupCount() > 1) {
				linkText = m2.replaceFirst(m2.group(1));
			}
			else {
				linkText = m.replaceFirst(m.group(1));
			}
			m = p.matcher(linkText);
//			bMatch = true;
		}
//		if (bMatch)
//			Utility.d("Me2dayUIUtility", String.format("getPlainText [%s]", linkText));
		return linkText;
	}
	
	private void setMessageText() {
		messageText = edit.getText().toString();
		messageText = getPlainText(messageText);
	}
	
	private void setWriteCount() {
		if (editcount != null) {
			String editText = messageText;//edit.getText().toString();//.trim();
			editText = editText.trim();

			//removeLinkText();
			
			if (150-editText.length()>=0) {
				editcount.setTextColor(this.getResources().getColor(R.color.solid_write_count));
			}
			else {
				editcount.setTextColor(this.getResources().getColor(R.color.solid_red));
			}
			editcount.setText(String.format("%d", 150-editText.length()));
		}		
	}

    private void showLoadingDialog() {
    	if(loadingDialog == null)
    		loadingDialog = new ProgressDialog(this);
    	
    	loadingDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
    	loadingDialog.setMessage(this.getText(R.string.loading));
    	loadingDialog.setOnKeyListener(new OnKeyListener(){
			
			public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {
				
				if(keyCode == KeyEvent.KEYCODE_SEARCH)
					return true;
				
				else if(keyCode == KeyEvent.KEYCODE_BACK)	{
					return false;
				}
				return false;
			}
		});
    	loadingDialog.show();
    }		

	private void onClickImageButton(View view) {
		
		Dialog dialog;
		if (getSelectedMediaFile() != null) {
			int itemResId = R.array.photoupload_context_menu2;
			dialog = new AlertDialog.Builder(this)
	        .setItems(itemResId, new DialogInterface.OnClickListener() {
	            public void onClick(DialogInterface dialog, int which) {
	            	switch (which) {
	            	case 0:
	            		onClickAlbumButton(true);
	            		break;
	            	case 1:
	            		onClickCameraButton(true);
	            		break;
	            	case 2:
	            		onClickAlbumButton(false);
	            		break;
	            	case 3:
	            		onClickCameraButton(false);
	            		break;
	            	case 4:
	            		setSelectedMediaFile(null);
	            		break;
	            	}
	            }
	        }).create();
		}
		else {
			int itemResId = R.array.photoupload_context_menu;
			dialog = new AlertDialog.Builder(this)
	        .setItems(itemResId, new DialogInterface.OnClickListener() {
	            public void onClick(DialogInterface dialog, int which) {
	            	switch (which) {
	            	case 0:
	            		onClickAlbumButton(true);
	            		break;
	            	case 1:
	            		onClickCameraButton(true);
	            		break;
	            	case 2:
	            		onClickAlbumButton(false);
	            		break;
	            	case 3:
	            		onClickCameraButton(false);
	            		break;
	            	}
	            }
	        }).create();
		}
		dialog.setTitle(this.getResources().getString(R.string.upload));
		/**
         * author daesoo,kim
         * date 2010.3.16
         */
		dialog.setOnKeyListener(new DialogInterface.OnKeyListener(){
			public boolean onKey(DialogInterface dialog, int keyCode,
					KeyEvent event) {
				if(keyCode == KeyEvent.KEYCODE_SEARCH)
					return true;
				return false;
			}
        });
		dialog.show();
	}
	private void onClickAlbumButton(boolean isPhoto) {
		if (isPhoto) {
			Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.INTERNAL_CONTENT_URI);
			this.startActivityForResult(intent, request_album);
		}
		else {
			Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.INTERNAL_CONTENT_URI);
			intent.setType("video/*"); 
			this.startActivityForResult(intent, request_album);
		}
	}
	private void onClickCameraButton(boolean isPhoto) {
		Intent intent = null;
		if (isPhoto) {
			intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
		}
		else {
			intent = new Intent(MediaStore.ACTION_VIDEO_CAPTURE);
		}
		int version = Integer.parseInt(Build.VERSION.SDK);
		switch (version) {
		case 3://Build.VERSION_CODES.CUPCAKE: 	1.5
		case 4://Build.VERSION_CODES.DOUNUT:	1.6
			if (isPhoto) {
				String storage = MediaStore.Images.Media.EXTERNAL_CONTENT_URI.toString();
				intent.putExtra(MediaStore.EXTRA_OUTPUT, storage);
			}
			else {
				String storage = MediaStore.Video.Media.EXTERNAL_CONTENT_URI.toString();
				intent.putExtra(MediaStore.EXTRA_OUTPUT, storage);
			}
			break;
		default:
			File file = null;
			if (isPhoto) {
				file = getPhotoTempFile();
				tempCameraPath = file.getAbsolutePath();
				Utility.d("WritePostActivity", String.format("onClickCameraButton tempCameraPath(%s)", tempCameraPath));
				intent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(file));
			}
			else {
//				file = getVideoTempFile();
//				tempCameraPath = file.getAbsolutePath();
				String storage = MediaStore.Video.Media.EXTERNAL_CONTENT_URI.toString();
				intent.putExtra(MediaStore.EXTRA_OUTPUT, storage);
			}
			break;
		}
		if (isPhoto) 
			this.startActivityForResult(intent, request_photo_camera);
		else
			this.startActivityForResult(intent, request_video_camera);
	}
	private void onClickCancelButton() {
		this.finish();
	}

	public boolean isAvailableExternalMemory() {
		boolean mExternalStorageAvailable = false;
		boolean mExternalStorageWriteable = false;
		String state = Environment.getExternalStorageState();
		if (Environment.MEDIA_MOUNTED.equals(state)) {   
			// We can read and write the media    
			mExternalStorageAvailable = mExternalStorageWriteable = true;
		} else if (Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)) {    
			// We can only read the media    
			mExternalStorageAvailable = true;    
			mExternalStorageWriteable = false;
		} else {    
			// Something else is wrong. It may be one of many other states, but all we need    
			//  to know is we can neither read nor write    
			mExternalStorageAvailable = mExternalStorageWriteable = false;
		}		
		return (mExternalStorageAvailable == true && mExternalStorageWriteable == true);
	}
	
	public File getExternalImagesFolder() {
//		if (mIsCreatedCacheFolder == false)
//			return null;
		if (isAvailableExternalMemory() == false)
			return null;
		
		File folder = new File(Environment.getExternalStorageDirectory().getAbsolutePath() 
				+ "/Android" + "/data" + "/" + getApplicationContext().getPackageName() + "/images");
		return folder;
	}
	
	
	public void createCacheFolder() {
		if (isAvailableExternalMemory() == false)
			return;
		
		File file = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/Android");
		if (file.exists() == false) {
			if (file.mkdir() == false) {
				//mIsCreatedCacheFolder = false;
				//return;
			}
		}
		file = new File(file.getAbsolutePath() + "/data");
		if (file.exists() == false)	{
			if (file.mkdir() == false) {
				//mIsCreatedCacheFolder = false;
				//return;
			}
		}
		file = new File(file.getAbsolutePath() + "/" + getApplicationContext().getPackageName());
		if (file.exists() == false)	{
			if (file.mkdir() == false) {
				//mIsCreatedCacheFolder = false;
				//return;
			}
		}
		File cachefile = new File(file.getAbsolutePath() + "/cache");
		if (cachefile.exists() == false){
			if (cachefile.mkdir() == false) {
				//mIsCreatedCacheFolder = false;
				//return;
			}
		}
		File imageFile = new File(file.getAbsoluteFile() + "/images");
		if (imageFile.exists() == false){
			imageFile.mkdir();
		}
		//mIsCreatedCacheFolder = true;
	}
	private File getPhotoTempFile() {
		File folder = getExternalImagesFolder();
		if (folder != null) {
			if (folder.exists() == false)
				createCacheFolder();
			
			File file = new File(folder.getAbsolutePath(), PHOTO_CAMERA_FILENAME);
			return file;
		}
		else {
			File file = new File(this.getCacheDir().getAbsolutePath(), PHOTO_CAMERA_FILENAME);
			return file;
		}
	}
	
	private boolean isEditing() {
		String message = edit.getText().toString();
		message = message.trim();
		if (message.length() > 0) 
			return true;

		if (selectedMediaFile != null)
			return true;
		
		return false;
	}
	
	private void onClickSendButton() {
		if (edit == null) 
			return;
		
		String message = edit.getText().toString();
		message = message.trim();
		hideKeyboard();

		postPoster = new CreatePostPoster();
		
		HttpParams params = new BasicHttpParams();
		HttpConnectionParams.setConnectionTimeout(params, Me2dayInfo.TIMEOUT);  
		HttpConnectionParams.setSoTimeout(params, Me2dayInfo.TIMEOUT); 
		DefaultHttpClient httpClient = new DefaultHttpClient(params);
		
		postPoster.setBody(message);
		postPoster.setAttachment(this.selectedMediaFile);
		isSendCompleted = false;
		compressCount = 0;
		
		if (selectedMediaFile != null) {
			createProgressDialog();
			onCheckProgress();
			if (mediaType == IMAGE_TYPE) {
				handler.postDelayed(new Runnable() {
					public void run() {
						// TODO Auto-generated method stub
						modifyImageAndSendPost();
					}
					
				}, 100);
			}
		}
		
		HttpRequestBase method = postPoster.createHttpMehtod(CreatePostPoster.create_post(null));
		postPoster.settingHttpClient(method, httpClient);
		
		HttpResponse response = null;
		try {
			response = httpClient.execute(method);
		} catch (ClientProtocolException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		isSendCompleted = true;
		if (progressDialog != null) {
			progressDialog.dismiss();
			progressDialog = null;
		}
		if (customProgressDialog != null) {
			customProgressDialog.dismiss();
			customProgressDialog = null;
		}
		
		InputStream in = null;
		try {
			in = response.getEntity().getContent();
		} catch (IllegalStateException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		
		String xmlMessage = Utility.convertStreamToString(in);
		in = new StringBufferInputStream(xmlMessage);
		
		int responseCode = response.getStatusLine().getStatusCode();
		Utility.d(category, String.format("Response Code = %d", responseCode));
		
		//Http Status Code가 200일때만 성공으로 취급한다. 
		if (responseCode != HttpURLConnection.HTTP_OK) {
			Utility.d(category, String.format("onClickLoginUsingOpenid(), Error(%d, %s) message(%s)", 
					responseCode, response.getStatusLine().getReasonPhrase(), xmlMessage));
			try {
				postPoster.onError(response, in);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			return;
		}
		
		Utility.d(category, xmlMessage);
		try {
			postPoster.onSuccess( response, in );
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		this.finish();
	}
	
	private void modifyImageAndSendPost() {
		Bitmap bitmap = null;
		//Utility.d("WritePostActivity", selectedImageFile);
		
		BitmapFactory.Options options = new BitmapFactory.Options();
		options.inJustDecodeBounds = true;
		bitmap = BitmapFactory.decodeFile(selectedMediaFile, options);
		
		int srcWidth = options.outWidth;
		int srcHeight = options.outHeight;
		
		if (srcWidth*srcHeight > IMAGE_ALLOW_WIDTH*IMAGE_ALLOW_HEIGHT) {

			// SampleSize는 2의 지수승으로 해야 품질 및 성능이 좋음
			int scaleSize = (srcWidth*srcHeight)/(IMAGE_ALLOW_WIDTH*IMAGE_ALLOW_HEIGHT);
			int sampleSize = 1;
			switch(scaleSize)
			{
			case 1:
				sampleSize = 1;
				break;
			case 2:	case 3:
				sampleSize = 1;
				break;
			case 4:	case 5:	case 6: case 7:
				sampleSize = 2;
				break;
			case 8: case 9: case 10: case 11: case 12: case 13: case 14: case 15:
				sampleSize = 4;
				break;
			default:
				sampleSize = 8;
				break;
			}
			
			options = new BitmapFactory.Options();
			options.inSampleSize = sampleSize;
			
			Utility.d("WritePostActivity", String.format("%s, sampleSize : %d", selectedMediaFile, sampleSize));
			bitmap = BitmapFactory.decodeFile(selectedMediaFile, options);
			
			if (bitmap != null) {
				ByteArrayOutputStream stream = new ByteArrayOutputStream();
				
				int newWidth = 0;
				int newHeight = 0;
				Rect resizedRect1 = getRatioSize(bitmap.getWidth(), bitmap.getHeight(), IMAGE_ALLOW_WIDTH, IMAGE_ALLOW_HEIGHT);
				Rect resizedRect2 = getRatioSize(bitmap.getWidth(), bitmap.getHeight(), IMAGE_ALLOW_HEIGHT, IMAGE_ALLOW_WIDTH);
				
				if (resizedRect1.width()*resizedRect1.height() > resizedRect2.width()*resizedRect2.height()) {
					newWidth = resizedRect1.width();
					newHeight = resizedRect1.height();
				}
				else {
					newWidth = resizedRect2.width();
					newHeight = resizedRect2.height();
				}
					
				Utility.d("WritePostActivity", String.format("Image Width:%d, Height:%d, Resize (%d, %d)", bitmap.getWidth(), bitmap.getHeight(), newWidth, newHeight));
				if (newWidth > 0 && newHeight > 0) {
					Bitmap newBitmap = Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true);
					File externCacheFolder = getExternalImagesFolder();
					File resizedFile = null;
					if (externCacheFolder != null) {
						if (externCacheFolder.exists() == false)
							createCacheFolder();
						
						resizedFile = new File(externCacheFolder.getAbsolutePath(), PHOTO_RESIZE_FILENAME);
					}
					else {
						resizedFile = new File(this.getCacheDir().getAbsolutePath(), PHOTO_RESIZE_FILENAME);
					}
					FileOutputStream fos = null;
					try {
						resizedFile.createNewFile();
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					try {
						fos = new FileOutputStream(resizedFile);
					} catch (FileNotFoundException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					newBitmap.compress(Bitmap.CompressFormat.JPEG, 80, fos);
					postPoster.setAttachment(resizedFile.getAbsolutePath());
					//ExifInterface newExif = null;	
				}
				bitmap.recycle();
			}
			else {
				Utility.d("WritePostActivity", "decode fail");
			}
		}
	}

	public static Rect getRatioSize(int imageWidth, int imageHeight, int maxWidth, int maxHeight) {
		int newWidth = 0;
		int newHeight = 0;
		float fRatioW = (float)maxWidth / (float)imageWidth;
		float fRatioH = (float)maxHeight / (float)imageHeight;

		// 더 많이 넘어간 놈을 기준으로 해서 처리하도록 한다.
		if ( fRatioW>fRatioH )
		{
			// 가로 간격이 더 넓으므로 세로를 더 조금 늘려서 맞출수 있다.
			newHeight = maxHeight;
			newWidth = (int)(fRatioH * imageWidth);
		}
		else
		{
			newWidth = maxWidth;
			newHeight = (int)(fRatioW * imageHeight);
		}
		return new Rect(0, 0, newWidth, newHeight);
	}
	
	private void showDialog2(int id) {
		switch(id) {
        case PROGRESS_DIALOG:
        	createProgressDialog();
            break;

        case SPINNER_PROGRESS_DIALOG:
            progressDialog = new ProgressDialog(WritePostActivity.this);    
            progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
            progressDialog.setMessage(this.getString(R.string.message_uploading_post));
            progressDialog.setCancelable(false);
    		/**
             * author daesoo,kim
             * date 2010.3.16
             */
    		progressDialog.setOnKeyListener(new DialogInterface.OnKeyListener(){
    			public boolean onKey(DialogInterface dialog, int keyCode,
    					KeyEvent event) {
    				if(keyCode == KeyEvent.KEYCODE_SEARCH)
    					return true;
    				return false;
    			}
            });
            progressDialog.show();
            break;
        }
		
	}
	private void createProgressDialog() {
		customProgressDialog = new Dialog(this);
		customProgressDialog.setTitle(R.string.message_uploading_post);
		customProgressDialog.setContentView(R.layout.upload_progress);
		ProgressBar progress = (ProgressBar)customProgressDialog.findViewById(R.id.progress);
		progress.setMax(100);		
		customProgressDialog.setCancelable(false);
		/**
         * author daesoo,kim
         * date 2010.3.16
         */
		customProgressDialog.setOnKeyListener(new DialogInterface.OnKeyListener(){
			public boolean onKey(DialogInterface dialog, int keyCode,
					KeyEvent event) {
				if(keyCode == KeyEvent.KEYCODE_SEARCH)
					return true;
				return false;
			}
        });
		customProgressDialog.show();
		
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		int version = 0;
		switch (requestCode) {
		case request_album:
			onResultAlbum(resultCode, data);
			break;
		case request_photo_camera:
			version = Integer.parseInt(Build.VERSION.SDK);
			switch (version) {
			case 3://Build.VERSION_CODES.CUPCAKE:
			case 4:
				onResultAlbum(resultCode, data);
				break;
			default:
				onResultCamera(resultCode, data);
				break;
			}
			break;
		case request_video_camera:
			version = Integer.parseInt(Build.VERSION.SDK);
			switch (version) {
			case 3://Build.VERSION_CODES.CUPCAKE:
			case 4:
				onResultAlbum(resultCode, data);
				break;
			default:
				onResultAlbum(resultCode, data);
				//onResultCamera(resultCode, data, false);
				break;
			}
			break;
		}
	}

	private void onResultAlbum(int resultCode, Intent data) {
		if (resultCode == RESULT_OK) {
			Uri uri = data.getData();
			if (uri != null) {
				String [] proj={MediaStore.Images.Media.DATA};
	
	            Cursor cursor = managedQuery(uri, proj, null, null, null);  
	            int column_index = cursor.getColumnIndexOrThrow(proj[0]);
	            cursor.moveToNext(); 
	            String filePath = cursor.getString(column_index);
				this.setSelectedMediaFile(filePath);
				Utility.d("WritePostActivity", String.format("onResultAlbum url(%s)", filePath));
			}
		}
	}
	private void onResultCamera(int resultCode, Intent data) {
		if (resultCode == RESULT_OK) {
			Utility.d("WritePostActivity", "onResultAlbum RESULT_OK");
			if (tempCameraPath != null) {
				this.setSelectedMediaFile(tempCameraPath);
				Utility.d("WritePostActivity", String.format("onResultAlbum url(%s)", tempCameraPath));
			}
			else {
				// 모토로이 카메라로 사진 찍은 후 사진 첨부 안되는 문제 수정
				tempCameraPath = getPhotoTempFile().getAbsolutePath();
				this.setSelectedMediaFile(tempCameraPath);
				Utility.d("WritePostActivity", String.format("onResultAlbum2 url(%s)", tempCameraPath));
			}
		}
	}
	public void afterTextChanged(Editable s) {
		
	}

	public void beforeTextChanged(CharSequence s, int start, int count,
			int after) {
		
	}

	public void onTextChanged(CharSequence s, int start, int before, int count) {
		setMessageText();
		setWriteCount();
		setSendButtonState();
	}
	
	public void onCheckProgress() {
		if (postPoster != null) {
			if (customProgressDialog != null) {
				if (postPoster.getTotalFileLength() != 0) {
					ProgressBar progress = (ProgressBar)customProgressDialog.findViewById(R.id.progress);
					long rate = (long)((postPoster.getSendigFileLength()*(100-compressCount))/postPoster.getTotalFileLength()) + compressCount;
					progress.setProgress((int)rate);
				}
				else if (compressCount < 50 && mediaType == IMAGE_TYPE) {
					ProgressBar progress = (ProgressBar)customProgressDialog.findViewById(R.id.progress);
					compressCount += 5;
					progress.setProgress(compressCount);
				}
				if (isSendCompleted == false) {
					handler.postDelayed(new Runnable() {
						public void run() {
							onCheckProgress();
						}
					}, 10);
				}
			}
		}
	}
}
