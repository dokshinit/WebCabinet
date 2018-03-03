package app.view;

import app.AppUI;
import app.model.AppModel;
import app.model.User;
import com.vaadin.icons.VaadinIcons;
import com.vaadin.server.ThemeResource;
import com.vaadin.shared.ui.ContentMode;
import com.vaadin.ui.*;
import com.vaadin.ui.themes.ValoTheme;

import java.util.ArrayList;

/**
 * @author Aleksey Dokshin <dant.it@gmail.com> (05.12.17).
 */
public class MainViewMenu extends CustomComponent {

    public static final String ID = "main-menu";
    private static final String STYLE_VISIBLE = "valo-menu-visible";
    private MenuBar.MenuItem settingsItem;
    private ArrayList<ValoMenuItemButton> viewItems;
    private CssLayout menuItemsLayout;

    public MainViewMenu() {
        setPrimaryStyleName("valo-menu");
        setId(ID);
        setSizeUndefined();

        viewItems = new ArrayList<>();

        setCompositionRoot(buildContent());
    }


    @Override
    public AppUI getUI() {
        return (AppUI) super.getUI();
    }

    private Component buildContent() {
        final CssLayout menuContent = new CssLayout();
        menuContent.addStyleName("sidebar");
        menuContent.addStyleName(ValoTheme.MENU_PART);
        menuContent.addStyleName("no-vertical-drag-hints");
        menuContent.addStyleName("no-horizontal-drag-hints");
        menuContent.setWidth(null);
        menuContent.setHeight("100%");

        menuContent.addComponent(buildTitle());
        menuContent.addComponent(buildUserMenu());
        menuContent.addComponent(buildToggleButton());
        menuContent.addComponent(buildMenuItems());

        return menuContent;
    }

    private Component buildTitle() {
        Label logo = new Label("Личный кабинет <span style='font-weight: bold'>СТК</span>" +
                "<div class='test'>Тестовый режим!</div>", ContentMode.HTML);
        logo.setSizeUndefined();
        logo.addStyleName("main-menu-logo");
        HorizontalLayout logoWrapper = new HorizontalLayout(logo);
        logoWrapper.setComponentAlignment(logo, Alignment.MIDDLE_CENTER);
        logoWrapper.addStyleName("valo-menu-title");
        logoWrapper.setSpacing(false);
        return logoWrapper;
    }

    private Component buildUserMenu() {
        final MenuBar settings = new MenuBar();
        settings.addStyleName("user-menu");
        final User user = AppUI.model().getUser();
        settingsItem = settings.addItem("", new ThemeResource("images/profile-pic-300px.jpg"), null);
        settingsItem.setText(user.getFirstName() + " " + user.getLastName());

        settingsItem.addItem("Профиль", (item) -> fireOnProfileItem()).setEnabled(false);
        settingsItem.addSeparator();
        settingsItem.addItem("Выход", (item) -> fireOnLogoutItem());

        return settings;
    }

    void fireOnProfileItem() {
    }

    void fireOnLogoutItem() {
        getUI().logAction(AppModel.LogActionType.LOGOUT);
        AppUI.userLogout();
    }

    private Component buildToggleButton() {
        Button valoMenuToggleButton = new Button("Меню", (e) -> {
            if (getCompositionRoot().getStyleName().contains(STYLE_VISIBLE)) {
                getCompositionRoot().removeStyleName(STYLE_VISIBLE);
            } else {
                getCompositionRoot().addStyleName(STYLE_VISIBLE);
            }
        });
        valoMenuToggleButton.setIcon(VaadinIcons.LIST);
        valoMenuToggleButton.addStyleName("valo-menu-toggle");
        valoMenuToggleButton.addStyleName(ValoTheme.BUTTON_BORDERLESS);
        valoMenuToggleButton.addStyleName(ValoTheme.BUTTON_SMALL);
        return valoMenuToggleButton;
    }

    private ValoMenuItemButton addItem(UnitViewType vtype) {
        ValoMenuItemButton menuItemComponent = new ValoMenuItemButton(vtype);
        viewItems.add(menuItemComponent);
        menuItemsLayout.addComponent(menuItemComponent);
        return menuItemComponent;
    }

    private Component buildMenuItems() {
        menuItemsLayout = new CssLayout();
        menuItemsLayout.addStyleName("valo-menuitems");

        addItem(UnitViewType.INFORMATION);
        addItem(UnitViewType.TURNOVER);
        addItem(UnitViewType.TRANSACTIONS);
        addItem(UnitViewType.CARDS);
        addItem(UnitViewType.REQUESTS);

        return menuItemsLayout;
    }

    @Override
    public void attach() {
        super.attach();
        //updateNotificationsCount(null);
    }

    public void hideMenu() {
        getCompositionRoot().removeStyleName(STYLE_VISIBLE);
    }

//    //@Subscribe
//    public void updateNotificationsCount(
//            final NotificationsCountUpdatedEvent event) {
//        int unreadNotificationsCount = DashboardUI.getDataProvider()
//                .getUnreadNotificationsCount();
//        notificationsBadge.setValue(String.valueOf(unreadNotificationsCount));
//        notificationsBadge.setVisible(unreadNotificationsCount > 0);
//    }

//    //@Subscribe
//    public void updateReportsCount(final ReportsCountUpdatedEvent event) {
//        reportsBadge.setValue(String.valueOf(event.getCount()));
//        reportsBadge.setVisible(event.getCount() > 0);
//    }

    public final class ValoMenuItemButton extends Button {

        private static final String STYLE_SELECTED = "selected";
        private final UnitViewType viewType;

        public ValoMenuItemButton(final UnitViewType view) {
            this.viewType = view;
            setPrimaryStyleName("valo-menu-item");
            setIcon(view.getIcon());
            setCaption(view.getViewTitle());
            addClickListener((event) -> {
                UI.getCurrent().getNavigator().navigateTo(view.getViewName());
            });
        }

        public void fireOnViewChanged(final UnitViewType vtype) {
            //AppServlet.logger.infof("FIRE ViewChange MENU! view='%s' changed='%s'", viewType.name(), vtype.name());
            removeStyleName(STYLE_SELECTED);
            if (vtype == viewType) addStyleName(STYLE_SELECTED);
        }
    }

    public void fireOnViewChanged(final UnitViewType vtype) {
        for (ValoMenuItemButton item : viewItems) item.fireOnViewChanged(vtype);
        hideMenu();
    }
}
