package at.platemate.menu;

import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;

import at.platemate.restaurant.Restaurant;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MenuService {

    private final MenuItemRepository menuItemRepository;
    private final MenuCategoryRepository menuCategoryRepository;

    public MenuService(MenuItemRepository menuItemRepository, MenuCategoryRepository menuCategoryRepository) {
        this.menuItemRepository = menuItemRepository;
        this.menuCategoryRepository = menuCategoryRepository;
    }

    public List<MenuItem> findAvailableItems(Restaurant restaurant) {
        return menuItemRepository.findByRestaurantAndAvailableTrueOrderBySortOrderAscNameAsc(restaurant);
    }

    public List<MenuItem> findAvailableItems(Restaurant restaurant, Locale locale) {
        return findAvailableItems(restaurant);
    }

    public List<MenuItem> findItems(Restaurant restaurant) {
        return menuItemRepository.findByRestaurantOrderBySortOrderAscNameAsc(restaurant);
    }

    public List<MenuItem> findItems(Restaurant restaurant, Locale locale) {
        return findItems(restaurant);
    }

    public MenuItem getMenuItem(Long id) {
        return menuItemRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Menu item not found: " + id));
    }

    public MenuItem addItem(Restaurant restaurant, String name, String description, BigDecimal price) {
        return menuItemRepository.save(new MenuItem(restaurant, name, description, price, true));
    }

    public List<MenuCategory> findCategories(Restaurant restaurant) {
        return menuCategoryRepository.findByRestaurantOrderBySortOrderAscNameAsc(restaurant);
    }

    public List<MenuCategory> findCategories(Restaurant restaurant, Locale locale) {
        return findCategories(restaurant);
    }

    public long countItems(MenuCategory category) {
        return menuItemRepository.countByCategory(category);
    }

    public MenuCategory getCategory(Long id) {
        return menuCategoryRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Menu category not found: " + id));
    }

    public MenuCategory addCategory(Restaurant restaurant, String name, int sortOrder) {
        return menuCategoryRepository.save(new MenuCategory(restaurant, name, sortOrder));
    }

    public MenuCategory addCategory(Restaurant restaurant, String name, String description, int sortOrder) {
        MenuCategory category = new MenuCategory(restaurant, name, sortOrder);
        category.setDescription(description);
        return menuCategoryRepository.save(category);
    }

    @Transactional
    public MenuCategory updateCategory(Long categoryId, String name, int sortOrder) {
        MenuCategory category = getCategory(categoryId);
        category.setName(name);
        category.setSortOrder(sortOrder);
        return category;
    }

    @Transactional
    public MenuCategory updateCategory(Long categoryId, String name, String description, int sortOrder) {
        MenuCategory category = updateCategory(categoryId, name, sortOrder);
        category.setDescription(description);
        return category;
    }

    @Transactional
    public MenuCategory updateCategoryTranslation(Long categoryId, Locale locale, String name, String description) {
        MenuCategory category = getCategory(categoryId);
        MenuCategoryTranslation translation = category.translation(locale.getLanguage());
        translation.setName(name);
        translation.setDescription(description);
        return category;
    }

    @Transactional
    public void deleteCategory(Long categoryId) {
        MenuCategory category = getCategory(categoryId);
        if (menuItemRepository.countByCategory(category) > 0) {
            throw new IllegalStateException("Cannot delete a category that still contains menu items.");
        }
        menuCategoryRepository.delete(category);
    }

    public MenuItem addItem(Restaurant restaurant, MenuCategory category, String name, String description,
            BigDecimal price, boolean available, String thumbnailImageUrl, int sortOrder) {
        MenuItem item = new MenuItem(restaurant, name, description, price, available);
        item.setCategory(category);
        item.setThumbnailImageUrl(thumbnailImageUrl);
        item.setSortOrder(sortOrder);
        return menuItemRepository.save(item);
    }

    @Transactional
    public MenuItem updateItem(Long itemId, MenuCategory category, String name, String description, BigDecimal price,
            boolean available, int sortOrder) {
        MenuItem item = getMenuItem(itemId);
        item.setCategory(category);
        item.setName(name);
        item.setDescription(description);
        item.setPrice(price);
        item.setAvailable(available);
        item.setSortOrder(sortOrder);
        return item;
    }

    @Transactional
    public MenuItem updateItemTranslation(Long itemId, Locale locale, String name, String description) {
        MenuItem item = getMenuItem(itemId);
        MenuItemTranslation translation = item.translation(locale.getLanguage());
        translation.setName(name);
        translation.setDescription(description);
        return item;
    }

    @Transactional
    public MenuItem updateThumbnail(Long itemId, String thumbnailImageUrl) {
        MenuItem item = getMenuItem(itemId);
        item.setThumbnailImageUrl(thumbnailImageUrl);
        return item;
    }

    @Transactional
    public void deleteItem(Long itemId) {
        menuItemRepository.delete(getMenuItem(itemId));
    }

    @Transactional
    public void toggleAvailability(Long id) {
        MenuItem item = getMenuItem(id);
        item.setAvailable(!item.isAvailable());
    }
}
