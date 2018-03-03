package app;

import app.view.MainView;
import app.view.UnitViewType;
import com.vaadin.navigator.Navigator;
import com.vaadin.navigator.View;
import com.vaadin.navigator.ViewChangeListener;
import com.vaadin.navigator.ViewProvider;
import com.vaadin.ui.ComponentContainer;
import com.vaadin.ui.UI;

@SuppressWarnings("serial")
public class AppNavigator extends Navigator {

    private static final UnitViewType ERROR_VIEW = UnitViewType.ERROR;
    private static final UnitViewType DEFAULT_VIEW = UnitViewType.INFORMATION;
    private ViewProvider errorViewProvider, defaultViewProvider;
    private MainView mainView;

    public AppNavigator(final MainView mainView, final ComponentContainer container) {
        super(UI.getCurrent(), container);
        this.mainView = mainView;
        initViewChangeListener();
        initViewProviders();
    }

    private void initViewChangeListener() {
        addViewChangeListener(new ViewChangeListener() {

            @Override
            public boolean beforeViewChange(final ViewChangeEvent event) {
                // Since there's no conditions in switching between the views we can always return true.
                return true;
            }

            @Override
            public void afterViewChange(final ViewChangeEvent event) {
                String name = event.getViewName();
                if (name.isEmpty()) name = DEFAULT_VIEW.getViewName();
                UnitViewType view = UnitViewType.getByViewName(name);
                //AppServlet.logger.infof("NavigatorView: After change: %s", view);
                mainView.fireOnViewChanged(view, event.getNewView());
            }
        });
    }

    private void initViewProviders() {
        // A dedicated view provider is added for each separate view type
        for (final UnitViewType viewType : UnitViewType.values()) {
            ViewProvider viewProvider = new ClassBasedViewProvider(viewType.getViewName(), viewType.getViewClass()) {

                // This field caches an already initialized view instance if the view should be cached (stateful views).
                private View cachedInstance;

                @Override
                public View getView(final String viewName) {
                    View result = null;
                    if (viewType.getViewName().equals(viewName)) {
                        if (viewType.isStateful()) {
                            // Stateful views byId lazily instantiated
                            if (cachedInstance == null) cachedInstance = super.getView(viewType.getViewName());
                            result = cachedInstance;
                        } else {
                            // Non-stateful views byId instantiated every time they're navigated to
                            result = super.getView(viewType.getViewName());
                        }
                    }
                    return result;
                }
            };

            if (viewType == ERROR_VIEW) errorViewProvider = viewProvider;
            if (viewType == DEFAULT_VIEW) defaultViewProvider = viewProvider;
            addProvider(viewProvider);
        }

        // Провайдер для базового адреса (без субстраниц).
        addProvider(new ViewProvider() {
            @Override
            public String getViewName(final String viewAndParameters) {
                return "";
            }

            @Override
            public View getView(final String viewName) {
                return defaultViewProvider.getView(DEFAULT_VIEW.getViewName());
            }
        });

        // Провайдер для ошибок.
        setErrorProvider(new ViewProvider() {
            @Override
            public String getViewName(final String viewAndParameters) {
                return ERROR_VIEW.getViewName();
            }

            @Override
            public View getView(final String viewName) {
                return errorViewProvider.getView(ERROR_VIEW.getViewName());
            }
        });
    }

    @Override
    public AppUI getUI() {
        return (AppUI) super.getUI();
    }

//    @Override
//    public void navigateTo(String navigationState) {
//        String name = navigationState;
//        if (name.isEmpty()) name = DEFAULT_VIEW.getViewName();
//        UnitViewType v = UnitViewType.getByViewName(name);
//        if (v == null) name = DEFAULT_VIEW.getViewName();
//        v = UnitViewType.getByViewName(name);
//        getUI().logAction(v, AppModel.LogActionType.OPENPAGE);
//        super.navigateTo(navigationState);
//    }
}
