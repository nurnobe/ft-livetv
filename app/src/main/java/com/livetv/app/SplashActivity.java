package com.livetv.app;

import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;

import androidx.appcompat.app.AppCompatActivity;

public class SplashActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        PackageManager pm = getPackageManager();

        boolean isTv =
                pm.hasSystemFeature(PackageManager.FEATURE_LEANBACK) ||
                        pm.hasSystemFeature(PackageManager.FEATURE_TELEVISION);

        if (isTv) {
            setRequestedOrientation(
                    ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        }

        setContentView(R.layout.activity_splash);

        new Handler().postDelayed(() -> {

            startActivity(
                    new Intent(
                            SplashActivity.this,
                            MainActivity.class));

            finish();

        }, 3000);
    }
}