package app.model;

import app.AppServlet;
import app.ExError;
import com.vaadin.server.VaadinSession;
import fbdbengine.FB_Connection;
import fbdbengine.FB_CustomException;
import fbdbengine.FB_Database;
import fbdbengine.FB_Query;
import util.StringTools;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;

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
    interface QBeforeTask {
        void run() throws ExError;
    }

    /** Интерфейс для вызова обработчика операции с БД. */
    @FunctionalInterface
    interface QTask {
        void run(final FB_Connection con) throws ExError, SQLException, Exception;
    }

    @FunctionalInterface
    interface QErrorTask {
        void run(Throwable ex) throws ExError;
    }

    /** Хелпер для операций с БД. */
    private void Q(QBeforeTask btask, QTask task, QErrorTask etask) throws ExError {
        if (btask != null) btask.run();

        // Соединение будет закрыто автоматически! (роллбэк)
        try (FB_Connection con = db().connect()) {

            task.run(con);

        } catch (ExError ex) {
            if (etask != null) etask.run(ex);
            logger.error(ex.getMessage());
            throw ex;

        } catch (Exception ex) {
            if (etask != null) etask.run(ex);
            FB_CustomException e = FB_CustomException.parse(ex);
            if (e != null) throw new ExError(ex, "Ошибка операции БД: %s", e.name + ": " + e.message);
            logger.error(ex.getMessage());
            throw new ExError(ex, "Ошибка операции БД! Детальная информация в логе.");
        }
    }

    /** Хелпер для операций с БД. Без обработчика до соединения с БД. */
    private void Q(QTask task) throws ExError {
        Q(null, task, null);
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
            Q((con) -> {
                FB_Query q = con.execute("SELECT IDDFIRM, IDDCLIENT, IDDSUB, CINN, CNAME, CTITLE, CSUBTITLE, CADDRESS, " +
                        " CEMAIL, CPHONE, IBFOND, IBWORK, DBCREDIT, CROLE, CFIRSTNAME, CLASTNAME " +
                        " FROM W_USER_LOGIN(?,?)", login, password);
                if (!q.next()) throw new ExError("Ошибка запроса авторизации!");
                user.login(q.getInteger("IDDFIRM"), q.getInteger("IDDCLIENT"), q.getInteger("IDDSUB"),
                        q.getString("CINN"), q.getString("CNAME"), q.getString("CTITLE"), q.getString("CSUBTITLE"),
                        q.getString("CADDRESS"), q.getString("CEMAIL"), q.getString("CPHONE"),
                        q.getInteger("IBFOND"), q.getInteger("IBWORK"), q.getLong("DBCREDIT"),
                        q.getString("CROLE"), q.getString("CFIRSTNAME"), q.getString("CLASTNAME"));
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
    //
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    public ArrayList<Azs> loadAzs(Integer iddfirm, LocalDate dtw, LocalDate dtwend) throws ExError {
        logger.info("LOAD AZS LIST!");
        final ArrayList<Azs> list = new ArrayList<>();
        Q((con) -> {
            FB_Query q = con.execute("SELECT IDD, CTITLE, CADDRESS, IDDFIRM, IDDFIRMMC, IBWORK "
                    + "FROM W_AZS_LIST(?,?,?) ORDER BY IDD", iddfirm, dtw, dtwend);
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
        logger.info("LOAD CONTRACT LIST!");
        final ArrayList<Contract> list = new ArrayList<>();
        Q((con) -> {
            FB_Query q = con.execute("SELECT IDD, CNUMBER, ITYPE, DTSIGN, DTSTARTFACT, DTENDFACT, IEXPIRED "
                    + "FROM W_CLIENT_CONTRACTS(?,?,?,?) ORDER BY IDD", iddfirm, iddclient, iddsub, dtw);
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
        logger.info("LOAD ACCS LIST!");
        final ArrayList<Acc> list = new ArrayList<>();
        Q((con) -> {
            FB_Query q = con.execute("SELECT IDD, IDDFIRM, IDDCLIENT, IDDSUB, IACCTYPE, IDDOIL, DBSTART, DBTRANS, DBPAY, DBEND "
                    + "FROM W_CLIENT_ACCS(?,?,?,?) ORDER BY IDD", iddfirm, iddclient, iddsub, dtw);
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
    public ArrayList<Card> loadClientCards(Integer iddfirm, Integer iddclient, Integer iddsub, LocalDate dtw, Integer iwork,
                                           int offset, int limit, String sort) throws ExError {
        logger.info("LOAD CARDS LIST!");
        final ArrayList<Card> list = new ArrayList<>();
        Q((con) -> {
            FB_Query q = con.execute("SELECT FIRST " + limit + " SKIP " + offset + " "
                            + " DTW, DTWEND, IDD, IACCTYPE, IBWORK, CDRIVER, CCAR, DBDAYLIMIT, CCOMMENT "
                            + " FROM W_CLIENT_CARDS(?,?,?,?,?) "
                            + (StringTools.isEmptySafe(sort) ? "" : " ORDER BY " + sort),
                    iddfirm, iddclient, iddsub, dtw, iwork);
            while (q.next()) {
                list.add(new Card(q.getLocalDate("DTW"), q.getLocalDate("DTWEND"), q.getString("IDD"),
                        q.getInteger("IACCTYPE"), q.getInteger("IBWORK"), q.getString("CDRIVER"), q.getString("CCAR"),
                        q.getLong("DBDAYLIMIT"), q.getString("CCOMMENT")));
            }
            q.closeSafe();
        });
        return list;
    }

    public int loadClientCardsCount(Integer iddfirm, Integer iddclient, Integer iddsub, LocalDate dtw, Integer iwork) throws ExError {
        final int[] count = {-1};
        Q((con) -> {
            FB_Query q = con.execute("SELECT count(*) FROM W_CLIENT_CARDS(?,?,?,?,?)",
                    iddfirm, iddclient, iddsub, dtw, iwork);
            if (q.next()) count[0] = q.getInteger(1);
            q.closeSafe();
        });
        return count[0];
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    //
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    public ArrayList<Transaction> loadClientTransactions(Integer iddfirm, Integer iddclient, Integer iddsub, LocalDate dtw,
                                                         LocalDate dtwend, Integer iddazs,
                                                         int offset, int limit, String sort) throws ExError {
        logger.infof("LOAD TRANS: azs=%d idx=%d count=%d sort='%s'", iddazs, offset, limit, sort);
        ArrayList<Transaction> list = new ArrayList<>();
        Q((con) -> {
            FB_Query q = con.execute("SELECT FIRST " + limit + " SKIP " + offset + " "
                            + "DTSTART, DTEND, IDDCARD, CCARD, IDD, IDDAZS, IDDTRK, IDDOIL, IACCTYPE, DBPRICE, DBVOLREQ, DBVOLUME, DBSUMMA "
                            + "FROM W_CLIENT_TRANS(?,?,?,NULL, ?,?,?) "
                            + (StringTools.isEmptySafe(sort) ? "" : " ORDER BY " + sort),
                    iddfirm, iddclient, iddsub, dtw, dtwend, iddazs);
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

    public int loadClientTransactionsCount(Integer iddfirm, Integer iddclient, Integer iddsub, LocalDate dtw, LocalDate dtwend,
                                           Integer iddazs) throws ExError {
        final int[] count = {-1};
        Q((con) -> {
            FB_Query q = con.execute("SELECT count(*) FROM W_CLIENT_TRANS(?,?,?,NULL, ?,?,?)",
                    iddfirm, iddclient, iddsub, dtw, dtwend, iddazs);
            if (q.next()) count[0] = q.getInteger(1);
            q.closeSafe();
        });
        return count[0];
    }

}
