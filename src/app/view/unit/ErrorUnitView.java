package app.view.unit;

import app.model.AppModel;
import com.vaadin.ui.*;

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

    @Override
    protected AppModel.LogActionPage getLogPage() {
        return AppModel.LogActionPage.ERROR;
    }
}
