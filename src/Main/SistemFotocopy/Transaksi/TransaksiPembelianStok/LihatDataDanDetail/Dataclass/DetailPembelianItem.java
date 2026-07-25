package SistemFotocopy.Transaksi.TransaksiPembelianStok.LihatDataDanDetail.Dataclass;

import java.text.NumberFormat;
import java.util.Locale;

public class DetailPembelianItem {
    private final String idProduk;
    private final String namaBarang;
    private int jumlah;
    private final double harga;

    public DetailPembelianItem(String idProduk, String namaBarang, int jumlah, double harga) {
        this.idProduk = idProduk;
        this.namaBarang = namaBarang;
        this.jumlah = jumlah;
        this.harga = harga;
    }

    // Constructor tanpa idProduk (untuk detail pembelian di DataPembelianStock)
    public DetailPembelianItem(String namaBarang, int jumlah, double harga) {
        this.idProduk = null;
        this.namaBarang = namaBarang;
        this.jumlah = jumlah;
        this.harga = harga;
    }

    public String getIdProduk() { return idProduk; }
    public String getNamaBarang() { return namaBarang; }
    public int getJumlah() { return jumlah; }
    public void setJumlah(int jumlah) { this.jumlah = jumlah; }
    public double getHarga() { return harga; }

    public String getHargaFormatted() {
        NumberFormat formatter = NumberFormat.getCurrencyInstance(new Locale("id", "ID"));
        return formatter.format(harga).replace("Rp", "Rp.");
    }
}