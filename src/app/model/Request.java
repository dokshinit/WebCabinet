package app.model;

import util.NumberTools;

import java.io.File;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static util.StringTools.isEmptySafe;

/**
 * @author Aleksey Dokshin <dant.it@gmail.com> (24.01.18).
 */
public class Request {

    public enum Type {
        REPORT(1, "Отчет"),
        CARDCHANGE(2, "Изменение данных"),
        CARDPAY(3, "Приобретение карт");

        public int id;
        public String title;

        Type(int id, String title) {
            this.id = id;
            this.title = title;
        }

        public static Type byId(int id) {
            for (Type item : values()) if (item.id == id) return item;
            return null;
        }

        public int getId() {
            return id;
        }

        public String getTitle() {
            return title;
        }
    }

    public enum State {

        // Незавершенные (>0).
        PROCESSING(1, "Обработка"),
        SENDING(2, "Отправка"),
        // Завершенные (<=0).
        FINISHED(0, "Завершено"),
        ERROR(-1, "Ошибка");

        public int id;
        public String title;

        State(int id, String title) {
            this.id = id;
            this.title = title;
        }

        public static State byId(int id) {
            for (State item : values()) if (item.id == id) return item;
            return null;
        }

        public int getId() {
            return id;
        }

        public String getTitle() {
            return title;
        }
    }

    public enum Mode {

        INCOMPLETE(1, "Исполняемые"),
        FINISHED(2, "Завершённые");

        public int id;
        public String title;

        Mode(int id, String title) {
            this.id = id;
            this.title = title;
        }

        public static Mode byId(int id) {
            for (Mode item : values()) if (item.id == id) return item;
            return null;
        }

        public int getId() {
            return id;
        }

        public String getTitle() {
            return title;
        }
    }

    public enum ReportType {
        TURNOVER(1, "Обороты клиента"),
        TRANSACTION(2, "Транзакции клиента"),
        CARD(3, "Карты клиента");

        public int id;
        public String cid;
        public String title;

        ReportType(int id, String title) {
            this.id = id;
            this.cid = "" + id;
            this.title = title;
        }

        public static ReportType byId(int id) {
            for (ReportType item : values()) if (item.id == id) return item;
            return null;
        }

        public int getId() {
            return id;
        }

        public String getCid() {
            return cid;
        }

        public String getTitle() {
            return title;
        }
    }

    private Integer id;
    private LocalDateTime dtCreate;
    private Type type;
    private Integer idSubType;
    private ReportType reportType;
    private String paramsTitle;
    private String params;
    private String comment;
    private State state;
    private LocalDateTime dtProcess;
    private String fileName;
    private Integer fileSize;
    private Integer sendTryRemain;
    private LocalDateTime dtSend;
    private String result;

    private ArrayList<CardItem> cardItems;

    public Request(Integer id, LocalDateTime dtCreate, Integer iType, Integer iSubType, String paramsTitle, String params, String comment, Integer istate,
                   LocalDateTime dtProcess, String fileName, Integer fileSize, Integer sendTryRemain, LocalDateTime dtSend, String result) {
        this.id = id;
        this.type = Type.byId(iType);
        this.idSubType = iSubType;
        this.reportType = type == Type.REPORT ? ReportType.byId(iSubType) : null;
        this.paramsTitle = paramsTitle;
        this.params = params;
        this.comment = comment;
        this.state = State.byId(istate);
        this.dtCreate = dtCreate;
        this.fileName = fileName;
        this.fileSize = fileSize;
        this.dtProcess = dtProcess;
        this.sendTryRemain = sendTryRemain;
        this.dtSend = dtSend;
        this.result = result;

        this.cardItems = new ArrayList<>();
    }

    public static Request newReport(ReportType rtype, String title, HashMap<String, String> paramsmap) {
        return new Request(null, null, Type.REPORT.id, rtype.id, title, Request.mapToParams(paramsmap), "", State.PROCESSING.id, null, null, null, null, null, null);
    }

    public String getParams() {
        return params;
    }

    private static final Pattern paramsPattern = Pattern.compile("([a-zA-Z0-9]{1,})(=)(.*)(\r\n|\n\r|\r|\n|$)");

    public String getParamsAsHTML() {
        StringBuilder sb = new StringBuilder();
        Matcher m = paramsPattern.matcher(params);
        sb.append("<div class='request-param-popup'>");
        while (m.find()) {
            sb.append(String.format(
                    "<span class='request-param-id'>%s</span> = <span class='request-param-value'>%s</span>%s",
                    m.group(1), m.group(3), m.group(4).isEmpty() ? "" : "<br>"));
        }
        return sb.append("</div>").toString();
    }

    public Integer getReportModeParam() {
        HashMap<String, String> map = paramsToMap(params);
        return NumberTools.parseInt(map.get("idReportMode"));
    }

    public void setReportModeParam(Integer id) {
        HashMap<String, String> map = paramsToMap(params);
        map.put("idReportMode", id == null ? "" : ("" + id));
        params = mapToParams(map);
    }

    public static HashMap<String, String> paramsToMap(String params) {
        HashMap<String, String> map = new HashMap<>();
        Matcher m = paramsPattern.matcher(params);
        while (m.find()) map.put(m.group(1), m.group(3));
        return map;
    }

    public static String mapToParams(HashMap<String, String> map) {
        StringBuilder sb = new StringBuilder();
        String[] keys = map.keySet().toArray(new String[1]);
        boolean isfirst = true;
        for (String key : keys) {
            String value = map.get(key);
            if (!isfirst) sb.append("\r\n"); //win
            sb.append(key).append('=').append(value);
            isfirst = false;
        }
        return sb.toString();
    }

    public Integer getId() {
        return id;
    }

    public LocalDateTime getDtCreate() {
        return dtCreate;
    }

    public Type getType() {
        return type;
    }

    public Integer getIdSubType() {
        return idSubType;
    }

    public ReportType getReportType() {
        return reportType;
    }

    public String getTitle() {
        if (type == Type.REPORT) {
            return reportType.getTitle();
        }
        // TODO: Здесь добавится расшифровка для других типов заявок (для подтипов, если надо).
        return type.getTitle();
    }

    public String getFullTitle() {
        return getTitle() + (isEmptySafe(paramsTitle) ? "" : " " + paramsTitle);
    }

    public String getParamsTitle() {
        return paramsTitle;
    }

    public String getComment() {
        return comment;
    }

    public String getFullComment() {
        return comment + (result != null && !result.isEmpty() ? "[" + result + "]" : "");
    }

    public State getState() {
        return state;
    }

    public LocalDateTime getDtProcess() {
        return dtProcess;
    }

    public String getFileName() {
        if (fileName != null && !fileName.isEmpty()) return fileName;
        return "";
    }

    public String getAnswerPath(User user) {
        return "/home/work/dev/JavaFX/WebRequestProcessor/answers" + File.separator + user.getIddClient() + (user.getIddClentSub() != 0 ? "-" + user.getIddClentSub() : "");
    }

    public Integer getFileSize() {
        return fileSize;
    }

    public Integer getSendTryRemain() {
        return sendTryRemain;
    }

    public void setSendTryRemain(Integer sendTryRemain) {
        this.sendTryRemain = sendTryRemain;
    }

    public LocalDateTime getDtSend() {
        return dtSend;
    }

    public String getResult() {
        return result;
    }

    public ArrayList<CardItem> getCardItems() {
        return cardItems;
    }

    public void updateByCreate(Integer id, LocalDateTime dtCreate) {
        this.id = id;
        this.dtCreate = dtCreate;
    }

    public void updateByProcess(LocalDateTime dtProcess) {
        this.dtProcess = dtProcess;
    }

    public void setFile(String fileName, Integer fileSize) {
        this.fileName = fileName;
        this.fileSize = fileSize;
    }

    public void updateBySend(LocalDateTime dtSend) {
        this.dtSend = dtSend;
    }

    public static class CardItem {
        private String idd;
        private String driver, car, comment;
        private Long dayLimit;
        private String oilLimit, azsLimit;
        private Card.WorkState work;

        public CardItem(String idd, String driver, String car, String comment, Long dayLimit, String oilLimit, String azsLimit, Card.WorkState work) {
            this.idd = idd;
            this.driver = driver;
            this.car = car;
            this.comment = comment;
            this.dayLimit = dayLimit;
            this.oilLimit = oilLimit;
            this.azsLimit = azsLimit;
            this.work = work;
        }

        public String getIdd() {
            return idd;
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

        public Long getDayLimit() {
            return dayLimit;
        }

        public String getOilLimit() {
            return oilLimit;
        }

        public String getAzsLimit() {
            return azsLimit;
        }

        public Card.WorkState getWork() {
            return work;
        }
    }
}
