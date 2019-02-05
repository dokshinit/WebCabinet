package app.view.unit;

import app.ExError;
import app.dialog.RequestDialog;
import app.model.*;
import com.vaadin.data.provider.DataProvider;
import com.vaadin.data.provider.GridSortOrderBuilder;
import com.vaadin.ui.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;

import static app.model.Helper.*;
import static app.view.unit.UHelper.*;

/**
 * @author Aleksey Dokshin <dant.it@gmail.com> (13.12.17).
 */
public class CardUnitView extends BaseUnitView<CardUnitView.PM> {

    class PM extends BaseUnitView.BaseParamsModel<PM> {
        public Card.WorkState workState;

        PM() {
            super(BaseParamsModel.DateLimitEnum.CLIENT, BaseParamsModel.DateLimitEnum.FIX,
                    BaseParamsModel.DatesModeEnum.DATE, null, null);
            ;
            workState = null;
        }

        @Override
        public void to(PM dst) {
            if (dst != null) {
                super.to(dst);
                dst.workState = workState;
            }
        }
    }

    private Grid<Card> grid;
    private SingleSelect<Card> singleSelect;
    private CardDataService dataService;

    private ComboBox<Card.WorkState> workCombo;

    public CardUnitView() {
        super("card", "Топливные карты");
    }

    @Override
    protected void initVars() {
        hasParams = true;
        hasUpdate = true;
        hasReport = true;

        curPM = new PM();
        toolbarPM = new PM();

        curPM.workState = Card.WorkState.WORK;
        try {
            dtFix = model.getFixDate();
            curPM.dtStart = dtFix.withDayOfMonth(1);
        } catch (Exception ex) {
            curPM.dtStart = LocalDate.now().withDayOfMonth(1);
        }
    }

    @Override
    protected void buildHeadToolbar() {
        buildHeadToolbarLayout();
        buildWorkStateCombo();
        buildHeadToolbarDates();
        buildHeadToolbarButtons();
    }

    private void buildWorkStateCombo() {
        workCombo = style(new ComboBox<>("Состояние"), "small");
        workCombo.setTextInputAllowed(false);
        workCombo.setEmptySelectionAllowed(true);
        workCombo.setEmptySelectionCaption("< Все >");
        workCombo.setItemCaptionGenerator(Card.WorkState::getTitleForMany);
        workCombo.setDataProvider(DataProvider.ofItems(Card.WorkState.values()));
        workCombo.setSelectedItem(curPM.workState);
        workCombo.addValueChangeListener((e) -> fireOnWorkStateChanged(e.getValue()));
        workCombo.setWidth("140px");

        HorizontalLayout comL = new HorizontalLayout(workCombo);
        comL.setMargin(false);

        toolbarL.addComponent(comL);
    }

    @SuppressWarnings("unchecked")
    @Override
    protected void buildBodyContent() {
        grid = style(new Grid<>(), "card-table");
        grid.setSizeFull();
        Grid.Column<Card, String> c1;
        Grid.Column<Card, Card.WorkState> c2;
        column(grid, Card::getDtw, Helper::fmtDate8, "DTW", "Изменена", ST_AL_CENTER, -90, null).setHidable(true);
        //column(grid, Card::getDtwEnd, UHelper::fmtDate8, "DTWEND", "Завершение", ST_AL_CENTER, -90, null).setHidable(true).setHidden(true);
        c1 = column(grid, Card::getIddCard, null, "IDD", "№", ST_AL_CENTER, -90, null);
        column(grid, Card::getAccType, AccType::getAbbreviation, "IACCTYPE", "Тип", ST_AL_CENTER, -50, null).setHidable(true);
        c2 = column(grid, Card::getWorkState, Card.WorkState::getTitle, "IBWORK", "Состояние", ST_AL_CENTER, -90, null).setHidable(true);
        column(grid, Card::getDtPay, Helper::fmtDate8, "DTPAY", "Покупка", ST_AL_CENTER, -90, null).setHidable(true).setHidden(true);
        column(grid, Card::getDriver, null, "CDRIVER", "Водитель", ST_AL_LEFT, -100, null);
        column(grid, Card::getCar, null, "CCAR", "Автомобиль", ST_AL_LEFT, -100, null);
        column(grid, Card::getDbDayLimit, v -> v == 0 ? "---" : fmtN2(v), "DBDAYLIMIT", "Лимит", ST_AL_RIGHT, -60, null).setHidable(true);
        column(grid, Card::getComment, null, "CCOMMENT", "Информация", ST_AL_LEFT, -100, null).setHidable(true);
        grid.setSelectionMode(Grid.SelectionMode.SINGLE);
        grid.setColumnReorderingAllowed(true);
        setRowHeight(grid);
        grid.recalculateColumnWidths();
        singleSelect = grid.asSingleSelect();

        grid.addItemClickListener(this::fireOnCardSelected);
        singleSelect.addValueChangeListener(e -> {
            if (e.getOldValue() != null && e.getValue() == null) singleSelect.setValue(e.getOldValue());
        });

        dataService = new CardDataService(grid);
        grid.setSortOrder(new GridSortOrderBuilder().thenDesc(c2).thenAsc(c1));
        addSizeReporterForExpandGridColumns(grid);

        bodyL.addComponent(grid);
        rootL.setSizeFull();
        rootL.setExpandRatio(bodyL, 1);
        bodyL.setSizeFull();
        bodyL.setExpandRatio(grid, 1);
    }

    private void fireOnCardSelected(Grid.ItemClick<Card> e) {
        if (e.getMouseEventDetails().isDoubleClick()) {
        }
    }

    private void fireOnWorkStateChanged(Card.WorkState ws) {
        toolbarPM.workState = ws;
        toolbarParamsChanged = true;
        updateButtonsState(!isValidToolbar());
    }

    @Override
    protected void fireOnReportButtonClick() {
        if (!checkForStartAndFix(curPM)) return;

        String s1;
        RequestBase.ParamsMap params = new RequestBase.ParamsMap()
                .put("dtw", s1 = fmtDate8(curPM.dtStart))
                .put("idWorkState", curPM.workState == null ? "" : "" + curPM.workState.id);
        //
        Request r = Request.newReport(Request.ReportType.CARD,
                "на " + s1 + (curPM.workState == null ? "" : " (" + curPM.workState.titleForMany + ")"), params);
        RequestDialog dlg = new RequestDialog(r);
        getUI().addWindow(dlg);
    }

    @Override
    protected void initToolbar() {
        super.initToolbar();
        workCombo.setSelectedItem(curPM.workState);
    }

    @Override
    protected AppModel.LogActionPage getLogPage() {
        return AppModel.LogActionPage.CARD;
    }

    @Override
    protected void updateData() {
        super.updateData();

        String s = "на <b>" + fmtDate8(curPM.dtStart) + "</b>";
        if (curPM.workState != null) {
            s += ", только <b>" + curPM.workState.titleForMany + "</b>";
        }
        paramsLabel.setValue(s);

        dataService.refresh();
    }

    private class CardDataService extends BaseDataService<Card> {

        private Integer iddfirm, iddclient, iddsub;
        private LocalDate dtw;
        private Card.WorkState workstate;

        public CardDataService(Grid<Card> grid) {
            super(grid);
        }

        @Override
        public void setup() {
            this.iddfirm = user.getFirm().id;
            this.iddclient = user.getIddClient();
            this.iddsub = user.getIddClentSub();
            this.dtw = curPM.dtStart;
            this.workstate = curPM.workState;
        }

        @Override
        protected ArrayList<Card> load(int offset, int limit, String sort) throws ExError {
            return model.loadClientCards(iddfirm, iddclient, iddsub, dtw, workstate, offset, limit, sort);
        }

        @Override
        protected int count() throws ExError {
            return model.loadClientCardsCount(iddfirm, iddclient, iddsub, dtw, workstate);
        }
    }
}
