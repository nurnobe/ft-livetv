package com.livetv.app;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import org.json.JSONArray;
import org.json.JSONObject;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

public class MainActivity extends AppCompatActivity {

    private WebView webView;
    private FrameLayout fullscreenContainer;
    private View noInternetContainer;

    private View customView;
    private WebChromeClient.CustomViewCallback customViewCallback;
    private String latestName = BuildConfig.VERSION_NAME;
    private String apkUrl = "https://github.com/nurnobe/ft-livetv/releases";
    private String latestChanges =
            "- Improved Performance\n- Fullscreen Support\n- Bug Fixes";
    private boolean updateAvailable = false;
    private boolean showingNoInternet = false;
    private File pendingInstallFile;

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

        setContentView(R.layout.activity_main);

        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        );

        getWindow().addFlags(
                android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        webView = findViewById(R.id.webView);
        fullscreenContainer = findViewById(R.id.fullscreenContainer);
        noInternetContainer = findViewById(R.id.noInternetContainer);

        Button btnRetryInternet = findViewById(R.id.btnRetryInternet);
        Button btnExitInternet = findViewById(R.id.btnExitInternet);

        btnRetryInternet.setOnClickListener(v -> {
            showingNoInternet = false;
            noInternetContainer.setVisibility(View.GONE);
            webView.setVisibility(View.VISIBLE);
            webView.loadUrl("https://app.cast.bd");
        });

        btnExitInternet.setOnClickListener(v -> finish());

        WebSettings settings = webView.getSettings();

        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setMediaPlaybackRequiresUserGesture(false);
        settings.setLoadWithOverviewMode(true);
        settings.setUseWideViewPort(true);
        settings.setBuiltInZoomControls(false);
        settings.setDisplayZoomControls(false);
        settings.setSupportZoom(false);
        settings.setCacheMode(WebSettings.LOAD_NO_CACHE);
        settings.setAllowFileAccess(true);
        settings.setAllowContentAccess(true);
        settings.setJavaScriptCanOpenWindowsAutomatically(true);

        if (android.os.Build.VERSION.SDK_INT >= 21) {
            settings.setMixedContentMode(
                    WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        }

        webView.setWebChromeClient(new WebChromeClient() {

            @Override
            public void onShowCustomView(
                    View view,
                    CustomViewCallback callback) {

                customView = view;
                customViewCallback = callback;

                setRequestedOrientation(
                        ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);

                fullscreenContainer.setVisibility(View.VISIBLE);
                fullscreenContainer.addView(view);

                webView.setVisibility(View.GONE);

                getWindow().getDecorView().setSystemUiVisibility(
                        View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                                | View.SYSTEM_UI_FLAG_FULLSCREEN
                                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                );
            }

            @Override
            public void onHideCustomView() {

                if (customView == null) return;

                fullscreenContainer.removeView(customView);
                fullscreenContainer.setVisibility(View.GONE);

                customView = null;
                webView.setVisibility(View.VISIBLE);

                setRequestedOrientation(
                        ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);

                if (customViewCallback != null) {
                    customViewCallback.onCustomViewHidden();
                }
            }
        });

        webView.setWebViewClient(new WebViewClient() {

            @Override
            public void onPageStarted(
                    WebView view,
                    String url,
                    Bitmap favicon) {

                showingNoInternet = false;
                noInternetContainer.setVisibility(View.GONE);
                webView.setVisibility(View.VISIBLE);
            }

            @Override
            public void onPageFinished(
                    WebView view,
                    String url) {

                if (!showingNoInternet) {
                    noInternetContainer.setVisibility(View.GONE);
                    webView.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onReceivedError(
                    WebView view,
                    WebResourceRequest request,
                    WebResourceError error) {

                if (request.isForMainFrame()) {

                    webView.stopLoading();
                    showingNoInternet = true;
                    webView.setVisibility(View.GONE);
                    noInternetContainer.setVisibility(View.VISIBLE);
                }
            }
        });

        webView.clearCache(true);
        webView.clearHistory();

        webView.loadUrl("https://app.cast.bd");

        checkForUpdate();

        getOnBackPressedDispatcher().addCallback(this,
                new OnBackPressedCallback(true) {
                    @Override
                    public void handleOnBackPressed() {

                        if (customView != null) {

                            fullscreenContainer.removeView(customView);
                            fullscreenContainer.setVisibility(View.GONE);

                            customView = null;
                            webView.setVisibility(View.VISIBLE);
                            return;
                        }

                        if (webView.canGoBack()) {

                            webView.goBack();

                        } else {

                            showExitDialog();

                        }
                    }
                });
    }

    private void showExitDialog() {

        View dialogView = getLayoutInflater()
                .inflate(R.layout.exit_dialog, null);

        AlertDialog dialog =
                new AlertDialog.Builder(MainActivity.this)
                        .setView(dialogView)
                        .setCancelable(false)
                        .create();

        Button btnExitNow =
                dialogView.findViewById(R.id.btnExitNow);

        Button btnStay =
                dialogView.findViewById(R.id.btnStay);

        btnExitNow.setOnClickListener(v -> {
            dialog.dismiss();
            finish();
        });

        btnStay.setOnClickListener(v ->
                dialog.dismiss());

        dialog.setOnShowListener(dialogInterface -> {
            if (dialog.getWindow() != null) {
                dialog.getWindow().setBackgroundDrawableResource(
                        android.R.color.transparent);
            }
        });

        dialog.show();
    }

    private void showUpdateDialog() {

        View dialogView = getLayoutInflater()
                .inflate(R.layout.update_dialog, null);

        AlertDialog dialog =
                new AlertDialog.Builder(MainActivity.this)
                        .setView(dialogView)
                        .setCancelable(false)
                        .create();

        TextView txtVersion =
                dialogView.findViewById(R.id.txtVersion);

        TextView txtChanges =
                dialogView.findViewById(R.id.txtChanges);

        Button btnUpdate =
                dialogView.findViewById(R.id.btnUpdate);

        Button btnLater =
                dialogView.findViewById(R.id.btnLater);

        txtVersion.setText(
                "FT Live TV v" + latestName);
        txtChanges.setText(latestChanges);

        btnUpdate.setOnClickListener(v -> {

            dialog.dismiss();
            downloadAndInstallUpdate();
        });

        btnLater.setOnClickListener(v ->
                dialog.dismiss());

        dialog.setOnShowListener(dialogInterface -> {
            if (dialog.getWindow() != null) {
                dialog.getWindow().setBackgroundDrawableResource(
                        android.R.color.transparent);
            }
        });

        dialog.show();
    }

    private void checkForUpdate() {

        new Thread(() -> {

            try {

                OkHttpClient client = new OkHttpClient();

                Request request = new Request.Builder()
                        .url("https://raw.githubusercontent.com/nurnobe/ft-livetv/main/version.json")
                        .build();

                Response response =
                        client.newCall(request).execute();

                if (response.body() == null) return;

                String json =
                        response.body().string();

                JSONObject object =
                        new JSONObject(json);

                int latestVersion =
                        object.getInt("versionCode");

                latestName =
                        object.getString("versionName");

                apkUrl =
                        object.getString("apkUrl");

                latestChanges =
                        getChangesText(object);

                updateAvailable =
                        latestVersion > BuildConfig.VERSION_CODE
                                && !latestName.equals(BuildConfig.VERSION_NAME);

                if (updateAvailable) {

                    runOnUiThread(this::showUpdateDialog);
                }

            } catch (Exception e) {
                e.printStackTrace();
            }

        }).start();
    }

    private String getChangesText(JSONObject object) {

        JSONArray changes = object.optJSONArray("changes");

        if (changes == null || changes.length() == 0) {
            return latestChanges;
        }

        StringBuilder builder = new StringBuilder();

        for (int i = 0; i < changes.length(); i++) {
            String change = changes.optString(i);

            if (change.isEmpty()) {
                continue;
            }

            if (builder.length() > 0) {
                builder.append("\n");
            }

            builder.append("- ").append(change);
        }

        if (builder.length() == 0) {
            return latestChanges;
        }

        return builder.toString();
    }

    private void downloadAndInstallUpdate() {

        ProgressDialog progressDialog = new ProgressDialog(MainActivity.this);
        progressDialog.setTitle("Downloading Update");
        progressDialog.setMessage("Please wait...");
        progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        progressDialog.setIndeterminate(false);
        progressDialog.setMax(100);
        progressDialog.setCancelable(false);
        progressDialog.show();

        new Thread(() -> {

            try {

                OkHttpClient client = new OkHttpClient();

                Request request = new Request.Builder()
                        .url(apkUrl)
                        .build();

                Response response =
                        client.newCall(request).execute();

                if (!response.isSuccessful() || response.body() == null) {
                    throw new Exception("Download failed");
                }

                long totalBytes =
                        response.body().contentLength();

                File apkFile = new File(
                        getCacheDir(),
                        "ft-live-tv-v" + latestName + ".apk");

                InputStream inputStream =
                        response.body().byteStream();

                FileOutputStream outputStream =
                        new FileOutputStream(apkFile);

                byte[] buffer = new byte[8192];
                long downloadedBytes = 0;
                int read;

                while ((read = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, read);
                    downloadedBytes += read;

                    if (totalBytes > 0) {
                        int progress =
                                (int) ((downloadedBytes * 100) / totalBytes);

                        runOnUiThread(() ->
                                progressDialog.setProgress(progress));
                    }
                }

                outputStream.flush();
                outputStream.close();
                inputStream.close();

                runOnUiThread(() -> {
                    progressDialog.dismiss();
                    installApk(apkFile);
                });

            } catch (Exception e) {
                e.printStackTrace();

                runOnUiThread(() -> {
                    progressDialog.dismiss();
                    Toast.makeText(
                            MainActivity.this,
                            "Update download failed. Please try again.",
                            Toast.LENGTH_LONG).show();
                });
            }

        }).start();
    }

    private void installApk(File apkFile) {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                && !getPackageManager().canRequestPackageInstalls()) {

            pendingInstallFile = apkFile;

            Toast.makeText(
                    MainActivity.this,
                    "Please allow install permission, then return to the app.",
                    Toast.LENGTH_LONG).show();

            Intent settingsIntent =
                    new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES);
            settingsIntent.setData(
                    Uri.parse("package:" + getPackageName()));
            startActivity(settingsIntent);
            return;
        }

        Uri apkUri = FileProvider.getUriForFile(
                MainActivity.this,
                BuildConfig.APPLICATION_ID + ".fileprovider",
                apkFile);

        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setDataAndType(
                apkUri,
                "application/vnd.android.package-archive");
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

        startActivity(intent);
    }

    @Override
    protected void onResume() {
        super.onResume();

        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        );

        if (pendingInstallFile != null
                && pendingInstallFile.exists()
                && (Build.VERSION.SDK_INT < Build.VERSION_CODES.O
                || getPackageManager().canRequestPackageInstalls())) {

            File apkFile = pendingInstallFile;
            pendingInstallFile = null;
            installApk(apkFile);
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }

}
