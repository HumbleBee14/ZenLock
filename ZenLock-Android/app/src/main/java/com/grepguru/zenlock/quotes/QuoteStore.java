package com.grepguru.zenlock.quotes;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.Nullable;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

public final class QuoteStore {

    public static final int MAX_QUOTES = 10;

    private static final String PREFS_NAME = "FocusLockPrefs";
    private static final String KEY_POOL = "quotes_pool";
    private static final Random RANDOM = new Random();


    private QuoteStore() {}

    @Nullable
    public static String random(Context context) {
        List<String> pool = all(context);
        if (pool.isEmpty()) return null;
        return pool.get(RANDOM.nextInt(pool.size()));
    }

    public static boolean hasQuotes(Context context) {
        return !all(context).isEmpty();
    }

    public static List<String> all(Context context) {
        SharedPreferences prefs = prefs(context);
        if (!prefs.contains(KEY_POOL)) save(context, Arrays.asList(context.getResources().getStringArray(com.grepguru.zenlock.R.array.default_quotes)));
        List<String> quotes = new ArrayList<>();
        try {
            JSONArray array = new JSONArray(prefs.getString(KEY_POOL, "[]"));
            for (int i = 0; i < array.length(); i++) quotes.add(array.getString(i));
        } catch (JSONException ignored) {
        }
        return quotes;
    }

    public static boolean canAdd(Context context) {
        return all(context).size() < MAX_QUOTES;
    }

    public static boolean add(Context context, String quote) {
        String trimmed = quote.trim();
        if (trimmed.isEmpty() || !canAdd(context)) return false;
        List<String> quotes = all(context);
        if (quotes.contains(trimmed)) return false;
        quotes.add(0, trimmed);
        save(context, quotes);
        return true;
    }

    public static void remove(Context context, String quote) {
        List<String> quotes = all(context);
        if (quotes.remove(quote)) save(context, quotes);
    }

    private static void save(Context context, List<String> quotes) {
        prefs(context).edit().putString(KEY_POOL, new JSONArray(quotes).toString()).apply();
    }

    private static SharedPreferences prefs(Context context) {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }
}
