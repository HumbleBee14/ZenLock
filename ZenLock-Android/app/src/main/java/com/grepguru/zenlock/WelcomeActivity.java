package com.grepguru.zenlock;

import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.ImageView;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.button.MaterialButton;

public class WelcomeActivity extends AppCompatActivity {

    private static final String PREFS_NAME = "FocusLockPrefs";
    private static final String KEY_SEEN = "onboarding_seen";
    private static final int[] PAGES = {R.layout.welcome_page_intro, R.layout.welcome_page_facts, R.layout.welcome_page_features};

    private ViewPager2 pager;
    private LinearLayout dots;
    private MaterialButton nextButton;
    private ObjectAnimator glowPulse;

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

        pager = findViewById(R.id.welcomePager);
        dots = findViewById(R.id.welcomeDots);
        nextButton = findViewById(R.id.welcomeNext);

        pager.setAdapter(new PageAdapter());
        pager.setOffscreenPageLimit(1);
        pager.setPageTransformer(this::transformPage);
        pager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                updateDots(position);
                nextButton.setText(position == PAGES.length - 1 ? "Get started" : "Next");
            }
        });

        nextButton.setOnClickListener(v -> {
            int current = pager.getCurrentItem();
            if (current == PAGES.length - 1) finishWelcome();
            else pager.setCurrentItem(current + 1, true);
        });
    }

    private static final int[] FACT_CARDS = {R.id.factCard1, R.id.factCard2, R.id.factCard3, R.id.factCard4};
    private static final float[] FACT_TILT = {-2.5f, 2f, -1.5f, 1.5f};

    private void transformPage(View page, float position) {
        if (page.findViewById(R.id.welcomeHero) != null) {
            transformIntro(page, position);
        } else if (page.findViewById(FACT_CARDS[0]) != null) {
            transformFacts(page, position);
        } else {
            page.setTranslationX(0f);
            page.setAlpha(1f);
            page.setTranslationZ(position < 0 ? 0f : 1f);
        }
    }

    private void transformIntro(View page, float position) {
        View hero = page.findViewById(R.id.welcomeHero);
        boolean leaving = position < 0 && position > -1f;
        float progress = leaving ? -position : 0f;
        page.setTranslationX(leaving ? -position * page.getWidth() : 0f);
        page.setTranslationZ(0f);
        page.setAlpha(1f - progress);
        float scale = 1f + progress * 0.6f;
        hero.setScaleX(scale);
        hero.setScaleY(scale);
    }

    private void transformFacts(View page, float position) {
        boolean arriving = position > 0 && position < 1f;
        boolean leaving = position < 0 && position > -1f;
        page.setTranslationX(arriving || leaving ? -position * page.getWidth() : 0f);
        page.setTranslationZ(arriving ? 1f : 0f);
        page.setAlpha(arriving ? 1f - position : 1f);
        float arriveScale = arriving ? 0.94f + 0.06f * (1f - position) : 1f;
        page.setScaleX(arriveScale);
        page.setScaleY(arriveScale);

        float progress = Math.max(0f, Math.min(1f, -position));
        float density = getResources().getDisplayMetrics().density;
        for (int i = 0; i < FACT_CARDS.length; i++) {
            View card = page.findViewById(FACT_CARDS[i]);
            if (card == null) continue;
            float delay = i * 0.12f;
            float local = Math.max(0f, Math.min(1f, (progress - delay) / (1f - delay)));
            float eased = local * local;
            float side = i % 2 == 0 ? -1f : 1f;
            card.setTranslationY(eased * page.getHeight() * 1.1f);
            card.setTranslationX(eased * side * 70f * density);
            card.setRotation(FACT_TILT[i] + eased * side * 38f);
            card.setAlpha(1f - eased * 0.5f);
        }
    }

    private void updateDots(int position) {
        for (int i = 0; i < dots.getChildCount(); i++) {
            ((ImageView) dots.getChildAt(i)).setImageResource(i == position ? R.drawable.dot_active : R.drawable.dot_inactive);
        }
    }

    private void startGlow(View glow) {
        if (glowPulse != null) glowPulse.cancel();
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

    private class PageAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

        @Override
        public int getItemViewType(int position) {
            return PAGES[position];
        }

        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(viewType, parent, false);
            return new RecyclerView.ViewHolder(view) {};
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
            View glow = holder.itemView.findViewById(R.id.welcomeGlow);
            if (glow != null) startGlow(glow);
        }

        @Override
        public int getItemCount() {
            return PAGES.length;
        }
    }
}
