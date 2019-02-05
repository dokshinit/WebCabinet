package app;

import app.model.AppModel;
import com.vaadin.annotations.VaadinServletConfiguration;
import com.vaadin.server.*;
import fbdbengine.FB_Database;
import org.jsoup.nodes.Element;
import xconfig.XConfig;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import java.io.File;
import java.net.URI;
import java.util.ArrayList;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;

@WebServlet(value = "/*", asyncSupported = true)
@VaadinServletConfiguration(productionMode = true, ui = AppUI.class)
@SuppressWarnings("serial")
public class AppServlet extends VaadinServlet implements SessionInitListener, SessionDestroyListener {

    /**
     * Логгер.
     */
    static {
        LoggerExt.setConsoleFormatter(new LogFormatter(true, false, true));
    }
    public static final LoggerExt logger = LoggerExt.getNewLogger("AppServlet").enable(true);

    /**
     * Карта активных соединений.
     */
    private final ConcurrentHashMap<String, VaadinSession> sessions = new ConcurrentHashMap<>();

    private FB_Database db;
    private String catalinaHome, appHome, answersPath;

    public FB_Database getDB() {
        return db;
    }

    public static AppServlet getApp() {
        return (AppServlet) getCurrent();
    }

    public static FB_Database db() {
        return getApp().getDB();
    }

    public static String getAnswersPath() {
        return getApp().answersPath;
    }

    public static boolean isDeveloping = false;

    @Override
    public void init(ServletConfig servletConfig) throws ServletException {
        catalinaHome = System.getProperty("catalina.base");
        appHome = servletConfig.getServletContext().getRealPath("/");

        logger.toFile(appHome + "/logs", logger.getFilePattern(), logger.getFileCheck());

        super.init(servletConfig);

        CustomizedSystemMessages messages = new CustomizedSystemMessages();
        messages.setSessionExpiredCaption("Сессия истекла");
        messages.setSessionExpiredMessage("Кликните <u>сюда</u> или нажмите ESC для продолжения!");
        messages.setAuthenticationErrorCaption("Ошибка авторизации");
        messages.setAuthenticationErrorMessage("Запомните все несохраненные данные, кликните <u>сюда</u> или нажмите ESC для продолжения!");
        messages.setCommunicationErrorCaption("Ошибка свзяи");
        messages.setCommunicationErrorMessage("Запомните все несохраненные данные, кликните <u>сюда</u> или нажмите ESC для продолжения!");
        messages.setCookiesDisabledCaption("Отключены cookies");
        messages.setCookiesDisabledMessage("Для работы данного приложения необходимы cookies.<br>Включите cookies в браузере и кликните <u>сюда</u> или нажмите ESC для продолжения!");
        messages.setInternalErrorCaption("Внутренняя ошибка");
        messages.setInternalErrorMessage("Пожалуйста, уведомите о данной ошибке администратора.<br>Запомните все несохраненные данные, кликните <u>сюда</u> или нажмите ESC для продолжения!");

        getService().setSystemMessagesProvider(e -> messages);
    }

    @Override
    protected final void servletInitialized() throws ServletException {
        super.servletInitialized();
        logger.infof("Servlet started!");
        getService().addSessionInitListener(this);
        getService().addSessionDestroyListener(this);

        String base, user, password;

        logger.infof("Загрузка конфигурации...");
        try {
            XConfig cfg = new XConfig();
            cfg.load(appHome + File.separator + "app.config");

            base = cfg.getKey("db.host", "127.0.0.1") + ":" + cfg.getKey("db.alias", "WebCenter");
            user = cfg.getKey("db.user", "WEB");
            password = cfg.getKey("db.password", "xxxxxxxx");
            answersPath = cfg.getKey("requestprocessor.answerspath", appHome + File.separator + "answers");
            isDeveloping = cfg.getIntKey("developing", 0) != 0;

        } catch (Exception ex) {
            base = "127.0.0.1:WebCenter";
            user = "WEB";
            password = "xxxxxxxx";
            answersPath = appHome + File.separator + "answers";
            isDeveloping = false;

            logger.infof("Ошибка загрузки конфигурации: %s! Приняты параметры по умолчанию!", ex.getMessage());
        }


        try {
            db = new FB_Database(false, base, user, password, "UTF-8", false);
        } catch (Exception ex) {
            throw new ServletException("Ошибка настройки параметров БД!", ex);
        }
    }


    @Override
    public void destroy() {
        final ArrayList<VaadinSession> vslist = new ArrayList<>(sessions.values());
        for (VaadinSession vs : vslist) {
            vs.access(() -> {
                if (vs.getState() == VaadinSession.State.OPEN) {
                    logger.infof("Session turn stop! pId=%s", vs.getPushId());
                    vs.getSession().invalidate(); // Делаем соединение невалидным (инициирует закрытие его UI и закрытие соединения)!
                    vs.close();
                }
            });
        }

        db.close();

        logger.infof("Servlet stopped!");
        super.destroy();
    }

    public static final Locale LOCALE_RU = new Locale("ru");

    @Override
    public void sessionInit(SessionInitEvent event) throws ServiceException {
        VaadinSession vs = event.getSession();
        String id = vs.getPushId();
        vs.setLocale(LOCALE_RU);

        logger.infof("Session started! pId=%s", id);
        sessions.put(id, vs);
        vs.setAttribute(AppModel.ATTR_ID, new AppModel(vs));
        vs.addBootstrapListener(new SessionBootstrapListener());
    }

    @Override
    public void sessionDestroy(SessionDestroyEvent event) {
        String id = event.getSession().getPushId();
        logger.infof("Session stopped! pId=%s", id);
        sessions.remove(id);
    }

    private class SessionBootstrapListener implements BootstrapListener {

        @Override
        public void modifyBootstrapPage(final BootstrapPageResponse response) {
            final Element head = response.getDocument().head();
            head.appendElement("meta")
                    .attr("name", "viewport")
                    .attr("content", "width=device-width, initial-scale=1, minimum-scale=0.5, user-scalable=yes"); // , maximum-scale=1.0, user-scalable=no
            head.appendElement("meta")
                    .attr("name", "apple-mobile-web-app-capable")
                    .attr("content", "yes");
            head.appendElement("meta")
                    .attr("name", "apple-mobile-web-app-status-bar-style")
                    .attr("content", "black-translucent");

            // Аттрибуты для нормального отображения иконки приложения на вкладках браузера.
            String contextPath = response.getRequest().getContextPath();
            String logo = contextPath + "/VAADIN/themes/webcabinet/favicon.ico";
            head.getElementsByAttributeValue("rel", "shortcut icon")
                    .attr("href", logo);
            head.getElementsByAttributeValue("rel", "icon")
                    .attr("href", logo);
            head.appendElement("link")
                    .attr("rel", "apple-touch-icon").attr("href", logo);
        }

        @Override
        public void modifyBootstrapFragment(final BootstrapFragmentResponse response) {
        }


    }

//
//    void mmm() {
//        URI uri = Page.getCurrent().getLocation();
//        String u0 = uri.getScheme();
//        String u1 = uri.getHost();
//        String u2 = "" + uri.getPort();
//        String u3 = uri.getPath();
//        String u4 = uri.getFragment();
//
//
//    }
}


