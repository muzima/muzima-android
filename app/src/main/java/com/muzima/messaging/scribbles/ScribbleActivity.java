package com.muzima.messaging.scribbles;

import android.annotation.TargetApi;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.view.View;
import android.widget.Toast;

import com.muzima.R;
import com.muzima.messaging.PassphraseRequiredActionBarActivity;
import com.muzima.messaging.net.TransportOption;

import org.whispersystems.libsignal.util.guava.Optional;

@TargetApi(Build.VERSION_CODES.JELLY_BEAN)
public class ScribbleActivity extends PassphraseRequiredActionBarActivity implements ScribbleFragment.Controller {

    private static final String TAG = ScribbleActivity.class.getSimpleName();

    public static final int SCRIBBLE_REQUEST_CODE = 31424;

    @Override
    protected void onPreCreate() {

    }

    @Override
    protected void onCreate(Bundle savedInstanceState, boolean ready) {
        setContentView(R.layout.scribble_activity);

        if (savedInstanceState == null) {
            ScribbleFragment fragment = ScribbleFragment.newInstance(getIntent().getData(), getResources().getConfiguration().locale, Optional.absent());
            getSupportFragmentManager().beginTransaction().add(R.id.fragment_container, fragment).commit();
        }

        if (Build.VERSION.SDK_INT >= 19) {
            getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN |
                    View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    public void onImageEditComplete(@NonNull Uri uri, int width, int height, long size, @NonNull Optional<String> message, @NonNull Optional<TransportOption> transport) {
        Intent intent = new Intent();
        intent.setData(uri);
        setResult(RESULT_OK, intent);

        finish();
    }

    @Override
    public void onImageEditFailure() {
        Toast.makeText(ScribbleActivity.this, R.string.ScribbleActivity_save_failure, Toast.LENGTH_SHORT).show();
        finish();
    }
}
