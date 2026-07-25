package SistemFotocopy.Master.CRUDMesin.Dataclass;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class MesinModel {
    private final StringProperty idMesin;
    private final StringProperty namaMesin;
    private final StringProperty merkMesin;
    private final StringProperty statusMesin;

    public MesinModel(String id, String nama, String merk, String status) {
        this.idMesin = new SimpleStringProperty(id);
        this.namaMesin = new SimpleStringProperty(nama);
        this.merkMesin = new SimpleStringProperty(merk);
        this.statusMesin = new SimpleStringProperty(status);
    }

    public String getIdMesin() {
        return idMesin.get();
    }

    public StringProperty idMesinProperty() {
        return idMesin;
    }

    public String getNamaMesin() {
        return namaMesin.get();
    }

    public StringProperty namaMesinProperty() {
        return namaMesin;
    }

    public String getMerkMesin() {
        return merkMesin.get();
    }

    public StringProperty merkMesinProperty() {
        return merkMesin;
    }

    public String getStatusMesin() {
        return statusMesin.get();
    }

    public StringProperty statusMesinProperty() {
        return statusMesin;
    }
}