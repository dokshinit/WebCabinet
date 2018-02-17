package app.view.unit;

import app.ExError;
import app.dialog.RequestDialog;
import app.model.*;
import com.vaadin.ui.*;

import java.time.LocalDate;
import java.util.ArrayList;

import static app.view.unit.Helper.*;

/**
 * @author Aleksey Dokshin <dant.it@gmail.com> (13.12.17).
 */
public class RequestUnitView extends BaseUnitView {

    private ComboBox<Request.Type> reqTypeCombo;
    private ComboBox<Request.Mode> reqModeCombo;
    private Grid<Request> grid;
    private SingleSelect<Request> singleSelect;
    private RequestDataService dataService;

    private Request.Type reqType;
    private Request.Mode reqMode;

    public RequestUnitView() {
        super("request", "Заявки");
    }

    @Override
    protected void initVars() {
        super.initVars();

        reqType = null;
        reqMode = null;
        dtStart = LocalDate.now().minusDays(30);
        dtEnd = LocalDate.now();
    }

    @Override
    protected void buildHeadToolbar() {
        buildHeadToolbarLayout();
        buildTypeAndModeCombos();
        buildHeadToolbarDtwsForPeriod();
        buildHeadToolbarButtons();
    }

    @Override
    protected void buildHeadToolbarButtons() {
        buildHeadTollbarButtonsLayout();
        buildHeadToolbarFilterButton();
    }

    private void buildTypeAndModeCombos() {
        reqTypeCombo = style(new ComboBox<>("Тип"), "small");
        reqTypeCombo.setTextInputAllowed(false);
        reqTypeCombo.setEmptySelectionAllowed(true);
        reqTypeCombo.setEmptySelectionCaption("< Все >");
        reqTypeCombo.setItemCaptionGenerator(Request.Type::getTitle);
        reqTypeCombo.addValueChangeListener((e) -> fireOnTypeChanged(e.getValue()));
        reqTypeCombo.setItems(Request.Type.values());
        reqTypeCombo.setWidth("180px");

        reqModeCombo = style(new ComboBox<>("Состояние"), "small");
        reqModeCombo.setTextInputAllowed(false);
        reqModeCombo.setEmptySelectionAllowed(true);
        reqModeCombo.setEmptySelectionCaption("< Все >");
        reqModeCombo.setItemCaptionGenerator(Request.Mode::getTitle);
        reqModeCombo.addValueChangeListener((e) -> fireOnModeChanged(e.getValue()));
        reqModeCombo.setItems(Request.Mode.values());
        reqModeCombo.setWidth("140px");

        HorizontalLayout typeL = new HorizontalLayout(reqTypeCombo, reqModeCombo);
        typeL.setMargin(false);

        toolbarL.addComponent(typeL);
    }

    @SuppressWarnings("unchecked")
    @Override
    protected void buildBodyContent() {
        grid = style(new Grid<>(), "request-table");
        grid.setSizeFull();

        column(grid, Request::getState, v -> v.title, "ISTATE", "Состояние", ST_AL_CENTER, 100, null);
        column(grid, Request::getDtCreate, v -> fmtDT86(v), "DTCREATE", "Время создания", ST_AL_LEFT, 150, null);
        column(grid, Request::getType, v -> v.getTitle(), "ITYPE", "Тип", ST_AL_LEFT, 150, null);
        column(grid, Request::getFullTitle, null, "ITYPE,CPARAMSTITLE", "Содержание", ST_AL_LEFT, -150, null);
        column(grid, Request::getFullComment, null, "CCOMMENT,CRESULT", "Примечание [причина]", ST_AL_LEFT, -150, null);
        grid.setSelectionMode(Grid.SelectionMode.SINGLE);
        grid.setColumnReorderingAllowed(true);
        setRowHeight(grid);
        grid.recalculateColumnWidths();
        singleSelect = grid.asSingleSelect();

        grid.addItemClickListener(this::fireOnRequestSelected);
        singleSelect.addValueChangeListener(e -> {
            if (e.getOldValue() != null && e.getValue() == null) singleSelect.setValue(e.getOldValue());
        });

        dataService = new RequestDataService(grid);
        addSizeReporterForExpandGridColumns(grid);

        bodyL.addComponent(grid);
        rootL.setSizeFull();
        rootL.setExpandRatio(bodyL, 1);
        bodyL.setSizeFull();
        bodyL.setExpandRatio(grid, 1);
    }

    private void fireOnRequestSelected(Grid.ItemClick<Request> e) {
        if (e.getMouseEventDetails().isDoubleClick()) {
            isRootLayoutClickSkip = true;
            RequestDialog dlg = new RequestDialog(e.getItem());
            getUI().addWindow(dlg);
        }
    }

    private void fireOnTypeChanged(Request.Type type) {
        reqType = type;
        toolbarParamsChanged = true;
        updateButtonsState(!isValid());
    }

    private void fireOnModeChanged(Request.Mode mode) {
        reqMode = mode;
        toolbarParamsChanged = true;
        updateButtonsState(!isValid());
    }

    @Override
    protected void fireOnDateChanged(int id) {
        fireOnDateChangedForPeriod(id);
    }

    @Override
    protected void validate() {
        dtStartBind.validate();
        dtEndBind.validate();
    }

    @Override
    protected boolean isValid() {
        return dtStartBind.isValid() && dtEndBind.isValid();
    }

    @Override
    protected AppModel.LogActionPage getLogPage() {
        return AppModel.LogActionPage.TRANS;
    }

    @Override
    protected void updateData() {
        updateTitleUpdateInfo();
        dataService.refresh();
    }

    private class RequestDataService extends BaseDataService<Request> {

        private LocalDate dtstart, dtend;
        private Request.Type type;
        private Request.Mode mode;

        public RequestDataService(Grid<Request> grid) {
            super(grid);
        }

        @Override
        public void setup() {
            this.dtstart = RequestUnitView.this.dtStart;
            this.dtend = RequestUnitView.this.dtEnd;
            this.type = RequestUnitView.this.reqType;
            this.mode = RequestUnitView.this.reqMode;
        }

        @Override
        protected ArrayList<Request> load(int offset, int limit, String sort) throws ExError {
            return model.loadRequests(dtstart, dtend, type, mode, offset, limit, sort);
        }

        @Override
        protected int count() throws ExError {
            return model.loadRequestsCount(dtstart, dtend, type, mode);
        }
    }
}
