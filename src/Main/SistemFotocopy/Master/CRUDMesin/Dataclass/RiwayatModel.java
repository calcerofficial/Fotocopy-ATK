package SistemFotocopy.Master.CRUDMesin.Dataclass;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class RiwayatModel {
    private final StringProperty idMesin;
    private final StringProperty tanggal;
    private final StringProperty keterangan;

    public RiwayatModel(String idMesin, String tanggal, String keterangan) {
        this.idMesin = new SimpleStringProperty(idMesin);
        this.tanggal = new SimpleStringProperty(tanggal);
        this.keterangan = new SimpleStringProperty(keterangan);
    }

    public StringProperty idMesinProperty() {
        return idMesin;
    }

    public StringProperty tanggalProperty() {
        return tanggal;
    }

    public StringProperty keteranganProperty() {
        return keterangan;
    }
}