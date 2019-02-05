package app.view.unit;

import app.ExError;
import app.dialog.RequestDialog;
import app.model.*;
import com.vaadin.icons.VaadinIcons;
import com.vaadin.server.UserError;
import com.vaadin.ui.*;

import java.time.LocalDate;
import java.util.*;

import static app.view.unit.UHelper.*;
import static app.model.Helper.*;

/**
 * @author Aleksey Dokshin <dant.it@gmail.com> (13.12.17).
 */
public class TransactionUnitView extends BaseUnitView<TransactionUnitView.PM> {

    class PM extends BaseUnitView.BaseParamsModel<PM> {
        public Azs azs;

        PM() {
            super(BaseParamsModel.DateLimitEnum.CLIENT, BaseParamsModel.DateLimitEnum.FIX,
                    BaseParamsModel.DatesModeEnum.PERIOD, null, null);
            azs = null;
        }

        @Override
        public void to(PM dst) {
            if (dst != null) {
                super.to(dst);
                dst.azs = azs;
            }
        }

        Integer getAzsIdd() {
            return azs == null ? null : azs.getIdd();
        }

        String getAzsIddAsString() {
            return getAzsIdd() == null ? "" : "" + getAzsIdd();
        }
    }

    private Grid<Transaction> grid;
    private TransactionDataService dataService;
    private SingleSelect<Transaction> singleSelect;
    private ComboBox<Azs> azsCombo;

    private ArrayList<Azs> azsList = null;

    public TransactionUnitView() {
        super("transaction", "Транзакции отпуска топлива");
    }

    @Override
    protected void initVars() {
        hasParams = true;
        hasUpdate = true;
        hasReport = true;

        curPM = new PM();
        toolbarPM = new PM();

        curPM.azs = null;
        try {
            dtFix = model.getFixDate();
            curPM.dtStart = dtFix.withDayOfMonth(1);
            curPM.dtEnd = dtFix;
        } catch (Exception ex) {
            curPM.dtStart = LocalDate.now().withDayOfMonth(1);
            curPM.dtEnd = curPM.dtStart.plusMonths(1).minusDays(1);
        }
    }

    protected Button exportButton;

    @Override
    protected void buildHeadTitle(String title) {
        super.buildHeadTitle(title);

        exportButton = style(new Button(VaadinIcons.AUTOMATION), "small", "report-button");
        exportButton.setSizeUndefined();
        exportButton.setEnabled(false);
        exportButton.setTabIndex(-1);
        exportButton.setDescription("Запросить экспорт данных");
        exportButton.addClickListener(e -> fireOnExportButtonClick()); // vaadin:newspaper
        titleL.addComponent(exportButton, 0);
    }

    @Override
    protected void updateButtonsState(boolean iserror) {
        super.updateButtonsState(iserror);
        if (toolbarShowing) { // Если тулбар показан.
            if (exportButton != null)
                exportButton.setEnabled(paramsValid && toolbarAlwaysShowed && !toolbarParamsChanged);
        } else {
            // Только при успешном чтении даты фиксации! Т.к. проверка дат, чтобы были не старше!
            if (exportButton != null) exportButton.setEnabled(paramsValid);
        }
    }

    @Override
    protected void buildHeadToolbar() {
        buildHeadToolbarLayout();
        buildAzsCombo();
        buildHeadToolbarDates();
        buildHeadToolbarButtons();
    }

    private void buildAzsCombo() {
        azsCombo = style(new ComboBox<>("Объект АЗС"), "small"); //AppUI.model().loadAzs(2, LocalDate.now(), LocalDate.now().minusMonths(1))
        azsCombo.setTextInputAllowed(false);
        azsCombo.setEmptySelectionAllowed(true);
        azsCombo.setEmptySelectionCaption("< Все >");
        azsCombo.setItemCaptionGenerator(Azs::getTitle);
        azsCombo.addValueChangeListener((e) -> fireOnAzsChanged(e.getValue()));
        azsCombo.setEnabled(false);
        try {
            if (azsList == null) {
                azsList = model.loadAzs(0, LocalDate.now().minusMonths(1), LocalDate.now());
                azsCombo.setItems(azsList);
                azsCombo.setEnabled(true);
            }
        } catch (ExError ex) {
            azsCombo.setComponentError(new UserError(ex.getMessage()));
        }
        HorizontalLayout azsL = new HorizontalLayout(azsCombo);
        azsL.setMargin(false);

        toolbarL.addComponent(azsL);
    }

    @SuppressWarnings("unchecked")
    @Override
    protected void buildBodyContent() {
        grid = style(new Grid<>(), "transaction-table");
        grid.setSizeFull();
        column(grid, Transaction::getStart, Helper::fmtDT86, "DTSTART", "Время", ST_AL_CENTER, -160, null);
        column(grid, Transaction::getIddAzs, null, "IDDAZS", "АЗС", ST_AL_CENTER, -50, null);
        column(grid, Transaction::getIdd, null, "IDD", "№", ST_AL_RIGHT, -70, null).setHidable(true).setHidden(true);
        column(grid, Transaction::getCard, null, "IDDCARD", "Карта", ST_AL_CENTER, -70, null);
        column(grid, Transaction::getCardInfo, null, "CCARD", "Информация", ST_AL_LEFT, -100, null).setSortable(false).setHidable(true).setHidden(true);
        column(grid, Transaction::getAccType, AccType::getAbbreviation, "IACCTYPE", "Тип", ST_AL_CENTER, -45, null).setHidable(true);
        column(grid, Transaction::getOil, Oil::getAbbreviation, "IDDOIL", "Н/П", ST_AL_CENTER, -60, null);
        column(grid, Transaction::getPrice, Helper::fmtN2, "DBPRICE", "Цена", ST_AL_RIGHT, -60, null);
        column(grid, Transaction::getVolume, Helper::fmtN2, "DBVOLUME", "Кол-во", ST_AL_RIGHT, -70, null);
        column(grid, Transaction::getSumma, Helper::fmtN2, "DBSUMMA", "Сумма", ST_AL_RIGHT, -80, null);
        grid.setSelectionMode(Grid.SelectionMode.SINGLE);
        grid.setColumnReorderingAllowed(true);
        setRowHeight(grid);
        grid.recalculateColumnWidths();
        singleSelect = grid.asSingleSelect();

        grid.addItemClickListener(this::fireOnTransactionSelected);
        singleSelect.addValueChangeListener(e -> {
            if (e.getOldValue() != null && e.getValue() == null) singleSelect.setValue(e.getOldValue());
        });

        dataService = new TransactionDataService(grid);
        addSizeReporterForExpandGridColumns(grid);

        bodyL.addComponent(grid);
        rootL.setSizeFull();
        rootL.setExpandRatio(bodyL, 1);
        bodyL.setSizeFull();
        bodyL.setExpandRatio(grid, 1);
    }

    private void fireOnTransactionSelected(Grid.ItemClick<Transaction> e) {
        if (e.getMouseEventDetails().isDoubleClick()) {
        }
    }

    private void fireOnAzsChanged(Azs azs) {
        toolbarPM.azs = azs;
        toolbarParamsChanged = true;
        updateButtonsState(!isValidToolbar());
    }

    @Override
    protected void fireOnReportButtonClick() {
        if (!checkForStartAndFix(curPM)) return;

        String s1, s2;
        RequestBase.ParamsMap params = new RequestBase.ParamsMap()
                .put("dtStart", s1 = fmtDate8(curPM.dtStart))
                .put("dtEnd", s2 = fmtDate8(curPM.dtEnd))
                .put("iddAzs", curPM.getAzsIddAsString());
        //
        Request r = Request.newReport(Request.ReportType.TRANSACTION,
                "за период с " + s1 + " по " + s2 + (curPM.getAzsIdd() == null ? "" : " на АЗС №" + curPM.getAzsIdd()), params);
        RequestDialog dlg = new RequestDialog(r);
        getUI().addWindow(dlg);
    }

    protected void fireOnExportButtonClick() {
        if (!checkForStartAndFix(curPM)) return;

        String s1, s2;
        RequestBase.ParamsMap params = new RequestBase.ParamsMap()
                .put("dtStart", s1 = fmtDate8(curPM.dtStart))
                .put("dtEnd", s2 = fmtDate8(curPM.dtEnd))
                .put("iddAzs", curPM.getAzsIddAsString());
        //
        Request r = Request.newExport(Request.ExportType.TRANSACTION,
                "за период с " + s1 + " по " + s2 + (curPM.getAzsIdd() == null ? "" : " на АЗС №" + curPM.getAzsIdd()), params);
        RequestDialog dlg = new RequestDialog(r);
        getUI().addWindow(dlg);
    }

    @Override
    protected void initToolbar() {
        super.initToolbar();
        azsCombo.setSelectedItem(curPM.azs);
    }

    @Override
    protected AppModel.LogActionPage getLogPage() {
        return AppModel.LogActionPage.TRANS;
    }

    @Override
    protected void updateData() {
        super.updateData();

        int iddazs = curPM.getAzsIdd() == null ? 0 : curPM.getAzsIdd();
        paramsLabel.setValue("<span class='datesrange'>за период с <b>" + fmtDate8(curPM.dtStart) +
                "</b> по <b>" + fmtDate8(curPM.dtEnd) + "</b></span>, " +
                "<span>" + (iddazs == 0 ? "по <b>всем АЗС</b>" : "по <b>АЗС №" + iddazs + "</b>") + "</span>");

        dataService.refresh();
    }

    private class TransactionDataService extends BaseDataService<Transaction> {

        private Integer iddfirm, iddclient, iddsub;
        private LocalDate dtstart, dtend;
        private Integer iddazs;

        public TransactionDataService(Grid<Transaction> grid) {
            super(grid);
        }

        @Override
        public void setup() {
            this.iddfirm = user.getFirm().id;
            this.iddclient = user.getIddClient();
            this.iddsub = user.getIddClentSub();
            this.dtstart = curPM.dtStart;
            this.dtend = curPM.dtEnd;
            this.iddazs = curPM.getAzsIdd();
        }

        @Override
        protected ArrayList<Transaction> load(int offset, int limit, String sort) throws ExError {
            return model.loadClientTransactions(iddfirm, iddclient, iddsub, dtstart, dtend, iddazs, offset, limit, sort);
        }

        @Override
        protected int count() throws ExError {
            return model.loadClientTransactionsCount(iddfirm, iddclient, iddsub, dtstart, dtend, iddazs);
        }
    }
}
