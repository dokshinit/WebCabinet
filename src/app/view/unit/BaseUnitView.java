package app.view.unit;

import app.AppServlet;
import app.AppUI;
import app.ExError;
import app.dialog.MessageDialog;
import app.model.*;
import app.sizer.SizeReporter;
import app.view.AbstractUnitView;
import com.vaadin.data.*;
import com.vaadin.data.provider.DataProvider;
import com.vaadin.data.provider.QuerySortOrder;
import com.vaadin.event.LayoutEvents;
import com.vaadin.icons.VaadinIcons;
import com.vaadin.server.Responsive;
import com.vaadin.server.UserError;
import com.vaadin.shared.data.sort.SortDirection;
import com.vaadin.shared.ui.ContentMode;
import com.vaadin.ui.*;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;
import util.CommonTools;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static app.model.Helper.*;
import static app.view.unit.UHelper.*;

/**
 * @author Aleksey Dokshin <dant.it@gmail.com> (13.12.17).
 */
public abstract class BaseUnitView<M extends BaseUnitView.BaseParamsModel> extends Panel implements AbstractUnitView {

    @Override
    public AppUI getUI() {
        return (AppUI) super.getUI();
    }

    public AppModel getModel() {
        return AppUI.model();
    }

    protected final AppModel model;
    protected final User user;

    protected VerticalLayout rootL;
    protected VerticalLayout headL;
    protected HorizontalLayout titleL;
    protected Label titleLabel, updateLabel;
    protected Button updateButton, reportButton;

    protected HorizontalLayout paramsL;
    protected Button paramsButton;
    protected Label paramsLabel;

    protected CssLayout toolbarL;
    protected HorizontalLayout dtwsL, combosL, buttonsL;
    protected DateField dtwField, dtwEndField;
    protected Button applyButton, cancelButton;
    //
    protected Binder<BaseUnitView> dtStartBind = new Binder<>();
    protected Binder<BaseUnitView> dtEndBind = new Binder<>();

    protected VerticalLayout bodyL;

    private SizeReporter gridSizeReporter;

    protected LocalDate dtFix;
    protected LocalDate datesCustomLimitStart, datesCustomLimitEnd;
    protected M curPM, toolbarPM;

    protected boolean hasUpdate = false;
    protected boolean hasReport = false;
    protected boolean hasParams = false;
    protected boolean toolbarAlwaysShowed = false;

    public BaseUnitView(String name, String title) {
        setLocale(AppServlet.LOCALE_RU);

        model = AppUI.model();
        user = model.getUser();
        dtFix = null;
        datesCustomLimitStart = null;
        datesCustomLimitEnd = null;

        curPM = null;
        toolbarPM = null;

        initVars();
        buildUI(name, title);

        // Тело.

        updateData();
        updateButtonsState(false);
    }

    /**
     * Метод для начального задания переменных (до создания UI!). Вызывается в конструкторе. Переопределяется в
     * потомках.
     */
    protected void initVars() {
        // Должно быть переопределено, если требуется модель тулбара задать!
    }

    protected void buildUI(String name, String title) {
        addStyleName("UV");
        addStyleName(name + "-UV");
        setSizeFull();

        rootL = style(new VerticalLayout(), "root-L");
        rootL.setSizeUndefined();
        rootL.setSpacing(false);
        rootL.setMargin(false);
        setContent(rootL);

        buildHead(title);
        buildBody();

        rootL.addLayoutClickListener(this::fireOnLayoutClick);
    }

    protected void buildHead(String title) {
        headL = style(new VerticalLayout(), "head-L");
        headL.setSpacing(false);
        rootL.addComponent(headL);

        buildHeadTitle(title); // Название.
        if (hasParams) {
            buildHeadParams(); // Параметры.
            buildHeadToolbar(); // Тулбар задания параметров.
            if (toolbarAlwaysShowed) showToolbar(true);
        }
    }

    protected void buildHeadTitle(String title) {
        titleLabel = style(new Label(title), "title-label");
        titleLabel.setSizeUndefined();

        updateLabel = style(new Label(), "info-label");
        updateLabel.setSizeUndefined();
        updateLabel.setDescription("Время запроса данных");

        updateButton = style(new Button(VaadinIcons.REFRESH), "small", "update-button");
        updateButton.setSizeUndefined();
        updateButton.setEnabled(false);
        updateButton.setTabIndex(-1);
        updateButton.setDescription("Обновить данные");
        if (hasUpdate) updateButton.addClickListener(e -> fireOnUpdateButtonClick());

        reportButton = style(new Button(VaadinIcons.CALENDAR_ENVELOPE), "small", "report-button");
        reportButton.setSizeUndefined();
        reportButton.setEnabled(false);
        reportButton.setTabIndex(-1);
        reportButton.setDescription("Запросить отчёт");
        if (hasReport) reportButton.addClickListener(e -> fireOnReportButtonClick());

        titleL = style(new HorizontalLayout(), "title-L");
        titleL.setSpacing(false);
        if (hasReport) titleL.addComponent(reportButton);
        if (hasUpdate) titleL.addComponents(updateButton, updateLabel);
        titleL.addComponent(titleLabel);

        headL.addComponents(titleL);
    }

    protected void buildHeadParams() {
        paramsL = style(new HorizontalLayout(), "params-L");
        paramsL.setSpacing(false);

        if (!toolbarAlwaysShowed) { //VaadinIcons.ANGLE_DOUBLE_DOWN   FORM  BULLETS COG_O
            paramsButton = style(new Button(VaadinIcons.ANGLE_DOUBLE_DOWN), "small", "params-button");
            paramsButton.setSizeUndefined();
            paramsButton.setEnabled(false);
            paramsButton.setTabIndex(-1);
            paramsButton.setDescription("Изменить параметры отбора данных");
            paramsButton.addClickListener(e -> fireOnParamsButtonClick());

            //Label c = style(new Label("Tooltip content!<div class='xm-arrow xm-arrow-up'></div>", ContentMode.HTML), "xm-tooltip");

            paramsLabel = style(new Label("", ContentMode.HTML), "params-label");
            paramsLabel.setSizeUndefined();

            paramsL.addComponents(paramsButton, paramsLabel);
        }

        headL.addComponent(paramsL);
    }

    /** Построение тулбара, вызывается из билда заголовка (если hasParams=true!), переопределяется потомком. */
    protected void buildHeadToolbar() {
    }

    protected void buildHeadToolbarLayout() {
        toolbarL = new CssLayout();
        Responsive.makeResponsive(toolbarL);
        toolbarL.addStyleName("toolbar-L");
        //headL.addComponent(toolbarL);
    }

    /** Вариант с одной\двумя датами для задания даты\периода. */
    protected void buildHeadToolbarDates() {
        dtwsL = style(new HorizontalLayout(), "dtws-L");
        dtwsL.setMargin(false);

        boolean isp = toolbarPM.hasPeriod();
        dtwField = style(new DateField(isp ? "Начало периода" : "Дата", toolbarPM.dtStart), "dtStart", "small");
        dtwField.addValueChangeListener((e) -> fireOnDateChanged(0));
        dtwField.setPlaceholder(isp ? "Начало" : "Дата");
        dtwField.setDateFormat("dd/MM/yyyy");
        dtStartBind.forField(dtwField)
                .asRequired("Обязательное поле")
                .withValidator(this::validatorDtwStart)
                .bind((v) -> toolbarPM.dtStart, (c, v) -> toolbarPM.dtStart = v);
        dtwsL.addComponent(dtwField);

        if (isp) {
            dtwEndField = style(new DateField("Конец периода", toolbarPM.dtEnd), "dtStart", "small");
            dtwEndField.addValueChangeListener((e) -> fireOnDateChanged(1));
            dtwEndField.setPlaceholder("Конец");
            dtwEndField.setDateFormat("dd/MM/yyyy");
            dtEndBind.forField(dtwEndField)
                    .asRequired("Обязательное поле")
                    .withValidator(this::validatorDtwEnd)
                    .bind((v) -> toolbarPM.dtEnd, (c, v) -> toolbarPM.dtEnd = v);
            dtwsL.addComponent(dtwEndField);
        }

        toolbarL.addComponent(dtwsL);
    }

    protected void buildHeadToolbarButtons() {
        applyButton = style(new Button("Применить"), "small", "process-button");
        applyButton.setDescription("Применить изменения");
        applyButton.setEnabled(false);
        if (hasParams) applyButton.addClickListener(e -> fireOnApplyButtonClick());

        cancelButton = style(new Button("Отменить"), "small", "cancel-button");
        cancelButton.setDescription("Отменить изменения");
        cancelButton.setEnabled(false);
        if (hasParams) cancelButton.addClickListener(e -> fireOnCancelButtonClick());

        buttonsL = style(new HorizontalLayout(), "buttons-L");
        buttonsL.setMargin(false);
        buttonsL.addComponents(applyButton, cancelButton);

        toolbarL.addComponent(buttonsL);
    }

    protected String rightNeg(long val) {
        return (val < 0) ? "v-align-right neg" : "v-align-right";
    }

    protected void buildBody() {
        bodyL = style(new VerticalLayout(), "body-L");
        bodyL.setSpacing(false);
        bodyL.setMargin(false);
        rootL.addComponents(bodyL);

        buildBodyContent();
    }

    /** Построение тела страницы. Переопределяется потомками. */
    protected void buildBodyContent() {
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    /**
     * Успешная валидация. Именно ее надо использовать, а не ValidationResult.ok(), т.к. все проверки делаются именно на
     * это поле!
     */
    protected static final ValidationResult VALIDATION_OK = ValidationResult.ok();

    /** Валидация произвольной даты на вхождение в допустимый период. */
    protected ValidationResult validatorDatesLimits(LocalDate dt) {
        // Ограничение начала периода.
        switch (toolbarPM.datesStartLimit) {
            case NO:
                break;
            case CLIENT:
                if (user.getDtStart() != null && dt.isBefore(user.getDtStart()))
                    return ValidationResult.error("Меньше даты начала учёта по клиенту!");
                break;
            case FIX:
                if (dtFix != null && dt.isBefore(dtFix))
                    return ValidationResult.error("Меньше даты актуальности!");
                break;
            case NOW:
                if (dt.isBefore(LocalDate.now()))
                    return ValidationResult.error("Меньше текущей даты!");
                break;
            case CUSTOM:
                if (datesCustomLimitStart != null && dt.isBefore(datesCustomLimitStart))
                    return ValidationResult.error("Меньше даты ограничения (" + fmtDate8(datesCustomLimitStart) + ")!");
                break;
        }
        // Ограничение конца периода.
        switch (toolbarPM.datesEndLimit) {
            case NO:
                break;
            case CLIENT:
                if (user.getDtStart() != null && dt.isAfter(user.getDtStart()))
                    return ValidationResult.error("Больше даты начала учёта по клиенту (" + fmtDate8(user.getDtStart()) + ")!");
                break;
            case FIX:
                if (dtFix != null && dt.isAfter(dtFix))
                    return ValidationResult.error("Больше даты актуальности (" + fmtDate8(dtFix) + ")!");
                break;
            case NOW:
                if (dt.isAfter(LocalDate.now()))
                    return ValidationResult.error("Больше текущей даты (" + fmtDate8(LocalDate.now()) + ")!");
                break;
            case CUSTOM:
                if (datesCustomLimitEnd != null && dt.isBefore(datesCustomLimitEnd))
                    return ValidationResult.error("Больше даты ограничения (" + fmtDate8(datesCustomLimitEnd) + ")!");
                break;
        }
        return VALIDATION_OK;
    }

    /** Валидация даты как начала периода (или просто даты, если не используется период). */
    protected ValidationResult validatorDtwStart(LocalDate dt, ValueContext valueContext) {
        ValidationResult res = validatorDatesLimits(dt);
        if (res != VALIDATION_OK) return res;

        if (toolbarPM.hasPeriod() && dtwEndField.getValue() != null) {
            if (dt.compareTo(dtwEndField.getValue()) > 0)
                return ValidationResult.error("Больше даты конца периода!");
            if (!dt.plusMonths(12).isAfter(dtwEndField.getValue()))
                return ValidationResult.error("Период больше 12 месяцев!");
        }
        return VALIDATION_OK;
    }

    /** Валидация даты как конца периода. */
    protected ValidationResult validatorDtwEnd(LocalDate dt, ValueContext valueContext) {
        ValidationResult res = validatorDatesLimits(dt);
        if (res != VALIDATION_OK) return res;

        if (toolbarPM.hasPeriod() && dtwField.getValue() != null) {
            if (dt.compareTo(dtwField.getValue()) < 0)
                return ValidationResult.error("Меньше даты начала периода!");
            if (!dt.minusMonths(12).isBefore(dtwField.getValue()))
                return ValidationResult.error("Период больше 12 месяцев!");
        }
        return VALIDATION_OK;
    }

    /** Валидация данных тулбара (вызываются валидаторы). Дополняется в потомках. */
    protected void validateToolbar() {
        if (toolbarPM.hasDate()) {
            dtStartBind.validate();
            if (toolbarPM.hasPeriod()) dtEndBind.validate();
        }
    }

    /** Проверка состояния валидности данных тулбара (валидаторов нарпимер). Дополняется в потомках. */
    protected boolean isValidToolbar() {
        if (toolbarPM.hasDate()) {
            if (!dtStartBind.isValid()) return false;
            if (toolbarPM.hasPeriod() && !dtEndBind.isValid()) return false;
        }
        return true;
    }

    /** Текущие параметры верны (по умолчанию - да, т.к. начальные). */
    protected boolean paramsValid = true;
    /** Открыт тулбар? */
    protected boolean toolbarShowing = false;
    /** Параметры в тулбаре изменены? */
    protected boolean toolbarParamsChanged = false;

    protected void updateButtonsState(boolean iserror) {
        if (toolbarShowing) {
            // Если тулбар показан.
            if (paramsButton != null) paramsButton.setEnabled(false);
            if (updateButton != null)
                updateButton.setEnabled(paramsValid && toolbarAlwaysShowed && !toolbarParamsChanged);
            if (reportButton != null)
                reportButton.setEnabled(paramsValid && toolbarAlwaysShowed && !toolbarParamsChanged);

            if (iserror) {
                if (applyButton != null) applyButton.setEnabled(false);
                if (cancelButton != null) cancelButton.setEnabled(true);
            } else {
                if (applyButton != null) applyButton.setEnabled(toolbarParamsChanged);
                if (cancelButton != null) cancelButton.setEnabled(!toolbarAlwaysShowed || toolbarParamsChanged);
            }
        } else {
            // Если тулбар спрятан.
            if (paramsButton != null) paramsButton.setEnabled(hasParams);

            if (hasUpdate && updateButton != null) updateButton.setEnabled(paramsValid);
            // Только при успешном чтении даты фиксации! Т.к. проверка дат, чтобы были не старше!
            if (hasReport && reportButton != null) reportButton.setEnabled(paramsValid);
        }
    }

    // Инициализация параметров тулбара. В потомках переопределяется и\или дополняется.
    protected void initToolbar() {
        toolbarPM.from(curPM); // из основной модели в модель тулбара.
        if (toolbarPM.hasDate()) {
            dtwField.setValue(toolbarPM.dtStart);
            if (toolbarPM.hasPeriod()) dtwEndField.setValue(toolbarPM.dtEnd);
        }
    }

    protected void showToolbar(boolean state) {
        if (state) {
            if (!toolbarShowing) {
                initToolbar(); // При каждом показе - инициализируется из модели.
                if (paramsLabel != null) paramsL.removeComponent(paramsLabel);
                paramsL.addComponent(toolbarL);

                paramsValid = true;
                toolbarShowing = true;
                toolbarParamsChanged = false;
                updateButtonsState(false);
            }
        } else {
            if (toolbarShowing) {
                paramsL.removeComponent(toolbarL);
                if (paramsLabel != null) paramsL.addComponent(paramsLabel);

                paramsValid = true;
                toolbarShowing = false;
                toolbarParamsChanged = false;
                updateButtonsState(false);
            }
        }
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    protected void fireOnUpdateButtonClick() {
        updateData();
    }

    protected void fireOnReportButtonClick() {
        // Реализация в потомках.
    }

    protected void fireOnParamsButtonClick() {
        // Показываем тулбар.
        showToolbar(true);
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    /**
     * Переменная для пропуска одной обработки клика на root раскладке. Введено для того, чтобы окно, открытое при
     * двойном клике на каком-либо элементе раскладки не закрылось из-за постфикстного срабатывания этой ф-ции.
     */
    protected boolean isRootLayoutClickSkip = false;

    protected void fireOnLayoutClick(LayoutEvents.LayoutClickEvent e) {
        if (!isRootLayoutClickSkip) {
            getUI().closeOpenedWindows();
            isRootLayoutClickSkip = false;
        }
    }

    /** Вызывается при обновлении DtFix (значение должно измениться). */
    protected void fireOnDtFixUpdated(LocalDate dtold, LocalDate dtnew) {
    }

    /** Обновление даты фиксации (безопасное). */
    protected boolean loadDtFixSafe() {
        try {
            LocalDate old = dtFix;
            dtFix = model.getFixDate();
            if (CommonTools.compareNullForward(old, dtFix) != 0) fireOnDtFixUpdated(old, dtFix);
            return true;
        } catch (Exception ignore) {
            return false;
        }
    }

    protected void fireOnDateChanged(int id) {
        if (toolbarPM.hasDate() && toolbarPM.isDtFixUsing()) loadDtFixSafe();

        // Проверка валидаторов.
        Binder<BaseUnitView> b1 = id == 0 ? dtStartBind : dtEndBind;
        boolean iserr = false;
        try {
            b1.writeBean(this);
        } catch (ValidationException ex) {
            iserr = true;
        }
        if (toolbarPM.hasPeriod()) {
            Binder<BaseUnitView> b2 = id == 0 ? dtEndBind : dtStartBind;
            try {
                b2.validate();
                b2.writeBean(this);
            } catch (ValidationException ex) {
                iserr = true;
            }
        }

        toolbarParamsChanged = true;
        updateButtonsState(iserr);
    }

    protected void fireOnApplyButtonClick() {
        validateToolbar();
        if (isValidToolbar()) {
            paramsValid = true;
            curPM.from(toolbarPM); // Сохраняем параметры.
            updateData();
            if (!toolbarAlwaysShowed) {
                showToolbar(false);
            } else {
                toolbarParamsChanged = false;
                updateButtonsState(false);
            }
        } else {
            paramsValid = false;
            toolbarParamsChanged = false;
            updateButtonsState(true);
        }
    }

    protected void fireOnCancelButtonClick() {
        initToolbar();
        paramsValid = true;
        if (!toolbarAlwaysShowed) {
            showToolbar(false);
        } else {
            toolbarParamsChanged = false;
            updateButtonsState(false);
        }
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    protected void updateUpdateLabel() {
        updateLabel.setValue(fmtDT86(LocalDateTime.now()));
    }

    protected void updateData() {
        updateUpdateLabel();
        model.logAction(getLogPage(), AppModel.LogActionType.LOADDATA);
    }

    protected AppModel.LogActionPage getLogPage() {
        throw new NotImplementedException();
    }

    protected <T, V> Grid.Column<T, V> column(Grid<T> g, ValueProvider<T, V> prov, ValueProvider<V, String> pres,
                                              String id, String title, StyleGenerator<T> style, int wfix, StyleGenerator<T> gen) {
        Grid.Column<T, V> c = gridColumn(g, prov, pres, id, title, style);
        if (wfix >= 0) {
            c.setWidth(wfix);        // Начальный размер (может сбрасываться (-1) или меняться).
            c.setMinimumWidth(wfix); // Фиксация размера!
            c.setMaximumWidth(wfix); // Фиксация размера!
            c.setExpandRatio(0);  // Коэффициент (вес) для растяжения.
            c.setResizable(false);
        } else {
            c.setWidth(-wfix);        // Начальный размер (может сбрасываться (-1) или меняться).
            c.setMinimumWidth(-wfix); // Минимальный размер, максимальный не ограничиваем!
            c.setMaximumWidth(5000);
            c.setExpandRatio(-wfix);  // Коэффициент (вес) для растяжения.
            c.setResizable(true);
        }
        if (gen != null) c.setStyleGenerator(gen);
        return c;
    }

    protected void addSizeReporterForExpandGridColumns(Grid g) {
        SizeReporter rep = new SizeReporter(g);
        rep.addResizeListener((e) -> updateGridColumns(g, rep));
    }

    protected boolean checkForStartAndFix(BaseParamsModel pm) {
        if (pm.hasDate() && pm.isDtFixUsing()) loadDtFixSafe();

        ValidationResult res = validatorDatesLimits(pm.dtStart);
        if (res != VALIDATION_OK) {
            showError("Некорректная дата " + (pm.hasPeriod() ? "начала периода " : "") + "(" + fmtDate8(pm.dtStart) + "):<br>"
                    + res.getErrorMessage());
            return false;
        }
        if (pm.hasPeriod()) {
            res = validatorDatesLimits(pm.dtEnd);
            if (res != VALIDATION_OK) {
                showError("Некорректная дата конца периода (" + fmtDate8(pm.dtStart) + "):<br>"
                        + res.getErrorMessage());
                return false;
            }
            if (pm.dtEnd.isBefore(pm.dtStart)) {
                showError("Дата начала периода (" + fmtDate8(pm.dtStart) + ") больше даты конца периода (" + fmtDate8(pm.dtEnd) + ")!");
                return false;
            }
            if (!pm.dtStart.plusMonths(12).isAfter(pm.dtEnd)) {
                showError("Период (" + fmtDate8(pm.dtStart) + " - " + fmtDate8(pm.dtEnd) + ") больше 12 месяцев!");
                return false;
            }
        }
        return true;
    }

    protected MessageDialog showInfo(String title, String message) {
        return MessageDialog.showInfo(getUI(), title, message);
    }

    protected MessageDialog showError(String title, String message) {
        return MessageDialog.showError(getUI(), title, message);
    }

    protected MessageDialog showError(String message) {
        return MessageDialog.showError(getUI(), "Ошибка", message);
    }

    /**
     * Базовая модель параметров для формирования данных. Реализует период отбора (или только одну дату) - это
     * настраивается в конструкторе. Если нужны еще параметры отбора - можно добавить в потомках (потомок указывается в
     * качестве параметра - для безгеморной перезаписи метода to.
     */
    public static class BaseParamsModel<T extends BaseParamsModel> {
        public enum DateLimitEnum {
            NO, NOW, FIX, CLIENT, CUSTOM
        }

        public enum DatesModeEnum {
            NO, DATE, PERIOD
        }

        public DatesModeEnum datesMode;
        public DateLimitEnum datesStartLimit, datesEndLimit; // Тип ограничения дат.
        public LocalDate dtStart, dtEnd;

        public BaseParamsModel(DateLimitEnum datesStartLimit, DateLimitEnum datesEndLimit, DatesModeEnum datesMode, LocalDate dtStart, LocalDate dtEnd) {
            this.datesMode = datesMode;
            this.datesStartLimit = datesStartLimit;
            this.datesEndLimit = datesEndLimit;
            this.dtStart = dtStart;
            this.dtEnd = dtEnd;
        }

        public void to(T dst) {
            if (dst != null) {
                dst.datesMode = datesMode;
                dst.datesStartLimit = datesStartLimit;
                dst.datesEndLimit = datesEndLimit;
                dst.dtStart = dtStart;
                dst.dtEnd = dtEnd;
            }
        }

        public final void from(T src) {
            if (src != null) src.to(this);
        }

        /** Возвращает true если имеется дата (подразумевается дата старта, т.е. в случае режимов дата или период). */
        public final boolean hasDate() {
            return (datesMode == DatesModeEnum.DATE || datesMode == DatesModeEnum.PERIOD);
        }

        /** Возвращает true если имеется период (дата старта и дата завершения). */
        public final boolean hasPeriod() {
            return datesMode == DatesModeEnum.PERIOD;
        }

        public final boolean isDtFixUsing() {
            return hasDate() && (datesStartLimit == DateLimitEnum.FIX || datesEndLimit == DateLimitEnum.FIX);
        }
    }

    /**
     * Сервис предоставления данных для больших таблиц (извлечение данных порционное, только той части, что нужна для
     * отображения плюс небольшой запас).
     */
    protected abstract class BaseDataService<T> {

        protected final Grid<T> grid;
        protected final DataProvider<T, Void> dataProvider;

        protected abstract void setup();

        protected abstract ArrayList<T> load(int offset, int limit, String sort) throws ExError;

        protected abstract int count() throws ExError;

        protected void refresh() {
            setup();
            grid.setDataProvider(dataProvider);
            grid.recalculateColumnWidths();
        }

        protected BaseDataService(Grid<T> grid) {
            this.grid = grid;
            this.dataProvider = DataProvider.fromCallbacks(
                    q -> fetch(grid, q.getOffset(), q.getLimit(), q.getSortOrders()).stream(),
                    q -> count(grid)
            );
        }

        public ArrayList<T> fetch(AbstractComponent errcomp, int offset, int limit, List<QuerySortOrder> order) {
            updateUpdateLabel();
            if (errcomp != null && errcomp.getComponentError() != null) errcomp.setComponentError(null);
            try {
                StringBuilder sb = new StringBuilder();
                boolean is = false;
                for (QuerySortOrder so : order) {
                    if (is) sb.append(", ");
                    sb.append(so.getSorted());
                    if (so.getDirection() == SortDirection.DESCENDING) sb.append(" DESC");
                    is = true;
                }
                return load(offset, limit, sb.toString());
            } catch (ExError ex) {
                if (errcomp != null) errcomp.setComponentError(new UserError(ex.getMessage()));
                return new ArrayList<>();
            }
        }

        public int count(AbstractComponent errcomp) {
            updateUpdateLabel();
            if (errcomp != null && errcomp.getComponentError() != null) errcomp.setComponentError(null);
            try {
                return count();
            } catch (ExError ex) {
                if (errcomp != null) errcomp.setComponentError(new UserError(ex.getMessage()));
                return 0;
            }
        }
    }
}
