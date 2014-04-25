package com.muzima.utils.video;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.provider.MediaStore.Video;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import com.muzima.R;
import com.muzima.utils.MediaUtils;
import com.muzima.view.forms.VideoComponent;

import static com.muzima.utils.Constants.APP_VIDEO_DIR;

public class VideoIntent extends Activity {
	private final static String TAG = "VideoIntent";

	public static final String KEY_VIDEO_PATH = "videoPath";
    public static final String KEY_VIDEO_CAPTION = "videoCaption";
    public static final String KEY_SECTION_NAME = "sectionName";

	private final int VIDEO_CAPTURE = 1;
	private final int VIDEO_CHOOSER = 2;
	
    private String VIDEO_FOLDER;
    private boolean isNewVideo;

    private TextView mNoVideoMessage;
    private ImageView mVideoThumbnail;
    private EditText mVideoCaption;
    private View mVideoPreview;
    private View mVideoAcceptContainer;
    private View mVideoRecordContainer;

    private String mSectionName;
    private String mBinaryName;
    private String mBinaryDescription;

	public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video);
        
        Intent i = getIntent();
        String formUuid = i.getStringExtra(VideoComponent.FORM_UUID);
        String videoPath = i.getStringExtra(KEY_VIDEO_PATH);
        mBinaryDescription  = i.getStringExtra(KEY_VIDEO_CAPTION);
        mSectionName = i.getStringExtra(KEY_SECTION_NAME);

        if (formUuid != null)
        	VIDEO_FOLDER = APP_VIDEO_DIR + "/" + formUuid;
        else
        	VIDEO_FOLDER = APP_VIDEO_DIR;

        if (savedInstanceState != null) {
            if (savedInstanceState.containsKey(KEY_VIDEO_PATH))
                mBinaryName = savedInstanceState.getString(KEY_VIDEO_PATH);
        } else {
        	if (videoPath != null) {
	            File video = new File(videoPath);
	            if (video.exists())
	                mBinaryName = video.getName();
        	}
        }

        if (savedInstanceState != null) {
            if (savedInstanceState.containsKey(KEY_VIDEO_PATH))
                mBinaryName = savedInstanceState.getString(KEY_VIDEO_PATH);

            if (savedInstanceState.containsKey(KEY_VIDEO_CAPTION))
                mBinaryDescription = savedInstanceState.getString(KEY_VIDEO_CAPTION);

            if (savedInstanceState.containsKey(KEY_SECTION_NAME))
                mSectionName = savedInstanceState.getString(KEY_SECTION_NAME);

        } else {
            if (videoPath != null) {
                File video = new File(videoPath);
                if (video.exists()) {
                    mBinaryName = video.getName();
                    isNewVideo = false;
                }
            }
        }
        
        mNoVideoMessage = (TextView) findViewById(R.id.noVideoMessage);
        mVideoPreview = (View) findViewById(R.id.videoPreview);
        mVideoCaption = (EditText) findViewById(R.id.videoCaption);
        mVideoThumbnail = (ImageView) findViewById(R.id.videoThumbnail);
        mVideoAcceptContainer = (View) findViewById(R.id.videoAcceptContainer);
        mVideoRecordContainer = (View) findViewById(R.id.videoRecordContainer);
        
        refreshVideoView();
	}
	
	public void acceptVideo(View view) {
    	String caption = mVideoCaption.getText().toString();
    	
    	if (caption == null || caption.length() < 1){
    		Toast.makeText(getApplicationContext(),"Please enter a caption for the video", Toast.LENGTH_SHORT).show();
    		return;
    	}
    	
    	String videoUri = VIDEO_FOLDER + File.separator  + mBinaryName;

        if (mBinaryName != null) {
            Intent i = new Intent();
            i.putExtra(KEY_SECTION_NAME, mSectionName);
            i.putExtra(KEY_VIDEO_PATH, videoUri);
            i.putExtra(KEY_VIDEO_CAPTION, caption);
            setResult(RESULT_OK, i);
        }
        finish();
	}
	
	public void rejectVideo(View view) {
		if (isNewVideo) 
			deleteMedia();
			
		mBinaryName=null;
		
		refreshVideoView();
	}
	
	public void recordVideo(View view) {
		isNewVideo = true;
		Intent i = new Intent(MediaStore.ACTION_VIDEO_CAPTURE);
		i.putExtra(MediaStore.EXTRA_OUTPUT, Video.Media.EXTERNAL_CONTENT_URI.toString());
		try {
			startActivityForResult(i, VIDEO_CAPTURE);
		} catch (ActivityNotFoundException e) {
			Toast.makeText(this,"activity_not_found - record video", Toast.LENGTH_SHORT).show();
		}		
	}
	
	public void chooseVideo(View view) {
		isNewVideo = false;
        Intent i;
        final boolean isKitKat = Build.VERSION.SDK_INT >= 19;

        if (isKitKat)
            i = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        else
            i = new Intent(Intent.ACTION_GET_CONTENT);

        try {
		    i.setType("video/*");
			startActivityForResult(i,VIDEO_CHOOSER);
		} catch (ActivityNotFoundException e) {
			Toast.makeText(this,"activity_not_found - choose video", Toast.LENGTH_SHORT).show();
		}
	}
	
	public void playVideo(View view) {
		Intent i = new Intent("android.intent.action.VIEW");
		File f = new File(VIDEO_FOLDER + File.separator + mBinaryName);
		i.setDataAndType(Uri.fromFile(f), "video/*");
		try {
			startActivity(i);
		} catch (ActivityNotFoundException e) {
			Toast.makeText(VideoIntent.this,"activity_not_found - play video", Toast.LENGTH_SHORT).show();
		}
	}
	
	private void refreshVideoView() {
		if (mBinaryName != null) {
			File f = new File(VIDEO_FOLDER + File.separator + mBinaryName);
			Bitmap thumbnail = ThumbnailUtils.createVideoThumbnail(f.getAbsolutePath(), Video.Thumbnails.MICRO_KIND);
			mVideoThumbnail.setImageBitmap(thumbnail);
			
			// show preview with thumbnail view
            mVideoPreview.setVisibility(View.VISIBLE);
            
            // show accept view
            mVideoAcceptContainer.setVisibility(View.VISIBLE);
            
            //hide record view
            mVideoRecordContainer.setVisibility(View.GONE);
            
            //hide no message view
            mNoVideoMessage.setVisibility(View.GONE);

            if (mBinaryDescription != null)
                mVideoCaption.setText(mBinaryDescription);
		} else {
			mVideoThumbnail.setImageBitmap(null);
			
			// hide preview with thumbnail view
            mVideoPreview.setVisibility(View.GONE);
            
            // hide accept view
            mVideoAcceptContainer.setVisibility(View.GONE);
            
            //show record view
            mVideoRecordContainer.setVisibility(View.VISIBLE);
            
            //show no message view
            mNoVideoMessage.setVisibility(View.VISIBLE);
		}
	}

    private void deleteMedia() {
    	//delete from media provider
        int del = MediaUtils.deleteVideoFileFromMediaProvider(this, VIDEO_FOLDER + File.separator + mBinaryName);
    	Log.i(TAG, "Deleted " + del + " rows from media content provider");
    }

	private String getPathFromUri(Uri uri) {
		if (uri.toString().startsWith("file")) {
			return uri.toString().substring(6);
		} else {
			String[] videoProjection = { Video.Media.DATA };
			Cursor c = null;
			try {
				c = getContentResolver().query(uri,
						videoProjection, null, null, null);
				int column_index = c.getColumnIndexOrThrow(Video.Media.DATA);
				String videoPath = null;
				if (c.getCount() > 0) {
					c.moveToFirst();
					videoPath = c.getString(column_index);
				}
				return videoPath;
			} finally {
				if (c != null) {
					c.close();
				}
			}
		}
	}

	public void setBinaryData(Object binaryUri) {
		// you are replacing an answer. remove the media.
		if (mBinaryName != null) {
			deleteMedia();
		}

		// get the file path and create a copy in the instance folder
		String binaryPath = getPathFromUri((Uri) binaryUri);
		String extension = binaryPath.substring(binaryPath.lastIndexOf("."));
		String destVideoPath = VIDEO_FOLDER + File.separator + System.currentTimeMillis() + extension;

		File source = new File(binaryPath);
		File newVideo = new File(destVideoPath);
		if (MediaUtils.folderExists(VIDEO_FOLDER))
            MediaUtils.copyFile(source, newVideo);

		if (newVideo.exists()) {
			ContentValues values = new ContentValues(6);
			values.put(Video.Media.TITLE, newVideo.getName());
			values.put(Video.Media.DISPLAY_NAME, newVideo.getName());
			values.put(Video.Media.DATE_ADDED, System.currentTimeMillis());
			values.put(Video.Media.DATA, newVideo.getAbsolutePath());

			Uri VideoURI = getContentResolver().insert(
					Video.Media.EXTERNAL_CONTENT_URI, values);
			Log.i(TAG, "Inserting VIDEO returned uri = " + VideoURI.toString());
		} else {
			Log.e(TAG, "Inserting Video file FAILED");
		}

		mBinaryName = newVideo.getName();
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        if (intent != null) {
            Uri media = intent.getData();
            setBinaryData(media);
            refreshVideoView();
        }
	}
	
    @Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
        outState.putString(KEY_SECTION_NAME, mSectionName);
        outState.putString(KEY_VIDEO_PATH, mBinaryName);
        outState.putString(KEY_VIDEO_CAPTION, mBinaryDescription);
	}
}
