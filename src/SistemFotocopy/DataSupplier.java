package SistemFotocopy;

import Database.DBConnection;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.BorderPane;
import java.sql.Statement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class DataSupplier {

    @FXML
    private Button btnBatal;

    @FXML
    private Button btnHapus;

    @FXML
    private Button btnNextPage;

    @FXML
    private Button btnPage1;

    @FXML
    private Button btnPrevPage;

    @FXML
    private Button btnSimpan;

    @FXML
    private Button btnUbah;

    @FXML
    private TableColumn<Supplier, String> colAlamat;

    @FXML
    private TableColumn<Supplier, String> colEmail;

    @FXML
    private TableColumn<Supplier, String> colIdSupplier;

    @FXML
    private TableColumn<Supplier, String> colNamaSupplier;

    @FXML
    private TableColumn<Supplier, String> colNoTelepon;

    @FXML
    private TableColumn<Supplier, String> colStatus;

    @FXML
    private Label lblInfoData;

    @FXML
    private Label lblSupplierAktif;

    @FXML
    private Label lblSupplierNonaktif;

    @FXML
    private Label lblTotalSupplier;

    @FXML
    private BorderPane rootPane;

    @FXML
    private TableView<Supplier> tblSupplier;

    @FXML
    private TextArea txtAlamatLengkap;

    @FXML
    private TextField txtCari;

    @FXML
    private TextField txtEmail;

    @FXML
    private TextField txtIdSupplier;

    @FXML
    private TextField txtNamaSupplier;

    @FXML
    private TextField txtNomorTelepon;

    @FXML
    private Label lblErrorNama;

    @FXML
    private Label lblErrorEmail;

    @FXML
    private Label lblErrorTelepon;

    @FXML
    private Label lblErrorAlamat;

    private DBConnection db = new DBConnection();

    private int currentPage = 1;

    private final int rowsPerPage = 10;

    // tampilan data di table
    private void loadData() {
        ObservableList<Supplier> list = FXCollections.observableArrayList();

        int offset = (currentPage - 1) * rowsPerPage;

        String query = "SELECT * FROM Supplier " +
                "ORDER BY (CASE WHEN Status_Supplier = 'NonAktif' THEN 1 ELSE 0 END) ASC, ID_Supplier ASC " +
                "OFFSET " + offset + " ROWS " +
                "FETCH NEXT " + rowsPerPage + " ROWS ONLY";

        try (Statement st = db.getConnection().createStatement();
             ResultSet rs = st.executeQuery(query)) {

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
            lblInfoData.setText("Menampilkan " + list.size() + " data pada halaman " + currentPage);
        } catch (SQLException e) {
            System.out.println("Gagal load data: " + e);
        }
    }

    // menampilkan pembaruan data di table
    private void updateDashboard() {
        String query = "SELECT " +
                "(SELECT COUNT(*) FROM Supplier) AS Total, " +
                "(SELECT COUNT(*) FROM Supplier WHERE Status_Supplier='aktif') AS Aktif, " +
                "(SELECT COUNT(*) FROM Supplier WHERE Status_Supplier='NonAktif') AS Nonaktif";

        try (Statement st = db.getConnection().createStatement();
             ResultSet rs = st.executeQuery(query)) {

            if (rs.next()) {
                lblTotalSupplier.setText(rs.getString("Total"));
                lblSupplierAktif.setText(rs.getString("Aktif"));
                lblSupplierNonaktif.setText(rs.getString("Nonaktif"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public class Supplier {
        private String id, nama, alamat, telepon, email, status;

        public Supplier(String id, String nama, String alamat, String telepon, String email, String status) {
            this.id = id;
            this.nama = nama;
            this.alamat = alamat;
            this.telepon = telepon;
            this.email = email;
            this.status = status;
        }

        public String getId() { return id; }
        public String getNama() { return nama; }
        public String getAlamat() { return alamat; }
        public String getTelepon() { return telepon; }
        public String getEmail() { return email; }
        public String getStatus() { return status; }
    }

    // id otomatis
    private void generateIdOtomatis() {
        String query = "SELECT TOP 1 ID_Supplier FROM Supplier ORDER BY ID_Supplier DESC";

        try (Statement st = db.getConnection().createStatement();
             ResultSet rs = st.executeQuery(query)) {

            if (rs.next()) {
                String lastId = rs.getString("ID_Supplier");
                int angka = Integer.parseInt(lastId.substring(3));
                int nextAngka = angka + 1;
                txtIdSupplier.setText("SPR" + String.format("%03d", nextAngka));
            } else {
                txtIdSupplier.setText("SPR001");
            }
        } catch (Exception e) {
            txtIdSupplier.setText("SPR001");
            System.out.println("Error saat generate ID: " + e.getMessage());
        }
    }

    @FXML
    public void initialize() {
        // Menghubungkan kolom tabel dengan variabel di class Supplier
        colIdSupplier.setCellValueFactory(new PropertyValueFactory<>("id"));
        colNamaSupplier.setCellValueFactory(new PropertyValueFactory<>("nama"));
        colAlamat.setCellValueFactory(new PropertyValueFactory<>("alamat"));
        colNoTelepon.setCellValueFactory(new PropertyValueFactory<>("telepon"));
        colEmail.setCellValueFactory(new PropertyValueFactory<>("email"));
        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));

        loadData();
        generateIdOtomatis();
        updateDashboard();

        setupInputValidation();

        txtCari.textProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue == null || newValue.isEmpty()) {
                loadData();
            } else {
                searchData(newValue);
            }
        });

        // Listener untuk mengisi form saat baris tabel diklik
        tblSupplier.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null) {
                txtIdSupplier.setText(newValue.getId());
                txtNamaSupplier.setText(newValue.getNama());
                txtAlamatLengkap.setText(newValue.getAlamat());
                txtNomorTelepon.setText(newValue.getTelepon());
                txtEmail.setText(newValue.getEmail());

                btnSimpan.setDisable(true);

            }
        });
    }

    // halaman kanan
    @FXML
    void handleNextPage(ActionEvent event) {
        currentPage++;
        loadData();

        if (tblSupplier.getItems().isEmpty()) {
            currentPage--;
            loadData();
        }
    }

    //halaman kiri
    @FXML
    void handlePrevPage(ActionEvent event) {
        if (currentPage > 1) {
            currentPage--;
            loadData();
        }
    }

    @FXML
    void handleBatal(ActionEvent event) {
        txtIdSupplier.clear();
        txtNamaSupplier.clear();
        txtEmail.clear();
        txtNomorTelepon.clear();
        txtAlamatLengkap.clear();

        btnSimpan.setDisable(false);

        generateIdOtomatis();
        loadData();
        updateDashboard();
    }

    @FXML
    void handleHapusData(ActionEvent event) {
        if (txtIdSupplier.getText().isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Peringatan", "Pilih data yang ingin dihapus terlebih dahulu.");
            return;
        }

        String query = "{call sp_DeleteSupplierSoft(?)}";

        try (java.sql.CallableStatement cs = db.getConnection().prepareCall(query)) {
            cs.setString(1, txtIdSupplier.getText());
            cs.execute();

            loadData();
            updateDashboard();
            handleBatal(event);

            showAlert(Alert.AlertType.INFORMATION, "Sukses", "Data berhasil dinonaktifkan.");
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Gagal", e.getMessage());
        }
    }

    @FXML
    void handleSimpanData(ActionEvent event) {
        // validasi data gaboleh sama
        String queryCek = "SELECT COUNT(*) FROM Supplier WHERE Nama_Supplier = ? OR Email = ? OR No_Telepon = ?";
        try (java.sql.PreparedStatement ps = db.getConnection().prepareStatement(queryCek)) {
            ps.setString(1, txtNamaSupplier.getText().trim());
            ps.setString(2, txtEmail.getText().trim());
            ps.setString(3, txtNomorTelepon.getText().trim());

            ResultSet rs = ps.executeQuery();
            if (rs.next() && rs.getInt(1) > 0) {
                showAlert(Alert.AlertType.WARNING, "Data Duplikat", "Nama, Email, atau No. Telepon sudah terdaftar!");
                return;
            }
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Error", e.getMessage());
            return;
        }

        if (!validasiInput()) return;

        String query = "{call sp_TambahSupplier(?,?,?,?)}";

        try (java.sql.CallableStatement cs = db.getConnection().prepareCall(query)) {
            cs.setString(1, txtNamaSupplier.getText());
            cs.setString(2, txtAlamatLengkap.getText());
            cs.setString(3, txtNomorTelepon.getText());
            cs.setString(4, txtEmail.getText());

            cs.execute();

            loadData();
            handleBatal(event);
            updateDashboard();
            generateIdOtomatis();

            showAlert(Alert.AlertType.INFORMATION, "Sukses", "Data Supplier berhasil disimpan.");

        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Gagal", e.getMessage());
        }
    }

    @FXML
    void handleUbahData(ActionEvent event) {
        // validasi duplikat dengan membawa ID saat ini
        if (isDataDuplicate(txtNamaSupplier.getText().trim(), txtEmail.getText().trim(), txtNomorTelepon.getText().trim(), txtIdSupplier.getText())) {
            showAlert(Alert.AlertType.WARNING, "Data Duplikat", "Data yang Anda masukkan sudah digunakan oleh supplier lain!");
            return;
        }

        if (!validasiInput()) return;

        String query = "{call sp_UpdateSupplier(?,?,?,?,?,?)}";

        try (java.sql.CallableStatement cs = db.getConnection().prepareCall(query)) {
            cs.setString(1, txtIdSupplier.getText().trim());
            cs.setString(2, txtNamaSupplier.getText().trim());
            cs.setString(3, txtAlamatLengkap.getText().trim());
            cs.setString(4, txtNomorTelepon.getText().trim());
            cs.setString(5, txtEmail.getText().trim());

            // Status default ke 'aktif' karena tidak ada ComboBox
            cs.setString(6, "aktif");

            cs.execute();

            loadData();
            handleBatal(event);
            updateDashboard();

            showAlert(Alert.AlertType.INFORMATION, "Sukses", "Data berhasil diubah.");

        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Gagal Ubah", e.getMessage());
        }
    }

    @FXML
    private void searchData(String keyword) {
        ObservableList<Supplier> list = FXCollections.observableArrayList();

        String query = "SELECT * FROM Supplier WHERE " +
                "ID_Supplier LIKE '%" + keyword + "%' OR " +
                "Nama_Supplier LIKE '%" + keyword + "%' OR " +
                "Alamat LIKE '%" + keyword + "%' OR " +
                "Email LIKE '%" + keyword + "%' OR " +
                "No_Telepon LIKE '%" + keyword + "%' OR " +
                "Status_Supplier LIKE '%" + keyword + "%'" +
                "ORDER BY (CASE WHEN Status_Supplier = 'NonAktif' THEN 1 ELSE 0 END) ASC, ID_Supplier ASC";

        try (Statement st = db.getConnection().createStatement();
             ResultSet rs = st.executeQuery(query)) {

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
        } catch (SQLException e) {
            System.out.println("Gagal mencari data: " + e.getMessage());
        }
    }

    // validasi duplicat data
    private boolean isDataDuplicate(String nama, String email, String telp, String currentId) {
        // Jika ada isinya, berarti mode Ubah (cek selain ID yang sedang diedit)
        String query = (currentId == null)
                ? "SELECT COUNT(*) FROM Supplier WHERE Nama_Supplier = ? OR Email = ? OR No_Telepon = ?"
                : "SELECT COUNT(*) FROM Supplier WHERE (Nama_Supplier = ? OR Email = ? OR No_Telepon = ?) AND ID_Supplier != ?";

        try (java.sql.PreparedStatement ps = db.getConnection().prepareStatement(query)) {
            ps.setString(1, nama);
            ps.setString(2, email);
            ps.setString(3, telp);

            if (currentId != null) {
                ps.setString(4, currentId);
            }

            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getInt(1) > 0; // Mengembalikan true jika ada data duplikat
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    // validasi input
    private boolean validasiInput() {
        StringBuilder pesan = new StringBuilder();

        // 1. Validasi Nama
        if (isKosong(txtNamaSupplier.getText())) {
            pesan.append("- Nama supplier wajib diisi.\n");
            showErrorLabel(lblErrorNama, "Wajib diisi");
        } else if (txtNamaSupplier.getText().trim().length() < 4) {
            pesan.append("- Nama supplier minimal 4 karakter.\n");
            showErrorLabel(lblErrorNama, "Min 4 karakter");
        } else if (!txtNamaSupplier.getText().trim().matches("^[a-zA-Z\\s]+$")) {
            pesan.append("- Nama hanya boleh huruf.\n");
            showErrorLabel(lblErrorNama, "Hanya huruf");
        } else {
            hideErrorLabel(lblErrorNama);
        }

        // 2. Validasi Email
        if (isKosong(txtEmail.getText())) {
            pesan.append("- Email wajib diisi.\n");
            showErrorLabel(lblErrorEmail, "Wajib diisi");
        } else if (!txtEmail.getText().trim().matches("^[a-z0-9@._-]+$")) {
            pesan.append("- Format email tidak valid.\n");
            showErrorLabel(lblErrorEmail, "Format salah");
        } else {
            hideErrorLabel(lblErrorEmail);
        }

        // 3. Validasi Telepon
        String telp = txtNomorTelepon.getText().trim();
        if (isKosong(telp)) {
            pesan.append("- Nomor telepon wajib diisi.\n");
            showErrorLabel(lblErrorTelepon, "Wajib diisi");
        } else if (!telp.startsWith("08") || telp.length() < 10 || telp.length() > 13 || !telp.matches("^[0-9]+$")) {
            pesan.append("- Nomor telepon harus diawali 08 dan 10-13 digit angka.\n");
            showErrorLabel(lblErrorTelepon, "Format salah");
        } else {
            hideErrorLabel(lblErrorTelepon);
        }

        // 4. Validasi Alamat
        if (isKosong(txtAlamatLengkap.getText())) {
            pesan.append("- Alamat wajib diisi.\n");
            showErrorLabel(lblErrorAlamat, "Wajib diisi");
        } else {
            hideErrorLabel(lblErrorAlamat);
        }

        // Cek apakah ada pesan error
        if (pesan.length() > 0) {
            showAlert(Alert.AlertType.WARNING, "Data Belum Lengkap", pesan.toString());
            return false;
        }
        return true;
    }

    // validasi input
    private void setupInputValidation() {
        // 1. NAMA LENGKAP - Hanya huruf dan spasi, minimal 4 karakter
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

        // 2. EMAIL - Hanya huruf kecil, angka, @, ., _, -
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

        // 3. NOMOR TELEPON - HARUS diawali 08, hanya angka, min 10 max 13
        TextFormatter<String> telpFormatter = new TextFormatter<>(change -> {
            String newText = change.getControlNewText();

            if (newText.isEmpty()) {
                txtNomorTelepon.setStyle(null);
                return change;
            }

            // Cek panjang maksimal 13
            if (newText.length() > 13) {
                return null;
            }

            // HARUS diawali "08" - cek 2 karakter pertama
            if (newText.length() >= 2) {
                if (!newText.substring(0, 2).equals("08")) {
                    txtNomorTelepon.setStyle("-fx-border-color: red; -fx-border-width: 2px;");
                    return null;
                }
            } else {
                // Jika panjang kurang dari 2, hanya boleh angka '0'
                if (!newText.matches("^0$")) {
                    txtNomorTelepon.setStyle("-fx-border-color: red; -fx-border-width: 2px;");
                    return null;
                }
            }

            // Cek apakah hanya angka
            if (!newText.matches("^[0-9]*$")) {
                txtNomorTelepon.setStyle("-fx-border-color: red; -fx-border-width: 2px;");
                return null;
            }

            // Reset style jika valid
            if (newText.startsWith("08")) {
                txtNomorTelepon.setStyle(null);
            }

            return change;
        });
        txtNomorTelepon.setTextFormatter(telpFormatter);

        // 4. ALAMAT - Hanya huruf, angka, spasi, dan titik
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

    // validasi eror
    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    // untuk cek apakah input kosong
    private boolean isKosong(String text) {
        return text == null || text.trim().isEmpty();
    }

    //untuk menampilkan pesan error di Label
    private void showErrorLabel(Label label, String pesan) {
        if (label != null) {
            label.setText(pesan);
            label.setVisible(true);
            label.setStyle("-fx-text-fill: red; -fx-font-size: 10px;");
        }
    }

    // untuk menyembunyikan label error saat input benar
    private void hideErrorLabel(Label label) {
        if (label != null) {
            label.setVisible(false);
        }
    }
}