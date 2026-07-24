package SistemFotocopy;

import Database.DBConnection;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.IOException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class DataPenjualan {

    // =====================================================================
    // FXML BINDINGS
    // =====================================================================

    @FXML private Button btnNext;
    @FXML private Button btnPrev;

    @FXML private TableColumn<PenjualanModel, String> colIdPenjualan;
    @FXML private TableColumn<PenjualanModel, String> colPegawai;
    @FXML private TableColumn<PenjualanModel, String> colTanggal;
    @FXML private TableColumn<PenjualanModel, Number> colTotal;
    @FXML private TableColumn<PenjualanModel, Number> colUangbayar;
    @FXML private TableColumn<PenjualanModel, Number> colKembalian;
    @FXML private TableColumn<PenjualanModel, String> colMetode;
    @FXML private TableColumn<PenjualanModel, String> colStatus;
    @FXML private TableColumn<PenjualanModel, Void>   colAksi;

    @FXML private Label lblBelumLunas;
    @FXML private Label lblInfoData;
    @FXML private Label lblLunas;
    @FXML private Label lblPageInfo;
    @FXML private Label lblTotalTransaksi;

    @FXML private TableView<PenjualanModel> tablePembelian;
    @FXML private TextField txtCari;

    // =====================================================================
    // STATE
    // =====================================================================

    private final DBConnection db = new DBConnection();

    private final ObservableList<PenjualanModel> masterData = FXCollections.observableArrayList();
    private FilteredList<PenjualanModel> filteredData;
    private ObservableList<PenjualanModel> currentPageData = FXCollections.observableArrayList();

    private static final int ITEMS_PER_PAGE = 5;
    private int currentPage = 0;
    private int totalPages = 0;
    private int totalItems = 0;

    // =====================================================================
    // INITIALIZE
    // =====================================================================

    @FXML
    public void initialize() {
        setupTabel();
        setupKolomAksi();
        setupSearch();

        loadDataPenjualan();
        hitungStatCard();

        // Setup pagination buttons
        btnPrev.setOnAction(e -> handlePrevPage());
        btnNext.setOnAction(e -> handleNextPage());
    }

    // =====================================================================
    // SETUP KOLOM TABEL
    // =====================================================================

    private void setupTabel() {
        colIdPenjualan.setCellValueFactory(d -> d.getValue().idPenjualanProperty());
        colPegawai.setCellValueFactory(d -> d.getValue().karyawanProperty());
        colTanggal.setCellValueFactory(d -> d.getValue().tanggalProperty());
        colTotal.setCellValueFactory(d -> d.getValue().totalHargaProperty());
        colUangbayar.setCellValueFactory(d -> d.getValue().uangBayarProperty());
        colKembalian.setCellValueFactory(d -> d.getValue().kembalianProperty());
        colMetode.setCellValueFactory(d -> d.getValue().metodeProperty());
        colStatus.setCellValueFactory(d -> d.getValue().statusProperty());

        // Format kolom uang
        colTotal.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(Number item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : formatRupiah(item.doubleValue()));
            }
        });
        colUangbayar.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(Number item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : formatRupiah(item.doubleValue()));
            }
        });
        colKembalian.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(Number item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : formatRupiah(item.doubleValue()));
            }
        });

        // Warna status
        colStatus.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item);
                    if (item.equalsIgnoreCase("Lunas")) {
                        setStyle("-fx-text-fill: #16A34A; -fx-font-weight: bold;");
                    } else {
                        setStyle("-fx-text-fill: #BA1A1A; -fx-font-weight: bold;");
                    }
                }
            }
        });
    }

    // =====================================================================
    // KOLOM AKSI — TOMBOL DETAIL MENGGUNAKAN FXML
    // =====================================================================

    private void setupKolomAksi() {
        colAksi.setCellFactory(col -> new TableCell<>() {
            private final Button btnDetail = new Button("Detail");
            {
                btnDetail.getStyleClass().add("btn-detail-aksi");
                btnDetail.setStyle("-fx-background-color: #3B82F6; -fx-text-fill: white; -fx-font-weight: bold; -fx-cursor: hand; -fx-background-radius: 5; -fx-padding: 4 12 4 12;");
                btnDetail.setOnAction(e -> {
                    PenjualanModel penjualan = getTableView().getItems().get(getIndex());
                    if (penjualan != null) {
                        bukaDetailPenjualanFXML(penjualan);
                    }
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setAlignment(Pos.CENTER);
                setGraphic(empty ? null : btnDetail);
            }
        });
    }

    // =====================================================================
    // BUKA DETAIL PENJUALAN MENGGUNAKAN FXML
    // =====================================================================

    // =====================================================================
// BUKA DETAIL PENJUALAN MENGGUNAKAN FXML
// =====================================================================

    private void bukaDetailPenjualanFXML(PenjualanModel penjualan) {
        try {
            System.out.println("Membuka detail untuk: " + penjualan.getIdPenjualan());

            // Load FXML DetailPenjualan
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/LayoutSistemFotocopy/DetailPenjualan.fxml"));

            if (loader.getLocation() == null) {
                tampilAlert(Alert.AlertType.ERROR, "Error", "File DetailPenjualan.fxml tidak ditemukan!");
                return;
            }

            Parent root = loader.load();
            DetailPenjualan controller = loader.getController();

            // Load detail penjualan dari database
            ObservableList<DetailPenjualanModel> detailList = loadDetailPenjualan(penjualan.getIdPenjualan());

            // Format tanggal
            String tanggal = penjualan.getTanggal();
            try {
                LocalDate date = LocalDate.parse(tanggal);
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
                tanggal = date.format(formatter);
            } catch (Exception e) {
                // gunakan apa adanya
            }

            // Buat string produk
            StringBuilder produkBuilder = new StringBuilder();
            int totalQty = 0;
            int counter = 1;

            for (DetailPenjualanModel detail : detailList) {
                produkBuilder.append(counter).append(". ")
                        .append(detail.getNamaProduk())
                        .append("\n")
                        .append("   ")
                        .append(detail.getJumlah()).append(" x ")
                        .append(formatRupiah(detail.getHargaSatuan()))
                        .append("\n");
                totalQty += detail.getJumlah();
                counter++;
            }

            if (detailList.isEmpty()) {
                produkBuilder.append("Tidak ada detail produk");
            }

            // PAKAI METHOD DENGAN 9 PARAMETER
            controller.setData(
                    penjualan.getIdPenjualan(),                 // ID
                    tanggal,                                    // Tanggal
                    penjualan.getKaryawan(),                    // Pegawai
                    produkBuilder.toString(),                   // Produk
                    String.valueOf(totalQty),                   // QTY
                    penjualan.getMetode(),                      // Metode
                    formatRupiah(penjualan.getTotalHarga()),    // Total
                    formatRupiah(penjualan.getUangBayar()),     // Bayar
                    formatRupiah(penjualan.getKembalian())      // Kembali
            );

            // Tampilkan stage
            Stage detailStage = new Stage();
            detailStage.setTitle("Detail Penjualan - " + penjualan.getIdPenjualan());
            Scene scene = new Scene(root);
            detailStage.setScene(scene);
            detailStage.setResizable(false);
            detailStage.initModality(Modality.APPLICATION_MODAL);
            detailStage.sizeToScene();
            detailStage.centerOnScreen();
            detailStage.showAndWait();

        } catch (IOException e) {
            e.printStackTrace();
            tampilAlert(Alert.AlertType.ERROR, "Error", "Gagal menampilkan detail penjualan: " + e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            tampilAlert(Alert.AlertType.ERROR, "Error", "Terjadi kesalahan: " + e.getMessage());
        }
    }

    // =====================================================================
    // LOAD DETAIL PENJUALAN - MENGGUNAKAN UDF
    // =====================================================================

    private ObservableList<DetailPenjualanModel> loadDetailPenjualan(String idPenjualan) {
        ObservableList<DetailPenjualanModel> list = FXCollections.observableArrayList();
        String sql = "SELECT * FROM dbo.fn_DetailPenjualanNota(?)";

        try (PreparedStatement ps = db.getConnection().prepareStatement(sql)) {
            ps.setString(1, idPenjualan);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(new DetailPenjualanModel(
                            rs.getString("Nama Produk"),
                            rs.getInt("Jumlah"),
                            rs.getDouble("Harga Satuan")
                    ));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            tampilAlert(Alert.AlertType.ERROR, "Gagal memuat detail penjualan",
                    "Error: " + e.getMessage());
        }
        return list;
    }

    // =====================================================================
    // SEARCH LISTENER
    // =====================================================================

    private void setupSearch() {
        txtCari.textProperty().addListener((obs, lama, baru) -> {
            if (filteredData == null) return;
            String kw = baru == null ? "" : baru.trim().toLowerCase();

            filteredData.setPredicate(p ->
                    kw.isEmpty()
                            || p.getIdPenjualan().toLowerCase().contains(kw)
                            || p.getKaryawan().toLowerCase().contains(kw)
                            || p.getMetode().toLowerCase().contains(kw)
                            || p.getStatus().toLowerCase().contains(kw)
            );
            currentPage = 0;
            applyPagination();
        });
    }

    // =====================================================================
    // LOAD DATA PENJUALAN
    // =====================================================================

    private void loadDataPenjualan() {
        masterData.clear();

        String sql = "SELECT * FROM v_TampilPenjualan ORDER BY ID_Penjualan DESC";

        try (java.sql.Statement st = db.getConnection().createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                masterData.add(new PenjualanModel(
                        rs.getString("ID_Penjualan"),
                        rs.getString("Karyawan"),
                        rs.getDate("Tanggal_Penjualan").toString(),
                        rs.getDouble("Total_Harga"),
                        rs.getDouble("Uang_Bayar"),
                        rs.getDouble("Kembalian"),
                        rs.getString("Metode_Pembayaran"),
                        rs.getString("Status_Penjualan")
                ));
            }

            filteredData = new FilteredList<>(masterData, p -> true);

            currentPage = 0;
            applyPagination();

        } catch (SQLException e) {
            tampilAlert(Alert.AlertType.ERROR, "Gagal memuat data penjualan", e.getMessage());
        }
    }

    // =====================================================================
    // STAT CARDS
    // =====================================================================

    private void hitungStatCard() {
        try {
            String query = "SELECT " +
                    "dbo.f_TotalTransaksi() AS Total, " +
                    "dbo.f_TotalLunas() AS Lunas, " +
                    "dbo.f_TotalBelumLunas() AS BelumLunas";

            try (java.sql.Statement st = db.getConnection().createStatement();
                 ResultSet rs = st.executeQuery(query)) {
                if (rs.next()) {
                    lblTotalTransaksi.setText(String.valueOf(rs.getInt("Total")));
                    lblLunas.setText(String.valueOf(rs.getInt("Lunas")));
                    lblBelumLunas.setText(String.valueOf(rs.getInt("BelumLunas")));
                }
            }
        } catch (SQLException e) {
            tampilAlert(Alert.AlertType.ERROR, "Gagal menghitung statistik", e.getMessage());
        }
    }

    // =====================================================================
    // PAGINATION
    // =====================================================================

    private void applyPagination() {
        if (filteredData == null) return;

        List<PenjualanModel> allItems = new ArrayList<>(filteredData);
        totalItems = allItems.size();

        totalPages = (int) Math.ceil((double) totalItems / ITEMS_PER_PAGE);
        if (totalPages == 0) totalPages = 1;

        if (currentPage >= totalPages) currentPage = totalPages - 1;
        if (currentPage < 0) currentPage = 0;

        int fromIndex = currentPage * ITEMS_PER_PAGE;
        int toIndex = Math.min(fromIndex + ITEMS_PER_PAGE, totalItems);

        currentPageData.clear();
        if (fromIndex < totalItems) {
            currentPageData.addAll(allItems.subList(fromIndex, toIndex));
        }

        tablePembelian.setItems(currentPageData);

        int startItem = totalItems > 0 ? fromIndex + 1 : 0;
        int endItem = Math.min(toIndex, totalItems);
        lblInfoData.setText("Menampilkan " + startItem + "-" + endItem + " dari " + totalItems + " data");
        lblPageInfo.setText("Hal " + (currentPage + 1) + " / " + totalPages);

        btnPrev.setDisable(currentPage == 0);
        btnNext.setDisable(currentPage >= totalPages - 1);
    }

    @FXML
    void handlePrevPage() {
        if (currentPage > 0) {
            currentPage--;
            applyPagination();
        }
    }

    @FXML
    void handleNextPage() {
        if (currentPage < totalPages - 1) {
            currentPage++;
            applyPagination();
        }
    }

    // =====================================================================
    // HELPER
    // =====================================================================

    private String formatRupiah(double nominal) {
        NumberFormat formatRp = NumberFormat.getNumberInstance(new Locale("in", "ID"));
        return "Rp " + formatRp.format(nominal);
    }

    private void tampilAlert(Alert.AlertType tipe, String judul, String pesan) {
        Alert a = new Alert(tipe);
        a.setTitle(judul);
        a.setHeaderText(null);
        a.setContentText(pesan);
        a.showAndWait();
    }

    // =====================================================================
    // MODEL — Penjualan
    // =====================================================================

    public static class PenjualanModel {
        private final StringProperty idPenjualan;
        private final StringProperty karyawan;
        private final StringProperty tanggal;
        private final DoubleProperty totalHarga;
        private final DoubleProperty uangBayar;
        private final DoubleProperty kembalian;
        private final StringProperty metode;
        private final StringProperty status;

        public PenjualanModel(String idPenjualan, String karyawan, String tanggal,
                              double totalHarga, double uangBayar, double kembalian,
                              String metode, String status) {
            this.idPenjualan = new SimpleStringProperty(idPenjualan);
            this.karyawan    = new SimpleStringProperty(karyawan);
            this.tanggal     = new SimpleStringProperty(tanggal);
            this.totalHarga  = new SimpleDoubleProperty(totalHarga);
            this.uangBayar   = new SimpleDoubleProperty(uangBayar);
            this.kembalian   = new SimpleDoubleProperty(kembalian);
            this.metode      = new SimpleStringProperty(metode);
            this.status      = new SimpleStringProperty(status);
        }

        public String getIdPenjualan() { return idPenjualan.get(); }
        public StringProperty idPenjualanProperty() { return idPenjualan; }

        public String getKaryawan() { return karyawan.get(); }
        public StringProperty karyawanProperty() { return karyawan; }

        public String getTanggal() { return tanggal.get(); }
        public StringProperty tanggalProperty() { return tanggal; }

        public double getTotalHarga() { return totalHarga.get(); }
        public DoubleProperty totalHargaProperty() { return totalHarga; }

        public double getUangBayar() { return uangBayar.get(); }
        public DoubleProperty uangBayarProperty() { return uangBayar; }

        public double getKembalian() { return kembalian.get(); }
        public DoubleProperty kembalianProperty() { return kembalian; }

        public String getMetode() { return metode.get(); }
        public StringProperty metodeProperty() { return metode; }

        public String getStatus() { return status.get(); }
        public StringProperty statusProperty() { return status; }
    }

    // =====================================================================
    // MODEL — Detail Penjualan
    // =====================================================================

    public static class DetailPenjualanModel {
        private final StringProperty namaProduk;
        private final IntegerProperty jumlah;
        private final DoubleProperty hargaSatuan;

        public DetailPenjualanModel(String namaProduk, int jumlah, double hargaSatuan) {
            this.namaProduk = new SimpleStringProperty(namaProduk);
            this.jumlah = new SimpleIntegerProperty(jumlah);
            this.hargaSatuan = new SimpleDoubleProperty(hargaSatuan);
        }

        public String getNamaProduk() { return namaProduk.get(); }
        public StringProperty namaProdukProperty() { return namaProduk; }

        public int getJumlah() { return jumlah.get(); }
        public IntegerProperty jumlahProperty() { return jumlah; }

        public double getHargaSatuan() { return hargaSatuan.get(); }
        public DoubleProperty hargaSatuanProperty() { return hargaSatuan; }
    }
}