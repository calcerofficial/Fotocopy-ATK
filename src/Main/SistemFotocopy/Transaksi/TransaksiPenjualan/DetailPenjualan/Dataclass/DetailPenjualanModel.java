package SistemFotocopy.Transaksi.TransaksiPenjualan.DetailPenjualan.Dataclass;

import javafx.beans.property.DoubleProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class DetailPenjualanModel {
    private final StringProperty namaProduk;
    private final IntegerProperty jumlah;
    private final DoubleProperty hargaSatuan;

    public DetailPenjualanModel(String namaProduk, int jumlah, double hargaSatuan) {
        this.namaProduk = new SimpleStringProperty(namaProduk);
        this.jumlah = new SimpleIntegerProperty(jumlah);
        this.hargaSatuan = new SimpleDoubleProperty(hargaSatuan);
    }

    public String getNamaProduk() { return namaProduk.get(); }
    public StringProperty namaProdukProperty() { return namaProduk; }

    public int getJumlah() { return jumlah.get(); }
    public IntegerProperty jumlahProperty() { return jumlah; }

    public double getHargaSatuan() { return hargaSatuan.get(); }
    public DoubleProperty hargaSatuanProperty() { return hargaSatuan; }
}