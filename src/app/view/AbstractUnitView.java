package app.view;

import com.vaadin.navigator.View;
import com.vaadin.server.Page;

/**
 * @author Aleksey Dokshin <dant.it@gmail.com> (13.12.17).
 */
public interface AbstractUnitView extends View {

    default void fireOnBrowserResized(Page.BrowserWindowResizeEvent event) {
    }
}
