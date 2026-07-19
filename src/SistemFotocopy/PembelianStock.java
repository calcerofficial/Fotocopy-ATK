package SistemFotocopy;

import Database.DBConnection;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
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
import javafx.util.Duration;

import java.net.URL;
import java.sql.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ResourceBundle;

public class PembelianStock implements Initializable {

    @FXML
    private Button btnBayar;

    @FXML
    private Button btnNextPage;

    @FXML
    private Button btnPage1;

    @FXML
    private Button btnPrevPage;

    @FXML
    private ComboBox<String> cbSupplier;

    @FXML
    private TableColumn<KatalogItem, String> colAksi;

    @FXML
    private TableColumn<KatalogItem, String> colHarga;

    @FXML
    private TableColumn<KatalogItem, String> colMerkBarang;

    @FXML
    private TableColumn<KatalogItem, String> colNamaBarang;

    @FXML
    private Label lblInfoData;

    @FXML
    private Label lblTotalHarga;

    @FXML
    private BorderPane rootPane;

    @FXML
    private TableView<KatalogItem> tblKatalog;

    @FXML
    private TextField txtCari;

    @FXML
    private VBox vboxItems;

    @FXML
    private TextField txtIdPembelian;

    @FXML
    private TextField txtIdPegawai;

    @FXML
    private TextField txtTanggal;

    // Data untuk tabel katalog
    private ObservableList<KatalogItem> katalogData = FXCollections.observableArrayList();
    private ObservableList<KatalogItem> filteredData = FXCollections.observableArrayList();

    // Data untuk detail pembelian
    private ObservableList<DetailPembelianItem> detailItems = FXCollections.observableArrayList();

    // Variabel untuk session login (diambil dari UserSession)
    private String currentUserID;
    private String currentUserName;

    // Variabel untuk pagination
    private int currentPage = 0;
    private int itemsPerPage = 10;
    private int totalItems = 0;

    // Variabel untuk menyimpan ID Pembelian yang sedang berjalan
    private String currentIDPembelian = "PBK001";
    private String selectedSupplierId = null;

    private Connection connection;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Ambil data session user yang login
        UserSession session = UserSession.getInstance();
        if (session.getIdPegawai() != null) {
            currentUserID = session.getIdPegawai();
            currentUserName = session.getUsername();
        } else {
            currentUserID = "ADM001";
            currentUserName = "admin";
        }

        setupDatabaseConnection();
        setupTableColumns();
        loadSuppliers();
        setupSearchListener();
        loadKatalogData();
        setupPaginationButtons();
        loadNextPembelianID();
        setupBayarButton();
        populateInfoFields();

        // Listener untuk combo box supplier
        cbSupplier.setOnAction(e -> {
            String selected = cbSupplier.getValue();
            if (selected != null && !selected.equals("Pilih Supplier...")) {
                selectedSupplierId = selected.split(" - ")[0];
                resetDetailPembelian();
            } else {
                selectedSupplierId = null;
                resetDetailPembelian();
            }
        });
    }

    private void setupDatabaseConnection() {
        try {
            connection = DBConnection.getConnection();
            System.out.println("Koneksi database berhasil!");
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Error", "Gagal koneksi ke database: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    private void populateInfoFields() {
        if (txtIdPembelian != null) {
            txtIdPembelian.setText(currentIDPembelian);
            txtIdPembelian.setEditable(false);
            txtIdPembelian.setStyle("-fx-background-color: #f8f9fa; -fx-text-fill: #0d6efd; -fx-font-weight: bold;");
        }

        if (txtIdPegawai != null) {
            txtIdPegawai.setText(currentUserID);
            txtIdPegawai.setEditable(false);
            txtIdPegawai.setStyle("-fx-background-color: #f8f9fa; -fx-text-fill: #198754; -fx-font-weight: bold;");
        }

        if (txtTanggal != null) {
            txtTanggal.setText(LocalDate.now().format(DateTimeFormatter.ofPattern("dd-MM-yyyy")));
            txtTanggal.setEditable(false);
            txtTanggal.setStyle("-fx-background-color: #f8f9fa; -fx-text-fill: #dc3545; -fx-font-weight: bold;");
        }
    }

    private void setupTableColumns() {
        colNamaBarang.setCellValueFactory(new PropertyValueFactory<>("namaBarang"));
        colMerkBarang.setCellValueFactory(new PropertyValueFactory<>("merkBarang"));
        colHarga.setCellValueFactory(new PropertyValueFactory<>("hargaFormatted"));
        colAksi.setCellValueFactory(new PropertyValueFactory<>("aksiButton"));

        tblKatalog.setItems(filteredData);
    }

    private void loadSuppliers() {
        if (connection == null) {
            showAlert("Error", "Koneksi database belum tersedia!", Alert.AlertType.ERROR);
            return;
        }

        cbSupplier.getItems().clear();
        cbSupplier.getItems().add("Pilih Supplier...");

        // PAKAI UDF f_SupplierAktif
        String query = "SELECT ID_Supplier, Nama_Supplier FROM dbo.f_SupplierAktif() ORDER BY Nama_Supplier";

        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {

            while (rs.next()) {
                String id = rs.getString("ID_Supplier");
                String nama = rs.getString("Nama_Supplier");
                cbSupplier.getItems().add(id + " - " + nama);
            }

            if (cbSupplier.getItems().size() > 1) {
                cbSupplier.setValue("Pilih Supplier...");
            }

        } catch (SQLException e) {
            e.printStackTrace();
            showAlert("Error", "Gagal memuat data supplier: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    private void setupSearchListener() {
        txtCari.textProperty().addListener((observable, oldValue, newValue) -> {
            filterKatalogData(newValue);
        });
    }

    private void filterKatalogData(String keyword) {
        filteredData.clear();

        if (keyword == null || keyword.trim().isEmpty()) {
            filteredData.addAll(katalogData);
        } else {
            String lowerKeyword = keyword.toLowerCase().trim();
            for (KatalogItem item : katalogData) {
                if (item.getNamaBarang().toLowerCase().contains(lowerKeyword) ||
                        item.getMerkBarang().toLowerCase().contains(lowerKeyword)) {
                    filteredData.add(item);
                }
            }
        }

        totalItems = filteredData.size();
        updatePaginationInfo();
        applyPagination();
    }

    private void loadKatalogData() {
        if (connection == null) {
            showAlert("Error", "Koneksi database belum tersedia!", Alert.AlertType.ERROR);
            return;
        }

        katalogData.clear();

        String query = "SELECT ID_Produk, Nama_Barang, Merk_Barang, Harga, Stok " +
                "FROM dbo.f_CariKatalogBarangPembelian() " +
                "ORDER BY Nama_Barang";

        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {

            while (rs.next()) {
                String id = rs.getString("ID_Produk");
                String nama = rs.getString("Nama_Barang");
                String merk = rs.getString("Merk_Barang");
                double harga = rs.getDouble("Harga");
                int stok = rs.getInt("Stok");

                KatalogItem item = new KatalogItem(id, nama, merk, harga, stok);
                item.setAksiListener(this);
                katalogData.add(item);
            }

            filteredData.addAll(katalogData);
            totalItems = filteredData.size();
            updatePaginationInfo();
            applyPagination();

        } catch (SQLException e) {
            e.printStackTrace();
            showAlert("Error", "Gagal memuat data katalog: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    private void loadNextPembelianID() {
        if (connection == null) {
            currentIDPembelian = "PBK001";
            if (txtIdPembelian != null) txtIdPembelian.setText(currentIDPembelian);
            return;
        }

        String query = "SELECT MAX(CAST(SUBSTRING(ID_Pembelian_Stok, 4, LEN(ID_Pembelian_Stok)) AS INT)) AS MaxID " +
                "FROM Pembelian_Stok";

        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {

            int maxId = 0;
            if (rs.next()) {
                maxId = rs.getInt("MaxID");
            }

            int nextId = maxId + 1;
            currentIDPembelian = "PBK" + String.format("%03d", nextId);

        } catch (SQLException e) {
            e.printStackTrace();
            currentIDPembelian = "PBK001";
        }

        if (txtIdPembelian != null) txtIdPembelian.setText(currentIDPembelian);
    }

    private void setupPaginationButtons() {
        btnPrevPage.setOnAction(e -> {
            if (currentPage > 0) {
                currentPage--;
                applyPagination();
            }
        });

        btnNextPage.setOnAction(e -> {
            int maxPage = (int) Math.ceil((double) totalItems / itemsPerPage) - 1;
            if (currentPage < maxPage) {
                currentPage++;
                applyPagination();
            }
        });
    }

    private void applyPagination() {
        int start = currentPage * itemsPerPage;
        int end = Math.min(start + itemsPerPage, filteredData.size());

        ObservableList<KatalogItem> pageData = FXCollections.observableArrayList();
        for (int i = start; i < end; i++) {
            pageData.add(filteredData.get(i));
        }

        tblKatalog.setItems(pageData);
        updatePaginationInfo();
    }

    private void updatePaginationInfo() {
        int totalPages = (int) Math.ceil((double) totalItems / itemsPerPage);
        int currentPageNumber = currentPage + 1;

        if (totalItems == 0) {
            lblInfoData.setText("Menampilkan 0 dari 0 data");
            btnPrevPage.setDisable(true);
            btnNextPage.setDisable(true);
            return;
        }

        int start = currentPage * itemsPerPage + 1;
        int end = Math.min(start + itemsPerPage - 1, totalItems);

        lblInfoData.setText("Menampilkan " + start + "-" + end + " dari " + totalItems + " data");

        btnPrevPage.setDisable(currentPage == 0);
        btnNextPage.setDisable(currentPage >= totalPages - 1);

        btnPage1.setText("Halaman " + currentPageNumber + "/" + totalPages);
    }

    private void setupBayarButton() {
        btnBayar.setOnAction(e -> handleBayar(e));
    }

    // =============================================================
    // CEK SALDO KAS - APAKAH CUKUP UNTUK PEMBELIAN
    // =============================================================
    private double getSaldoKas() {
        double pendapatanSemua = 0;
        double pengeluaranSemua = 0;

        // Total Pendapatan dari Penjualan
        String queryPendapatan = "SELECT ISNULL(SUM(Total_Harga), 0) AS Total FROM Penjualan WHERE Status_Penjualan = 'Lunas'";
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(queryPendapatan)) {
            if (rs.next()) {
                pendapatanSemua = rs.getDouble("Total");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        // Total Pengeluaran dari Pembelian Stok
        String queryPengeluaranPembelian = "SELECT ISNULL(SUM(Total_Harga), 0) AS Total FROM Pembelian_Stok WHERE Status_Pembayaran = 'Lunas'";
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(queryPengeluaranPembelian)) {
            if (rs.next()) {
                pengeluaranSemua += rs.getDouble("Total");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        // Total Pengeluaran dari Maintenance
        String queryPengeluaranMaintenance = "SELECT ISNULL(SUM(Biaya_Maintenance_Mesin), 0) AS Total FROM Maintenance_Mesin WHERE Status_Maintenance = 'selesai'";
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(queryPengeluaranMaintenance)) {
            if (rs.next()) {
                pengeluaranSemua += rs.getDouble("Total");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return pendapatanSemua - pengeluaranSemua;
    }

    @FXML
    void handleBayar(ActionEvent event) {
        if (cbSupplier.getValue() == null || cbSupplier.getValue().equals("Pilih Supplier...")) {
            showAlert("Peringatan", "Silakan pilih supplier terlebih dahulu!", Alert.AlertType.WARNING);
            return;
        }

        if (detailItems.isEmpty()) {
            showAlert("Peringatan", "Belum ada item yang dipilih untuk dibeli!", Alert.AlertType.WARNING);
            return;
        }

        // =============================================================
        // HITUNG TOTAL HARGA PEMBELIAN
        // =============================================================
        double totalHarga = 0;
        for (DetailPembelianItem item : detailItems) {
            totalHarga += item.getJumlah() * item.getHarga();
        }

        // =============================================================
        // CEK SALDO KAS - APAKAH CUKUP?
        // =============================================================
        double saldoKas = getSaldoKas();

        if (saldoKas < totalHarga) {
            // Saldo tidak cukup
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Saldo Tidak Cukup");
            alert.setHeaderText("❌ Pembelian Gagal");
            alert.setContentText(
                    "Saldo kas tidak mencukupi untuk pembelian ini!\n\n" +
                            "💰 Saldo Kas Saat Ini: " + formatRupiah(saldoKas) + "\n" +
                            "🛒 Total Pembelian: " + formatRupiah(totalHarga) + "\n" +
                            "🔴 Kekurangan: " + formatRupiah(totalHarga - saldoKas) + "\n\n" +
                            "Silakan tambahkan pendapatan terlebih dahulu."
            );
            alert.showAndWait();
            return;
        }

        // =============================================================
        // SALDO CUKUP - LANJUTKAN PROSES PEMBAYARAN
        // =============================================================
        String supplierValue = cbSupplier.getValue();
        String idSupplier = supplierValue.split(" - ")[0];

        StringBuilder detailBuilder = new StringBuilder();
        for (DetailPembelianItem item : detailItems) {
            if (detailBuilder.length() > 0) {
                detailBuilder.append("|");
            }
            detailBuilder.append(item.getIdProduk())
                    .append(":")
                    .append(item.getJumlah())
                    .append(":")
                    .append((long) item.getHarga());
        }

        final double finalTotalHarga = totalHarga;
        final String detailString = detailBuilder.toString();
        final String idSupplierFinal = idSupplier;

        // Show loading dialog
        Stage loadingStage = new Stage();
        loadingStage.initModality(Modality.APPLICATION_MODAL);
        loadingStage.setTitle("Memproses Pembayaran");
        loadingStage.setResizable(false);

        VBox loadingBox = new VBox(20);
        loadingBox.setAlignment(Pos.CENTER);
        loadingBox.setPadding(new Insets(30));

        Label loadingLabel = new Label("⏳ Menunggu pembayaran...");
        loadingLabel.setFont(Font.font("System", FontWeight.BOLD, 16));

        ProgressIndicator progressIndicator = new ProgressIndicator();

        loadingBox.getChildren().addAll(progressIndicator, loadingLabel);
        Scene loadingScene = new Scene(loadingBox, 350, 200);
        loadingStage.setScene(loadingScene);

        PauseTransition pause = new PauseTransition(Duration.seconds(2));

        pause.setOnFinished(e -> {
            loadingStage.close();

            // Proses pembelian di background
            try {
                String result = processPembelian(idSupplierFinal, detailString, finalTotalHarga);

                Platform.runLater(() -> {
                    if (result != null) {
                        showAlert("Sukses", "✅ Pembayaran telah berhasil!\nID Pembelian: " + result, Alert.AlertType.INFORMATION);
                        showDetailPembayaran(result, idSupplierFinal, detailItems, finalTotalHarga);
                        resetForm();
                        loadNextPembelianID();
                        populateInfoFields();
                    } else {
                        showAlert("Error", "Gagal memproses pembelian. Silakan coba lagi.", Alert.AlertType.ERROR);
                    }
                });

            } catch (Exception ex) {
                ex.printStackTrace();
                Platform.runLater(() -> {
                    showAlert("Error", "Terjadi kesalahan: " + ex.getMessage(), Alert.AlertType.ERROR);
                });
            }
        });

        loadingStage.show();
        pause.play();
    }

    private String processPembelian(String idSupplier, String detailString, double totalHarga) {
        if (connection == null) {
            showAlert("Error", "Koneksi database tidak tersedia!", Alert.AlertType.ERROR);
            return null;
        }

        String generatedID = null;
        String sql = "{CALL sp_TambahPembelianStok(?, ?, ?, ?)}";

        try (CallableStatement cs = connection.prepareCall(sql)) {
            cs.setString(1, currentUserID);
            cs.setString(2, idSupplier);
            cs.setDate(3, java.sql.Date.valueOf(LocalDate.now()));
            cs.setString(4, detailString);

            boolean hasResultSet = cs.execute();

            if (hasResultSet) {
                ResultSet rs = cs.getResultSet();
                if (rs.next()) {
                    generatedID = rs.getString("ID_Pembelian_Baru");
                }
                rs.close();
            }

            if (cs.getMoreResults()) {
                ResultSet errorRs = cs.getResultSet();
                if (errorRs.next()) {
                    String errorMsg = errorRs.getString("ErrorMessage");
                    if (errorMsg != null) {
                        showAlert("Error", "Gagal memproses pembelian: " + errorMsg, Alert.AlertType.ERROR);
                        return null;
                    }
                }
                errorRs.close();
            }

        } catch (SQLException e) {
            e.printStackTrace();
            showAlert("Error", "Database error: " + e.getMessage(), Alert.AlertType.ERROR);
            return null;
        }

        return generatedID;
    }

    private void showDetailPembayaran(String idPembelian, String idSupplier,
                                      ObservableList<DetailPembelianItem> items, double totalHarga) {
        Stage detailStage = new Stage();
        detailStage.initModality(Modality.APPLICATION_MODAL);
        detailStage.setTitle("Detail Pembayaran");
        detailStage.setResizable(false);

        VBox mainBox = new VBox(15);
        mainBox.setPadding(new Insets(20));
        mainBox.setAlignment(Pos.TOP_CENTER);

        Label headerLabel = new Label("📋 DETAIL PEMBAYARAN");
        headerLabel.setFont(Font.font("System", FontWeight.BOLD, 18));

        VBox infoBox = new VBox(5);
        infoBox.setAlignment(Pos.CENTER_LEFT);

        Label idLabel = new Label("ID Pembelian: " + idPembelian);
        idLabel.setFont(Font.font("System", FontWeight.BOLD, 14));

        Label supplierLabel = new Label("Supplier: " + getSupplierName(idSupplier));
        Label tanggalLabel = new Label("Tanggal: " + LocalDate.now().format(DateTimeFormatter.ofPattern("dd-MM-yyyy")));
        Label pegawaiLabel = new Label("Pegawai: " + currentUserName + " (" + currentUserID + ")");

        infoBox.getChildren().addAll(idLabel, supplierLabel, tanggalLabel, pegawaiLabel);

        Separator separator1 = new Separator();

        Label itemsLabel = new Label("Daftar Item:");
        itemsLabel.setFont(Font.font("System", FontWeight.BOLD, 14));

        TableView<DetailPembelianItem> detailTable = new TableView<>();
        detailTable.setPrefWidth(450);
        detailTable.setPrefHeight(200);
        detailTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        TableColumn<DetailPembelianItem, String> colNama = new TableColumn<>("Nama Barang");
        colNama.setCellValueFactory(new PropertyValueFactory<>("namaBarang"));
        colNama.setPrefWidth(200);

        TableColumn<DetailPembelianItem, Integer> colJumlah = new TableColumn<>("Jumlah");
        colJumlah.setCellValueFactory(new PropertyValueFactory<>("jumlah"));
        colJumlah.setPrefWidth(80);
        colJumlah.setStyle("-fx-alignment: CENTER;");

        TableColumn<DetailPembelianItem, String> colHargaItem = new TableColumn<>("Harga");
        colHargaItem.setCellValueFactory(new PropertyValueFactory<>("hargaFormatted"));
        colHargaItem.setPrefWidth(150);
        colHargaItem.setStyle("-fx-alignment: CENTER-RIGHT;");

        // HAPUS KOLOM SUBTOTAL
        detailTable.getColumns().addAll(colNama, colJumlah, colHargaItem);
        detailTable.setItems(items);

        Separator separator2 = new Separator();

        HBox totalBox = new HBox(20);
        totalBox.setAlignment(Pos.CENTER_RIGHT);

        Label totalLabel = new Label("Total Harga:");
        totalLabel.setFont(Font.font("System", FontWeight.BOLD, 16));

        Label totalValueLabel = new Label(formatRupiah(totalHarga));
        totalValueLabel.setFont(Font.font("System", FontWeight.BOLD, 18));
        totalValueLabel.setStyle("-fx-text-fill: #2e7d32;");

        totalBox.getChildren().addAll(totalLabel, totalValueLabel);

        Label statusLabel = new Label("✅ Status: LUNAS");
        statusLabel.setFont(Font.font("System", FontWeight.BOLD, 16));
        statusLabel.setStyle("-fx-text-fill: #2e7d32;");

        Button btnOk = new Button("OK");
        btnOk.setPrefWidth(100);
        btnOk.setOnAction(e -> detailStage.close());

        mainBox.getChildren().addAll(
                headerLabel,
                infoBox,
                separator1,
                itemsLabel,
                detailTable,
                separator2,
                totalBox,
                statusLabel,
                btnOk
        );

        Scene scene = new Scene(mainBox, 500, 500);
        detailStage.setScene(scene);
        detailStage.showAndWait();
    }

    private String getSupplierName(String idSupplier) {
        if (connection == null) {
            return idSupplier;
        }

        String query = "SELECT Nama_Supplier FROM Supplier WHERE ID_Supplier = ?";

        try (PreparedStatement ps = connection.prepareStatement(query)) {
            ps.setString(1, idSupplier);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                return rs.getString("Nama_Supplier");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return idSupplier;
    }

    private void resetForm() {
        detailItems.clear();
        vboxItems.getChildren().clear();
        lblTotalHarga.setText("Rp. 0");
        cbSupplier.setValue("Pilih Supplier...");
        txtCari.clear();
        katalogData.clear();
        filteredData.clear();
        tblKatalog.setItems(filteredData);
        updatePaginationInfo();
        populateInfoFields();
    }

    private void resetDetailPembelian() {
        detailItems.clear();
        vboxItems.getChildren().clear();
        lblTotalHarga.setText("Rp. 0");
    }

    private String formatRupiah(double amount) {
        return String.format("Rp. %,.0f", amount);
    }

    private void showAlert(String title, String message, Alert.AlertType type) {
        Platform.runLater(() -> {
            Alert alert = new Alert(type);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.showAndWait();
        });
    }

    public void addItemToDetail(KatalogItem item) {
        for (DetailPembelianItem existingItem : detailItems) {
            if (existingItem.getIdProduk().equals(item.getIdProduk())) {
                existingItem.setJumlah(existingItem.getJumlah() + 1);
                updateDetailUI();
                return;
            }
        }

        DetailPembelianItem newItem = new DetailPembelianItem(
                item.getIdProduk(),
                item.getNamaBarang(),
                1,
                item.getHarga()
        );

        detailItems.add(newItem);
        updateDetailUI();
    }

    private void updateDetailUI() {
        vboxItems.getChildren().clear();

        if (detailItems.isEmpty()) {
            Label emptyLabel = new Label("Belum ada item yang dipilih");
            emptyLabel.setStyle("-fx-text-fill: #888888; -fx-font-style: italic;");
            emptyLabel.setAlignment(Pos.CENTER);
            emptyLabel.setPrefWidth(Double.MAX_VALUE);
            vboxItems.getChildren().add(emptyLabel);
            lblTotalHarga.setText("Rp. 0");
            return;
        }

        double totalHarga = 0;

        for (DetailPembelianItem item : detailItems) {
            totalHarga += item.getJumlah() * item.getHarga();

            VBox itemCard = new VBox(8);
            itemCard.setPadding(new Insets(10, 12, 10, 12));
            itemCard.setStyle(
                    "-fx-background-color: white; " +
                            "-fx-background-radius: 8; " +
                            "-fx-border-color: #e0e0e0; " +
                            "-fx-border-radius: 8; " +
                            "-fx-border-width: 1; " +
                            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.05), 4, 0, 0, 2);"
            );

            HBox nameRow = new HBox(10);
            nameRow.setAlignment(Pos.CENTER_LEFT);

            Label nameLabel = new Label(item.getNamaBarang());
            nameLabel.setFont(Font.font("System", FontWeight.BOLD, 14));
            nameLabel.setStyle("-fx-text-fill: #1a1a2e;");

            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);

            Label subtotalLabel = new Label(formatRupiah(item.getJumlah() * item.getHarga()));
            subtotalLabel.setFont(Font.font("System", FontWeight.BOLD, 14));
            subtotalLabel.setStyle("-fx-text-fill: #198754;");

            nameRow.getChildren().addAll(nameLabel, spacer, subtotalLabel);

            HBox controlRow = new HBox(8);
            controlRow.setAlignment(Pos.CENTER_LEFT);

            Button btnMinus = new Button("−");
            btnMinus.setPrefWidth(30);
            btnMinus.setPrefHeight(30);
            btnMinus.setStyle(
                    "-fx-background-color: #dc3545; " +
                            "-fx-text-fill: white; " +
                            "-fx-font-weight: bold; " +
                            "-fx-background-radius: 15; " +
                            "-fx-font-size: 14;"
            );
            btnMinus.setOnAction(e -> {
                if (item.getJumlah() > 1) {
                    item.setJumlah(item.getJumlah() - 1);
                    updateDetailUI();
                } else {
                    detailItems.remove(item);
                    updateDetailUI();
                }
            });

            Label countLabel = new Label(String.valueOf(item.getJumlah()));
            countLabel.setFont(Font.font("System", FontWeight.BOLD, 14));
            countLabel.setStyle("-fx-text-fill: #0d6efd;");
            countLabel.setPrefWidth(30);
            countLabel.setAlignment(Pos.CENTER);

            Button btnPlus = new Button("+");
            btnPlus.setPrefWidth(30);
            btnPlus.setPrefHeight(30);
            btnPlus.setStyle(
                    "-fx-background-color: #198754; " +
                            "-fx-text-fill: white; " +
                            "-fx-font-weight: bold; " +
                            "-fx-background-radius: 15; " +
                            "-fx-font-size: 14;"
            );
            btnPlus.setOnAction(e -> {
                item.setJumlah(item.getJumlah() + 1);
                updateDetailUI();
            });

            Region spacer2 = new Region();
            HBox.setHgrow(spacer2, Priority.ALWAYS);

            Label priceLabel = new Label(formatRupiah(item.getHarga()) + " / item");
            priceLabel.setFont(Font.font("System", 12));
            priceLabel.setStyle("-fx-text-fill: #6c757d;");

            Button btnRemove = new Button("✕");
            btnRemove.setPrefWidth(30);
            btnRemove.setPrefHeight(30);
            btnRemove.setStyle(
                    "-fx-background-color: #6c757d; " +
                            "-fx-text-fill: white; " +
                            "-fx-font-weight: bold; " +
                            "-fx-background-radius: 15; " +
                            "-fx-font-size: 12;"
            );
            btnRemove.setOnAction(e -> {
                detailItems.remove(item);
                updateDetailUI();
            });

            controlRow.getChildren().addAll(
                    btnMinus, countLabel, btnPlus, spacer2, priceLabel, btnRemove
            );

            itemCard.getChildren().addAll(nameRow, controlRow);
            vboxItems.getChildren().add(itemCard);
        }

        lblTotalHarga.setText(formatRupiah(totalHarga));
    }

    // ==================== INNER CLASS ====================

    public static class KatalogItem {
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

        public void setAksiListener(PembelianStock controller) {
            aksiButton.setOnAction(e -> controller.addItemToDetail(this));
        }
    }

    public static class DetailPembelianItem {
        private final String idProduk;
        private final String namaBarang;
        private int jumlah;
        private final double harga;

        public DetailPembelianItem(String idProduk, String namaBarang, int jumlah, double harga) {
            this.idProduk = idProduk;
            this.namaBarang = namaBarang;
            this.jumlah = jumlah;
            this.harga = harga;
        }

        public String getIdProduk() { return idProduk; }
        public String getNamaBarang() { return namaBarang; }
        public int getJumlah() { return jumlah; }
        public void setJumlah(int jumlah) { this.jumlah = jumlah; }
        public double getHarga() { return harga; }
        public String getHargaFormatted() { return String.format("Rp. %,.0f", harga); }
    }
}