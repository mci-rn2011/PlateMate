package at.platemate.ui.preferences;

import java.util.Locale;

import at.platemate.i18n.PlateMateI18NProvider;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.spring.annotation.VaadinSessionScope;
import org.springframework.stereotype.Service;

@Service
@VaadinSessionScope
public class UiPreferencesService {

    private Locale locale = PlateMateI18NProvider.ENGLISH;
    private ThemeMode themeMode = ThemeMode.LIGHT;

    public Locale getLocale() {
        return locale;
    }

    public void setLocale(Locale locale) {
        this.locale = PlateMateI18NProvider.GERMAN.getLanguage().equals(locale.getLanguage())
                ? PlateMateI18NProvider.GERMAN
                : PlateMateI18NProvider.ENGLISH;
    }

    public ThemeMode getThemeMode() {
        return themeMode;
    }

    public void toggleTheme() {
        this.themeMode = themeMode == ThemeMode.DARK ? ThemeMode.LIGHT : ThemeMode.DARK;
    }

    public void apply(UI ui) {
        ui.setLocale(locale);
        if (themeMode == ThemeMode.DARK) {
            ui.getElement().setAttribute("theme", "dark");
            ui.getPage().executeJs("document.documentElement.setAttribute('theme', 'dark')");
        } else {
            ui.getElement().removeAttribute("theme");
            ui.getPage().executeJs("document.documentElement.removeAttribute('theme')");
        }
    }
}
