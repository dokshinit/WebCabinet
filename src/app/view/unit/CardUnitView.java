package app.view.unit;

import app.AppServlet;
import app.AppUI;
import app.ExError;
import app.model.AppModel;
import app.model.Card;
import app.sizer.SizeReporter;
import app.view.AbstractUnitView;
import com.vaadin.data.Binder;
import com.vaadin.data.ValidationException;
import com.vaadin.data.ValueProvider;
import com.vaadin.data.provider.DataProvider;
import com.vaadin.data.provider.GridSortOrderBuilder;
import com.vaadin.data.provider.QuerySortOrder;
import com.vaadin.server.Responsive;
import com.vaadin.server.UserError;
import com.vaadin.shared.data.sort.SortDirection;
import com.vaadin.ui.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static app.view.unit.Helper.*;

/**
 * @author Aleksey Dokshin <dant.it@gmail.com> (13.12.17).
 */
public class CardUnitView extends VerticalLayout implements AbstractUnitView {

    public AppModel getModel() {
        return AppUI.model();
    }

    private SizeReporter gridSizeReporter;
    private Grid<Card> grid;
    private SingleSelect<Card> singleSelect;
    private Button filterButton, reportButton;

    private ComboBox<Card.WorkState> workCombo;
    private DateField dtwField;

    private Card.WorkState workState;
    private LocalDate dtw;

    public CardUnitView() {
        setSizeFull();
        Responsive.makeResponsive(this);
        addStyleName("cards-UV");
        setMargin(false);
        setSpacing(false);

        workCombo = null;
        dtw = LocalDate.now().minusYears(1).withDayOfMonth(1);

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

    private Binder<CardUnitView> dtwBind = new Binder<>();

    private Component buildToolbar() {
        VerticalLayout header = style(new VerticalLayout(), "header-L");
        header.setSpacing(false);

        Label title = style(new Label("Топливные карты"), "title");
        title.setSizeUndefined();

        workCombo = style(new ComboBox<>("Состояние"), "small");
        workCombo.setTextInputAllowed(false);
        workCombo.setEmptySelectionAllowed(true);
        workCombo.setEmptySelectionCaption("< Все >");
        workCombo.setItemCaptionGenerator(Card.WorkState::getTitle);
        workCombo.addValueChangeListener((e) -> fireOnWorkStateChanged(e.getValue()));
        workCombo.setDataProvider(DataProvider.ofItems(Card.WorkState.values()));

        dtwField = new DateField("Дата", dtw);
        dtwField.addStyleName("dtw");
        dtwField.addStyleName("small");
        dtwField.addValueChangeListener((e) -> fireOnDtwChanged(0));
        dtwField.setPlaceholder("Дата");
        dtwField.setDateFormat("dd/MM/yyyy");
        dtwBind.forField(dtwField)
                .asRequired("Обязательное поле")
                .bind((v) -> dtw, (c, v) -> dtw = v);

        HorizontalLayout dtwL = new HorizontalLayout(dtwField, workCombo);
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

        CssLayout tools = new CssLayout(dtwL, btnL);
        Responsive.makeResponsive(tools);
        tools.addStyleName("toolbar-L");

        header.addComponents(title, tools);
        header.setComponentAlignment(title, Alignment.MIDDLE_LEFT);
        header.setComponentAlignment(tools, Alignment.MIDDLE_RIGHT);

        return header;
    }

    private CardDataService dataService;

    private <V> Grid.Column<Card, V> cardColumn(ValueProvider<Card, V> prov, ValueProvider<V, String> pres,
                                                String id, String title, StyleGenerator<Card> style, int wmin) {
        Grid.Column<Card, V> c = gridColumn(grid, prov, pres, id, title, style);
        c.setMinimumWidth(wmin);
        c.setExpandRatio(wmin);
        return c;
    }

    @SuppressWarnings("unchecked")
    private Grid<Card> buildGrid() {
        grid = new Grid<>();
        grid.setSelectionMode(Grid.SelectionMode.SINGLE);
        grid.setSizeFull();
        grid.addStyleName("cards-table");

        Grid.Column<Card, String> c1;
        Grid.Column<Card, Card.WorkState> c2;

        cardColumn(Card::getDtw, Helper::fmtDate8, "DTW", "Начало", ST_AL_CENTER, 90).setHidable(true);
        cardColumn(Card::getDtwEnd, Helper::fmtDate8, "DTWEND", "Завершение", ST_AL_CENTER, 90).setHidable(true).setHidden(true);
        c1 = cardColumn(Card::getIddCard, null, "IDD", "№", ST_AL_CENTER, 90);
        cardColumn(Card::getAccType, v -> v.abbreviation, "IACCTYPE", "Тип", ST_AL_CENTER, 50).setHidable(true);
        c2 = cardColumn(Card::getWorkState, v -> v.title, "IBWORK", "Состояние", ST_AL_CENTER, 90).setHidable(true);
        cardColumn(Card::getDriver, null, "CDRIVER", "Водитель", ST_AL_LEFT, 100);
        cardColumn(Card::getCar, null, "CCAR", "Автомобиль", ST_AL_LEFT, 100);
        cardColumn(Card::getDbDayLimit, v -> v == 0 ? "---" : fmtN2(v), "DBDAYLIMIT", "Лимит", ST_AL_RIGHT, 60).setHidable(true);
        cardColumn(Card::getComment, null, "CCOMMENT", "Информация", ST_AL_LEFT, 100).setHidable(true);

        grid.setColumnReorderingAllowed(true);
        grid.recalculateColumnWidths();

        dataService = new CardDataService(dtw, workState);
        DataProvider<Card, Void> dataProvider = DataProvider.fromCallbacks(
                q -> dataService.fetch(filterButton, q.getOffset(), q.getLimit(), q.getSortOrders()).stream(),
                q -> dataService.count(filterButton)

        );
        grid.setDataProvider(dataProvider);
        grid.setSortOrder(new GridSortOrderBuilder().thenDesc(c2).thenAsc(c1));

        gridSizeReporter = new SizeReporter(grid);
        gridSizeReporter.addResizeListener((e) -> updateGridColumns(grid, gridSizeReporter));

        return grid;
    }

    public void fireOnLayoutClick() {
        getUI().closeOpenedWindows();
    }

    private void fireOnWorkStateChanged(Card.WorkState ws) {
        if (dtwBind.isValid()) {
            filterButton.setEnabled(true);
            reportButton.setEnabled(true);
        }
        workState = ws;
    }


    private void fireOnDtwChanged(int id) {
        // Проверка валидаторов.
        try {
            dtwBind.writeBean(this);
            filterButton.setEnabled(true);
            reportButton.setEnabled(true);
        } catch (ValidationException ex) {
            filterButton.setEnabled(false);
            reportButton.setEnabled(false);
        }
    }

    private void fireOnFilterApply() {
        dtwBind.validate();
        if (dtwBind.isValid()) {
            dataService.setup(dtw, workState);
            grid.setDataProvider(grid.getDataProvider());
            grid.recalculateColumnWidths();
            filterButton.setEnabled(false);
        } else {
            filterButton.setEnabled(false);
            reportButton.setEnabled(false);
        }
    }

    private void fireOnCreateReport() {
        for (Grid.Column<Card, ?> c : grid.getColumns()) {
            AppServlet.logger.infof("GRID-COLUMN: name='%s' width=%f", c.getId(), c.getWidth());
        }
    }


    private class CardDataService {

        private Integer iddfirm, iddclient, iddsub;
        private LocalDate dtw;
        private Integer iwork;

        public CardDataService(LocalDate dtw, Card.WorkState workstate) {
            setup(dtw, workstate);
        }

        public void setup(LocalDate dtw, Card.WorkState workstate) {
            this.iddfirm = getModel().getUser().getFirm().id;
            this.iddclient = getModel().getUser().getIddClient();
            this.iddsub = getModel().getUser().getIddClentSub();
            this.dtw = dtw;
            this.iwork = workstate == null ? null : workstate.id;
        }

        public ArrayList<Card> fetch(AbstractComponent errcomp, int offset, int limit, List<QuerySortOrder> order) {
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
                return getModel().loadClientCards(iddfirm, iddclient, iddsub, dtw, iwork, offset, limit, sb.toString());
            } catch (ExError ex) {
                if (errcomp != null) errcomp.setComponentError(new UserError(ex.getMessage()));
                return new ArrayList<>();
            }
        }

        public int count(AbstractComponent errcomp) {
            if (errcomp != null && errcomp.getComponentError() != null) errcomp.setComponentError(null);
            try {
                return getModel().loadClientCardsCount(iddfirm, iddclient, iddsub, dtw, iwork);
            } catch (ExError ex) {
                if (errcomp != null) errcomp.setComponentError(new UserError(ex.getMessage()));
                return 0;
            }
        }
    }
}
