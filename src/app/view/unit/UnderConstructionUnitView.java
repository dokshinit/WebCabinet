package app.view.unit;

import app.AppUI;
import app.model.AppModel;
import app.view.AbstractUnitView;
import com.vaadin.server.Responsive;
import com.vaadin.shared.ui.ContentMode;
import com.vaadin.ui.*;
import com.vaadin.ui.themes.ValoTheme;

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