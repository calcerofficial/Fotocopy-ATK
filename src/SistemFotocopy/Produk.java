package SistemFotocopy;

import javafx.beans.property.*;

public class Produk {
    private final StringProperty idProduk;
    private final StringProperty namaProduk;
    private final StringProperty merk;
    private final StringProperty kategori;
    private final DoubleProperty harga;
    private final IntegerProperty stok;
    private final StringProperty status;

    public Produk(String idProduk, String namaProduk, String merk, String kategori, double harga, int stok, String status) {
        this.idProduk = new SimpleStringProperty(idProduk);
        this.namaProduk = new SimpleStringProperty(namaProduk);
        this.merk = new SimpleStringProperty(merk);
        this.kategori = new SimpleStringProperty(kategori);
        this.harga = new SimpleDoubleProperty(harga);
        this.stok = new SimpleIntegerProperty(stok);
        this.status = new SimpleStringProperty(status);
    }

    public StringProperty idProdukProperty() { return idProduk; }
    public StringProperty namaProdukProperty() { return namaProduk; }
    public StringProperty merkProperty() { return merk; }
    public StringProperty kategoriProperty() { return kategori; }
    public DoubleProperty hargaProperty() { return harga; }
    public IntegerProperty stokProperty() { return stok; }
    public StringProperty statusProperty() { return status; }

    public String getIdProduk() { return idProduk.get(); }
    public String getNamaProduk() { return namaProduk.get(); }
    public String getMerk() { return merk.get(); }
    public String getKategori() { return kategori.get(); }
    public double getHarga() { return harga.get(); }
    public int getStok() { return stok.get(); }
    public String getStatus() { return status.get(); }
}