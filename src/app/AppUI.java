package app;

import app.model.AppModel;
import app.view.LoginView;
import app.view.MainView;
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

import java.util.Locale;

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
        model = (AppModel) getSession().getAttribute(AppModel.ATTR_ID);

        AppServlet.logger.infof("AppUI init! id=%d", getUIId());
        setLocale(Locale.US);
        getReconnectDialogConfiguration().setDialogText("Соединение разорвано, переподключение!");
        Responsive.makeResponsive(this);
        //addStyleName(ValoTheme.UI_WITH_MENU);

        updateContent();

        // Some views need to be aware of browser resize events so a
        // BrowserResizeEvent gets fired to the event bus on every occasion.
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
            mainView = null;
            removeStyleName(ValoTheme.UI_WITH_MENU);
            setContent(loginView = new LoginView());
            addStyleName("login-ui");
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
}
