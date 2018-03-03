package app.view.unit;

import com.vaadin.shared.ui.ContentMode;
import com.vaadin.ui.*;

/**
 * @author Aleksey Dokshin <dant.it@gmail.com> (13.12.17).
 */
public class UnderConstructionUnitView extends BaseUnitView {

    public UnderConstructionUnitView() {
        super("underconsruction", "В разработке");
    }

    @Override
    protected void buildBodyContent() {
        bodyL.addComponent(new Label("<span style='color: #D06060'>Раздел находится в разработке!</span>", ContentMode.HTML));
    }
}