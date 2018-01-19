package app.view;

import app.AppServlet;
import app.AppUI;
import app.ExError;
import app.dialog.MessageDialog;
import app.model.AppModel;
import com.vaadin.data.Binder;
import com.vaadin.data.ValidationException;
import com.vaadin.event.ShortcutAction.KeyCode;
import com.vaadin.icons.VaadinIcons;
import com.vaadin.server.UserError;
import com.vaadin.shared.ui.ContentMode;
import com.vaadin.ui.*;
import com.vaadin.ui.themes.ValoTheme;

/**
 * Компонент, реализующий процесс авторизации пользователя (готовый контент UI).
 */
@SuppressWarnings("serial")
public class LoginView extends Panel {

    private TextField loginField;
    private PasswordField passwordField;
    private Button signinButton;

    public LoginView() {
        final Component loginForm = buildLoginComponent();

        VerticalLayout panellayout = new VerticalLayout();
        panellayout.setMargin(false);
        panellayout.setSpacing(false);
        panellayout.setSizeUndefined(); // Стилем регулироваться будет.
        panellayout.addComponent(loginForm);
        panellayout.setComponentAlignment(loginForm, Alignment.MIDDLE_CENTER);
        panellayout.addStyleName("layout");

        setSizeFull();
        setContent(panellayout);
        addStyleName("login-view");
    }

    String login = "12345", password = "1234";
    Binder<String> loginBind = new Binder<>();
    Binder<String> passwordBind = new Binder<>();

    private Component buildLoginComponent() {

        // Раскладка-псевдоокно.
        final VerticalLayout layout = new VerticalLayout();
        layout.setSizeUndefined();
        layout.setMargin(false);
        layout.setSpacing(false);
        layout.addStyleName("panel");

        // Заголовок.
        Label logo1 = new Label("<b>ООО \"Транзит-Плюс\"</b>", ContentMode.HTML);
        logo1.addStyleName("firm");
        Label logo2 = new Label("<b>С</b>истема <b>Т</b>опливных <b>К</b>арт", ContentMode.HTML);
        logo2.addStyleName("system");

        VerticalLayout labels = new VerticalLayout();
        labels.addStyleName("logo");
        labels.setHeightUndefined();
        labels.setMargin(false);
        labels.setSpacing(false);
        labels.addComponents(logo1, logo2);
        labels.setComponentAlignment(logo1, Alignment.MIDDLE_CENTER);
        labels.setComponentAlignment(logo2, Alignment.MIDDLE_CENTER);

        Label title = new Label("Авторизация доступа", ContentMode.HTML);
        title.addStyleName("title");

        // Поля.
        loginField = new TextField("ИНН");
        loginField.setWidth("225px");
        loginField.addStyleNames(ValoTheme.TEXTFIELD_INLINE_ICON);
        loginField.addStyleName("login");
        loginField.setIcon(VaadinIcons.USER);
        loginField.setPlaceholder("Логин");
        loginBind.forField(loginField)
                .asRequired("Обязательное поле")
                .withValidator((v) -> v.length() > 3, "Не может быть короче трех символов!")
                .bind((v) -> login, (c, v) -> login = v);
        passwordField = new PasswordField("Пароль");
        passwordField.setWidth("225px");
        passwordField.addStyleNames(ValoTheme.TEXTFIELD_INLINE_ICON);
        passwordField.addStyleName("password");
        passwordField.setIcon(VaadinIcons.LOCK);
        passwordField.setPlaceholder("Пароль");
        passwordBind.forField(passwordField)
                .asRequired("Обязательное поле")
                .withValidator((v) -> v.length() > 3, "Не может быть короче трех символов!")
                .bind((v) -> password, (c, v) -> password = v);

        signinButton = new Button("Войти в кабинет");
        signinButton.addStyleName("signin");
        signinButton.setClickShortcut(KeyCode.ENTER);
        signinButton.setWidth("225px");
        signinButton.focus();

        // Примечание.
        PopupView pv = new PopupView(new NotePopupContent());
        pv.setHideOnMouseOut(true);
        pv.addStyleName("note-popup"); // Этот стиль применяется к кнопке!

        VerticalLayout fields = new VerticalLayout();
        fields.addStyleName("form");
        fields.addComponents(title, loginField, passwordField, signinButton, pv);
        fields.setComponentAlignment(title, Alignment.MIDDLE_CENTER);
        fields.setComponentAlignment(signinButton, Alignment.BOTTOM_LEFT);

        signinButton.addClickListener((e) -> fireOnLogin());

        layout.addComponents(labels, fields);

        loginBind.readBean("");
        passwordBind.readBean("");

        return layout;
    }

    private void fireOnLogin() {
        // Проверка валидаторов.
        try {
            loginBind.writeBean("");
            passwordBind.writeBean("");
        } catch (ValidationException ex) {
            return;
        }
        AppServlet.logger.infof("Login: '%s' Psw: '%s'", login, password);

        // Авторизация.
        AppModel m = AppUI.model();
        boolean isold = m.isUserAuthorized();
        try {
            m.loginUser(loginField.getValue(), passwordField.getValue());
            signinButton.setComponentError(null);
            if (!m.isUserAuthorized()) {
                loginField.setComponentError(new UserError("Ошибка авторизации!"));
                passwordField.setComponentError(new UserError("Ошибка авторизации!"));
                MessageDialog dlg = MessageDialog.showInfo(getUI(), "Ошибка авторизации!", "Неверный идентификатор или пароль пользователя!");
            } else {
                loginField.setComponentError(null);
                passwordField.setComponentError(null);
            }
        } catch (ExError ex) {
            signinButton.setComponentError(new UserError(ex.getMessage()));
        }
        // При изменении состояния авторизации - обновляем контент.
        if (m.isUserAuthorized() != isold) {
            AppUI.applyForUIs((curui, ui) -> {
                ui.updateContent();
                if (ui != curui) ui.push();
            });
        }
    }


    private class NotePopupContent implements PopupView.Content {

        @Override
        public Component getPopupComponent() {
            Panel p = new Panel();
            p.setSizeUndefined();
            Label label = new Label();
            label.setValue("Для использования данного ресурса необходимо быть действующим клиентом " +
                    "системы топливных карт и иметь рабочий аккаунт для авторизации. Подробную информацию о системе " +
                    "топливных карт можно получить на официальном сайте " +
                    "<a href=\"http://tp-rk.ru\">ООО \"Транзит-Плюс\"</a>.");
            label.setContentMode(ContentMode.HTML);
            label.addStyleName("wrapped-label");
            AppServlet.logger.infof("POPUPPAGE: w=%d", getUI().getPage().getBrowserWindowWidth());
            if (getUI().getPage().getBrowserWindowWidth() > 800) label.setWidth("800px");
            else label.setWidth("100%");
            p.setContent(label);
            p.addStyleName("note-popup-panel"); // Применяем стиль к панели для снятия её декорации.
            return p;
        }

        @Override
        public String getMinimizedValueAsHTML() {
            return "* Примечание";
        }
    }
}
