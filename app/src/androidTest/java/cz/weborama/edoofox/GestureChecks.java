package cz.weborama.edoofox;

import android.app.Activity;
import android.app.Instrumentation;
import android.content.Intent;
import android.os.SystemClock;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BooleanSupplier;

/** Local fixtures only, in a separate application sandbox. */
final class GestureChecks {
    private final Instrumentation test;
    private WebView web;
    private BrowserGestureLayout chrome;
    private final AtomicInteger loads = new AtomicInteger();
    GestureChecks(Instrumentation test) { this.test = test; }
    void run() {
        check(test.getTargetContext().getPackageName().endsWith(".qa"), "QA package required");
        test.getTargetContext().getSharedPreferences("school", 0).edit().putString("subdomain", "gesture-test").commit();
        Activity activity = test.startActivitySync(new Intent(test.getTargetContext(), MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        test.runOnMainSync(() -> {
            web = find(activity.getWindow().getDecorView(), WebView.class);
            chrome = find(activity.getWindow().getDecorView(), BrowserGestureLayout.class);
            web.stopLoading();
            WebViewClient original = web.getWebViewClient();
            web.setWebViewClient(new WebViewClient() {
                @Override public WebResourceResponse shouldInterceptRequest(WebView v, WebResourceRequest r) {
                    String html = "<meta name='viewport' content='width=device-width,initial-scale=1'><body style='margin:0;background:#faf8fd'>"
                            + (r.getUrl().getPath().equals("/login") ? "<div data-name='Login'>Sign in</div>" : "")
                            + "<h1>Gesture test</h1><div id='nested' style='height:120px;overflow:auto'><div style='height:900px'>Nested scroll</div></div>"
                            + "<div style='height:3500px;padding:20px'>Edoofox local test page</div></body>";
                    return new WebResourceResponse("text/html", "UTF-8", new ByteArrayInputStream(html.getBytes(StandardCharsets.UTF_8)));
                }
                @Override public void onPageStarted(WebView v, String url, android.graphics.Bitmap icon) { loads.incrementAndGet(); original.onPageStarted(v,url,icon); }
                @Override public void onPageFinished(WebView v, String url) { original.onPageFinished(v,url); }
            });
            web.loadUrl("https://gesture-test.edookit.net/login");
        });
        waitFor(() -> web.getProgress() == 100 && web.getUrl().endsWith("/login"), "login fixture");
        SystemClock.sleep(2500);
        check(main(chrome::isHeaderShown), "signed-out header remains visible");
        swipe(.5f,.8f,.5f,.3f,false);
        check(main(chrome::isHeaderShown), "scrolling cannot hide login header");
        test.runOnMainSync(() -> web.loadUrl("https://gesture-test.edookit.net/one"));
        waitFor(() -> web.getProgress() == 100 && web.getUrl().endsWith("/one"), "first fixture");
        waitFor(chrome::isHeaderHidden, "content automatically hides header without scrolling");
        test.runOnMainSync(() -> web.loadUrl("https://gesture-test.edookit.net/two"));
        waitFor(() -> web.getProgress() == 100 && web.getUrl().endsWith("/two"), "second fixture");
        swipe(.25f,.55f,.8f,.55f,false);
        waitFor(() -> web.getUrl().endsWith("/one"), "swipe Back");
        swipe(.8f,.55f,.25f,.55f,false);
        waitFor(() -> web.getUrl().endsWith("/two"), "swipe Forward");
        swipe(.5f,.8f,.5f,.3f,false);
        waitFor(() -> chrome.isHeaderHidden(), "header hides");
        check(main(() -> ((View) web.getParent()).getTop() == 0), "no header gap");
        screenshot("gesture-hidden.png");
        test.runOnMainSync(() -> web.scrollTo(0,0));
        SystemClock.sleep(500);
        int before = loads.get();
        swipe(.5f,.4f,.5f,.8f,false);
        waitFor(() -> chrome.isHeaderShown(), "first pull reveals header");
        check(loads.get() == before, "first pull does not reload");
        SystemClock.sleep(2200);
        check(main(chrome::isHeaderShown), "periodic page checks leave manually revealed menu open");
        swipe(.5f,.4f,.5f,.47f,false);
        check(loads.get() == before, "short pull does not reload");
        swipe(.5f,.4f,.5f,.8f,true);
        check(loads.get() == before, "cancel does not reload");
        swipe(.5f,.4f,.5f,.8f,false);
        waitFor(() -> loads.get() == before + 1 && web.getProgress() == 100, "second pull refreshes once");
        screenshot("gesture-header.png");
        test.runOnMainSync(() -> web.evaluateJavascript("document.getElementById('nested').scrollTop=300", null));
        SystemClock.sleep(300);
        int nestedBefore = loads.get();
        // The nested scroller starts below the heading, within the first 200 CSS pixels.
        swipePixels(150,110,150,300,false);
        check(loads.get() == nestedBefore, "nested scroll does not refresh");
        check(Double.parseDouble(js("document.getElementById('nested').scrollTop")) < 300, "nested content actually scrolls");
        js("document.body.innerHTML=\"<div style='height:250px;overflow:auto'><div style='width:2000px;height:220px'>Horizontal table</div></div>\"");
        swipePixels(70,110,300,110,false);
        check(main(() -> web.getUrl().endsWith("/two")), "horizontal widget does not navigate");
        js("document.body.innerHTML=\"<input style='width:95%;height:220px' value='Editable text'>\"");
        swipePixels(70,110,300,110,false);
        check(main(() -> web.getUrl().endsWith("/two")), "input does not navigate");
        js("document.body.innerHTML=\"<div data-name='Login'>Sign in again</div><div style='height:3000px'>Login fixture</div>\"");
        waitFor(chrome::isHeaderShown, "same-document logout reveals header");
        swipe(.5f,.8f,.5f,.3f,false);
        check(main(chrome::isHeaderShown), "logout header stays pinned");
        js("document.querySelector('[data-name=Login]').remove()");
        waitFor(chrome::isHeaderHidden, "same-document login hides header");
        js("document.body.innerHTML=''");
        waitFor(chrome::isHeaderShown, "blank content keeps header available");
        test.runOnMainSync(() -> web.loadUrl("https://uuidentity.plus4u.net/uu-oidc-maing02/test"));
        waitFor(chrome::isHeaderShown, "Plus4U header visible");
        SystemClock.sleep(2200);
        swipe(.5f,.8f,.5f,.3f,false);
        check(main(chrome::isHeaderShown), "Plus4U header stays visible");
        test.runOnMainSync(activity::finish);
    }
    private String js(String script) {
        java.util.concurrent.CountDownLatch done = new java.util.concurrent.CountDownLatch(1);
        AtomicReference<String> result = new AtomicReference<>();
        test.runOnMainSync(() -> web.evaluateJavascript(script, value -> { result.set(value); done.countDown(); }));
        try { check(done.await(5, java.util.concurrent.TimeUnit.SECONDS), "script finished"); }
        catch (InterruptedException e) { throw new AssertionError(e); }
        SystemClock.sleep(250);
        return result.get();
    }
    private void screenshot(String name) {
        java.io.File dir = new java.io.File(test.getTargetContext().getExternalFilesDir(null), "smoke");
        check(dir.isDirectory() || dir.mkdirs(), "screenshot directory");
        android.graphics.Bitmap bitmap = test.getUiAutomation().takeScreenshot();
        try (java.io.FileOutputStream out = new java.io.FileOutputStream(new java.io.File(dir,name))) {
            bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG,100,out);
        } catch(java.io.IOException e) { throw new AssertionError(e); }
        finally { bitmap.recycle(); }
    }
    private boolean main(BooleanSupplier condition) {
        AtomicReference<Boolean> result = new AtomicReference<>(false);
        test.runOnMainSync(() -> result.set(condition.getAsBoolean()));
        return result.get();
    }
    private void waitFor(BooleanSupplier condition, String label) {
        long end = SystemClock.uptimeMillis()+10000;
        while (SystemClock.uptimeMillis()<end) { if(main(condition)) { SystemClock.sleep(400); return; } SystemClock.sleep(100); }
        throw new AssertionError(label);
    }
    private void swipe(float x1,float y1,float x2,float y2,boolean cancel) {
        int[] size = new int[2]; test.runOnMainSync(() -> {size[0]=web.getWidth();size[1]=web.getHeight();});
        gesture(x1*size[0],y1*size[1],x2*size[0],y2*size[1],cancel);
    }
    private void swipePixels(float x1,float y1,float x2,float y2,boolean cancel) {
        float d = test.getTargetContext().getResources().getDisplayMetrics().density;
        gesture(x1*d,y1*d,x2*d,y2*d,cancel);
    }
    private void gesture(float x1,float y1,float x2,float y2,boolean cancel) {
        int[] origin = new int[2]; test.runOnMainSync(() -> web.getLocationOnScreen(origin));
        long down=SystemClock.uptimeMillis();
        send(down,MotionEvent.ACTION_DOWN,origin[0]+x1,origin[1]+y1);
        SystemClock.sleep(80);
        for(int i=1;i<=12;i++) { send(down,MotionEvent.ACTION_MOVE,origin[0]+x1+(x2-x1)*i/12,origin[1]+y1+(y2-y1)*i/12); SystemClock.sleep(20); }
        send(down,cancel?MotionEvent.ACTION_CANCEL:MotionEvent.ACTION_UP,origin[0]+x2,origin[1]+y2);
        SystemClock.sleep(650);
    }
    private void send(long down,int action,float x,float y) {
        MotionEvent event=MotionEvent.obtain(down,SystemClock.uptimeMillis(),action,x,y,0);
        test.sendPointerSync(event); event.recycle();
    }
    private static void check(boolean value,String message) { if(!value) throw new AssertionError(message); }
    private static <T extends View> T find(View view,Class<T> type) {
        if(type.isInstance(view)) return type.cast(view);
        if(view instanceof ViewGroup group) for(int i=0;i<group.getChildCount();i++) {T result=find(group.getChildAt(i),type);if(result!=null)return result;}
        return null;
    }
}
