package SistemFotocopy;

import Database.DBConnection;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
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
import java.util.Comparator;
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
    private SortedList<PenjualanModel>   sortedData;

    private static final int ITEMS_PER_PAGE = 5;
    private int currentPage = 1;
    private List<PenjualanModel> halamanSaatIni = new ArrayList<>();

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
    // JENDELA DETAIL PENJUALAN — UKURAN SEDANG (TIDAK FULLSCREEN)
    // =====================================================================

    private void bukaJendelaDetailPenjualan(PenjualanModel penjualan) {
        // =============================================================
        // HEADER JUDUL
        // =============================================================
        Label lblJudul = new Label("Detail Penjualan");
        lblJudul.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #1E293B;");

        Label lblSubJudul = new Label("Detail Nota: " + penjualan.getIdPenjualan() + " (" + penjualan.getKaryawan() + ")");
        lblSubJudul.setStyle("-fx-font-size: 13px; -fx-text-fill: #475569;");

        VBox headerBox = new VBox(5, lblJudul, lblSubJudul);
        headerBox.setPadding(new Insets(0, 0, 12, 0));

        // =============================================================
        // TABEL DETAIL
        // =============================================================
        TableView<DetailPenjualanModel> tableDetail = new TableView<>();
        tableDetail.setStyle("-fx-background-color: #FFFFFF; -fx-border-color: #E2E8F0; -fx-border-width: 1px;");

        TableColumn<DetailPenjualanModel, String> colNamaProduk = new TableColumn<>("Nama Produk");
        colNamaProduk.setCellValueFactory(d -> d.getValue().namaProdukProperty());
        colNamaProduk.setPrefWidth(180);
        colNamaProduk.setStyle("-fx-alignment: CENTER-LEFT; -fx-font-size: 13px;");

        TableColumn<DetailPenjualanModel, Number> colQty = new TableColumn<>("Qty");
        colQty.setCellValueFactory(d -> d.getValue().qtyProperty());
        colQty.setPrefWidth(70);
        colQty.setStyle("-fx-alignment: CENTER; -fx-font-size: 13px;");

        TableColumn<DetailPenjualanModel, Number> colHargaSatuan = new TableColumn<>("Harga Satuan");
        colHargaSatuan.setCellValueFactory(d -> d.getValue().hargaSatuanProperty());
        colHargaSatuan.setPrefWidth(140);
        colHargaSatuan.setStyle("-fx-alignment: CENTER-RIGHT; -fx-font-size: 13px;");
        colHargaSatuan.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(Number item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : formatRupiah(item.doubleValue()));
                setStyle("-fx-alignment: CENTER-RIGHT;");
            }
        });

        TableColumn<DetailPenjualanModel, Number> colSubtotal = new TableColumn<>("Subtotal");
        colSubtotal.setCellValueFactory(d -> d.getValue().subtotalProperty());
        colSubtotal.setPrefWidth(140);
        colSubtotal.setStyle("-fx-alignment: CENTER-RIGHT; -fx-font-size: 13px; -fx-font-weight: bold;");
        colSubtotal.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(Number item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : formatRupiah(item.doubleValue()));
                setStyle("-fx-alignment: CENTER-RIGHT; -fx-font-weight: bold;");
            }
        });

        tableDetail.getColumns().addAll(colNamaProduk, colQty, colHargaSatuan, colSubtotal);
        tableDetail.setItems(loadDetailPenjualan(penjualan.getIdPenjualan()));
        tableDetail.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        tableDetail.setPrefHeight(200);
        tableDetail.setMaxHeight(250);

        // =============================================================
        // FOOTER — TOTAL, BAYAR, KEMBALIAN (3 kolom terpisah)
        // =============================================================
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

        // Baris 1: Label
        footerGrid.add(lblTotalLabel, 0, 0);
        footerGrid.add(lblBayarLabel, 1, 0);
        footerGrid.add(lblKembalianLabel, 2, 0);

        // Baris 2: Value
        footerGrid.add(lblTotalValue, 0, 1);
        footerGrid.add(lblBayarValue, 1, 1);
        footerGrid.add(lblKembalianValue, 2, 1);

        // Set alignment
        GridPane.setHalignment(lblTotalLabel, Pos.CENTER_LEFT.getHpos());
        GridPane.setHalignment(lblBayarLabel, Pos.CENTER_LEFT.getHpos());
        GridPane.setHalignment(lblKembalianLabel, Pos.CENTER_LEFT.getHpos());
        GridPane.setHalignment(lblTotalValue, Pos.CENTER_LEFT.getHpos());
        GridPane.setHalignment(lblBayarValue, Pos.CENTER_LEFT.getHpos());
        GridPane.setHalignment(lblKembalianValue, Pos.CENTER_LEFT.getHpos());

        // =============================================================
        // TOMBOL TUTUP
        // =============================================================
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

        // =============================================================
        // MAIN LAYOUT
        // =============================================================
        VBox root = new VBox(12);
        root.setPadding(new Insets(20, 25, 20, 25));
        root.setStyle("-fx-background-color: #FFFFFF; -fx-border-color: #CBD5E1; -fx-border-width: 1px; -fx-border-radius: 8;");
        root.getChildren().addAll(headerBox, tableDetail, footerGrid, buttonBox);

        // =============================================================
        // SCENE DAN STAGE — UKURAN SEDANG
        // =============================================================
        Scene scene = new Scene(root);

        Stage stage = new Stage();
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.setTitle("Detail Penjualan - " + penjualan.getIdPenjualan());
        stage.setScene(scene);

        // =============================================================
        // ATUR UKURAN WINDOW — TIDAK FULLSCREEN
        // =============================================================
        stage.setWidth(600);
        stage.setHeight(420);
        stage.setMinWidth(550);
        stage.setMinHeight(380);
        stage.setMaxWidth(700);
        stage.setMaxHeight(500);
        stage.setResizable(false);

        // Center di tengah layar
        stage.centerOnScreen();

        stage.show();

        // Styling header tabel
        tableDetail.setRowFactory(tv -> new TableRow<DetailPenjualanModel>() {
            @Override
            protected void updateItem(DetailPenjualanModel item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setStyle("");
                } else {
                    setStyle("-fx-background-color: #FFFFFF; -fx-border-color: #F1F5F9; -fx-border-width: 0 0 1px 0;");
                }
            }
        });
    }

    // =====================================================================
    // LOAD DETAIL PENJUALAN
    // =====================================================================

    private ObservableList<DetailPenjualanModel> loadDetailPenjualan(String idPenjualan) {
        ObservableList<DetailPenjualanModel> list = FXCollections.observableArrayList();
        String sql = "SELECT * FROM fn_DetailPenjualanNota(?)";

        try (PreparedStatement ps = db.getConnection().prepareStatement(sql)) {
            ps.setString(1, idPenjualan);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(new DetailPenjualanModel(
                            rs.getString("Nama Produk"),
                            rs.getInt("Qty"),
                            rs.getDouble("Harga Satuan"),
                            rs.getDouble("Subtotal")
                    ));
                }
            }
        } catch (SQLException e) {
            tampilAlert(Alert.AlertType.ERROR, "Gagal memuat detail penjualan", e.getMessage());
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
            );
            currentPage = 1;
            refreshPagination();
        });
    }

    // =====================================================================
    // LOAD DATA PENJUALAN — pakai v_TampilPenjualan
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

            sortedData = new SortedList<>(filteredData, new Comparator<PenjualanModel>() {
                @Override
                public int compare(PenjualanModel o1, PenjualanModel o2) {
                    return o2.getIdPenjualan().compareTo(o1.getIdPenjualan());
                }
            });

            currentPage = 1;
            refreshPagination();

        } catch (SQLException e) {
            tampilAlert(Alert.AlertType.ERROR, "Gagal memuat data penjualan", e.getMessage());
        }
    }

    // =====================================================================
    // STAT CARDS
    // =====================================================================

    private void hitungStatCard() {
        String sql = "SELECT " +
                "  COUNT(*) AS Total, " +
                "  SUM(CASE WHEN Status_Penjualan = 'Lunas' THEN 1 ELSE 0 END) AS Lunas, " +
                "  SUM(CASE WHEN Status_Penjualan = 'Batal Pembayaran' THEN 1 ELSE 0 END) AS BelumLunas " +
                "FROM Penjualan";

        try (java.sql.Statement st = db.getConnection().createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            if (rs.next()) {
                lblTotalTransaksi.setText(String.valueOf(rs.getInt("Total")));
                lblLunas.setText(String.valueOf(rs.getInt("Lunas")));
                lblBelumLunas.setText(String.valueOf(rs.getInt("BelumLunas")));
            }
        } catch (SQLException e) {
            tampilAlert(Alert.AlertType.ERROR, "Gagal menghitung statistik", e.getMessage());
        }
    }

    // =====================================================================
    // PAGINATION
    // =====================================================================

    private void refreshPagination() {
        if (sortedData == null) return;

        List<PenjualanModel> semuaData = new ArrayList<>(sortedData);
        int total      = semuaData.size();
        int totalPages = Math.max(1, (int) Math.ceil((double) total / ITEMS_PER_PAGE));

        if (currentPage > totalPages) currentPage = totalPages;
        if (currentPage < 1)          currentPage = 1;

        int from = (currentPage - 1) * ITEMS_PER_PAGE;
        int to   = Math.min(from + ITEMS_PER_PAGE, total);

        halamanSaatIni = (from < total) ? semuaData.subList(from, to) : new ArrayList<>();

        tablePembelian.setItems(FXCollections.observableArrayList(halamanSaatIni));

        lblInfoData.setText("Menampilkan " + halamanSaatIni.size() + " dari " + total + " data");
        lblPageInfo.setText("Hal " + currentPage + " / " + totalPages);

        btnPrev.setDisable(currentPage <= 1);
        btnNext.setDisable(currentPage >= totalPages);
    }

    @FXML
    void handlePrevPage() {
        if (currentPage > 1) {
            currentPage--;
            refreshPagination();
        }
    }

    @FXML
    void handleNextPage() {
        if (sortedData == null) return;
        int totalPages = Math.max(1, (int) Math.ceil((double) sortedData.size() / ITEMS_PER_PAGE));
        if (currentPage < totalPages) {
            currentPage++;
            refreshPagination();
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

        public StringProperty tanggalProperty() { return tanggal; }

        public double getTotalHarga() { return totalHarga.get(); }
        public DoubleProperty totalHargaProperty() { return totalHarga; }

        public double getUangBayar() { return uangBayar.get(); }
        public DoubleProperty uangBayarProperty() { return uangBayar; }

        public double getKembalian() { return kembalian.get(); }
        public DoubleProperty kembalianProperty() { return kembalian; }

        public StringProperty metodeProperty() { return metode; }
        public StringProperty statusProperty() { return status; }
    }

    // =====================================================================
    // MODEL — Detail Penjualan (untuk jendela Detail)
    // =====================================================================

    public static class DetailPenjualanModel {
        private final StringProperty namaProduk;
        private final javafx.beans.property.IntegerProperty qty;
        private final DoubleProperty hargaSatuan;
        private final DoubleProperty subtotal;

        public DetailPenjualanModel(String namaProduk, int qty, double hargaSatuan, double subtotal) {
            this.namaProduk  = new SimpleStringProperty(namaProduk);
            this.qty         = new javafx.beans.property.SimpleIntegerProperty(qty);
            this.hargaSatuan = new SimpleDoubleProperty(hargaSatuan);
            this.subtotal    = new SimpleDoubleProperty(subtotal);
        }

        public StringProperty namaProdukProperty() { return namaProduk; }
        public javafx.beans.property.IntegerProperty qtyProperty() { return qty; }
        public DoubleProperty hargaSatuanProperty() { return hargaSatuan; }
        public DoubleProperty subtotalProperty() { return subtotal; }
    }
}