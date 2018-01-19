package app.view.unit;

import app.AppServlet;
import app.AppUI;
import app.ExError;
import app.model.AppModel;
import app.model.Azs;
import app.model.Transaction;
import app.sizer.SizeReporter;
import app.view.AbstractUnitView;
import com.vaadin.data.Binder;
import com.vaadin.data.ValidationException;
import com.vaadin.data.ValueProvider;
import com.vaadin.data.provider.DataProvider;
import com.vaadin.data.provider.QuerySortOrder;
import com.vaadin.server.Responsive;
import com.vaadin.server.UserError;
import com.vaadin.shared.data.sort.SortDirection;
import com.vaadin.ui.*;

import java.time.LocalDate;
import java.util.*;

import static app.view.unit.Helper.*;

/**
 * @author Aleksey Dokshin <dant.it@gmail.com> (13.12.17).
 */
public class TransactionUnitView extends VerticalLayout implements AbstractUnitView {

    public AppModel getModel() {
        return AppUI.model();
    }

    private SizeReporter gridSizeReporter;
    private Grid<Transaction> grid;
    private SingleSelect<Transaction> singleSelect;
    private Button filterButton, reportButton;

    private ComboBox<Azs> azsCombo;
    private DateField dtwField, dtwEndField;

    private Integer iddAzs;
    private LocalDate dtw, dtwEnd;

    public TransactionUnitView() {
        setSizeFull();
        Responsive.makeResponsive(this);
        addStyleName("transactions-UV");
        setMargin(false);
        setSpacing(false);

        iddAzs = null;
        dtw = LocalDate.now().minusYears(1).withDayOfMonth(1);
        dtwEnd = dtw.plusMonths(1).minusDays(1);

        addComponent(buildToolbar());

        grid = buildGrid();
        singleSelect = grid.asSingleSelect();
        addComponent(grid);
        setExpandRatio(grid, 1);

        addLayoutClickListener((e) -> fireOnLayoutClick());
    }

    @Override
    public AppUI getUI() {
        return (AppUI) super.getUI();
    }

    private Binder<TransactionUnitView> dtwBind = new Binder<>();
    private Binder<TransactionUnitView> dtwEndBind = new Binder<>();

    private ArrayList<Azs> azsList = null;

    private Component buildToolbar() {
        VerticalLayout header = style(new VerticalLayout(), "header-L");
        header.setSpacing(false);

        Label title = style(new Label("Транзакции отпуска топлива"), "title");
        title.setSizeUndefined();

        azsCombo = style(new ComboBox<>("Объект АЗС"), "small"); //AppUI.model().loadAzs(2, LocalDate.now(), LocalDate.now().minusMonths(1))
        azsCombo.setTextInputAllowed(false);
        azsCombo.setEmptySelectionAllowed(true);
        azsCombo.setEmptySelectionCaption("< Все >");
        azsCombo.setItemCaptionGenerator(Azs::getTitle);
        azsCombo.addValueChangeListener((e) -> fireOnAzsChanged(e.getValue()));
        azsCombo.setEnabled(false);

        try {
            if (azsList == null) {
                azsList = getModel().loadAzs(0, LocalDate.now().minusMonths(1), LocalDate.now());
                azsCombo.setItems(azsList);
                azsCombo.setEnabled(true);
            }
        } catch (ExError ex) {
            azsCombo.setComponentError(new UserError(ex.getMessage()));
        }

        HorizontalLayout azsL = new HorizontalLayout(azsCombo);
        azsL.setMargin(false);

        dtwField = style(new DateField("Начало периода", dtw), "dtw", "small");
        dtwField.addValueChangeListener((e) -> fireOnDtwChanged(0));
        dtwField.setPlaceholder("Начало");
        dtwField.setDateFormat("dd/MM/yyyy");
        dtwBind.forField(dtwField)
                .asRequired("Обязательное поле")
                .withValidator((v) -> !(dtwEndField.getValue() != null && (v.compareTo(dtwEndField.getValue()) > 0)), "Позже даты конца периода!")
                .bind((v) -> dtw, (c, v) -> dtw = v);

        dtwEndField = style(new DateField("Конец периода", dtwEnd), "dtw", "small");
        dtwEndField.addValueChangeListener((e) -> fireOnDtwChanged(1));
        dtwEndField.setPlaceholder("Конец");
        dtwEndField.setDateFormat("dd/MM/yyyy");
        dtwEndBind.forField(dtwEndField)
                .asRequired("Обязательное поле")
                .withValidator((v) -> !(dtwField.getValue() != null && (v.compareTo(dtwField.getValue()) < 0)), "Раньше даты начала периода!")
                .bind((v) -> dtwEnd, (c, v) -> dtwEnd = v);

        HorizontalLayout dtwL = new HorizontalLayout(dtwField, dtwEndField);
        dtwL.setMargin(false);

        filterButton = new Button("Фильтр");
        filterButton.addStyleName("small");
        filterButton.setDescription("Применить фильтр");
        filterButton.addClickListener(event -> fireOnFilterApply());
        filterButton.setEnabled(false);

        reportButton = new Button("Отчёт"); // FILE_TEXT FILE_ZIP NEWSPAPER
        reportButton.addStyleName("small");
        reportButton.setDescription("Сформировать отчёт");
        reportButton.addClickListener(event -> fireOnCreateReport());
        reportButton.setEnabled(false);

        HorizontalLayout btnL = new HorizontalLayout(filterButton, reportButton);
        btnL.addStyleName("buttons-L");
        btnL.setMargin(false);
        btnL.setExpandRatio(reportButton, 1.0f);

        CssLayout tools = new CssLayout(azsL, dtwL, btnL);
        Responsive.makeResponsive(tools);
        tools.addStyleName("toolbar-L");

        header.addComponents(title, tools);
        header.setComponentAlignment(title, Alignment.MIDDLE_LEFT);
        header.setComponentAlignment(tools, Alignment.MIDDLE_RIGHT);

        return header;
    }

    private TransactionDataService dataService;

    private <V> Grid.Column<Transaction, V> transColumn(ValueProvider<Transaction, V> prov, ValueProvider<V, String> pres,
                                                        String id, String title, StyleGenerator<Transaction> style, int wmin) {
        Grid.Column<Transaction, V> c = gridColumn(grid, prov, pres, id, title, style);
        c.setMinimumWidth(wmin);
        c.setExpandRatio(wmin);
        return c;
    }

    @SuppressWarnings("unchecked")
    private Grid<Transaction> buildGrid() {
        grid = new Grid<>();
        grid.setSelectionMode(Grid.SelectionMode.SINGLE);
        grid.setSizeFull();
        grid.addStyleName("transactions-table");

        transColumn(t -> t.getStart(), v -> fmtDT86(v), "DTSTART", "Время", ST_AL_CENTER, 160);
        transColumn(t -> t.getIddAzs(), null, "IDDAZS", "АЗС", ST_AL_CENTER, 50);
        transColumn(t -> t.getIdd(), null, "IDD", "№", ST_AL_RIGHT, 70).setHidable(true).setHidden(true);
        transColumn(t -> t.getCard(), null, "IDDCARD", "Карта", ST_AL_CENTER, 70);
        transColumn(t -> t.getCardInfo(), null, "CCARD", "Информация", ST_AL_LEFT, 100).setSortable(false).setHidable(true).setHidden(true);
        transColumn(t -> t.getAccType().abbreviation, null, "IACCTYPE", "Тип", ST_AL_CENTER, 45).setHidable(true);
        transColumn(t -> t.getOil().abbreviation, null, "IDDOIL", "Н/П", ST_AL_CENTER, 60);
        transColumn(t -> t.getPrice(), v -> fmtN2(v), "DBPRICE", "Цена", ST_AL_RIGHT, 60);
        transColumn(t -> t.getVolume(), v -> fmtN2(v), "DBVOLUME", "Кол-во", ST_AL_RIGHT, 70);
        transColumn(t -> t.getSumma(), v -> fmtN2(v), "DBSUMMA", "Сумма", ST_AL_RIGHT, 80);

        grid.setColumnReorderingAllowed(true);
        grid.recalculateColumnWidths();

        dataService = new TransactionDataService(dtw, dtwEnd, iddAzs);
        DataProvider<Transaction, Void> dataProvider = DataProvider.fromCallbacks(
                q -> dataService.fetch(filterButton, q.getOffset(), q.getLimit(), q.getSortOrders()).stream(),
                q -> dataService.count(filterButton)

        );
        grid.setDataProvider(dataProvider);

        gridSizeReporter = new SizeReporter(grid);
        gridSizeReporter.addResizeListener((e) -> updateGridColumns(grid, gridSizeReporter));

        return grid;
    }

    public void fireOnLayoutClick() {
        getUI().closeOpenedWindows();
    }

    private void fireOnAzsChanged(Azs azs) {
        if (dtwBind.isValid() && dtwEndBind.isValid()) {
            filterButton.setEnabled(true);
            reportButton.setEnabled(true);
        }
        iddAzs = azs == null ? null : azs.getIdd();
        //AppServlet.logger.infof("AZS CHANGE: %d", iddAzs);
    }


    private void fireOnDtwChanged(int id) {
        // Проверка валидаторов.
        Binder<TransactionUnitView> b1 = id == 0 ? dtwBind : dtwEndBind;
        Binder<TransactionUnitView> b2 = id == 0 ? dtwEndBind : dtwBind;
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

        if (iserr) {
            filterButton.setEnabled(false);
            reportButton.setEnabled(false);
        } else {
            filterButton.setEnabled(true);
            reportButton.setEnabled(true);
        }
    }

    private void fireOnFilterApply() {
        dtwBind.validate();
        dtwEndBind.validate();
        if (dtwBind.isValid() && dtwEndBind.isValid()) {
            dataService.setup(dtw, dtwEnd, iddAzs);
            grid.setDataProvider(grid.getDataProvider());
            grid.recalculateColumnWidths();
            filterButton.setEnabled(false);
        } else {
            filterButton.setEnabled(false);
            reportButton.setEnabled(false);
        }
    }

    private void fireOnCreateReport() {
        for (Grid.Column<Transaction, ?> c : grid.getColumns()) {
            AppServlet.logger.infof("GRID-COLUMN: name='%s' width=%f", c.getId(), c.getWidth());
        }
    }


    private class TransactionDataService {

        private Integer iddfirm, iddclient, iddsub;
        private LocalDate dtw, dtwend;
        private Integer iddazs;

        public TransactionDataService(LocalDate dtw, LocalDate dtwEnd, Integer iddazs) {
            setup(dtw, dtwEnd, iddazs);
        }

        public void setup(LocalDate dtw, LocalDate dtwEnd, Integer iddazs) {
            this.iddfirm = getModel().getUser().getFirm().id;
            this.iddclient = getModel().getUser().getIddClient();
            this.iddsub = getModel().getUser().getIddClentSub();
            this.dtw = dtw;
            this.dtwend = dtwEnd.plusDays(1);
            this.iddazs = iddazs;
        }

        public ArrayList<Transaction> fetch(AbstractComponent errcomp, int offset, int limit, List<QuerySortOrder> order) {
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
                return getUI().getModel().loadClientTransactions(iddfirm, iddclient, iddsub, dtw, dtwend, iddazs, offset, limit, sb.toString());
            } catch (ExError ex) {
                if (errcomp != null) errcomp.setComponentError(new UserError(ex.getMessage()));
                return new ArrayList<>();
            }
        }

        public int count(AbstractComponent errcomp) {
            if (errcomp != null && errcomp.getComponentError() != null) errcomp.setComponentError(null);
            try {
                return getUI().getModel().loadClientTransactionsCount(iddfirm, iddclient, iddsub, dtw, dtwend, iddazs);
            } catch (ExError ex) {
                if (errcomp != null) errcomp.setComponentError(new UserError(ex.getMessage()));
                return 0;
            }
        }
    }
}
