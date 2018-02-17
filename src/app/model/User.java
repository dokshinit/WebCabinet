package app.model;

import java.time.LocalDate;

/**
 * @author Aleksey Dokshin <dant.it@gmail.com> (28.11.17).
 */
public class User {

    public enum ClientWorkState {
        NO, LOCK, YES
    }

    // Информация о фирме-клиенте.
    private Firm firm;
    private int iddClient, iddClentSub; // Для доступа к информации.
    private String clientINN;
    private String clientName;
    private String clientTitle;
    private String clientSubTitle;
    private String clientAddress;
    private String clientEmail;
    private String clientPhone;
    private Boolean clientFond;
    private ClientWorkState clientWork;
    private Long clientCredit;
    private LocalDate dtStart;

    // Информация о пользователе.
    private int id;
    private String role;
    private String login;
    private String password;
    private String firstName;
    private String lastName;
    //
    private boolean isAuthorized;

    public User() {
        id = 0;
        firm = null;
        iddClient = iddClentSub = 0;
        clientINN = "";
        clientName = clientTitle = clientSubTitle = "";
        clientAddress = clientEmail = clientPhone = "";
        role = login = password = "";
        firstName = lastName = "";
        isAuthorized = false;
    }

    public synchronized int getId() {
        return id;
    }

    public synchronized Firm getFirm() {
        return firm;
    }

    public synchronized int getIddClient() {
        return iddClient;
    }

    public synchronized int getIddClentSub() {
        return iddClentSub;
    }

    public synchronized String getClientINN() {
        return clientINN;
    }

    public synchronized String getClientName() {
        return clientName;
    }

    public synchronized String getClientTitle() {
        return clientTitle;
    }

    public synchronized String getClientSubTitle() {
        return clientSubTitle;
    }

    public synchronized String getClientAddress() {
        return clientAddress;
    }

    public synchronized String getClientEmail() {
        return clientEmail;
    }

    public synchronized String getClientPhone() {
        return clientPhone;
    }

    public synchronized Boolean isClientFond() {
        return clientFond;
    }

    public synchronized ClientWorkState getClientWork() {
        return clientWork;
    }

    public synchronized Long getClientCredit() {
        return clientCredit;
    }

    public synchronized LocalDate getDtStart() {
        return dtStart;
    }

    public synchronized String getRole() {
        return role;
    }

    public synchronized String getFirstName() {
        return firstName;
    }

    public synchronized String getLastName() {
        return lastName;
    }

    public synchronized String getLogin() {
        return login;
    }

    public synchronized String getPassword() {
        return password;
    }

    public synchronized String getTitle() {
        return firstName;
    }

    public synchronized boolean isAuthorized() {
        return isAuthorized;
    }

    // Только для вызова из модели!
    synchronized void login(int id, int iddFirm, int iddClient, int iddClentSub,
                            String clientINN, String clientName, String clientTitle, String clientSubTitle,
                            String clientAddress, String clientEmail, String clientPhone,
                            Integer clientFond, Integer clientWork, Long clientCredit, LocalDate dtStart,
                            String role, String firstName, String lastName) {

        this.id = id;
        this.firm = Firm.byId(iddFirm);
        this.iddClient = iddClient;
        this.iddClentSub = iddClentSub;
        this.clientINN = clientINN;
        this.clientName = clientName;
        this.clientTitle = clientTitle;
        this.clientSubTitle = clientSubTitle;
        this.clientAddress = clientAddress;
        this.clientEmail = clientEmail;
        this.clientPhone = clientPhone;
        this.clientFond = clientFond != 0;
        this.clientWork = clientWork < 0 ? ClientWorkState.NO : (clientWork > 0 ? ClientWorkState.YES : ClientWorkState.LOCK);
        this.clientCredit = clientCredit;
        this.dtStart = dtStart;
        this.role = role;
        this.firstName = firstName;
        this.lastName = lastName;
        this.isAuthorized = true;
    }

    // Только для вызова из модели!
    synchronized void logout() {
        this.isAuthorized = false;
    }
}
