package app.view;

import app.view.unit.*;
import com.vaadin.icons.VaadinIcons;
import com.vaadin.navigator.View;
import com.vaadin.server.Resource;

public enum UnitViewType {
    INFORMATION("information", "Информация", InformationUnitView.class, VaadinIcons.HOME, true),
    TURNOVER("turnover", "Обороты", TurnoverUnitView.class, VaadinIcons.INVOICE, true),
    TRANSACTIONS("transactions", "Транзакции", TransactionUnitView.class, VaadinIcons.TABLE, true),
    CARDS("cards", "Карты", CardUnitView.class, VaadinIcons.USER_CARD, true),
    REQUESTS("requests", "Заявки", RequestUnitView.class, VaadinIcons.RECORDS, true),
    //
    ERROR("error", "Ошибка", ErrorUnitView.class, VaadinIcons.WARNING, false);

    private final String viewTitle;
    private final String viewName;
    private final Class<? extends View> viewClass;
    private final Resource icon;
    private final boolean stateful;

    UnitViewType(final String viewName,
                 final String viewTitle,
                 final Class<? extends View> viewClass, final Resource icon,
                 final boolean stateful) {
        this.viewName = viewName;
        this.viewTitle = viewTitle;
        this.viewClass = viewClass;
        this.icon = icon;
        this.stateful = stateful;
    }

    public boolean isStateful() {
        return stateful;
    }

    public String getViewName() {
        return viewName;
    }

    public String getViewTitle() {
        return viewTitle;
    }

    public Class<? extends View> getViewClass() {
        return viewClass;
    }

    public Resource getIcon() {
        return icon;
    }

    public static UnitViewType getByViewName(final String viewName) {
        UnitViewType result = null;
        for (UnitViewType viewType : values()) {
            if (viewType.getViewName().equals(viewName)) {
                result = viewType;
                break;
            }
        }
        return result;
    }
}
