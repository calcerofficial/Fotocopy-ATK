package SistemFotocopy.Master.CRUDMesin.Dataclass;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class DetailProdukMesinModel {
    private final StringProperty idMesin;
    private final StringProperty idProduk;
    private final StringProperty namaLayanan;

    public DetailProdukMesinModel(String idMesin, String idProduk, String namaLayanan) {
        this.idMesin = new SimpleStringProperty(idMesin);
        this.idProduk = new SimpleStringProperty(idProduk);
        this.namaLayanan = new SimpleStringProperty(namaLayanan);
    }

    public StringProperty idMesinProperty() {
        return idMesin;
    }

    public StringProperty idProdukProperty() {
        return idProduk;
    }

    public StringProperty namaLayananProperty() {
        return namaLayanan;
    }
}