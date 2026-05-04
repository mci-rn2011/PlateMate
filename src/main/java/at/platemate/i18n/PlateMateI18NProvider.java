package at.platemate.i18n;

import java.text.MessageFormat;
import java.util.List;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

import com.vaadin.flow.i18n.I18NProvider;
import org.springframework.stereotype.Component;

@Component
public class PlateMateI18NProvider implements I18NProvider {

    public static final Locale ENGLISH = Locale.ENGLISH;
    public static final Locale GERMAN = Locale.GERMAN;

    private static final String BUNDLE_PREFIX = "i18n.messages";
    private static final List<Locale> PROVIDED_LOCALES = List.of(ENGLISH, GERMAN);

    @Override
    public List<Locale> getProvidedLocales() {
        return PROVIDED_LOCALES;
    }

    @Override
    public String getTranslation(String key, Locale locale, Object... params) {
        Locale resolvedLocale = PROVIDED_LOCALES.contains(locale) ? locale : ENGLISH;
        try {
            ResourceBundle bundle = ResourceBundle.getBundle(BUNDLE_PREFIX, resolvedLocale);
            String value = bundle.getString(key);
            return params.length == 0 ? value : MessageFormat.format(value, params);
        } catch (MissingResourceException ex) {
            return "!" + key + "!";
        }
    }
}
