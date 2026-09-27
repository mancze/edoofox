package cz.weborama.edoofox;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.ScrollView;
import android.widget.TextView;

/** Scrollable, grouped menu with visible icons on every supported Android version. */
final class AppMenu {
    private final Context context;
    private final LinearLayout items;
    private final PopupWindow popup;

    AppMenu(Context context) {
        this.context = context;
        items = new LinearLayout(context);
        items.setOrientation(LinearLayout.VERTICAL);
        items.setPadding(0, dp(8), 0, dp(8));
        ScrollView scroll = new ScrollView(context);
        scroll.setFillViewport(false);
        scroll.addView(items);
        int width = Math.min(dp(288), context.getResources().getDisplayMetrics().widthPixels - dp(24));
        popup = new PopupWindow(scroll, width, -2, true);
        GradientDrawable background = new GradientDrawable();
        background.setColor(Color.WHITE);
        background.setCornerRadius(dp(12));
        popup.setBackgroundDrawable(background);
        popup.setElevation(dp(8));
        popup.setOutsideTouchable(true);
        popup.setInputMethodMode(PopupWindow.INPUT_METHOD_NOT_NEEDED);
    }

    void group(int title) {
        if (items.getChildCount() > 0) {
            View divider = new View(context);
            divider.setBackgroundColor(0xffe6e0eb);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, dp(1));
            params.setMargins(dp(16), dp(8), dp(16), dp(4));
            items.addView(divider, params);
        }
        TextView heading = new TextView(context);
        heading.setText(title);
        heading.setTextColor(0xff62556d);
        heading.setTextSize(12);
        heading.setPadding(dp(16), dp(8), dp(16), dp(8));
        if (Build.VERSION.SDK_INT >= 28) heading.setAccessibilityHeading(true);
        items.addView(heading);
    }

    void action(int label, int icon, boolean enabled, Runnable action) {
        TextView row = new TextView(context);
        row.setText(label);
        row.setTextColor(0xff29232e);
        row.setTextSize(16);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(16), dp(12), dp(16), dp(12));
        row.setMinHeight(dp(48));
        row.setCompoundDrawablesRelativeWithIntrinsicBounds(icon, 0, 0, 0);
        row.setCompoundDrawablePadding(dp(16));
        android.util.TypedValue selectable = new android.util.TypedValue();
        context.getTheme().resolveAttribute(android.R.attr.selectableItemBackground, selectable, true);
        row.setBackgroundResource(selectable.resourceId);
        row.setEnabled(enabled);
        row.setAlpha(enabled ? 1f : .38f);
        row.setFocusable(enabled);
        row.setOnClickListener(v -> { popup.dismiss(); action.run(); });
        items.addView(row, new LinearLayout.LayoutParams(-1, -2));
    }

    void show(View anchor) { popup.showAsDropDown(anchor, 0, 0, Gravity.END); }

    private int dp(int value) {
        return Math.round(value * context.getResources().getDisplayMetrics().density);
    }
}
