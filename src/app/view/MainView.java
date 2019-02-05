package app.view;

import app.AppNavigator;
import app.AppUI;
import app.model.AppModel;
import com.vaadin.navigator.View;
import com.vaadin.server.Page;
import com.vaadin.server.Responsive;
import com.vaadin.ui.*;

import static app.view.unit.UHelper.*;

@SuppressWarnings("serial")
public class MainView extends HorizontalLayout {

    final MainViewMenu menu;
    final CssLayout content;
    final AppNavigator navigator;
    UnitViewType unitViewType = null;
    AbstractUnitView unitView = null;

    public MainView() {
        setSizeFull();
        setSpacing(false);
        setMargin(false);
        addStyleName("main-view");
        Responsive.makeResponsive(this);

        addComponent(menu = new MainViewMenu());

        content = style(new CssLayout(), "unitview-content");
        content.setSizeFull();
        addComponent(content);
        setExpandRatio(content, 1.0f);

        navigator = new AppNavigator(this, content);

        // Для того, чтобы меню скрывалось (если всплывающее) при клике на контенте.
        content.addLayoutClickListener((event) -> menu.hideMenu());
    }

    @Override
    public AppUI getUI() {
        return (AppUI) super.getUI();
    }

    public void fireOnBrowserResized(final Page.BrowserWindowResizeEvent event) {
        if (unitView != null) unitView.fireOnBrowserResized(event);
    }

    public void fireOnViewChanged(UnitViewType vtype, View view) {
        menu.fireOnViewChanged(vtype);
        AppUI ui = getUI();
        ui.fireOnBrowserResized(null);
        ui.closeOpenedWindows();
        unitViewType = vtype;
        unitView = (AbstractUnitView) view;
        ui.logAction(vtype, AppModel.LogActionType.OPENPAGE);
    }

    public UnitViewType getUnitViewType() {
        return unitViewType;
    }

    public AbstractUnitView getUnitView() {
        return unitView;
    }

    @Override
    public void detach() {
        navigator.destroy(); // Для того, чтобы отключать навигатор от UI.
        super.detach();
    }
}
