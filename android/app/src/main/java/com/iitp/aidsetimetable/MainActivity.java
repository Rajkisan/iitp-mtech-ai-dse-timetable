package com.iitp.aidsetimetable;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Message;
import android.view.Gravity;
import android.view.View;
import android.webkit.CookieManager;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ProgressBar;

public class MainActivity extends Activity {
    private static final String HOME_URL = "file:///android_asset/www/index.html";

    private WebView webView;
    private ProgressBar progressBar;
    private Button homeButton;

    @SuppressLint({"SetJavaScriptEnabled", "AddJavascriptInterface"})
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        configureCookieManager();

        progressBar = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        progressBar.setMax(100);
        progressBar.setVisibility(View.GONE);

        webView = new WebView(this);
        configureWebView(webView);

        webView.addJavascriptInterface(new MoodleEmailBridge(this), "AndroidMoodle");
        webView.setWebViewClient(new TimetableWebViewClient());
        webView.setWebChromeClient(new TimetableWebChromeClient());

        FrameLayout root = new FrameLayout(this);
        root.addView(webView, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
        ));

        FrameLayout.LayoutParams progressParams = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                dp(3)
        );
        root.addView(progressBar, progressParams);

        homeButton = createHomeButton();
        FrameLayout.LayoutParams homeParams = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                dp(36),
                Gravity.TOP | Gravity.START
        );
        homeParams.setMargins(dp(10), dp(10), 0, 0);
        root.addView(homeButton, homeParams);

        setContentView(root);

        if (savedInstanceState == null) {
            webView.loadUrl(HOME_URL);
        } else {
            webView.restoreState(savedInstanceState);
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        flushCookies();
        webView.saveState(outState);
    }

    @Override
    protected void onPause() {
        flushCookies();
        super.onPause();
    }

    @Override
    protected void onStop() {
        flushCookies();
        super.onStop();
    }

    @Override
    public void onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack();
            return;
        }
        super.onBackPressed();
    }

    @Override
    protected void onDestroy() {
        if (webView != null) {
            flushCookies();
            webView.destroy();
        }
        super.onDestroy();
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private Button createHomeButton() {
        Button button = new Button(this);
        button.setText("Home");
        button.setTextSize(12);
        button.setTextColor(Color.WHITE);
        button.setAllCaps(false);
        button.setMinWidth(0);
        button.setMinHeight(0);
        button.setMinimumWidth(0);
        button.setMinimumHeight(0);
        button.setPadding(dp(12), 0, dp(12), 0);
        button.setVisibility(View.GONE);
        button.setContentDescription("Go to timetable home");

        GradientDrawable background = new GradientDrawable();
        background.setColor(Color.argb(210, 15, 47, 104));
        background.setCornerRadius(dp(18));
        button.setBackground(background);

        button.setOnClickListener(view -> {
            flushCookies();
            webView.loadUrl(HOME_URL);
        });

        return button;
    }

    private void updateHomeButton(String url) {
        if (homeButton == null) return;

        boolean isHome = url != null && url.startsWith(HOME_URL);
        homeButton.setVisibility(isHome ? View.GONE : View.VISIBLE);
    }

    @SuppressLint("SetJavaScriptEnabled")
    private void configureWebView(WebView targetWebView) {
        WebSettings settings = targetWebView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(true);
        settings.setAllowFileAccess(true);
        settings.setAllowContentAccess(true);
        settings.setLoadWithOverviewMode(true);
        settings.setUseWideViewPort(true);
        settings.setJavaScriptCanOpenWindowsAutomatically(true);
        settings.setSupportMultipleWindows(false);
        settings.setBuiltInZoomControls(false);
        settings.setDisplayZoomControls(false);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            settings.setAllowFileAccessFromFileURLs(true);
            settings.setAllowUniversalAccessFromFileURLs(true);
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            settings.setSafeBrowsingEnabled(true);
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            settings.setMixedContentMode(WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            CookieManager.getInstance().setAcceptThirdPartyCookies(targetWebView, true);
        }
    }

    @SuppressWarnings("deprecation")
    private void configureCookieManager() {
        CookieManager.setAcceptFileSchemeCookies(true);
        CookieManager.getInstance().setAcceptCookie(true);
        flushCookies();
    }

    private void flushCookies() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            CookieManager.getInstance().flush();
        }
    }

    private boolean shouldOpenOutside(Uri uri) {
        String scheme = uri.getScheme();
        if (scheme == null) return true;

        return !scheme.equals("http")
                && !scheme.equals("https")
                && !scheme.equals("file")
                && !scheme.equals("about")
                && !scheme.equals("data");
    }

    private boolean openOutsideApp(Uri uri) {
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, uri));
            return true;
        } catch (ActivityNotFoundException ignored) {
            return true;
        }
    }

    private boolean isMoodlePage(String url) {
        if (url == null) return false;

        Uri uri = Uri.parse(url);
        String host = uri.getHost();
        String path = uri.getPath();
        return "cetpgex.iitp.ac.in".equalsIgnoreCase(host)
                && path != null
                && path.startsWith("/moodle/");
    }

    private void injectMoodleEmailCapture() {
        String script = "(function(){"
                + "if(window.__iitpMoodleEmailBridge){return;}"
                + "window.__iitpMoodleEmailBridge=true;"
                + "var selectors=["
                + "'input[type=\"email\"]',"
                + "'input[name=\"email\"]',"
                + "'input[name=\"username\"]',"
                + "'#username',"
                + "'input[autocomplete=\"username\"]'"
                + "];"
                + "function stored(){try{return AndroidMoodle.getEmail()||'';}catch(e){return '';}}"
                + "function save(value){"
                + "value=(value||'').trim();"
                + "if(value.indexOf('@')>-1){try{AndroidMoodle.saveEmail(value);}catch(e){}}"
                + "}"
                + "function wire(field){"
                + "if(!field||field.dataset.iitpMoodleEmailWired){return;}"
                + "field.dataset.iitpMoodleEmailWired='1';"
                + "var known=stored();"
                + "if(known&&!field.value){"
                + "field.value=known;"
                + "field.dispatchEvent(new Event('input',{bubbles:true}));"
                + "field.dispatchEvent(new Event('change',{bubbles:true}));"
                + "}"
                + "['input','change','blur'].forEach(function(eventName){"
                + "field.addEventListener(eventName,function(){save(field.value);});"
                + "});"
                + "var form=field.form;"
                + "if(form&&!form.dataset.iitpMoodleEmailFormWired){"
                + "form.dataset.iitpMoodleEmailFormWired='1';"
                + "form.addEventListener('submit',function(){save(field.value);});"
                + "}"
                + "}"
                + "function scan(){document.querySelectorAll(selectors.join(',')).forEach(wire);}"
                + "scan();"
                + "new MutationObserver(scan).observe(document.documentElement,{childList:true,subtree:true});"
                + "})();";
        webView.evaluateJavascript(script, null);
    }

    private class TimetableWebViewClient extends WebViewClient {
        @Override
        public void onPageStarted(WebView view, String url, Bitmap favicon) {
            progressBar.setVisibility(View.VISIBLE);
            updateHomeButton(url);
        }

        @Override
        public void onPageFinished(WebView view, String url) {
            progressBar.setVisibility(View.GONE);
            flushCookies();
            updateHomeButton(url);
            if (isMoodlePage(url)) {
                injectMoodleEmailCapture();
            }
        }

        @Override
        public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
            Uri uri = request.getUrl();
            if (shouldOpenOutside(uri)) {
                return openOutsideApp(uri);
            }
            return false;
        }

        @SuppressWarnings("deprecation")
        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            Uri uri = Uri.parse(url);
            if (shouldOpenOutside(uri)) {
                return openOutsideApp(uri);
            }
            return false;
        }
    }

    private class TimetableWebChromeClient extends WebChromeClient {
        @Override
        public void onProgressChanged(WebView view, int newProgress) {
            progressBar.setProgress(newProgress);
            progressBar.setVisibility(newProgress >= 100 ? View.GONE : View.VISIBLE);
        }

        @Override
        public boolean onCreateWindow(
                WebView view,
                boolean isDialog,
                boolean isUserGesture,
                Message resultMsg
        ) {
            WebView popup = new WebView(MainActivity.this);
            configureWebView(popup);
            popup.setWebViewClient(new WebViewClient() {
                @Override
                public void onPageStarted(WebView popupView, String url, Bitmap favicon) {
                    if (url != null) {
                        webView.loadUrl(url);
                        popupView.destroy();
                    }
                }

                @Override
                public boolean shouldOverrideUrlLoading(WebView popupView, WebResourceRequest request) {
                    webView.loadUrl(request.getUrl().toString());
                    return true;
                }

                @SuppressWarnings("deprecation")
                @Override
                public boolean shouldOverrideUrlLoading(WebView popupView, String url) {
                    webView.loadUrl(url);
                    return true;
                }
            });

            WebView.WebViewTransport transport = (WebView.WebViewTransport) resultMsg.obj;
            transport.setWebView(popup);
            resultMsg.sendToTarget();
            return true;
        }
    }

    private static class MoodleEmailBridge {
        private static final String PREFS_NAME = "moodle_login";
        private static final String EMAIL_KEY = "email";

        private final SharedPreferences prefs;

        MoodleEmailBridge(Context context) {
            prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        }

        @JavascriptInterface
        public void saveEmail(String email) {
            String normalized = normalizeEmail(email);
            if (!normalized.isEmpty()) {
                prefs.edit().putString(EMAIL_KEY, normalized).apply();
            }
        }

        @JavascriptInterface
        public String getEmail() {
            return prefs.getString(EMAIL_KEY, "");
        }

        private String normalizeEmail(String email) {
            if (email == null) return "";

            String normalized = email.trim();
            if (normalized.length() > 254
                    || !normalized.contains("@")
                    || normalized.matches(".*\\s+.*")) {
                return "";
            }
            return normalized;
        }
    }
}
