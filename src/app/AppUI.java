package app;

import app.model.AppModel;
import app.view.LoginView;
import app.view.MainView;
import app.view.UnitViewType;
import com.vaadin.annotations.Push;
import com.vaadin.annotations.Theme;
import com.vaadin.annotations.Title;
import com.vaadin.annotations.Widgetset;
import com.vaadin.server.Page;
import com.vaadin.server.Responsive;
import com.vaadin.server.VaadinRequest;
import com.vaadin.server.VaadinSession;
import com.vaadin.shared.communication.PushMode;
import com.vaadin.ui.*;
import com.vaadin.ui.themes.ValoTheme;

import java.net.URI;
import java.util.Locale;

import static app.AppServlet.logger;
import static app.view.unit.Helper.style;

/**
 * @author lex
 */
@Theme("webcabinet")
@Widgetset("app.WidgetSet")
@Title("Личный кабинет")
@Push(PushMode.MANUAL)
public class AppUI extends UI {

    protected AppModel model;
    protected MainView mainView;
    protected LoginView loginView;

    public AppModel getModel() {
        return model;
    }

    public static AppUI ui() {
        return (AppUI) AppUI.getCurrent();
    }

    public static AppModel model() {
        return ui().getModel();
    }

    @Override
    protected void init(VaadinRequest vaadinRequest) {
//        Page page = Page.getCurrent();
//        URI loc = page.getLocation();
//        logger.infof("INITUI: loc='%s'", loc.toString());
//        if (!loc.getPath().isEmpty() && !"/".equals(loc.getPath())) {
//            try {
//                loc = new URI(loc.getScheme() + "://" + loc.getHost() + ":" + loc.getPort() + "/" +
//                        (loc.getFragment() == null ? "" : ("#" + loc.getFragment())));
//                page.setLocation(loc);
//                logger.infof("INITUI: newloc='%s'", loc.toString());
//            } catch (Exception ex) {
//                logger.infof("INITUI: newloc error '%s'", ex.getMessage());
//            }
//        }
        this.setLocale(AppServlet.LOCALE_RU);

        model = (AppModel) getSession().getAttribute(AppModel.ATTR_ID);

        AppServlet.logger.infof("AppUI init! id=%d", getUIId());
        setLocale(Locale.US);
        getReconnectDialogConfiguration().setDialogText("Соединение разорвано, переподключение!");
        Responsive.makeResponsive(this);

        updateContent();

        Page.getCurrent().addBrowserWindowResizeListener((e) -> fireOnBrowserResized(e));
    }

    public void updateContent() {
        if (model.isUserAuthorized()) {
            if (loginView != null) {
                loginView = null;
                removeStyleName("login-ui");
                setContent(null);
                //setResponsive(false);
                Responsive.makeResponsive(this);
            }
            addStyleName(ValoTheme.UI_WITH_MENU);
            setContent(mainView = new MainView());
            getNavigator().navigateTo(getNavigator().getState());
        } else {
            logAction(AppModel.LogActionPage.LOGIN, AppModel.LogActionType.OPENPAGE);
            mainView = null;
            removeStyleName(ValoTheme.UI_WITH_MENU);
            setContent(loginView = style(new LoginView(), "login-ui"));
        }
    }

    @Override
    public void detach() {
        AppServlet.logger.infof("AppUI detach! id=%d", getUIId());
        super.detach();
    }

    @Override
    public void close() {
        AppServlet.logger.infof("AppUI close! id=%d", getUIId());
        super.close();
    }

    public void closeOpenedWindows() {
        for (Window window : getWindows()) window.close();
    }

    public void fireOnBrowserResized(Page.BrowserWindowResizeEvent event) {
        if (mainView != null) mainView.fireOnBrowserResized(event);
    }

    @FunctionalInterface
    public interface AppUIRunnable {
        void run(AppUI curui, AppUI ui);
    }

    public static void applyForUIs(AppUIRunnable run) {
        UI curui = getCurrent(); // Передаём внутрь тек.UI, т.к. при access он переключается на запрощенный!
        for (UI ui : VaadinSession.getCurrent().getUIs()) {
            ui.access(() -> run.run((AppUI) curui, (AppUI) ui));
        }
    }

    public static void userLogout() {
        // Снимаем авторизацию.
        AppUI.model().logoutUser();
        // Обновляем все UI сессии.
        AppUI.applyForUIs((curui, ui) -> {
            ui.updateContent();
            if (ui != curui) ui.push(); // Если не текущий UI, то обновляем его состояние.
        });
    }

    public void logAction(AppModel.LogActionPage page, AppModel.LogActionType type) {
        getModel().logAction(page, type);
    }

    public void logAction(UnitViewType vtype, AppModel.LogActionType type) {
        logAction(AppModel.LogActionPage.byUVType(vtype), type);
    }

    public void logAction(AppModel.LogActionType type) {
        if (mainView != null) {
            logAction(mainView.getUnitViewType(), type);
        } else if (loginView != null) {
            logAction(AppModel.LogActionPage.LOGIN, type);
        }
    }
}
