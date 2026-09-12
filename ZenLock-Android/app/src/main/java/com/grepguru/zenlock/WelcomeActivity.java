package com.grepguru.zenlock;

import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;

public class WelcomeActivity extends AppCompatActivity {

    private static final String PREFS_NAME = "FocusLockPrefs";
    private static final String KEY_SEEN = "onboarding_seen";

    private View introPage;
    private View featuresPage;
    private ImageView introDot;
    private ImageView featuresDot;
    private MaterialButton nextButton;
    private ObjectAnimator glowPulse;
    private boolean showingFeatures;

    public static boolean shouldShow(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        if (prefs.getBoolean(KEY_SEEN, false)) return false;
        boolean existingUser = prefs.contains("isLocked") || prefs.contains("whitelisted_apps") || prefs.contains("unlock_pin");
        if (existingUser) {
            prefs.edit().putBoolean(KEY_SEEN, true).apply();
            return false;
        }
        return true;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_welcome);

        introPage = findViewById(R.id.welcomePageIntro);
        featuresPage = findViewById(R.id.welcomePageFeatures);
        introDot = findViewById(R.id.welcomeDotIntro);
        featuresDot = findViewById(R.id.welcomeDotFeatures);
        nextButton = findViewById(R.id.welcomeNext);

        nextButton.setOnClickListener(v -> {
            if (showingFeatures) finishWelcome();
            else showFeatures();
        });
        startGlow(findViewById(R.id.welcomeGlow));
    }

    private void startGlow(View glow) {
        glowPulse = ObjectAnimator.ofPropertyValuesHolder(glow,
                PropertyValuesHolder.ofFloat(View.SCALE_X, 0.9f, 1.1f),
                PropertyValuesHolder.ofFloat(View.SCALE_Y, 0.9f, 1.1f),
                PropertyValuesHolder.ofFloat(View.ALPHA, 0.6f, 1f));
        glowPulse.setDuration(1800);
        glowPulse.setRepeatCount(ValueAnimator.INFINITE);
        glowPulse.setRepeatMode(ValueAnimator.REVERSE);
        glowPulse.setInterpolator(new AccelerateDecelerateInterpolator());
        glowPulse.start();
    }

    private void showFeatures() {
        showingFeatures = true;
        introPage.animate().alpha(0f).setDuration(200).withEndAction(() -> {
            introPage.setVisibility(View.GONE);
            featuresPage.setAlpha(0f);
            featuresPage.setVisibility(View.VISIBLE);
            featuresPage.animate().alpha(1f).setDuration(250).start();
        }).start();
        introDot.setImageResource(R.drawable.dot_inactive);
        featuresDot.setImageResource(R.drawable.dot_active);
        nextButton.setText("Get started");
    }

    private void finishWelcome() {
        getSharedPreferences(PREFS_NAME, MODE_PRIVATE).edit().putBoolean(KEY_SEEN, true).apply();
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    @Override
    protected void onDestroy() {
        if (glowPulse != null) glowPulse.cancel();
        super.onDestroy();
    }
}
