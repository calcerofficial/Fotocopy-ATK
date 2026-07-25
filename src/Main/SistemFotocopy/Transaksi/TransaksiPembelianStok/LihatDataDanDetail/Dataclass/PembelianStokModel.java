package SistemFotocopy.Transaksi.TransaksiPembelianStok.LihatDataDanDetail.Dataclass;

import java.text.NumberFormat;
import java.util.Locale;

public class PembelianStokModel {
    private final String idPembelian;
    private final String pegawai;
    private final String supplier;
    private final String tanggal;
    private final String statusPembayaran;
    private final double totalHarga;
    private final String metodePembayaran;

    public PembelianStokModel(String idPembelian, String pegawai, String supplier,
                              String tanggal, String statusPembayaran, double totalHarga) {
        this.idPembelian = idPembelian;
        this.pegawai = pegawai;
        this.supplier = supplier;
        this.tanggal = tanggal;
        this.statusPembayaran = statusPembayaran;
        this.totalHarga = totalHarga;
        this.metodePembayaran = "-";
    }

    public PembelianStokModel(String idPembelian, String pegawai, String supplier,
                              String tanggal, String statusPembayaran,
                              double totalHarga, String metodePembayaran) {
        this.idPembelian = idPembelian;
        this.pegawai = pegawai;
        this.supplier = supplier;
        this.tanggal = tanggal;
        this.statusPembayaran = statusPembayaran;
        this.totalHarga = totalHarga;
        this.metodePembayaran = metodePembayaran != null ? metodePembayaran : "-";
    }

    public String getIdPembelian() { return idPembelian; }
    public String getPegawai() { return pegawai; }
    public String getSupplier() { return supplier; }
    public String getTanggal() { return tanggal; }
    public String getStatusPembayaran() { return statusPembayaran; }
    public double getTotalHarga() { return totalHarga; }
    public String getMetodePembayaran() { return metodePembayaran; }

    public String getTotalHargaFormatted() {
        NumberFormat formatter = NumberFormat.getCurrencyInstance(new Locale("id", "ID"));
        return formatter.format(totalHarga).replace("Rp", "Rp.");
    }
}