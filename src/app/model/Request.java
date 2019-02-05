package app.model;

import app.AppServlet;

import java.io.File;
import java.time.LocalDateTime;

/**
 * @author Aleksey Dokshin <dant.it@gmail.com> (24.01.18).
 */
public class Request extends RequestBase {

    /** Режим фильтрации запросов. */
    public enum FilterMode {

        INCOMPLETE(1, "Исполняемые"),
        FINISHED(2, "Завершённые");

        public int id;
        public String title;

        FilterMode(int id, String title) {
            this.id = id;
            this.title = title;
        }

        public static FilterMode byId(Integer id) {
            if (id == null) return null;
            for (FilterMode item : values()) if (item.id == id) return item;
            return null;
        }

        public int getId() {
            return id;
        }

        public String getTitle() {
            return title;
        }
    }

    public Request(Integer id, LocalDateTime dtCreate, Integer iType, Integer iSubType,
                   String paramsTitle, String params, String comment, Integer istate,
                   LocalDateTime dtProcess, String fileName, Integer fileSize, Integer sendTryRemain,
                   LocalDateTime dtSend, String result) {
        super(id, dtCreate, iType, iSubType, paramsTitle, params, comment, istate, dtProcess, fileName,
                fileSize, sendTryRemain, dtSend, result);
    }

    public static Request newReport(ReportType rtype, String paramstitle, ParamsMap paramsmap) {
        return new Request(null, null, Type.REPORT.id, rtype.id, paramstitle, paramsmap.toString(),
                "", State.PROCESSING.id, null, null, null, null, null, null);
    }

    public static Request newExport(ExportType etype, String paramstitle, ParamsMap paramsmap) {
        return new Request(null, null, Type.EXPORT.id, etype.id, paramstitle, paramsmap.toString(),
                "", State.PROCESSING.id, null, null, null, null, null, null);
    }

    public String getParamsAsHTML() {
        StringBuilder sb = new StringBuilder();
        sb.append("<div class='request-param-popup'>");
        paramsMap.forEach((i, li, e) -> sb.append("<span class='request-param-id'>").append(e.getKey()).
                append("</span> = <span class='request-param-value'>").append(e.getValue()).append("</span>").
                append(i < li ? "<br>" : ""));
        return sb.append("</div>").toString();
    }

    public String getFullComment() {
        return comment + (result != null && !result.isEmpty() ? "[" + result + "]" : "");
    }

    /** Путь для файла ответа на заявку. */
    public String getAnswerPath(User user) {
        return AppServlet.getAnswersPath() + File.separator
                + user.getIddClient() + (user.getIddClentSub() != 0 ? "-" + user.getIddClentSub() : "");
    }

    public void setSendTryRemain(Integer sendTryRemain) {
        this.sendTryRemain = sendTryRemain;
    }

    public void updateByCreate(Integer id, LocalDateTime dtCreate) {
        this.id = id;
        this.dtCreate = dtCreate;
    }
}
