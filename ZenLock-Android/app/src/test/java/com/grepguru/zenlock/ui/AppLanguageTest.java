package com.grepguru.zenlock.ui;

import static org.junit.Assert.*;
import android.content.Context;
import android.content.res.Configuration;
import android.os.LocaleList;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.os.LocaleListCompat;
import com.grepguru.zenlock.R;
import java.util.Locale;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = {28, 33})
public class AppLanguageTest {
    private final Context context = RuntimeEnvironment.getApplication();
    @After public void reset() {
        if (android.os.Build.VERSION.SDK_INT >= 33) {
            context.getSystemService(android.app.LocaleManager.class).setApplicationLocales(LocaleList.getEmptyLocaleList());
        } else {
            AppCompatDelegate.setApplicationLocales(LocaleListCompat.getEmptyLocaleList());
        }
        context.getSharedPreferences("FocusLockPrefs", 0).edit().clear().commit();
    }
    @Test public void catalogueUsesNativeNamesAndPortugueseHasNoRegionLabel() {
        String[] tags = AppLanguage.languageTags(context);
        String[] names = AppLanguage.languageNames(context);
        assertArrayEquals(new String[]{"en", "pt-BR", "ar", "fr", "de", "es"}, tags);
        assertArrayEquals(new String[]{"English", "Português", "العربية", "Français", "Deutsch", "Español"}, names);
    }
    @Test public void languageControlStaysEnglishEvenInArabic() {
        Configuration config = new Configuration(context.getResources().getConfiguration());
        config.setLocales(new LocaleList(Locale.forLanguageTag("ar")));
        Context arabic = context.createConfigurationContext(config);
        assertEquals("Change language", arabic.getString(R.string.change_language));
        assertEquals("الإعدادات", arabic.getString(R.string.layout_settings));
    }
    @Test public void explicitChoiceLocalizesBackgroundContextAndPreservesFocusState() {
        context.getSharedPreferences("FocusLockPrefs", 0).edit()
                .putLong("lockEndTime", 987654321L).putBoolean("isLocked", true).commit();
        AppLanguage.select(context, "pt-BR");
        assertEquals("pt-BR", AppLanguage.localizedContext(context).getResources()
                .getConfiguration().getLocales().get(0).toLanguageTag());
        assertEquals(987654321L, context.getSharedPreferences("FocusLockPrefs", 0).getLong("lockEndTime", 0));
        assertTrue(context.getSharedPreferences("FocusLockPrefs", 0).getBoolean("isLocked", false));
    }
    @Test public void noExplicitChoiceStartsInEnglishEvenOnArabicPhone() {
        reset();
        Configuration config = new Configuration(context.getResources().getConfiguration());
        config.setLocale(Locale.forLanguageTag("ar"));
        Context phone = context.createConfigurationContext(config);
        AppLanguage.initializeDefault(phone);
        assertEquals("en", AppLanguage.selectedTag(phone));
    }
    @Test public void everyTranslationResolvesAndOnlyArabicMirrorsLayout() {
        for (String tag : AppLanguage.languageTags(context)) {
            AppLanguage.select(context, tag);
            Context localized = AppLanguage.localizedContext(context);
            assertEquals(tag, localized.getResources().getConfiguration().getLocales().get(0).toLanguageTag());
            assertEquals(tag.equals("ar") ? android.view.View.LAYOUT_DIRECTION_RTL : android.view.View.LAYOUT_DIRECTION_LTR,
                    localized.getResources().getConfiguration().getLayoutDirection());
            assertFalse(localized.getString(R.string.layout_settings).isEmpty());
            assertEquals("Change language", localized.getString(R.string.change_language));
        }
    }
    @Test @Config(sdk = 33)
    public void defaultInitializationDoesNotOverwriteUpgradeMigrationOrSystemReset() {
        reset();
        context.getSharedPreferences("FocusLockPrefs", 0).edit().putBoolean("language_initialized", true).commit();
        AppLanguage.initializeDefault(context);
        assertTrue("An initialized app must respect a system reset",
                context.getSystemService(android.app.LocaleManager.class).getApplicationLocales().isEmpty());
    }
    @Test @Config(sdk = 33)
    public void systemLanguageSelectedBeforeFirstLaunchCanLaterResetToPhone() {
        reset();
        android.app.LocaleManager manager = context.getSystemService(android.app.LocaleManager.class);
        manager.setApplicationLocales(LocaleList.forLanguageTags("fr"));
        AppLanguage.initializeDefault(context);
        assertEquals("fr", AppLanguage.selectedTag(context));
        manager.setApplicationLocales(LocaleList.getEmptyLocaleList());
        AppLanguage.initializeDefault(context);
        assertTrue(manager.getApplicationLocales().isEmpty());
    }
    @Test @Config(sdk = 33)
    public void systemResetUsesSystemLocalesEvenWhenCallerRetainsArabicContext() {
        AppLanguage.select(context, "ar");
        Context retained = AppLanguage.localizedContext(context);
        context.getSystemService(android.app.LocaleManager.class).setApplicationLocales(LocaleList.getEmptyLocaleList());
        String expected = androidx.core.app.LocaleManagerCompat.getSystemLocales(context).get(0).getLanguage();
        assertEquals(expected, Locale.forLanguageTag(AppLanguage.selectedTag(retained)).getLanguage());
    }
    @Test @Config(sdk = 33)
    public void pendingOlderAndroidChoiceMigratesBeforeBackgroundResourcesAreRead() {
        reset();
        context.getSharedPreferences("FocusLockPrefs", 0).edit().putBoolean("language_initialized", true)
                .putString("pending_legacy_language", "pt-BR").commit();
        assertEquals("pt-BR", AppLanguage.localizedContext(context).getResources().getConfiguration()
                .getLocales().get(0).toLanguageTag());
        android.app.LocaleManager manager = context.getSystemService(android.app.LocaleManager.class);
        assertEquals("pt-BR", manager.getApplicationLocales().get(0).toLanguageTag());
        manager.setApplicationLocales(LocaleList.getEmptyLocaleList());
        AppLanguage.initializeDefault(context);
        assertTrue("A later system reset must not resurrect the old selection", manager.getApplicationLocales().isEmpty());
    }
    @Test @Config(sdk = 33)
    public void explicitFrameworkChoiceWinsOverPendingOlderChoice() {
        reset();
        context.getSharedPreferences("FocusLockPrefs", 0).edit().putString("pending_legacy_language", "pt-BR").commit();
        context.getSystemService(android.app.LocaleManager.class).setApplicationLocales(LocaleList.forLanguageTags("fr"));
        assertEquals("fr", AppLanguage.selectedTag(context));
        assertFalse(context.getSharedPreferences("FocusLockPrefs", 0).contains("pending_legacy_language"));
    }
    @Test @Config(sdk = 28)
    public void olderAndroidRestoresPersistedChoiceBeforeAnyActivityExists() {
        AppLanguage.select(context, "pt-BR");
        AppCompatDelegate.setApplicationLocales(LocaleListCompat.getEmptyLocaleList());
        assertEquals("pt-BR", AppLanguage.localizedContext(context).getResources().getConfiguration()
                .getLocales().get(0).toLanguageTag());
    }
    @Test public void localePersistenceHasOneOwnerOnOlderAndroid() throws Exception {
        android.content.ComponentName component = new android.content.ComponentName(context,
                "androidx.appcompat.app.AppLocalesMetadataHolderService");
        try {
            android.content.pm.ServiceInfo info = context.getPackageManager().getServiceInfo(component,
                    android.content.pm.PackageManager.GET_META_DATA | android.content.pm.PackageManager.MATCH_DISABLED_COMPONENTS);
            assertTrue("Do not enable a second asynchronous locale migration owner",
                    info.metaData == null || !info.metaData.getBoolean("autoStoreLocales", false));
        } catch (android.content.pm.PackageManager.NameNotFoundException absent) {
            // No metadata service means automatic storage/migration is disabled.
        }
    }
    @Test public void defaultQuotesStayEnglishAndCustomQuotesKeepTheirOriginalLanguage() {
        AppLanguage.select(context, "fr");
        Context french = AppLanguage.localizedContext(context);
        java.util.List<String> defaults = com.grepguru.zenlock.quotes.QuoteStore.all(french);
        assertEquals("Focus on being productive instead of busy.", defaults.get(0));
        com.grepguru.zenlock.quotes.QuoteStore.remove(french, defaults.get(0));
        String custom = "ركّز على ما يهمك";
        assertTrue(com.grepguru.zenlock.quotes.QuoteStore.add(french, custom));
        for (String tag : AppLanguage.languageTags(context)) {
            AppLanguage.select(context, tag);
            Context localized = AppLanguage.localizedContext(context);
            assertArrayEquals(french.getResources().getStringArray(R.array.default_quotes),
                    localized.getResources().getStringArray(R.array.default_quotes));
            java.util.List<String> quotes = com.grepguru.zenlock.quotes.QuoteStore.all(localized);
            assertEquals(custom, quotes.get(0));
            assertFalse(quotes.contains(defaults.get(0)));
            assertTrue(quotes.contains(defaults.get(1)));
        }
    }
    @Test public void unknownLanguageCannotChangeSelection() {
        AppLanguage.select(context, "de");
        try { AppLanguage.select(context, "xx"); fail("Unknown language accepted"); }
        catch (IllegalArgumentException expected) { }
        assertEquals("de", AppLanguage.selectedTag(context));
    }
}
