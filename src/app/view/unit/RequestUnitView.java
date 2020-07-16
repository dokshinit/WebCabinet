package app.view.unit;

import app.ExError;
import app.dialog.RequestDialog;
import app.model.*;
import com.vaadin.ui.*;

import java.time.LocalDate;
import java.util.ArrayList;

import static app.view.unit.UHelper.*;
import static app.model.Helper.*;

/**
 * @author Aleksey Dokshin <dant.it@gmail.com> (13.12.17).
 */
public class RequestUnitView extends BaseUnitView<RequestUnitView.PM> {

    class PM extends BaseUnitView.BaseParamsModel<PM> {
        public Request.Type reqType;
        public Request.FilterMode reqMode;

        PM() {
            super(BaseParamsModel.DateLimitEnum.CLIENT, BaseParamsModel.DateLimitEnum.NOW,
                    BaseParamsModel.DatesModeEnum.PERIOD, null, null);
            reqType = null;
            reqMode = null;
        }

        @Override
        public void to(PM dst) {
            if (dst != null) {
                super.to(dst);
                dst.reqType = reqType;
                dst.reqMode = reqMode;
            }
        }
    }

    private ComboBox<Request.Type> reqTypeCombo;
    private ComboBox<Request.FilterMode> reqModeCombo;
    private Grid<Request> grid;
    private SingleSelect<Request> singleSelect;
    private RequestDataService dataService;


    public RequestUnitView() {
        super("request", "Заявки");
    }

    @Override
    protected void initVars() {
        hasParams = true;
        hasUpdate = true;
        hasReport = false;
        toolbarAlwaysShowed = true;

        curPM = new PM();
        toolbarPM = new PM();

        curPM.reqType = null;
        curPM.reqMode = null;
        curPM.dtEnd = LocalDate.now();
        curPM.dtStart = curPM.dtEnd.minusMonths(1);
    }

    @Override
    protected void buildHeadToolbar() {
        buildHeadToolbarLayout();
        buildTypeAndModeCombos();
        buildHeadToolbarDates();
        buildHeadToolbarButtons();
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
        reqModeCombo.setItemCaptionGenerator(Request.FilterMode::getTitle);
        reqModeCombo.addValueChangeListener((e) -> fireOnModeChanged(e.getValue()));
        reqModeCombo.setItems(Request.FilterMode.values());
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
        toolbarPM.reqType = type;
        toolbarParamsChanged = true;
        updateButtonsState(!isValidToolbar());
    }

    private void fireOnModeChanged(Request.FilterMode mode) {
        toolbarPM.reqMode = mode;
        toolbarParamsChanged = true;
        updateButtonsState(!isValidToolbar());
    }

    @Override
    protected void initToolbar() {
        super.initToolbar();
        reqTypeCombo.setSelectedItem(curPM.reqType);
        reqModeCombo.setSelectedItem(curPM.reqMode);
    }

    @Override
    protected AppModel.LogActionPage getLogPage() {
        return AppModel.LogActionPage.TRANS;
    }

    @Override
    protected void updateData() {
        updateUpdateLabel();
        dataService.refresh();
    }

    private class RequestDataService extends BaseGridDataService<Request> {

        private LocalDate dtstart, dtend;
        private Request.Type type;
        private Request.FilterMode mode;

        public RequestDataService(Grid<Request> grid) {
            super(grid);
        }

        @Override
        public void setup() {
            this.dtstart = curPM.dtStart;
            this.dtend = curPM.dtEnd;
            this.type = curPM.reqType;
            this.mode = curPM.reqMode;
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
