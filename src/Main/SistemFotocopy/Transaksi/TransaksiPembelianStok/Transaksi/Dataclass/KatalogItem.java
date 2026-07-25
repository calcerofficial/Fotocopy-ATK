package SistemFotocopy.Transaksi.TransaksiPembelianStok.Transaksi.Dataclass;

import SistemFotocopy.Transaksi.TransaksiPembelianStok.Transaksi.Controller.TransaksiPembelianStock;
import javafx.scene.control.Button;

public class KatalogItem {
    private final String idProduk;
    private final String namaBarang;
    private final String merkBarang;
    private final double harga;
    private final int stok;
    private final Button aksiButton;

    public KatalogItem(String idProduk, String namaBarang, String merkBarang, double harga, int stok) {
        this.idProduk = idProduk;
        this.namaBarang = namaBarang;
        this.merkBarang = merkBarang;
        this.harga = harga;
        this.stok = stok;

        this.aksiButton = new Button("+");
        this.aksiButton.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white; -fx-font-weight: bold;");
        this.aksiButton.setPrefWidth(30);
    }

    public String getIdProduk() { return idProduk; }
    public String getNamaBarang() { return namaBarang; }
    public String getMerkBarang() { return merkBarang; }
    public double getHarga() { return harga; }
    public String getHargaFormatted() { return String.format("Rp. %,.0f", harga); }
    public int getStok() { return stok; }
    public Button getAksiButton() { return aksiButton; }

    public void setAksiListener(TransaksiPembelianStock controller) {
        aksiButton.setOnAction(e -> controller.addItemToDetail(this));
    }


}