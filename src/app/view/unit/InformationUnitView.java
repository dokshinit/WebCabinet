package app.view.unit;

import app.AppUI;
import app.model.*;
import app.view.AbstractUnitView;
import com.google.gwt.user.client.ui.HTMLTable;
import com.vaadin.data.ValueProvider;
import com.vaadin.data.provider.DataProvider;
import com.vaadin.server.UserError;
import com.vaadin.shared.data.sort.SortDirection;
import com.vaadin.shared.ui.ContentMode;
import com.vaadin.shared.ui.grid.HeightMode;
import com.vaadin.ui.*;
import com.vaadin.ui.components.grid.HeaderRow;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.EventObject;

import static app.AppServlet.logger;
import static app.view.unit.Helper.*;

/**
 * @author Aleksey Dokshin <dant.it@gmail.com> (13.12.17).
 */
public class InformationUnitView extends Panel implements AbstractUnitView {

    private final AppModel model;
    private final User user;

    private Label titleInfo;
    private VerticalLayout infoL, contractL, accL;

    public InformationUnitView() {
        addStyleName("information-UV");
        setSizeFull();

        VerticalLayout root = style(new VerticalLayout(), "root-L");
        root.setSizeUndefined();
        root.setSpacing(false);
        root.setMargin(false);
        setContent(root);

        model = AppUI.model();
        user = model.getUser();

        // Заголовок
        VerticalLayout headLayout = style(new VerticalLayout(), "header-L");
        HorizontalLayout hl = style(new HorizontalLayout(), "title-L");
        Label title = style(new Label("Сводная информация"), "title");
        title.setSizeUndefined();
        titleInfo = style(new Label("(на " + fmtDT86(LocalDateTime.now()) + ")"), "info");
        titleInfo.setSizeUndefined();
        Button updateButton = style(new Button("Обновить"), "small", "update");
        updateButton.setSizeUndefined();
        hl.addComponents(title, titleInfo, updateButton);
        headLayout.addComponents(hl);

        // Тело.
        VerticalLayout bodyLayout = style(new VerticalLayout(), "body-L");
        buildInfo();
        buildContracts();
        buildAccs();
        bodyLayout.addComponents(infoL, contractL, accL);

        root.addComponents(headLayout, bodyLayout);

        root.addLayoutClickListener((e) -> fireOnLayoutClick());
        updateButton.addClickListener((e) -> fireOnUpdateClick());
    }

    @Override
    public AppUI getUI() {
        return (AppUI) super.getUI();
    }

    protected void fireOnLayoutClick() {
        getUI().closeOpenedWindows();
    }

    protected void fireOnUpdateClick() {
        titleInfo.setValue("(на " + fmtDT86(LocalDateTime.now()) + ")");
        updateContracts();
        updateAccs();
    }

    void buildInfo() {
        infoL = style(new VerticalLayout(), "info-L");
        infoL.setSpacing(false);

        Label firm1 = style(new Label("Эмитент:"), "hhh");
        Label firmTitle;
        if (user.getFirm() == Firm.TP) {
            firmTitle = style(new Label("<a href='http://www.tp-rk.ru'>" + user.getFirm().title + "</a>", ContentMode.HTML), "firm");
        } else {
            firmTitle = style(new Label(user.getFirm().title), "firm");
        }
        HorizontalLayout h1 = new HorizontalLayout(firm1, firmTitle);

        Label client1 = style(new Label("Клиент:"), "hhh");
        Label clientTitle = style(new Label(user.getClientTitle()), "clienttitle");
        HorizontalLayout h2 = new HorizontalLayout(client1, clientTitle);

        infoL.addComponents(h1, h2);

        if (!user.getClientSubTitle().isEmpty()) {
            Label sub1 = style(new Label("Подразделение:"), "hhh");
            Label clientSubTitle = style(new Label(user.getClientSubTitle()), "subtitle");
            HorizontalLayout h3 = new HorizontalLayout(sub1, clientSubTitle);
            infoL.addComponents(h3);
        }
    }


    private Grid<Contract> contractGrid;

    private <V> Grid.Column<Contract, V> contractColumn(ValueProvider<Contract, V> prov, ValueProvider<V, String> pres,
                                                        String id, String title, StyleGenerator<Contract> style, int wfix) {
        Grid.Column<Contract, V> c = gridColumn(contractGrid, prov, pres, id, title, style);
        c.setWidth(wfix);
        c.setResizable(false);
        c.setSortable(false);
        return c;
    }

    @SuppressWarnings({"Convert2MethodRef", "unchecked"})
    void buildContracts() {
        contractL = style(new VerticalLayout(), "contract-L");
        contractL.setSpacing(false);

        Label title = style(new Label("Договоры"), "hhh");
        contractL.addComponent(title);

        contractGrid = style(new Grid<>(), "contracts-table");
        contractColumn(c -> c.getIdd(), null, "IDD", "Код", ST_AL_RIGHT, 60);
        contractColumn(c -> c.getNumber(), null, "CNUMBER", "Номер", ST_AL_CENTER, 150);
        Grid.Column<Contract, ContractType> typeC = contractColumn(c -> c.getType(), v -> v.abbreviation, "ITYPE", "Тип", ST_AL_CENTER, 50);
        typeC.setDescriptionGenerator((v1) -> v1.getType().title);
        contractColumn(c -> c.getDtSign(), v -> fmtDate8(v), "DTSIGN", "Подписан", ST_AL_CENTER, 100);
        contractColumn(c -> c.getDtStartFact(), v -> fmtDate8(v), "DTSTARTFACT", "Начало", ST_AL_CENTER, 100);
        contractColumn(c -> c.getDtEndFact(), v -> fmtDate8(v), "DTENDFACT", "Завершение", ST_AL_CENTER, 100);
        contractColumn(c -> c.getExpiredState(), v -> Contract.getExpiredTitle(v), "IEXPIRED", "Состояние", ST_AL_CENTER, 100);
        contractGrid.setSelectionMode(Grid.SelectionMode.NONE);
        contractGrid.setColumnReorderingAllowed(false);
        contractGrid.setWidth("660px");
        contractGrid.setRowHeight(30);
        contractGrid.setStyleGenerator((t) -> t.getExpiredStyle());
        contractL.addComponent(contractGrid);
        contractL.setExpandRatio(contractGrid, 1);

        updateContracts();
    }

    void updateContracts() {
        try {
            ArrayList<Contract> list = model.loadClientContracts(user.getFirm().id, user.getIddClient(), user.getIddClentSub(), LocalDate.now().minusYears(1));
            contractGrid.setDataProvider(DataProvider.ofCollection(list));
            //contractGrid.setHeight((list.size() + 1) * 30, Unit.PIXELS);
            contractGrid.sort("DTSTARTFACT", SortDirection.DESCENDING);
            contractGrid.setHeightMode(HeightMode.ROW);
            contractGrid.setHeightByRows(list.size());

        } catch (Exception ex) {
            logger.error("Error: ", ex);
            contractL.setComponentError(new UserError("Ошибка запроса БД!"));
        }
    }


    private Grid<Acc> accGrid;

    private <V> Grid.Column<Acc, V> accColumn(ValueProvider<Acc, V> prov, ValueProvider<V, String> pres,
                                              String id, String title, StyleGenerator<Acc> style, int wfix) {
        Grid.Column<Acc, V> c = gridColumn(accGrid, prov, pres, id, title, style);
        c.setWidth(wfix);
        c.setResizable(false);
        c.setSortable(false);
        return c;
    }

    String rightNeg(long val) {
        if (val < 0) {
            return "v-align-right neg";
        }
        return "v-align-right";
    }

    void buildAccs() {
        accL = style(new VerticalLayout(), "acc-L");
        accL.setSpacing(false);

        Label title = style(new Label("Лицевые счета"), "hhh");
        accL.addComponent(title);

        GridLayout gl = new GridLayout();
        gl.setColumns(7);
        gl.addStyleName("my-grid");

        String[] h = {"my-grid-header"};
        gl.addComponents(
                style(new Label("Код"), h),
                style(new Label("Тип"), h),
                style(new Label("Н/П"), h),
                style(new Label("Нач.остаток"), h),
                style(new Label("Приход"), h),
                style(new Label("Расход"), h),
                style(new Label("Тек.остаток"), h)
        );

        String[] h2 = {"my-grid-cell"};
        gl.addComponents(
                style(new Label("Код"), h2),
                style(new Label("Тип"), h2),
                style(new Label("Н/П"), h2),
                style(new Label("Нач.остаток"), h2),
                style(new Label("Приход"), h2),
                style(new Label("Расход"), h2),
                style(new Label("Тек.остаток"), h2)
        );

        accL.addComponent(gl);


        accGrid = style(new Grid<>(), "accs-table");
        accColumn(c -> c.getIdd(), null, "IDD", "Код", ST_AL_RIGHT, 60);
        Grid.Column<Acc, AccType> typeC = accColumn(c -> c.getType(), v -> v.abbreviation, "ITYPE", "Тип", ST_AL_CENTER, 50);
        typeC.setDescriptionGenerator((v1) -> v1.getType().title);
        Grid.Column<Acc, Oil> oilC = accColumn(c -> c.getOil(), v -> v != null ? v.abbreviation : "", "IDDOIL", "Н/П", ST_AL_CENTER, 50);
        oilC.setDescriptionGenerator((v1) -> v1.getOil() != null ? v1.getOil().title : null);
        accColumn(c -> c.getDbStart(), v -> fmtN2(v), "DBSTART", "Нач.остаток", ST_AL_RIGHT, 125).setStyleGenerator(v -> rightNeg(v.getDbStart()));
        accColumn(c -> c.getDbPay(), v -> fmtN2(v), "DBPAY", "Приход*", ST_AL_RIGHT, 125);
        accColumn(c -> c.getDbSale(), v -> fmtN2(v), "DBTRANS", "Расход*", ST_AL_RIGHT, 125);
        accColumn(c -> c.getDbEnd(), v -> fmtN2(v), "DBEND", "Тек.остаток*", ST_AL_RIGHT, 125).setStyleGenerator(v -> rightNeg(v.getDbEnd()));
        HeaderRow r = accGrid.getDefaultHeaderRow();
        r.getCell("DBPAY").setHtml("Приход<span class='star'>*</span>");
        r.getCell("DBTRANS").setHtml("Расход<span class='star'>*</span>");
        r.getCell("DBEND").setHtml("Тек.остаток<span class='star'>*</span>");

        accGrid.setSelectionMode(Grid.SelectionMode.NONE);
        accGrid.setColumnReorderingAllowed(false);
        accGrid.setWidth("660px");
        accGrid.setRowHeight(30);
        accL.addComponent(accGrid);
        accL.addComponent(style(new Label("<span class='star'>*</span> Цифры на момент последней консолидации данных!", ContentMode.HTML), "note"));
        accL.addComponent(style(new Label("<span class='star'>*</span> Задержка актуализации данных может составлять до одного <u>рабочего</u> дня!", ContentMode.HTML), "note"));
        accL.setExpandRatio(accGrid, 1);

        updateAccs();
    }

    void updateAccs() {
        try {
            ArrayList<Acc> list = model.loadClientAccs(user.getFirm().id, user.getIddClient(), user.getIddClentSub(), LocalDate.now().minusYears(1));
            accGrid.setDataProvider(DataProvider.ofCollection(list));
            accGrid.setHeightMode(HeightMode.ROW);
            accGrid.setHeightByRows(list.size());
            accGrid.sort("IDD", SortDirection.ASCENDING);

        } catch (Exception ex) {
            logger.error("Error: ", ex);
            accL.setComponentError(new UserError("Ошибка запроса БД!"));
        }
    }
}
