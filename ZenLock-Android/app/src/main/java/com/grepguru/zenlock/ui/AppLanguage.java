package com.grepguru.zenlock.ui;

import android.app.LocaleManager;
import android.content.Context;
import android.content.res.Configuration;
import android.os.Build;
import android.os.LocaleList;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.app.LocaleManagerCompat;
import androidx.core.os.LocaleListCompat;
import com.grepguru.zenlock.R;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

/** Resource-backed catalogue; framework persistence on 33+, explicit AppCompat restoration below 33. */
public final class AppLanguage {
    private static final String PENDING_LEGACY = "pending_legacy_language";
    private static final String INITIALIZED = "language_initialized";
    private static final String ANDROID_NS = "http://schemas.android.com/apk/res/android";
    private AppLanguage() { }

    public static String[] languageTags(Context context) {
        List<String> tags = new ArrayList<>();
        try (android.content.res.XmlResourceParser parser = context.getResources().getXml(R.xml.locale_config)) {
            while (parser.next() != XmlPullParser.END_DOCUMENT) {
                if (parser.getEventType() == XmlPullParser.START_TAG && "locale".equals(parser.getName())) {
                    tags.add(parser.getAttributeValue(ANDROID_NS, "name"));
                }
            }
        } catch (IOException | XmlPullParserException e) {
            throw new IllegalStateException("Invalid language catalogue", e);
        }
        return tags.toArray(new String[0]);
    }

    public static String[] languageNames(Context context) {
        String[] tags = languageTags(context);
        String[] names = new String[tags.length];
        for (int i = 0; i < tags.length; i++) {
            Locale locale = Locale.forLanguageTag(tags[i]);
            String name = locale.getDisplayLanguage(locale);
            names[i] = name.substring(0, 1).toUpperCase(locale) + name.substring(1);
        }
        return names;
    }

    private static LocaleListCompat requestedLocales(Context context) {
        if (Build.VERSION.SDK_INT < 33) {
            LocaleListCompat live = AppCompatDelegate.getApplicationLocales();
            if (!live.isEmpty()) return live;
            // We own pre-33 persistence so background work can migrate before any Activity starts.
            return LocaleListCompat.forLanguageTags(context.getSharedPreferences("FocusLockPrefs", Context.MODE_PRIVATE)
                    .getString(PENDING_LEGACY, ""));
        }
        migrateLegacySelection(context);
        return LocaleManagerCompat.getApplicationLocales(context);
    }

    /** One-shot handoff before either an Activity or a background receiver renders on Android 13. */
    @androidx.annotation.RequiresApi(33)
    private static synchronized void migrateLegacySelection(Context context) {
        android.content.SharedPreferences prefs = context.getSharedPreferences("FocusLockPrefs", Context.MODE_PRIVATE);
        String pending = prefs.getString(PENDING_LEGACY, "");
        if (pending.isEmpty()) return;
        LocaleManager manager = context.getSystemService(LocaleManager.class);
        if (manager.getApplicationLocales().isEmpty()) {
            manager.setApplicationLocales(LocaleList.forLanguageTags(pending));
        }
        // Consume the handoff once. A subsequent System default reset must stay a reset.
        prefs.edit().remove(PENDING_LEGACY).putBoolean(INITIALIZED, true).apply();
    }

    public static String selectedTag(Context context) {
        LocaleListCompat locales = requestedLocales(context);
        if (!locales.isEmpty()) return locales.get(0).toLanguageTag();
        String[] supported = languageTags(context);
        if (context.getSharedPreferences("FocusLockPrefs", Context.MODE_PRIVATE).getBoolean(INITIALIZED, false)) {
            // Android Settings can reset an app to the phone language. Respect that explicit reset.
            LocaleListCompat phone = LocaleManagerCompat.getSystemLocales(context);
            for (int i = 0; i < phone.size(); i++) {
                for (String tag : supported) {
                    if (Locale.forLanguageTag(tag).getLanguage().equals(phone.get(i).getLanguage())) return tag;
                }
            }
        }
        return supported[0];
    }

    /** Called before Activity.onCreate: the first launch is English, regardless of phone language. */
    public static void initializeDefault(Context context) {
        LocaleListCompat requested = requestedLocales(context);
        boolean initialized = context.getSharedPreferences("FocusLockPrefs", Context.MODE_PRIVATE)
                .getBoolean(INITIALIZED, false);
        if (!requested.isEmpty() && !initialized) {
            context.getSharedPreferences("FocusLockPrefs", Context.MODE_PRIVATE).edit()
                    .putBoolean(INITIALIZED, true).apply();
        }
        // Seed once. After initialization an empty framework locale represents System default.
        if (requested.isEmpty() && !initialized) select(context, languageTags(context)[0]);
        else if (Build.VERSION.SDK_INT < 33 && !requested.isEmpty()) {
            context.getSharedPreferences("FocusLockPrefs", Context.MODE_PRIVATE).edit()
                    .putString(PENDING_LEGACY, requested.toLanguageTags()).apply();
            AppCompatDelegate.setApplicationLocales(requested);
        }
    }

    public static void select(Context context, String tag) {
        boolean supported = false;
        for (String candidate : languageTags(context)) if (candidate.equals(tag)) supported = true;
        if (!supported) throw new IllegalArgumentException("Unsupported app language: " + tag);
        context.getSharedPreferences("FocusLockPrefs", Context.MODE_PRIVATE).edit()
                .putBoolean(INITIALIZED, true).apply();
        if (Build.VERSION.SDK_INT >= 33) {
            context.getSystemService(LocaleManager.class).setApplicationLocales(LocaleList.forLanguageTags(tag));
            context.getSharedPreferences("FocusLockPrefs", Context.MODE_PRIVATE).edit().remove(PENDING_LEGACY).apply();
        } else {
            context.getSharedPreferences("FocusLockPrefs", Context.MODE_PRIVATE).edit().putString(PENDING_LEGACY, tag).apply();
            AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(tag));
        }
    }

    /** Services/receivers do not inherit AppCompatActivity's localized context on Android 9–12. */
    public static Context localizedContext(Context context) {
        Configuration config = new Configuration(context.getResources().getConfiguration());
        Locale locale = Locale.forLanguageTag(selectedTag(context));
        config.setLocale(locale);
        config.setLayoutDirection(locale);
        return context.createConfigurationContext(config);
    }
}
