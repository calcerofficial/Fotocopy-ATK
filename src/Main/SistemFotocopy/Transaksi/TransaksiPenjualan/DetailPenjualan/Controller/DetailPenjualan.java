package SistemFotocopy.Transaksi.TransaksiPenjualan.DetailPenjualan.Controller;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;

public class DetailPenjualan {

    @FXML
    private HBox Kembalian;

    @FXML
    private Label lblNamaPegawai;

    @FXML
    private Label lblTanggal;

    @FXML
    private Label lblproduk1;

    @FXML
    private Label metode;

    @FXML
    private Label qty;

    @FXML
    private Label totalharga;

    @FXML
    private Label uangBayar;

    @FXML
    private Label lblKembalian;

    @FXML
    private Label lblIdPenjualan;

    /**
     * Set data dengan 9 parameter (termasuk ID Penjualan)
     */
    public void setData(String idPenjualan, String tanggal, String pegawai, String produk,
                        String qty, String metode, String total,
                        String bayar, String kembali) {
        // Set ID
        if (lblIdPenjualan != null) {
            lblIdPenjualan.setText("ID Penjualan: " + (idPenjualan != null ? idPenjualan : "-"));
        }

        // Set Tanggal
        if (lblTanggal != null) {
            lblTanggal.setText(tanggal != null ? tanggal : "-");
        }

        // Set Pegawai
        if (lblNamaPegawai != null) {
            lblNamaPegawai.setText(pegawai != null ? pegawai : "-");
        }

        // Set Produk
        if (lblproduk1 != null) {
            lblproduk1.setText(produk != null ? produk : "Tidak ada produk");
        }

        // Set QTY
        if (this.qty != null) {
            this.qty.setText(qty != null ? qty : "0");
        }

        // Set Metode
        if (this.metode != null) {
            this.metode.setText(metode != null ? metode : "-");
        }

        // Set Total
        if (totalharga != null) {
            totalharga.setText(total != null ? total : "Rp 0");
        }

        // Set Uang Bayar
        if (uangBayar != null) {
            uangBayar.setText(bayar != null ? bayar : "Rp 0");
        }

        // Set Kembalian
        if (lblKembalian != null) {
            lblKembalian.setText(kembali != null ? kembali : "Rp 0");
        }
    }
}