package app.view;

import app.AppNavigator;
import app.AppUI;
import com.vaadin.navigator.View;
import com.vaadin.server.Page;
import com.vaadin.server.Responsive;
import com.vaadin.ui.*;
import com.vaadin.ui.themes.ValoTheme;

import static app.view.unit.Helper.*;

@SuppressWarnings("serial")
public class MainView extends HorizontalLayout {

    final MainViewMenu menu;
    final CssLayout content;
    final AppNavigator navigator;
    AbstractUnitView unitView = null;

    public MainView() {
        setSizeFull();
        setSpacing(false);
        setMargin(false);
        addStyleName("main-view");
        Responsive.makeResponsive(this);

        addComponent(menu = new MainViewMenu());

        content = style(new CssLayout(), "unitview-content");
        content.setSizeFull();
        addComponent(content);
        setExpandRatio(content, 1.0f);

        navigator = new AppNavigator(this, content);

        // Для того, чтобы меню скрывалось (если всплывающее) при клике на контенте.
        content.addLayoutClickListener((event) -> menu.hideMenu());

//        final Button logoutButton = new Button("Отлогиниться", (e) -> fireOnLogout());
//        addComponent(logoutButton);
//
//        final Button exitButton = new Button("Выход", (e) -> {
//            for (UI ui : VaadinSession.getCurrent().getUIs()) {
//                ui.access(() -> {
//                    //view.getPage().setLocation("/Logout");
//                    //view.close();
//                    ui.setContent(new Label("LOGOUT!"));
//                    if (ui != getUI()) ui.push();
//                });
//            }
//            //getSession().close();
//        });
//        addComponent(exitButton);
//
//        final Button accButton = new Button("Счета");
//        accButton.addListener((e) -> {
//            Window w = new Window("Счета клиентов");
//            w.setClosable(true);
//            w.setResizable(true);
//            getUI().addWindow(w);
//            w.center();
//        });
//        addComponent(accButton);
    }

    public void fireOnBrowserResized(final Page.BrowserWindowResizeEvent event) {
        if (unitView != null) unitView.fireOnBrowserResized(event);
    }

    public void fireOnViewChanged(UnitViewType vtype, View view) {
        menu.fireOnViewChanged(vtype);
        AppUI ui = (AppUI) getUI();
        ui.fireOnBrowserResized(null);
        ui.closeOpenedWindows();
        unitView = (AbstractUnitView) view;
    }

    @Override
    public void detach() {
        navigator.destroy(); // Для того, чтобы отключать навигатор от UI.
        super.detach();
    }
}
