package SistemFotocopy;

import Database.DBConnection;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import java.net.URL;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.NumberFormat;
import java.time.Year;
import java.util.Locale;
import java.util.ResourceBundle;

public class DashboardKaryawan implements Initializable {

    @FXML
    private BarChart<String, Number> ChartPenjualan;

    @FXML
    private Label Pendapatan;

    @FXML
    private Label Pengeluaran;

    @FXML
    private Label Saldo;

    @FXML
    private VBox StatusMesin;

    private DBConnection db = new DBConnection();
    private int currentYear = Year.now().getValue();

    // Timer untuk auto refresh (setiap 5 detik)
    private PauseTransition refreshTimer;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        loadAllData();
        setupAutoRefresh();
    }

    private void setupAutoRefresh() {
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
        loadStatusMesin();
    }

    private void refreshAllData() {
        // Ganti lambda dengan anonymous class
        Platform.runLater(new Runnable() {
            @Override
            public void run() {
                loadKartuRingkasan();
                loadGrafikPenjualan(currentYear);
                loadStatusMesin();
            }
        });
    }

    // =============================================================
    // KARTU RINGKASAN - PAKAI UDF ✅
    // =============================================================
    private void loadKartuRingkasan() {
        try {
            // 1. Pendapatan Bulan Ini
            String queryPendapatan = "SELECT dbo.f_PendapatanBulanIni() AS Total";
            try (PreparedStatement ps = db.getConnection().prepareStatement(queryPendapatan);
                 ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Pendapatan.setText(formatRupiah(rs.getDouble("Total")));
                }
            } catch (SQLException e) {
                System.err.println("ERROR f_PendapatanBulanIni: " + e.getMessage());
                Pendapatan.setText("Rp. 0");
            }

            // 2. Pengeluaran Bulan Ini
            String queryPengeluaran = "SELECT dbo.f_PengeluaranBulanIni() AS Total";
            try (PreparedStatement ps = db.getConnection().prepareStatement(queryPengeluaran);
                 ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Pengeluaran.setText(formatRupiah(rs.getDouble("Total")));
                }
            } catch (SQLException e) {
                System.err.println("ERROR f_PengeluaranBulanIni: " + e.getMessage());
                Pengeluaran.setText("Rp. 0");
            }

            // 3. Saldo Kas
            String querySaldo = "SELECT dbo.f_SaldoKas() AS Total";
            try (PreparedStatement ps = db.getConnection().prepareStatement(querySaldo);
                 ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Saldo.setText(formatRupiah(rs.getDouble("Total")));
                }
            } catch (SQLException e) {
                System.err.println("ERROR f_SaldoKas: " + e.getMessage());
                Saldo.setText("Rp. 0");
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String formatRupiah(double nominal) {
        NumberFormat formatRp = NumberFormat.getNumberInstance(new Locale("in", "ID"));
        return "Rp. " + formatRp.format(nominal);
    }

    // =============================================================
    // GRAFIK PENJUALAN - PAKAI UDF ✅
    // =============================================================
    private void loadGrafikPenjualan(int tahun) {
        if (ChartPenjualan == null) return;

        ChartPenjualan.getData().clear();
        ChartPenjualan.setLegendVisible(true);
        ChartPenjualan.setTitle("Grafik Pendapatan & Pengeluaran " + tahun);

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

            ChartPenjualan.getData().addAll(seriesPendapatan, seriesPengeluaran);

            // Warna Hijau untuk Pendapatan
            for (XYChart.Data<String, Number> data : seriesPendapatan.getData()) {
                terapkanWarnaBatang(data, "#22C55E");
            }

            // Warna Merah untuk Pengeluaran
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

    // =============================================================
    // STATUS MESIN - PAKAI VIEW ✅
    // =============================================================
    private void loadStatusMesin() {
        if (StatusMesin == null) return;

        StatusMesin.getChildren().clear();

        try {
            // PAKAI VIEW v_TampilSemuaMesin + status maintenance terbaru
            String query =
                    "SELECT " +
                            "    M.ID_Mesin, " +
                            "    M.Nama_Mesin, " +
                            "    COALESCE(MM.Status_Maintenance, 'aktif') AS Status, " +
                            "    MM.Tanggal_Maintenance_Mesin " +
                            "FROM v_TampilSemuaMesin M " +
                            "LEFT JOIN ( " +
                            "    SELECT *, ROW_NUMBER() OVER (PARTITION BY ID_Mesin ORDER BY Tanggal_Maintenance_Mesin DESC) AS rn " +
                            "    FROM Maintenance_Mesin " +
                            ") MM ON M.ID_Mesin = MM.ID_Mesin AND MM.rn = 1 " +
                            "ORDER BY M.Nama_Mesin";

            try (PreparedStatement ps = db.getConnection().prepareStatement(query);
                 ResultSet rs = ps.executeQuery()) {

                while (rs.next()) {
                    String idMesin = rs.getString("ID_Mesin");
                    String namaMesin = rs.getString("Nama_Mesin");
                    String status = rs.getString("Status");

                    if (status == null) {
                        status = "aktif";
                    }

                    javafx.scene.layout.HBox card = new javafx.scene.layout.HBox();
                    card.setStyle(
                            "-fx-background-color: white; " +
                                    "-fx-padding: 12 16; " +
                                    "-fx-border-color: #E5E7EB; " +
                                    "-fx-border-width: 1; " +
                                    "-fx-border-radius: 6; " +
                                    "-fx-background-radius: 6; " +
                                    "-fx-spacing: 12; " +
                                    "-fx-alignment: CENTER_LEFT;"
                    );

                    javafx.scene.control.Label iconLabel = new javafx.scene.control.Label();
                    iconLabel.setStyle("-fx-font-size: 20px;");

                    javafx.scene.control.Label nameLabel = new javafx.scene.control.Label(
                            idMesin + " - " + namaMesin
                    );
                    nameLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 13px; -fx-text-fill: #1A1A2E;");
                    nameLabel.setMaxWidth(Double.MAX_VALUE);
                    javafx.scene.layout.HBox.setHgrow(nameLabel, javafx.scene.layout.Priority.ALWAYS);

                    javafx.scene.control.Label statusLabel = new javafx.scene.control.Label();

                    if (status.equalsIgnoreCase("rusak")) {
                        iconLabel.setText("🔴");
                        statusLabel.setText("RUSAK");
                        statusLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 12px; -fx-text-fill: #EF4444;");
                        card.setStyle(
                                "-fx-background-color: #FEF2F2; " +
                                        "-fx-padding: 12 16; " +
                                        "-fx-border-color: #FCA5A5; " +
                                        "-fx-border-width: 1; " +
                                        "-fx-border-radius: 6; " +
                                        "-fx-background-radius: 6; " +
                                        "-fx-spacing: 12; " +
                                        "-fx-alignment: CENTER_LEFT;"
                        );
                    } else if (status.equalsIgnoreCase("selesai")) {
                        iconLabel.setText("✅");
                        statusLabel.setText("SELESAI");
                        statusLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 12px; -fx-text-fill: #10B981;");
                    } else {
                        iconLabel.setText("🟢");
                        statusLabel.setText("AKTIF");
                        statusLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 12px; -fx-text-fill: #22C55E;");
                    }

                    card.getChildren().addAll(iconLabel, nameLabel, statusLabel);
                    StatusMesin.getChildren().add(card);
                }
            }

            if (StatusMesin.getChildren().isEmpty()) {
                javafx.scene.control.Label emptyLabel = new javafx.scene.control.Label(
                        "Belum ada data mesin"
                );
                emptyLabel.setStyle("-fx-text-fill: #9CA3AF; -fx-font-style: italic; -fx-padding: 20;");
                emptyLabel.setMaxWidth(Double.MAX_VALUE);
                emptyLabel.setAlignment(javafx.geometry.Pos.CENTER);
                StatusMesin.getChildren().add(emptyLabel);
            }

        } catch (SQLException e) {
            e.printStackTrace();
            javafx.scene.control.Label errorLabel = new javafx.scene.control.Label(
                    "Gagal memuat status mesin: " + e.getMessage()
            );
            errorLabel.setStyle("-fx-text-fill: #EF4444; -fx-padding: 20;");
            errorLabel.setMaxWidth(Double.MAX_VALUE);
            errorLabel.setAlignment(javafx.geometry.Pos.CENTER);
            StatusMesin.getChildren().add(errorLabel);
        }
    }

    // =============================================================
    // METHOD UNTUK REFRESH MANUAL
    // =============================================================
    public void refreshDashboard() {
        refreshAllData();
    }

    public void stopAutoRefresh() {
        if (refreshTimer != null) {
            refreshTimer.stop();
        }
    }
}