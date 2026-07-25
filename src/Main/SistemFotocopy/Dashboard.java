package SistemFotocopy;

import Database.DBConnection;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.util.Duration;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.NumberFormat;
import java.time.Year;
import java.util.Locale;

public class Dashboard {

    @FXML private Label HasilKaryawanAktif;
    @FXML private Label HasilPendapat;
    @FXML private Label HasilPengeluaran;
    @FXML private Label HasilSaldo;
    @FXML private Label HasilStock;
    @FXML private Label PrestaseKeuntungan;
    @FXML private Label TotalOmsetBulan;

    @FXML private TableColumn<ProdukModel, String> colMiniBarang;
    @FXML private TableColumn<ProdukModel, Number> colMiniStock;
    @FXML private TableView<ProdukModel> miniStockTable;

    @FXML private BarChart<String, Number> salesBarChart;

    private DBConnection db = new DBConnection();
    private int currentYear = Year.now().getValue();

    private PauseTransition refreshTimer;

    @FXML
    public void initialize() {
        loadAllData();

        refreshTimer = new PauseTransition(Duration.seconds(5));
        // Ganti lambda dengan anonymous class
        refreshTimer.setOnFinished(new javafx.event.EventHandler<javafx.event.ActionEvent>() {
            @Override
            public void handle(javafx.event.ActionEvent event) {
                refreshAllData();
                refreshTimer.playFromStart();
            }
        });
        refreshTimer.play();
    }

    private void loadAllData() {
        loadKartuRingkasan();
        loadGrafikPenjualan(currentYear);
        loadViewBarang();
    }

    private void refreshAllData() {
        Platform.runLater(new Runnable() {
            @Override
            public void run() {
                loadKartuRingkasan();
                loadGrafikPenjualan(currentYear);
                loadViewBarang();
            }
        });
    }

    // =========================================================
    // KARTU RINGKASAN - PAKAI UDF ✅
    // =========================================================
    private void loadKartuRingkasan() {
        try {
            // 1. Karyawan Aktif - PAKAI UDF YANG SUDAH ADA
            String queryKaryawan = "SELECT dbo.f_TotalPegawaiAktif() AS Total";
            try (PreparedStatement ps = db.getConnection().prepareStatement(queryKaryawan);
                 ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    HasilKaryawanAktif.setText(String.valueOf(rs.getInt("Total")));
                }
            } catch (SQLException e) {
                System.err.println("ERROR f_TotalPegawaiAktif: " + e.getMessage());
                HasilKaryawanAktif.setText("0");
            }

            // 2. Pendapatan Bulan Ini
            String queryPendapatan = "SELECT dbo.f_PendapatanBulanIni() AS Total";
            try (PreparedStatement ps = db.getConnection().prepareStatement(queryPendapatan);
                 ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    double pendapatan = rs.getDouble("Total");
                    HasilPendapat.setText(formatRupiah(pendapatan));
                    TotalOmsetBulan.setText(formatRupiah(pendapatan));
                }
            } catch (SQLException e) {
                System.err.println("ERROR f_PendapatanBulanIni: " + e.getMessage());
                HasilPendapat.setText("Rp. 0");
                TotalOmsetBulan.setText("Rp. 0");
            }

            // 3. Pengeluaran Bulan Ini
            String queryPengeluaran = "SELECT dbo.f_PengeluaranBulanIni() AS Total";
            try (PreparedStatement ps = db.getConnection().prepareStatement(queryPengeluaran);
                 ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    HasilPengeluaran.setText(formatRupiah(rs.getDouble("Total")));
                }
            } catch (SQLException e) {
                System.err.println("ERROR f_PengeluaranBulanIni: " + e.getMessage());
                HasilPengeluaran.setText("Rp. 0");
            }

            // 4. Saldo Kas
            String querySaldo = "SELECT dbo.f_SaldoKas() AS Total";
            try (PreparedStatement ps = db.getConnection().prepareStatement(querySaldo);
                 ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    HasilSaldo.setText(formatRupiah(rs.getDouble("Total")));
                }
            } catch (SQLException e) {
                System.err.println("ERROR f_SaldoKas: " + e.getMessage());
                HasilSaldo.setText("Rp. 0");
            }

            // 5. Persentase Stok Tersedia
            String queryStok = "SELECT dbo.f_PersentaseStokTersedia() AS Persentase";
            try (PreparedStatement ps = db.getConnection().prepareStatement(queryStok);
                 ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    HasilStock.setText(String.format("%.0f%%", rs.getDouble("Persentase")));
                }
            } catch (SQLException e) {
                System.err.println("ERROR f_PersentaseStokTersedia: " + e.getMessage());
                HasilStock.setText("0%");
            }

            // 6. Persentase Keuntungan
            String queryKeuntungan = "SELECT dbo.f_PersentaseKeuntungan() AS Persentase, dbo.f_KeuntunganBulanIni() AS Keuntungan";
            try (PreparedStatement ps = db.getConnection().prepareStatement(queryKeuntungan);
                 ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    double persentase = rs.getDouble("Persentase");
                    double keuntungan = rs.getDouble("Keuntungan");

                    String persentaseText;
                    if (keuntungan >= 0) {
                        persentaseText = String.format("+%.1f%%", persentase);
                        PrestaseKeuntungan.setStyle("-fx-text-fill: #22C55E; -fx-font-weight: bold;");
                    } else {
                        persentaseText = String.format("%.1f%%", persentase);
                        PrestaseKeuntungan.setStyle("-fx-text-fill: #EF4444; -fx-font-weight: bold;");
                    }
                    PrestaseKeuntungan.setText(persentaseText);
                }
            } catch (SQLException e) {
                System.err.println("ERROR f_PersentaseKeuntungan: " + e.getMessage());
                PrestaseKeuntungan.setText("0%");
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // =========================================================
    // GRAFIK PENJUALAN - PAKAI UDF ✅
    // =========================================================
    private void loadGrafikPenjualan(int tahun) {
        if (salesBarChart == null) return;

        salesBarChart.getData().clear();
        salesBarChart.setLegendVisible(true);
        salesBarChart.setTitle("Grafik Pendapatan & Pengeluaran " + tahun);

        XYChart.Series<String, Number> seriesPendapatan = new XYChart.Series<>();
        seriesPendapatan.setName("Pendapatan");

        XYChart.Series<String, Number> seriesPengeluaran = new XYChart.Series<>();
        seriesPengeluaran.setName("Pengeluaran");

        String sql = "SELECT Bulan, Pendapatan, Pengeluaran FROM dbo.f_DataGrafikBulanan(?)";

        try (PreparedStatement ps = db.getConnection().prepareStatement(sql)) {
            ps.setInt(1, tahun);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String bulan = rs.getString("Bulan");
                    double pendapatan = rs.getDouble("Pendapatan");
                    double pengeluaran = rs.getDouble("Pengeluaran");

                    seriesPendapatan.getData().add(new XYChart.Data<>(bulan, pendapatan));
                    seriesPengeluaran.getData().add(new XYChart.Data<>(bulan, pengeluaran));
                }
            }

            salesBarChart.getData().addAll(seriesPendapatan, seriesPengeluaran);

            // Ganti lambda dengan anonymous class
            for (XYChart.Data<String, Number> data : seriesPendapatan.getData()) {
                terapkanWarnaBatang(data, "#22C55E");
            }
            for (XYChart.Data<String, Number> data : seriesPengeluaran.getData()) {
                terapkanWarnaBatang(data, "#EF4444");
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void terapkanWarnaBatang(XYChart.Data<String, Number> data, String warnaHex) {
        if (data.getNode() != null) {
            data.getNode().setStyle("-fx-bar-fill: " + warnaHex + ";");
        } else {
            // Ganti lambda dengan anonymous class
            data.nodeProperty().addListener(new javafx.beans.value.ChangeListener<javafx.scene.Node>() {
                @Override
                public void changed(javafx.beans.value.ObservableValue<? extends javafx.scene.Node> observable,
                                    javafx.scene.Node nodeLama,
                                    javafx.scene.Node nodeBaru) {
                    if (nodeBaru != null) {
                        nodeBaru.setStyle("-fx-bar-fill: " + warnaHex + ";");
                    }
                }
            });
        }
    }

    @FXML
    void onGantiTahun(int tahunDipilih) {
        this.currentYear = tahunDipilih;
        loadGrafikPenjualan(tahunDipilih);
    }

    // =========================================================
    // VIEW BARANG
    // =========================================================
    private void loadViewBarang() {
        if (miniStockTable == null) return;

        // Ganti lambda dengan anonymous class
        colMiniBarang.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("namaBarang"));
        colMiniStock.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("stok"));

        colMiniStock.setCellFactory(new javafx.util.Callback<TableColumn<ProdukModel, Number>, TableCell<ProdukModel, Number>>() {
            @Override
            public TableCell<ProdukModel, Number> call(TableColumn<ProdukModel, Number> param) {
                return new TableCell<ProdukModel, Number>() {
                    @Override
                    protected void updateItem(Number item, boolean empty) {
                        super.updateItem(item, empty);
                        if (empty || item == null) {
                            setText(null);
                            setStyle("");
                        } else {
                            setText(String.valueOf(item.intValue()));
                            if (item.intValue() <= 0) {
                                setStyle("-fx-text-fill: #EF4444; -fx-font-weight: bold;");
                            } else if (item.intValue() <= 10) {
                                setStyle("-fx-text-fill: #F59E0B; -fx-font-weight: bold;");
                            } else {
                                setStyle("-fx-text-fill: #16A34A;");
                            }
                        }
                    }
                };
            }
        });

        ObservableList<ProdukModel> daftarBarang = FXCollections.observableArrayList();

        String sql = "SELECT Nama_Barang, Stok FROM Produk " +
                "WHERE Kategori_Produk = 'Barang' " +
                "ORDER BY Nama_Barang";

        try (PreparedStatement ps = db.getConnection().prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                daftarBarang.add(new ProdukModel(
                        rs.getString("Nama_Barang"),
                        rs.getInt("Stok")
                ));
            }
            miniStockTable.setItems(daftarBarang);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private String formatRupiah(double nominal) {
        NumberFormat formatRp = NumberFormat.getNumberInstance(new Locale("in", "ID"));
        return "Rp. " + formatRp.format(nominal);
    }

    public void refreshDashboard() {
        refreshAllData();
    }

    public void stopAutoRefresh() {
        if (refreshTimer != null) {
            refreshTimer.stop();
        }
    }

    // =========================================================
    // MODEL — Produk
    // =========================================================
    public static class ProdukModel {
        private final StringProperty namaBarang;
        private final IntegerProperty stok;

        public ProdukModel(String namaBarang, int stok) {
            this.namaBarang = new SimpleStringProperty(namaBarang);
            this.stok = new SimpleIntegerProperty(stok);
        }

        public StringProperty namaBarangProperty() { return namaBarang; }
        public IntegerProperty stokProperty() { return stok; }
    }
}