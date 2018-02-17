package app.view.unit;

import app.ExError;
import app.dialog.RequestDialog;
import app.model.AccType;
import app.model.AppModel;
import app.model.Card;
import app.model.Request;
import com.vaadin.data.provider.DataProvider;
import com.vaadin.data.provider.GridSortOrderBuilder;
import com.vaadin.ui.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;

import static app.view.unit.Helper.*;

/**
 * @author Aleksey Dokshin <dant.it@gmail.com> (13.12.17).
 */
public class CardUnitView extends BaseUnitView {

    private Grid<Card> grid;
    private SingleSelect<Card> singleSelect;
    private CardDataService dataService;

    private ComboBox<Card.WorkState> workCombo;

    private Card.WorkState workState;

    public CardUnitView() {
        super("card", "Топливные карты");
    }

    @Override
    protected void initVars() {
        super.initVars();

        workState = Card.WorkState.WORK;
        dtStart = LocalDate.now();
    }

    @Override
    protected void buildHeadToolbar() {
        buildHeadToolbarLayout();
        buildWorkStateCombo();
        buildHeadToolbarDtwForDate();
        buildHeadToolbarButtons();
    }

    private void buildWorkStateCombo() {
        workCombo = style(new ComboBox<>("Состояние"), "small");
        workCombo.setTextInputAllowed(false);
        workCombo.setEmptySelectionAllowed(true);
        workCombo.setEmptySelectionCaption("< Все >");
        workCombo.setItemCaptionGenerator(Card.WorkState::getTitleForMany);
        workCombo.setDataProvider(DataProvider.ofItems(Card.WorkState.values()));
        workCombo.setSelectedItem(workState);
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
        //column(grid, Card::getDtwEnd, Helper::fmtDate8, "DTWEND", "Завершение", ST_AL_CENTER, -90, null).setHidable(true).setHidden(true);
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
        workState = ws;
        toolbarParamsChanged = true;
        updateButtonsState(!isValid());
    }

    @Override
    public void fireOnDateChanged(int id) {
        fireOnDateChangedForDate();
    }

    @Override
    protected void fireOnCreateReport() {
        if (!checkForStartAndFix(dtStart, null)) return;

        HashMap<String, String> params = new HashMap<>();
        String s1;
        params.put("dtw", s1 = fmtDate8(dtStart));
        params.put("idWorkState", workState == null ? "" : "" + workState.id);
        //
        Request r = Request.newReport(Request.ReportType.CARD,
                "на " + s1 + (workState == null ? "" : " (" + workState.titleForMany + ")"), params);
        RequestDialog dlg = new RequestDialog(r);
        getUI().addWindow(dlg);
    }


    @Override
    protected void validate() {
        dtStartBind.validate();
    }

    @Override
    protected boolean isValid() {
        return dtStartBind.isValid();
    }

    @Override
    protected AppModel.LogActionPage getLogPage() {
        return AppModel.LogActionPage.CARD;
    }

    @Override
    protected void updateData() {
        updateTitleUpdateInfo();
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
            this.dtw = CardUnitView.this.dtStart;
            this.workstate = CardUnitView.this.workState;
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
