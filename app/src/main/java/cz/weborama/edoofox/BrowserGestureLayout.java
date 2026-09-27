package cz.weborama.edoofox;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.SystemClock;
import android.view.Gravity;
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.accessibility.AccessibilityManager;
import android.webkit.WebView;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.TextView;
import org.json.JSONObject;

/** Browser controls and gestures, without overriding Android's system edge gestures. */
public final class BrowserGestureLayout extends FrameLayout {
    private enum Mode { UNDECIDED, SCROLL, PULL, HISTORY, IGNORE }
    private View toolbar;
    private View page;
    private WebView web;
    private Runnable refresh;
    private final TextView feedback;
    private final ImageButton menuButton;
    private final int barHeight;
    private final int slop;
    private float offset;
    private ValueAnimator animator;
    private boolean enabled;
    private boolean loading;
    private boolean autoHideHeader;
    private Mode mode = Mode.IGNORE;
    private float downX, downY, lastY, startOffset, distance;
    private long downTime;
    private int sequence;
    private boolean probed, protectedTarget, horizontalContent, canScrollUp, canScrollDown;
    private boolean refreshOnThisPull, thresholdReached, tracking, pageAtTopOnDown;
    private int historyDirection;
    private final AccessibilityManager.TouchExplorationStateChangeListener explorationListener = active -> {
        if (active) { cancelGesture(); reveal(); }
    };

    public BrowserGestureLayout(Context context) {
        super(context);
        barHeight = dp(52);
        slop = ViewConfiguration.get(context).getScaledTouchSlop();
        setClipChildren(true);
        feedback = new TextView(context);
        feedback.setTextSize(15);
        feedback.setTextColor(Color.rgb(103, 51, 156));
        feedback.setGravity(Gravity.CENTER);
        feedback.setPadding(dp(18), dp(12), dp(18), dp(12));
        feedback.setElevation(dp(5));
        GradientDrawable background = new GradientDrawable();
        background.setColor(Color.WHITE);
        background.setCornerRadius(dp(28));
        feedback.setBackground(background);
        feedback.setVisibility(GONE);
        feedback.setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
        menuButton = new ImageButton(context);
        menuButton.setId(R.id.show_app_menu);
        menuButton.setImageResource(R.drawable.ic_fox);
        menuButton.setScaleType(android.widget.ImageView.ScaleType.FIT_CENTER);
        menuButton.setContentDescription(context.getString(R.string.show_app_menu));
        menuButton.setTooltipText(context.getString(R.string.show_app_menu));
        GradientDrawable circle = new GradientDrawable();
        circle.setShape(GradientDrawable.OVAL);
        circle.setColor(Color.WHITE);
        circle.setStroke(dp(1), Color.rgb(226, 212, 240));
        menuButton.setBackground(new android.graphics.drawable.RippleDrawable(
                android.content.res.ColorStateList.valueOf(Color.argb(45, 103, 51, 156)), circle, null));
        menuButton.setPadding(dp(10), dp(10), dp(10), dp(10));
        menuButton.setElevation(dp(6));
        menuButton.setVisibility(GONE);
        menuButton.setOnClickListener(v -> { cancelGesture(); reveal(); });
    }

    public void setPanels(View toolbar, View page) {
        this.toolbar = toolbar;
        this.page = page;
        LayoutParams pageParams = new LayoutParams(-1, -1);
        pageParams.topMargin = barHeight;
        addView(page, pageParams);
        addView(toolbar, new LayoutParams(-1, barHeight, Gravity.TOP));
        addView(feedback, new LayoutParams(-2, -2, Gravity.TOP | Gravity.CENTER_HORIZONTAL));
        LayoutParams buttonParams = new LayoutParams(dp(56), dp(56), Gravity.BOTTOM | Gravity.END);
        buttonParams.setMargins(dp(16), dp(16), dp(16), dp(16));
        addView(menuButton, buttonParams);
    }

    public void attach(WebView view, Runnable refreshAction) {
        cancelGesture();
        web = view;
        view.setOnScrollChangeListener((v, x, y, oldX, oldY) -> updateMenuButton());
        refresh = refreshAction;
        reveal();
    }

    public void setGesturesEnabled(boolean value) {
        boolean changed = enabled != value;
        enabled = value;
        if (!value) { cancelGesture(); reveal(); }
        else if (changed) hideHeaderIfAllowed();
        updateMenuButton();
    }

    public void setHeaderAutoHide(boolean value) {
        if (autoHideHeader == value) {
            if (value) hideHeaderIfAllowed();
            return;
        }
        autoHideHeader = value;
        if (value) hideHeaderIfAllowed();
        else { cancelGesture(); reveal(); }
    }

    private void hideHeaderIfAllowed() {
        if (autoHideHeader && usable() && !tracking) animateHeader(barHeight);
    }

    public void setLoading(boolean value) { loading = value; updateMenuButton(); }
    public boolean isHeaderShown() { return offset < 1; }
    public boolean isHeaderHidden() { return offset >= barHeight - 1; }
    public void reveal() { animateHeader(0); }

    private boolean usable() {
        AccessibilityManager accessibility = getContext().getSystemService(AccessibilityManager.class);
        return enabled && web != null && web.isShown() && !accessibility.isTouchExplorationEnabled();
    }

    @Override public boolean dispatchTouchEvent(MotionEvent event) {
        if (event.getActionMasked() == MotionEvent.ACTION_DOWN) {
            boolean touchingMenu = menuButton.isShown() && event.getX() >= menuButton.getLeft()
                    && event.getX() <= menuButton.getRight() && event.getY() >= menuButton.getTop()
                    && event.getY() <= menuButton.getBottom();
            tracking = !touchingMenu && usable() && event.getY() >= page.getTop() && event.getY() < page.getBottom();
            mode = tracking ? Mode.UNDECIDED : Mode.IGNORE;
            sequence++;
            downX = event.getX(); downY = lastY = event.getY();
            downTime = SystemClock.uptimeMillis();
            startOffset = offset;
            pageAtTopOnDown = web != null && !web.canScrollVertically(-1);
            refreshOnThisPull = isHeaderShown() && !loading;
            thresholdReached = false;
            probed = false;
            distance = 0;
            if (tracking) probePage(downX - page.getLeft(), downY - page.getTop(), sequence);
            else if (!usable()) reveal();
        }
        if (event.getPointerCount() > 1) {
            mode = Mode.IGNORE;
            clearFeedback();
        }
        boolean result = super.dispatchTouchEvent(event);
        if (event.getActionMasked() == MotionEvent.ACTION_UP || event.getActionMasked() == MotionEvent.ACTION_CANCEL) {
            if (mode == Mode.SCROLL) animateHeader(offset > barHeight / 2f ? barHeight : 0);
            tracking = false;
        }
        return result;
    }

    @Override public boolean onInterceptTouchEvent(MotionEvent event) {
        if (!tracking || !usable() || event.getPointerCount() != 1) return false;
        if (mode == Mode.PULL || mode == Mode.HISTORY) return true;
        if (event.getActionMasked() != MotionEvent.ACTION_MOVE) return false;
        float dx = event.getX() - downX;
        float dy = event.getY() - downY;
        if (mode == Mode.UNDECIDED && probed && Math.max(Math.abs(dx), Math.abs(dy)) > slop) {
            if (protectedTarget || SystemClock.uptimeMillis() - downTime > ViewConfiguration.getLongPressTimeout()) {
                mode = Mode.IGNORE;
            } else if (Math.abs(dx) > Math.abs(dy) * 1.6f && Math.abs(dx) > dp(24)) {
                historyDirection = dx > 0 ? -1 : 1;
                // Leave edge swipes to Android, and horizontal widgets/tables to the page.
                boolean awayFromEdges = downX > dp(32) && downX < getWidth() - dp(32);
                if (awayFromEdges && !horizontalContent && !web.canScrollHorizontally(historyDirection)
                        && web.canGoBackOrForward(historyDirection)) {
                    mode = Mode.HISTORY;
                } else mode = Mode.IGNORE;
            } else if (Math.abs(dy) > Math.abs(dx) * 1.4f) {
                stopAnimation();
                if (dy > 0 && pageAtTopOnDown && !canScrollUp && !web.canScrollVertically(-1)
                        && (startOffset > 0 || refreshOnThisPull)) mode = Mode.PULL;
                else mode = Mode.SCROLL;
            }
        }
        if (mode == Mode.SCROLL) {
            if (canScrollDown || canScrollUp || web.canScrollVertically(1) || web.canScrollVertically(-1) || dy > 0) {
                // Ordinary page scrolling may hide controls, but must never reveal them.
                setHeaderOffset(offset + Math.max(0, lastY - event.getY()));
            }
            lastY = event.getY();
        }
        return mode == Mode.PULL || mode == Mode.HISTORY;
    }

    // This container has no click action: taps belong to its children and all gestures
    // have accessible toolbar equivalents. Physical left/right follows the swipe, not text direction.
    @android.annotation.SuppressLint({"ClickableViewAccessibility", "RtlHardcoded"})
    @Override public boolean onTouchEvent(MotionEvent event) {
        if (mode != Mode.PULL && mode != Mode.HISTORY) return tracking;
        if (event.getActionMasked() == MotionEvent.ACTION_MOVE) {
            if (mode == Mode.PULL) {
                distance = Math.max(0, event.getY() - downY);
                if (refreshOnThisPull) {
                    float progress = Math.min(1, distance / dp(180));
                    page.setTranslationY(Math.min(dp(56), distance * .3f));
                    showFeedback(distance >= dp(180) ? R.string.release_refresh : R.string.pull_refresh,
                            Gravity.TOP | Gravity.CENTER_HORIZONTAL, progress);
                    notifyThreshold(distance >= dp(180));
                } else {
                    setHeaderOffset(startOffset - distance * .75f);
                }
            } else {
                distance = (event.getX() - downX) * -historyDirection;
                float progress = Math.max(0, Math.min(1, distance / historyThreshold()));
                showFeedback(historyDirection == -1 ? R.string.gesture_back : R.string.gesture_forward,
                        Gravity.CENTER_VERTICAL | (historyDirection == -1 ? Gravity.LEFT : Gravity.RIGHT), progress);
                notifyThreshold(distance >= historyThreshold());
            }
            return true;
        }
        if (event.getActionMasked() == MotionEvent.ACTION_UP || event.getActionMasked() == MotionEvent.ACTION_CANCEL) {
            boolean released = event.getActionMasked() == MotionEvent.ACTION_UP;
            distance = mode == Mode.PULL ? Math.max(0, event.getY() - downY)
                    : (event.getX() - downX) * -historyDirection;
            if (mode == Mode.PULL) {
                if (refreshOnThisPull) {
                    if (released && thresholdReached && distance >= dp(180) && !loading && usable()) refresh.run();
                } else animateHeader(released && distance >= dp(28) ? 0 : startOffset);
            } else if (released && distance >= historyThreshold() && usable() && web.canGoBackOrForward(historyDirection)) {
                web.goBackOrForward(historyDirection);
            }
            clearFeedback();
            mode = Mode.IGNORE;
            return true;
        }
        return true;
    }

    private float historyThreshold() { return Math.max(dp(96), Math.min(dp(160), getWidth() * .22f)); }

    private void notifyThreshold(boolean reached) {
        if (reached && !thresholdReached) performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
        thresholdReached = reached;
    }

    private void showFeedback(int text, int gravity, float progress) {
        LayoutParams params = (LayoutParams) feedback.getLayoutParams();
        params.gravity = gravity;
        params.setMargins(dp(16), Math.round(barHeight - offset) + dp(12), dp(16), 0);
        feedback.setLayoutParams(params);
        feedback.setText(text);
        feedback.setAlpha(.45f + progress * .55f);
        feedback.setVisibility(VISIBLE);
    }

    private void clearFeedback() {
        feedback.setVisibility(GONE);
        if (page != null) page.setTranslationY(0);
    }

    private void cancelGesture() {
        sequence++;
        mode = Mode.IGNORE;
        tracking = false;
        clearFeedback();
    }

    private void stopAnimation() {
        if (animator != null) { animator.cancel(); animator = null; }
    }

    private void animateHeader(float target) {
        if (toolbar == null) return;
        stopAnimation();
        if (Math.abs(offset - target) < 1) { setHeaderOffset(target); return; }
        animator = ValueAnimator.ofFloat(offset, target);
        animator.setDuration(180);
        animator.addUpdateListener(animation -> setHeaderOffset((float) animation.getAnimatedValue()));
        animator.start();
    }

    private void setHeaderOffset(float value) {
        offset = autoHideHeader ? Math.max(0, Math.min(barHeight, value)) : 0;
        toolbar.setTranslationY(-offset);
        toolbar.setImportantForAccessibility(isHeaderHidden() ? IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS : IMPORTANT_FOR_ACCESSIBILITY_AUTO);
        LayoutParams params = (LayoutParams) page.getLayoutParams();
        int margin = Math.round(barHeight - offset);
        if (params.topMargin != margin) { params.topMargin = margin; page.setLayoutParams(params); }
        updateMenuButton();
    }

    private void updateMenuButton() {
        // Leave the website's scroll-to-top control untouched. This shortcut only
        // occupies the corner once the document has returned to the top.
        boolean show = autoHideHeader && isHeaderHidden() && !loading && usable()
                && web.getScrollY() == 0 && !web.canScrollVertically(-1);
        menuButton.setVisibility(show ? VISIBLE : GONE);
    }

    private void probePage(float x, float y, int request) {
        WebView source = web;
        // Read layout only. No bridge or page modification. Inspect nested scroll containers,
        // since WebView.canScrollVertically alone cannot see a scrolling element inside HTML.
        String script = "(() => { const s=innerWidth/" + Math.max(1, web.getWidth()) + ";"
                + "let e=document.elementFromPoint(" + x + "*s," + y + "*s), up=false, down=false, horizontal=false;"
                + "const edit=!!(e && e.closest('input,textarea,select,[contenteditable]:not([contenteditable=false]),video,audio,iframe,canvas,[role=slider]'));"
                + "for(;e;e=e.parentElement){const c=getComputedStyle(e);"
                + "if(e.scrollWidth>e.clientWidth+2 && /(auto|scroll)/.test(c.overflowX)) horizontal=true;"
                + "if(e===document.scrollingElement || /(auto|scroll)/.test(c.overflowY)){"
                + "up=up||e.scrollTop>1; down=down||e.scrollTop+e.clientHeight<e.scrollHeight-1;}}"
                + "return {edit,up,down,horizontal}; })()";
        source.evaluateJavascript(script, result -> {
            if (request != sequence || source != web || !tracking) return;
            try {
                JSONObject state = new JSONObject(result);
                protectedTarget = state.getBoolean("edit");
                horizontalContent = state.getBoolean("horizontal");
                canScrollUp = state.getBoolean("up");
                canScrollDown = state.getBoolean("down");
                probed = true;
            } catch (Exception ignored) { mode = Mode.IGNORE; }
        });
    }

    @Override protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        getContext().getSystemService(AccessibilityManager.class)
                .addTouchExplorationStateChangeListener(explorationListener);
    }

    @Override protected void onDetachedFromWindow() {
        getContext().getSystemService(AccessibilityManager.class)
                .removeTouchExplorationStateChangeListener(explorationListener);
        cancelGesture();
        stopAnimation();
        super.onDetachedFromWindow();
    }

    private int dp(int value) { return Math.round(value * getResources().getDisplayMetrics().density); }
}
