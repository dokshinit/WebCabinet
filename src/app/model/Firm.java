package app.model;

/**
 * @author Aleksey Dokshin <dant.it@gmail.com> (20.12.17).
 */
public enum Firm {

    TP(1, "ТП", "Транзит", "ООО \"Транзит-Плюс\""),
    K2000(2, "К2", "Компания", "ООО \"Компания 2000\""),
    SNOW(103, "СН", "Снежинка", "ООО \"Снежинка\"");

    public final int id;
    public final String abbreviation;
    public final String name;
    public final String title;

    Firm(int idd, String abbreviation, String name, String title) {
        this.id = idd;
        this.abbreviation = abbreviation;
        this.name = name;
        this.title = title;
    }

    public static Firm byId(int idd) {
        for (Firm item : values()) if (item.id == idd) return item;
        return null;
    }
}
