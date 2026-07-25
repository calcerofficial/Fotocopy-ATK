package SistemFotocopy.Transaksi.TransaksiPenjualan.Transaksi.Dataclass;

public class DetailDataPenjualan {
    private String idPenjualan;
    private String idProduk;
    private String namaBarang;
    private int jumlah;
    private double harga;

    public DetailDataPenjualan(String idPenjualan, String idProduk, String namaBarang, int jumlah, double harga) {
        this.idPenjualan = idPenjualan;
        this.idProduk = idProduk;
        this.namaBarang = namaBarang;
        this.jumlah = jumlah;
        this.harga = harga;
    }

    public String getIdPenjualan() { return idPenjualan; }
    public String getIdProduk() { return idProduk; }
    public String getNamaBarang() { return namaBarang; }
    public int getJumlah() { return jumlah; }
    public double getHarga() { return harga; }

    public String getHargaFormatted() {
        return "Rp " + String.format("%,d", (long) harga).replace(',', '.');
    }

    public void setIdPenjualan(String idPenjualan) { this.idPenjualan = idPenjualan; }
    public void setIdProduk(String idProduk) { this.idProduk = idProduk; }
    public void setNamaBarang(String namaBarang) { this.namaBarang = namaBarang; }
    public void setJumlah(int jumlah) { this.jumlah = jumlah; }
    public void setHarga(double harga) { this.harga = harga; }
}