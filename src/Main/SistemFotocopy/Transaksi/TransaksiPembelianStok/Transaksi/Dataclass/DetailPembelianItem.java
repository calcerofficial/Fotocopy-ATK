package SistemFotocopy.Transaksi.TransaksiPembelianStok.Transaksi.Dataclass;

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

    public String getIdProduk() { return idProduk; }
    public String getNamaBarang() { return namaBarang; }
    public int getJumlah() { return jumlah; }
    public void setJumlah(int jumlah) { this.jumlah = jumlah; }
    public double getHarga() { return harga; }
    public String getHargaFormatted() { return String.format("Rp. %,.0f", harga); }
}