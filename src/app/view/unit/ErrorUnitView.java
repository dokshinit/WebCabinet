package app.view.unit;

import app.AppUI;
import app.view.AbstractUnitView;
import com.vaadin.server.Responsive;
import com.vaadin.ui.*;
import com.vaadin.ui.themes.ValoTheme;

/**
 * @author Aleksey Dokshin <dant.it@gmail.com> (13.12.17).
 */
public class ErrorUnitView extends BaseUnitView {

    public ErrorUnitView() {
        super("error", "Ошибка");
    }

    @Override
    protected void buildBodyContent() {
        bodyL.addComponent(new Label("Ошибка приложения!"));
    }
}
