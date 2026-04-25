package de.leycm.label4j.locale;

import lombok.NonNull;

import java.util.Locale;

public final class Locales {
    private static final @NonNull String CONSTANT_FILE_TAG = "const";

    public static @NonNull String localeToFilename(final @NonNull Locale locale) {
        if (locale.equals(Locale.ROOT)) return CONSTANT_FILE_TAG;
        return locale.getLanguage().toLowerCase() + "_" + locale.getCountry().toLowerCase();
    }

    public static @NonNull Locale filenameToLocale(final @NonNull String fileTag) {
        if (fileTag.equalsIgnoreCase(CONSTANT_FILE_TAG)) return Locale.ROOT;
        return Locale.forLanguageTag(fileTag.replace("_", "-"));
    }

    private Locales() {
        throw new UnsupportedOperationException("This util class can not be constructed");
    }
}
