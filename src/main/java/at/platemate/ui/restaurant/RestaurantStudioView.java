package at.platemate.ui.restaurant;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import at.platemate.auth.MockSessionService;
import at.platemate.menu.MenuCategory;
import at.platemate.menu.MenuItem;
import at.platemate.menu.MenuService;
import at.platemate.restaurant.CuisinePreset;
import at.platemate.restaurant.Restaurant;
import at.platemate.restaurant.RestaurantOpeningHours;
import at.platemate.restaurant.RestaurantService;
import at.platemate.ui.layout.MainLayout;
import at.platemate.upload.UploadStorageService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.BigDecimalField;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.timepicker.TimePicker;
import com.vaadin.flow.component.upload.Upload;
import com.vaadin.flow.component.upload.receivers.MemoryBuffer;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

@Route(value = "restaurant/studio", layout = MainLayout.class)
@PageTitle("Restaurant Studio | PlateMate")
public class RestaurantStudioView extends VerticalLayout {

    private static final String PLACEHOLDER_BANNER = "placeholders/restaurant-banner.svg";
    private static final String PLACEHOLDER_LOGO = "placeholders/restaurant-logo.svg";
    private static final String PLACEHOLDER_ITEM = "placeholders/menu-item.svg";

    private final MenuService menuService;
    private final RestaurantService restaurantService;
    private final UploadStorageService uploadStorageService;
    private final Restaurant restaurant;
    private final VerticalLayout itemBoard = new VerticalLayout();
    private final Span menuCount = new Span();
    private final Map<DayOfWeek, OpeningHoursRow> hoursRows = new EnumMap<>(DayOfWeek.class);

    public RestaurantStudioView(
            MenuService menuService,
            RestaurantService restaurantService,
            UploadStorageService uploadStorageService,
            MockSessionService sessionService) {
        this.menuService = menuService;
        this.restaurantService = restaurantService;
        this.uploadStorageService = uploadStorageService;
        this.restaurant = sessionService.getCurrentUser()
                .flatMap(user -> restaurantService.findForOwner(user).stream().findFirst())
                .orElse(null);

        setSizeFull();
        setPadding(false);
        setSpacing(false);
        addClassName("pm-restaurant-page");

        if (restaurant == null) {
            add(createEmptyState(getTranslation("restaurant.studio.empty.title"),
                    getTranslation("restaurant.studio.empty.detail")));
            return;
        }

        add(createStudioHeader());
        add(createStudioShell());
        refreshItems();
    }

    private Div createStudioHeader() {
        Div header = new Div();
        header.addClassNames("pm-restaurant-hero", "pm-studio-hero");

        Div copy = new Div();
        copy.addClassName("pm-restaurant-hero-copy");
        Span eyebrow = new Span(getTranslation("restaurant.studio.eyebrow"));
        eyebrow.addClassName("pm-eyebrow");
        H1 title = new H1(restaurant.getName(getLocale()));
        Paragraph intro = new Paragraph(getTranslation("restaurant.studio.intro"));
        copy.add(eyebrow, title, intro, createStudioStats());

        Div previewWrap = new Div();
        previewWrap.addClassName("pm-storefront-preview-wrap");
        Span previewLabel = new Span(getTranslation("restaurant.studio.preview"));
        previewLabel.addClassName("pm-preview-label");

        Div preview = new Div();
        preview.addClassName("pm-storefront-preview");
        Image banner = new Image(imageOrPlaceholder(restaurant.getBannerImageUrl(), PLACEHOLDER_BANNER),
                getTranslation("restaurant.studio.image.bannerAlt", restaurant.getName(getLocale())));
        banner.addClassName("pm-storefront-banner");
        Image logo = new Image(imageOrPlaceholder(restaurant.getLogoImageUrl(), PLACEHOLDER_LOGO),
                getTranslation("restaurant.studio.image.logoAlt", restaurant.getName(getLocale())));
        logo.addClassName("pm-storefront-logo");
        Div details = new Div();
        details.addClassName("pm-storefront-details");
        details.add(new H2(restaurant.getName(getLocale())), new Paragraph(restaurant.getDescription(getLocale())), createStatusChip());
        preview.add(banner, logo, details);
        previewWrap.add(previewLabel, preview);

        header.add(copy, previewWrap);
        return header;
    }

    private Div createStudioStats() {
        Div stats = new Div();
        stats.addClassName("pm-studio-stats");
        stats.add(createStudioStat(getTranslation("restaurant.studio.stats.sections"),
                String.valueOf(menuService.findCategories(restaurant).size())));
        stats.add(createStudioStat(getTranslation("restaurant.studio.stats.items"),
                String.valueOf(menuService.findItems(restaurant).size())));
        stats.add(createStudioStat(getTranslation("restaurant.studio.stats.status"),
                statusLabel(restaurant.getStatus())));
        return stats;
    }

    private Div createStudioStat(String label, String value) {
        Div stat = new Div();
        stat.addClassName("pm-studio-stat");
        Span statValue = new Span(value);
        statValue.addClassName("pm-studio-stat-value");
        Span statLabel = new Span(label);
        statLabel.addClassName("pm-studio-stat-label");
        stat.add(statValue, statLabel);
        return stat;
    }

    private HorizontalLayout createStudioShell() {
        HorizontalLayout shell = new HorizontalLayout();
        shell.addClassName("pm-studio-shell");
        shell.setWidthFull();
        shell.setAlignItems(Alignment.START);

        VerticalLayout leftRail = new VerticalLayout();
        leftRail.addClassName("pm-studio-rail");
        leftRail.setPadding(false);
        leftRail.setSpacing(false);
        leftRail.add(createProfileCard(), createImagesCard(), createOpeningHoursCard());

        VerticalLayout main = new VerticalLayout();
        main.addClassName("pm-studio-main");
        main.setPadding(false);
        main.setSpacing(false);
        main.add(createMenuWorkshop(), itemBoard);

        shell.add(leftRail, main);
        shell.expand(main);
        return shell;
    }

    private Div createMenuWorkshop() {
        Div workshop = new Div();
        workshop.addClassName("pm-menu-workshop");
        workshop.add(createCategoryCard(), createMenuComposer());
        return workshop;
    }

    private Div createProfileCard() {
        Div card = createCard(getTranslation("restaurant.studio.profile.title"));

        TextField name = new TextField(getTranslation("restaurant.studio.profile.name"));
        name.setValue(valueOrEmpty(restaurant.getName()));
        TextArea description = new TextArea(getTranslation("field.description"));
        description.setValue(valueOrEmpty(restaurant.getDescription()));
        ComboBox<CuisinePreset> category = cuisineSelect(getTranslation("restaurant.studio.profile.category"));
        category.setValue(CuisinePreset.fromCategory(restaurant.getCategory()));
        TextField address = new TextField(getTranslation("grid.address"));
        address.setValue(valueOrEmpty(restaurant.getAddress()));
        TextField nameDe = new TextField(getTranslation("restaurant.studio.profile.name.de"));
        nameDe.setValue(valueOrEmpty(restaurant.getName(Locale.GERMAN)));
        TextArea descriptionDe = new TextArea(getTranslation("restaurant.studio.profile.description.de"));
        descriptionDe.setValue(valueOrEmpty(restaurant.getDescription(Locale.GERMAN)));
        ComboBox<CuisinePreset> categoryDe = cuisineSelect(getTranslation("restaurant.studio.profile.category.de"));
        categoryDe.setValue(CuisinePreset.fromCategory(restaurant.getCategory(Locale.GERMAN)));
        name.setWidthFull();
        description.setWidthFull();
        address.setWidthFull();
        nameDe.setWidthFull();
        descriptionDe.setWidthFull();
        category.addValueChangeListener(event -> {
            if (event.isFromClient()) {
                categoryDe.setValue(event.getValue());
            }
        });
        categoryDe.addValueChangeListener(event -> {
            if (event.isFromClient()) {
                category.setValue(event.getValue());
            }
        });

        Button save = new Button(getTranslation("restaurant.studio.profile.save"), event -> runAndReload(() -> {
            CuisinePreset selectedCuisine = category.getValue() == null ? CuisinePreset.RAMEN : category.getValue();
            restaurantService.updateProfile(
                    restaurant.getId(),
                    name.getValue(),
                    description.getValue(),
                    selectedCuisine.englishName(),
                    address.getValue(),
                    restaurant.getLogoImageUrl(),
                    restaurant.getBannerImageUrl());
            restaurantService.updateProfileTranslation(restaurant.getId(), Locale.GERMAN,
                    nameDe.getValue(), descriptionDe.getValue(), selectedCuisine.germanName());
        }));
        save.addClassName("pm-soft-action");

        Div germanFields = new Div();
        germanFields.addClassName("pm-profile-language");
        Image germanFlag = new Image("icons/flag-de.svg", "Deutsch");
        germanFlag.addClassName("pm-profile-language-header");
        germanFields.add(germanFlag, nameDe, descriptionDe, categoryDe);

        Div englishFields = new Div();
        englishFields.addClassName("pm-profile-language");
        Image englishFlag = new Image("icons/flag-gb.svg", "English");
        englishFlag.addClassName("pm-profile-language-header");
        englishFields.add(englishFlag, name, description, category);

        Div profileGrid = new Div(germanFields, englishFields);
        profileGrid.addClassName("pm-profile-grid");

        Div actions = new Div(address, save);
        actions.addClassName("pm-profile-actions");
        card.add(profileGrid, actions);
        return card;
    }

    private Div createImagesCard() {
        Div card = createCard(getTranslation("restaurant.studio.images.title"));
        card.add(new Paragraph(getTranslation("restaurant.studio.images.detail")));
        card.add(createImageUpload(getTranslation("restaurant.profile.logo"), restaurant.getLogoImageUrl(), path -> restaurantService.updateProfile(
                restaurant.getId(),
                restaurant.getName(),
                restaurant.getDescription(),
                restaurant.getCategory(),
                restaurant.getAddress(),
                path,
                restaurant.getBannerImageUrl())));
        card.add(createImageUpload(getTranslation("restaurant.profile.banner"), restaurant.getBannerImageUrl(), path -> restaurantService.updateProfile(
                restaurant.getId(),
                restaurant.getName(),
                restaurant.getDescription(),
                restaurant.getCategory(),
                restaurant.getAddress(),
                restaurant.getLogoImageUrl(),
                path)));
        return card;
    }

    private Div createOpeningHoursCard() {
        Div card = createCard(getTranslation("restaurant.profile.openingHours"));
        card.add(new Paragraph(getTranslation("restaurant.studio.hours.detail")));

        Map<DayOfWeek, RestaurantOpeningHours> existing = new EnumMap<>(DayOfWeek.class);
        restaurantService.findOpeningHours(restaurant).forEach(row -> existing.put(row.getDayOfWeek(), row));

        for (DayOfWeek day : DayOfWeek.values()) {
            RestaurantOpeningHours value = existing.get(day);
            OpeningHoursRow row = new OpeningHoursRow(day, value,
                    getTranslation("restaurant.studio.hours.opens"),
                    getTranslation("restaurant.studio.hours.closes"),
                    getTranslation("restaurant.studio.hours.closed"),
                    getTranslation("dayOfWeek." + day.name()));
            hoursRows.put(day, row);
            card.add(row.layout());
        }

        Button save = new Button(getTranslation("restaurant.studio.hours.save"), event -> runAndReload(() -> {
            List<RestaurantOpeningHours> rows = new ArrayList<>();
            for (OpeningHoursRow row : hoursRows.values()) {
                rows.add(row.toEntity(restaurant));
            }
            restaurantService.replaceOpeningHours(restaurant.getId(), rows);
        }));
        save.addClassNames("pm-soft-action", "pm-hours-save");
        card.add(save);
        return card;
    }

    private Div createCategoryCard() {
        Div card = createCard(getTranslation("restaurant.studio.structure.title"));
        refreshCategoryList(card);
        return card;
    }

    private void refreshCategoryList(Div card) {
        card.removeAll();
        card.add(new H3(getTranslation("restaurant.studio.structure.title")));

        List<MenuCategory> categories = menuService.findCategories(restaurant);
        for (MenuCategory category : categories) {
            Div row = new Div();
            row.addClassName("pm-category-row");
            Span sort = new Span(String.valueOf(category.getSortOrder()));
            sort.addClassName("pm-sort-badge");
            Span label = new Span(category.getName(getLocale()));
            Span count = new Span(getTranslation("restaurant.studio.structure.itemCount", menuService.countItems(category)));
            count.addClassName("pm-category-count");
            Div labelGroup = new Div(label, count);
            labelGroup.addClassName("pm-category-label");
            Div actions = new Div();
            actions.addClassName("pm-inline-actions");
            Button edit = iconButton(VaadinIcon.COG, getTranslation("action.edit"), "pm-soft-action",
                    event -> openCategoryDialog(category));
            Button delete = iconButton(VaadinIcon.TRASH, getTranslation("action.delete"), "pm-danger-action",
                    event -> openDeleteCategoryDialog(category));
            actions.add(edit, delete);
            row.add(sort, labelGroup, actions);
            card.add(row);
        }

        Button addCategory = new Button(getTranslation("restaurant.studio.structure.add"), event -> openCategoryDialog(null));
        addCategory.addClassNames("pm-primary-action", "pm-category-add-action");
        card.add(addCategory);
    }

    private Div createMenuComposer() {
        Div card = createCard(getTranslation("restaurant.studio.item.create"));

        ComboBox<MenuCategory> category = categorySelect();
        TextField name = new TextField(getTranslation("restaurant.studio.item.name"));
        TextArea description = new TextArea(getTranslation("field.description"));
        BigDecimalField price = new BigDecimalField(getTranslation("field.price"));
        IntegerField sortOrder = new IntegerField(getTranslation("restaurant.studio.item.sort"));
        sortOrder.setValue(0);
        price.setPrefixComponent(new Span("€"));
        Button add = new Button(getTranslation("action.addItem"), event -> {
            BigDecimal itemPrice = price.getValue();
            if (name.isEmpty() || itemPrice == null || category.getValue() == null) {
                Notification.show(getTranslation("restaurant.studio.item.required"));
                return;
            }
            menuService.addItem(restaurant, category.getValue(), name.getValue(), description.getValue(), itemPrice,
                    true, null, sortOrder.getValue() == null ? 0 : sortOrder.getValue());
            name.clear();
            description.clear();
            price.clear();
            refreshItems();
        });
        add.addClassName("pm-primary-action");

        Div actions = new Div(add);
        actions.addClassName("pm-composer-actions");
        description.addClassName("pm-composer-description");
        Div row = new Div(category, name, price, sortOrder, description, actions);
        row.addClassName("pm-composer-row");

        VerticalLayout form = new VerticalLayout(row);
        form.setPadding(false);
        form.setSpacing(true);
        card.add(form);
        return card;
    }

    private void refreshItems() {
        List<MenuItem> items = menuService.findItems(restaurant);
        menuCount.setText(getTranslation("restaurant.studio.customerMenu.itemCount", items.size()));
        itemBoard.removeAll();
        itemBoard.setPadding(false);
        itemBoard.setSpacing(false);
        itemBoard.addClassName("pm-menu-board");

        Div heading = new Div();
        heading.addClassName("pm-section-heading");
        heading.add(new H2(getTranslation("restaurant.studio.customerMenu.title")),
                new Paragraph(getTranslation("restaurant.studio.customerMenu.detail")));
        itemBoard.add(heading);

        if (items.isEmpty()) {
            itemBoard.add(createEmptyState(getTranslation("restaurant.studio.customerMenu.empty.title"),
                    getTranslation("restaurant.studio.customerMenu.empty.detail")));
            return;
        }

        Div grid = new Div();
        grid.addClassName("pm-menu-card-grid");
        items.stream()
                .sorted(Comparator.comparingInt((MenuItem item) -> item.getCategory() == null ? Integer.MAX_VALUE : item.getCategory().getSortOrder())
                        .thenComparingInt(MenuItem::getSortOrder)
                        .thenComparing(item -> item.getName()))
                .forEach(item -> grid.add(createMenuItemCard(item)));
        itemBoard.add(grid);
    }

    private Div createMenuItemCard(MenuItem item) {
        Div card = new Div();
        card.addClassName("pm-menu-item-card");
        if (!item.isAvailable()) {
            card.addClassName("is-muted");
        }

        Image thumb = new Image(imageOrPlaceholder(item.getThumbnailImageUrl(), PLACEHOLDER_ITEM), item.getName());
        thumb.addClassName("pm-item-thumb");

        Div copy = new Div();
        copy.addClassName("pm-menu-item-copy");
        Div itemTop = new Div();
        itemTop.addClassName("pm-menu-item-top");
        H3 name = new H3(item.getName());
        itemTop.add(name);
        Paragraph category = new Paragraph(item.getCategory() == null
                ? getTranslation("restaurant.studio.item.uncategorized")
                : item.getCategory().getName());
        Paragraph description = new Paragraph(item.getDescription());
        Span price = new Span(money(item.getPrice()));
        price.addClassName("pm-price");
        copy.add(itemTop, category, description, price);

        Button toggle = new Button(item.isAvailable()
                ? getTranslation("restaurant.studio.item.pause")
                : getTranslation("restaurant.studio.item.resume"), event -> {
            menuService.toggleAvailability(item.getId());
            refreshItems();
        });
        toggle.addClassNames("pm-availability-toggle", item.isAvailable() ? "is-pause" : "is-resume");
        Button edit = iconButton(VaadinIcon.COG, getTranslation("action.edit"), "pm-soft-action",
                event -> openItemDialog(item));
        Button delete = iconButton(VaadinIcon.TRASH, getTranslation("action.delete"), "pm-danger-action",
                event -> openDeleteItemDialog(item));

        Div actions = new Div(edit, delete);
        actions.addClassName("pm-card-actions");
        card.add(toggle, thumb, copy, actions);
        return card;
    }

    private void openCategoryDialog(MenuCategory category) {
        Dialog dialog = new Dialog();
        dialog.addClassName("pm-editor-dialog");
        dialog.setWidth("min(1280px, calc(100vw - 2rem))");
        dialog.setHeaderTitle(category == null
                ? getTranslation("restaurant.studio.structure.add")
                : getTranslation("restaurant.studio.structure.edit"));
        TextField name = new TextField(getTranslation("field.name"));
        TextArea description = new TextArea(getTranslation("field.description"));
        IntegerField sort = new IntegerField(getTranslation("restaurant.studio.item.sortOrder"));
        sort.setValue(category == null ? menuService.findCategories(restaurant).size() + 1 : category.getSortOrder());
        TextField nameDe = new TextField(getTranslation("restaurant.studio.structure.name.de"));
        TextArea descriptionDe = new TextArea(getTranslation("restaurant.studio.structure.description.de"));
        name.setWidthFull();
        description.setWidthFull();
        sort.setWidthFull();
        nameDe.setWidthFull();
        descriptionDe.setWidthFull();
        if (category != null) {
            name.setValue(category.getName());
            description.setValue(valueOrEmpty(category.getDescription()));
            nameDe.setValue(valueOrEmpty(category.getName(Locale.GERMAN)));
            descriptionDe.setValue(valueOrEmpty(category.getDescription(Locale.GERMAN)));
        }
        Button cancel = new Button(getTranslation("action.close"), event -> dialog.close());
        Button save = new Button(getTranslation("action.save"), event -> runAndReload(() -> {
            if (category == null) {
                MenuCategory created = menuService.addCategory(restaurant, name.getValue(), description.getValue(),
                        sort.getValue() == null ? 0 : sort.getValue());
                menuService.updateCategoryTranslation(created.getId(), Locale.GERMAN,
                        nameDe.getValue(), descriptionDe.getValue());
            } else {
                menuService.updateCategory(category.getId(), name.getValue(), description.getValue(),
                        sort.getValue() == null ? 0 : sort.getValue());
                menuService.updateCategoryTranslation(category.getId(), Locale.GERMAN,
                        nameDe.getValue(), descriptionDe.getValue());
            }
            dialog.close();
        }));
        save.addClassName("pm-primary-action");
        Div form = new Div();
        form.addClassName("pm-editor-form");
        form.add(
                languageBlock("icons/flag-de.svg", "Deutsch", nameDe, descriptionDe),
                languageBlock("icons/flag-gb.svg", "English", name, description),
                metaBlock(sort));
        dialog.add(form);
        dialog.getFooter().add(cancel, save);
        dialog.open();
    }

    private void openItemDialog(MenuItem item) {
        Dialog dialog = new Dialog();
        dialog.addClassName("pm-editor-dialog");
        dialog.setWidth("min(1280px, calc(100vw - 2rem))");
        dialog.setHeaderTitle(getTranslation("restaurant.studio.item.edit", item.getName()));
        ComboBox<MenuCategory> category = categorySelect();
        category.setValue(item.getCategory());
        TextField name = new TextField(getTranslation("field.name"));
        name.setValue(valueOrEmpty(item.getName()));
        TextArea description = new TextArea(getTranslation("field.description"));
        description.setValue(valueOrEmpty(item.getDescription()));
        TextField nameDe = new TextField(getTranslation("restaurant.studio.item.name.de"));
        nameDe.setValue(valueOrEmpty(item.getName(Locale.GERMAN)));
        TextArea descriptionDe = new TextArea(getTranslation("restaurant.studio.item.description.de"));
        descriptionDe.setValue(valueOrEmpty(item.getDescription(Locale.GERMAN)));
        BigDecimalField price = new BigDecimalField(getTranslation("field.price"));
        price.setValue(item.getPrice());
        Checkbox available = new Checkbox(getTranslation("restaurant.studio.item.available"), item.isAvailable());
        IntegerField sort = new IntegerField(getTranslation("restaurant.studio.item.sortOrder"));
        sort.setValue(item.getSortOrder());
        category.setWidthFull();
        name.setWidthFull();
        description.setWidthFull();
        nameDe.setWidthFull();
        descriptionDe.setWidthFull();
        price.setWidthFull();
        sort.setWidthFull();

        Div upload = createImageUpload(getTranslation("restaurant.menu.thumbnail"), item.getThumbnailImageUrl(), path -> menuService.updateThumbnail(item.getId(), path));
        Button close = new Button(getTranslation("action.close"), event -> dialog.close());
        Button save = new Button(getTranslation("action.save"), event -> runAndReload(() -> {
            menuService.updateItem(item.getId(), category.getValue(), name.getValue(), description.getValue(),
                    price.getValue(), available.getValue(), sort.getValue() == null ? 0 : sort.getValue());
            menuService.updateItemTranslation(item.getId(), Locale.GERMAN, nameDe.getValue(), descriptionDe.getValue());
            dialog.close();
        }));
        save.addClassName("pm-primary-action");

        Div form = new Div();
        form.addClassName("pm-editor-form");
        form.add(
                languageBlock("icons/flag-de.svg", "Deutsch", nameDe, descriptionDe),
                languageBlock("icons/flag-gb.svg", "English", name, description),
                metaBlock(category, price, sort, upload, available));
        dialog.add(form);
        dialog.getFooter().add(close, save);
        dialog.open();
    }

    private Div languageBlock(String flagPath, String altText, com.vaadin.flow.component.Component... fields) {
        Div block = new Div();
        block.addClassName("pm-editor-language");
        Image flag = new Image(flagPath, altText);
        flag.addClassName("pm-editor-language-flag");
        block.add(flag);
        block.add(fields);
        return block;
    }

    private Div metaBlock(com.vaadin.flow.component.Component... fields) {
        Div block = new Div();
        block.addClassName("pm-editor-meta");
        block.add(fields);
        return block;
    }

    private ComboBox<MenuCategory> categorySelect() {
        ComboBox<MenuCategory> category = new ComboBox<>(getTranslation("restaurant.menu.category"));
        category.setItems(menuService.findCategories(restaurant));
        category.setItemLabelGenerator(value -> value.getName(getLocale()));
        return category;
    }

    private ComboBox<CuisinePreset> cuisineSelect(String label) {
        ComboBox<CuisinePreset> cuisine = new ComboBox<>(label);
        cuisine.setItems(CuisinePreset.values());
        cuisine.setItemLabelGenerator(value -> value.displayName(getLocale()));
        cuisine.setWidthFull();
        return cuisine;
    }

    private Div createImageUpload(String label, String oldPath, ImagePathConsumer consumer) {
        Div wrapper = new Div();
        wrapper.addClassName("pm-upload-placeholder");
        wrapper.add(new Span(label));
        MemoryBuffer buffer = new MemoryBuffer();
        Upload upload = new Upload(buffer);
        upload.setAcceptedFileTypes("image/png", "image/jpeg", "image/webp", "image/svg+xml");
        upload.setMaxFiles(1);
        upload.addSucceededListener(event -> runAndReload(() -> {
            String path = uploadStorageService.saveRestaurantImage(
                    restaurant.getId(),
                    event.getFileName(),
                    event.getMIMEType(),
                    event.getContentLength(),
                    buffer.getInputStream(),
                    oldPath);
            consumer.accept(path);
        }));
        wrapper.add(upload);
        return wrapper;
    }

    private Span createStatusChip() {
        Span status = new Span(statusLabel(restaurant.getStatus()));
        status.addClassNames("pm-status-pill", restaurant.isOpen() ? "is-open" : "is-closed");
        return status;
    }

    private void openDeleteItemDialog(MenuItem item) {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle(getTranslation("restaurant.studio.item.delete.title", item.getName()));
        dialog.add(new Paragraph(getTranslation("restaurant.studio.item.delete.detail")));
        Button close = new Button(getTranslation("action.close"), event -> dialog.close());
        Button delete = new Button(getTranslation("action.delete"), event -> runAndReload(() -> {
            menuService.deleteItem(item.getId());
            dialog.close();
        }));
        delete.addClassName("pm-danger-action");
        dialog.getFooter().add(close, delete);
        dialog.open();
    }

    private void openDeleteCategoryDialog(MenuCategory category) {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle(getTranslation("restaurant.studio.structure.delete.title", category.getName()));
        dialog.add(new Paragraph(getTranslation("restaurant.studio.structure.delete.detail")));
        Button close = new Button(getTranslation("action.close"), event -> dialog.close());
        Button delete = new Button(getTranslation("action.delete"), event -> runAndReload(() -> {
            menuService.deleteCategory(category.getId());
            dialog.close();
        }));
        delete.addClassName("pm-danger-action");
        dialog.getFooter().add(close, delete);
        dialog.open();
    }

    private Button iconButton(VaadinIcon vaadinIcon, String ariaLabel, String className,
            com.vaadin.flow.component.ComponentEventListener<com.vaadin.flow.component.ClickEvent<Button>> listener) {
        Icon icon = vaadinIcon.create();
        Button button = new Button(icon, listener);
        button.addClassNames(className, "pm-icon-action");
        button.getElement().setAttribute("aria-label", ariaLabel);
        button.setTooltipText(ariaLabel);
        return button;
    }

    private String statusLabel(at.platemate.restaurant.RestaurantStatus status) {
        return getTranslation("restaurantStatus." + status.name());
    }

    private Div createCard(String title) {
        Div card = new Div();
        card.addClassName("pm-restaurant-card");
        card.add(new H3(title));
        return card;
    }

    private Div createEmptyState(String title, String detail) {
        Div empty = new Div();
        empty.addClassName("pm-empty-state");
        empty.add(new H2(title), new Paragraph(detail));
        return empty;
    }

    private String money(BigDecimal amount) {
        return amount + " €";
    }

    private String imageOrPlaceholder(String imageUrl, String placeholder) {
        return imageUrl == null || imageUrl.isBlank() ? placeholder : imageUrl;
    }

    private String valueOrEmpty(String value) {
        return value == null ? "" : value;
    }

    private void runAndReload(Runnable action) {
        try {
            action.run();
            getUI().ifPresent(ui -> ui.getPage().reload());
        } catch (RuntimeException ex) {
            Notification.show(ex.getMessage());
        }
    }

    @FunctionalInterface
    private interface ImagePathConsumer {
        void accept(String imagePath);
    }

    private static class OpeningHoursRow {
        private final DayOfWeek day;
        private final Checkbox closed;
        private final TimePicker opensAt;
        private final TimePicker closesAt;
        private final HorizontalLayout layout;

        OpeningHoursRow(DayOfWeek day, RestaurantOpeningHours value, String opensLabel, String closesLabel,
                String closedLabel, String dayLabel) {
            this.day = day;
            this.closed = new Checkbox(closedLabel, value != null && value.isClosed());
            this.opensAt = new TimePicker(opensLabel);
            this.closesAt = new TimePicker(closesLabel);
            this.opensAt.setValue(value == null || value.getOpensAt() == null ? LocalTime.of(10, 0) : value.getOpensAt());
            this.closesAt.setValue(value == null || value.getClosesAt() == null ? LocalTime.of(22, 0) : value.getClosesAt());
            setTimeInputsEnabled(!this.closed.getValue());
            this.closed.addValueChangeListener(event -> setTimeInputsEnabled(!event.getValue()));
            Span label = new Span(dayLabel);
            label.addClassName("pm-day-label");
            this.layout = new HorizontalLayout(label, opensAt, closesAt, closed);
            this.layout.addClassName("pm-opening-row");
            this.layout.setAlignItems(Alignment.END);
        }

        HorizontalLayout layout() {
            return layout;
        }

        RestaurantOpeningHours toEntity(Restaurant restaurant) {
            return new RestaurantOpeningHours(restaurant, day, opensAt.getValue(), closesAt.getValue(), closed.getValue());
        }

        private void setTimeInputsEnabled(boolean enabled) {
            opensAt.setEnabled(enabled);
            closesAt.setEnabled(enabled);
        }
    }
}
