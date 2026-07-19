package SistemFotocopy;

import Database.DBConnection;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.net.URL;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.NumberFormat;
import java.util.Locale;
import java.util.ResourceBundle;

public class DataPembelianStock implements Initializable {

    @FXML private Button btnNext;
    @FXML private Button btnPrev;

    @FXML private TableColumn<PembelianStokModel, Void> colAksi;
    @FXML private TableColumn<PembelianStokModel, String> colIdPembelian;
    @FXML private TableColumn<PembelianStokModel, String> colMetode;
    @FXML private TableColumn<PembelianStokModel, String> colPegawai;
    @FXML private TableColumn<PembelianStokModel, String> colStatus;
    @FXML private TableColumn<PembelianStokModel, String> colSupplier;
    @FXML private TableColumn<PembelianStokModel, String> colTanggal;
    @FXML private TableColumn<PembelianStokModel, String> colTotal;

    @FXML private Label lblBelumLunas;
    @FXML private Label lblInfoData;
    @FXML private Label lblLunas;
    @FXML private Label lblPageInfo;
    @FXML private Label lblTotalTransaksi;

    @FXML private TableView<PembelianStokModel> tablePembelian;
    @FXML private TextField txtCari;

    private ObservableList<PembelianStokModel> masterData = FXCollections.observableArrayList();
    private FilteredList<PembelianStokModel> filteredData;
    private ObservableList<PembelianStokModel> pageData = FXCollections.observableArrayList();

    private Connection connection;

    private int currentPage = 0;
    private final int itemsPerPage = 10;
    private int totalItems = 0;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setupDatabaseConnection();
        setupTableColumns();

        filteredData = new FilteredList<>(masterData, p -> true);

        setupSearchListener();
        setupPaginationButtons();

        loadData();
        hitungStatCard();
    }

    private void setupDatabaseConnection() {
        try {
            connection = DBConnection.getConnection();
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Error", "Gagal koneksi ke database: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    private void setupTableColumns() {
        colIdPembelian.setCellValueFactory(new PropertyValueFactory<>("idPembelian"));
        colPegawai.setCellValueFactory(new PropertyValueFactory<>("pegawai"));
        colSupplier.setCellValueFactory(new PropertyValueFactory<>("supplier"));
        colTanggal.setCellValueFactory(new PropertyValueFactory<>("tanggal"));
        colMetode.setCellValueFactory(new PropertyValueFactory<>("metodePembayaran"));
        colTotal.setCellValueFactory(new PropertyValueFactory<>("totalHargaFormatted"));
        colStatus.setCellValueFactory(new PropertyValueFactory<>("statusPembayaran"));

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

        // Kolom Aksi dengan tombol Detail
        colAksi.setCellFactory(param -> new TableCell<>() {
            private final Button btnDetail = new Button("Detail");

            {
                btnDetail.getStyleClass().add("btn-detail");
                btnDetail.setStyle("-fx-background-color: #3B82F6; -fx-text-fill: white; -fx-font-weight: bold; -fx-cursor: hand;");
                btnDetail.setOnAction(event -> {
                    PembelianStokModel model = getTableView().getItems().get(getIndex());
                    showDetailPembelian(model);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    HBox actionBox = new HBox(btnDetail);
                    actionBox.setAlignment(Pos.CENTER);
                    setGraphic(actionBox);
                }
            }
        });

        tablePembelian.setItems(pageData);
    }

    // =============================================================
    // LOAD DATA - PAKAI VIEW ✅
    // =============================================================
    private void loadData() {
        if (connection == null) return;

        masterData.clear();

        String query = "SELECT ID_Pembelian_Stok, Pegawai, Supplier, Tanggal_Pembelian, Status_Pembayaran, Total_Harga " +
                "FROM v_TampilPembelianStok ORDER BY Tanggal_Pembelian DESC, ID_Pembelian_Stok DESC";

        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {

            while (rs.next()) {
                String id = rs.getString("ID_Pembelian_Stok");
                String pegawai = rs.getString("Pegawai");
                String supplier = rs.getString("Supplier");
                String tanggal = rs.getString("Tanggal_Pembelian");
                String status = rs.getString("Status_Pembayaran");
                double total = rs.getDouble("Total_Harga");

                masterData.add(new PembelianStokModel(id, pegawai, supplier, tanggal, status, total));
            }

            filteredData = new FilteredList<>(masterData, p -> true);
            totalItems = filteredData.size();
            currentPage = 0;
            applyPagination();

        } catch (SQLException e) {
            e.printStackTrace();
            showAlert("Error", "Gagal memuat data: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    // =============================================================
    // STAT CARDS - PAKAI UDF ✅
    // =============================================================
    private void hitungStatCard() {
        if (connection == null) return;

        try {
            String query = "SELECT " +
                    "dbo.f_TotalTransaksiPembelian() AS Total, " +
                    "dbo.f_TotalLunasPembelian() AS Lunas, " +
                    "dbo.f_TotalBelumLunasPembelian() AS BelumLunas";

            try (Statement stmt = connection.createStatement();
                 ResultSet rs = stmt.executeQuery(query)) {
                if (rs.next()) {
                    lblTotalTransaksi.setText(String.valueOf(rs.getInt("Total")));
                    lblLunas.setText(String.valueOf(rs.getInt("Lunas")));
                    lblBelumLunas.setText(String.valueOf(rs.getInt("BelumLunas")));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            showAlert("Error", "Gagal menghitung statistik: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    // =============================================================
    // SEARCH LISTENER
    // =============================================================
    private void setupSearchListener() {
        txtCari.textProperty().addListener((observable, oldValue, newValue) -> {
            filteredData.setPredicate(model -> {
                if (newValue == null || newValue.isEmpty()) {
                    return true;
                }
                String lowerCaseFilter = newValue.toLowerCase();

                return model.getIdPembelian().toLowerCase().contains(lowerCaseFilter)
                        || model.getSupplier().toLowerCase().contains(lowerCaseFilter)
                        || model.getPegawai().toLowerCase().contains(lowerCaseFilter)
                        || model.getStatusPembayaran().toLowerCase().contains(lowerCaseFilter);
            });

            totalItems = filteredData.size();
            currentPage = 0;
            applyPagination();
        });
    }

    private void setupPaginationButtons() {
        btnPrev.setOnAction(e -> {
            if (currentPage > 0) {
                currentPage--;
                applyPagination();
            }
        });

        btnNext.setOnAction(e -> {
            if ((currentPage + 1) * itemsPerPage < totalItems) {
                currentPage++;
                applyPagination();
            }
        });
    }

    private void applyPagination() {
        int fromIndex = currentPage * itemsPerPage;
        int toIndex = Math.min(fromIndex + itemsPerPage, totalItems);

        pageData.clear();

        if (fromIndex < toIndex) {
            pageData.addAll(filteredData.subList(fromIndex, toIndex));
        }

        updatePaginationInfo();
    }

    private void updatePaginationInfo() {
        int totalPages = (int) Math.ceil((double) totalItems / itemsPerPage);
        if (totalPages == 0) totalPages = 1;

        lblPageInfo.setText("Hal " + (currentPage + 1) + " / " + totalPages);

        int startItem = totalItems == 0 ? 0 : (currentPage * itemsPerPage) + 1;
        int endItem = Math.min((currentPage + 1) * itemsPerPage, totalItems);

        lblInfoData.setText("Menampilkan " + startItem + "-" + endItem + " dari " + totalItems + " data");

        btnPrev.setDisable(currentPage == 0);
        btnNext.setDisable((currentPage + 1) >= totalPages);
    }

    // =============================================================
    // DETAIL PEMBELIAN - PAKAI QUERY LANGSUNG (TANPA SUBTOTAL)
    // =============================================================
    private void showDetailPembelian(PembelianStokModel model) {
        ObservableList<DetailPembelianItem> items = FXCollections.observableArrayList();

        if (connection != null) {
            // =============================================================
            // QUERY LANGSUNG TANPA SUBTOTAL
            // =============================================================
            String query = "SELECT p.Nama_Barang AS Produk, dp.Jumlah, dp.Harga AS [Harga Satuan] " +
                    "FROM Detail_Pembelian_Stok dp " +
                    "JOIN Produk p ON dp.ID_Produk = p.ID_Produk " +
                    "WHERE dp.ID_Pembelian_Stok = ?";

            try (PreparedStatement ps = connection.prepareStatement(query)) {
                ps.setString(1, model.getIdPembelian());
                ResultSet rs = ps.executeQuery();
                while (rs.next()) {
                    String produk = rs.getString("Produk");
                    int jumlah = rs.getInt("Jumlah");
                    double hargaSatuan = rs.getDouble("Harga Satuan");
                    items.add(new DetailPembelianItem(produk, jumlah, hargaSatuan));
                }
                rs.close();
            } catch (SQLException e) {
                e.printStackTrace();
                showAlert("Error", "Gagal memuat detail: " + e.getMessage(), Alert.AlertType.ERROR);
                return;
            }
        }

        // =============================================================
        // HEADER
        // =============================================================
        Label lblJudul = new Label("Detail Pembelian Stock");
        lblJudul.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #1E293B;");

        Label lblSubJudul = new Label("ID Pembelian: " + model.getIdPembelian() + " | Supplier: " + model.getSupplier());
        lblSubJudul.setStyle("-fx-font-size: 13px; -fx-text-fill: #475569;");

        VBox headerBox = new VBox(5, lblJudul, lblSubJudul);
        headerBox.setPadding(new Insets(0, 0, 12, 0));

        // =============================================================
        // INFO
        // =============================================================
        GridPane infoGrid = new GridPane();
        infoGrid.setHgap(15);
        infoGrid.setVgap(5);
        infoGrid.setPadding(new Insets(0, 0, 10, 0));

        Label lblPegawai = new Label("Pegawai:");
        lblPegawai.setStyle("-fx-font-weight: bold; -fx-text-fill: #475569;");
        Label lblPegawaiValue = new Label(model.getPegawai());
        lblPegawaiValue.setStyle("-fx-text-fill: #1E293B;");

        Label lblTanggal = new Label("Tanggal:");
        lblTanggal.setStyle("-fx-font-weight: bold; -fx-text-fill: #475569;");
        Label lblTanggalValue = new Label(model.getTanggal());
        lblTanggalValue.setStyle("-fx-text-fill: #1E293B;");

        Label lblMetode = new Label("Metode:");
        lblMetode.setStyle("-fx-font-weight: bold; -fx-text-fill: #475569;");
        Label lblMetodeValue = new Label(model.getMetodePembayaran());
        lblMetodeValue.setStyle("-fx-text-fill: #1E293B;");

        infoGrid.add(lblPegawai, 0, 0);
        infoGrid.add(lblPegawaiValue, 1, 0);
        infoGrid.add(lblTanggal, 2, 0);
        infoGrid.add(lblTanggalValue, 3, 0);
        infoGrid.add(lblMetode, 4, 0);
        infoGrid.add(lblMetodeValue, 5, 0);

        // =============================================================
        // TABEL DETAIL - TANPA KOLOM SUBTOTAL
        // =============================================================
        TableView<DetailPembelianItem> detailTable = new TableView<>();
        detailTable.setStyle("-fx-background-color: #FFFFFF; -fx-border-color: #E2E8F0; -fx-border-width: 1px;");

        TableColumn<DetailPembelianItem, String> colNama = new TableColumn<>("Nama Barang");
        colNama.setCellValueFactory(new PropertyValueFactory<>("namaBarang"));
        colNama.setPrefWidth(250);
        colNama.setStyle("-fx-alignment: CENTER-LEFT; -fx-font-size: 13px;");

        TableColumn<DetailPembelianItem, Integer> colJumlah = new TableColumn<>("Jumlah");
        colJumlah.setCellValueFactory(new PropertyValueFactory<>("jumlah"));
        colJumlah.setPrefWidth(100);
        colJumlah.setStyle("-fx-alignment: CENTER; -fx-font-size: 13px;");

        TableColumn<DetailPembelianItem, String> colHargaItem = new TableColumn<>("Harga Satuan");
        colHargaItem.setCellValueFactory(new PropertyValueFactory<>("hargaFormatted"));
        colHargaItem.setPrefWidth(180);
        colHargaItem.setStyle("-fx-alignment: CENTER-RIGHT; -fx-font-size: 13px;");

        // HAPUS KOLOM SUBTOTAL
        detailTable.getColumns().addAll(colNama, colJumlah, colHargaItem);
        detailTable.setItems(items);
        detailTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        detailTable.setPrefHeight(200);
        detailTable.setMaxHeight(250);

        // =============================================================
        // FOOTER
        // =============================================================
        GridPane footerGrid = new GridPane();
        footerGrid.setHgap(30);
        footerGrid.setPadding(new Insets(12, 15, 10, 15));
        footerGrid.setStyle("-fx-background-color: #F8FAFC; -fx-border-color: #E2E8F0; -fx-border-width: 1px 0 0 0;");

        Label lblTotalLabel = new Label("Total Harga:");
        lblTotalLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #475569;");
        Label lblTotalValue = new Label(model.getTotalHargaFormatted());
        lblTotalValue.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #1E293B;");

        Label lblStatusLabel = new Label("Status:");
        lblStatusLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #475569;");
        Label lblStatusValue = new Label(model.getStatusPembayaran().toUpperCase());
        lblStatusValue.setStyle("-fx-font-size: 14px; -fx-font-weight: bold;");

        if (model.getStatusPembayaran().equalsIgnoreCase("lunas")) {
            lblStatusValue.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #16A34A;");
        } else {
            lblStatusValue.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #BA1A1A;");
        }

        footerGrid.add(lblTotalLabel, 0, 0);
        footerGrid.add(lblTotalValue, 0, 1);
        footerGrid.add(lblStatusLabel, 1, 0);
        footerGrid.add(lblStatusValue, 1, 1);

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
        // MAIN
        // =============================================================
        VBox root = new VBox(12);
        root.setPadding(new Insets(20, 25, 20, 25));
        root.setStyle("-fx-background-color: #FFFFFF; -fx-border-color: #CBD5E1; -fx-border-width: 1px; -fx-border-radius: 8;");
        root.getChildren().addAll(headerBox, infoGrid, detailTable, footerGrid, buttonBox);

        Scene scene = new Scene(root);

        Stage stage = new Stage();
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.setTitle("Detail Pembelian - " + model.getIdPembelian());
        stage.setScene(scene);
        stage.setWidth(650);
        stage.setHeight(480);
        stage.setMinWidth(600);
        stage.setMinHeight(420);
        stage.setMaxWidth(750);
        stage.setMaxHeight(550);
        stage.setResizable(false);
        stage.centerOnScreen();
        stage.show();
    }

    // =============================================================
    // HELPER
    // =============================================================
    private void showAlert(String title, String message, Alert.AlertType type) {
        Platform.runLater(() -> {
            Alert alert = new Alert(type);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.showAndWait();
        });
    }

    private static String formatRupiah(double amount) {
        NumberFormat formatter = NumberFormat.getCurrencyInstance(new Locale("id", "ID"));
        return formatter.format(amount).replace("Rp", "Rp.");
    }

    // =============================================================
    // MODEL - Pembelian Stok
    // =============================================================
    public static class PembelianStokModel {
        private final String idPembelian;
        private final String pegawai;
        private final String supplier;
        private final String tanggal;
        private final String statusPembayaran;
        private final double totalHarga;

        public PembelianStokModel(String idPembelian, String pegawai, String supplier,
                                  String tanggal, String statusPembayaran, double totalHarga) {
            this.idPembelian = idPembelian;
            this.pegawai = pegawai;
            this.supplier = supplier;
            this.tanggal = tanggal;
            this.statusPembayaran = statusPembayaran;
            this.totalHarga = totalHarga;
        }

        public String getIdPembelian() { return idPembelian; }
        public String getPegawai() { return pegawai; }
        public String getSupplier() { return supplier; }
        public String getTanggal() { return tanggal; }
        public String getStatusPembayaran() { return statusPembayaran; }
        public String getMetodePembayaran() { return "Transfer"; }

        public String getTotalHargaFormatted() {
            NumberFormat formatter = NumberFormat.getCurrencyInstance(new Locale("id", "ID"));
            return formatter.format(totalHarga).replace("Rp", "Rp.");
        }
    }

    // =============================================================
    // MODEL - Detail Pembelian (TANPA SUBTOTAL)
    // =============================================================
    public static class DetailPembelianItem {
        private final String namaBarang;
        private final int jumlah;
        private final double harga;

        public DetailPembelianItem(String namaBarang, int jumlah, double harga) {
            this.namaBarang = namaBarang;
            this.jumlah = jumlah;
            this.harga = harga;
        }

        public String getNamaBarang() { return namaBarang; }
        public int getJumlah() { return jumlah; }
        public double getHarga() { return harga; }
        public String getHargaFormatted() { return formatRupiah(harga); }
        // HAPUS method getSubtotalFormatted()
    }
}