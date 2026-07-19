package SistemFotocopy;

import Database.DBConnection;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
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

    @FXML private TableColumn<Supplier, String> colAlamat;
    @FXML private TableColumn<Supplier, String> colEmail;
    @FXML private TableColumn<Supplier, String> colIdSupplier;
    @FXML private TableColumn<Supplier, String> colNamaSupplier;
    @FXML private TableColumn<Supplier, String> colNoTelepon;
    @FXML private TableColumn<Supplier, String> colStatus;

    @FXML private Label lblInfoData;
    @FXML private Label lblSupplierAktif;
    @FXML private Label lblSupplierNonaktif;
    @FXML private Label lblTotalSupplier;
    @FXML private BorderPane rootPane;

    @FXML private TableView<Supplier> tblSupplier;
    @FXML private TextArea txtAlamatLengkap;
    @FXML private TextField txtCari;
    @FXML private TextField txtEmail;
    @FXML private TextField txtIdSupplier;
    @FXML private TextField txtNamaSupplier;
    @FXML private TextField txtNomorTelepon;

    @FXML private Label lblErrorNama;
    @FXML private Label lblErrorEmail;
    @FXML private Label lblErrorTelepon;
    @FXML private Label lblErrorAlamat;
    @FXML private Label lblInfoEmail;

    private DBConnection db = new DBConnection();
    private int currentPage = 1;
    private final int rowsPerPage = 10;
    private ObservableList<Supplier> masterData = FXCollections.observableArrayList();

    // =========================================================
    // MODEL SUPPLIER
    // =========================================================
    public static class Supplier {
        private final StringProperty id;
        private final StringProperty nama;
        private final StringProperty alamat;
        private final StringProperty telepon;
        private final StringProperty email;
        private final StringProperty status;

        public Supplier(String id, String nama, String alamat, String telepon, String email, String status) {
            this.id = new SimpleStringProperty(id);
            this.nama = new SimpleStringProperty(nama);
            this.alamat = new SimpleStringProperty(alamat);
            this.telepon = new SimpleStringProperty(telepon);
            this.email = new SimpleStringProperty(email);
            this.status = new SimpleStringProperty(status);
        }

        public StringProperty idProperty() { return id; }
        public StringProperty namaProperty() { return nama; }
        public StringProperty alamatProperty() { return alamat; }
        public StringProperty teleponProperty() { return telepon; }
        public StringProperty emailProperty() { return email; }
        public StringProperty statusProperty() { return status; }

        public String getId() { return id.get(); }
        public String getNama() { return nama.get(); }
        public String getAlamat() { return alamat.get(); }
        public String getTelepon() { return telepon.get(); }
        public String getEmail() { return email.get(); }
        public String getStatus() { return status.get(); }
    }

    // =========================================================
    // INITIALIZE
    // =========================================================
    @FXML
    public void initialize() {
        btnUbah.setDisable(true);
        btnHapus.setDisable(true);

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

        // Auto-fill email
        txtEmail.textProperty().addListener((obs, oldVal, newVal) -> {
            lblInfoEmail.setVisible(!newVal.contains("@") && !newVal.isEmpty());
        });

        txtEmail.focusedProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal) {
                String email = txtEmail.getText().trim();
                if (!email.isEmpty() && !email.contains("@")) {
                    txtEmail.setText(email + "@gmail.com");
                    lblInfoEmail.setVisible(false);
                }
            }
        });

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

                btnSimpan.setDisable(true);
                btnUbah.setDisable(false);
                btnHapus.setDisable(false);

                // Reset error labels saat pilih data baru
                hideAllErrorLabels();
                resetStyle();
            }
        });
    }

    // =========================================================
    // LOAD DATA - PAKAI VIEW ✅
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
                masterData.add(new Supplier(
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
    // HITUNG STATISTIK - PAKAI UDF ✅
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
    // CARI SUPPLIER - PAKAI UDF ✅
    // =========================================================
    private void cariSupplier(String keyword) {
        ObservableList<Supplier> list = FXCollections.observableArrayList();
        String query = "SELECT ID_Supplier, Nama_Supplier, Alamat, No_Telepon, Email, Status_Supplier " +
                "FROM dbo.f_CariSupplier(?)";

        try (PreparedStatement ps = db.getConnection().prepareStatement(query)) {
            ps.setString(1, keyword);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(new Supplier(
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
        // RESET semua error label dan style TERLEBIH DAHULU
        hideAllErrorLabels();
        resetStyle();

        StringBuilder pesan = new StringBuilder();
        String currentId = txtIdSupplier.getText().trim();

        // Nama
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

        // Email
        if (isKosong(txtEmail.getText())) {
            pesan.append("- Email wajib diisi.\n");
            showErrorLabel(lblErrorEmail, "Wajib diisi");
            txtEmail.setStyle("-fx-border-color: red; -fx-border-width: 2px;");
        } else if (!txtEmail.getText().trim().matches("^[a-z0-9@._-]+$")) {
            pesan.append("- Format email tidak valid.\n");
            showErrorLabel(lblErrorEmail, "Format salah");
            txtEmail.setStyle("-fx-border-color: red; -fx-border-width: 2px;");
        } else if (isDataDuplicate(null, txtEmail.getText().trim(), null, currentId)) {
            pesan.append("- Email sudah digunakan.\n");
            showErrorLabel(lblErrorEmail, "Email sudah digunakan");
            txtEmail.setStyle("-fx-border-color: red; -fx-border-width: 2px;");
        }

        // Telepon
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

        // Alamat
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
        lblInfoEmail.setVisible(false);
        txtNomorTelepon.clear();
        txtAlamatLengkap.clear();
        btnSimpan.setDisable(false);
        btnUbah.setDisable(true);
        btnHapus.setDisable(true);
        generateIdOtomatis();
        loadData();
        hitungStatistikSupplier();

        hideAllErrorLabels();
        resetStyle();
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

        if (!validasiInput()) return;

        String query = "{call sp_UpdateSupplier(?,?,?,?,?,?)}";
        try (CallableStatement cs = db.getConnection().prepareCall(query)) {
            cs.setString(1, txtIdSupplier.getText().trim());
            cs.setString(2, txtNamaSupplier.getText().trim());
            cs.setString(3, txtAlamatLengkap.getText().trim());
            cs.setString(4, txtNomorTelepon.getText().trim());
            cs.setString(5, txtEmail.getText().trim());
            cs.setString(6, "aktif");
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

        String query = "{call sp_DeleteSupplierSoft(?)}";
        try (CallableStatement cs = db.getConnection().prepareCall(query)) {
            cs.setString(1, txtIdSupplier.getText());
            cs.execute();
            showAlert(Alert.AlertType.INFORMATION, "Sukses", "Data berhasil dinonaktifkan.");
            handleBatal(null);
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Gagal", e.getMessage());
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