package app.view;

import com.vaadin.ui.VerticalLayout;

/**
 * @author Aleksey Dokshin <dant.it@gmail.com> (12.01.18).
 */
public class StaticTable extends VerticalLayout {

    public class Cell {

    }

    public class Row {
        public Row() {
        }
    }


    private final int columnCount;
    private int rowCount;
    private Row headerRow;

    public StaticTable(int columns) {
        super();

        columnCount = columns;
        rowCount = 0;
    }

    public StaticTable setColumn(String title, String width) {
        return this;
    }

    //public Row addRow();
}
