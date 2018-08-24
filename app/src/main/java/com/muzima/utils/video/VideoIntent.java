/*
 * Copyright (c) 2014 - 2018. The Trustees of Indiana University, Moi University
 * and Vanderbilt University Medical Center.
 *
 * This version of the code is licensed under the MPL 2.0 Open Source license
 * with additional health care disclaimer.
 * If the user is an entity intending to commercialize any application that uses
 *  this code in a for-profit venture,please contact the copyright holder.
 */

package com.muzima.utils.video;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.os.Bundle;
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

import java.io.File;

import static com.muzima.utils.Constants.APP_VIDEO_DIR;

public class VideoIntent extends Activity {

	public static final String KEY_VIDEO_PATH = "videoPath";
    public static final String KEY_VIDEO_CAPTION = "videoCaption";
    public static final String KEY_SECTION_NAME = "sectionName";

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
        String videoPath = i.getStringExtra(KEY_VIDEO_PATH);
        mBinaryDescription  = i.getStringExtra(KEY_VIDEO_CAPTION);
        mSectionName = i.getStringExtra(KEY_SECTION_NAME);

        // we are not using formUuid in the media path anymore
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
        
        mNoVideoMessage = findViewById(R.id.noVideoMessage);
        mVideoPreview = findViewById(R.id.videoPreview);
        mVideoCaption = findViewById(R.id.videoCaption);
        mVideoThumbnail = findViewById(R.id.videoThumbnail);
        mVideoAcceptContainer = findViewById(R.id.videoAcceptContainer);
        mVideoRecordContainer = findViewById(R.id.videoRecordContainer);
        
        refreshVideoView();
	}
	
	public void acceptVideo(View view) {
    	String caption = mVideoCaption.getText().toString();
    	
    	if (caption == null || caption.length() < 1){
    		Toast.makeText(getApplicationContext(),getString(R.string.hint_video_caption_prompt), Toast.LENGTH_SHORT).show();
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
			deleteVideo();
			
		mBinaryName=null;
		
		refreshVideoView();
	}
	
	public void recordVideo(View view) {
		isNewVideo = true;
		Intent i = new Intent(MediaStore.ACTION_VIDEO_CAPTURE);
		i.putExtra(MediaStore.EXTRA_OUTPUT, Video.Media.EXTERNAL_CONTENT_URI.toString());
		try {
            int VIDEO_RECORD = 1;
            startActivityForResult(i, VIDEO_RECORD);
		} catch (ActivityNotFoundException e) {
			Toast.makeText(this,getString(R.string.info_video_record_activity_unavailable), Toast.LENGTH_SHORT).show();
		}		
	}
	
	public void chooseVideo(View view) {
		isNewVideo = false;
        Intent i;
		i = new Intent(Intent.ACTION_PICK, MediaStore.Video.Media.EXTERNAL_CONTENT_URI);

        try {
		    i.setType("video/*");
            int VIDEO_CHOOSE = 2;
            startActivityForResult(i, VIDEO_CHOOSE);
		} catch (ActivityNotFoundException e) {
			Toast.makeText(this,getString(R.string.info_video_chose_activity_unavailable), Toast.LENGTH_SHORT).show();
		}
	}
	
	public void playVideo(View view) {
		Intent i = new Intent("android.intent.action.VIEW");
		File f = new File(VIDEO_FOLDER + File.separator + mBinaryName);
		i.setDataAndType(Uri.fromFile(f), "video/*");
		try {
			startActivity(i);
		} catch (ActivityNotFoundException e) {
			Toast.makeText(VideoIntent.this,getString(R.string.info_video_play_activity_unavailable), Toast.LENGTH_SHORT).show();
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

            // show caption view
            mVideoCaption.setVisibility(View.VISIBLE);
            
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

            // hide caption view
            mVideoCaption.setVisibility(View.GONE);
            
            //show record view
            mVideoRecordContainer.setVisibility(View.VISIBLE);
            
            //show no message view
            mNoVideoMessage.setVisibility(View.VISIBLE);
		}
	}

    private void deleteVideo() {
    	//delete from media provider
        int del = MediaUtils.deleteVideoFileFromMediaProvider(this, VIDEO_FOLDER + File.separator + mBinaryName);
    	Log.i(getClass().getSimpleName(), "Deleted " + del + " rows from media content provider");
    }

	private String getPathFromUri(Uri uri) {
		if (uri.toString().startsWith("file"))
			return uri.toString().substring(6);
		else {
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
				if (c != null)
					c.close();
			}
		}
	}

	private void saveVideo(Object videoUri) {
		// you are replacing an answer. remove the media.
		if (mBinaryName != null)
			deleteVideo();

		// get the file path and create a copy in the instance folder
		String videoPath = getPathFromUri((Uri) videoUri);
		String extension = videoPath.substring(videoPath.lastIndexOf("."));
		String destVideoPath = VIDEO_FOLDER + File.separator + System.currentTimeMillis() + extension;

		File source = new File(videoPath);
		File newVideo = new File(destVideoPath);
		if (MediaUtils.folderExists(VIDEO_FOLDER))
            MediaUtils.copyFile(source, newVideo);

		if (newVideo.exists())
		    mBinaryName = newVideo.getName();
		else
			Log.e(getClass().getSimpleName(), "Inserting Video file FAILED");

	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        if (intent != null) {
            Uri videoUri = intent.getData();
            saveVideo(videoUri);
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
