package cz.weborama.edoofox;

import android.app.Activity;
import android.app.Instrumentation;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.SystemClock;
import android.view.View;
import android.view.ViewGroup;
import android.view.MotionEvent;
import android.webkit.CookieManager;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.TextView;

import java.io.File;
import java.io.FileOutputStream;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BooleanSupplier;

/** Run only on the isolated Edoofox emulator. Never enters real credentials. */
public class SmokeInstrumentation extends Instrumentation {
    private static final String HOME = "https://zsslovanak.edookit.net/";
    private Activity activity;
    private WebView web;
    private String phase;

    @Override public void onCreate(Bundle arguments) {
        super.onCreate(arguments);
        phase = arguments == null ? null : arguments.getString("phase");
        start();
    }

    @Override public void onStart() {
        Bundle report = new Bundle();
        try {
            if ("gestures".equals(phase)) {
                new GestureChecks(this).run();
                report.putString("stream", "PASS: isolated gesture QA: history, header, two-step refresh, short/cancelled pulls, nested scrolling, horizontal widget, text input.\n");
                finish(Activity.RESULT_OK, report);
                return;
            }
            if ("schools".equals(phase)) {
                getTargetContext().getSharedPreferences("school", 0).edit().clear().commit();
            }
            activity = startActivitySync(new Intent(getTargetContext(), MainActivity.class)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
            runOnMainSync(() -> web = findWeb(activity.getWindow().getDecorView()));
            require(web != null, "WebView exists");
            if ("schools".equals(phase)) {
                schoolSelectionChecks();
                report.putString("stream", "PASS: first-run chooser, validation, normalization, cancel, switching, history reset, old-school external routing.\n");
                finish(Activity.RESULT_OK, report);
                return;
            }
            if ("verify-school".equals(phase)) {
                require("another-school".equals(savedSchool()), "selected school persisted across process restart");
                require(!visibleText(activity.getString(R.string.choose_school)), "saved school skips setup");
                openSchoolMenu();
                chooseSchool("zsslovanak", false);
                report.putString("stream", "PASS: selected school persisted across process restart; switched back to test school.\n");
                finish(Activity.RESULT_OK, report);
                return;
            }
            if (savedSchool() == null) chooseSchool("zsslovanak", false);
            if (!awaitSchoolReady()) {
                report.putString("stream", "SKIP: expected public login not shown; retained session left unchanged.\n");
                finish(Activity.RESULT_OK, report);
                return;
            }
            if ("verify-cookie".equals(phase)) {
                AtomicReference<String> cookie = new AtomicReference<>();
                runOnMainSync(() -> cookie.set(CookieManager.getInstance().getCookie(HOME)));
                require(cookie.get() != null && cookie.get().contains("edoofox_smoke=persisted"),
                        "persistent cookie survives process restart");
                runOnMainSync(() -> {
                    CookieManager.getInstance().setCookie(HOME,
                            "edoofox_smoke=; Max-Age=0; Path=/; Secure; SameSite=Lax");
                    CookieManager.getInstance().flush();
                });
                report.putString("stream", "PASS: cookie persisted across process restart; test cookie removed.\n");
            } else {
                screenshot("01-login.png");
                tapElement("[data-name=Login]");
                await(() -> js("location.hostname").contains("uuidentity.plus4u.net"), "identity navigation stays in app");
                await(() -> js("document.body.innerText").contains("Zůstat přihlášený"), "identity form rendered");
                screenshot("02-sign-in.png");

                runOnMainSync(() -> web.loadDataWithBaseURL(HOME,
                        "<html><head><title>Edoofox test</title></head><body>"
                        + "<a id='external' href='https://example.org/'>External</a>"
                        + "<a id='internal' href='#inside'>Internal</a></body></html>",
                        "text/html", "UTF-8", HOME));
                await(() -> js("document.title").contains("Edoofox test"), "fixture loaded");
                IntentFilter external = new IntentFilter(Intent.ACTION_VIEW);
                external.addCategory(Intent.CATEGORY_BROWSABLE);
                external.addDataScheme("https");
                ActivityMonitor monitor = addMonitor(external, new ActivityResult(Activity.RESULT_CANCELED, null), true);
                js("document.getElementById('external').click(); true");
                await(() -> monitor.getHits() == 1, "external link dispatched to another app");
                require(js("document.title").contains("Edoofox test"), "external link preserves current page");
                removeMonitor(monitor);

                js("document.getElementById('internal').click(); true");
                await(() -> js("location.hash").contains("inside"), "internal navigation stays in WebView");
                sendKeyDownUpSync(android.view.KeyEvent.KEYCODE_BACK);
                await(() -> !js("location.hash").contains("inside"), "system Back returns through WebView history");
                require(!activity.isFinishing() && !activity.isDestroyed(), "Back keeps the app open while history exists");

                runOnMainSync(() -> {
                    web.getSettings().setBlockNetworkLoads(true);
                    web.loadUrl(HOME + "?edoofox_test=" + System.currentTimeMillis());
                });
                await(() -> visibleText(activity.getString(R.string.error_title)), "offline error displayed");
                screenshot("03-offline.png");
                runOnMainSync(() -> {
                    web.getSettings().setBlockNetworkLoads(false);
                    findButton(activity.getWindow().getDecorView(), activity.getString(R.string.retry)).performClick();
                });
                await(() -> js("document.title").contains("Přihlašovací")
                        && !visibleText(activity.getString(R.string.error_title)), "retry recovers");
                runOnMainSync(() -> {
                    CookieManager.getInstance().setCookie(HOME,
                            "edoofox_smoke=persisted; Max-Age=600; Path=/; Secure; SameSite=Lax");
                    CookieManager.getInstance().flush();
                });
                report.putString("stream", "PASS: school login, Plus4U form, internal links, Back, external intent, offline error, retry.\n");
            }
            finish(Activity.RESULT_OK, report);
        } catch (Throwable failure) {
            report.putString("stream", "FAIL: " + failure + "\n");
            finish(Activity.RESULT_CANCELED, report);
        }
    }

    private String js(String script) {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<String> result = new AtomicReference<>("");
        runOnMainSync(() -> web.evaluateJavascript(script, value -> { result.set(value); latch.countDown(); }));
        try { require(latch.await(5, TimeUnit.SECONDS), "JavaScript callback"); }
        catch (InterruptedException e) { throw new AssertionError(e); }
        return result.get();
    }

    private String savedSchool() {
        return getTargetContext().getSharedPreferences("school", 0).getString("subdomain", null);
    }

    private void schoolSelectionChecks() throws Exception {
        require(visibleText(activity.getString(R.string.choose_school)), "first launch asks for school");
        AtomicReference<String> initialUrl = new AtomicReference<>();
        runOnMainSync(() -> initialUrl.set(web.getUrl()));
        require(initialUrl.get() == null, "no school is loaded before selection");
        screenshot("04-school-setup.png");
        runOnMainSync(() -> {
            android.widget.EditText input = activity.findViewById(R.id.school_subdomain);
            input.setText("school.edookit.net.evil.test");
            findButton(activity.getWindow().getDecorView(), activity.getString(R.string.use_school)).performClick();
            require(input.getError() != null, "malformed subdomain rejected");
        });
        require(savedSchool() == null, "invalid input is not persisted");
        chooseSchool(" ZSSLOVANAK ", true);
        require("zsslovanak".equals(savedSchool()), "school normalized and persisted");
        WebView first = web;
        openSchoolMenu();
        screenshot("05-switch-school.png");
        runOnMainSync(() -> activity.findViewById(R.id.cancel_school).performClick());
        require("zsslovanak".equals(savedSchool()), "cancel preserves selection");
        require(!visibleText(activity.getString(R.string.choose_school)), "cancel dismisses picker");
        openSchoolMenu();
        chooseSchool("another-school", true);
        require(web != first, "switch creates fresh navigation history");
        require("another-school".equals(savedSchool()), "new school persisted");
        require(visibleText("another-school.edookit.net"), "header shows selected school");
        runOnMainSync(() -> require(!web.canGoBack(), "previous school's history is inaccessible"));

        String selectedHome = new LinkPolicy("another-school").home;
        runOnMainSync(() -> web.loadDataWithBaseURL(selectedHome,
                "<html><head><title>School routing test</title></head><body><a id='old' href='" + HOME
                        + "'>Old school</a></body></html>", "text/html", "UTF-8", selectedHome));
        await(() -> js("document.title").contains("School routing test"), "new-school fixture loaded");
        IntentFilter external = new IntentFilter(Intent.ACTION_VIEW);
        external.addCategory(Intent.CATEGORY_BROWSABLE);
        external.addDataScheme("https");
        ActivityMonitor monitor = addMonitor(external, new ActivityResult(Activity.RESULT_CANCELED, null), true);
        js("document.getElementById('old').click(); true");
        await(() -> monitor.getHits() == 1, "old school now opens externally");
        removeMonitor(monitor);
        require(js("location.hostname").contains("another-school.edookit.net"), "current school remains selected");
    }

    private void chooseSchool(String value, boolean blockNetwork) {
        runOnMainSync(() -> {
            ((android.widget.EditText) activity.findViewById(R.id.school_subdomain)).setText(value);
            findButton(activity.getWindow().getDecorView(), activity.getString(R.string.use_school)).performClick();
            web = findWeb(activity.getWindow().getDecorView());
            if (blockNetwork) web.getSettings().setBlockNetworkLoads(true);
        });
    }

    private void openSchoolMenu() throws Exception {
        android.accessibilityservice.AccessibilityServiceInfo info = getUiAutomation().getServiceInfo();
        info.flags |= android.accessibilityservice.AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS;
        getUiAutomation().setServiceInfo(info);
        runOnMainSync(() -> findDescription(activity.getWindow().getDecorView(), activity.getString(R.string.more)).performClick());
        getUiAutomation().waitForIdle(100, 3000);
        await(() -> {
            for (android.view.accessibility.AccessibilityWindowInfo window : getUiAutomation().getWindows()) {
                android.view.accessibility.AccessibilityNodeInfo root = window.getRoot();
                if (root == null) continue;
                java.util.List<android.view.accessibility.AccessibilityNodeInfo> items = root.findAccessibilityNodeInfosByText(activity.getString(R.string.switch_school));
                if (items.isEmpty()) continue;
                android.graphics.Rect bounds = new android.graphics.Rect();
                items.get(0).getBoundsInScreen(bounds);
                if (bounds.isEmpty()) continue;
                long now = SystemClock.uptimeMillis();
                MotionEvent down = MotionEvent.obtain(now, now, MotionEvent.ACTION_DOWN, bounds.centerX(), bounds.centerY(), 0);
                MotionEvent up = MotionEvent.obtain(now, now + 100, MotionEvent.ACTION_UP, bounds.centerX(), bounds.centerY(), 0);
                sendPointerSync(down);
                sendPointerSync(up);
                down.recycle();
                up.recycle();
                return true;
            }
            return false;
        }, "Switch school menu clicked");
        await(() -> visibleText(activity.getString(R.string.choose_school)), "switch picker opened");
    }

    private View findDescription(View view, String text) {
        if (text.contentEquals(view.getContentDescription() == null ? "" : view.getContentDescription())) return view;
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                View found = findDescription(group.getChildAt(i), text);
                if (found != null) return found;
            }
        }
        return null;
    }

    private boolean awaitSchoolReady() {
        // Plus4U may first redirect with prompt=none to restore an existing session.
        // Wait for that automatic round trip before tapping the interactive login button.
        long[] readySince = {0};
        String[] lastState = {""};
        await(() -> {
            String current = js("document.readyState !== 'complete' ? 'loading' : document.querySelector('[data-name=Login]') ? 'login' : location.pathname === '/' ? 'home' : 'loading'");
            boolean ready = current.equals("\"login\"") || current.equals("\"home\"");
            if (!ready || !current.equals(lastState[0])) readySince[0] = SystemClock.uptimeMillis();
            lastState[0] = current;
            return ready && SystemClock.uptimeMillis() - readySince[0] >= 5000;
        }, "school login settled after session restoration");
        return lastState[0].equals("\"login\"");
    }

    private void tapElement(String selector) throws Exception {
        org.json.JSONArray rect = new org.json.JSONArray(js("(() => { const r = document.querySelector('"
                + selector + "').getBoundingClientRect(); return [r.x + r.width / 2, r.y + r.height / 2, innerWidth]; })()"));
        int[] location = new int[2];
        AtomicReference<Integer> width = new AtomicReference<>();
        runOnMainSync(() -> { web.getLocationOnScreen(location); width.set(web.getWidth()); });
        float scale = (float) (width.get() / rect.getDouble(2));
        float x = location[0] + (float) rect.getDouble(0) * scale;
        float y = location[1] + (float) rect.getDouble(1) * scale;
        long now = SystemClock.uptimeMillis();
        MotionEvent down = MotionEvent.obtain(now, now, MotionEvent.ACTION_DOWN, x, y, 0);
        MotionEvent up = MotionEvent.obtain(now, now + 100, MotionEvent.ACTION_UP, x, y, 0);
        sendPointerSync(down);
        sendPointerSync(up);
        down.recycle();
        up.recycle();
    }

    private void await(BooleanSupplier predicate, String label) {
        long deadline = SystemClock.uptimeMillis() + 45000;
        while (SystemClock.uptimeMillis() < deadline) {
            if (predicate.getAsBoolean()) return;
            SystemClock.sleep(200);
        }
        throw new AssertionError(label);
    }

    private boolean visibleText(String text) {
        AtomicReference<Boolean> found = new AtomicReference<>(false);
        runOnMainSync(() -> found.set(findText(activity.getWindow().getDecorView(), text)));
        return found.get();
    }

    private boolean findText(View view, String text) {
        if (!view.isShown()) return false;
        if (view instanceof TextView && text.contentEquals(((TextView) view).getText())) return true;
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) if (findText(group.getChildAt(i), text)) return true;
        }
        return false;
    }

    private WebView findWeb(View view) {
        if (view instanceof WebView) return (WebView) view;
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                WebView found = findWeb(group.getChildAt(i));
                if (found != null) return found;
            }
        }
        return null;
    }

    private Button findButton(View view, String text) {
        if (view instanceof Button && text.contentEquals(((Button) view).getText())) return (Button) view;
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                Button found = findButton(group.getChildAt(i), text);
                if (found != null) return found;
            }
        }
        return null;
    }

    private void screenshot(String name) throws Exception {
        waitForIdleSync();
        // Window/Back animations continue after the main thread becomes idle.
        SystemClock.sleep(750);
        Bitmap bitmap = getUiAutomation().takeScreenshot();
        require(bitmap != null, "screenshot captured");
        File directory = new File(getTargetContext().getExternalFilesDir(null), "smoke");
        require(directory.isDirectory() || directory.mkdirs(), "screenshot directory");
        try (FileOutputStream output = new FileOutputStream(new File(directory, name))) {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, output);
        }
        bitmap.recycle();
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
