package at.platemate.ui.legal;

import at.platemate.ui.login.LoginView;
import at.platemate.ui.preferences.PreferenceControls;
import at.platemate.ui.preferences.UiPreferencesService;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouterLink;

@Route("privacy")
@PageTitle("Privacy | PlateMate")
public class PrivacyView extends VerticalLayout {

    public PrivacyView(UiPreferencesService preferences) {
        preferences.apply(UI.getCurrent());
        setSizeFull();
        setAlignItems(Alignment.CENTER);
        addClassName("pm-public-page");

        VerticalLayout card = new VerticalLayout();
        card.addClassName("pm-legal-card");
        card.add(
                new H1(getTranslation("legal.privacy.heading")),
                new Paragraph(getTranslation("legal.privacy.body")),
                new Paragraph(getTranslation("legal.placeholder")),
                new PreferenceControls(this, preferences),
                new RouterLink(getTranslation("legal.backToLogin"), LoginView.class));

        add(card);
    }
}
