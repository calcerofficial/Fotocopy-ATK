package SistemFotocopy.Transaksi.TransaksiPenjualan.HasilTransaksi.Dataclass;

import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class DataPenjualanModel {
    private final StringProperty idPenjualan;
    private final StringProperty karyawan;
    private final StringProperty tanggal;
    private final DoubleProperty totalHarga;
    private final DoubleProperty uangBayar;
    private final DoubleProperty kembalian;
    private final StringProperty metode;
    private final StringProperty status;

    public DataPenjualanModel(String idPenjualan, String karyawan, String tanggal,
                              double totalHarga, double uangBayar, double kembalian,
                              String metode, String status) {
        this.idPenjualan = new SimpleStringProperty(idPenjualan);
        this.karyawan = new SimpleStringProperty(karyawan);
        this.tanggal = new SimpleStringProperty(tanggal);
        this.totalHarga = new SimpleDoubleProperty(totalHarga);
        this.uangBayar = new SimpleDoubleProperty(uangBayar);
        this.kembalian = new SimpleDoubleProperty(kembalian);
        this.metode = new SimpleStringProperty(metode);
        this.status = new SimpleStringProperty(status);
    }

    public String getIdPenjualan() { return idPenjualan.get(); }
    public StringProperty idPenjualanProperty() { return idPenjualan; }

    public String getKaryawan() { return karyawan.get(); }
    public StringProperty karyawanProperty() { return karyawan; }

    public String getTanggal() { return tanggal.get(); }
    public StringProperty tanggalProperty() { return tanggal; }

    public double getTotalHarga() { return totalHarga.get(); }
    public DoubleProperty totalHargaProperty() { return totalHarga; }

    public double getUangBayar() { return uangBayar.get(); }
    public DoubleProperty uangBayarProperty() { return uangBayar; }

    public double getKembalian() { return kembalian.get(); }
    public DoubleProperty kembalianProperty() { return kembalian; }

    public String getMetode() { return metode.get(); }
    public StringProperty metodeProperty() { return metode; }

    public String getStatus() { return status.get(); }
    public StringProperty statusProperty() { return status; }
}