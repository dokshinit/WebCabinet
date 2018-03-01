package app.model;

import app.AppServlet;
import app.ExError;
import app.view.UnitViewType;
import com.vaadin.server.VaadinSession;
import fbdbengine.FB_Connection;
import fbdbengine.FB_CustomException;
import fbdbengine.FB_Database;
import fbdbengine.FB_Query;
import util.StringTools;

import java.io.File;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;

import static app.AppServlet.logger;

/**
 * Модель приложения (сессии пользователя)
 *
 * @author Aleksey Dokshin <dant.it@gmail.com> (28.11.17).
 */
public class AppModel {

    // ID для аттрибута сессии в который помещается ссылка на данную модель.
    public static final String ATTR_ID = "AppModel";

    // Ссылка на сессию.
    private final VaadinSession vaadinSession;

    private final User user;

    public AppModel(VaadinSession vaadinSession) {
        this.vaadinSession = vaadinSession;
        this.user = new User();
    }

    public VaadinSession getVaadinSession() {
        return vaadinSession;
    }

    public FB_Database db() {
        return AppServlet.db();
    }

    public static AppModel get() {
        return (AppModel) VaadinSession.getCurrent().getAttribute(ATTR_ID);
    }


    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Вспомогательный инструментарий для операций с БД.
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    /** Интерфейс для вызова обработчика операции с БД. */
    @FunctionalInterface
    interface QFBBeforeTask {
        void run() throws ExError;
    }

    /** Интерфейс для вызова обработчика операции с БД. */
    @FunctionalInterface
    interface QFBTask {
        void run(final FB_Connection con) throws ExError, SQLException, Exception;
    }

    @FunctionalInterface
    interface QFBErrorTask {
        void run(Throwable ex) throws ExError;
    }

    /** Хелпер для операций с БД. */
    void QFB(FB_Connection con, QFBBeforeTask btask, QFBTask task, QFBErrorTask etask) throws ExError {
        if (btask != null) btask.run();
        boolean isextcon = con != null;
        // Если не внешнее - открываем локальное (будет закрыто автоматически с роллбэк).
        if (!isextcon) {
            try {
                con = db().connect(); // Соединение

            } catch (Exception ex) {
                if (etask != null) etask.run(ex);
                FB_CustomException e = FB_CustomException.parse(ex);
                if (e != null) throw new ExError(ex, "Ошибка подключения к БД: %s", e.name + ": " + e.message);
                logger.error("Ошибка подключения к БД!", ex);
                throw new ExError(ex, "Ошибка подключения к БД! Детальная информация в логе.");
            }
        }
        // Соединение установлено (или уже было открыто - если внешнее).
        try {
            task.run(con);

        } catch (ExError ex) {
            if (etask != null) etask.run(ex);
            logger.error(ex.getMessage());
            throw ex;

        } catch (Exception ex) {
            if (etask != null) etask.run(ex);
            FB_CustomException e = FB_CustomException.parse(ex);
            if (e != null) throw new ExError(ex, "Ошибка операции БД: %s", e.name + ": " + e.message);
            logger.error("Ошибка операции БД!", ex);
            throw new ExError(ex, "Ошибка операции БД! Детальная информация в логе.");

        } finally {
            // Если не внешнее - закрываем с роллбэк (если нужно сохранение данных - это надо сделать в теле задачи).
            if (!isextcon) FB_Connection.closeSafe(con);
        }
    }

    /** Хелпер для операций с БД. Без обработчика до соединения с БД. */
    void QFB(FB_Connection con, QFBTask task) throws ExError {
        QFB(con, null, task, null);
    }

    void QFB(QFBTask task) throws ExError {
        QFB(null, null, task, null);
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    public enum LogActionPage {
        LOGIN(1, "Авторизация"),
        //
        INFO(10, "Информация"),
        TURN(11, "Обороты"),
        TRANS(12, "Транзакции"),
        CARD(13, "Карты"),
        REQUEST(14, "Заявки");

        public int id;
        public String title;

        LogActionPage(int id, String title) {
            this.id = id;
            this.title = title;
        }

        public static LogActionPage byId(int idd) {
            for (LogActionPage item : values()) if (item.id == idd) return item;
            return null;
        }

        public static LogActionPage byUVType(UnitViewType vtype) {
            switch (vtype) {
                case INFORMATION:
                    return INFO;
                case TURNOVER:
                    return TURN;
                case TRANSACTIONS:
                    return TRANS;
                case CARDS:
                    return CARD;
                case REQUESTS:
                    return REQUEST;
                default:
                    return null;
            }
        }
    }

    public enum LogActionType {
        OPENPAGE(1, "Открытие страницы"),
        //
        LOGIN(10, "Авторизация"),
        LOGOUT(11, "Деавторизация"),
        //
        LOADDATA(20, "Загрузка данных");

        public int id;
        public String title;

        LogActionType(int id, String title) {
            this.id = id;
            this.title = title;
        }

        public static LogActionType byId(int idd) {
            for (LogActionType item : values()) if (item.id == idd) return item;
            return null;
        }
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Запись в лог событий БД.
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    public void logAction(LogActionPage page, LogActionType type) {
        try {
            QFB((con) -> {
                FB_Query q = con.execute("SELECT ID FROM W_LOG(?,?,?)", user.getId(), page.id, type.id);
                if (!q.next()) throw new ExError("Ошибка записи в лог!");
                con.commit();
                q.closeSafe();
            });
        } catch (ExError ex) {
            logger.errorf(ex, "Ошибка записи в лог! [page=%s type=%s]", page.name(), type.name());
        }
        //logger.infof("Navigator: navigateTo: %s [%s]", page.name(), type.name());
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Авторизаци \ деавторизация пользователя.
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    public User getUser() {
        return user;
    }

    public boolean isUserAuthorized() {
        return user.isAuthorized();
    }

    public void loginUser(String login, String password) throws ExError {
        synchronized (user) {
            user.logout();
            // Авторизация пользователя.
            QFB((con) -> {
                FB_Query q = con.execute("SELECT ID, IDDFIRM, IDDCLIENT, IDDSUB, CINN, CNAME, CTITLE, CSUBTITLE, CADDRESS, " +
                        " CEMAIL, CPHONE, IBFOND, IBWORK, DBCREDIT, DTSTART, CROLE, CFIRSTNAME, CLASTNAME " +
                        " FROM W_USER_LOGIN(?,?)", login, password);
                if (q.next()) {
                    user.login(q.getInteger("ID"), q.getInteger("IDDFIRM"), q.getInteger("IDDCLIENT"), q.getInteger("IDDSUB"),
                            q.getString("CINN"), q.getString("CNAME"), q.getString("CTITLE"), q.getString("CSUBTITLE"),
                            q.getString("CADDRESS"), q.getString("CEMAIL"), q.getString("CPHONE"),
                            q.getInteger("IBFOND"), q.getInteger("IBWORK"), q.getLong("DBCREDIT"), q.getLocalDate("DTSTART"),
                            q.getString("CROLE"), q.getString("CFIRSTNAME"), q.getString("CLASTNAME"));
                }
                q.closeSafe();
            });
        }
    }

    public void logoutUser() {
        synchronized (user) {
            user.logout();
        }
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Запрос даты фиксации из БД (дата актуальности данных - актуальны по нее включительно).
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    public LocalDate getFixDate() throws ExError {
        LocalDate[] fixDate = {null};
        QFB((con) -> {
            FB_Query q = con.execute("SELECT FIXDATE FROM W_GETFIXDATE");
            if (q.next()) {
                fixDate[0] = q.getLocalDate("FIXDATE");
            }
            q.closeSafe();
        });
        return fixDate[0];
    }


    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    //
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    public ArrayList<Azs> loadAzs(Integer iddfirm, LocalDate dtstart, LocalDate dtend) throws ExError {
        //logger.info("LOAD AZS LIST!");
        final ArrayList<Azs> list = new ArrayList<>();
        QFB((con) -> {
            FB_Query q = con.execute("SELECT IDD, CTITLE, CADDRESS, IDDFIRM, IDDFIRMMC, IBWORK "
                    + "FROM W_AZS_LIST(?,?,?) ORDER BY IDD", iddfirm, dtstart, dtend);
            while (q.next()) {
                list.add(new Azs(q.getInteger("IDD"), q.getString("CTITLE"), q.getString("CADDRESS"),
                        q.getInteger("IDDFIRM"), q.getInteger("IDDFIRMMC"), q.getInteger("IBWORK")));
            }
            q.closeSafe();
        });
        return list;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    //
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    public ArrayList<Contract> loadClientContracts(Integer iddfirm, Integer iddclient, Integer iddsub, LocalDate dtw) throws ExError {
        //logger.info("LOAD CONTRACT LIST!");
        final ArrayList<Contract> list = new ArrayList<>();
        QFB((con) -> {
            FB_Query q = con.execute("SELECT IDD, CNUMBER, ITYPE, DTSIGN, DTSTARTFACT, DTENDFACT, IEXPIRED "
                    + "FROM W_CLIENT_CONTRACT_VIEW(?,?,?,?) ORDER BY IDD", iddfirm, iddclient, iddsub, dtw);
            while (q.next()) {
                list.add(new Contract(q.getInteger("IDD"), q.getString("CNUMBER"), q.getInteger("ITYPE"), q.getLocalDate("DTSIGN"),
                        q.getLocalDate("DTSTARTFACT"), q.getLocalDate("DTENDFACT"), q.getInteger("IEXPIRED")));
            }
            q.closeSafe();
        });
        return list;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    //
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    public ArrayList<Acc> loadClientAccs(Integer iddfirm, Integer iddclient, Integer iddsub, LocalDate dtw) throws ExError {
        //logger.info("LOAD ACCS LIST!");
        final ArrayList<Acc> list = new ArrayList<>();
        QFB((con) -> {
            FB_Query q = con.execute("SELECT IDD, IDDFIRM, IDDCLIENT, IDDSUB, IACCTYPE, IDDOIL, DBSTART, DBTRANS, DBPAY, DBEND "
                    + "FROM W_CLIENT_ACC_VIEW(?,?,?,?) ORDER BY IDD", iddfirm, iddclient, iddsub, dtw);
            while (q.next()) {
                list.add(new Acc(q.getInteger("IDD"), q.getInteger("IDDFIRM"), q.getInteger("IDDCLIENT"), q.getInteger("IDDSUB"),
                        q.getInteger("IACCTYPE"), q.getInteger("IDDOIL"),
                        q.getLong("DBSTART"), q.getLong("DBTRANS"), q.getLong("DBPAY"), q.getLong("DBEND")));
            }
            q.closeSafe();
        });
        return list;
    }


    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    //
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    public ArrayList<Card> loadClientCards(Integer iddfirm, Integer iddclient, Integer iddsub, LocalDate dtw, Card.WorkState workstate,
                                           int offset, int limit, String sort) throws ExError {
        //logger.info("LOAD CARDS LIST!");
        final ArrayList<Card> list = new ArrayList<>();
        QFB((con) -> {
            FB_Query q = con.execute("SELECT FIRST " + limit + " SKIP " + offset + " "
                            + " DTW, DTWEND, IDD, IACCTYPE, IBWORK, DTPAY, CDRIVER, CCAR, DBDAYLIMIT, CCOMMENT "
                            + " FROM W_CLIENT_CARD_VIEW(?,?,?,?,?) "
                            + (StringTools.isEmptySafe(sort) ? "" : " ORDER BY " + sort),
                    iddfirm, iddclient, iddsub, dtw, workstate == null ? null : workstate.id);
            while (q.next()) {
                list.add(new Card(q.getLocalDate("DTW"), q.getLocalDate("DTWEND"), q.getString("IDD"),
                        q.getInteger("IACCTYPE"), q.getInteger("IBWORK"), q.getLocalDate("DTPAY"), q.getString("CDRIVER"), q.getString("CCAR"),
                        q.getLong("DBDAYLIMIT"), q.getString("CCOMMENT")));
            }
            q.closeSafe();
        });
        return list;
    }

    public int loadClientCardsCount(Integer iddfirm, Integer iddclient, Integer iddsub, LocalDate dtw, Card.WorkState workstate) throws ExError {
        final int[] count = {-1};
        QFB((con) -> {
            FB_Query q = con.execute("SELECT count(*) FROM W_CLIENT_CARD_VIEW(?,?,?,?,?)",
                    iddfirm, iddclient, iddsub, dtw, workstate == null ? null : workstate.id);
            if (q.next()) count[0] = q.getInteger(1);
            q.closeSafe();
        });
        return count[0];
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    //
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    public ArrayList<Transaction> loadClientTransactions(Integer iddfirm, Integer iddclient, Integer iddsub, LocalDate dtstart,
                                                         LocalDate dtend, Integer iddazs,
                                                         int offset, int limit, String sort) throws ExError {
        ArrayList<Transaction> list = new ArrayList<>();
        QFB((con) -> {
            FB_Query q = con.execute("SELECT FIRST " + limit + " SKIP " + offset + " "
                            + "DTSTART, DTEND, IDDCARD, CCARD, IDD, IDDAZS, IDDTRK, IDDOIL, IACCTYPE, DBPRICE, DBVOLREQ, DBVOLUME, DBSUMMA "
                            + "FROM W_CLIENT_TRANS_VIEW(?,?,?,NULL, ?,?,?) "
                            + (StringTools.isEmptySafe(sort) ? "" : " ORDER BY " + sort),
                    iddfirm, iddclient, iddsub, dtstart, dtend, iddazs);
            while (q.next()) {
                list.add(new Transaction(
                        q.getLocalDateTime("DTSTART"),
                        q.getLocalDateTime("DTEND"),
                        q.getString("IDDCARD"),
                        q.getString("CCARD"),
                        q.getInteger("IDD"),
                        q.getInteger("IDDAZS"),
                        q.getInteger("IDDTRK"),
                        q.getInteger("IDDOIL"),
                        q.getInteger("IACCTYPE"),
                        q.getLong("DBPRICE"),
                        q.getLong("DBVOLREQ"),
                        q.getLong("DBVOLUME"),
                        q.getLong("DBSUMMA")));
            }
            q.closeSafe();
        });
        return list;
    }

    public int loadClientTransactionsCount(Integer iddfirm, Integer iddclient, Integer iddsub, LocalDate dtstart, LocalDate dtend,
                                           Integer iddazs) throws ExError {
        final int[] count = {-1};
        QFB((con) -> {
            FB_Query q = con.execute("SELECT count(*) FROM W_CLIENT_TRANS_VIEW(?,?,?,NULL, ?,?,?)",
                    iddfirm, iddclient, iddsub, dtstart, dtend, iddazs);
            if (q.next()) count[0] = q.getInteger(1);
            q.closeSafe();
        });
        return count[0];
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    //
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    public ArrayList<Saldo> loadClientSaldos(Integer iddfirm, Integer iddclient, Integer iddsub, LocalDate dtw) throws ExError {
        //logger.info("LOAD SALDO LIST!" + dtStart);
        final ArrayList<Saldo> list = new ArrayList<>();
        QFB((con) -> {
            FB_Query q = con.execute("SELECT IDDACC, IACCTYPE, IDDOIL, DBSALDO "
                            + "FROM W_CLIENT_COMMON_SALDO_VIEW(?,?,?,?) ORDER BY IACCTYPE, IDDOIL",
                    iddfirm, iddclient, iddsub, dtw);
            while (q.next()) {
                list.add(new Saldo(q.getInteger("IDDACC"), q.getInteger("IACCTYPE"), q.getInteger("IDDOIL"),
                        q.getLong("DBSALDO")));
            }
            q.closeSafe();
        });
        return list;
    }

    public ArrayList<Sale> loadClientSales(Integer iddfirm, Integer iddclient, Integer iddsub, LocalDate dtstart, LocalDate dtend) throws ExError {
        //logger.info("LOAD SALE LIST!");
        final ArrayList<Sale> list = new ArrayList<>();
        QFB((con) -> {
            FB_Query q = con.execute("SELECT IACCTYPE, IDDSUB, IDDOIL, DBPRICE, DBVOLUME, DBSUMMA "
                            + "FROM W_CLIENT_COMMON_SALE_VIEW(?,?,?,?,?) ORDER BY IDDOIL, DBPRICE",
                    iddfirm, iddclient, iddsub, dtstart, dtend);
            while (q.next()) {
                list.add(new Sale(q.getInteger("IACCTYPE"), q.getInteger("IDDOIL"),
                        q.getLong("DBPRICE"), q.getLong("DBVOLUME"), q.getLong("DBSUMMA")));
            }
            q.closeSafe();
        });
        return list;
    }

    public ArrayList<Pay> loadClientPays(Integer iddfirm, Integer iddclient, Integer iddsub, LocalDate dtstart, LocalDate dtend) throws ExError {
        //logger.info("LOAD PAY LIST!");
        final ArrayList<Pay> list = new ArrayList<>();
        QFB((con) -> {
            FB_Query q = con.execute("SELECT DTDOC, CDOC, IACCTYPE, IDDOIL, DBVOLUME, DBSUMMA "
                    + "FROM W_CLIENT_COMMON_PAY_VIEW(?,?,?,?,?) ORDER BY DTDOC, CDOC", iddfirm, iddclient, iddsub, dtstart, dtend);
            while (q.next()) {
                list.add(new Pay(q.getLocalDate("DTDOC"), q.getString("CDOC"), q.getInteger("IACCTYPE"), q.getInteger("IDDOIL"),
                        q.getLong("DBVOLUME"), q.getLong("DBSUMMA")));
            }
            q.closeSafe();
        });
        return list;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    //
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    public ArrayList<Request> loadRequests(LocalDate dtstart, LocalDate dtend, Request.Type type, Request.Mode mode,
                                           int offset, int limit, String sort) throws ExError {
        final ArrayList<Request> list = new ArrayList<>();
        QFB((con) -> {
            Integer itype = type == null ? null : type.id;
            Integer imode = mode == null ? 0 : mode.id;
            FB_Query q = con.execute("SELECT FIRST " + limit + " SKIP " + offset + " "
                            + " ID, DTCREATE, ITYPE, ISUBTYPE, CPARAMSTITLE, CPARAMS, CCOMMENT, ISTATE, DTPROCESS, CFILENAME, "
                            + " IFILESIZE, ISENDTRYREMAIN, DTSEND, CRESULT FROM W_REQUEST_VIEW(?, ?,?,?,?) "
                            + (StringTools.isEmptySafe(sort) ? "" : " ORDER BY " + sort),
                    user.getId(), dtstart, dtend, itype, imode);
            while (q.next()) {
                list.add(new Request(
                        q.getInteger("ID"), q.getLocalDateTime("DTCREATE"),
                        q.getInteger("ITYPE"), q.getInteger("ISUBTYPE"), q.getString("CPARAMSTITLE"), q.getString("CPARAMS"),
                        q.getString("CCOMMENT"), q.getInteger("ISTATE"),
                        q.getLocalDateTime("DTPROCESS"), q.getString("CFILENAME"), q.getInteger("IFILESIZE"),
                        q.getInteger("ISENDTRYREMAIN"), q.getLocalDateTime("DTSEND"), q.getString("CRESULT")));
            }
            q.closeSafe();
        });
        return list;
    }

    public int loadRequestsCount(LocalDate dtstart, LocalDate dtend, Request.Type type, Request.Mode mode) throws ExError {
        final int[] count = {0};
        QFB((con) -> {
            Integer itype = type == null ? null : type.id;
            Integer imode = mode == null ? 0 : mode.id;
            FB_Query q = con.execute("SELECT count(*) FROM W_REQUEST_VIEW(?, ?,?,?,?)",
                    user.getId(), dtstart, dtend, itype, imode);
            if (q.next()) count[0] = q.getInteger(1);
            q.closeSafe();
        });
        return count[0];
    }

    public void createRequest(Request req) throws ExError {
        QFB((con) -> {
            FB_Query q = con.execute("SELECT ID, DTCREATE FROM W_REQUEST_CREATE(?,?,?,?,?,?,?)",
                    user.getId(), req.getType().id, req.getIdSubType(), req.getParamsTitle(), req.getParams(), req.getComment(), req.getSendTryRemain());
            if (!q.next()) throw new ExError("Ошибка создания заявки!");
            req.updateByCreate(q.getInteger("ID"), q.getLocalDateTime("DTCREATE"));
            q.closeSafe();

            if (req.getType() == Request.Type.CARDCHANGE && !req.getCardItems().isEmpty()) {
                q = con.query("SELECT ID FROM W_REQUEST_ADDCARD(?,?,?,?,?,?,?,?)");

                for (Request.CardItem it : req.getCardItems()) {
                    Integer wrk = it.getWork() == null ? null : it.getWork().id;
                    q.execute(req.getId(), it.getIdd(), it.getDriver(), it.getCar(), it.getDayLimit(), it.getOilLimit(), it.getAzsLimit(), wrk);
                    if (!q.next()) throw new ExError("Ошибка добавления карты в заявку!");
                }
                q.closeSafe();
            }
            con.commit();
        });
    }
}
