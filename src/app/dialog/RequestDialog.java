package app.dialog;

import app.ExError;
import app.model.AppModel;
import app.model.Request;
import app.model.User;
import com.vaadin.icons.VaadinIcons;
import com.vaadin.server.FileDownloader;
import com.vaadin.server.Resource;
import com.vaadin.server.StreamResource;
import com.vaadin.server.ThemeResource;
import com.vaadin.shared.ui.ContentMode;
import com.vaadin.ui.*;
import util.StringTools;

import java.io.File;
import java.io.FileInputStream;
import java.util.Optional;

import static app.AppServlet.logger;
import static app.model.Helper.fmtDT86;
import static app.view.unit.UHelper.style;

/**
 * @author Aleksey Dokshin <dant.it@gmail.com> (05.12.17).
 */
public class RequestDialog extends Window implements Button.ClickListener {

    private static final String home = "images/message-dialog/";

    public static Resource imgConfirmButton = new ThemeResource(home + "");
    public static Resource imgCancelButton = new ThemeResource(home + "");

    protected VerticalLayout contL;
    protected VerticalLayout formL;
    protected CheckBox checkSend;
    protected Button confirmButton, cancelButton;
    protected boolean confirmed;
    protected Request request;
    protected boolean isRO;

    public RequestDialog(Request req) {
        super();

        request = req;
        isRO = request.getId() != null;

        buildUI();
    }

    private int col1w = 150;

    private Label addLine(String s, AbstractComponent comp, AbstractComponent comp2) {
        Label l1 = style(new Label(s), "line-title");
        l1.setSizeUndefined();
        l1.setWidth(col1w, Unit.PIXELS);

        comp.setSizeUndefined();
        HorizontalLayout hL = style(new HorizontalLayout(), "line-L");
        hL.setSpacing(false);
        hL.addComponents(l1, comp);
        if (comp2 != null) {
            hL.addComponent(comp2);
        }
        hL.setExpandRatio(comp, 1);
        hL.setSizeUndefined();
        hL.setWidth("100%");
        hL.setMargin(false);

        formL.addComponents(hL);
        return l1;
    }

    private Label addLine(String s, AbstractComponent comp) {
        return addLine(s, comp, null);
    }

    private Label addLine(String s, String t) {
        Label l2 = style(new Label(t, ContentMode.HTML), "line-text");
        addLine(s, l2);
        return l2;
    }

    class ModeItem {
        int id;
        String title;

        public ModeItem(int id, String title) {
            this.id = id;
            this.title = title;
        }
    }

    ComboBox<ModeItem> modeCombo;

    private void buildUI() {

        User user = AppModel.get().getUser();

        setCaption(isRO ? "Просмотр заявки" : "Создание заявки");

        formL = style(new VerticalLayout(), "form-L");
        formL.setMargin(false);
        if (isRO) addLine("Номер", "" + request.getId());
        if (isRO) addLine("Создана", fmtDT86(request.getDtCreate()));
        addLine("Тип", request.getType().title);
        addLine("Название", request.getTitle());
        Label l2 = addLine("Параметры", request.getParamsTitle());
        l2.setDescription(request.getParamsAsHTML(), ContentMode.HTML);

        ////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        if (request.getType() == Request.Type.REPORT) {
            modeCombo = style(new ComboBox<>(), "small", "report-mode");
            modeCombo.setEmptySelectionAllowed(false);
            modeCombo.setTextInputAllowed(false);
            modeCombo.setItemCaptionGenerator(e -> e.title);

            ModeItem[] items = null;
            ModeItem defItem = null;

            switch (request.getReportType()) {
                case TURNOVER:
                    break;
                case TRANSACTION:
                    items = new ModeItem[]{
                            new ModeItem(1, "Без группировки"),
                            defItem = new ModeItem(2, "Группировка по картам"),
                            new ModeItem(3, "Группировка по видам Н/П"),
                            new ModeItem(4, "Группировка по картам и видам Н/П"),
                            new ModeItem(5, "Группировка по видам Н/П и картам")};
                    break;
                case CARD:
                    items = new ModeItem[]{
                            new ModeItem(1, "Без группировки"),
                            defItem = new ModeItem(2, "Группировка по состоянию")};
                    break;
            }
            if (defItem != null) {
                modeCombo.setItems(items);
                Integer id = request.getReportModeParam();
                if (id != null) for (ModeItem i : items) if (i.id == id) defItem = i;
                modeCombo.setSelectedItem(defItem);
                if (isRO) {
                    modeCombo.setEnabled(false);
                    addLine("Вид отчёта", defItem.title);
                } else {
                    addLine("Вид отчёта", modeCombo);
                }
            }
        }

        ////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        if (request.getType() == Request.Type.EXPORT) {
            modeCombo = style(new ComboBox<>(), "small", "report-mode");
            modeCombo.setEmptySelectionAllowed(false);
            modeCombo.setTextInputAllowed(false);
            modeCombo.setItemCaptionGenerator(e -> e.title);

            ModeItem[] items = null;
            ModeItem defItem = null;

            switch (request.getExportType()) {
                case TRANSACTION:
                    items = new ModeItem[]{
                            defItem = new ModeItem(1, "XLS - Microsoft Excel 97-2003")};
                    break;
            }
            if (defItem != null) {
                modeCombo.setItems(items);
                Integer id = request.getExportModeParam();
                if (id != null) for (ModeItem i : items) if (i.id == id) defItem = i;
                modeCombo.setSelectedItem(defItem);
                if (isRO) {
                    modeCombo.setEnabled(false);
                    addLine("Формат экспорта", defItem.title);
                } else {
                    addLine("Формат экспорта", modeCombo);
                }
            }
        }


        if (isRO) {
            if (request.getDtProcess() != null) addLine("Обработана", fmtDT86(request.getDtProcess()));
            if (request.getFileSize() != null) {
                String path = request.getAnswerPath(user) + File.separator;
                String name = request.getFileName();
                //
                Label flab = style(new Label(name, ContentMode.HTML), "line-text");
                Button fbutt = style(new Button(VaadinIcons.DOWNLOAD), "small", "line-button");
                fbutt.setDescription("Скачать файл");
                addLine("Файл ответа", flab, fbutt);
                addLine("Размер ответа", "" + request.getFileSize());
                //
                StreamResource myResource = new StreamResource((StreamResource.StreamSource) () -> {
                    try {
                        return new FileInputStream(path + name);
                    } catch (Exception e) {
                        logger.errorf(e, "Ошибка создания потока чтения файла для скачивания: %s", path + name);
                        return null;
                    }
                }, name);
                FileDownloader fileDownloader = new FileDownloader(myResource);
                fileDownloader.extend(fbutt);

                fbutt.addClickListener((e) -> {
                    fbutt.setEnabled(false);
                    fbutt.setDescription("Для повторного скачивания откройте заявку заново");
                });
            }
            if (request.getDtSend() != null) addLine("Ответ отправлен", fmtDT86(request.getDtSend()));
            addLine("Состояние", request.getState().title);
            if (request.getState() == Request.State.ERROR)
                addLine("Причина", request.getResult() == null ? "" : request.getResult());
            if (!request.getComment().isEmpty())
                addLine("Примечание", request.getComment());
        }
        checkSend = style(new CheckBox("Отправить ответ на учетный email клиента?"), "line-check");
        if (!isRO) {
            if (!StringTools.isEmptySafe(user.getClientEmail())) {
                checkSend.setValue(true);
            } else {
                checkSend.setValue(false);
                checkSend.setEnabled(false);
                checkSend.setDescription("В учётных данных не указан email клиента!");
            }
            addLine("", checkSend);
        }

        confirmButton = style(new Button("Подтвердить"), "small", "confirm-button");//, imgConfirmButton);
        confirmButton.addClickListener(this);
        confirmButton.setVisible(!isRO); // Только для создания запроса! Для просмотра - отключаем.
        cancelButton = style(new Button(isRO ? "Закрыть" : "Отменить"), "small", "cancel-button");//, imgCancelButton);
        cancelButton.addClickListener(this);

        HorizontalLayout buttL = style(new HorizontalLayout(confirmButton, cancelButton), "button-L");
        buttL.setComponentAlignment(confirmButton, Alignment.MIDDLE_LEFT);
        buttL.setComponentAlignment(cancelButton, Alignment.MIDDLE_RIGHT);
        buttL.setWidth("100%");

        contL = style(new VerticalLayout(formL, buttL), "root-L");
        setContent(contL);

        addStyleName("request-dialog");
        setModal(true);
        setResizable(false);

        center();
    }

    @Override
    public void buttonClick(Button.ClickEvent clickEvent) {
        if (clickEvent.getSource() == confirmButton) {
            confirmed = true;
            try {
                if (checkSend.getValue()) {
                    request.setSendTryRemain(3); // Указываем 3 попытки отправки.
                }
                Optional<ModeItem> item;
                switch (request.getType()) {
                    case REPORT:
                        item = modeCombo.getSelectedItem();
                        if (item.isPresent()) request.setReportModeParam(item.get().id);
                        break;
                    case EXPORT:
                        item = modeCombo.getSelectedItem();
                        if (item.isPresent()) request.setExportModeParam(item.get().id);
                        break;
                }
                AppModel.get().createRequest(request);
            } catch (ExError ex) {
                MessageDialog dlg = MessageDialog.showInfo(getUI(), "Ошибка создания заявки!", ex.getMessage());
                getUI().addWindow(dlg);
            }
        }
        close();
    }

    public boolean isConfirmed() {
        return confirmed;
    }
}
