package cz.weborama.edoofox;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

/** Native, scrollable About sheet; DialogFragment restores it after configuration changes. */
public class AboutDialog extends DialogFragment {
    public static final String TAG = "about";

    @Override public Dialog onCreateDialog(Bundle state) {
        MainActivity activity = (MainActivity) getActivity();
        ScrollView scroll = new ScrollView(activity);
        LinearLayout body = new LinearLayout(activity);
        body.setOrientation(LinearLayout.VERTICAL);
        body.setGravity(Gravity.CENTER_HORIZONTAL);
        body.setPadding(dp(24), dp(16), dp(24), dp(16));
        ImageView fox = new ImageView(activity);
        fox.setImageResource(R.drawable.ic_fox);
        fox.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        body.addView(fox, new LinearLayout.LayoutParams(dp(88), dp(88)));
        TextView name = text(getString(R.string.app_name), 26);
        name.setTextColor(Color.rgb(103, 51, 156));
        name.setTypeface(null, android.graphics.Typeface.BOLD);
        body.addView(name);
        body.addView(text(getString(R.string.about_slogan), 17));
        body.addView(text(getString(R.string.about_unofficial), 14));
        body.addView(text(getString(R.string.about_version, BuildConfig.VERSION_NAME), 14));
        body.addView(text(getString(R.string.about_author, getString(R.string.author_name)), 16));
        Button github = new Button(activity);
        github.setText(R.string.github_project);
        github.setOnClickListener(v -> activity.openProjectPage());
        body.addView(github, new LinearLayout.LayoutParams(-1, -2));
        scroll.addView(body);
        return new AlertDialog.Builder(activity)
                .setTitle(R.string.about)
                .setView(scroll)
                .setPositiveButton(R.string.close, (dialog, which) -> dismiss())
                .create();
    }

    private TextView text(String value, int size) {
        TextView label = new TextView(getActivity());
        label.setText(value);
        label.setTextSize(size);
        label.setTextColor(Color.DKGRAY);
        label.setGravity(Gravity.CENTER);
        label.setPadding(0, dp(8), 0, dp(8));
        return label;
    }

    private int dp(int value) { return Math.round(value * getResources().getDisplayMetrics().density); }
}
