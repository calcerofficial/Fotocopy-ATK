package SistemFotocopy.Master.CRUDPegawai.Dataclass;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class PegawaiModel {
    private final StringProperty idPegawai;
    private final StringProperty namaPegawai;
    private final StringProperty alamat;
    private final StringProperty noTelepon;
    private final StringProperty email;
    private final StringProperty username;
    private final StringProperty status;

    public PegawaiModel(String idPegawai, String namaPegawai, String alamat,
                        String noTelepon, String email, String username, String status) {
        this.idPegawai = new SimpleStringProperty(idPegawai);
        this.namaPegawai = new SimpleStringProperty(namaPegawai);
        this.alamat = new SimpleStringProperty(alamat);
        this.noTelepon = new SimpleStringProperty(noTelepon);
        this.email = new SimpleStringProperty(email);
        this.username = new SimpleStringProperty(username);
        this.status = new SimpleStringProperty(status);
    }

    public String getIdPegawai() {
        return idPegawai.get();
    }

    public StringProperty idPegawaiProperty() {
        return idPegawai;
    }

    public String getNamaPegawai() {
        return namaPegawai.get();
    }

    public StringProperty namaPegawaiProperty() {
        return namaPegawai;
    }

    public String getAlamat() {
        return alamat.get();
    }

    public StringProperty alamatProperty() {
        return alamat;
    }

    public String getNoTelepon() {
        return noTelepon.get();
    }

    public StringProperty noTeleponProperty() {
        return noTelepon;
    }

    public String getEmail() {
        return email.get();
    }

    public StringProperty emailProperty() {
        return email;
    }

    public String getUsername() {
        return username.get();
    }

    public StringProperty usernameProperty() {
        return username;
    }

    public String getStatus() {
        return status.get();
    }

    public StringProperty statusProperty() {
        return status;
    }
}