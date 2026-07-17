package SistemFotocopy;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.IntegerProperty;

import Database.DBConnection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.NumberFormat;
import java.time.Year;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class Dashboard {

    @FXML
    private Label HasilKaryawanAktif;

    @FXML
    private Label HasilPendapat;

    @FXML
    private Label HasilPengeluaran;

    @FXML
    private Label HasilSaldo;

    @FXML
    private Label HasilStock;

    @FXML
    private Label PrestaseKeuntungan;

    @FXML
    private Label TotalOmsetBulan;

    // Ganti dari TableColumn<?,?> -> TableColumn<ProdukModel, ...>
    @FXML
    private TableColumn<ProdukModel, String> colMiniBarang;

    // =============================================================
    // UBAH: colMiniStock sekarang menampilkan STOCK (Integer)
    // =============================================================
    @FXML
    private TableColumn<ProdukModel, Number> colMiniStock;

    // Ganti dari TableView<?> -> TableView<ProdukModel>
    @FXML
    private TableView<ProdukModel> miniStockTable;

    @FXML
    private BarChart<String, Number> salesBarChart;

    private DBConnection db = new DBConnection();

    @FXML
    public void initialize() {
        loadKartuRingkasan();
        loadGrafikPenjualan(Year.now().getValue());
        loadViewBarang();
    }

    // =========================================================
    // KARTU RINGKASAN (Saldo, Pendapatan, Pengeluaran, dst)
    // =========================================================
    private void loadKartuRingkasan() {
        loadKaryawanAktif();
        loadKetersediaanStok();

        double pendapatanBulanIni = hitungTotal(
                "SELECT ISNULL(SUM(Total_Harga), 0) AS Total FROM Penjualan " +
                        "WHERE Status_Penjualan = 'Lunas' " +
                        "AND MONTH(Tanggal_Penjualan) = MONTH(GETDATE()) " +
                        "AND YEAR(Tanggal_Penjualan) = YEAR(GETDATE())"
        );

        double pengeluaranBulanIni = hitungTotal(
                "SELECT ISNULL(SUM(Total_Harga), 0) AS Total FROM Pembelian_Stok " +
                        "WHERE Status_Pembayaran = 'Lunas' " +
                        "AND MONTH(Tanggal_Pembelian) = MONTH(GETDATE()) " +
                        "AND YEAR(Tanggal_Pembelian) = YEAR(GETDATE())"
        );

        double pendapatanSemua = hitungTotal(
                "SELECT ISNULL(SUM(Total_Harga), 0) AS Total FROM Penjualan WHERE Status_Penjualan = 'Lunas'"
        );

        double pengeluaranSemua = hitungTotal(
                "SELECT ISNULL(SUM(Total_Harga), 0) AS Total FROM Pembelian_Stok WHERE Status_Pembayaran = 'Lunas'"
        );

        double saldoKas = pendapatanSemua - pengeluaranSemua;
        double keuntunganBulanIni = pendapatanBulanIni - pengeluaranBulanIni;
        double persentaseKeuntungan = pendapatanBulanIni > 0
                ? (keuntunganBulanIni / pendapatanBulanIni) * 100
                : 0;

        HasilPendapat.setText(formatRupiah(pendapatanBulanIni));
        HasilPengeluaran.setText(formatRupiah(pengeluaranBulanIni));
        HasilSaldo.setText(formatRupiah(saldoKas));
        TotalOmsetBulan.setText(formatRupiah(pendapatanBulanIni));
        PrestaseKeuntungan.setText(String.format("%.1f%%", persentaseKeuntungan));
    }

    private void loadKaryawanAktif() {
        String query = "SELECT COUNT(*) AS Total FROM Pegawai WHERE Status_Pegawai = 'aktif'";
        try (PreparedStatement ps = db.getConnection().prepareStatement(query);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                HasilKaryawanAktif.setText(String.valueOf(rs.getInt("Total")));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void loadKetersediaanStok() {
        String query = "SELECT " +
                "CAST(SUM(CASE WHEN Stok > 0 THEN 1 ELSE 0 END) AS FLOAT) AS Tersedia, " +
                "COUNT(*) AS Total " +
                "FROM Produk";
        try (PreparedStatement ps = db.getConnection().prepareStatement(query);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                double tersedia = rs.getDouble("Tersedia");
                int total = rs.getInt("Total");
                double persentase = total > 0 ? (tersedia / total) * 100 : 0;
                HasilStock.setText(String.format("%.0f%%", persentase));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // Helper generik: jalankan query SUM/COUNT 1 baris dengan kolom hasil bernama "Total"
    private double hitungTotal(String query) {
        double hasil = 0;
        try (PreparedStatement ps = db.getConnection().prepareStatement(query);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                hasil = rs.getDouble("Total");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return hasil;
    }

    private String formatRupiah(double nominal) {
        NumberFormat formatRp = NumberFormat.getNumberInstance(new Locale("in", "ID"));
        return "Rp. " + formatRp.format(nominal);
    }

    // =========================================================
    // GRAFIK PENJUALAN — pakai UDF fn_DataGrafikBulanan(@p_Tahun)
    // Satu batang per bulan (JAN-DEC), warna berdasarkan tren:
    // - HIJAU  : naik dibanding bulan sebelumnya
    // - MERAH  : turun dibanding bulan sebelumnya
    // - ORANYE : bulan pertama / nilai sama dengan bulan sebelumnya
    // =========================================================

    private void loadGrafikPenjualan(int tahun) {
        if (salesBarChart == null) return;

        salesBarChart.getData().clear();
        salesBarChart.setLegendVisible(false);
        salesBarChart.setTitle("Grafik Omset Bersih " + tahun);

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Omset Bersih");

        List<Double> nilaiBulanan = new ArrayList<>();

        String sql = "SELECT * FROM fn_DataGrafikBulanan(?)";

        try (PreparedStatement ps = db.getConnection().prepareStatement(sql)) {
            ps.setInt(1, tahun);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String bulan = rs.getString("Bulan");
                    double pendapatan = rs.getDouble("Pendapatan");
                    double pengeluaran = rs.getDouble("Pengeluaran");
                    double bersih = pendapatan - pengeluaran;

                    series.getData().add(new XYChart.Data<>(bulan, bersih));
                    nilaiBulanan.add(bersih);
                }
            }

            salesBarChart.getData().add(series);

            // Warnai tiap batang sesuai tren naik/turun/netral
            for (int i = 0; i < series.getData().size(); i++) {
                XYChart.Data<String, Number> data = series.getData().get(i);
                double sekarang = nilaiBulanan.get(i);
                double sebelumnya = (i == 0) ? sekarang : nilaiBulanan.get(i - 1);

                String warna;
                if (i == 0 || sekarang == sebelumnya) {
                    warna = "#F97316"; // ORANYE - netral / bulan pertama
                } else if (sekarang > sebelumnya) {
                    warna = "#22C55E"; // HIJAU - naik
                } else {
                    warna = "#EF4444"; // MERAH - turun
                }

                terapkanWarnaBatang(data, warna);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // Node batang belum tentu langsung tersedia saat data ditambahkan,
    // jadi kita pasang listener kalau belum ada, atau langsung set kalau sudah ada.
    private void terapkanWarnaBatang(XYChart.Data<String, Number> data, String warnaHex) {
        if (data.getNode() != null) {
            data.getNode().setStyle("-fx-bar-fill: " + warnaHex + ";");
        } else {
            data.nodeProperty().addListener((obs, nodeLama, nodeBaru) -> {
                if (nodeBaru != null) {
                    nodeBaru.setStyle("-fx-bar-fill: " + warnaHex + ";");
                }
            });
        }
    }

    // Panggil ini kalau nanti Nabil tambah ComboBox pilih tahun
    @FXML
    void onGantiTahun(int tahunDipilih) {
        loadGrafikPenjualan(tahunDipilih);
    }

    // =========================================================
    // VIEW BARANG (mini table) — Nama Barang + STOCK (bukan Harga)
    // Hanya produk berkategori 'barang' (bukan 'layanan')
    // =========================================================

    private void loadViewBarang() {
        if (miniStockTable == null) return;

        // =============================================================
        // UBAH: colMiniStock sekarang menampilkan STOK (bukan Harga)
        // =============================================================
        colMiniBarang.setCellValueFactory(d -> d.getValue().namaBarangProperty());
        colMiniStock.setCellValueFactory(d -> d.getValue().stokProperty());

        // =============================================================
        // Format kolom stock - tampilkan angka biasa (tanpa format Rupiah)
        // =============================================================
        colMiniStock.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(Number item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    // Tampilkan stok sebagai angka biasa
                    setText(String.valueOf(item.intValue()));

                    // Opsional: beri warna merah jika stok <= 0
                    if (item.intValue() <= 0) {
                        setStyle("-fx-text-fill: #EF4444; -fx-font-weight: bold;");
                    } else {
                        setStyle("-fx-text-fill: #16A34A;");
                    }
                }
            }
        });

        ObservableList<ProdukModel> daftarBarang = FXCollections.observableArrayList();

        // =============================================================
        // UBAH QUERY: ambil Stok (bukan Harga)
        // =============================================================
        String sql = "SELECT Nama_Barang, Stok FROM Produk " +
                "WHERE Kategori_Produk = 'barang' " +
                "ORDER BY Nama_Barang";

        try (PreparedStatement ps = db.getConnection().prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                daftarBarang.add(new ProdukModel(
                        rs.getString("Nama_Barang"),
                        rs.getInt("Stok")  // Ambil Stok sebagai integer
                ));
            }
            miniStockTable.setItems(daftarBarang);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // =========================================================
    // MODEL — Produk (untuk mini table View Barang)
    // =========================================================

    public static class ProdukModel {
        private final StringProperty namaBarang;
        private final IntegerProperty stok;  // UBAH: dari DoubleProperty jadi IntegerProperty

        // =============================================================
        // UBAH CONSTRUCTOR: parameter stok sebagai int
        // =============================================================
        public ProdukModel(String namaBarang, int stok) {
            this.namaBarang = new SimpleStringProperty(namaBarang);
            this.stok = new SimpleIntegerProperty(stok);
        }

        public StringProperty namaBarangProperty() { return namaBarang; }
        public IntegerProperty stokProperty() { return stok; }  // UBAH: return IntegerProperty
    }
}