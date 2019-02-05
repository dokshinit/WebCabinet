package app.view.unit;

import app.sizer.SizeReporter;
import com.vaadin.data.ValueProvider;
import com.vaadin.server.Sizeable;
import com.vaadin.ui.AbstractComponent;
import com.vaadin.ui.Grid;
import com.vaadin.ui.StyleGenerator;

import java.util.Collection;

/**
 * @author Aleksey Dokshin <dant.it@gmail.com> (27.12.17).
 */
public class UHelper {

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

    private static double boundedGridColumnWidth(Grid.Column col) {
        if (col.isHidden()) return 0;
        double w = col.isWidthUndefined() ? 0 : col.getWidth();
        double min = col.getMinimumWidth();
        if (min >= 0 && w < min) w = min;
        double max = col.getMaximumWidth();
        if (max >= 0 && w > max) w = max;
        return w;
    }

//    private static LoggerExt logger;
//
//    static {
//        logger = LoggerExt.getNewLogger("UHelper").enable(true);
//    }

    public static <T> void updateGridColumns(Grid<T> grid, SizeReporter sizeReporter) {
        double ww = 0;
        // Вычисляем фактическую суммарную ширину колонок (у тех, что не определены - берем по ограничениям, если есть.
        for (Grid.Column<T, ?> c : grid.getColumns()) ww += boundedGridColumnWidth(c);
        // Если есть минимальная ширина, и она больше ширины таблицы - сбрасываем ширины изменяемых столбцов для пересчёта.
        if (ww > 0 && ww < sizeReporter.getWidth()) {
            //logger.info("UPD-COLS: ww=" + ww + " size=" + sizeReporter.getWidth());
            for (Grid.Column<T, ?> c : grid.getColumns()) {
                if (c.isHidden()) continue;
                double min = c.getMinimumWidth();
                double max = c.getMaximumWidth();
                double w = c.getWidth();
                if (min >= 0 && max >= 0 && min == max) { // Fixed column.
                    if (c.getWidth() != min) c.setWidth(min);
                } else {
                    if (!c.isWidthUndefined()) c.setWidthUndefined();
                }
                //logger.info("[" + c.getId() + "]: min=" + min + " max=" + max + " w=" + w + " -> w=" + c.getWidth());
            }
        }
    }

}
