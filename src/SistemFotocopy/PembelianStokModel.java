package SistemFotocopy;

import java.text.NumberFormat;
import java.util.Locale;

public class PembelianStokModel {
    private String idPembelian;
    private String pegawai;
    private String supplier;
    private String tanggal;
    private String statusPembayaran;
    private double totalHarga;
    private String metodePembayaran; // Will just be "-" since not in DB view

    public PembelianStokModel(String idPembelian, String pegawai, String supplier, String tanggal, String statusPembayaran, double totalHarga) {
        this.idPembelian = idPembelian;
        this.pegawai = pegawai;
        this.supplier = supplier;
        this.tanggal = tanggal;
        this.statusPembayaran = statusPembayaran;
        this.totalHarga = totalHarga;
        this.metodePembayaran = "-";
    }

    public String getIdPembelian() {
        return idPembelian;
    }

    public String getPegawai() {
        return pegawai;
    }

    public String getSupplier() {
        return supplier;
    }

    public String getTanggal() {
        return tanggal;
    }

    public String getStatusPembayaran() {
        return statusPembayaran;
    }

    public double getTotalHarga() {
        return totalHarga;
    }

    public String getMetodePembayaran() {
        return metodePembayaran;
    }

    public String getTotalHargaFormatted() {
        NumberFormat formatter = NumberFormat.getCurrencyInstance(new Locale("id", "ID"));
        return formatter.format(totalHarga).replace("Rp", "Rp.");
    }
}
