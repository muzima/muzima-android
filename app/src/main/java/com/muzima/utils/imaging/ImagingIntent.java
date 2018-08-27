/*
 * Copyright (c) 2014 - 2018. The Trustees of Indiana University, Moi University
 * and Vanderbilt University Medical Center.
 *
 * This version of the code is licensed under the MPL 2.0 Open Source license
 * with additional health care disclaimer.
 * If the user is an entity intending to commercialize any application that uses
 *  this code in a for-profit venture,please contact the copyright holder.
 */

package com.muzima.utils.imaging;

import android.content.ActivityNotFoundException;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.provider.MediaStore.Images;
import android.util.Log;
import android.view.Display;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;
import com.muzima.R;
import com.muzima.utils.MediaUtils;
import com.muzima.view.BaseActivity;

import java.io.File;

import static com.muzima.utils.Constants.APP_IMAGE_DIR;
import static com.muzima.utils.Constants.TMP_FILE_PATH;

public class ImagingIntent extends BaseActivity {

    private static final int IMAGE_CAPTURE = 1;
    private static final int IMAGE_CHOOSE = 2;
    
    public static final String KEY_IMAGE_PATH = "imagePath";
    public static final String KEY_IMAGE_CAPTION = "imageCaption";
    public static final String KEY_SECTION_NAME = "sectionName";
    private String IMAGE_FOLDER;

    private ImageView mImagePreview;
    private EditText mImageCaption;
//    private View mCaptionContainer;
    private View mImageAcceptContainer;
    private View mImageCaptureContainer;
    
    private String mSectionName;
    private String mBinaryName;
    private String mBinaryDescription;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_imaging);
        Intent i = getIntent();
        String imagePath = i.getStringExtra(KEY_IMAGE_PATH);
        mBinaryDescription  = i.getStringExtra(KEY_IMAGE_CAPTION);
        mSectionName = i.getStringExtra(KEY_SECTION_NAME);

        // we are not using formUuid in the media path anymore
        IMAGE_FOLDER = APP_IMAGE_DIR;

        if (savedInstanceState != null) {
            if (savedInstanceState.containsKey(KEY_IMAGE_PATH))
                mBinaryName = savedInstanceState.getString(KEY_IMAGE_PATH);

            if (savedInstanceState.containsKey(KEY_IMAGE_CAPTION))
                mBinaryDescription = savedInstanceState.getString(KEY_IMAGE_CAPTION);

            if (savedInstanceState.containsKey(KEY_SECTION_NAME))
                mSectionName = savedInstanceState.getString(KEY_SECTION_NAME);

        } else {
            if (imagePath != null) {
                File image = new File(imagePath);
                if (image.exists())
                    mBinaryName = image.getName();
            }
        }

        mImagePreview = findViewById(R.id.imagePreview);
        mImageCaption = findViewById(R.id.imageCaption);
        mImageAcceptContainer =  findViewById(R.id.imageAcceptContainer);
        mImageCaptureContainer = findViewById(R.id.imageCaptureContainer);

        refreshImageView();
    }
    public void acceptImage(View view) {
        String caption = mImageCaption.getText().toString();

        if (caption == null || caption.length() < 1){
            Toast.makeText(getApplicationContext(),getString(R.string.hint_image_caption_prompt), Toast.LENGTH_SHORT).show();
            return;
        }

        String imageUri = IMAGE_FOLDER + File.separator + mBinaryName;
        if (mBinaryName != null) {
            Intent i = new Intent();
            i.putExtra(KEY_SECTION_NAME, mSectionName);
            i.putExtra(KEY_IMAGE_PATH, imageUri);
            i.putExtra(KEY_IMAGE_CAPTION, caption);
            setResult(RESULT_OK, i);
        }
        finish();
    }

    public void rejectImage(View view) {
        mBinaryName=null;
        mImagePreview.setImageResource(R.drawable.user_pic);
        mImagePreview.getLayoutParams().height = 150;
        mImagePreview.getLayoutParams().width = 120;
        refreshImageView();
    }

    public void captureImage(View view) {
        Intent i = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);

        i.putExtra(android.provider.MediaStore.EXTRA_OUTPUT,
                Uri.fromFile(new File(TMP_FILE_PATH)));
        try {
            startActivityForResult(i,IMAGE_CAPTURE);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(getApplicationContext(),getString(R.string.error_image_capture_activity_unavailable), Toast.LENGTH_SHORT).show();
        }
    }

    public void chooseImage(View view) {
        Intent i;
        i = new Intent(Intent.ACTION_PICK,android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);

        try {
            i.setType("image/*");
            startActivityForResult(i, IMAGE_CHOOSE);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(getApplicationContext(),getString(R.string.error_image_chose_activity_unavailable), Toast.LENGTH_SHORT).show();
        }
    }
    
    @Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putString(KEY_SECTION_NAME, mSectionName);
		outState.putString(KEY_IMAGE_PATH, mBinaryName);
		outState.putString(KEY_IMAGE_CAPTION, mBinaryDescription);
	}

	private void refreshImageView() {

        // Only add the imageView if the user has taken a picture
        if (mBinaryName != null) {
        	mImagePreview.setAdjustViewBounds(true);
        	resizeImageView();
            Display display =
                ((WindowManager) getApplicationContext().getSystemService(Context.WINDOW_SERVICE))
                        .getDefaultDisplay();
            int screenWidth = display.getWidth();
            int screenHeight = display.getHeight();

            File f = new File(IMAGE_FOLDER + File.separator + mBinaryName);

            if (f.exists()) {
                Bitmap bmp = MediaUtils.getBitmapScaledToDisplay(f, screenHeight, screenWidth);
                mImagePreview.setImageBitmap(bmp);
            } else {
                mImagePreview.setImageBitmap(null);
            }

            mImagePreview.setPadding(10, 10, 10, 10);
            mImagePreview.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent i = new Intent("android.intent.action.VIEW");
                    String[] projection = {"_id"};
                    Cursor c = getApplicationContext().getContentResolver()
                                .query(Images.Media.EXTERNAL_CONTENT_URI,
                                    projection, "_data='" + IMAGE_FOLDER + File.separator + mBinaryName + "'", null, null);
                    if (c.getCount() > 0) {
                        c.moveToFirst();
                        String id = c.getString(c.getColumnIndex("_id"));

                        Log.i(getClass().getSimpleName(), "setting view path to: "  + Uri.withAppendedPath(
                                        Images.Media.EXTERNAL_CONTENT_URI, id));

                        i.setDataAndType(Uri.withAppendedPath(
                            Images.Media.EXTERNAL_CONTENT_URI, id), "image/*");
                        try {
                            ImagingIntent.this.startActivity(i);
                        } catch (ActivityNotFoundException e) {
                            Toast.makeText(getApplicationContext(),getString(R.string.error_image_view_activity_unavailable), Toast.LENGTH_SHORT).show();
                        }
                    }
                    c.close();
                }
            });
            // show accept view
            mImageAcceptContainer.setVisibility(View.VISIBLE);

            // show caption view
            mImageCaption.setVisibility(View.VISIBLE);

            //hide capture view
            mImageCaptureContainer.setVisibility(View.GONE);

            if (mBinaryDescription != null)
                mImageCaption.setText(mBinaryDescription);

        } else {
            //no image do allot here
            // show accept view
            mImageAcceptContainer.setVisibility(View.GONE);

            // show caption view
            mImageCaption.setVisibility(View.GONE);

            //hide capture view
            mImageCaptureContainer.setVisibility(View.VISIBLE);
        }
    }

	@Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);

        if (resultCode == RESULT_CANCELED) {
            // request was canceled, so do nothing
            return;
        }

        ContentValues values;
        Uri imageURI;
        String destImagePath = IMAGE_FOLDER + File.separator + System.currentTimeMillis();

        switch (requestCode) {
            case IMAGE_CAPTURE:
                File tmpCapturedImage = new File(TMP_FILE_PATH);

                File capturedImage = new File(destImagePath);

                if (MediaUtils.folderExists(IMAGE_FOLDER)) {
                    if (!tmpCapturedImage.renameTo(capturedImage))
                        Log.e(getClass().getSimpleName(), "Failed to rename " + tmpCapturedImage.getAbsolutePath());
                    else {
                        Log.i(getClass().getSimpleName(), "renamed " + tmpCapturedImage.getAbsolutePath() + " to " + capturedImage.getAbsolutePath());
                        mBinaryName = capturedImage.getName();
                        refreshImageView();
                    }
                }

                break;
            case IMAGE_CHOOSE:
                String sourceImagePath = null;
                Uri selectedImage = intent.getData();
                if (selectedImage.toString().startsWith("file"))
                    sourceImagePath = selectedImage.toString().substring(6);
                else {
                    String[] projection = {MediaStore.Images.Media.DATA};
                    Cursor cursor = managedQuery(selectedImage, projection, null, null, null);
                    startManagingCursor(cursor);
                    int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
                    cursor.moveToFirst();
                    sourceImagePath = cursor.getString(column_index);
                }

                // Copy file to sdcard
                File source = new File(sourceImagePath);
                File chosenImage = new File(destImagePath);
                if (MediaUtils.folderExists(IMAGE_FOLDER))
                	MediaUtils.copyFile(source, chosenImage);
                
                if (chosenImage.exists()) {
                	mBinaryName = chosenImage.getName();
                    refreshImageView();
                } else {
                    Log.e(getClass().getSimpleName(), "NO IMAGE EXISTS at: " + source.getAbsolutePath());
                }
                break;
        }
    }
    
    private void resizeImageView() {
        int width, height;
        Display display = getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        width = size.x;
        height = size.y;

        // Calculate image sizes
        if (height > width) {
            mImagePreview.getLayoutParams().height = (int) (width * 0.7);
            mImagePreview.getLayoutParams().width = (int) (width * 0.7);
        } else {
            mImagePreview.getLayoutParams().height = (int) (height * 0.58);
            mImagePreview.getLayoutParams().width = (int) (height * 0.58);
        }
    }
}