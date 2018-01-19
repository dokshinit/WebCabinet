package app.model;

/**
 * @author Aleksey Dokshin <dant.it@gmail.com> (22.12.17).
 */
public enum Oil {

    AI80(3, "Бензин АИ-80", "АИ-80"),
    AI92(4, "Бензин АИ-92", "АИ-92"),
    AI95(5, "Бензин АИ-95", "АИ-95"),
    DT(6, "Дизельное топливо", "ДТ"),
    GAS(15, "Сжиженный газ", "СУГ");

    public final int id;
    public final String title, abbreviation;

    Oil(int idd, String title, String abbreviation) {
        this.id = idd;
        this.title = title;
        this.abbreviation = abbreviation;
    }

    public static Oil byId(int idd) {
        for (Oil item : values()) if (item.id == idd) return item;
        return null;
    }
}
