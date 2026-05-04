package at.platemate.ui.preferences;

import java.util.Locale;

import at.platemate.i18n.PlateMateI18NProvider;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;

public class PreferenceControls extends HorizontalLayout {

    public PreferenceControls(Component translationSource, UiPreferencesService preferences) {
        setAlignItems(Alignment.CENTER);
        setSpacing(true);

        Button language = new Button(languageToggleFlag(preferences), event -> {
            preferences.setLocale(nextLocale(preferences));
            preferences.apply(UI.getCurrent());
            UI.getCurrent().getPage().reload();
        });
        language.addClassName("pm-language-toggle");
        language.getElement().setAttribute("aria-label", languageAriaLabel(translationSource, preferences));
        language.getElement().setAttribute("title", languageAriaLabel(translationSource, preferences));

        Button theme = new Button(themeIcon(preferences));
        theme.addClickListener(event -> {
            preferences.toggleTheme();
            preferences.apply(UI.getCurrent());
            theme.setIcon(themeIcon(preferences));
            theme.getElement().setAttribute("aria-label", themeAriaLabel(translationSource, preferences));
            theme.getElement().setAttribute("title", themeAriaLabel(translationSource, preferences));
        });
        theme.addClassName("pm-theme-toggle");
        theme.getElement().setAttribute("aria-label", themeAriaLabel(translationSource, preferences));
        theme.getElement().setAttribute("title", themeAriaLabel(translationSource, preferences));

        add(language, theme);
        addClassName("pm-preference-controls");
    }

    private Span themeIcon(UiPreferencesService preferences) {
        Span icon = new Span(preferences.getThemeMode() == ThemeMode.DARK ? "\u2600\uFE0F" : "\uD83C\uDF19");
        icon.addClassName("pm-theme-emoji");
        return icon;
    }

    private Image languageToggleFlag(UiPreferencesService preferences) {
        boolean isGerman = preferences.getLocale().getLanguage().equals("de");
        Image flag = new Image(isGerman ? "icons/flag-gb.svg" : "icons/flag-de.svg",
                isGerman ? "English" : "Deutsch");
        flag.addClassName("pm-language-flag");
        return flag;
    }

    private Locale nextLocale(UiPreferencesService preferences) {
        return preferences.getLocale().getLanguage().equals("de")
                ? PlateMateI18NProvider.ENGLISH
                : PlateMateI18NProvider.GERMAN;
    }

    private String languageAriaLabel(Component translationSource, UiPreferencesService preferences) {
        return preferences.getLocale().getLanguage().equals("de")
                ? translationSource.getTranslation("nav.language.switchToEnglish")
                : translationSource.getTranslation("nav.language.switchToGerman");
    }

    private String themeAriaLabel(Component translationSource, UiPreferencesService preferences) {
        return preferences.getThemeMode() == ThemeMode.DARK
                ? translationSource.getTranslation("nav.theme.switchToLight")
                : translationSource.getTranslation("nav.theme.switchToDark");
    }
}
