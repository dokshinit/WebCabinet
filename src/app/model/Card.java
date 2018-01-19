package app.model;

import java.time.LocalDate;

/**
 * @author Aleksey Dokshin <dant.it@gmail.com> (28.12.17).
 */
public class Card {

    public enum WorkState {
        WORK(1, "Рабочая"),
        LOCK(0, "Запрет"),
        ARREST(-1, "Арест"),
        DESTROY(-2, "Изъята");

        public int id;
        public String title;

        WorkState(int id, String title) {
            this.id = id;
            this.title = title;
        }

        public static WorkState byId(int id) {
            for (WorkState item : values()) if (item.id == id) return item;
            return null;
        }

        public String getTitle() {
            return title;
        }
    }

    private LocalDate dtw, dtwEnd;
    private String iddCard;
    private AccType accType;
    private WorkState workState;
    private String driver, car, comment;
    private Long dbDayLimit; // 10*мл.

    public Card(LocalDate dtw, LocalDate dtwEnd, String iddCard, Integer iAccType, Integer iWork, String driver, String car, Long dbDayLimit, String comment) {
        this.dtw = dtw;
        this.dtwEnd = dtwEnd;
        this.iddCard = iddCard.substring(3);
        this.accType = AccType.byId(iAccType);
        this.workState = WorkState.byId(iWork);
        this.driver = driver;
        this.car = car;
        this.dbDayLimit = dbDayLimit;
        this.comment = comment;
    }

    public LocalDate getDtw() {
        return dtw;
    }

    public LocalDate getDtwEnd() {
        return dtwEnd;
    }

    public String getIddCard() {
        return iddCard;
    }

    public AccType getAccType() {
        return accType;
    }

    public WorkState getWorkState() {
        return workState;
    }

    public String getDriver() {
        return driver;
    }

    public String getCar() {
        return car;
    }

    public String getComment() {
        return comment;
    }

    public Long getDbDayLimit() {
        return dbDayLimit;
    }
}
