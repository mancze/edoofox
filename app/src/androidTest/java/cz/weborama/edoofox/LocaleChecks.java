package cz.weborama.edoofox;

import android.content.Context;
import android.content.res.Configuration;
import android.os.LocaleList;

/** Resource tests only; no system settings, account storage, or network changes. */
final class LocaleChecks {
    static void run(Context base) {
        for (String tag : new String[]{"cs-CZ", "sk-SK", "en-US", "de-DE", "ar-EG", "de-DE,cs-CZ", "en-US,cs-CZ", "de-DE,sk-SK", "sk-SK,en-US", "de-DE,en-GB,cs-CZ"}) {
            Configuration config = new Configuration(base.getResources().getConfiguration());
            config.setLocales(LocaleList.forLanguageTags(tag));
            Context localized = base.createConfigurationContext(config);
            boolean czech = tag.startsWith("cs") || tag.startsWith("sk") || tag.equals("de-DE,cs-CZ") || tag.equals("de-DE,sk-SK");
            check(localized.getString(R.string.choose_school), czech ? "Vyberte svou školu" : "Choose your school");
            check(localized.getString(R.string.dashboard), czech ? "Nástěnka Edookitu" : "Edookit dashboard");
            check(localized.getString(R.string.cancel), czech ? "Zrušit" : "Cancel");
            Configuration referenceConfig = new Configuration(config);
            referenceConfig.setLocales(LocaleList.forLanguageTags(czech ? "cs-CZ" : "en-US"));
            Context reference = base.createConfigurationContext(referenceConfig);
            try {
                for (java.lang.reflect.Field field : R.string.class.getFields()) {
                    int id = field.getInt(null);
                    check(localized.getString(id), reference.getString(id));
                }
            } catch (IllegalAccessException e) { throw new AssertionError(e); }
        }
        if (android.os.Build.VERSION.SDK_INT >= 33) {
            check(new android.app.LocaleConfig(base).getSupportedLocales().toLanguageTags(), "cs,en");
        }
    }
    private static void check(String actual, String expected) {
        if (!expected.equals(actual)) throw new AssertionError("Expected " + expected + ", got " + actual);
    }
}
