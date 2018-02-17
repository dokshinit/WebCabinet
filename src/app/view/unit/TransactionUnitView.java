package app.view.unit;

import app.ExError;
import app.dialog.RequestDialog;
import app.model.*;
import com.vaadin.server.UserError;
import com.vaadin.ui.*;

import java.time.LocalDate;
import java.util.*;

import static app.view.unit.Helper.*;

/**
 * @author Aleksey Dokshin <dant.it@gmail.com> (13.12.17).
 */
public class TransactionUnitView extends BaseUnitView {

    private Grid<Transaction> grid;
    private TransactionDataService dataService;
    private SingleSelect<Transaction> singleSelect;
    private ComboBox<Azs> azsCombo;

    private Integer iddAzs;

    private ArrayList<Azs> azsList = null;

    public TransactionUnitView() {
        super("transaction", "Транзакции отпуска топлива");
    }

    @Override
    protected void initVars() {
        super.initVars();

        iddAzs = null;
        dtStart = LocalDate.now().withDayOfMonth(1);
        dtEnd = dtStart.plusMonths(1).minusDays(1);
    }

    @Override
    protected void buildHeadToolbar() {
        buildHeadToolbarLayout();
        buildAzsCombo();
        buildHeadToolbarDtwsForPeriod();
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
        iddAzs = azs == null ? null : azs.getIdd();
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
    protected void fireOnCreateReport() {
        if (!checkForStartAndFix(dtStart, dtEnd)) return;

        HashMap<String, String> params = new HashMap<>();
        String s1, s2;
        params.put("dtStart", s1 = fmtDate8(dtStart));
        params.put("dtEnd", s2 = fmtDate8(dtEnd));
        params.put("iddAzs", iddAzs == null ? "" : "" + iddAzs);
        //
        Request r = Request.newReport(Request.ReportType.TRANSACTION,
                "за период с " + s1 + " по " + s2 + (iddAzs == null ? "" : " на АЗС №" + iddAzs), params);
        RequestDialog dlg = new RequestDialog(r);
        getUI().addWindow(dlg);
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
            this.dtstart = TransactionUnitView.this.dtStart;
            this.dtend = TransactionUnitView.this.dtEnd;
            this.iddazs = TransactionUnitView.this.iddAzs;
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
