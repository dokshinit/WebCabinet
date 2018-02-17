package app.view.unit;

import app.model.*;
import com.vaadin.data.provider.DataProvider;
import com.vaadin.server.UserError;
import com.vaadin.shared.data.sort.SortDirection;
import com.vaadin.shared.ui.ContentMode;
import com.vaadin.ui.*;
import com.vaadin.ui.components.grid.HeaderRow;

import java.time.LocalDate;
import java.util.ArrayList;

import static app.AppServlet.logger;
import static app.view.unit.Helper.*;

/**
 * @author Aleksey Dokshin <dant.it@gmail.com> (13.12.17).
 */
public class InformationUnitView extends BaseUnitView {

    private VerticalLayout infoL, contractL, accL;
    private Label fixDateLabel;
    private Grid<Contract> contractGrid;
    private Grid<Acc> accGrid;

    public InformationUnitView() {
        super("information", "Сводная информация");
    }

    @Override
    protected void buildBodyContent() {
        buildInfo();
        buildContracts();
        buildAccs();
    }

    private void buildInfo() {
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

        bodyL.addComponent(infoL);
    }

    @SuppressWarnings("unchecked")
    private void buildContracts() {
        contractL = style(new VerticalLayout(), "contract-L");
        contractL.setSpacing(false);

        contractGrid = style(new Grid<>(), "contracts-table");
        column(contractGrid, Contract::getIdd, null, "IDD", "Код", ST_AL_RIGHT, 60, null);
        column(contractGrid, Contract::getNumber, null, "CNUMBER", "Номер", ST_AL_CENTER, 150, null);
        Grid.Column<Contract, ContractType> typeC =
                column(contractGrid, Contract::getType, ContractType::getAbbreviation, "ITYPE", "Тип", ST_AL_CENTER, 50, null);
        typeC.setDescriptionGenerator((v1) -> v1.getType().title);
        column(contractGrid, Contract::getDtSign, Helper::fmtDate8, "DTSIGN", "Подписан", ST_AL_CENTER, 100, null);
        column(contractGrid, Contract::getDtStartFact, Helper::fmtDate8, "DTSTARTFACT", "Начало", ST_AL_CENTER, 100, null);
        column(contractGrid, Contract::getDtEndFact, Helper::fmtDate8, "DTENDFACT", "Завершение", ST_AL_CENTER, 100, null);
        column(contractGrid, Contract::getExpiredState, Contract::getExpiredTitle, "IEXPIRED", "Состояние", ST_AL_CENTER, 100, null);
        contractGrid.setSelectionMode(Grid.SelectionMode.NONE);
        contractGrid.setColumnReorderingAllowed(false);
        contractGrid.setWidth("660px");
        setRowHeight(contractGrid);
        contractGrid.setStyleGenerator(Contract::getExpiredStyle);
        contractL.addComponents(style(new Label("Договоры"), "hhh"), contractGrid);
        //contractL.setExpandRatio(contractGrid, 1);

        bodyL.addComponent(contractL);
    }

    @SuppressWarnings("unchecked")
    private void buildAccs() {
        accL = style(new VerticalLayout(), "acc-L");
        accL.setSpacing(false);

        accGrid = style(new Grid<>(), "accs-table");
        column(accGrid, Acc::getIdd, null, "IDD", "Код", ST_AL_RIGHT, 60, null);
        Grid.Column<Acc, AccType> typeC = column(accGrid, Acc::getType, v -> v.abbreviation, "ITYPE", "Тип", ST_AL_CENTER, 50, null);
        typeC.setDescriptionGenerator((v1) -> v1.getType().title);
        Grid.Column<Acc, Oil> oilC = column(accGrid, Acc::getOil, v -> v != null ? v.abbreviation : "", "IDDOIL", "Н/П", ST_AL_CENTER, 50, null);
        oilC.setDescriptionGenerator((v1) -> v1.getOil() != null ? v1.getOil().title : null);
        column(accGrid, Acc::getDbStart, Helper::fmtN2, "DBSTART", "Нач.остаток*", ST_AL_RIGHT, 125, v -> rightNeg(v.getDbStart()));
        column(accGrid, Acc::getDbPay, Helper::fmtN2, "DBPAY", "Приход*", ST_AL_RIGHT, 125, null);
        column(accGrid, Acc::getDbSale, Helper::fmtN2, "DBTRANS", "Расход*", ST_AL_RIGHT, 125, null);
        column(accGrid, Acc::getDbEnd, Helper::fmtN2, "DBEND", "Тек.остаток*", ST_AL_RIGHT, 125, v -> rightNeg(v.getDbEnd()));
        HeaderRow r = accGrid.getDefaultHeaderRow();
        r.getCell("DBSTART").setHtml("Нач.остаток<span class='star'>*</span>");
        r.getCell("DBPAY").setHtml("Приход<span class='star'>*</span>");
        r.getCell("DBTRANS").setHtml("Расход<span class='star'>*</span>");
        r.getCell("DBEND").setHtml("Тек.остаток<span class='star'>*</span>");
        accGrid.setSelectionMode(Grid.SelectionMode.NONE);
        accGrid.setColumnReorderingAllowed(false);
        accGrid.setWidth("660px");
        setRowHeight(accGrid);

        fixDateLabel = style(new Label("<span class='star'>*</span> Дата фиксации данных: ---!", ContentMode.HTML), "note");

        accL.addComponents(style(new Label("Лицевые счета"), "hhh"), accGrid,
                style(new Label("<span class='star'>*</span> Дата начала учёта клиента: <span class='fixdate'>" +
                        fmtDate8(user.getDtStart()) + "</span>!", ContentMode.HTML), "note"),
                fixDateLabel,
                style(new Label("<span class='star'>*</span> Цифры на момент последней консолидации данных!", ContentMode.HTML), "note"),
                style(new Label("<span class='star'>*</span> Задержка обработки данных, при отсутствии форс-мажорных " +
                        "обстоятельств, может составлять до одного <u>рабочего</u> дня!", ContentMode.HTML), "note", "wrapped-label"));

        bodyL.addComponent(accL);
    }

    @Override
    protected AppModel.LogActionPage getLogPage() {
        return AppModel.LogActionPage.INFO;
    }

    @SuppressWarnings("Duplicates")
    @Override
    protected void updateData() {
        super.updateData();

        try {
            ArrayList<Contract> list = model.loadClientContracts(user.getFirm().id, user.getIddClient(), user.getIddClentSub(), LocalDate.now());
            contractGrid.setDataProvider(DataProvider.ofCollection(list));
            contractGrid.sort("DTSTARTFACT", SortDirection.DESCENDING);
            setHeightByCollection(contractGrid, list);

        } catch (Exception ex) {
            logger.error("Error: ", ex);
            contractL.setComponentError(new UserError("Ошибка запроса БД!"));
        }

        try {
            ArrayList<Acc> list = model.loadClientAccs(user.getFirm().id, user.getIddClient(), user.getIddClentSub(), LocalDate.now());
            accGrid.setDataProvider(DataProvider.ofCollection(list));
            setHeightByCollection(accGrid, list);
            accGrid.sort("IDD", SortDirection.ASCENDING);

        } catch (Exception ex) {
            logger.error("Error: ", ex);
            accL.setComponentError(new UserError("Ошибка запроса БД!"));
        }

        try {
            dtFix = model.getFixDate();
            fixDateLabel.setValue("<span class='star'>*</span> Дата фиксации данных: <span class='fixdate'>" + fmtDate8(dtFix) + "</span>!");
        } catch (Exception ex) {
            logger.error("Error: ", ex);
            fixDateLabel.setComponentError(new UserError("Ошибка запроса БД!"));
        }
    }
}