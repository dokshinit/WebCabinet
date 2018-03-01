package app.view.unit;

import app.AppUI;
import app.ExError;
import app.dialog.MessageDialog;
import app.model.*;
import app.sizer.SizeReporter;
import app.view.AbstractUnitView;
import com.google.gwt.validation.client.impl.Validation;
import com.vaadin.data.*;
import com.vaadin.data.provider.DataProvider;
import com.vaadin.data.provider.QuerySortOrder;
import com.vaadin.event.LayoutEvents;
import com.vaadin.server.Responsive;
import com.vaadin.server.UserError;
import com.vaadin.shared.data.sort.SortDirection;
import com.vaadin.ui.*;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static app.AppServlet.logger;
import static app.view.unit.Helper.*;

/**
 * @author Aleksey Dokshin <dant.it@gmail.com> (13.12.17).
 */
public abstract class BaseUnitView extends Panel implements AbstractUnitView {

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
    protected Label title, titleUpdateInfo;
    protected Button updateButton;

    protected CssLayout toolbarL;
    protected HorizontalLayout dtwsL, combosL, buttonsL;
    protected DateField dtwField, dtwEndField;
    protected Button filterButton, reportButton;

    protected VerticalLayout bodyL;

    private SizeReporter gridSizeReporter;

    protected LocalDate dtFix;
    protected LocalDate dtStart, dtEnd;

    public BaseUnitView(String name, String title) {
        model = AppUI.model();
        user = model.getUser();
        dtFix = null;
        dtStart = dtEnd = null;

        initVars();
        buildUI(name, title);

        // Тело.

        rootL.addLayoutClickListener(this::fireOnLayoutClick);
        updateButton.addClickListener(e -> fireOnUpdateClick());

        updateData();
        updateButtonsState(false);
    }


    protected void initVars() {
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
    }

    protected void buildHead(String title) {
        headL = style(new VerticalLayout(), "head-L");
        headL.setSpacing(false);
        rootL.addComponent(headL);

        buildHeadTitle(title);
        buildHeadToolbar();
    }

    protected void buildHeadTitle(String title) {
        titleL = style(new HorizontalLayout(), "title-L");
        Label titleLabel = style(new Label(title), "title");
        titleLabel.setSizeUndefined();
        titleUpdateInfo = style(new Label(), "info");
        titleUpdateInfo.setSizeUndefined();
        updateButton = style(new Button("Обновить"), "small", "update");
        updateButton.setSizeUndefined();
        titleL.addComponents(updateButton, titleLabel, titleUpdateInfo);
        headL.addComponents(titleL);
    }

    protected void buildHeadToolbar() {
    }

    protected ValidationResult validatorDtwStart(LocalDate dt, ValueContext valueContext) {
        if (user.getDtStart() != null && dt.isBefore(user.getDtStart()))
            return ValidationResult.error("Меньше даты начала учёта по клиенту!");
        if (dtFix != null && dt.isAfter(dtFix))
            return ValidationResult.error("Больше даты актуальности!");

        if (dtwEndField.getValue() != null) {
            if (dt.compareTo(dtwEndField.getValue()) > 0)
                return ValidationResult.error("Больше даты конца периода!");
            if (!dt.plusMonths(3).isAfter(dtwEndField.getValue()))
                return ValidationResult.error("Период больше трёх месяцев!");
        }
        return ValidationResult.ok();
    }

    protected ValidationResult validatorDtwEnd(LocalDate dt, ValueContext valueContext) {
        if (user.getDtStart() != null && dt.isBefore(user.getDtStart()))
            return ValidationResult.error("Меньше даты начала учёта по клиенту!");
        if (dtFix != null && dt.isAfter(dtFix))
            return ValidationResult.error("Больше даты актуальности!");

        if (dtwField.getValue() != null) {
            if (dt.compareTo(dtwField.getValue()) < 0)
                return ValidationResult.error("Меньше даты начала периода!");
            if (!dt.minusMonths(3).isBefore(dtwField.getValue()))
                return ValidationResult.error("Период больше трёх месяцев!");
        }
        return ValidationResult.ok();
    }

    protected void buildHeadToolbarDtwsForPeriod() {
        dtwField = style(new DateField("Начало периода", dtStart), "dtStart", "small");
        dtwField.addValueChangeListener((e) -> fireOnDateChanged(0));
        dtwField.setPlaceholder("Начало");
        dtwField.setDateFormat("dd/MM/yyyy");
        dtStartBind.forField(dtwField)
                .asRequired("Обязательное поле")
                .withValidator(this::validatorDtwStart)
                .bind((v) -> dtStart, (c, v) -> dtStart = v);

        dtwEndField = style(new DateField("Конец периода", dtEnd), "dtStart", "small");
        dtwEndField.addValueChangeListener((e) -> fireOnDateChanged(1));
        dtwEndField.setPlaceholder("Конец");
        dtwEndField.setDateFormat("dd/MM/yyyy");
        dtEndBind.forField(dtwEndField)
                .asRequired("Обязательное поле")
                .withValidator(this::validatorDtwEnd)
                .bind((v) -> dtEnd, (c, v) -> dtEnd = v);

        dtwsL = style(new HorizontalLayout(dtwField, dtwEndField), "dtws-L");
        dtwsL.setMargin(false);

        toolbarL.addComponent(dtwsL);
    }

    protected void buildHeadToolbarDtwForDate() {
        dtwField = style(new DateField("Дата", dtStart), "dtStart", "small");
        dtwField.addValueChangeListener((e) -> fireOnDateChanged(0));
        dtwField.setPlaceholder("Дата");
        dtwField.setDateFormat("dd/MM/yyyy");
        dtStartBind.forField(dtwField)
                .asRequired("Обязательное поле")
                .withValidator(this::validatorDtwStart)
                .bind((v) -> dtStart, (c, v) -> dtStart = v);

        dtwsL = style(new HorizontalLayout(dtwField), "dtws-L");
        dtwsL.setMargin(false);

        toolbarL.addComponent(dtwsL);
    }

    protected void buildHeadToolbarFilterButton() {
        filterButton = new Button("Применить");
        filterButton.addStyleName("small");
        filterButton.setDescription("Применить фильтр");
        filterButton.addClickListener(event -> fireOnFilterApply());
        filterButton.setEnabled(false);
        buttonsL.addComponent(filterButton);
    }

    protected void buildHeadToolbarReportButton() {
        reportButton = new Button("Отчёт"); // FILE_TEXT FILE_ZIP NEWSPAPER
        reportButton.addStyleName("small");
        reportButton.setDescription("Сформировать отчёт");
        reportButton.addClickListener(event -> fireOnCreateReport());
        reportButton.setEnabled(false);
        buttonsL.addComponent(reportButton);
    }

    protected void buildHeadTollbarButtonsLayout() {
        buttonsL = new HorizontalLayout();
        buttonsL.addStyleName("buttons-L");
        buttonsL.setMargin(false);
        toolbarL.addComponent(buttonsL);
    }

    protected void buildHeadToolbarLayout() {
        toolbarL = new CssLayout();
        Responsive.makeResponsive(toolbarL);
        toolbarL.addStyleName("toolbar-L");
        headL.addComponent(toolbarL);
    }

    protected void buildHeadToolbarButtons() {
        buildHeadTollbarButtonsLayout();
        buildHeadToolbarFilterButton();
        buildHeadToolbarReportButton();
    }

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

    protected void fireOnUpdateClick() {
        updateData();
    }

    protected Binder<BaseUnitView> dtStartBind = new Binder<>();
    protected Binder<BaseUnitView> dtEndBind = new Binder<>();

    protected void fireOnDateChangedForPeriod(int id) {
        try {
            dtFix = model.getFixDate();
        } catch (Exception ignore) {
        }

        // Проверка валидаторов.
        Binder<BaseUnitView> b1 = id == 0 ? dtStartBind : dtEndBind;
        Binder<BaseUnitView> b2 = id == 0 ? dtEndBind : dtStartBind;
        boolean iserr = false;
        try {
            b1.writeBean(this);
        } catch (ValidationException ex) {
            iserr = true;
        }
        try {
            b2.validate();
            b2.writeBean(this);
        } catch (ValidationException ex) {
            iserr = true;
        }

        toolbarParamsChanged = true;
        updateButtonsState(iserr);
    }

    protected void fireOnDateChangedForDate() {
        try {
            dtFix = model.getFixDate();
        } catch (Exception ignore) {
        }

        // Проверка валидаторов.
        Binder<BaseUnitView> b1 = dtStartBind;
        boolean iserr = false;
        try {
            b1.writeBean(this);
        } catch (ValidationException ex) {
            iserr = true;
        }

        toolbarParamsChanged = true;
        updateButtonsState(iserr);
    }

    protected void fireOnDateChanged(int id) {
        throw new NotImplementedException();
    }

    protected void validate() {
    }

    protected boolean isValid() {
        return true;
    }

    protected boolean toolbarParamsChanged = false;

    protected void updateButtonsState(boolean iserror) {
        if (iserror) {
            if (filterButton != null) filterButton.setEnabled(false);
            if (reportButton != null) reportButton.setEnabled(false);
            if (updateButton != null) updateButton.setEnabled(false);
        } else {
            if (filterButton != null) filterButton.setEnabled(toolbarParamsChanged);
            if (reportButton != null)
                reportButton.setEnabled(!toolbarParamsChanged); // Только при успешном чтении даты фиксации! Т.к. проверка дат, чтобы были не старше!
            if (updateButton != null) updateButton.setEnabled(!toolbarParamsChanged);
        }
    }

    protected void fireOnFilterApply() {
        validate();
        if (isValid()) {
            updateData();
            toolbarParamsChanged = false;
            updateButtonsState(false);
        } else {
            toolbarParamsChanged = false;
            updateButtonsState(true);
        }
    }

    protected void fireOnCreateReport() {
    }

    protected String rightNeg(long val) {
        if (val < 0) return "v-align-right neg";
        return "v-align-right";
    }

    protected void buildBody() {
        bodyL = style(new VerticalLayout(), "body-L");
        bodyL.setSpacing(false);
        bodyL.setMargin(false);
        rootL.addComponents(bodyL);

        buildBodyContent();
    }

    protected void buildBodyContent() {
    }

    protected void updateTitleUpdateInfo() {
        titleUpdateInfo.setValue("(обновлено: " + fmtDT86(LocalDateTime.now()) + ")");
    }

    protected void updateData() {
        updateTitleUpdateInfo();
        model.logAction(getLogPage(), AppModel.LogActionType.LOADDATA);
    }

    protected AppModel.LogActionPage getLogPage() {
        throw new NotImplementedException();
    }

    protected <T, V> Grid.Column<T, V> column(Grid<T> g, ValueProvider<T, V> prov, ValueProvider<V, String> pres,
                                              String id, String title, StyleGenerator<T> style, int wfix, StyleGenerator<T> gen) {
        Grid.Column<T, V> c = gridColumn(g, prov, pres, id, title, style);
        if (wfix >= 0) {
            c.setWidth(wfix);
            c.setMinimumWidth(wfix);
            c.setMaximumWidth(wfix);
            c.setExpandRatio(wfix); // ?
            c.setResizable(false);
        } else {
            c.setWidth(-wfix);
            c.setMinimumWidth(-wfix);
            c.setExpandRatio(-wfix);
        }
        if (gen != null) c.setStyleGenerator(gen);
        return c;
    }

    protected void addSizeReporterForExpandGridColumns(Grid g) {
        SizeReporter rep = new SizeReporter(g);
        rep.addResizeListener((e) -> updateGridColumns(g, rep));
    }

    protected boolean checkForStartAndFix(LocalDate dtstart, LocalDate dtend) {
        try {
            dtFix = model.getFixDate();
        } catch (Exception ignore) {
        }

        if (dtstart.isBefore(user.getDtStart())) {
            showError("Дата " + (dtend != null ? "(" : "начала периода (") + fmtDate8(dtstart) + ") "
                    + "меньше даты начала учёта по клиенту (" + fmtDate8(user.getDtStart()) + ")!<br>"
                    + "Даты отчёта не могут быть меньше даты начала учёта по клиенту!");
            return false;
        }
        if (dtstart.isAfter(dtFix)) {
            showError("Дата " + (dtend != null ? "(" : "начала периода (") + fmtDate8(dtstart) + ") "
                    + "больше даты актуальности данных (" + fmtDate8(dtFix) + ")!<br>"
                    + "Даты отчёта не могут быть больше даты актуальности данных!");
            return false;
        }
        if (dtend != null) {
            if (dtend.isBefore(dtstart)) {
                showError("Дата начала периода (" + fmtDate8(dtstart) + ") больше даты конца периода (" + fmtDate8(dtend) + ")!");
                return false;
            }
            if (dtend.isBefore(user.getDtStart())) {
                showError("Дата конца периода (" + fmtDate8(dtend) + ") меньше даты начала учёта по клиенту (" + fmtDate8(user.getDtStart()) + ")!<br>"
                        + "Даты отчёта не могут быть меньше даты начала учёта по клиенту!");
                return false;
            }
            if (dtend.isAfter(dtFix)) {
                showError("Дата конца периода (" + fmtDate8(dtend)
                        + ") больше даты актуальности данных (" + fmtDate8(dtFix) + ")!<br>"
                        + "Даты отчёта не могут быть больше даты актуальности данных!");
                return false;
            }
            if (!dtstart.plusMonths(3).isAfter(dtend)) {
                showError("Период (" + fmtDate8(dtstart) + " - " + fmtDate8(dtend) + ") больше трёх месяцев!");
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
                    q -> fetch(filterButton, q.getOffset(), q.getLimit(), q.getSortOrders()).stream(),
                    q -> count(filterButton)
            );
        }

        public ArrayList<T> fetch(AbstractComponent errcomp, int offset, int limit, List<QuerySortOrder> order) {
            updateTitleUpdateInfo();
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
            updateTitleUpdateInfo();
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
