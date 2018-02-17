package app.view.unit;

import app.dialog.RequestDialog;
import app.model.*;
import com.vaadin.data.provider.DataProvider;
import com.vaadin.server.UserError;
import com.vaadin.shared.ui.ContentMode;
import com.vaadin.ui.*;
import com.vaadin.ui.components.grid.FooterCell;
import com.vaadin.ui.components.grid.FooterRow;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;

import static app.AppServlet.logger;
import static app.view.unit.Helper.*;

/**
 * @author Aleksey Dokshin <dant.it@gmail.com> (13.12.17).
 */
public class TurnoverUnitView extends BaseUnitView {

    private Grid<Saldo> startGrid;
    private Grid<Sale> saleGrid;
    private Grid<Pay> payGrid;
    private Grid<Saldo> endGrid;
    private Label fixDateLabel;

    public TurnoverUnitView() {
        super("turnover", "Обороты по клиенту");
    }

    @Override
    protected void initVars() {
        super.initVars();

        dtStart = LocalDate.now().withDayOfMonth(1);
        dtEnd = dtStart.plusMonths(1).minusDays(1);
    }

    @Override
    protected void buildHeadToolbar() {
        buildHeadToolbarLayout();
        buildHeadToolbarDtwsForPeriod();
        buildHeadToolbarButtons();
    }

    @Override
    protected void buildBodyContent() {
        buildStart();
        buildSale();
        buildPay();
        buildEnd();
    }

    private void buildStart() {
        startGrid = style(new Grid<>(), "start-table");
        column(startGrid, Saldo::getAccType, v -> v.title, "ITYPE", "Тип", ST_AL_LEFT, 180, null);
        column(startGrid, Saldo::getOil, v -> v != null ? v.abbreviation : "", "IDDOIL", "Н/П", ST_AL_CENTER, 100, null);
        column(startGrid, Saldo::getSaldo, v -> fmtN2(v), "DBSALDO", "Нач.остаток", ST_AL_RIGHT, 120, v -> rightNeg(v.getSaldo()));
        startGrid.setSelectionMode(Grid.SelectionMode.NONE);
        startGrid.setColumnReorderingAllowed(false);
        startGrid.setWidth("400px");
        setRowHeight(startGrid);

        bodyL.addComponents(style(new Label("Сальдо начальное"), "hhh"), startGrid);
    }

    private void buildSale() {
        saleGrid = style(new Grid<>(), "sale-table");
        column(saleGrid, Sale::getOil, v -> v.abbreviation, "IDDOIL", "Н/П", ST_AL_LEFT, 80, null);
        column(saleGrid, Sale::getPrice, v -> v == 0 ? "о/х" : fmtN2(v), "DBPRICE", "Цена (руб.)", ST_AL_RIGHT, 100, null);
        column(saleGrid, Sale::getVolume, v -> fmtN2(v), "DBVOLUME", "Кол-во (л.)", ST_AL_RIGHT, 100, null);
        column(saleGrid, Sale::getSumma, v -> v == 0 ? "---" : fmtN2(v), "DBSUMMA", "Сумма (руб.)", ST_AL_RIGHT, 120, null);
        FooterRow f = saleGrid.prependFooterRow();
        f.setStyleName("summary");
        FooterCell c1 = f.join(f.getCell("IDDOIL"), f.getCell("DBPRICE"));
        c1.setStyleName("left");
        c1.setText("Итого:");
        f.getCell("DBVOLUME").setStyleName("right");
        f.getCell("DBSUMMA").setStyleName("right");
        saleGrid.setSelectionMode(Grid.SelectionMode.NONE);
        saleGrid.setColumnReorderingAllowed(false);
        saleGrid.setWidth("400px");
        setRowHeight(saleGrid);

        bodyL.addComponents(style(new Label("Расход"), "hhh"), saleGrid);
    }

    private void buildPay() {
        payGrid = style(new Grid<>(), "pay-table");
        column(payGrid, Pay::getDtw, v -> fmtDate8(v), "DTW", "Дата", ST_AL_CENTER, 100, null);
        column(payGrid, Pay::getOil, v -> v != null ? v.abbreviation : "", "IDDOIL", "Н/П", ST_AL_CENTER, 80, null);
        column(payGrid, Pay::getVolume, v -> fmtN2(v), "DBVOLUME", "Объём (л.)", ST_AL_RIGHT, 100, null);
        column(payGrid, Pay::getSumma, v -> fmtN2(v), "DBSUMMA", "Сумма (руб.)", ST_AL_RIGHT, 120, v -> rightNeg(v.getSumma()));
        column(payGrid, Pay::getDoc, null, "CDOC", "Документ", ST_AL_LEFT, 200, null);
        FooterRow f = payGrid.prependFooterRow();
        f.setStyleName("summary");
        FooterCell c1 = f.join(f.getCell("DTW"), f.getCell("IDDOIL"));
        c1.setStyleName("left");
        c1.setText("Итого:");
        f.getCell("DBVOLUME").setStyleName("right");
        f.getCell("DBSUMMA").setStyleName("right");
        payGrid.setSelectionMode(Grid.SelectionMode.NONE);
        payGrid.setColumnReorderingAllowed(false);
        payGrid.setWidth("600px");
        setRowHeight(payGrid);

        bodyL.addComponents(style(new Label("Приход"), "hhh"), payGrid);
    }

    private void buildEnd() {
        endGrid = style(new Grid<>(), "end-table");
        column(endGrid, Saldo::getAccType, v -> v.title, "ITYPE", "Тип", ST_AL_LEFT, 180, null);
        column(endGrid, Saldo::getOil, v -> v != null ? v.title : "", "IDDOIL", "Н/П", ST_AL_CENTER, 100, null);
        column(endGrid, Saldo::getSaldo, v -> fmtN2(v), "DBSALDO", "Кон.остаток", ST_AL_RIGHT, 120, v -> rightNeg(v.getSaldo()));
        endGrid.setSelectionMode(Grid.SelectionMode.NONE);
        endGrid.setColumnReorderingAllowed(false);
        endGrid.setWidth("400px");
        setRowHeight(endGrid);

        fixDateLabel = style(new Label("<span class='star'>*</span> Дата фиксации данных: ---!", ContentMode.HTML), "note");

        bodyL.addComponents(style(new Label("Сальдо конечное"), "hhh"), endGrid,
                style(new Label("<span class='star'>*</span> Дата начала учёта клиента: <span class='fixdate'>" +
                        fmtDate8(user.getDtStart()) + "</span>!", ContentMode.HTML), "note"),
                fixDateLabel,
                style(new Label("<span class='star'>*</span> Цифры на момент последней консолидации данных!", ContentMode.HTML), "note"),
                style(new Label("<span class='star'>*</span> Задержка обработки данных, при отсутствии форс-мажорных " +
                        "обстоятельств может составлять до одного <u>рабочего</u> дня!", ContentMode.HTML), "note", "wrapped-label"));
    }

    @Override
    protected void fireOnDateChanged(int id) {
        fireOnDateChangedForPeriod(id);
    }

    @Override
    protected void fireOnCreateReport() {
        if (!checkForStartAndFix(dtStart, dtEnd)) return;

        HashMap<String, String> params = new HashMap<>();
        String s1, s2;
        params.put("dtStart", s1 = fmtDate8(dtStart));
        params.put("dtEnd", s2 = fmtDate8(dtEnd));
        //
        Request r = Request.newReport(Request.ReportType.TURNOVER, "за период с " + s1 + " по " + s2, params);
        RequestDialog dlg = new RequestDialog(r);
        getUI().addWindow(dlg);
    }

    @Override
    protected void updateButtonsState(boolean iserror) {
        super.updateButtonsState(iserror);
    }

    @Override
    protected AppModel.LogActionPage getLogPage() {
        return AppModel.LogActionPage.TURN;
    }

    @SuppressWarnings("Duplicates")
    @Override
    protected void updateData() {
        super.updateData();

        try {
            ArrayList<Saldo> list = model.loadClientSaldos(user.getFirm().id, user.getIddClient(), user.getIddClentSub(), dtStart);
            startGrid.setDataProvider(DataProvider.ofCollection(list));
            setHeightByCollection(startGrid, list);
        } catch (Exception ex) {
            logger.error("Error: ", ex);
            startGrid.setComponentError(new UserError("Ошибка запроса БД!"));
            setHeightByCollection(startGrid, null);
        }

        try {
            ArrayList<Sale> list = model.loadClientSales(user.getFirm().id, user.getIddClient(), user.getIddClentSub(), dtStart, dtEnd);
            Long vol = 0L, sum = 0L;
            for (Sale s : list) {
                vol += s.getVolume();
                sum += s.getSumma();
            }
            FooterRow f = saleGrid.getFooterRow(0);
            f.getCell("DBVOLUME").setHtml("<span" + (vol < 0 ? " class='neg'" : "") + ">" + fmtN2(vol) + "</span>");
            f.getCell("DBSUMMA").setHtml("<span" + (sum < 0 ? " class='neg'" : "") + ">" + fmtN2(sum) + "</span>");
            saleGrid.setDataProvider(DataProvider.ofCollection(list));
            setHeightByCollection(saleGrid, list);
        } catch (Exception ex) {
            logger.error("Error: ", ex);
            saleGrid.setComponentError(new UserError("Ошибка запроса БД!"));
            setHeightByCollection(saleGrid, null);
        }

        try {
            ArrayList<Pay> list = model.loadClientPays(user.getFirm().id, user.getIddClient(), user.getIddClentSub(), dtStart, dtEnd);
            Long vol = 0L, sum = 0L;
            for (Pay s : list) {
                vol += s.getVolume();
                sum += s.getSumma();
            }
            FooterRow f = payGrid.getFooterRow(0);
            f.getCell("DBVOLUME").setHtml("<span" + (vol < 0 ? " class='neg'" : "") + ">" + fmtN2(vol) + "</span>");
            f.getCell("DBSUMMA").setHtml("<span" + (sum < 0 ? " class='neg'" : "") + ">" + fmtN2(sum) + "</span>");
            payGrid.setDataProvider(DataProvider.ofCollection(list));
            setHeightByCollection(payGrid, list);
        } catch (Exception ex) {
            logger.error("Error: ", ex);
            payGrid.setComponentError(new UserError("Ошибка запроса БД!"));
            setHeightByCollection(payGrid, null);
        }

        try {
            ArrayList<Saldo> list = model.loadClientSaldos(user.getFirm().id, user.getIddClient(), user.getIddClentSub(), dtEnd);
            endGrid.setDataProvider(DataProvider.ofCollection(list));
            setHeightByCollection(endGrid, list);
        } catch (Exception ex) {
            logger.error("Error: ", ex);
            endGrid.setComponentError(new UserError("Ошибка запроса БД!"));
            setHeightByCollection(endGrid, null);
        }

        try {
            dtFix = model.getFixDate();
            fixDateLabel.setValue("<span class='star'>*</span> Дата фиксации данных: <span class='fixdate'>" + fmtDate8(dtFix) + "</span>!");
            fixDateLabel.setComponentError(null);
        } catch (Exception ex) {
            logger.error("Error: ", ex);
            fixDateLabel.setComponentError(new UserError("Ошибка запроса БД!"));
        }
    }
}
