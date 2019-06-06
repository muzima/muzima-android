package com.muzima.messaging;

import android.Manifest;
import android.animation.Animator;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewAnimationUtils;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.muzima.MuzimaApplication;
import com.muzima.R;
import com.muzima.messaging.crypto.ProfileKeyUtil;
import com.muzima.messaging.customcomponents.InputAwareLayout;
import com.muzima.messaging.customcomponents.emoji.EmojiDrawer;
import com.muzima.messaging.customcomponents.emoji.EmojiToggle;
import com.muzima.messaging.exceptions.BitmapDecodingException;
import com.muzima.messaging.mms.GlideApp;
import com.muzima.messaging.profiles.SystemProfileUtil;
import com.muzima.messaging.push.AccountManagerFactory;
import com.muzima.messaging.sqlite.database.SignalAddress;
import com.muzima.messaging.utils.FileProviderUtil;
import com.muzima.messaging.utils.Util;
import com.muzima.utils.BitmapUtil;
import com.muzima.utils.IntentUtils;
import com.muzima.utils.Permissions;
import com.muzima.utils.ViewUtil;
import com.muzima.view.login.LoginActivity;
import com.soundcloud.android.crop.Crop;

import org.whispersystems.signalservice.api.SignalServiceAccountManager;
import org.whispersystems.signalservice.api.crypto.ProfileCipher;
import org.whispersystems.signalservice.api.util.StreamDetails;
import org.whispersystems.signalservice.internal.util.concurrent.ListenableFuture;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.security.SecureRandom;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutionException;

import javax.inject.Inject;

import static android.provider.MediaStore.EXTRA_OUTPUT;

public class CreateProfileActivity extends BaseActionBarActivity {
    private static final String TAG = CreateProfileActivity.class.getSimpleName();

    public static final String NEXT_INTENT    = "next_intent";
    public static final String EXCLUDE_SYSTEM = "exclude_system";

    private static final int REQUEST_CODE_AVATAR = 1;

    @Inject
    SignalServiceAccountManager accountManager;

    private InputAwareLayout container;
    private ImageView avatar;
    private Button finishButton;
    private EditText name;
    private EmojiToggle emojiToggle;
    private EmojiDrawer emojiDrawer;
    private View reveal;

    private Intent nextIntent;
    private byte[] avatarBytes;
    private File captureFile;

    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        setContentView(R.layout.profile_create_activity);

        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        getSupportActionBar().setTitle(R.string.createProfileActivity_your_profile_info);

        initializeResources();
        initializeEmojiInput();
        initializeProfileName(getIntent().getBooleanExtra(EXCLUDE_SYSTEM, false));
        initializeProfileAvatar(getIntent().getBooleanExtra(EXCLUDE_SYSTEM, false));
        initializeAccountManager();

        MuzimaApplication.getInstance(this).injectDependencies(this);
    }

    public void initializeAccountManager(){
        accountManager = AccountManagerFactory.createManager(CreateProfileActivity.this,
                TextSecurePreferences.getLocalNumber(CreateProfileActivity.this),
                TextSecurePreferences.getPushServerPassword(CreateProfileActivity.this));
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onBackPressed() {
        if (container.isInputOpen()) container.hideCurrentInput(name);
        else                         super.onBackPressed();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        if (container.getCurrentInput() == emojiDrawer) {
            container.hideAttachedInput(true);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[], @NonNull int[] grantResults) {
        Permissions.onRequestPermissionsResult(this, requestCode, permissions, grantResults);
    }

    @SuppressLint("StaticFieldLeak")
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode) {
            case REQUEST_CODE_AVATAR:
                if (resultCode == Activity.RESULT_OK) {
                    Uri outputFile = Uri.fromFile(new File(getCacheDir(), "cropped"));
                    Uri inputFile  = (data != null ? data.getData() : null);

                    if (inputFile == null && captureFile != null) {
                        inputFile = Uri.fromFile(captureFile);
                    }

                    if (data != null && data.getBooleanExtra("delete", false)) {
                        avatarBytes = null;
                        avatar.setImageDrawable(new ResourceContactPhoto(R.drawable.ic_camera_alt_white_24dp).asDrawable(this, getResources().getColor(R.color.gray70)));
                    } else {
                        new Crop(inputFile).output(outputFile).asSquare().start(this);
                    }
                }
                break;
            case Crop.REQUEST_CROP:
                if (resultCode == Activity.RESULT_OK) {
                    new AsyncTask<Void, Void, byte[]>() {
                        @Override
                        protected byte[] doInBackground(Void... params) {
                            try {
                                BitmapUtil.ScaleResult result = BitmapUtil.createScaledBytes(CreateProfileActivity.this, Crop.getOutput(data), new ProfileMediaConstraints());
                                return result.getBitmap();
                            } catch (BitmapDecodingException e) {
                                Log.w(TAG, e);
                                return null;
                            }
                        }

                        @Override
                        protected void onPostExecute(byte[] result) {
                            if (result != null) {
                                avatarBytes = result;
                                GlideApp.with(CreateProfileActivity.this)
                                        .load(avatarBytes)
                                        .skipMemoryCache(true)
                                        .diskCacheStrategy(DiskCacheStrategy.NONE)
                                        .circleCrop()
                                        .into(avatar);
                            } else {
                                Toast.makeText(CreateProfileActivity.this, R.string.CreateProfileActivity_error_setting_profile_photo, Toast.LENGTH_LONG).show();
                            }
                        }
                    }.execute();
                }
                break;
        }
    }

    private void initializeResources() {
        TextView skipButton       = ViewUtil.findById(this, R.id.skip_button);

        this.avatar       = ViewUtil.findById(this, R.id.avatar);
        this.name         = ViewUtil.findById(this, R.id.name);
        this.emojiToggle  = ViewUtil.findById(this, R.id.emoji_toggle);
        this.emojiDrawer  = ViewUtil.findById(this, R.id.emoji_drawer);
        this.container    = ViewUtil.findById(this, R.id.container);
        this.finishButton = ViewUtil.findById(this, R.id.finish_button);
        this.reveal       = ViewUtil.findById(this, R.id.reveal);
        this.nextIntent   = new Intent(CreateProfileActivity.this,LoginActivity.class);

        this.avatar.setImageDrawable(new ResourceContactPhoto(R.drawable.ic_camera_alt_white_24dp).asDrawable(this, getResources().getColor(R.color.grey_300)));

        this.avatar.setOnClickListener(view -> Permissions.with(this)
                .request(Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                .ifNecessary()
                .onAnyResult(this::handleAvatarSelectionWithPermissions)
                .execute());

        this.name.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override
            public void afterTextChanged(Editable s) {
                if (s.toString().getBytes().length > ProfileCipher.NAME_PADDED_LENGTH) {
                    name.setError(getString(R.string.createProfileActivity_too_long));
                    finishButton.setEnabled(false);
                } else if (name.getError() != null || !finishButton.isEnabled()) {
                    name.setError(null);
                    finishButton.setEnabled(true);
                }
            }
        });

        this.finishButton.setOnClickListener(view -> {
            handleUpload();
        });

        skipButton.setOnClickListener(view -> {
            if (nextIntent != null) startActivity(nextIntent);
            finish();
        });
    }

    private void initializeProfileName(boolean excludeSystem) {
        if (!TextUtils.isEmpty(TextSecurePreferences.getProfileName(this))) {
            String profileName = TextSecurePreferences.getProfileName(this);

            name.setText(profileName);
            name.setSelection(profileName.length(), profileName.length());
        } else if (!excludeSystem) {
            SystemProfileUtil.getSystemProfileName(this).addListener(new ListenableFuture.Listener<String>() {
                @Override
                public void onSuccess(String result) {
                    if (!TextUtils.isEmpty(result)) {
                        name.setText(result);
                        name.setSelection(result.length(), result.length());
                    }
                }

                @Override
                public void onFailure(ExecutionException e) {
                    Log.w(TAG, e);
                }
            });
        }
    }

    private void initializeProfileAvatar(boolean excludeSystem) {
        SignalAddress ourAddress = SignalAddress.fromSerialized(TextSecurePreferences.getLocalNumber(this));

        if (AvatarHelper.getAvatarFile(this, ourAddress).exists() && AvatarHelper.getAvatarFile(this, ourAddress).length() > 0) {
            new AsyncTask<Void, Void, byte[]>() {
                @Override
                protected byte[] doInBackground(Void... params) {
                    try {
                        return Util.readFully(AvatarHelper.getInputStreamFor(CreateProfileActivity.this, ourAddress));
                    } catch (IOException e) {
                        Log.w(TAG, e);
                        return null;
                    }
                }

                @Override
                protected void onPostExecute(byte[] result) {
                    if (result != null) {
                        avatarBytes = result;
                        GlideApp.with(CreateProfileActivity.this)
                                .load(result)
                                .circleCrop()
                                .into(avatar);
                    }
                }
            }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        } else if (!excludeSystem) {
            SystemProfileUtil.getSystemProfileAvatar(this, new ProfileMediaConstraints()).addListener(new ListenableFuture.Listener<byte[]>() {
                @Override
                public void onSuccess(byte[] result) {
                    if (result != null) {
                        avatarBytes = result;
                        GlideApp.with(CreateProfileActivity.this)
                                .load(result)
                                .circleCrop()
                                .into(avatar);
                    }
                }

                @Override
                public void onFailure(ExecutionException e) {
                    Log.w(TAG, e);
                }
            });
        }
    }

    private void initializeEmojiInput() {
        this.emojiToggle.attach(emojiDrawer);

        this.emojiToggle.setOnClickListener(v -> {
            if (container.getCurrentInput() == emojiDrawer) {
                container.showSoftkey(name);
            } else {
                container.show(name, emojiDrawer);
            }
        });

        this.emojiDrawer.setEmojiEventListener(new EmojiDrawer.EmojiEventListener() {
            @Override
            public void onKeyEvent(KeyEvent keyEvent) {
                name.dispatchKeyEvent(keyEvent);
            }

            @Override
            public void onEmojiSelected(String emoji) {
                final int start = name.getSelectionStart();
                final int end   = name.getSelectionEnd();

                name.getText().replace(Math.min(start, end), Math.max(start, end), emoji);
                name.setSelection(start + emoji.length());
            }
        });

        this.container.addOnKeyboardShownListener(() -> emojiToggle.setToEmoji());
        this.name.setOnClickListener(v -> container.showSoftkey(name));
    }

    private Intent createAvatarSelectionIntent(@Nullable File captureFile, boolean includeClear, boolean includeCamera) {
        List<Intent> extraIntents  = new LinkedList<>();
        Intent       galleryIntent = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.INTERNAL_CONTENT_URI);
        galleryIntent.setType("image/*");

        if (!IntentUtils.isResolvable(CreateProfileActivity.this, galleryIntent)) {
            galleryIntent = new Intent(Intent.ACTION_GET_CONTENT);
            galleryIntent.setType("image/*");
        }

        if (includeCamera) {
            Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

            if (captureFile != null && cameraIntent.resolveActivity(getPackageManager()) != null) {
                cameraIntent.putExtra(EXTRA_OUTPUT, FileProviderUtil.getUriFor(this, captureFile));
                extraIntents.add(cameraIntent);
            }
        }

        if (includeClear) {
            extraIntents.add(new Intent("com.muzima.action.CLEAR_PROFILE_PHOTO"));
        }

        Intent chooserIntent = Intent.createChooser(galleryIntent, getString(R.string.createProfileActivity_profile_photo));

        if (!extraIntents.isEmpty()) {
            chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, extraIntents.toArray(new Intent[0]));
        }


        return chooserIntent;
    }

    private void handleAvatarSelectionWithPermissions() {
        boolean hasCameraPermission = Permissions.hasAll(this, Manifest.permission.CAMERA);

        if (hasCameraPermission) {
            try {
                captureFile = File.createTempFile("capture", "jpg", getExternalCacheDir());
            } catch (IOException e) {
                Log.w(TAG, e);
                captureFile = null;
            }
        }

        Intent chooserIntent = createAvatarSelectionIntent(captureFile, avatarBytes != null, hasCameraPermission);
        startActivityForResult(chooserIntent, REQUEST_CODE_AVATAR);
    }

    private void handleUpload() {
        final String        name;
        final StreamDetails avatar;

        if (TextUtils.isEmpty(this.name.getText().toString())) name = null;
        else                                                   name = this.name.getText().toString();

        if (avatarBytes == null || avatarBytes.length == 0) avatar = null;
        else                                                avatar = new StreamDetails(new ByteArrayInputStream(avatarBytes),
                "image/jpeg", avatarBytes.length);

        new AsyncTask<Void, Void, Boolean>() {
            @Override
            protected Boolean doInBackground(Void... params) {
                Context context    = CreateProfileActivity.this;
                byte[]  profileKey = ProfileKeyUtil.getProfileKey(CreateProfileActivity.this);

                try {
                    accountManager.setProfileName(profileKey, name);
                    TextSecurePreferences.setProfileName(context, name);
                } catch (IOException e) {
                    Log.w(TAG, e);
                    return false;

                }

                try {
                    accountManager.setProfileAvatar(profileKey, avatar);
                    AvatarHelper.setAvatar(CreateProfileActivity.this, SignalAddress.fromSerialized(TextSecurePreferences.getLocalNumber(context)), avatarBytes);
                    TextSecurePreferences.setProfileAvatarId(CreateProfileActivity.this, new SecureRandom().nextInt());
                } catch (IOException e) {
                    Log.w(TAG, e);
                    return false;
                }

                return true;
            }

            @Override
            public void onPostExecute(Boolean result) {
                super.onPostExecute(result);

                if (result) {
                    if (captureFile != null) captureFile.delete();
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) handleFinishedLollipop();
                    else                                                       handleFinishedLegacy();
                } else        {
                    Toast.makeText(CreateProfileActivity.this, R.string.createProfileActivity_problem_setting_profile, Toast.LENGTH_LONG).show();
                }
            }
        }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    private void handleFinishedLegacy() {
        //finishButton.setProgress(0);
        if (nextIntent != null) startActivity(nextIntent);
        finish();
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private void handleFinishedLollipop() {
        int[] finishButtonLocation = new int[2];
        int[] revealLocation       = new int[2];

        finishButton.getLocationInWindow(finishButtonLocation);
        reveal.getLocationInWindow(revealLocation);

        int finishX = finishButtonLocation[0] - revealLocation[0];
        int finishY = finishButtonLocation[1] - revealLocation[1];

        finishX += finishButton.getWidth() / 2;
        finishY += finishButton.getHeight() / 2;

        Animator animation = ViewAnimationUtils.createCircularReveal(reveal, finishX, finishY, 0f, (float) Math.max(reveal.getWidth(), reveal.getHeight()));
        animation.setDuration(500);
        animation.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {}

            @Override
            public void onAnimationEnd(Animator animation) {
                //finishButton.setProgress(0);
                if (nextIntent != null)  startActivity(nextIntent);
                finish();
            }

            @Override
            public void onAnimationCancel(Animator animation) {}
            @Override
            public void onAnimationRepeat(Animator animation) {}
        });

        reveal.setVisibility(View.VISIBLE);
        animation.start();
    }
}
