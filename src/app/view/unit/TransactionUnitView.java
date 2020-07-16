package app.view.unit;

import app.ExError;
import app.dialog.RequestDialog;
import app.model.*;
import com.vaadin.icons.VaadinIcons;
import com.vaadin.server.UserError;
import com.vaadin.ui.*;
import util.CommonTools;

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
        public Card card;

        PM() {
            super(BaseParamsModel.DateLimitEnum.CLIENT, BaseParamsModel.DateLimitEnum.FIX,
                    BaseParamsModel.DatesModeEnum.PERIOD, null, null);
            azs = null;
            card = null;
        }

        @Override
        public void to(PM dst) {
            if (dst != null) {
                super.to(dst);
                dst.azs = azs;
                dst.card = card;
            }
        }

        Integer getAzsIdd() {
            return azs == null ? null : azs.getIdd();
        }

        String getAzsIddAsString() {
            return getAzsIdd() == null ? "" : "" + getAzsIdd();
        }

        String getCardIdd() {
            return card == null ? null : card.getIdd();
        }

        String getCardIddAsString() {
            return card == null ? "" : card.getIdd().trim();
        }

        String getCardTitle() {
            return card == null ? null : card.getTitle();
        }
    }

    private Grid<Transaction> grid;
    private TransactionDataService dataService;
    private SingleSelect<Transaction> singleSelect;
    private ComboBox<Azs> azsCombo;
    private ComboBox<Card> cardCombo;

    private ArrayList<Azs> azsList = null;
    private ArrayList<Card> cardList = null;

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
        curPM.card = null;
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
        buildHeadToolbarDates();
        buildAzsCombo();
        buildCardCombo();
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
        updateAzs();
        HorizontalLayout azsL = new HorizontalLayout(azsCombo);
        azsL.setMargin(false);

        toolbarL.addComponent(azsL);
    }

    private void updateAzs() {
        if (!isValidToolbar()) return;
        try {
            Optional<Azs> item = azsCombo.getSelectedItem();
            azsList = model.loadAzs(0, curPM.dtStart, curPM.dtEnd); // по всем фирмам т.к. могут заправляться и на снежинке!
            azsCombo.setItems(azsList);
            azsCombo.setEnabled(true);
            if (item.isPresent()) {
                int idd = item.get().getIdd();
                azsList.stream().filter(a -> a.getIdd() == idd).findFirst()
                        .ifPresent(azs -> azsCombo.setSelectedItem(azs));
            }
        } catch (ExError ex) {
            azsCombo.setComponentError(new UserError(ex.getMessage()));
        }
    }

    private void buildCardCombo() {
        cardCombo = style(new ComboBox<>("Карта"), "small");
        cardCombo.setTextInputAllowed(false);
        cardCombo.setEmptySelectionAllowed(true);
        cardCombo.setEmptySelectionCaption("< Все >");
        cardCombo.setItemCaptionGenerator(Card::getTitle);
        cardCombo.addValueChangeListener((e) -> fireOnCardChanged(e.getValue()));
        cardCombo.setEnabled(false);
        updateCards();

        HorizontalLayout azsL = new HorizontalLayout(cardCombo);
        azsL.setMargin(false);

        toolbarL.addComponent(azsL);
    }

    private void updateCards() {
        if (!isValidToolbar()) return;
        try {
            Optional<Card> item = cardCombo.getSelectedItem();
            cardList = model.loadClientWorkCards(user.getFirm().id, user.getIddClient(), user.getIddClentSub(),
                    curPM.dtStart, curPM.dtEnd);
            cardCombo.setItems(cardList);
            cardCombo.setEnabled(true);
            if (item.isPresent()) {
                String idd = item.get().getIdd();
                cardList.stream().filter(c -> CommonTools.isEqualValues(c.getIdd(), idd)).findFirst()
                        .ifPresent(card -> cardCombo.setSelectedItem(card));
            }
        } catch (ExError ex) {
            cardCombo.setComponentError(new UserError(ex.getMessage()));
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    protected void buildBodyContent() {
        grid = style(new Grid<>(), "transaction-table");
        grid.setSizeFull();
        column(grid, Transaction::getStart, Helper::fmtDT86, "DTSTART", "Время", ST_AL_CENTER, -160, null);
        column(grid, Transaction::getIddAzs, null, "IDDAZS", "АЗС", ST_AL_CENTER, -50, null);
        column(grid, Transaction::getIdd, null, "IDD", "№", ST_AL_RIGHT, -70, null).setHidable(true).setHidden(true);
        column(grid, Transaction::getCardTitle, null, "IDDCARD", "Карта", ST_AL_CENTER, -70, null);
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

    @Override
    protected void fireOnDateChanged(int id) {
        super.fireOnDateChanged(id);

        if (isValidToolbar()) {
            updateAzs();
            updateCards();
        }
    }

    private void fireOnAzsChanged(Azs azs) {
        toolbarPM.azs = azs;
        toolbarParamsChanged = true;
        updateButtonsState(!isValidToolbar());
    }

    private void fireOnCardChanged(Card card) {
        toolbarPM.card = card;
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
                .put("iddAzs", curPM.getAzsIddAsString())
                .put("iddCard", curPM.getCardIddAsString());
        //
        Request r = Request.newReport(Request.ReportType.TRANSACTION,
                "за период с " + s1 + " по " + s2
                        + (curPM.getAzsIdd() == null ? "" : " на АЗС №" + curPM.getAzsIdd())
                        + (curPM.getCardIdd() == null ? "" : " по карте №" + curPM.getCardTitle()),
                params);
        RequestDialog dlg = new RequestDialog(r);
        getUI().addWindow(dlg);
    }

    protected void fireOnExportButtonClick() {
        if (!checkForStartAndFix(curPM)) return;

        String s1, s2;
        RequestBase.ParamsMap params = new RequestBase.ParamsMap()
                .put("dtStart", s1 = fmtDate8(curPM.dtStart))
                .put("dtEnd", s2 = fmtDate8(curPM.dtEnd))
                .put("iddAzs", curPM.getAzsIddAsString())
                .put("iddCard", curPM.getCardIddAsString());
        //
        Request r = Request.newExport(Request.ExportType.TRANSACTION,
                "за период с " + s1 + " по " + s2
                        + (curPM.getAzsIdd() == null ? "" : " на АЗС №" + curPM.getAzsIdd())
                        + (curPM.getCardIdd() == null ? "" : " по карте №" + curPM.getCardTitle()),
                params);
        RequestDialog dlg = new RequestDialog(r);
        getUI().addWindow(dlg);
    }

    @Override
    protected void initToolbar() {
        super.initToolbar();
        azsCombo.setSelectedItem(curPM.azs);
        cardCombo.setSelectedItem(curPM.card);
    }

    @Override
    protected AppModel.LogActionPage getLogPage() {
        return AppModel.LogActionPage.TRANS;
    }

    @Override
    protected void updateData() {
        super.updateData();

        int iddazs = curPM.getAzsIdd() == null ? 0 : curPM.getAzsIdd();
        String iddcard = curPM.getCardIdd();
        paramsLabel.setValue("<span class='datesrange'>за период с <b>" + fmtDate8(curPM.dtStart) +
                "</b> по <b>" + fmtDate8(curPM.dtEnd) + "</b></span>, " +
                "<span>" + (iddazs == 0 ? "по <b>всем АЗС</b>" : "по <b>АЗС №" + iddazs + "</b>") + "</span>" +
                "<span>" + (iddcard == null ? "по <b>всем картам</b>" : "по <b>карте №" + iddcard + "</b>") + "</span>"
        );

        dataService.refresh();
    }

    private class TransactionDataService extends BaseGridDataService<Transaction> {

        private Integer iddfirm, iddclient, iddsub;
        private LocalDate dtstart, dtend;
        private Integer iddazs;
        private String iddcard;

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
            this.iddcard = curPM.getCardIdd();
        }

        @Override
        protected ArrayList<Transaction> load(int offset, int limit, String sort) throws ExError {
            return model.loadClientTransactions(iddfirm, iddclient, iddsub, dtstart, dtend, iddazs, iddcard, offset, limit, sort);
        }

        @Override
        protected int count() throws ExError {
            return model.loadClientTransactionsCount(iddfirm, iddclient, iddsub, dtstart, dtend, iddazs, iddcard);
        }
    }
}
