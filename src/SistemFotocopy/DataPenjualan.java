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
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.NumberFormat;
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
    // KOLOM AKSI — TOMBOL DETAIL
    // =====================================================================

    private void setupKolomAksi() {
        colAksi.setCellFactory(col -> new TableCell<>() {
            private final Button btnDetail = new Button("Detail");
            {
                btnDetail.getStyleClass().add("btn-detail-aksi");
                btnDetail.setStyle("-fx-background-color: #3B82F6; -fx-text-fill: white; -fx-font-weight: bold; -fx-cursor: hand;");
                btnDetail.setOnAction(e -> {
                    PenjualanModel penjualan = getTableView().getItems().get(getIndex());
                    bukaJendelaDetailPenjualan(penjualan);
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
    // JENDELA DETAIL PENJUALAN
    // =====================================================================

    private void bukaJendelaDetailPenjualan(PenjualanModel penjualan) {
        if (penjualan == null) return;

        Label lblJudul = new Label("Detail Penjualan");
        lblJudul.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #1E293B;");

        Label lblSubJudul = new Label("Detail Nota: " + penjualan.getIdPenjualan() + " (" + penjualan.getKaryawan() + ")");
        lblSubJudul.setStyle("-fx-font-size: 13px; -fx-text-fill: #475569;");

        VBox headerBox = new VBox(5, lblJudul, lblSubJudul);
        headerBox.setPadding(new Insets(0, 0, 12, 0));

        TableView<DetailPenjualanModel> tableDetail = new TableView<>();
        tableDetail.setStyle("-fx-background-color: #FFFFFF; -fx-border-color: #E2E8F0; -fx-border-width: 1px;");
        tableDetail.setPlaceholder(new Label("Tidak ada detail penjualan"));

        // Kolom Nama Produk
        TableColumn<DetailPenjualanModel, String> colNamaProduk = new TableColumn<>("Nama Produk");
        colNamaProduk.setCellValueFactory(d -> d.getValue().namaProdukProperty());
        colNamaProduk.setPrefWidth(200);
        colNamaProduk.setStyle("-fx-alignment: CENTER-LEFT; -fx-font-size: 13px;");

        // Kolom Jumlah (ganti dari Qty)
        TableColumn<DetailPenjualanModel, Number> colJumlah = new TableColumn<>("Jumlah");
        colJumlah.setCellValueFactory(d -> d.getValue().jumlahProperty());
        colJumlah.setPrefWidth(80);
        colJumlah.setStyle("-fx-alignment: CENTER; -fx-font-size: 13px;");

        // Kolom Harga Satuan
        TableColumn<DetailPenjualanModel, Number> colHargaSatuan = new TableColumn<>("Harga Satuan");
        colHargaSatuan.setCellValueFactory(d -> d.getValue().hargaSatuanProperty());
        colHargaSatuan.setPrefWidth(150);
        colHargaSatuan.setStyle("-fx-alignment: CENTER-RIGHT; -fx-font-size: 13px;");
        colHargaSatuan.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(Number item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : formatRupiah(item.doubleValue()));
                setStyle("-fx-alignment: CENTER-RIGHT;");
            }
        });

        tableDetail.getColumns().addAll(colNamaProduk, colJumlah, colHargaSatuan);
        tableDetail.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        tableDetail.setPrefHeight(200);
        tableDetail.setMaxHeight(250);

        // Load data detail
        ObservableList<DetailPenjualanModel> detailData = loadDetailPenjualan(penjualan.getIdPenjualan());
        tableDetail.setItems(detailData);

        GridPane footerGrid = new GridPane();
        footerGrid.setHgap(30);
        footerGrid.setPadding(new Insets(12, 15, 10, 15));
        footerGrid.setStyle("-fx-background-color: #F8FAFC; -fx-border-color: #E2E8F0; -fx-border-width: 1px 0 0 0;");

        Label lblTotalLabel = new Label("Total:");
        lblTotalLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #475569;");
        Label lblTotalValue = new Label(formatRupiah(penjualan.getTotalHarga()));
        lblTotalValue.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #1E293B;");

        Label lblBayarLabel = new Label("Bayar:");
        lblBayarLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #475569;");
        Label lblBayarValue = new Label(formatRupiah(penjualan.getUangBayar()));
        lblBayarValue.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #16A34A;");

        Label lblKembalianLabel = new Label("Kembalian:");
        lblKembalianLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #475569;");
        Label lblKembalianValue = new Label(formatRupiah(penjualan.getKembalian()));
        lblKembalianValue.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #3B82F6;");

        footerGrid.add(lblTotalLabel, 0, 0);
        footerGrid.add(lblBayarLabel, 1, 0);
        footerGrid.add(lblKembalianLabel, 2, 0);
        footerGrid.add(lblTotalValue, 0, 1);
        footerGrid.add(lblBayarValue, 1, 1);
        footerGrid.add(lblKembalianValue, 2, 1);

        GridPane.setHalignment(lblTotalLabel, Pos.CENTER_LEFT.getHpos());
        GridPane.setHalignment(lblBayarLabel, Pos.CENTER_LEFT.getHpos());
        GridPane.setHalignment(lblKembalianLabel, Pos.CENTER_LEFT.getHpos());
        GridPane.setHalignment(lblTotalValue, Pos.CENTER_LEFT.getHpos());
        GridPane.setHalignment(lblBayarValue, Pos.CENTER_LEFT.getHpos());
        GridPane.setHalignment(lblKembalianValue, Pos.CENTER_LEFT.getHpos());

        Button btnTutup = new Button("Tutup");
        btnTutup.setStyle(
                "-fx-background-color: #475569; " +
                        "-fx-text-fill: white; " +
                        "-fx-font-size: 13px; " +
                        "-fx-font-weight: bold; " +
                        "-fx-padding: 8 25 8 25; " +
                        "-fx-cursor: hand; " +
                        "-fx-background-radius: 5;"
        );
        btnTutup.setOnAction(e -> ((Stage) btnTutup.getScene().getWindow()).close());

        HBox buttonBox = new HBox(btnTutup);
        buttonBox.setAlignment(Pos.CENTER_RIGHT);
        buttonBox.setPadding(new Insets(10, 0, 0, 0));

        VBox root = new VBox(12);
        root.setPadding(new Insets(20, 25, 20, 25));
        root.setStyle("-fx-background-color: #FFFFFF; -fx-border-color: #CBD5E1; -fx-border-width: 1px; -fx-border-radius: 8;");
        root.getChildren().addAll(headerBox, tableDetail, footerGrid, buttonBox);

        Scene scene = new Scene(root);

        Stage stage = new Stage();
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.setTitle("Detail Penjualan - " + penjualan.getIdPenjualan());
        stage.setScene(scene);
        stage.setWidth(600);
        stage.setHeight(420);
        stage.setMinWidth(550);
        stage.setMinHeight(380);
        stage.setMaxWidth(700);
        stage.setMaxHeight(500);
        stage.setResizable(false);
        stage.centerOnScreen();
        stage.show();
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
                            rs.getInt("Jumlah"),  // Tetap pakai Qty, bukan Jumlah
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
        return "Rp. " + formatRp.format(nominal);
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
        private final IntegerProperty jumlah;  // Ganti dari qty ke jumlah
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