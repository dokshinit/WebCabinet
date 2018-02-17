package app.view.unit;

import app.model.Card;
import app.sizer.SizeReporter;
import com.vaadin.data.ValueProvider;
import com.vaadin.server.Sizeable;
import com.vaadin.ui.AbstractComponent;
import com.vaadin.ui.Grid;
import com.vaadin.ui.StyleGenerator;
import util.NumberTools;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collection;

/**
 * @author Aleksey Dokshin <dant.it@gmail.com> (27.12.17).
 */
public class Helper {

    public static final int ROW_HEIGHT = 26;

    public static void setRowHeight(Grid g) {
        g.setRowHeight(ROW_HEIGHT);
    }

    public static void setHeightByCollection(Grid g, Collection col) {
        double bodyH = (col == null ? 0 : col.size()) * g.getBodyRowHeight();
        double headH = g.getHeaderRowCount() * g.getHeaderRowHeight();
        double footH = g.getFooterRowCount() * g.getFooterRowHeight();
        g.setHeight((float) (headH + bodyH + footH), Sizeable.Unit.PIXELS);
    }


    public static String fmtN2(Long num) {
        return NumberTools.format2.format(num / 100.0);
    }

    public static String fmtN3_2(Long num) {
        return NumberTools.format2.format(Math.round(num / 10.0) / 100.0);
    }

    public static String fmtN3(Long num) {
        return NumberTools.format3.format(num / 1000.0);
    }

    public static final DateTimeFormatter DT_FORMATTER_DDMMYYYYHHMMSS = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss");
    public static final DateTimeFormatter DATE_FORMATTER_DDMMYYYY = DateTimeFormatter.ofPattern("dd.MM.yyyy");
    public static final LocalDate DATE_2100 = LocalDate.of(2100, 01, 01);

    public static String fmtDate8(LocalDate dt) {
        if (DATE_2100.equals(dt)) return "---";
        return DATE_FORMATTER_DDMMYYYY.format(dt);
    }

    public static String fmtDT86(LocalDateTime dt) {
        if (DATE_2100.equals(dt)) return "---";
        return DT_FORMATTER_DDMMYYYYHHMMSS.format(dt);
    }

    public static <T extends AbstractComponent> T style(T comp, String... styles) {
        for (String s : styles) comp.addStyleName(s);
        return comp;
    }

    public static StyleGenerator ST_AL_CENTER = (t) -> "v-align-center";
    public static StyleGenerator ST_AL_RIGHT = (t) -> "v-align-right";
    public static StyleGenerator ST_AL_LEFT = (t) -> "v-align-left";

    public static <T, V> Grid.Column<T, V> gridColumn(Grid<T> grid, ValueProvider<T, V> prov, ValueProvider<V, String> pres,
                                                      String id, String title, StyleGenerator<T> style) {
        Grid.Column<T, V> c = pres == null ? grid.addColumn(prov) : grid.addColumn(prov, pres);
        c.setId(id).setCaption(title);
        if (style != null) c.setStyleGenerator(style);
        return c;
    }

    public static <T> void updateGridColumns(Grid<T> grid, SizeReporter sizeReporter) {
        double ww = 0;
        for (Grid.Column<T, ?> c : grid.getColumns()) if (!c.isWidthUndefined()) ww += c.getWidth();
        if (ww > 0 && ww < sizeReporter.getWidth()) {
            for (Grid.Column<T, ?> c : grid.getColumns()) c.setWidthUndefined();
        }
    }

}
