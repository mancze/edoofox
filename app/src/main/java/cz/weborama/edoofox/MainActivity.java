package cz.weborama.edoofox;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.net.http.SslError;
import android.os.Build;
import android.os.Bundle;
import android.os.Message;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowInsets;
import android.webkit.CookieManager;
import android.webkit.SslErrorHandler;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {
    private static final int PICK_FILES = 100;
    // Replace with the project's repository URL when it is available.
    private static final String PROJECT_URL = "https://github.com/";
    private static final int PURPLE = Color.rgb(103, 51, 156);
    private WebView webView;
    private ProgressBar progress;
    private LinearLayout errorPanel;
    private TextView errorMessage;
    private boolean failed;
    private String retryUrl;
    private String school;
    private LinkPolicy links;
    private FrameLayout content;
    private BrowserGestureLayout chrome;
    private boolean keyboardVisible;
    private final android.os.Handler headerHandler = new android.os.Handler(android.os.Looper.getMainLooper());
    private boolean resumed;
    private int pageGeneration;
    private int contentSamples;
    private final Runnable headerCheck = new Runnable() {
        @Override public void run() {
            if (!resumed) return;
            checkPageHeader();
            headerHandler.postDelayed(this, 1000);
        }
    };

    // Presentation heuristic only, never an authentication/security decision. Return
    // a boolean, not page text, credentials, cookies, or user identity. Unknown pages stay visible.
    private static final String SCHOOL_CONTENT = "(() => {"
            + "if(document.readyState!=='complete'||!document.body) return false;"
            + "if(/\\/user\\/(?:[^/]*(?:login|logout|password|register|callback))/i.test(location.pathname)) return false;"
            + "if(document.querySelector('[data-name=Login],.login-page-container,.outside-page-container,input[type=password]')) return false;"
            + "return document.body.innerText.trim().length>0;})()";
    private android.widget.ScrollView schoolPanel;
    private android.widget.EditText schoolInput;
    private TextView schoolLabel;
    private ImageButton refresh;
    private ImageButton more;
    private ValueCallback<Uri[]> fileCallback;
    private android.window.OnBackInvokedCallback backCallback;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        school = LinkPolicy.normalizeSchool(getSharedPreferences("school", MODE_PRIVATE).getString("subdomain", null));
        if (school != null) links = new LinkPolicy(school);
        buildLayout();
        configureWebView();
        if (Build.VERSION.SDK_INT >= 33) {
            backCallback = this::navigateBack;
            getOnBackInvokedDispatcher().registerOnBackInvokedCallback(
                    android.window.OnBackInvokedDispatcher.PRIORITY_DEFAULT, backCallback);
        }
        if (school == null) {
            showSchoolPicker();
        } else {
            retryUrl = home();
            if (state == null || !school.equals(state.getString("school")) || webView.restoreState(state) == null) {
                webView.loadUrl(home());
            }
            if (state != null && state.getBoolean("picking_school")) showSchoolPicker();
        }
        if (state != null && schoolPanel.getVisibility() == View.VISIBLE) {
            schoolInput.setText(state.getString("school_draft", ""));
        }
    }

    private void buildLayout() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.WHITE);
        root.setOnApplyWindowInsetsListener((view, insets) -> {
            if (Build.VERSION.SDK_INT >= 30) {
                android.graphics.Insets safe = insets.getInsets(WindowInsets.Type.systemBars()
                        | WindowInsets.Type.displayCutout() | WindowInsets.Type.ime());
                view.setPadding(safe.left, safe.top, safe.right, safe.bottom);
                keyboardVisible = insets.isVisible(WindowInsets.Type.ime());
                if (chrome != null) updateGestures();
            } else {
                view.setPadding(insets.getSystemWindowInsetLeft(), insets.getSystemWindowInsetTop(),
                        insets.getSystemWindowInsetRight(), insets.getSystemWindowInsetBottom());
            }
            return insets;
        });
        if (Build.VERSION.SDK_INT >= 30) getWindow().setDecorFitsSystemWindows(false);
        else root.setFitsSystemWindows(true);

        LinearLayout toolbar = new LinearLayout(this);
        toolbar.setGravity(Gravity.CENTER_VERTICAL);
        toolbar.setPadding(dp(8), 0, dp(4), 0);
        ImageView logo = new ImageView(this);
        logo.setImageResource(R.drawable.ic_fox);
        logo.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        toolbar.addView(logo, new LinearLayout.LayoutParams(dp(44), dp(44)));
        TextView title = new TextView(this);
        title.setText(R.string.app_name);
        title.setTextColor(PURPLE);
        title.setTextSize(19);
        title.setTypeface(null, android.graphics.Typeface.BOLD);
        LinearLayout branding = new LinearLayout(this);
        branding.setOrientation(LinearLayout.VERTICAL);
        branding.addView(title);
        schoolLabel = new TextView(this);
        schoolLabel.setTextSize(12);
        schoolLabel.setTextColor(Color.DKGRAY);
        schoolLabel.setSingleLine(true);
        schoolLabel.setEllipsize(android.text.TextUtils.TruncateAt.END);
        schoolLabel.setText(links == null ? "" : links.host());
        branding.addView(schoolLabel);
        toolbar.addView(branding, new LinearLayout.LayoutParams(0, -2, 1));
        refresh = iconButton(R.drawable.ic_refresh, R.string.refresh);
        refresh.setOnClickListener(v -> reloadPage());
        toolbar.addView(refresh, new LinearLayout.LayoutParams(dp(48), dp(48)));
        more = iconButton(R.drawable.ic_more, R.string.more);
        more.setOnClickListener(v -> {
            PopupMenu menu = new PopupMenu(this, more);
            menu.getMenu().add(0, 1, 0, R.string.dashboard);
            menu.getMenu().add(0, 2, 1, R.string.open_browser);
            menu.getMenu().add(0, 3, 2, R.string.switch_school);
            menu.getMenu().add(0, 4, 3, R.string.gesture_back).setEnabled(webView.canGoBack());
            menu.getMenu().add(0, 5, 4, R.string.gesture_forward).setEnabled(webView.canGoForward());
            menu.getMenu().add(0, 6, 5, R.string.about);
            menu.setOnMenuItemClickListener(item -> {
                if (item.getItemId() == 1) webView.loadUrl(home());
                else if (item.getItemId() == 3) showSchoolPicker();
                else if (item.getItemId() == 4) webView.goBack();
                else if (item.getItemId() == 5) webView.goForward();
                else if (item.getItemId() == 6) {
                    if (getFragmentManager().findFragmentByTag(AboutDialog.TAG) == null) {
                        new AboutDialog().show(getFragmentManager(), AboutDialog.TAG);
                    }
                }
                else openExternal(links.isInternal(webView.getUrl()) ? webView.getUrl() : home());
                return true;
            });
            menu.show();
        });
        toolbar.addView(more, new LinearLayout.LayoutParams(dp(48), dp(48)));
        toolbar.setBackgroundColor(Color.WHITE);

        content = new FrameLayout(this);
        webView = new WebView(this);
        webView.setId(View.generateViewId());
        content.addView(webView, new FrameLayout.LayoutParams(-1, -1));

        errorPanel = new LinearLayout(this);
        errorPanel.setOrientation(LinearLayout.VERTICAL);
        errorPanel.setGravity(Gravity.CENTER);
        errorPanel.setPadding(dp(28), dp(24), dp(28), dp(24));
        errorPanel.setBackgroundColor(Color.rgb(250, 248, 253));
        ImageView fox = new ImageView(this);
        fox.setImageResource(R.drawable.ic_fox);
        errorPanel.addView(fox, new LinearLayout.LayoutParams(dp(120), dp(120)));
        TextView heading = new TextView(this);
        heading.setText(R.string.error_title);
        heading.setTextColor(PURPLE);
        heading.setTextSize(23);
        heading.setGravity(Gravity.CENTER);
        if (Build.VERSION.SDK_INT >= 28) heading.setAccessibilityHeading(true);
        errorPanel.addView(heading);
        errorMessage = new TextView(this);
        errorMessage.setTextSize(16);
        errorMessage.setGravity(Gravity.CENTER);
        errorMessage.setTextColor(Color.DKGRAY);
        errorMessage.setPadding(0, dp(12), 0, dp(20));
        errorPanel.addView(errorMessage);
        Button retry = new Button(this);
        retry.setText(R.string.retry);
        retry.setOnClickListener(v -> reloadPage());
        errorPanel.addView(retry);
        errorPanel.setVisibility(View.GONE);
        content.addView(errorPanel, new FrameLayout.LayoutParams(-1, -1));

        progress = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        progress.setMax(100);
        progress.setProgressTintList(android.content.res.ColorStateList.valueOf(PURPLE));
        progress.setContentDescription(getString(R.string.loading));
        progress.setVisibility(View.GONE);
        content.addView(progress, new FrameLayout.LayoutParams(-1, dp(3), Gravity.TOP));
        schoolPanel = buildSchoolPicker();
        schoolPanel.setVisibility(View.GONE);
        content.addView(schoolPanel, new FrameLayout.LayoutParams(-1, -1));
        chrome = new BrowserGestureLayout(this);
        chrome.setPanels(toolbar, content);
        root.addView(chrome, new LinearLayout.LayoutParams(-1, 0, 1));
        setContentView(root);
    }

    private android.widget.ScrollView buildSchoolPicker() {
        android.widget.ScrollView scroll = new android.widget.ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(Color.rgb(250, 248, 253));
        LinearLayout panel = new LinearLayout(this);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setGravity(Gravity.CENTER);
        panel.setPadding(dp(24), dp(24), dp(24), dp(24));
        ImageView fox = new ImageView(this);
        fox.setImageResource(R.drawable.ic_fox);
        fox.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        panel.addView(fox, new LinearLayout.LayoutParams(dp(108), dp(108)));
        TextView heading = new TextView(this);
        heading.setText(R.string.choose_school);
        heading.setTextColor(PURPLE);
        heading.setTextSize(26);
        heading.setGravity(Gravity.CENTER);
        panel.addView(heading);
        TextView instructions = new TextView(this);
        instructions.setText(R.string.school_instructions);
        instructions.setTextColor(Color.DKGRAY);
        instructions.setTextSize(16);
        instructions.setGravity(Gravity.CENTER);
        instructions.setPadding(0, dp(12), 0, dp(24));
        panel.addView(instructions);
        LinearLayout address = new LinearLayout(this);
        address.setGravity(Gravity.CENTER_VERTICAL);
        schoolInput = new android.widget.EditText(this);
        schoolInput.setId(R.id.school_subdomain);
        schoolInput.setSingleLine(true);
        schoolInput.setTextSize(18);
        schoolInput.setHint(R.string.school_subdomain);
        schoolInput.setContentDescription(getString(R.string.school_subdomain));
        schoolInput.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_URI);
        schoolInput.setImeOptions(android.view.inputmethod.EditorInfo.IME_ACTION_GO);
        schoolInput.setImportantForAutofill(View.IMPORTANT_FOR_AUTOFILL_NO);
        schoolInput.setOnEditorActionListener((v, action, event) -> {
            if (action == android.view.inputmethod.EditorInfo.IME_ACTION_GO) { saveSchool(); return true; }
            return false;
        });
        address.addView(schoolInput, new LinearLayout.LayoutParams(0, dp(56), 1));
        TextView suffix = new TextView(this);
        suffix.setText(R.string.school_suffix);
        suffix.setTextSize(18);
        suffix.setTextColor(PURPLE);
        address.addView(suffix);
        panel.addView(address, new LinearLayout.LayoutParams(-1, -2));
        Button save = new Button(this);
        save.setText(R.string.use_school);
        save.setOnClickListener(v -> saveSchool());
        panel.addView(save, new LinearLayout.LayoutParams(-1, -2));
        Button cancel = new Button(this);
        cancel.setId(R.id.cancel_school);
        cancel.setText(R.string.cancel);
        cancel.setOnClickListener(v -> hideSchoolPicker());
        panel.addView(cancel, new LinearLayout.LayoutParams(-1, -2));
        scroll.addView(panel, new android.widget.ScrollView.LayoutParams(-1, -1));
        return scroll;
    }

    private void showSchoolPicker() {
        schoolInput.setText(school == null ? "" : school);
        schoolInput.setError(null);
        schoolInput.selectAll();
        schoolPanel.findViewById(R.id.cancel_school).setVisibility(school == null ? View.GONE : View.VISIBLE);
        schoolPanel.setVisibility(View.VISIBLE);
        updateGestures();
        refresh.setEnabled(false);
        more.setEnabled(false);
        webView.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS);
    }

    private void hideSchoolPicker() {
        android.view.inputmethod.InputMethodManager keyboard = getSystemService(android.view.inputmethod.InputMethodManager.class);
        keyboard.hideSoftInputFromWindow(schoolInput.getWindowToken(), 0);
        schoolPanel.setVisibility(View.GONE);
        updateGestures();
        refresh.setEnabled(true);
        more.setEnabled(true);
        webView.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_AUTO);
    }

    private void saveSchool() {
        String chosen = LinkPolicy.normalizeSchool(schoolInput.getText().toString());
        if (chosen == null) {
            schoolInput.setError(getString(R.string.invalid_school));
            return;
        }
        if (chosen.equals(school)) { hideSchoolPicker(); return; }
        if (!getSharedPreferences("school", MODE_PRIVATE).edit().putString("subdomain", chosen).commit()) {
            schoolInput.setError(getString(R.string.school_save_error));
            return;
        }
        hideSchoolPicker();
        // A new WebView drops the previous school's history and pending navigation.
        // Cookies remain in the browser store, scoped by their originating domains.
        WebView previous = webView;
        webView = null;
        previous.stopLoading();
        content.removeView(previous);
        previous.destroy();
        if (fileCallback != null) { fileCallback.onReceiveValue(null); fileCallback = null; }
        school = chosen;
        links = new LinkPolicy(chosen);
        retryUrl = home();
        failed = false;
        errorPanel.setVisibility(View.GONE);
        schoolLabel.setText(links.host());
        webView = new WebView(this);
        webView.setId(View.generateViewId());
        content.addView(webView, 0, new FrameLayout.LayoutParams(-1, -1));
        configureWebView();
        webView.loadUrl(home());
    }

    private String home() { return links.home; }

    private void updateGestures() {
        chrome.setGesturesEnabled(school != null && !failed && !keyboardVisible
                && schoolPanel.getVisibility() != View.VISIBLE);
    }

    private ImageButton iconButton(int icon, int label) {
        ImageButton button = new ImageButton(this);
        button.setImageResource(icon);
        button.setContentDescription(getString(label));
        button.setTooltipText(getString(label));
        android.util.TypedValue background = new android.util.TypedValue();
        getTheme().resolveAttribute(android.R.attr.selectableItemBackgroundBorderless, background, true);
        button.setBackgroundResource(background.resourceId);
        return button;
    }

    @SuppressLint("SetJavaScriptEnabled")
    private void configureWebView() {
        pageGeneration++;
        contentSamples = 0;
        chrome.setHeaderAutoHide(false);
        chrome.attach(webView, this::reloadPage);
        updateGestures();
        WebView.setWebContentsDebuggingEnabled(BuildConfig.DEBUG);
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setAllowFileAccess(false);
        settings.setAllowContentAccess(false);
        settings.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);
        settings.setSupportMultipleWindows(true);
        settings.setJavaScriptCanOpenWindowsAutomatically(false);
        settings.setBuiltInZoomControls(true);
        settings.setDisplayZoomControls(false);
        CookieManager.getInstance().setAcceptCookie(true);
        CookieManager.getInstance().setAcceptThirdPartyCookies(webView, false);

        webView.setWebViewClient(new WebViewClient() {
            @Override public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                if (view != webView || links == null) return true;
                return request.isForMainFrame() && route(request.getUrl().toString());
            }

            @Override public void onPageStarted(WebView view, String url, android.graphics.Bitmap favicon) {
                if (view != webView || links == null) return;
                if (!links.isEmbedded(url)) {
                    view.stopLoading();
                    openExternal(url);
                    return;
                }
                retryUrl = url;
                pageGeneration++;
                contentSamples = 0;
                if (LinkPolicy.isAuthentication(url)) chrome.setHeaderAutoHide(false);
                failed = false;
                chrome.setLoading(true);
                updateGestures();
                errorPanel.setVisibility(View.GONE);
                webView.setVisibility(View.VISIBLE);
                progress.setProgress(0);
                progress.setVisibility(View.VISIBLE);
            }

            @Override public void onPageFinished(WebView view, String url) {
                if (view != webView) return;
                progress.setVisibility(View.GONE);
                chrome.setLoading(false);
                CookieManager.getInstance().flush();
                checkPageHeader();
            }

            @Override public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
                if (view == webView && request.isForMainFrame()) showError(R.string.error_message);
            }

            @Override public void onReceivedHttpError(WebView view, WebResourceRequest request, WebResourceResponse response) {
                if (view == webView && request.isForMainFrame() && response.getStatusCode() >= 400) showError(R.string.error_server);
            }

            @Override public void onReceivedSslError(WebView view, SslErrorHandler handler, SslError error) {
                handler.cancel();
                if (view != webView) return;
                // Ignore a failed third-party resource; never bypass an invalid certificate.
                if (error.getUrl().equals(view.getUrl()) || error.getUrl().equals(retryUrl)) {
                    showError(R.string.error_secure);
                }
            }

            @Override public boolean onRenderProcessGone(WebView view, android.webkit.RenderProcessGoneDetail detail) {
                if (view != webView) return true;
                // Recreate the screen with a fresh WebView after a renderer failure.
                ((ViewGroup) view.getParent()).removeView(view);
                view.destroy();
                webView = null;
                recreate();
                return true;
            }
        });

        webView.setWebChromeClient(new WebChromeClient() {
            @Override public void onProgressChanged(WebView view, int value) {
                if (view != webView) return;
                progress.setProgress(value);
                progress.setVisibility(failed || value == 100 ? View.GONE : View.VISIBLE);
            }

            @Override public boolean onCreateWindow(WebView view, boolean dialog, boolean userGesture, Message result) {
                if (!userGesture || view != webView || links == null) return false;
                // target=_blank and window.open must obey the same origin rule.
                WebView popup = new WebView(MainActivity.this);
                popup.getSettings().setAllowFileAccess(false);
                popup.getSettings().setAllowContentAccess(false);
                popup.setWebViewClient(new WebViewClient() {
                    private boolean handled;
                    private void dispatch(String url) {
                        if (view != webView) { popup.post(popup::destroy); return; }
                        if (handled || "about:blank".equals(url)) return;
                        handled = true;
                        if (links.isEmbedded(url)) webView.loadUrl(url);
                        else openExternal(url);
                        popup.post(popup::destroy);
                    }
                    @Override public boolean shouldOverrideUrlLoading(WebView ignored, WebResourceRequest request) {
                        dispatch(request.getUrl().toString());
                        return true;
                    }
                    @Override public void onPageStarted(WebView ignored, String url, android.graphics.Bitmap icon) {
                        popup.stopLoading();
                        dispatch(url);
                    }
                });
                ((WebView.WebViewTransport) result.obj).setWebView(popup);
                result.sendToTarget();
                return true;
            }

            @Override public boolean onShowFileChooser(WebView view, ValueCallback<Uri[]> callback, FileChooserParams params) {
                if (fileCallback != null) fileCallback.onReceiveValue(null);
                fileCallback = callback;
                try {
                    startActivityForResult(params.createIntent(), PICK_FILES);
                } catch (ActivityNotFoundException e) {
                    fileCallback.onReceiveValue(null);
                    fileCallback = null;
                    toast(R.string.file_picker_unavailable);
                }
                return true;
            }
        });
        webView.setDownloadListener((url, agent, disposition, mime, length) -> openExternal(url));
    }

    private boolean route(String url) {
        if (links.isEmbedded(url)) return false;
        openExternal(url);
        return true;
    }

    void openProjectPage() { openExternal(PROJECT_URL); }

    private void openExternal(String url) {
        if (!LinkPolicy.canOpenExternally(url)) {
            toast(R.string.unsupported_link);
            return;
        }
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            intent.addCategory(Intent.CATEGORY_BROWSABLE);
            String scheme = intent.getData().getScheme();
            if ("https".equalsIgnoreCase(scheme) || "http".equalsIgnoreCase(scheme)) {
                // Resolve to a browser, even if another app claims this website's links.
                intent.setSelector(new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_APP_BROWSER));
            }
            startActivity(intent);
        } catch (ActivityNotFoundException | SecurityException e) {
            toast(R.string.no_handler);
        }
    }

    private void showError(int message) {
        failed = true;
        contentSamples = 0;
        chrome.setHeaderAutoHide(false);
        chrome.setLoading(false);
        updateGestures();
        progress.setVisibility(View.GONE);
        errorMessage.setText(message);
        errorPanel.setVisibility(View.VISIBLE);
        webView.setVisibility(View.INVISIBLE);
    }

    private void reloadPage() {
        chrome.setLoading(true);
        if (failed) webView.loadUrl(links.isEmbedded(retryUrl) ? retryUrl : home());
        else webView.reload();
    }

    private void navigateBack() {
        if (schoolPanel.getVisibility() == View.VISIBLE) {
            if (school == null) finish();
            else hideSchoolPicker();
            return;
        }
        if (webView != null && webView.canGoBack()) webView.goBack();
        else finish();
    }

    @SuppressWarnings("deprecation")
    // API 33+ uses OnBackInvokedDispatcher above; this handles Android 8–12 only.
    @SuppressLint("GestureBackNavigation")
    @Override public void onBackPressed() { navigateBack(); }

    @Override protected void onActivityResult(int request, int result, Intent data) {
        super.onActivityResult(request, result, data);
        if (request == PICK_FILES && fileCallback != null) {
            fileCallback.onReceiveValue(WebChromeClient.FileChooserParams.parseResult(result, data));
            fileCallback = null;
        }
    }

    @Override protected void onSaveInstanceState(Bundle state) {
        state.putString("school", school);
        state.putBoolean("picking_school", schoolPanel.getVisibility() == View.VISIBLE);
        state.putString("school_draft", schoolInput.getText().toString());
        if (webView != null) webView.saveState(state);
        super.onSaveInstanceState(state);
    }

    @Override protected void onPause() {
        resumed = false;
        headerHandler.removeCallbacks(headerCheck);
        if (webView != null) webView.onPause();
        CookieManager.getInstance().flush();
        super.onPause();
    }

    @Override protected void onResume() {
        super.onResume();
        if (webView != null) webView.onResume();
        resumed = true;
        headerHandler.removeCallbacks(headerCheck);
        headerHandler.post(headerCheck);
    }

    private void checkPageHeader() {
        if (!resumed || webView == null) return;
        WebView source = webView;
        String url = source.getUrl();
        if (failed || links == null || !links.isInternal(url)) {
            contentSamples = 0;
            chrome.setHeaderAutoHide(false);
            return;
        }
        int generation = pageGeneration;
        source.evaluateJavascript(SCHOOL_CONTENT, result -> {
            if (!resumed || source != webView || generation != pageGeneration
                    || !java.util.Objects.equals(url, source.getUrl()) || failed) return;
            if ("true".equals(result)) {
                if (contentSamples < 2 && ++contentSamples == 2) chrome.setHeaderAutoHide(true);
            } else {
                contentSamples = 0;
                chrome.setHeaderAutoHide(false);
            }
        });
    }

    @Override protected void onDestroy() {
        resumed = false;
        headerHandler.removeCallbacks(headerCheck);
        if (fileCallback != null) fileCallback.onReceiveValue(null);
        if (Build.VERSION.SDK_INT >= 33 && backCallback != null) {
            getOnBackInvokedDispatcher().unregisterOnBackInvokedCallback(backCallback);
        }
        if (webView != null) {
            ((ViewGroup) webView.getParent()).removeView(webView);
            webView.destroy();
        }
        super.onDestroy();
    }

    private void toast(int message) { Toast.makeText(this, message, Toast.LENGTH_LONG).show(); }
    private int dp(int value) { return Math.round(value * getResources().getDisplayMetrics().density); }
}
