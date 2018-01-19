package app.view.unit;

import app.AppUI;
import app.view.AbstractUnitView;
import com.vaadin.server.Responsive;
import com.vaadin.shared.ui.ContentMode;
import com.vaadin.ui.*;
import com.vaadin.ui.themes.ValoTheme;

/**
 * @author Aleksey Dokshin <dant.it@gmail.com> (13.12.17).
 */
public class UnderConstructionUnitView extends Panel implements AbstractUnitView {

    protected CssLayout layout;
    protected final VerticalLayout root;

    public UnderConstructionUnitView() {
        super();

        addStyleName(ValoTheme.PANEL_BORDERLESS);
        setSizeFull();

        root = new VerticalLayout();
        root.setSizeFull();
        root.setSpacing(false);
        root.addStyleName("undeconstruction-unitview");
        setContent(root);
        Responsive.makeResponsive(root);

        Component content = buildContent();
        root.addComponent(content);
        root.setExpandRatio(content, 1);

        root.addLayoutClickListener((e) -> fireOnLayoutClick());
    }

    @Override
    public AppUI getUI() {
        return (AppUI) super.getUI();
    }

    protected void fireOnLayoutClick() {
        ((AppUI) getUI()).closeOpenedWindows();
    }

    protected Component buildContent() {
        layout = new CssLayout();
        layout.addStyleName("undeconstruction-headLayout");
        Responsive.makeResponsive(layout);

        layout.addComponent(new Label("<span style='color: #D06060'>Раздел находится в разработке!</span>", ContentMode.HTML));

        return layout;
    }
}