package com.muzima.utils.imaging;

import android.annotation.SuppressLint;
import android.content.ActivityNotFoundException;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore.Images;
import android.util.Log;
import android.view.Display;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;
import com.actionbarsherlock.R;
import com.muzima.utils.ImageUtils;
import com.muzima.view.BaseActivity;

import java.io.*;
import java.nio.channels.FileChannel;

public class ImagingIntent extends BaseActivity {
	private final static String t = "ImagingIntent";
    
    public static final int IMAGE_CAPTURE = 1;
    public static final int IMAGE_CHOOSER = 2;
    
    public static final String KEY_IMAGE_PATH = "imagepath";

    private Button mChooseButton;
    private Button mCaptureButton;
    private Button mSaveImageButton;
    private ImageView mImagePreview;
    
    private String mBinaryName;

    private final String mInstanceFolderRoot =  Environment.getExternalStorageDirectory().getPath();
    private final String mInstanceFolder = mInstanceFolderRoot + "/thaiya";
    private final String TMPFILE_PATH = mInstanceFolder + "/.cache";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_imaging);
        
        if (savedInstanceState != null) {
            if (savedInstanceState.containsKey(KEY_IMAGE_PATH)) {
                mBinaryName = savedInstanceState.getString(KEY_IMAGE_PATH);
            }
        }
        
        mSaveImageButton = (Button) findViewById(R.id.saveImage);
        mImagePreview = (ImageView) findViewById(R.id.imagePreview);
        mCaptureButton = (Button) findViewById(R.id.imageCapture);
        mCaptureButton.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				
				Intent i = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
                // We give the camera an absolute filename/path where to put the
                // picture because of bug:
                // http://code.google.com/p/android/issues/detail?id=1480
                // The bug appears to be fixed in Android 2.0+, but as of feb 2,
                // 2010, G1 phones only run 1.6. Without specifying the path the
                // images returned by the camera in 1.6 (and earlier) are ~1/4
                // the size. boo.

                // if this gets modified, the onActivityResult in
                // FormEntyActivity will also need to be updated.
                i.putExtra(android.provider.MediaStore.EXTRA_OUTPUT,
                    Uri.fromFile(new File(TMPFILE_PATH)));
                try {
                    startActivityForResult(i,IMAGE_CAPTURE);
                } catch (ActivityNotFoundException e) {
                    Toast.makeText(getApplicationContext(),"image capture", Toast.LENGTH_SHORT).show();
                }
			}
		});
        
        // setup chooser button
        mChooseButton = (Button) findViewById(R.id.chooseImage);

        // launch capture intent on click
        mChooseButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(Intent.ACTION_GET_CONTENT);
                i.setType("image/*");

                try {
                    startActivityForResult(i,IMAGE_CHOOSER);
                } catch (ActivityNotFoundException e) {
                    Toast.makeText(getApplicationContext(),"Activity not found choose image",
                        Toast.LENGTH_SHORT).show();
                }

            }
        });
        
        // launch capture intent on click
        mSaveImageButton.setOnClickListener(new OnClickListener() {
        	@Override
        	public void onClick(View v) {
        		String imageUri = mInstanceFolder + "/" + mBinaryName;
                if (mBinaryName != null) {
                    Intent i = new Intent();
                    i.putExtra("IMAGE_URI", imageUri);
                    setResult(RESULT_OK, i);
                }
                finish();
        	}
        });
        
        refreshImageView();
    }
    
    @Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putString(KEY_IMAGE_PATH, mBinaryName);
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

            File f = new File(mInstanceFolder + "/" + mBinaryName);

            if (f.exists()) {
                Bitmap bmp = ImageUtils.getBitmapScaledToDisplay(f, screenHeight, screenWidth);
                mImagePreview.setImageBitmap(bmp);
            } else {
                mImagePreview.setImageBitmap(null);
            }

            mImagePreview.setPadding(10, 10, 10, 10);
            mImagePreview.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent i = new Intent("android.intent.action.VIEW");
                    String[] projection = {
                        "_id"
                    };
                    Cursor c =
                        getApplicationContext().getContentResolver()
                                .query(
                                    Images.Media.EXTERNAL_CONTENT_URI,
                                    projection, "_data='" + mInstanceFolder + "/" + mBinaryName + "'",
                                    null, null);
                    if (c.getCount() > 0) {
                        c.moveToFirst();
                        String id = c.getString(c.getColumnIndex("_id"));

                        Log.i(
                            t,
                            "setting view path to: "
                                    + Uri.withAppendedPath(
                                        Images.Media.EXTERNAL_CONTENT_URI,
                                        id));

                        i.setDataAndType(Uri.withAppendedPath(
                            Images.Media.EXTERNAL_CONTENT_URI, id),
                            "image/*");
                        try {
                            ImagingIntent.this.startActivity(i);
                        } catch (ActivityNotFoundException e) {
                            Toast.makeText(getApplicationContext(),"activity_not_found view image",
                                Toast.LENGTH_SHORT).show();
                        }
                    }else
                    	System.out.println("c.getcount = 0");
                    c.close();
                }
            });
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
        switch (requestCode) {
            case IMAGE_CAPTURE:
                File newImage = new File(TMPFILE_PATH);

                String s = mInstanceFolder + "/" + System.currentTimeMillis() + ".jpg";

                File nf = new File(s);
                if (!newImage.renameTo(nf)) {
                    Log.e(t, "Failed to rename " + newImage.getAbsolutePath());
                } else {
                    Log.i(t, "renamed " + newImage.getAbsolutePath() + " to " + nf.getAbsolutePath());
                }

                // Add the new image to the Media content provider so that the
                // viewing is fast in Android 2.0+
                values = new ContentValues(6);
                mBinaryName = nf.getName();
                values.put(Images.Media.TITLE, mBinaryName);
                values.put(Images.Media.DISPLAY_NAME, mBinaryName);
                values.put(Images.Media.DATE_TAKEN, System.currentTimeMillis());
                values.put(Images.Media.MIME_TYPE, "image/jpeg");
                values.put(Images.Media.DATA, nf.getAbsolutePath());

                imageURI = getContentResolver().insert(Images.Media.EXTERNAL_CONTENT_URI, values);
                Log.i(t, "Inserting image returned uri = " + imageURI.toString());
                refreshImageView();

                break;
            case IMAGE_CHOOSER:
                String sourceImagePath = null;
                Uri selectedImage = intent.getData();
                if (selectedImage.toString().startsWith("file")) {
                    sourceImagePath = selectedImage.toString().substring(6);
                } else {
                    String[] projection = {
                        Images.Media.DATA
                    };
                    Cursor cursor = managedQuery(selectedImage, projection, null, null, null);
                    startManagingCursor(cursor);
                    int column_index = cursor.getColumnIndexOrThrow(Images.Media.DATA);
                    cursor.moveToFirst();
                    sourceImagePath = cursor.getString(column_index);
                }

                
                // Copy file to sdcard
                String destImagePath = mInstanceFolder + "/" + System.currentTimeMillis() + ".jpg";

                File source = new File(sourceImagePath);
                File chosenImage = new File(destImagePath);
                if (ImageUtils.folderExists(mInstanceFolder))
                	copyFile(source, chosenImage);
                
                if (chosenImage.exists()) {
                    // Add the new image to the Media content provider so that the
                    // viewing is fast in Android 2.0+
                	mBinaryName = chosenImage.getName();
                    values = new ContentValues(6);
                    values.put(Images.Media.TITLE, mBinaryName );
                    values.put(Images.Media.DISPLAY_NAME, mBinaryName);
                    values.put(Images.Media.DATE_TAKEN, System.currentTimeMillis());
                    values.put(Images.Media.MIME_TYPE, "image/jpeg");
                    values.put(Images.Media.DATA, chosenImage.getAbsolutePath());

                    imageURI =
                        getContentResolver().insert(Images.Media.EXTERNAL_CONTENT_URI, values);
                    Log.i(t, "Inserting image returned uri = " + imageURI.toString());
                    refreshImageView();

                } else {
                    Log.e(t, "NO IMAGE EXISTS at: " + source.getAbsolutePath());
                }
                break;
        }
    }
    
    private static void copyFile(File sourceFile, File destFile) {
        if (sourceFile.exists()) {
            FileChannel src;
            try {
                src = new FileInputStream(sourceFile).getChannel();
                FileChannel dst = new FileOutputStream(destFile).getChannel();
                dst.transferFrom(src, 0, src.size());
                src.close();
                dst.close();
            } catch (FileNotFoundException e) {
                Log.e(t, "FileNotFoundExeception while copying file");
                e.printStackTrace();
            } catch (IOException e) {
                Log.e(t, "IOExeception while copying file");
                e.printStackTrace();
            }
        } else {
            Log.e(t, "Source file does not exist: " + sourceFile.getAbsolutePath());
        }

    }
    
    @SuppressLint("NewApi")
    private void resizeImageView() {
        int width, height;

        Display display = getWindowManager().getDefaultDisplay();
        if (android.os.Build.VERSION.SDK_INT >= 13) {
             Point size = new Point();
             display.getSize(size);
             width = size.x;
             height = size.y;
        } else {
            width = display.getWidth();  // @deprecated
            height = display.getHeight();  // @deprecated
        }

        // Calculate image sizes
        if (height > width) {
            mImagePreview.getLayoutParams().height = (int) (width * 0.8);
            mImagePreview.getLayoutParams().width = (int) (width * 0.8);
        } else {
        	mImagePreview.getLayoutParams().height = (int) (height * 0.6);
        	mImagePreview.getLayoutParams().width = (int) (height * 0.6);
        }
    }
    
    
}