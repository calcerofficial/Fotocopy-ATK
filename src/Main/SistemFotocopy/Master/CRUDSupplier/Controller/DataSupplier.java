package SistemFotocopy.Master.CRUDSupplier.Controller;

import Database.DBConnection;
import SistemFotocopy.Master.CRUDSupplier.Dataclass.SupplierModel;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class DataSupplier {

    @FXML private Button btnBatal;
    @FXML private Button btnHapus;
    @FXML private Button btnNextPage;
    @FXML private Button btnPage1;
    @FXML private Button btnPrevPage;
    @FXML private Button btnSimpan;
    @FXML private Button btnUbah;
    @FXML private Button btnAktifkan;

    @FXML private TableColumn<SupplierModel, String> colAlamat;
    @FXML private TableColumn<SupplierModel, String> colEmail;
    @FXML private TableColumn<SupplierModel, String> colIdSupplier;
    @FXML private TableColumn<SupplierModel, String> colNamaSupplier;
    @FXML private TableColumn<SupplierModel, String> colNoTelepon;
    @FXML private TableColumn<SupplierModel, String> colStatus;

    @FXML private Label lblInfoData;
    @FXML private Label lblSupplierAktif;
    @FXML private Label lblSupplierNonaktif;
    @FXML private Label lblTotalSupplier;
    @FXML private BorderPane rootPane;

    @FXML private TableView<SupplierModel> tblSupplier;
    @FXML private TextArea txtAlamatLengkap;
    @FXML private TextField txtCari;
    @FXML private TextField txtEmail;
    @FXML private TextField txtIdSupplier;
    @FXML private TextField txtNamaSupplier;
    @FXML private TextField txtNomorTelepon;
    @FXML private TextField txtStatus;

    // ===== TAMBAHKAN LABEL STATUS =====
    @FXML private Label lblStatus;

    @FXML private Label lblErrorNama;
    @FXML private Label lblErrorEmail;
    @FXML private Label lblErrorTelepon;
    @FXML private Label lblErrorAlamat;
    @FXML private Label lblInfoEmail;

    private DBConnection db = new DBConnection();
    private int currentPage = 1;
    private final int rowsPerPage = 10;
    private ObservableList<SupplierModel> masterData = FXCollections.observableArrayList();
    private String currentStatus = "";

    // =========================================================
    // VALIDASI EMAIL - DOMAIN YANG DIIZINKAN
    // =========================================================
    private boolean isValidEmail(String email) {
        if (isKosong(email)) return false;

        email = email.trim().toLowerCase();

        // Domain yang diizinkan
        String[] allowedDomains = {
                "@gmail.com",
                "@yahoo.com",
                "@outlook.com",
                "@icloud.com",
                "@ac.id",
                "@edu"
        };

        // Cek domain standar
        for (String domain : allowedDomains) {
            if (email.endsWith(domain)) {
                String prefix = email.substring(0, email.length() - domain.length());
                return !prefix.isEmpty() && prefix.matches("^[a-z0-9._-]+$");
            }
        }

        // Cek domain student.(kampus).ac.id
        if (email.matches("^[a-z0-9._-]+@student\\.[a-z]+\\.ac\\.id$")) {
            return true;
        }

        return false;
    }

    // =========================================================
    // INITIALIZE
    // =========================================================
    @FXML
    public void initialize() {
        btnUbah.setDisable(true);
        btnHapus.setDisable(true);

        // ===== STATUS & TOMBOL AKTIFKAN DEFAULT SEMBUNYI =====
        txtStatus.setVisible(false);
        txtStatus.setManaged(false);
        lblStatus.setVisible(false);
        lblStatus.setManaged(false);
        btnAktifkan.setVisible(false);
        btnAktifkan.setManaged(false);

        // ===== INFO EMAIL =====
        lblInfoEmail.setText("Format: @gmail.com, @yahoo.com, @outlook.com, @icloud.com, @ac.id, @edu, @student.(kampus).ac.id");
        lblInfoEmail.setStyle("-fx-text-fill: #666; -fx-font-size: 10px; -fx-font-style: italic;");
        lblInfoEmail.setVisible(true);
        lblInfoEmail.setManaged(true);

        colIdSupplier.setCellValueFactory(cellData -> cellData.getValue().idProperty());
        colNamaSupplier.setCellValueFactory(cellData -> cellData.getValue().namaProperty());
        colAlamat.setCellValueFactory(cellData -> cellData.getValue().alamatProperty());
        colNoTelepon.setCellValueFactory(cellData -> cellData.getValue().teleponProperty());
        colEmail.setCellValueFactory(cellData -> cellData.getValue().emailProperty());
        colStatus.setCellValueFactory(cellData -> cellData.getValue().statusProperty());

        setupInputValidation();
        loadData();
        generateIdOtomatis();
        hitungStatistikSupplier();

        // Search
        txtCari.textProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal == null || newVal.isEmpty()) {
                loadData();
            } else {
                cariSupplier(newVal);
            }
        });

        // Row selection
        tblSupplier.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                txtIdSupplier.setText(newVal.getId());
                txtNamaSupplier.setText(newVal.getNama());
                txtAlamatLengkap.setText(newVal.getAlamat());
                txtNomorTelepon.setText(newVal.getTelepon());
                txtEmail.setText(newVal.getEmail());

                currentStatus = newVal.getStatus();

                btnSimpan.setDisable(true);
                btnUbah.setDisable(false);
                btnHapus.setDisable(false);

                // ===== CEK STATUS =====
                boolean isNonAktif = "NonAktif".equalsIgnoreCase(currentStatus);

                if (isNonAktif) {
                    // Disable semua input field
                    setAllFieldsDisable(true);

                    // Tampilkan status dan tombol aktifkan
                    txtStatus.setVisible(true);
                    txtStatus.setManaged(true);
                    lblStatus.setVisible(true);
                    lblStatus.setManaged(true);
                    btnAktifkan.setVisible(true);
                    btnAktifkan.setManaged(true);
                    btnAktifkan.setDisable(false);

                    txtStatus.setText(currentStatus);

                    // Button Ubah dan Hapus di-disable
                    btnUbah.setDisable(true);
                    btnHapus.setDisable(true);

                    // Simpan tetap disable
                    btnSimpan.setDisable(true);

                    // Tampilkan pesan info
                    lblInfoData.setText("⚠ Supplier NonAktif - Tidak dapat diedit. Klik tombol 'Aktifkan' untuk mengubah status.");

                } else {
                    // Jika Aktif, enable semua field
                    setAllFieldsDisable(false);

                    // Sembunyikan komponen status
                    txtStatus.setVisible(false);
                    txtStatus.setManaged(false);
                    lblStatus.setVisible(false);
                    lblStatus.setManaged(false);
                    btnAktifkan.setVisible(false);
                    btnAktifkan.setManaged(false);
                    txtStatus.clear();

                    // Enable button Ubah dan Hapus
                    btnUbah.setDisable(false);
                    btnHapus.setDisable(false);

                    // Reset info
                    lblInfoData.setText("Menampilkan " + masterData.size() + " data pada halaman " + currentPage);
                }

                hideAllErrorLabels();
                resetStyle();
            } else {
                // Jika tidak ada data dipilih
                setAllFieldsDisable(false);

                // Sembunyikan semua
                txtStatus.setVisible(false);
                txtStatus.setManaged(false);
                lblStatus.setVisible(false);
                lblStatus.setManaged(false);
                btnAktifkan.setVisible(false);
                btnAktifkan.setManaged(false);
                txtStatus.clear();

                btnUbah.setDisable(true);
                btnHapus.setDisable(true);
                btnSimpan.setDisable(false);

                // Reset info
                lblInfoData.setText("Menampilkan " + masterData.size() + " data pada halaman " + currentPage);
            }
        });
    }

    // =========================================================
    // METHOD UNTUK SET DISABLE SEMUA FIELD
    // =========================================================
    private void setAllFieldsDisable(boolean disable) {
        txtNamaSupplier.setDisable(disable);
        txtEmail.setDisable(disable);
        txtNomorTelepon.setDisable(disable);
        txtAlamatLengkap.setDisable(disable);

        // ID selalu disable
        txtIdSupplier.setDisable(true);

        // Style untuk menunjukkan field disabled
        if (disable) {
            txtNamaSupplier.setStyle("-fx-opacity: 0.6; -fx-border-color: #cccccc;");
            txtEmail.setStyle("-fx-opacity: 0.6; -fx-border-color: #cccccc;");
            txtNomorTelepon.setStyle("-fx-opacity: 0.6; -fx-border-color: #cccccc;");
            txtAlamatLengkap.setStyle("-fx-opacity: 0.6; -fx-border-color: #cccccc;");
        } else {
            txtNamaSupplier.setStyle(null);
            txtEmail.setStyle(null);
            txtNomorTelepon.setStyle(null);
            txtAlamatLengkap.setStyle(null);
        }
    }

    // =========================================================
    // LOAD DATA
    // =========================================================
    private void loadData() {
        masterData.clear();
        int offset = (currentPage - 1) * rowsPerPage;

        String query = "SELECT ID_Supplier, Nama_Supplier, Alamat, No_Telepon, Email, Status_Supplier " +
                "FROM v_TampilSemuaSupplier " +
                "ORDER BY CASE WHEN Status_Supplier = 'NonAktif' THEN 1 ELSE 0 END, ID_Supplier " +
                "OFFSET " + offset + " ROWS FETCH NEXT " + rowsPerPage + " ROWS ONLY";

        try (java.sql.Statement st = db.getConnection().createStatement();
             ResultSet rs = st.executeQuery(query)) {

            while (rs.next()) {
                masterData.add(new SupplierModel(
                        rs.getString("ID_Supplier"),
                        rs.getString("Nama_Supplier"),
                        rs.getString("Alamat"),
                        rs.getString("No_Telepon"),
                        rs.getString("Email"),
                        rs.getString("Status_Supplier")
                ));
            }
            tblSupplier.setItems(masterData);
            lblInfoData.setText("Menampilkan " + masterData.size() + " data pada halaman " + currentPage);
        } catch (SQLException e) {
            System.out.println("Gagal load data: " + e);
        }
    }

    // =========================================================
    // HITUNG STATISTIK
    // =========================================================
    private void hitungStatistikSupplier() {
        try {
            String query = "SELECT " +
                    "dbo.f_TotalSemuaSupplier() AS Total, " +
                    "dbo.f_TotalSupplierAktif() AS Aktif, " +
                    "dbo.f_TotalSupplierNonAktif() AS NonAktif";

            try (java.sql.Statement st = db.getConnection().createStatement();
                 ResultSet rs = st.executeQuery(query)) {
                if (rs.next()) {
                    lblTotalSupplier.setText(String.valueOf(rs.getInt("Total")));
                    lblSupplierAktif.setText(String.valueOf(rs.getInt("Aktif")));
                    lblSupplierNonaktif.setText(String.valueOf(rs.getInt("NonAktif")));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // =========================================================
    // CARI SUPPLIER
    // =========================================================
    private void cariSupplier(String keyword) {
        ObservableList<SupplierModel> list = FXCollections.observableArrayList();
        String query = "SELECT ID_Supplier, Nama_Supplier, Alamat, No_Telepon, Email, Status_Supplier " +
                "FROM dbo.f_CariSupplier(?)";

        try (PreparedStatement ps = db.getConnection().prepareStatement(query)) {
            ps.setString(1, keyword);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(new SupplierModel(
                            rs.getString("ID_Supplier"),
                            rs.getString("Nama_Supplier"),
                            rs.getString("Alamat"),
                            rs.getString("No_Telepon"),
                            rs.getString("Email"),
                            rs.getString("Status_Supplier")
                    ));
                }
                tblSupplier.setItems(list);
            }
        } catch (SQLException e) {
            System.out.println("Gagal mencari data: " + e.getMessage());
        }
    }

    // =========================================================
    // GET STATUS DARI DATABASE
    // =========================================================
    private String getStatusDariDatabase(String idSupplier) {
        String query = "SELECT Status_Supplier FROM Supplier WHERE ID_Supplier = ?";
        try (PreparedStatement ps = db.getConnection().prepareStatement(query)) {
            ps.setString(1, idSupplier);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("Status_Supplier");
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return "aktif";
    }

    // =========================================================
    // GENERATE ID OTOMATIS
    // =========================================================
    private void generateIdOtomatis() {
        String query = "SELECT TOP 1 ID_Supplier FROM Supplier ORDER BY ID_Supplier DESC";
        try (java.sql.Statement st = db.getConnection().createStatement();
             ResultSet rs = st.executeQuery(query)) {
            if (rs.next()) {
                String lastId = rs.getString("ID_Supplier");
                int angka = Integer.parseInt(lastId.substring(3));
                txtIdSupplier.setText("SPR" + String.format("%03d", angka + 1));
            } else {
                txtIdSupplier.setText("SPR001");
            }
        } catch (Exception e) {
            txtIdSupplier.setText("SPR001");
        }
    }

    // =========================================================
    // VALIDASI INPUT
    // =========================================================
    private void setupInputValidation() {
        TextFormatter<String> namaFormatter = new TextFormatter<>(change -> {
            String newText = change.getControlNewText();
            if (newText.isEmpty()) {
                txtNamaSupplier.setStyle(null);
                return change;
            }
            if (!newText.matches("^[a-zA-Z\\s]*$")) {
                txtNamaSupplier.setStyle("-fx-border-color: red; -fx-border-width: 2px;");
                return null;
            }
            txtNamaSupplier.setStyle(null);
            return change;
        });
        txtNamaSupplier.setTextFormatter(namaFormatter);

        TextFormatter<String> emailFormatter = new TextFormatter<>(change -> {
            String newText = change.getControlNewText();
            if (newText.isEmpty()) {
                txtEmail.setStyle(null);
                return change;
            }
            if (!newText.matches("^[a-z0-9@._-]*$")) {
                txtEmail.setStyle("-fx-border-color: red; -fx-border-width: 2px;");
                return null;
            }
            txtEmail.setStyle(null);
            return change;
        });
        txtEmail.setTextFormatter(emailFormatter);

        TextFormatter<String> telpFormatter = new TextFormatter<>(change -> {
            String newText = change.getControlNewText();
            if (newText.isEmpty()) {
                txtNomorTelepon.setStyle(null);
                return change;
            }
            if (newText.length() > 13) return null;
            if (newText.length() >= 2 && !newText.substring(0, 2).equals("08")) {
                txtNomorTelepon.setStyle("-fx-border-color: red; -fx-border-width: 2px;");
                return null;
            }
            if (!newText.matches("^[0-9]*$")) {
                txtNomorTelepon.setStyle("-fx-border-color: red; -fx-border-width: 2px;");
                return null;
            }
            if (newText.startsWith("08")) txtNomorTelepon.setStyle(null);
            return change;
        });
        txtNomorTelepon.setTextFormatter(telpFormatter);

        TextFormatter<String> alamatFormatter = new TextFormatter<>(change -> {
            String newText = change.getControlNewText();
            if (newText.isEmpty()) {
                txtAlamatLengkap.setStyle(null);
                return change;
            }
            if (!newText.matches("^[a-zA-Z0-9\\s.]*$")) {
                txtAlamatLengkap.setStyle("-fx-border-color: red; -fx-border-width: 2px;");
                return null;
            }
            txtAlamatLengkap.setStyle(null);
            return change;
        });
        txtAlamatLengkap.setTextFormatter(alamatFormatter);
    }

    // =========================================================
    // CEK DUPLIKAT
    // =========================================================
    private boolean isDataDuplicate(String nama, String email, String telp, String currentId) {
        String query;
        if (currentId == null || currentId.isEmpty()) {
            query = "SELECT COUNT(*) FROM Supplier WHERE Nama_Supplier = ? OR Email = ? OR No_Telepon = ?";
        } else {
            query = "SELECT COUNT(*) FROM Supplier WHERE (Nama_Supplier = ? OR Email = ? OR No_Telepon = ?) AND ID_Supplier != ?";
        }

        try (PreparedStatement ps = db.getConnection().prepareStatement(query)) {
            ps.setString(1, nama != null ? nama : "");
            ps.setString(2, email != null ? email : "");
            ps.setString(3, telp != null ? telp : "");

            if (currentId != null && !currentId.isEmpty()) {
                ps.setString(4, currentId);
            }

            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    // =========================================================
    // VALIDASI INPUT - FINAL
    // =========================================================
    private boolean validasiInput() {
        hideAllErrorLabels();
        resetStyle();

        StringBuilder pesan = new StringBuilder();
        String currentId = txtIdSupplier.getText().trim();

        if (isKosong(txtNamaSupplier.getText())) {
            pesan.append("- Nama supplier wajib diisi.\n");
            showErrorLabel(lblErrorNama, "Wajib diisi");
            txtNamaSupplier.setStyle("-fx-border-color: red; -fx-border-width: 2px;");
        } else if (txtNamaSupplier.getText().trim().length() < 4) {
            pesan.append("- Nama supplier minimal 4 karakter.\n");
            showErrorLabel(lblErrorNama, "Min 4 karakter");
            txtNamaSupplier.setStyle("-fx-border-color: red; -fx-border-width: 2px;");
        } else if (!txtNamaSupplier.getText().trim().matches("^[a-zA-Z\\s]+$")) {
            pesan.append("- Nama hanya boleh huruf dan spasi.\n");
            showErrorLabel(lblErrorNama, "Hanya huruf dan spasi");
            txtNamaSupplier.setStyle("-fx-border-color: red; -fx-border-width: 2px;");
        } else if (isDataDuplicate(txtNamaSupplier.getText().trim(), null, null, currentId)) {
            pesan.append("- Nama supplier sudah terdaftar.\n");
            showErrorLabel(lblErrorNama, "Nama sudah terdaftar");
            txtNamaSupplier.setStyle("-fx-border-color: red; -fx-border-width: 2px;");
        }

        // Validasi Email dengan domain yang diizinkan
        if (isKosong(txtEmail.getText())) {
            pesan.append("- Email wajib diisi.\n");
            showErrorLabel(lblErrorEmail, "Wajib diisi");
            txtEmail.setStyle("-fx-border-color: red; -fx-border-width: 2px;");
        } else if (!isValidEmail(txtEmail.getText().trim())) {
            pesan.append("- Format email tidak valid. Gunakan domain: @gmail.com, @yahoo.com, @outlook.com, @icloud.com, @ac.id, @edu, @student.(kampus).ac.id\n");
            showErrorLabel(lblErrorEmail, "Format email tidak valid");
            txtEmail.setStyle("-fx-border-color: red; -fx-border-width: 2px;");
        } else if (isDataDuplicate(null, txtEmail.getText().trim(), null, currentId)) {
            pesan.append("- Email sudah digunakan.\n");
            showErrorLabel(lblErrorEmail, "Email sudah digunakan");
            txtEmail.setStyle("-fx-border-color: red; -fx-border-width: 2px;");
        }

        String telp = txtNomorTelepon.getText().trim();
        if (isKosong(telp)) {
            pesan.append("- Nomor telepon wajib diisi.\n");
            showErrorLabel(lblErrorTelepon, "Wajib diisi");
            txtNomorTelepon.setStyle("-fx-border-color: red; -fx-border-width: 2px;");
        } else if (!telp.startsWith("08")) {
            pesan.append("- Nomor telepon harus diawali 08.\n");
            showErrorLabel(lblErrorTelepon, "Harus diawali 08");
            txtNomorTelepon.setStyle("-fx-border-color: red; -fx-border-width: 2px;");
        } else if (telp.length() < 10 || telp.length() > 13) {
            pesan.append("- Nomor telepon harus 10-13 digit.\n");
            showErrorLabel(lblErrorTelepon, "10-13 digit");
            txtNomorTelepon.setStyle("-fx-border-color: red; -fx-border-width: 2px;");
        } else if (!telp.matches("^[0-9]+$")) {
            pesan.append("- Nomor telepon hanya boleh angka.\n");
            showErrorLabel(lblErrorTelepon, "Hanya angka");
            txtNomorTelepon.setStyle("-fx-border-color: red; -fx-border-width: 2px;");
        } else if (isDataDuplicate(null, null, telp, currentId)) {
            pesan.append("- Nomor telepon sudah terdaftar.\n");
            showErrorLabel(lblErrorTelepon, "Nomor sudah terdaftar");
            txtNomorTelepon.setStyle("-fx-border-color: red; -fx-border-width: 2px;");
        }

        if (isKosong(txtAlamatLengkap.getText())) {
            pesan.append("- Alamat wajib diisi.\n");
            showErrorLabel(lblErrorAlamat, "Wajib diisi");
            txtAlamatLengkap.setStyle("-fx-border-color: red; -fx-border-width: 2px;");
        } else if (!txtAlamatLengkap.getText().trim().matches("^[a-zA-Z0-9\\s.]+$")) {
            pesan.append("- Alamat hanya boleh huruf, angka, spasi, dan titik.\n");
            showErrorLabel(lblErrorAlamat, "Alamat tidak valid");
            txtAlamatLengkap.setStyle("-fx-border-color: red; -fx-border-width: 2px;");
        }

        if (pesan.length() > 0) {
            showAlert(Alert.AlertType.WARNING, "Data Belum Lengkap", pesan.toString());
            return false;
        }
        return true;
    }

    private boolean isKosong(String text) {
        return text == null || text.trim().isEmpty();
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

    private void hideAllErrorLabels() {
        hideErrorLabel(lblErrorNama);
        hideErrorLabel(lblErrorEmail);
        hideErrorLabel(lblErrorTelepon);
        hideErrorLabel(lblErrorAlamat);
    }

    private void resetStyle() {
        txtNamaSupplier.setStyle(null);
        txtEmail.setStyle(null);
        txtNomorTelepon.setStyle(null);
        txtAlamatLengkap.setStyle(null);
    }

    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    // =========================================================
    // HANDLE BUTTONS
    // =========================================================

    @FXML
    void handleBatal(ActionEvent event) {
        txtIdSupplier.clear();
        txtNamaSupplier.clear();
        txtEmail.clear();
        txtNomorTelepon.clear();
        txtAlamatLengkap.clear();
        txtStatus.clear();

        btnSimpan.setDisable(false);
        btnUbah.setDisable(true);
        btnHapus.setDisable(true);

        // ===== SEMBUNYIKAN STATUS DAN TOMBOL AKTIFKAN =====
        txtStatus.setVisible(false);
        txtStatus.setManaged(false);
        lblStatus.setVisible(false);
        lblStatus.setManaged(false);
        btnAktifkan.setVisible(false);
        btnAktifkan.setManaged(false);

        // ===== ENABLE KEMBALI SEMUA FIELD =====
        setAllFieldsDisable(false);

        currentStatus = "";
        generateIdOtomatis();
        loadData();
        hitungStatistikSupplier();
        hideAllErrorLabels();
        resetStyle();

        // Reset info
        lblInfoData.setText("Menampilkan " + masterData.size() + " data pada halaman " + currentPage);
    }

    @FXML
    void handleSimpanData(ActionEvent event) {
        if (!validasiInput()) return;

        String query = "{call sp_TambahSupplier(?,?,?,?)}";
        try (CallableStatement cs = db.getConnection().prepareCall(query)) {
            cs.setString(1, txtNamaSupplier.getText().trim());
            cs.setString(2, txtAlamatLengkap.getText().trim());
            cs.setString(3, txtNomorTelepon.getText().trim());
            cs.setString(4, txtEmail.getText().trim());
            cs.execute();
            showAlert(Alert.AlertType.INFORMATION, "Sukses", "Data Supplier berhasil disimpan.");
            handleBatal(null);
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Gagal", e.getMessage());
        }
    }

    @FXML
    void handleUbahData(ActionEvent event) {
        if (txtIdSupplier.getText().isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Peringatan", "Pilih data yang ingin diubah terlebih dahulu.");
            return;
        }

        String idSupplier = txtIdSupplier.getText().trim();
        String statusSekarang = getStatusDariDatabase(idSupplier);

        if ("NonAktif".equalsIgnoreCase(statusSekarang)) {
            showAlert(Alert.AlertType.WARNING, "Tidak Bisa Update",
                    "Supplier dengan status NonAktif tidak dapat diubah. " +
                            "Silakan aktifkan terlebih dahulu menggunakan tombol Aktifkan.");
            return;
        }

        if (!validasiInput()) return;

        String query = "{call sp_UpdateSupplier(?,?,?,?,?,?)}";
        try (CallableStatement cs = db.getConnection().prepareCall(query)) {
            cs.setString(1, txtIdSupplier.getText().trim());
            cs.setString(2, txtNamaSupplier.getText().trim());
            cs.setString(3, txtAlamatLengkap.getText().trim());
            cs.setString(4, txtNomorTelepon.getText().trim());
            cs.setString(5, txtEmail.getText().trim());
            cs.setString(6, statusSekarang);
            cs.execute();
            showAlert(Alert.AlertType.INFORMATION, "Sukses", "Data berhasil diubah.");
            handleBatal(null);
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Gagal Ubah", e.getMessage());
        }
    }

    @FXML
    void handleHapusData(ActionEvent event) {
        if (txtIdSupplier.getText().isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Peringatan", "Pilih data yang ingin dihapus terlebih dahulu.");
            return;
        }

        // CEK STATUS SEBELUM HAPUS
        String idSupplier = txtIdSupplier.getText().trim();
        String statusSekarang = getStatusDariDatabase(idSupplier);

        if ("NonAktif".equalsIgnoreCase(statusSekarang)) {
            showAlert(Alert.AlertType.WARNING, "Tidak Bisa Hapus",
                    "Supplier dengan status NonAktif tidak dapat dihapus.");
            return;
        }

        Alert konfirmasi = new Alert(Alert.AlertType.CONFIRMATION);
        konfirmasi.setTitle("Konfirmasi Hapus");
        konfirmasi.setHeaderText(null);
        konfirmasi.setContentText("Yakin mau menonaktifkan supplier dengan ID " + txtIdSupplier.getText() + " ?");

        konfirmasi.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                eksekusiHapus(txtIdSupplier.getText().trim());
            }
        });
    }

    private void eksekusiHapus(String idSupplier) {
        String query = "{call sp_DeleteSupplierSoft(?)}";
        try (CallableStatement cs = db.getConnection().prepareCall(query)) {
            cs.setString(1, idSupplier);
            cs.execute();
            showAlert(Alert.AlertType.INFORMATION, "Sukses", "Supplier berhasil dinonaktifkan.");
            handleBatal(null);
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Gagal", e.getMessage());
        }
    }

    @FXML
    void handleAktifkanData(ActionEvent event) {
        if (txtIdSupplier.getText().isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Peringatan", "Pilih data yang ingin diaktifkan terlebih dahulu.");
            return;
        }

        String idSupplier = txtIdSupplier.getText().trim();
        String statusSekarang = getStatusDariDatabase(idSupplier);

        if ("aktif".equalsIgnoreCase(statusSekarang)) {
            showAlert(Alert.AlertType.INFORMATION, "Info", "Supplier sudah dalam status Aktif.");
            return;
        }

        Alert konfirmasi = new Alert(Alert.AlertType.CONFIRMATION);
        konfirmasi.setTitle("Konfirmasi Aktifkan");
        konfirmasi.setHeaderText(null);
        konfirmasi.setContentText("Yakin mau mengaktifkan supplier dengan ID " + idSupplier + " ?");

        konfirmasi.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                eksekusiAktifkan(idSupplier);
            }
        });
    }

    private void eksekusiAktifkan(String idSupplier) {
        String query = "UPDATE Supplier SET Status_Supplier = 'aktif' WHERE ID_Supplier = ?";
        try (PreparedStatement ps = db.getConnection().prepareStatement(query)) {
            ps.setString(1, idSupplier);
            int rows = ps.executeUpdate();

            if (rows > 0) {
                showAlert(Alert.AlertType.INFORMATION, "Sukses", "Supplier berhasil diaktifkan.");
                handleBatal(null);
            } else {
                showAlert(Alert.AlertType.WARNING, "Gagal", "Supplier tidak ditemukan.");
            }
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Gagal Aktifkan", e.getMessage());
        }
    }

    @FXML
    void handleNextPage(ActionEvent event) {
        currentPage++;
        loadData();
        if (tblSupplier.getItems().isEmpty()) {
            currentPage--;
            loadData();
        }
    }

    @FXML
    void handlePrevPage(ActionEvent event) {
        if (currentPage > 1) {
            currentPage--;
            loadData();
        }
    }
}