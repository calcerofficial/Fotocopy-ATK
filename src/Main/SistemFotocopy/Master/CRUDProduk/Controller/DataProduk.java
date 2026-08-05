package SistemFotocopy.Master.CRUDProduk.Controller;

import Database.DBConnection;
import SistemFotocopy.Master.CRUDProduk.Dataclass.ProdukModel;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.animation.PauseTransition;
import javafx.util.Duration;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.NumberFormat;
import java.util.Locale;

public class DataProduk {

    // =========================================================
    // HELPER BORDER STYLES
    // =========================================================
    private void resetBorder(Control control) {
        control.setStyle("-fx-border-color: #ced4da; -fx-border-width: 1px; -fx-border-radius: 3px;");
    }
    private void setGreenBorder(Control control) {
        control.setStyle("-fx-border-color: #28a745; -fx-border-width: 2px; -fx-border-radius: 3px;");
    }
    private void setOrangeBorder(Control control) {
        control.setStyle("-fx-border-color: #ffc107; -fx-border-width: 2px; -fx-border-radius: 3px;");
    }
    private void setRedBorder(Control control) {
        control.setStyle("-fx-border-color: #dc3545; -fx-border-width: 2px; -fx-border-radius: 3px;");
    }
    
    @FXML private Button btnBatal;
    @FXML private Button btnHapus;
    @FXML private Button btnNextPage;
    @FXML private Button btnPage1;
    @FXML private Button btnPrevPage;
    @FXML private Button btnSimpan;
    @FXML private Button btnUbah;
    @FXML private Button btnAktifkan;

    @FXML private ComboBox<String> cmbKategoriProduk;

    // ERROR LABELS
    @FXML private Label lblErrorNama;
    @FXML private Label lblErrorHarga;
    @FXML private Label lblErrorStock;
    @FXML private Label lblErrorMerk;

    // STATUS - SEMUA KOMPONEN STATUS
    @FXML private Label lblStatusLabel;
    @FXML private Label lblStatusValue;
    @FXML private Label lblStatusHint;

    @FXML private TableView<ProdukModel> tblProduk;
    @FXML private TableColumn<ProdukModel, String> colIdBarang;
    @FXML private TableColumn<ProdukModel, String> colNamaBarang;
    @FXML private TableColumn<ProdukModel, String> colMerkProduk;
    @FXML private TableColumn<ProdukModel, String> colKategoriProduk;
    @FXML private TableColumn<ProdukModel, String> colHarga;
    @FXML private TableColumn<ProdukModel, String> colStock;
    @FXML private TableColumn<ProdukModel, String> colStatusProduk;

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

    private ObservableList<ProdukModel> listProduk = FXCollections.observableArrayList();
    private ObservableList<ProdukModel> filteredList = FXCollections.observableArrayList();
    private DBConnection db = new DBConnection();

    private int currentPage = 1;
    private final int rowsPerPage = 10;

    private boolean isUpdatingHarga = false;
    private boolean isUpdatingKategori = false;
    private String statusProdukTerpilih = "";

    @FXML
    public void initialize() {
        btnSimpan.setDisable(false);
        btnUbah.setDisable(true);
        btnHapus.setDisable(true);

        txtIdBarang.setDisable(true);

        // SEMUA KOMPONEN STATUS HILANG TOTAL AWALNYA
        hideAllStatusComponents();

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

        setupInputValidation();

        cmbKategoriProduk.getSelectionModel().selectedItemProperty().addListener((options, oldValue, newValue) -> {
            if (isUpdatingKategori) return;

            hideErrorLabel(lblErrorStock);
            hideErrorLabel(lblErrorMerk);
            hideErrorLabel(lblErrorNama);
            hideErrorLabel(lblErrorHarga);

            resetBorder(txtStockBarang);
            resetBorder(txtMerk);
            resetBorder(txtNamaBarang);
            resetBorder(txtHargaBarang);

            updateStockMerkState(newValue);

            if (newValue != null && tblProduk.getSelectionModel().getSelectedItem() == null) {
                generateIdOtomatis();
                hideAllStatusComponents();
            }
        });
        loadDataProduk();

        // LISTENER KLIK TABEL
        tblProduk.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            if (newSelection != null) {
                statusProdukTerpilih = newSelection.getStatus();

                btnSimpan.setDisable(true);

                txtIdBarang.setText(newSelection.getIdProduk());
                txtNamaBarang.setText(newSelection.getNamaProduk());

                isUpdatingKategori = true;
                cmbKategoriProduk.setValue(newSelection.getKategori());
                isUpdatingKategori = false;

                isUpdatingHarga = true;
                txtHargaBarang.setText(formatRupiah(newSelection.getHarga()));
                isUpdatingHarga = false;

                if ("NonTersedia".equalsIgnoreCase(statusProdukTerpilih)) {
                    setAllFieldsDisable(true);
                    showAllStatusComponents("NonTersedia");
                    btnHapus.setDisable(true);
                    btnUbah.setDisable(true);
                    lblInfoData.setText("⚠ Produk NonTersedia - Klik tombol 'Aktifkan' untuk mengubah status.");
                } else {
                    hideAllStatusComponents();
                    btnUbah.setDisable(false);
                    btnHapus.setDisable(false);
                    lblInfoData.setText("");

                    String kategori = newSelection.getKategori();
                    setAllFieldsEditable(true);

                    if ("layanan".equalsIgnoreCase(kategori)) {
                        txtStockBarang.setText("-");
                        txtStockBarang.setDisable(true);
                        txtMerk.setText("-");
                        txtMerk.setDisable(true);
                    } else {
                        txtStockBarang.setText(String.valueOf(newSelection.getStok()));
                        txtStockBarang.setDisable(false);
                        txtMerk.setText(newSelection.getMerk());
                        txtMerk.setDisable(false);
                    }

                    resetBorder(txtStockBarang);
                    resetBorder(txtMerk);
                }

                hideAllErrorLabels();
            } else {
                btnUbah.setDisable(true);
                btnHapus.setDisable(true);
                statusProdukTerpilih = "";
                setAllFieldsEditable(true);
                setAllFieldsDisable(false);
                hideAllStatusComponents();

                if (!lblInfoData.getText().contains("⚠")) {
                    updateInfoData();
                } else {
                    lblInfoData.setText("");
                }
            }
        });

        txtCari.textProperty().addListener((observable, oldValue, newValue) -> {
            cariDataProduk(newValue);
        });
    }

    // =========================================================
    // METHOD UNTUK SET EDITABLE SEMUA FIELD
    // =========================================================
    private void setAllFieldsEditable(boolean editable) {
        txtNamaBarang.setDisable(!editable);
        txtHargaBarang.setDisable(!editable);
        cmbKategoriProduk.setDisable(!editable);
    }

    // =========================================================
    // METHOD UNTUK SET DISABLE SEMUA FIELD
    // =========================================================
    private void setAllFieldsDisable(boolean disable) {
        txtNamaBarang.setDisable(disable);
        txtHargaBarang.setDisable(disable);
        txtStockBarang.setDisable(disable);
        txtMerk.setDisable(disable);
        cmbKategoriProduk.setDisable(true);
        txtIdBarang.setDisable(true);

        if (disable) {
            txtNamaBarang.setStyle("-fx-opacity: 0.6;");
            txtHargaBarang.setStyle("-fx-opacity: 0.6;");
            txtStockBarang.setStyle("-fx-opacity: 0.6;");
            txtMerk.setStyle("-fx-opacity: 0.6;");
        } else {
            resetBorder(txtNamaBarang);
            resetBorder(txtHargaBarang);
            resetBorder(txtStockBarang);
            resetBorder(txtMerk);
        }
    }

    // =========================================================
    // SHOW/HIDE SEMUA KOMPONEN STATUS
    // =========================================================
    private void showAllStatusComponents(String status) {
        lblStatusLabel.setVisible(true);
        lblStatusLabel.setManaged(true);

        lblStatusValue.setVisible(true);
        lblStatusValue.setManaged(true);
        lblStatusValue.setText("⚠ " + status);
        lblStatusValue.setStyle("-fx-text-fill: #ff4444; -fx-font-weight: bold; -fx-font-size: 14px; " +
                "-fx-background-color: #fff0f0; -fx-padding: 4 12; -fx-border-radius: 4; -fx-background-radius: 4;");

        lblStatusHint.setVisible(true);
        lblStatusHint.setManaged(true);

        btnAktifkan.setVisible(true);
        btnAktifkan.setManaged(true);
        btnAktifkan.setDisable(false);
    }

    private void hideAllStatusComponents() {
        lblStatusLabel.setVisible(false);
        lblStatusLabel.setManaged(false);

        lblStatusValue.setVisible(false);
        lblStatusValue.setManaged(false);
        lblStatusValue.setText("");

        lblStatusHint.setVisible(false);
        lblStatusHint.setManaged(false);
        lblStatusHint.setText("");

        btnAktifkan.setVisible(false);
        btnAktifkan.setManaged(false);
        btnAktifkan.setDisable(true);
    }

    // =========================================================
    // UPDATE STOCK & MERK STATE
    // =========================================================
    private void updateStockMerkState(String kategori) {
        if ("Layanan".equalsIgnoreCase(kategori)) {
            txtStockBarang.setText("-");
            txtMerk.setText("-");
            txtStockBarang.setDisable(true);
            txtMerk.setDisable(true);
            txtStockBarang.setStyle(null);
            txtMerk.setStyle(null);
            hideErrorLabel(lblErrorStock);
            hideErrorLabel(lblErrorMerk);
        } else if ("Barang".equalsIgnoreCase(kategori)) {
            if ("-".equals(txtStockBarang.getText())) txtStockBarang.clear();
            if ("-".equals(txtMerk.getText())) txtMerk.clear();

            txtStockBarang.setDisable(false);
            txtMerk.setDisable(false);

            resetBorder(txtStockBarang);
            resetBorder(txtMerk);
        }
    }

    // =========================================================
    // HITUNG STATISTIK
    // =========================================================
    private void hitungStatistikProduk() {
        try {
            String query = "SELECT " +
                    "dbo.f_TotalSemuaProduk() AS Total, " +
                    "dbo.f_TotalProdukTersedia() AS Tersedia, " +
                    "dbo.f_TotalProdukNonTersedia() AS NonTersedia";

            try (java.sql.Statement st = db.getConnection().createStatement();
                 ResultSet rs = st.executeQuery(query)) {
                if (rs.next()) {
                    lblTotalProduk.setText(String.valueOf(rs.getInt("Total")));
                    lblProdukTersedia.setText(String.valueOf(rs.getInt("Tersedia")));
                    lblProdukTidakTersedia.setText(String.valueOf(rs.getInt("NonTersedia")));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // =========================================================
    // VALIDASI Duplikat
    // =========================================================
    private boolean isNamaProdukDuplikat(String nama, String idKecuali) {
        String query = "SELECT COUNT(*) FROM Produk WHERE Nama_Barang = ? AND ID_Produk != ?";
        try (PreparedStatement ps = db.getConnection().prepareStatement(query)) {
            ps.setString(1, nama);
            ps.setString(2, idKecuali);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
        } catch (Exception e) {
            System.out.println("Error cek duplikat: " + e.getMessage());
        }
        return false;
    }

    // =========================================================
    // VALIDASI INPUT
    // =========================================================
    private void setupInputValidation() {
        // 1. NAMA PRODUK - Hanya huruf, angka, spasi, dan minimal 4 karakter
        TextFormatter<String> namaFormatter = new TextFormatter<>(change -> {
            String newText = change.getControlNewText();
            if (newText.isEmpty()) {
                resetBorder(txtNamaBarang);
                return change;
            }
            if (!newText.matches("^[a-zA-Z0-9\\s]*$")) {
                setRedBorder(txtNamaBarang);
                return null;
            }
            // VALIDASI MINIMAL 4 KARAKTER
            if (newText.length() < 4) {
                setOrangeBorder(txtNamaBarang);
            } else {
                setGreenBorder(txtNamaBarang);
            }
            return change;
        });
        txtNamaBarang.setTextFormatter(namaFormatter);

        // LISTENER REAL-TIME VALIDASI NAMA
        txtNamaBarang.textProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null && !newVal.isEmpty()) {
                if (newVal.length() < 4) {
                    setOrangeBorder(txtNamaBarang);
                    showErrorLabel(lblErrorNama, "Nama produk minimal 4 karakter");
                } else if (!newVal.matches("^[a-zA-Z0-9\\s]+$")) {
                    setRedBorder(txtNamaBarang);
                    showErrorLabel(lblErrorNama, "Nama hanya boleh huruf, angka, dan spasi");
                } else {
                    setGreenBorder(txtNamaBarang);
                    hideErrorLabel(lblErrorNama);
                }
            } else {
                resetBorder(txtNamaBarang);
                hideErrorLabel(lblErrorNama);
            }
        });

        // 2. HARGA - (kode tetap sama seperti sebelumnya)
        txtHargaBarang.textProperty().addListener((observable, oldValue, newValue) -> {
            if (isUpdatingHarga) return;

            if (newValue == null || newValue.isEmpty()) {
                resetBorder(txtHargaBarang);
                hideErrorLabel(lblErrorHarga);
                return;
            }

            String cleanString = newValue.replaceAll("[^0-9]", "");

            if (cleanString.isEmpty()) {
                isUpdatingHarga = true;
                txtHargaBarang.setText("");
                isUpdatingHarga = false;
                setRedBorder(txtHargaBarang);
                showErrorLabel(lblErrorHarga, "Harga harus diisi angka");
                return;
            }

            if (cleanString.startsWith("0")) {
                isUpdatingHarga = true;
                txtHargaBarang.setText(oldValue != null ? oldValue : "");
                isUpdatingHarga = false;
                setRedBorder(txtHargaBarang);
                showErrorLabel(lblErrorHarga, "Harga tidak boleh diawali 0");
                return;
            }

            try {
                int value = Integer.parseInt(cleanString);

                if (value < 1000) {
                    setRedBorder(txtHargaBarang);
                    showErrorLabel(lblErrorHarga, "Harga minimal Rp1.000");
                    return;
                }

                if (value > 100000) {
                    setRedBorder(txtHargaBarang);
                    showErrorLabel(lblErrorHarga, "Harga maksimal Rp100.000");
                    return;
                }

                isUpdatingHarga = true;
                String formatted = formatRupiah(value);
                txtHargaBarang.setText(formatted);
                txtHargaBarang.positionCaret(formatted.length());
                isUpdatingHarga = false;

                setGreenBorder(txtHargaBarang);
                hideErrorLabel(lblErrorHarga);

            } catch (NumberFormatException e) {
                setRedBorder(txtHargaBarang);
                showErrorLabel(lblErrorHarga, "Harga harus berupa angka");
            }
        });

        // 3. STOCK - (kode tetap sama seperti sebelumnya)
        txtStockBarang.textProperty().addListener((observable, oldValue, newValue) -> {
            if (txtStockBarang.isDisabled()) {
                resetBorder(txtStockBarang);
                hideErrorLabel(lblErrorStock);
                return;
            }

            if (newValue == null || newValue.isEmpty()) {
                resetBorder(txtStockBarang);
                hideErrorLabel(lblErrorStock);
                return;
            }

            String cleanString = newValue.replaceAll("[^0-9]", "");

            if (cleanString.isEmpty()) {
                txtStockBarang.setText("");
                setRedBorder(txtStockBarang);
                showErrorLabel(lblErrorStock, "Stock harus diisi angka");
                return;
            }

            if (cleanString.startsWith("0")) {
                txtStockBarang.setText(oldValue != null ? oldValue : "");
                setRedBorder(txtStockBarang);
                showErrorLabel(lblErrorStock, "Stock tidak boleh diawali 0");
                return;
            }

            try {
                int value = Integer.parseInt(cleanString);

                if (value < 10) {
                    setRedBorder(txtStockBarang);
                    showErrorLabel(lblErrorStock, "Stock minimal 10");
                    return;
                }

                setGreenBorder(txtStockBarang);
                hideErrorLabel(lblErrorStock);

            } catch (NumberFormatException e) {
                setRedBorder(txtStockBarang);
                showErrorLabel(lblErrorStock, "Stock harus berupa angka");
            }
        });

        // 4. MERK - (kode tetap sama seperti sebelumnya)
        TextFormatter<String> merkFormatter = new TextFormatter<>(change -> {
            if (txtMerk.isDisabled()) {
                resetBorder(txtMerk);
                return change;
            }

            String newText = change.getControlNewText();
            if (newText.isEmpty()) {
                resetBorder(txtMerk);
                return change;
            }
            if (!newText.matches("^[a-zA-Z0-9\\s]*$")) {
                setRedBorder(txtMerk);
                return null;
            }
            setGreenBorder(txtMerk);
            if (lblErrorMerk != null) hideErrorLabel(lblErrorMerk);
            return change;
        });
        txtMerk.setTextFormatter(merkFormatter);
        
        txtMerk.textProperty().addListener((obs, oldVal, newVal) -> {
            if (txtMerk.isDisabled()) return;
            if (newVal != null && !newVal.isEmpty()) {
                if (!newVal.matches("^[a-zA-Z0-9\\s]+$")) {
                    setRedBorder(txtMerk);
                    showErrorLabel(lblErrorMerk, "Merk hanya boleh huruf, angka, dan spasi");
                } else {
                    setGreenBorder(txtMerk);
                    hideErrorLabel(lblErrorMerk);
                }
            } else {
                resetBorder(txtMerk);
                hideErrorLabel(lblErrorMerk);
            }
        });
    }

    // =========================================================
    // CHECK INPUT ERRORS
    // =========================================================
    private boolean checkInputErrors() {
        boolean hasError = false;

        // VALIDASI NAMA PRODUK - MINIMAL 4 KARAKTER
        String nama = txtNamaBarang.getText();
        if (!nama.isEmpty()) {
            if (nama.length() < 4) {
                setOrangeBorder(txtNamaBarang);
                showErrorLabel(lblErrorNama, "Nama produk minimal 4 karakter");
                hasError = true;
            } else if (!nama.matches("^[a-zA-Z0-9\\s]+$")) {
                setRedBorder(txtNamaBarang);
                showErrorLabel(lblErrorNama, "Nama hanya boleh huruf, angka, dan spasi");
                hasError = true;
            } else {
                setGreenBorder(txtNamaBarang);
                hideErrorLabel(lblErrorNama);
            }
        }

        // VALIDASI HARGA
        String hargaText = txtHargaBarang.getText();
        if (!hargaText.isEmpty()) {
            try {
                int harga = Integer.parseInt(hargaText.replaceAll("[^0-9]", ""));
                if (harga < 1000 || harga > 100000) {
                    setRedBorder(txtHargaBarang);
                    showErrorLabel(lblErrorHarga, "Harga harus 1000 - 100000");
                    hasError = true;
                } else {
                    setGreenBorder(txtHargaBarang);
                    hideErrorLabel(lblErrorHarga);
                }
            } catch (NumberFormatException e) {
                setRedBorder(txtHargaBarang);
                showErrorLabel(lblErrorHarga, "Harga harus berupa angka");
                hasError = true;
            }
        }

        // VALIDASI STOCK UNTUK BARANG
        String kategori = cmbKategoriProduk.getValue();
        if ("Barang".equalsIgnoreCase(kategori) && !txtStockBarang.isDisabled()) {
            String stockText = txtStockBarang.getText();
            if (stockText == null || stockText.isEmpty()) {
                setRedBorder(txtStockBarang);
                showErrorLabel(lblErrorStock, "Stock wajib diisi");
                hasError = true;
            } else if (!stockText.equals("-")) {
                try {
                    int stock = Integer.parseInt(stockText);
                    if (stock < 10) {
                        setRedBorder(txtStockBarang);
                        showErrorLabel(lblErrorStock, "Stock minimal 10");
                        hasError = true;
                    } else {
                        setGreenBorder(txtStockBarang);
                        hideErrorLabel(lblErrorStock);
                    }
                } catch (NumberFormatException e) {
                    setRedBorder(txtStockBarang);
                    showErrorLabel(lblErrorStock, "Stock harus berupa angka");
                    hasError = true;
                }
            }
        } else {
            hideErrorLabel(lblErrorStock);
            if (!txtStockBarang.isDisabled()) resetBorder(txtStockBarang);
        }

        // VALIDASI MERK UNTUK BARANG
        if ("Barang".equalsIgnoreCase(kategori) && !txtMerk.isDisabled()) {
            String merk = txtMerk.getText();
            if (merk == null || merk.isEmpty()) {
                setRedBorder(txtMerk);
                showErrorLabel(lblErrorMerk, "Merk wajib diisi");
                hasError = true;
            } else if (!merk.matches("^[a-zA-Z0-9\\s]+$")) {
                setRedBorder(txtMerk);
                showErrorLabel(lblErrorMerk, "Merk hanya boleh huruf, angka, dan spasi");
                hasError = true;
            } else {
                setGreenBorder(txtMerk);
                hideErrorLabel(lblErrorMerk);
            }
        } else {
            hideErrorLabel(lblErrorMerk);
            if (!txtMerk.isDisabled()) resetBorder(txtMerk);
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

    // =========================================================
    // GENERATE ID OTOMATIS
    // =========================================================
    private void generateIdOtomatis() {
        String query = "SELECT MAX(CAST(SUBSTRING(ID_Produk, 4, LEN(ID_Produk)) AS INT)) AS max_angka FROM Produk";
        try {
            try (PreparedStatement ps = db.getConnection().prepareStatement(query);
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

    // =========================================================
    // LOAD DATA PRODUK
    // =========================================================
    private void loadDataProduk() {
        listProduk.clear();
        String query = "SELECT * FROM v_TampilSemuaProduk " +
                "ORDER BY CASE WHEN Status_Barang = 'NonTersedia' THEN 2 ELSE 1 END ASC, ID_Produk ASC";

        try {
            try (PreparedStatement ps = db.getConnection().prepareStatement(query);
                 ResultSet rs = ps.executeQuery()) {

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

                    listProduk.add(new ProdukModel(id, nama, merk, kategori, harga, stok, status));
                }

                filteredList.setAll(listProduk);
                currentPage = 1;
                updateTableAndPagination();
                hitungStatistikProduk();

            }
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Error Database", "Gagal memuat data: " + e.getMessage());
        }
    }

    // =========================================================
    // PAGINATION
    // =========================================================
    private void updateTableAndPagination() {
        int totalRows = filteredList.size();
        int maxPage = (int) Math.ceil((double) totalRows / rowsPerPage);
        if (maxPage == 0) maxPage = 1;

        if (currentPage > maxPage) currentPage = maxPage;
        if (currentPage < 1) currentPage = 1;

        int fromIndex = (currentPage - 1) * rowsPerPage;
        int toIndex = Math.min(fromIndex + rowsPerPage, totalRows);

        ObservableList<ProdukModel> pageItems = FXCollections.observableArrayList();
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

    // =========================================================
    // SIMPAN DATA
    // =========================================================
    @FXML
    void handleSimpanData(ActionEvent event) {
        if (checkInputErrors()) {
            showAlert(Alert.AlertType.WARNING, "Validasi Gagal", "Mohon perbaiki input yang ditandai merah");
            return;
        }

        // ===== VALIDASI NAMA =====
        if (txtNamaBarang.getText().isEmpty()) {
            showErrorLabel(lblErrorNama, "Nama produk wajib diisi");
            showAlert(Alert.AlertType.WARNING, "Validasi Gagal", "Nama produk wajib diisi");
            return;
        }

        if (txtNamaBarang.getText().trim().length() < 4) {
            showErrorLabel(lblErrorNama, "Nama produk minimal 4 karakter");
            showAlert(Alert.AlertType.WARNING, "Validasi Gagal", "Nama produk minimal 4 karakter");
            return;
        }

        // ===== VALIDASI HARGA =====
        if (txtHargaBarang.getText().isEmpty()) {
            showErrorLabel(lblErrorHarga, "Harga wajib diisi");
            showAlert(Alert.AlertType.WARNING, "Validasi Gagal", "Harga wajib diisi");
            return;
        }

        try {
            int harga = Integer.parseInt(txtHargaBarang.getText().replaceAll("[^0-9]", ""));
            if (harga < 1000) {
                showErrorLabel(lblErrorHarga, "Harga minimal Rp1.000");
                showAlert(Alert.AlertType.WARNING, "Validasi Gagal", "Harga minimal Rp1.000");
                return;
            }
            if (harga > 100000) {
                showErrorLabel(lblErrorHarga, "Harga maksimal Rp100.000");
                showAlert(Alert.AlertType.WARNING, "Validasi Gagal", "Harga maksimal Rp100.000");
                return;
            }
        } catch (NumberFormatException e) {
            showErrorLabel(lblErrorHarga, "Harga harus berupa angka");
            showAlert(Alert.AlertType.WARNING, "Validasi Gagal", "Harga harus berupa angka");
            return;
        }

        // ===== VALIDASI KATEGORI =====
        String kategori = cmbKategoriProduk.getValue();
        if (kategori == null) {
            showAlert(Alert.AlertType.WARNING, "Validasi Gagal", "Kategori wajib dipilih");
            return;
        }

        // ===== CEK DUPLIKAT NAMA =====
        if (isNamaProdukDuplikat(txtNamaBarang.getText().trim(), "")) {
            showErrorLabel(lblErrorNama, "Nama produk sudah terdaftar!");
            showAlert(Alert.AlertType.WARNING, "Validasi Gagal", "Nama produk sudah terdaftar.");
            return;
        }

        // ===== VALIDASI KHUSUS BARANG =====
        if ("Barang".equalsIgnoreCase(kategori)) {
            if (txtStockBarang.getText().isEmpty() || txtStockBarang.getText().equals("-")) {
                showErrorLabel(lblErrorStock, "Stock wajib diisi");
                showAlert(Alert.AlertType.WARNING, "Validasi Gagal", "Stock wajib diisi");
                return;
            }

            try {
                int stock = Integer.parseInt(txtStockBarang.getText().trim());
                if (stock < 10) {
                    showErrorLabel(lblErrorStock, "Stock minimal 10");
                    showAlert(Alert.AlertType.WARNING, "Validasi Gagal", "Stock minimal 10");
                    return;
                }
            } catch (NumberFormatException e) {
                showErrorLabel(lblErrorStock, "Stock harus berupa angka");
                showAlert(Alert.AlertType.WARNING, "Validasi Gagal", "Stock harus berupa angka");
                return;
            }

            if (txtMerk.getText().isEmpty() || txtMerk.getText().equals("-")) {
                showErrorLabel(lblErrorMerk, "Merk wajib diisi");
                showAlert(Alert.AlertType.WARNING, "Validasi Gagal", "Merk wajib diisi");
                return;
            }

            if (!txtMerk.getText().matches("^[a-zA-Z0-9\\s]+$")) {
                showErrorLabel(lblErrorMerk, "Merk hanya boleh huruf, angka, dan spasi");
                showAlert(Alert.AlertType.WARNING, "Validasi Gagal", "Merk hanya boleh huruf, angka, dan spasi");
                return;
            }
        }

        // ===== PROSES SIMPAN =====
        String sqlProcedure = "{CALL sp_TambahProduk(?, ?, ?, ?, ?)}";
        try {
            try (CallableStatement cs = db.getConnection().prepareCall(sqlProcedure)) {
                cs.setString(1, txtNamaBarang.getText().trim());
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

    // =========================================================
    // AKTIFKAN DATA
    // =========================================================
    @FXML
    void handleAktifkanData(ActionEvent event) {
        if (txtIdBarang.getText() == null || txtIdBarang.getText().isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Peringatan", "Pilih data produk yang akan diaktifkan!");
            return;
        }

        Alert konfirmasi = new Alert(Alert.AlertType.CONFIRMATION);
        konfirmasi.setTitle("Konfirmasi Aktivasi");
        konfirmasi.setHeaderText(null);
        konfirmasi.setContentText("Yakin ingin mengaktifkan produk dengan ID " + txtIdBarang.getText() +
                " ?\nStatus akan berubah dari NonTersedia menjadi Tersedia.");

        konfirmasi.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                updateStatusProduk(txtIdBarang.getText().trim(), "Tersedia");
            }
        });
    }

    // =========================================================
    // UBAH DATA
    // =========================================================
    @FXML
    void handleUbahData(ActionEvent event) {
        ProdukModel produkTerpilih = tblProduk.getSelectionModel().getSelectedItem();
        if (produkTerpilih == null) {
            showAlert(Alert.AlertType.WARNING, "Peringatan", "Pilih data produk di tabel yang ingin diubah!");
            return;
        }

        String statusSekarang = getStatusDariDatabase(txtIdBarang.getText().trim());
        if ("NonTersedia".equalsIgnoreCase(statusSekarang)) {
            showAlert(Alert.AlertType.WARNING, "Tidak Bisa Ubah",
                    "Produk dengan status NonTersedia tidak dapat diubah.\nGunakan tombol 'Aktifkan' untuk mengubah status.");
            return;
        }

        if (checkInputErrors()) {
            showAlert(Alert.AlertType.WARNING, "Validasi Gagal", "Mohon perbaiki input yang ditandai merah");
            return;
        }

        // ===== VALIDASI NAMA =====
        if (txtNamaBarang.getText().isEmpty()) {
            showErrorLabel(lblErrorNama, "Nama produk wajib diisi");
            showAlert(Alert.AlertType.WARNING, "Validasi Gagal", "Nama produk wajib diisi");
            return;
        }

        if (txtNamaBarang.getText().trim().length() < 4) {
            showErrorLabel(lblErrorNama, "Nama produk minimal 4 karakter");
            showAlert(Alert.AlertType.WARNING, "Validasi Gagal", "Nama produk minimal 4 karakter");
            return;
        }

        // ===== VALIDASI HARGA =====
        if (txtHargaBarang.getText().isEmpty()) {
            showErrorLabel(lblErrorHarga, "Harga wajib diisi");
            showAlert(Alert.AlertType.WARNING, "Validasi Gagal", "Harga wajib diisi");
            return;
        }

        try {
            int harga = Integer.parseInt(txtHargaBarang.getText().replaceAll("[^0-9]", ""));
            if (harga < 1000) {
                showErrorLabel(lblErrorHarga, "Harga minimal Rp1.000");
                showAlert(Alert.AlertType.WARNING, "Validasi Gagal", "Harga minimal Rp1.000");
                return;
            }
            if (harga > 100000) {
                showErrorLabel(lblErrorHarga, "Harga maksimal Rp100.000");
                showAlert(Alert.AlertType.WARNING, "Validasi Gagal", "Harga maksimal Rp100.000");
                return;
            }
        } catch (NumberFormatException e) {
            showErrorLabel(lblErrorHarga, "Harga harus berupa angka");
            showAlert(Alert.AlertType.WARNING, "Validasi Gagal", "Harga harus berupa angka");
            return;
        }

        // ===== CEK DUPLIKAT NAMA (KECUALI DIRI SENDIRI) =====
        if (isNamaProdukDuplikat(txtNamaBarang.getText().trim(), txtIdBarang.getText().trim())) {
            showErrorLabel(lblErrorNama, "Nama produk sudah terdaftar di data lain!");
            showAlert(Alert.AlertType.WARNING, "Validasi Gagal", "Nama produk sudah digunakan oleh produk lain.");
            return;
        }

        // ===== PROSES UPDATE =====
        String sqlProcedure = "{CALL sp_UpdateProduk(?, ?, ?, ?, ?, ?)}";
        try {
            try (CallableStatement cs = db.getConnection().prepareCall(sqlProcedure)) {
                cs.setString(1, txtIdBarang.getText());
                cs.setString(2, txtNamaBarang.getText().trim());
                cs.setString(3, cmbKategoriProduk.getValue());
                cs.setDouble(4, hilangkanFormatRupiah(txtHargaBarang.getText()));

                String kategori = cmbKategoriProduk.getValue();
                if ("Layanan".equalsIgnoreCase(kategori)) {
                    cs.setString(5, "0");
                    cs.setString(6, "-");
                } else {
                    // VALIDASI STOCK
                    if (txtStockBarang.getText().isEmpty() || txtStockBarang.getText().equals("-")) {
                        showErrorLabel(lblErrorStock, "Stock wajib diisi");
                        showAlert(Alert.AlertType.WARNING, "Validasi Gagal", "Stock wajib diisi");
                        return;
                    }

                    try {
                        int stock = Integer.parseInt(txtStockBarang.getText().trim());
                        if (stock < 10) {
                            showErrorLabel(lblErrorStock, "Stock minimal 10");
                            showAlert(Alert.AlertType.WARNING, "Validasi Gagal", "Stock minimal 10");
                            return;
                        }
                    } catch (NumberFormatException e) {
                        showErrorLabel(lblErrorStock, "Stock harus berupa angka");
                        showAlert(Alert.AlertType.WARNING, "Validasi Gagal", "Stock harus berupa angka");
                        return;
                    }

                    if (txtMerk.getText().isEmpty() || txtMerk.getText().equals("-")) {
                        showErrorLabel(lblErrorMerk, "Merk wajib diisi");
                        showAlert(Alert.AlertType.WARNING, "Validasi Gagal", "Merk wajib diisi");
                        return;
                    }

                    cs.setString(5, txtStockBarang.getText().trim());
                    cs.setString(6, txtMerk.getText().trim());
                }

                cs.execute();
                showAlert(Alert.AlertType.INFORMATION, "Sukses", "Data produk berhasil diperbarui!");
                loadDataProduk();
                handleBatal(null);
            }
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Error Update", "Gagal mengubah data: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // =========================================================
    // UPDATE STATUS PRODUK
    // =========================================================
    private void updateStatusProduk(String idProduk, String statusBaru) {
        String query = "UPDATE Produk SET Status_Barang = ? WHERE ID_Produk = ?";

        try (PreparedStatement ps = db.getConnection().prepareStatement(query)) {
            ps.setString(1, statusBaru);
            ps.setString(2, idProduk);
            int rowsAffected = ps.executeUpdate();

            if (rowsAffected > 0) {
                showAlert(Alert.AlertType.INFORMATION, "Sukses",
                        "Produk dengan ID " + idProduk + " berhasil diaktifkan.");
                loadDataProduk();
                hitungStatistikProduk();
                handleBatal(null);
            }
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Gagal mengaktifkan produk", e.getMessage());
        }
    }

    // =========================================================
    // AMBIL STATUS DARI DATABASE
    // =========================================================
    private String getStatusDariDatabase(String idProduk) {
        String query = "SELECT Status_Barang FROM Produk WHERE ID_Produk = ?";
        try (PreparedStatement ps = db.getConnection().prepareStatement(query)) {
            ps.setString(1, idProduk);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("Status_Barang");
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return "";
    }

    // =========================================================
    // HAPUS DATA
    // =========================================================
    @FXML
    void handleHapusData(ActionEvent event) {
        ProdukModel produkTerpilih = tblProduk.getSelectionModel().getSelectedItem();
        if (produkTerpilih == null) {
            showAlert(Alert.AlertType.WARNING, "Peringatan", "Pilih data produk di tabel!");
            return;
        }

        String statusSekarang = getStatusDariDatabase(txtIdBarang.getText().trim());
        if ("NonTersedia".equalsIgnoreCase(statusSekarang)) {
            showAlert(Alert.AlertType.WARNING, "Tidak Bisa Hapus",
                    "Produk dengan status NonTersedia tidak dapat dihapus.");
            return;
        }

        Alert konfirmasi = new Alert(Alert.AlertType.CONFIRMATION);
        konfirmasi.setTitle("Konfirmasi Hapus");
        konfirmasi.setHeaderText(null);
        konfirmasi.setContentText("Yakin mau menonaktifkan produk dengan ID " + produkTerpilih.getIdProduk() + " ?");

        konfirmasi.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                eksekusiHapus(produkTerpilih.getIdProduk());
            }
        });
    }

    private void eksekusiHapus(String idProduk) {
        String sqlProcedure = "{CALL sp_DeleteProdukSoft(?)}";
        try {
            try (CallableStatement cs = db.getConnection().prepareCall(sqlProcedure)) {
                cs.setString(1, idProduk);
                cs.execute();

                showAlert(Alert.AlertType.INFORMATION, "Sukses", "Produk berhasil dinonaktifkan!");
                loadDataProduk();
                hitungStatistikProduk();
                handleBatal(null);
            }
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Error Delete", "Gagal menonaktifkan produk: " + e.getMessage());
        }
    }

    // =========================================================
    // BATAL / RESET FORM
    // =========================================================
    @FXML
    void handleBatal(ActionEvent event) {
        tblProduk.getSelectionModel().clearSelection();
        txtIdBarang.clear();
        txtNamaBarang.clear();
        txtMerk.clear();
        cmbKategoriProduk.getSelectionModel().clearSelection();
        cmbKategoriProduk.setDisable(false);
        txtHargaBarang.clear();
        txtStockBarang.clear();

        hideAllStatusComponents();

        txtStockBarang.setDisable(false);
        txtMerk.setDisable(false);
        txtStockBarang.setStyle(null);
        txtMerk.setStyle(null);

        btnSimpan.setDisable(false);
        btnUbah.setDisable(true);
        btnHapus.setDisable(true);
        statusProdukTerpilih = "";

        hideAllErrorLabels();

        resetBorder(txtNamaBarang);
        resetBorder(txtHargaBarang);
        resetBorder(txtStockBarang);
        resetBorder(txtMerk);

        if (!lblInfoData.getText().contains("⚠")) {
            updateInfoData();
        } else {
            lblInfoData.setText("");
        }
    }

    private void updateInfoData() {
        int totalRows = filteredList.size();
        if (totalRows == 0) {
            lblInfoData.setText("Menampilkan 0 dari 0 data");
        } else {
            int fromIndex = (currentPage - 1) * rowsPerPage;
            int toIndex = Math.min(fromIndex + rowsPerPage, totalRows);
            lblInfoData.setText("Menampilkan " + (fromIndex + 1) + "-" + toIndex + " dari " + totalRows + " data");
        }
    }

    // =========================================================
    // CARI DATA
    // =========================================================
    private void cariDataProduk(String keyword) {
        if (keyword == null || keyword.isEmpty()) {
            filteredList.setAll(listProduk);
        } else {
            filteredList.clear();
            for (ProdukModel p : listProduk) {
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

    // =========================================================
    // HELPER FORMAT RUPIAH
    // =========================================================
    private String formatRupiah(double nilai) {
        NumberFormat nf = NumberFormat.getCurrencyInstance(new Locale("id", "ID"));
        return nf.format(nilai).replaceAll(",00", "");
    }

    private double hilangkanFormatRupiah(String textRupiah) {
        if (textRupiah == null || textRupiah.isEmpty()) return 0;
        String clean = textRupiah.replaceAll("[^0-9]", "");
        return clean.isEmpty() ? 0 : Double.parseDouble(clean);
    }

    // =========================================================
    // HELPER ALERT
    // =========================================================
    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}