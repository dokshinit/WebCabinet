package app.dialog;

import app.AppUI;
import com.vaadin.server.Resource;
import com.vaadin.server.ThemeResource;
import com.vaadin.shared.ui.ContentMode;
import com.vaadin.ui.*;

/**
 * @author Aleksey Dokshin <dant.it@gmail.com> (05.12.17).
 */
public class MessageDialog extends Window implements Button.ClickListener {

    private static final String home = "images/message-dialog/";

    public static Resource imgConfirmButton = new ThemeResource(home + "");
    public static Resource imgCancelButton = new ThemeResource(home + "");
    public static Resource imgCloseButton = new ThemeResource(home + "../cancel_16x16.png");

    public static Resource imgInfo = new ThemeResource(home + "info_24x24.png");
    public static Resource imgInfoBig = new ThemeResource(home + "info_64x64.png");
    public static Resource imgWarning = new ThemeResource(home + "warning_32x32.png");
    public static Resource imgWarningBig = new ThemeResource(home + "warning_64x64.png");
    public static Resource imgError = new ThemeResource(home + "error_32x32.png");
    public static Resource imgErrorBig = new ThemeResource(home + "error_64x64.png");
    public static Resource imgConfigm = new ThemeResource(home + "confirm_32x32.png");
    public static Resource imgConfirmBig = new ThemeResource(home + "confirm_64x64.png");

    public enum Type {
        INFO, WARNING, ERROR, CONFIRM
    }

    protected VerticalLayout contentLayout;
    protected HorizontalLayout messageLayout;
    protected Image messageImage;
    protected Label messageLabel;
    protected HorizontalLayout buttonLayout;
    protected Button confirmButton, cancelButton, closeButton;
    protected boolean confirmed;

    protected MessageDialog setup(Resource icon, String title, Resource msgicon, String msgtext) {
        messageLayout = new HorizontalLayout();
        messageLayout.setMargin(false);
        messageLayout.setSizeFull();
        messageLayout.addComponent(messageImage = new Image(null, msgicon));
        messageLayout.addComponent(messageLabel = new Label(msgtext, ContentMode.HTML));
        messageImage.addStyleName("message-image");
        messageLabel.addStyleName("message-label");
        messageLayout.setComponentAlignment(messageImage, Alignment.MIDDLE_CENTER);
        messageLayout.setComponentAlignment(messageLabel, Alignment.MIDDLE_LEFT);
        messageLayout.setExpandRatio(messageLabel, 1f);
        messageLayout.addStyleName("message-headLayout");

        buttonLayout = new HorizontalLayout();
        buttonLayout.setMargin(false);
        buttonLayout.setSizeFull();
        buttonLayout.addStyleName("buttons-headLayout");

        contentLayout = new VerticalLayout();
        contentLayout.setSizeFull();
        contentLayout.addComponent(messageLayout);
        contentLayout.addComponent(buttonLayout);
        contentLayout.addStyleName("dialog-headLayout");

        setIcon(icon);
        setCaption(title);
        setModal(true);
        setContent(contentLayout);

        contentLayout.setSizeUndefined(); // Для того, чтобы окно растянулось по контенту!
        setResizable(false);
        addStyleName("message-dialog");

        return this;
    }

    private void addButton(Button button, Alignment align) {
        buttonLayout.addComponent(button);
        buttonLayout.setComponentAlignment(button, align);
    }

    public MessageDialog(Type type, String title, String message) {
        super();

        confirmed = false;

        confirmButton = new Button("Подтвердить", imgConfirmButton);
        confirmButton.addClickListener(this);
        cancelButton = new Button("Отменить", imgCancelButton);
        cancelButton.addClickListener(this);
        closeButton = new Button("Закрыть");
        closeButton.setIcon(imgCloseButton);
        //closeButton.addStyleName(ValoTheme.BUTTON_ICON_ALIGN_RIGHT);
        closeButton.addClickListener(this);

        switch (type) {
            case WARNING:
                setup(imgWarning, title == null ? "Предупреждение" : title, imgWarningBig, message);
                addButton(closeButton, Alignment.MIDDLE_RIGHT);
                addStyleName("warning");
                break;
            case ERROR:
                setup(imgError, title == null ? "Ошибка" : title, imgErrorBig, message);
                addButton(closeButton, Alignment.MIDDLE_RIGHT);
                addStyleName("error");
                break;
            case CONFIRM:
                setup(imgConfigm, title == null ? "Подтверждение" : title, imgConfirmBig, message);
                addButton(confirmButton, Alignment.MIDDLE_LEFT);
                addButton(cancelButton, Alignment.MIDDLE_RIGHT);
                addStyleName("confirm");
                break;
            case INFO:
            default:
                setup(imgInfo, title == null ? "Информация" : title, imgInfoBig, message);
                addButton(closeButton, Alignment.MIDDLE_RIGHT);
                addStyleName("info");
                break;
        }

        center();
    }

    @Override
    public void buttonClick(Button.ClickEvent clickEvent) {
        confirmed = (clickEvent.getSource() == confirmButton);
        close();
    }

    public boolean isConfirmed() {
        return confirmed;
    }

    public static MessageDialog showInfo(UI ui, String title, String message) {
        MessageDialog dlg = new MessageDialog(Type.INFO, title, message);
        if (ui == null) ui = AppUI.ui();
        ui.addWindow(dlg);
        return dlg;
    }

}
