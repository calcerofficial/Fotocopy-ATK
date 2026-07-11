package SistemFotocopy;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.text.NumberFormat;
import java.util.Locale;

public class DataProduk {

    @FXML private Button btnBatal;
    @FXML private Button btnHapus;
    @FXML private Button btnNextPage;
    @FXML private Button btnPage1;
    @FXML private Button btnPrevPage;
    @FXML private Button btnSimpan;
    @FXML private Button btnUbah;
    @FXML private ComboBox<String> cmbKategoriProduk;
    @FXML private ComboBox<String> cmbStatus;

    // ERROR LABELS
    @FXML private Label lblErrorNama;
    @FXML private Label lblErrorHarga;
    @FXML private Label lblErrorStock;
    @FXML private Label lblErrorMerk;

    @FXML private TableView<Produk> tblProduk;
    @FXML private TableColumn<Produk, String> colIdBarang;
    @FXML private TableColumn<Produk, String> colNamaBarang;
    @FXML private TableColumn<Produk, String> colMerkProduk;
    @FXML private TableColumn<Produk, String> colKategoriProduk;
    @FXML private TableColumn<Produk, String> colHarga;
    @FXML private TableColumn<Produk, String> colStock;
    @FXML private TableColumn<Produk, String> colStatusProduk;

    @FXML private Label lblInfoData;
    @FXML private Label lblProdukTersedia;
    @FXML private Label lblProdukTidakTersedia;
    @FXML private Label lblTotalProduk;
    @FXML private BorderPane rootPane;

    @FXML private TextField txtCari;
    @FXML private TextField txtHargaBarang;
    @FXML private TextField txtIdBarang;
    @FXML private TextField txtMerk;
    @FXML private TextField txtNamaBarang;
    @FXML private TextField txtStockBarang;

    private ObservableList<Produk> listProduk = FXCollections.observableArrayList();
    private ObservableList<Produk> filteredList = FXCollections.observableArrayList();
    private Connection conn;

    private int currentPage = 1;
    private final int rowsPerPage = 10;

    // Flag untuk mencegah loop saat update text
    private boolean isUpdatingHarga = false;

    private void koneksi() {
        try {
            String url = "jdbc:sqlserver://kelompok-5.database.windows.net:1433;database=FotoCopyATK;user=hilmi;password=Kelompok5;trustServerCertificate=true;";
            conn = DriverManager.getConnection(url);
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Error Koneksi", "Gagal terhubung ke database: " + e.getMessage());
        }
    }

    @FXML
    public void initialize() {
        koneksi();

        btnSimpan.setDisable(false);
        btnUbah.setDisable(true);
        btnHapus.setDisable(true);

        txtIdBarang.setDisable(true);
        cmbStatus.setDisable(true);

        // BINDING KOLOM TABEL
        colIdBarang.setCellValueFactory(cellData -> cellData.getValue().idProdukProperty());
        colNamaBarang.setCellValueFactory(cellData -> cellData.getValue().namaProdukProperty());
        colMerkProduk.setCellValueFactory(cellData -> cellData.getValue().merkProperty());
        colKategoriProduk.setCellValueFactory(cellData -> cellData.getValue().kategoriProperty());
        colStatusProduk.setCellValueFactory(cellData -> cellData.getValue().statusProperty());

        colHarga.setCellValueFactory(cellData -> {
            double harga = cellData.getValue().getHarga();
            return new javafx.beans.property.SimpleStringProperty(formatRupiah(harga));
        });

        colStock.setCellValueFactory(cellData -> {
            String kat = cellData.getValue().getKategori();
            if ("layanan".equalsIgnoreCase(kat)) {
                return new javafx.beans.property.SimpleStringProperty("-");
            } else {
                return new javafx.beans.property.SimpleStringProperty(String.valueOf(cellData.getValue().getStok()));
            }
        });

        cmbKategoriProduk.setItems(FXCollections.observableArrayList("Barang", "Layanan"));
        cmbStatus.setItems(FXCollections.observableArrayList("Tersedia", "NonTersedia"));

        // =========================================================
        // VALIDASI INPUT
        // =========================================================
        setupInputValidation();

        // LISTENER KATEGORI
        cmbKategoriProduk.getSelectionModel().selectedItemProperty().addListener((options, oldValue, newValue) -> {
            if (newValue != null && tblProduk.getSelectionModel().getSelectedItem() == null) {
                generateIdOtomatis();
                cmbStatus.setValue("Tersedia");
                cmbStatus.setDisable(true);

                if ("Layanan".equalsIgnoreCase(newValue)) {
                    txtStockBarang.setText("-");
                    txtMerk.setText("-");
                    txtStockBarang.setDisable(true);
                    txtMerk.setDisable(true);
                } else {
                    txtStockBarang.clear();
                    txtMerk.clear();
                    txtStockBarang.setDisable(false);
                    txtMerk.setDisable(false);
                }
            }
        });

        loadDataProduk();

        // LISTENER KLIK TABEL
        tblProduk.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            if (newSelection != null) {
                btnSimpan.setDisable(true);
                btnUbah.setDisable(false);
                btnHapus.setDisable(false);

                txtIdBarang.setText(newSelection.getIdProduk());
                txtNamaBarang.setText(newSelection.getNamaProduk());
                txtMerk.setText(newSelection.getMerk());
                cmbKategoriProduk.setValue(newSelection.getKategori());

                // Set harga dengan format Rupiah
                isUpdatingHarga = true;
                txtHargaBarang.setText(formatRupiah(newSelection.getHarga()));
                isUpdatingHarga = false;

                // STATUS MUNCUL SAAT UBAH
                cmbStatus.setValue(newSelection.getStatus());
                cmbStatus.setDisable(false);

                if ("layanan".equalsIgnoreCase(newSelection.getKategori())) {
                    txtStockBarang.setText("-");
                    txtStockBarang.setDisable(true);
                    txtMerk.setDisable(true);
                } else {
                    txtStockBarang.setText(String.valueOf(newSelection.getStok()));
                    txtStockBarang.setDisable(false);
                    txtMerk.setDisable(false);
                }

                hideAllErrorLabels();
            }
        });

        txtCari.textProperty().addListener((observable, oldValue, newValue) -> {
            cariDataProduk(newValue);
        });
    }

    // =========================================================
    // VALIDASI INPUT - FINAL
    // =========================================================
    private void setupInputValidation() {
        // 1. NAMA PRODUK - Hanya huruf, spasi, dan angka (TIDAK BOLEH SIMBOL)
        TextFormatter<String> namaFormatter = new TextFormatter<>(change -> {
            String newText = change.getControlNewText();
            if (newText.isEmpty()) {
                txtNamaBarang.setStyle(null);
                return change;
            }
            if (!newText.matches("^[a-zA-Z0-9\\s]*$")) {
                txtNamaBarang.setStyle("-fx-border-color: red; -fx-border-width: 2px;");
                return null;
            }
            txtNamaBarang.setStyle(null);
            return change;
        });
        txtNamaBarang.setTextFormatter(namaFormatter);

        // 2. HARGA - Format Rupiah otomatis
        // 2. HARGA - Format Rupiah otomatis, minimal 1000, TIDAK BISA 0 DI AWAL
        txtHargaBarang.textProperty().addListener((observable, oldValue, newValue) -> {
            if (isUpdatingHarga) return;

            // Jika kosong, reset
            if (newValue == null || newValue.isEmpty()) {
                txtHargaBarang.setStyle(null);
                hideErrorLabel(lblErrorHarga);
                return;
            }

            // Hanya ambil angka
            String cleanString = newValue.replaceAll("[^0-9]", "");

            // Jika tidak ada angka, clear
            if (cleanString.isEmpty()) {
                isUpdatingHarga = true;
                txtHargaBarang.setText("");
                isUpdatingHarga = false;
                txtHargaBarang.setStyle("-fx-border-color: red; -fx-border-width: 2px;");
                showErrorLabel(lblErrorHarga, "Harga harus diisi angka");
                return;
            }

            // CEK APAKAH DIAWALI 0 - LANGSUNG TOLAK/TIDAK BISA DIKETIK
            if (cleanString.startsWith("0")) {
                // Kembalikan ke nilai sebelumnya (oldValue)
                isUpdatingHarga = true;
                txtHargaBarang.setText(oldValue != null ? oldValue : "");
                isUpdatingHarga = false;
                txtHargaBarang.setStyle("-fx-border-color: red; -fx-border-width: 2px;");
                showErrorLabel(lblErrorHarga, "Harga tidak boleh diawali 0");
                return;
            }

            try {
                int value = Integer.parseInt(cleanString);

                // Validasi minimal 1000
                if (value < 1000) {
                    txtHargaBarang.setStyle("-fx-border-color: red; -fx-border-width: 2px;");
                    showErrorLabel(lblErrorHarga, "Harga minimal Rp1.000");
                    return;
                }

                // Validasi maksimal 100000
                if (value > 100000) {
                    txtHargaBarang.setStyle("-fx-border-color: red; -fx-border-width: 2px;");
                    showErrorLabel(lblErrorHarga, "Harga maksimal Rp100.000");
                    return;
                }

                // Format Rupiah
                isUpdatingHarga = true;
                String formatted = formatRupiah(value);
                txtHargaBarang.setText(formatted);
                txtHargaBarang.positionCaret(formatted.length());
                isUpdatingHarga = false;

                txtHargaBarang.setStyle(null);
                hideErrorLabel(lblErrorHarga);

            } catch (NumberFormatException e) {
                txtHargaBarang.setStyle("-fx-border-color: red; -fx-border-width: 2px;");
                showErrorLabel(lblErrorHarga, "Harga harus berupa angka");
            }
        });

        // 3. STOCK - Hanya angka, minimal 10
        txtStockBarang.textProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue == null || newValue.isEmpty()) {
                txtStockBarang.setStyle(null);
                hideErrorLabel(lblErrorStock);
                return;
            }

            // Hanya ambil angka
            String cleanString = newValue.replaceAll("[^0-9]", "");

            // Jika tidak ada angka, clear
            if (cleanString.isEmpty()) {
                txtStockBarang.setText("");
                txtStockBarang.setStyle("-fx-border-color: red; -fx-border-width: 2px;");
                showErrorLabel(lblErrorStock, "Stock harus diisi angka");
                return;
            }

            // CEK APAKAH DIAWALI 0 - LANGSUNG TOLAK
            if (cleanString.startsWith("0")) {
                txtStockBarang.setText(oldValue != null ? oldValue : "");
                txtStockBarang.setStyle("-fx-border-color: red; -fx-border-width: 2px;");
                showErrorLabel(lblErrorStock, "Stock tidak boleh diawali 0");
                return;
            }

            try {
                int value = Integer.parseInt(cleanString);

                // Validasi minimal 10
                if (value < 10) {
                    txtStockBarang.setStyle("-fx-border-color: red; -fx-border-width: 2px;");
                    showErrorLabel(lblErrorStock, "Stock minimal 10");
                    return;
                }

                txtStockBarang.setStyle(null);
                hideErrorLabel(lblErrorStock);

            } catch (NumberFormatException e) {
                txtStockBarang.setStyle("-fx-border-color: red; -fx-border-width: 2px;");
                showErrorLabel(lblErrorStock, "Stock harus berupa angka");
            }
        });

        // 4. MERK - Hanya huruf, angka, dan spasi (TIDAK BOLEH SIMBOL)
        TextFormatter<String> merkFormatter = new TextFormatter<>(change -> {
            String newText = change.getControlNewText();
            if (newText.isEmpty()) {
                txtMerk.setStyle(null);
                return change;
            }
            // Hanya huruf, angka, dan spasi
            if (!newText.matches("^[a-zA-Z0-9\\s]*$")) {
                txtMerk.setStyle("-fx-border-color: red; -fx-border-width: 2px;");
                return null;
            }
            txtMerk.setStyle(null);
            return change;
        });
        txtMerk.setTextFormatter(merkFormatter);
    }

    // =========================================================
    // CHECK INPUT ERRORS
    // =========================================================
    private boolean checkInputErrors() {
        boolean hasError = false;

        // Cek Nama Produk
        String nama = txtNamaBarang.getText();
        if (!nama.isEmpty() && !nama.matches("^[a-zA-Z0-9\\s]+$")) {
            txtNamaBarang.setStyle("-fx-border-color: red; -fx-border-width: 2px;");
            showErrorLabel(lblErrorNama, "Nama hanya boleh huruf, angka, dan spasi");
            hasError = true;
        } else {
            txtNamaBarang.setStyle(null);
            hideErrorLabel(lblErrorNama);
        }

        // Cek Harga
        String hargaText = txtHargaBarang.getText();
        if (!hargaText.isEmpty()) {
            try {
                int harga = Integer.parseInt(hargaText.replaceAll("[^0-9]", ""));
                if (harga < 1000 || harga > 100000) {
                    txtHargaBarang.setStyle("-fx-border-color: red; -fx-border-width: 2px;");
                    showErrorLabel(lblErrorHarga, "Harga harus 1000 - 100000");
                    hasError = true;
                } else {
                    txtHargaBarang.setStyle(null);
                    hideErrorLabel(lblErrorHarga);
                }
            } catch (NumberFormatException e) {
                txtHargaBarang.setStyle("-fx-border-color: red; -fx-border-width: 2px;");
                showErrorLabel(lblErrorHarga, "Harga harus berupa angka");
                hasError = true;
            }
        }

        // Cek Stock (hanya jika kategori Barang)
        String kategori = cmbKategoriProduk.getValue();
        if ("Barang".equalsIgnoreCase(kategori)) {
            String stockText = txtStockBarang.getText();
            if (!stockText.isEmpty() && !stockText.equals("-")) {
                try {
                    int stock = Integer.parseInt(stockText);
                    if (stock < 10) {
                        txtStockBarang.setStyle("-fx-border-color: red; -fx-border-width: 2px;");
                        showErrorLabel(lblErrorStock, "Stock minimal 10");
                        hasError = true;
                    } else {
                        txtStockBarang.setStyle(null);
                        hideErrorLabel(lblErrorStock);
                    }
                } catch (NumberFormatException e) {
                    txtStockBarang.setStyle("-fx-border-color: red; -fx-border-width: 2px;");
                    showErrorLabel(lblErrorStock, "Stock harus berupa angka");
                    hasError = true;
                }
            }
        }

        // Cek Merk (hanya jika kategori Barang)
        if ("Barang".equalsIgnoreCase(kategori)) {
            String merk = txtMerk.getText();
            if (!merk.isEmpty() && !merk.matches("^[a-zA-Z0-9]+$")) {
                txtMerk.setStyle("-fx-border-color: red; -fx-border-width: 2px;");
                showErrorLabel(lblErrorMerk, "Merk hanya boleh huruf dan angka");
                hasError = true;
            } else {
                txtMerk.setStyle(null);
                hideErrorLabel(lblErrorMerk);
            }
        }

        return hasError;
    }

    // =========================================================
    // ERROR LABEL HELPERS
    // =========================================================
    private void hideAllErrorLabels() {
        if (lblErrorNama != null) { lblErrorNama.setVisible(false); lblErrorNama.setText(""); }
        if (lblErrorHarga != null) { lblErrorHarga.setVisible(false); lblErrorHarga.setText(""); }
        if (lblErrorStock != null) { lblErrorStock.setVisible(false); lblErrorStock.setText(""); }
        if (lblErrorMerk != null) { lblErrorMerk.setVisible(false); lblErrorMerk.setText(""); }
    }

    private void showErrorLabel(Label errorLabel, String message) {
        if (errorLabel != null) {
            errorLabel.setText("⚠ " + message);
            errorLabel.setVisible(true);
            errorLabel.setManaged(true);
            errorLabel.setStyle("-fx-text-fill: #ff4444; -fx-font-size: 11px; -fx-padding: 2 0 0 5; -fx-font-weight: bold;");
        }
    }

    private void hideErrorLabel(Label errorLabel) {
        if (errorLabel != null) {
            errorLabel.setVisible(false);
            errorLabel.setManaged(false);
            errorLabel.setText("");
        }
    }

    private void generateIdOtomatis() {
        String query = "SELECT MAX(CAST(SUBSTRING(ID_Produk, 4, LEN(ID_Produk)) AS INT)) AS max_angka FROM Produk";
        try {
            if (conn == null || conn.isClosed()) koneksi();
            try (PreparedStatement ps = conn.prepareStatement(query);
                 ResultSet rs = ps.executeQuery()) {

                if (rs.next() && rs.getObject("max_angka") != null) {
                    int nextNumber = rs.getInt("max_angka") + 1;
                    txtIdBarang.setText(String.format("PDK%03d", nextNumber));
                } else {
                    txtIdBarang.setText("PDK001");
                }
            }
        } catch (Exception e) {
            System.out.println("Gagal generate ID otomatis: " + e.getMessage());
        }
    }

    private void loadDataProduk() {
        listProduk.clear();
        String query = "SELECT * FROM v_TampilSemuaProduk " +
                "ORDER BY CASE WHEN Status_Barang = 'NonTersedia' THEN 2 ELSE 1 END ASC, ID_Produk ASC";

        try {
            if (conn == null || conn.isClosed()) koneksi();

            try (PreparedStatement ps = conn.prepareStatement(query);
                 ResultSet rs = ps.executeQuery()) {

                int total = 0, tersedia = 0, tidakTersedia = 0;

                while (rs.next()) {
                    String id = rs.getString("ID_Produk");
                    String nama = rs.getString("Nama_Barang");
                    String kategori = rs.getString("Kategori_Produk");
                    double harga = rs.getDouble("Harga");
                    int stok = 0;

                    try {
                        stok = rs.getInt("Stok");
                    } catch (Exception ignored) {}

                    String merk = rs.getString("Merk_Barang");
                    String status = rs.getString("Status_Barang");

                    listProduk.add(new Produk(id, nama, merk, kategori, harga, stok, status));

                    total++;
                    if ("Tersedia".equalsIgnoreCase(status) || "tersedia".equalsIgnoreCase(status)) tersedia++;
                    else tidakTersedia++;
                }

                filteredList.setAll(listProduk);
                currentPage = 1;
                updateTableAndPagination();

                lblTotalProduk.setText(String.valueOf(total));
                lblProdukTersedia.setText(String.valueOf(tersedia));
                lblProdukTidakTersedia.setText(String.valueOf(tidakTersedia));
            }
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Error Database", "Gagal memuat data: " + e.getMessage());
        }
    }

    private void updateTableAndPagination() {
        int totalRows = filteredList.size();
        int maxPage = (int) Math.ceil((double) totalRows / rowsPerPage);
        if (maxPage == 0) maxPage = 1;

        if (currentPage > maxPage) currentPage = maxPage;
        if (currentPage < 1) currentPage = 1;

        int fromIndex = (currentPage - 1) * rowsPerPage;
        int toIndex = Math.min(fromIndex + rowsPerPage, totalRows);

        ObservableList<Produk> pageItems = FXCollections.observableArrayList();
        if (totalRows > 0) {
            pageItems.setAll(filteredList.subList(fromIndex, toIndex));
        }
        tblProduk.setItems(pageItems);

        if (totalRows == 0) {
            lblInfoData.setText("Menampilkan 0 dari 0 data");
        } else {
            lblInfoData.setText("Menampilkan " + (fromIndex + 1) + "-" + toIndex + " dari " + totalRows + " data");
        }

        btnPage1.setText(String.valueOf(currentPage));
        btnPrevPage.setDisable(currentPage == 1);
        btnNextPage.setDisable(currentPage == maxPage || totalRows == 0);
    }

    @FXML
    void handleNextPage(ActionEvent event) {
        currentPage++;
        updateTableAndPagination();
    }

    @FXML
    void handlePrevPage(ActionEvent event) {
        currentPage--;
        updateTableAndPagination();
    }

    @FXML
    void handleSimpanData(ActionEvent event) {
        // Validasi input
        if (checkInputErrors()) {
            showAlert(Alert.AlertType.WARNING, "Validasi Gagal", "Mohon perbaiki input yang ditandai merah");
            return;
        }

        // Validasi wajib isi
        if (txtNamaBarang.getText().isEmpty()) {
            showErrorLabel(lblErrorNama, "Nama produk wajib diisi");
            showAlert(Alert.AlertType.WARNING, "Validasi Gagal", "Nama produk wajib diisi");
            return;
        }

        if (txtHargaBarang.getText().isEmpty()) {
            showErrorLabel(lblErrorHarga, "Harga wajib diisi");
            showAlert(Alert.AlertType.WARNING, "Validasi Gagal", "Harga wajib diisi");
            return;
        }

        String kategori = cmbKategoriProduk.getValue();
        if (kategori == null) {
            showAlert(Alert.AlertType.WARNING, "Validasi Gagal", "Kategori wajib dipilih");
            return;
        }

        if ("Barang".equalsIgnoreCase(kategori)) {
            if (txtStockBarang.getText().isEmpty()) {
                showErrorLabel(lblErrorStock, "Stock wajib diisi");
                showAlert(Alert.AlertType.WARNING, "Validasi Gagal", "Stock wajib diisi");
                return;
            }
            if (txtMerk.getText().isEmpty()) {
                showErrorLabel(lblErrorMerk, "Merk wajib diisi");
                showAlert(Alert.AlertType.WARNING, "Validasi Gagal", "Merk wajib diisi");
                return;
            }
        }

        String sqlProcedure = "{CALL sp_TambahProduk(?, ?, ?, ?, ?)}";
        try {
            if (conn == null || conn.isClosed()) koneksi();

            try (CallableStatement cs = conn.prepareCall(sqlProcedure)) {
                cs.setString(1, txtNamaBarang.getText());
                cs.setString(2, cmbKategoriProduk.getValue());
                cs.setDouble(3, hilangkanFormatRupiah(txtHargaBarang.getText()));

                String stok = txtStockBarang.getText();
                if ("Layanan".equalsIgnoreCase(kategori)) {
                    cs.setString(4, "0");
                } else {
                    cs.setString(4, stok);
                }

                String merk = txtMerk.getText();
                if ("Layanan".equalsIgnoreCase(kategori)) {
                    cs.setString(5, "-");
                } else {
                    cs.setString(5, merk);
                }

                cs.execute();
                showAlert(Alert.AlertType.INFORMATION, "Sukses", "Produk baru berhasil ditambahkan!");
                loadDataProduk();
                handleBatal(null);
            }
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Error Tambah", "Gagal menyimpan data: " + e.getMessage());
        }
    }

    @FXML
    void handleUbahData(ActionEvent event) {
        Produk produkTerpilih = tblProduk.getSelectionModel().getSelectedItem();
        if (produkTerpilih == null) {
            showAlert(Alert.AlertType.WARNING, "Peringatan", "Pilih data produk di tabel yang ingin diubah!");
            return;
        }

        // Validasi input
        if (checkInputErrors()) {
            showAlert(Alert.AlertType.WARNING, "Validasi Gagal", "Mohon perbaiki input yang ditandai merah");
            return;
        }

        String statusLama = produkTerpilih.getStatus();
        String statusBaru = cmbStatus.getValue();

        if (("Tersedia".equalsIgnoreCase(statusLama) || "tersedia".equalsIgnoreCase(statusLama))
                && "NonTersedia".equalsIgnoreCase(statusBaru)) {
            showAlert(Alert.AlertType.WARNING, "Peringatan", "Untuk menonaktifkan produk, silakan pakai tombol 'Hapus Data'!");
            cmbStatus.setValue(statusLama);
            return;
        }

        String sqlProcedure = "{CALL sp_UpdateProduk(?, ?, ?, ?, ?, ?)}";
        try {
            if (conn == null || conn.isClosed()) koneksi();

            try (CallableStatement cs = conn.prepareCall(sqlProcedure)) {
                cs.setString(1, txtIdBarang.getText());
                cs.setString(2, txtNamaBarang.getText());
                cs.setDouble(3, hilangkanFormatRupiah(txtHargaBarang.getText()));

                String kategori = cmbKategoriProduk.getValue();
                String stok = txtStockBarang.getText();
                if ("Layanan".equalsIgnoreCase(kategori)) {
                    cs.setString(4, "0");
                } else {
                    cs.setString(4, stok);
                }

                String merk = txtMerk.getText();
                if ("Layanan".equalsIgnoreCase(kategori)) {
                    cs.setString(5, "-");
                } else {
                    cs.setString(5, merk);
                }

                cs.setString(6, statusBaru);

                cs.execute();
                showAlert(Alert.AlertType.INFORMATION, "Sukses", "Data produk berhasil diperbarui!");
                loadDataProduk();
                handleBatal(null);
            }
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Error Update", "Gagal mengubah data: " + e.getMessage());
        }
    }

    @FXML
    void handleHapusData(ActionEvent event) {
        Produk produkTerpilih = tblProduk.getSelectionModel().getSelectedItem();
        if (produkTerpilih == null) {
            showAlert(Alert.AlertType.WARNING, "Peringatan", "Pilih data produk di tabel!");
            return;
        }

        String sqlProcedure = "{CALL sp_DeleteProdukSoft(?)}";
        try {
            if (conn == null || conn.isClosed()) koneksi();

            try (CallableStatement cs = conn.prepareCall(sqlProcedure)) {
                cs.setString(1, produkTerpilih.getIdProduk());
                cs.execute();

                showAlert(Alert.AlertType.INFORMATION, "Sukses", "Produk berhasil dinonaktifkan!");
                loadDataProduk();
                handleBatal(null);
            }
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Error Delete", "Gagal menonaktifkan produk: " + e.getMessage());
        }
    }

    @FXML
    void handleBatal(ActionEvent event) {
        tblProduk.getSelectionModel().clearSelection();
        txtIdBarang.clear();
        txtNamaBarang.clear();
        txtMerk.clear();
        cmbKategoriProduk.setValue(null);
        txtHargaBarang.clear();
        txtStockBarang.clear();
        cmbStatus.setValue(null);
        cmbStatus.setDisable(true);

        txtStockBarang.setDisable(false);
        txtMerk.setDisable(false);

        btnSimpan.setDisable(false);
        btnUbah.setDisable(true);
        btnHapus.setDisable(true);

        hideAllErrorLabels();

        // Reset style
        txtNamaBarang.setStyle(null);
        txtHargaBarang.setStyle(null);
        txtStockBarang.setStyle(null);
        txtMerk.setStyle(null);
    }

    private void cariDataProduk(String keyword) {
        if (keyword == null || keyword.isEmpty()) {
            filteredList.setAll(listProduk);
        } else {
            filteredList.clear();
            for (Produk p : listProduk) {
                if (p.getNamaProduk().toLowerCase().contains(keyword.toLowerCase()) ||
                        p.getIdProduk().toLowerCase().contains(keyword.toLowerCase()) ||
                        p.getMerk().toLowerCase().contains(keyword.toLowerCase())) {
                    filteredList.add(p);
                }
            }
        }
        currentPage = 1;
        updateTableAndPagination();
    }

    private String formatRupiah(double nilai) {
        NumberFormat nf = NumberFormat.getCurrencyInstance(new Locale("id", "ID"));
        return nf.format(nilai).replaceAll(",00", "");
    }

    private double hilangkanFormatRupiah(String textRupiah) {
        if (textRupiah == null || textRupiah.isEmpty()) return 0;
        String clean = textRupiah.replaceAll("[^0-9]", "");
        return clean.isEmpty() ? 0 : Double.parseDouble(clean);
    }

    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}