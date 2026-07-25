package SistemFotocopy.LihatDataProduk.Dataclass;

import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;

public class DataProdukModel {
    private final SimpleStringProperty idProduk;
    private final SimpleStringProperty namaBarang;
    private final SimpleStringProperty merk;
    private final SimpleDoubleProperty harga;
    private final SimpleIntegerProperty stok;
    private final SimpleStringProperty status;

    public DataProdukModel(String idProduk, String namaBarang, String merk,
                           double harga, int stok, String status) {
        this.idProduk = new SimpleStringProperty(idProduk);
        this.namaBarang = new SimpleStringProperty(namaBarang);
        this.merk = new SimpleStringProperty(merk);
        this.harga = new SimpleDoubleProperty(harga);
        this.stok = new SimpleIntegerProperty(stok);
        this.status = new SimpleStringProperty(status);
    }

    public String getIdProduk() { return idProduk.get(); }
    public SimpleStringProperty idProdukProperty() { return idProduk; }

    public String getNamaBarang() { return namaBarang.get(); }
    public SimpleStringProperty namaBarangProperty() { return namaBarang; }

    public String getMerk() { return merk.get(); }
    public SimpleStringProperty merkProperty() { return merk; }

    public double getHarga() { return harga.get(); }
    public SimpleDoubleProperty hargaProperty() { return harga; }

    public int getStok() { return stok.get(); }
    public SimpleIntegerProperty stokProperty() { return stok; }

    public String getStatus() { return status.get(); }
    public SimpleStringProperty statusProperty() { return status; }
}