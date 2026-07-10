package SistemFotocopy;

import Database.DBConnection;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import java.sql.PreparedStatement;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;

public class DataPegawai {

    // ===================== FORM FIELDS =====================
    @FXML private BorderPane rootPane;

    @FXML private TextField txtIdPegawai;
    @FXML private TextField txtNamaLengkap;
    @FXML private TextField txtEmail;
    @FXML private TextField txtNomorTelepon;
    @FXML private TextArea txtAlamatLengkap;
    @FXML private TextField txtUsername;
    @FXML private PasswordField txtPassword;
    @FXML private TextField txtPasswordVisible;
    @FXML private ComboBox<String> cmbRole;
    @FXML private ComboBox<String> cmbFilterStatus;
    @FXML private Label lblPreviewId;
    @FXML private Button btnTogglePassword;


    @FXML
    void handlePrevPage(ActionEvent event) {
        // TODO: logika pagination halaman sebelumnya
    }

    @FXML
    void handleNextPage(ActionEvent event) {
        // TODO: logika pagination halaman selanjutnya
    }
    // ===================== BUTTONS =====================
    @FXML private Button btnSimpan;
    @FXML private Button btnUbah;
    @FXML private Button btnHapus;
    @FXML private Button btnBatal;
    @FXML private ComboBox<String> cmbStatus;

    // ===================== TABLE & SEARCH =====================
    @FXML private TableView<PegawaiModel> tblPegawai;
    @FXML private TableColumn<PegawaiModel, String> colIdPegawai;
    @FXML private TableColumn<PegawaiModel, String> colNamaPegawai;
    @FXML private TableColumn<PegawaiModel, String> colAlamat;
    @FXML private TableColumn<PegawaiModel, String> colNoTelepon;
    @FXML private TableColumn<PegawaiModel, String> colEmail;
    @FXML private TableColumn<PegawaiModel, String> colUsername;
    @FXML private TableColumn<PegawaiModel, String> colStatus;
    @FXML private TextField txtCari;
    @FXML private Label lblInfoData;
    private enum Mode { TAMBAH, UBAH }
    private Mode mode = Mode.TAMBAH;

    // ===================== PAGINATION (placeholder, belum dipakai) =====================
    @FXML private Button btnPrevPage;
    @FXML private Button btnPage1;
    @FXML private Button btnNextPage;

    // ===================== DASHBOARD CARDS =====================
    @FXML private Label lblTotalPegawai;
    @FXML private Label lblPegawaiAktif;
    @FXML private Label lblPegawaiNonAktif;

    // ===================== STATE =====================
    private final DBConnection dbConnection = new DBConnection();
    private final ObservableList<PegawaiModel> masterData = FXCollections.observableArrayList();
    private FilteredList<PegawaiModel> filteredData;
    private boolean statusTampilPassword = false;

    // =========================================================
    // INITIALIZE
    // =========================================================
    @FXML
    public void initialize() {
        setupTableColumns();
        setupSearchListener();
        setupRowSelectionListener();
        setupFilterStatusListener();
        setupPreviewIdListener();

        cmbStatus.setItems(FXCollections.observableArrayList("aktif", "NonAktif"));
        cmbRole.setItems(FXCollections.observableArrayList("PGW - Pegawai", "ADM - Admin"));
        cmbFilterStatus.setItems(FXCollections.observableArrayList("Semua Status", "aktif", "NonAktif"));
        cmbFilterStatus.setValue("Semua Status");

        tampilkanData();
        hitungDashboard();
        resetForm();
    }

    private void setupTableColumns() {
        colIdPegawai.setCellValueFactory(data -> data.getValue().idPegawaiProperty());
        colNamaPegawai.setCellValueFactory(data -> data.getValue().namaPegawaiProperty());
        colAlamat.setCellValueFactory(data -> data.getValue().alamatProperty());
        colNoTelepon.setCellValueFactory(data -> data.getValue().noTeleponProperty());
        colEmail.setCellValueFactory(data -> data.getValue().emailProperty());
        colUsername.setCellValueFactory(data -> data.getValue().usernameProperty());
        colStatus.setCellValueFactory(data -> data.getValue().statusProperty());

        // Comparator custom: "aktif" selalu tampil di atas "NonAktif" saat sorting ascending
        colStatus.setComparator((s1, s2) -> {
            if (s1.equalsIgnoreCase(s2)) return 0;
            return s1.equalsIgnoreCase("aktif") ? -1 : 1;
        });
    }

    private void setupSearchListener() {
        txtCari.textProperty().addListener((obs, oldVal, newVal) -> terapkanFilter());
    }

    private void setupRowSelectionListener() {
        tblPegawai.getSelectionModel().selectedItemProperty().addListener((obs, oldSel, newSel) -> {
            if (newSel != null) {
                isiFormDariTabel(newSel);
            }
        });
    }

    // =========================================================
    // TAMPILKAN DATA (READ)
    // =========================================================
    private void tampilkanData() {
        masterData.clear();
        String query = "SELECT ID_Pegawai, Nama_Pegawai, Alamat, No_Telepon, Email, Username, Status_Pegawai " +
                "FROM v_TampilSemuaPegawai ORDER BY ID_Pegawai";

        try (ResultSet rs = dbConnection.stat.executeQuery(query)) {
            while (rs.next()) {
                masterData.add(new PegawaiModel(
                        rs.getString("ID_Pegawai"),
                        rs.getString("Nama_Pegawai"),
                        rs.getString("Alamat"),
                        rs.getString("No_Telepon"),
                        rs.getString("Email"),
                        rs.getString("Username"),
                        rs.getString("Status_Pegawai")
                ));
            }
            filteredData = new FilteredList<>(masterData, p -> true);
            tblPegawai.setItems(filteredData);
            updateInfoData();
        } catch (SQLException e) {
            tampilkanAlert(Alert.AlertType.ERROR, "Gagal memuat data", e.getMessage());
        }
    }

    private void updateInfoData() {
        int total = filteredData == null ? 0 : filteredData.size();
        lblInfoData.setText("Menampilkan " + total + " dari " + masterData.size() + " data");
    }

    // =========================================================
    // DASHBOARD CARDS
    // =========================================================
    private void hitungDashboard() {
        String query = "SELECT " +
                "COUNT(*) AS Total, " +
                "SUM(CASE WHEN Status_Pegawai = 'aktif' THEN 1 ELSE 0 END) AS Aktif, " +
                "SUM(CASE WHEN Status_Pegawai = 'NonAktif' THEN 1 ELSE 0 END) AS NonAktif " +
                "FROM Pegawai";

        try (ResultSet rs = dbConnection.stat.executeQuery(query)) {
            if (rs.next()) {
                lblTotalPegawai.setText(String.valueOf(rs.getInt("Total")));
                lblPegawaiAktif.setText(String.valueOf(rs.getInt("Aktif")));
                lblPegawaiNonAktif.setText(String.valueOf(rs.getInt("NonAktif")));
            }
        } catch (SQLException e) {
            tampilkanAlert(Alert.AlertType.ERROR, "Gagal menghitung dashboard", e.getMessage());
        }
    }

    // =========================================================
    // ISI FORM DARI BARIS TABEL YANG DIKLIK
    // =========================================================
    private void isiFormDariTabel(PegawaiModel pegawai) {
        mode = Mode.UBAH;

        txtIdPegawai.setText(pegawai.getIdPegawai());
        txtNamaLengkap.setText(pegawai.getNamaPegawai());
        txtAlamatLengkap.setText(pegawai.getAlamat());
        txtNomorTelepon.setText(pegawai.getNoTelepon());
        txtEmail.setText(pegawai.getEmail());
        txtUsername.setText(pegawai.getUsername());

        cmbRole.setValue(pegawai.getIdPegawai().startsWith("ADM") ? "ADM - Admin" : "PGW - Pegawai");
        cmbRole.setDisable(true);
        lblPreviewId.setText("ID sudah ditetapkan, tidak berubah");

        cmbStatus.setDisable(false);
        cmbStatus.setValue(pegawai.getStatus());

        txtPassword.clear();
        txtPasswordVisible.clear();
        txtPassword.setPromptText("Kosongkan jika tidak ganti password");
        txtPasswordVisible.setPromptText("Kosongkan jika tidak ganti password");

        btnSimpan.setDisable(true);
    }

    // =========================================================
    // SIMPAN DATA (CREATE)
    // =========================================================
    @FXML
    void handleSimpanData(ActionEvent event) {
        if (!validasiInput(true)) return;

        String query = "{call sp_TambahPegawai(?,?,?,?,?,?,?)}";

        try (CallableStatement cs = dbConnection.getConnection().prepareCall(query)) {
            cs.setString(1, txtNamaLengkap.getText().trim());
            cs.setString(2, txtAlamatLengkap.getText().trim());
            cs.setString(3, txtNomorTelepon.getText().trim());
            cs.setString(4, txtEmail.getText().trim());
            cs.setString(5, txtUsername.getText().trim());
            cs.setString(6, getPasswordText());
            cs.setString(7, ambilKodeRole());

            try (ResultSet rs = cs.executeQuery()) {
                if (rs.next()) {
                    String idBaru = rs.getString("ID_Pegawai_Baru");
                    tampilkanAlert(Alert.AlertType.INFORMATION, "Sukses",
                            "Pegawai baru berhasil ditambahkan dengan ID: " + idBaru);
                }
            }

            tampilkanData();
            hitungDashboard();
            resetForm();

        } catch (SQLException e) {
            tampilkanAlert(Alert.AlertType.ERROR, "Gagal menyimpan data", e.getMessage());
        }
    }

    private String ambilKodeRole() {
        String pilihan = cmbRole.getValue();
        if (pilihan != null && pilihan.startsWith("ADM")) {
            return "ADM";
        }
        return "PGW";
    }

    // =========================================================
    // UBAH DATA (UPDATE)
    // =========================================================
    @FXML
    void handleUbahData(ActionEvent event) {
        if (txtIdPegawai.getText() == null || txtIdPegawai.getText().isEmpty()) {
            tampilkanAlert(Alert.AlertType.WARNING, "Peringatan", "Pilih dulu data pegawai dari tabel yang mau diubah!");
            return;
        }
        if (!validasiInput(false)) return; // password tidak wajib saat update

        String query = "{call sp_UpdatePegawai(?,?,?,?,?,?,?,?)}";

        try {
            String passwordFinal = getPasswordText();
            if (isKosong(passwordFinal)) {
                passwordFinal = ambilPasswordLama(txtIdPegawai.getText().trim());
            }

            try (CallableStatement cs = dbConnection.getConnection().prepareCall(query)) {
                cs.setString(1, txtIdPegawai.getText().trim());
                cs.setString(2, txtNamaLengkap.getText().trim());
                cs.setString(3, txtAlamatLengkap.getText().trim());
                cs.setString(4, txtNomorTelepon.getText().trim());
                cs.setString(5, txtEmail.getText().trim());
                cs.setString(6, txtUsername.getText().trim());
                cs.setString(7, passwordFinal);
                cs.setString(8, cmbStatus.getValue() == null ? "aktif" : cmbStatus.getValue());

                cs.execute();
            }

            tampilkanAlert(Alert.AlertType.INFORMATION, "Sukses", "Data pegawai berhasil diubah.");

            tampilkanData();
            hitungDashboard();
            resetForm();

        } catch (SQLException e) {
            tampilkanAlert(Alert.AlertType.ERROR, "Gagal mengubah data", e.getMessage());
        }
    }

    // Ambil password lama langsung dari tabel Pegawai (bukan dari view, karena view sengaja tidak expose password)
    private String ambilPasswordLama(String idPegawai) throws SQLException {
        String query = "SELECT Password FROM Pegawai WHERE ID_Pegawai = ?";
        try (PreparedStatement ps = dbConnection.getConnection().prepareStatement(query)) {
            ps.setString(1, idPegawai);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("Password");
                }
            }
        }
        throw new SQLException("Password lama tidak ditemukan untuk ID " + idPegawai);
    }

    // =========================================================
    // HAPUS DATA (SOFT DELETE)
    // =========================================================
    @FXML
    void handleHapusData(ActionEvent event) {
        if (txtIdPegawai.getText() == null || txtIdPegawai.getText().isEmpty()) {
            tampilkanAlert(Alert.AlertType.WARNING, "Peringatan", "Pilih dulu data pegawai dari tabel yang mau dihapus!");
            return;
        }

        Alert konfirmasi = new Alert(Alert.AlertType.CONFIRMATION);
        konfirmasi.setTitle("Konfirmasi Hapus");
        konfirmasi.setHeaderText(null);
        konfirmasi.setContentText("Yakin mau menonaktifkan pegawai dengan ID " + txtIdPegawai.getText() + " ?");

        konfirmasi.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                eksekusiHapus(txtIdPegawai.getText().trim());
            }
        });
    }

    private void eksekusiHapus(String idPegawai) {
        String query = "{call sp_DeletePegawaiSoft(?)}";

        try (CallableStatement cs = dbConnection.getConnection().prepareCall(query)) {
            cs.setString(1, idPegawai);
            cs.execute();

            tampilkanAlert(Alert.AlertType.INFORMATION, "Sukses", "Pegawai berhasil dinonaktifkan.");

            tampilkanData();
            hitungDashboard();
            resetForm();

        } catch (SQLException e) {
            tampilkanAlert(Alert.AlertType.ERROR, "Gagal menghapus data", e.getMessage());
        }
    }

    // =========================================================
    // BATAL / RESET FORM
    // =========================================================
    @FXML
    void handleBatal(ActionEvent event) {
        resetForm();
    }

    private void resetForm() {
        mode = Mode.TAMBAH;

        txtIdPegawai.clear();
        txtNamaLengkap.clear();
        txtEmail.clear();
        txtNomorTelepon.clear();
        txtAlamatLengkap.clear();
        txtUsername.clear();
        txtPassword.clear();
        txtPasswordVisible.clear();
        txtPassword.setPromptText("Masukan Password...");
        txtPasswordVisible.setPromptText("Masukan Password...");

        cmbRole.setValue("PGW - Pegawai");
        cmbRole.setDisable(false);
        previewIdBaru();

        cmbStatus.setValue("aktif");
        cmbStatus.setDisable(true);

        btnSimpan.setDisable(false);

        tblPegawai.getSelectionModel().clearSelection();
    }

    // =========================================================
    // TOGGLE PASSWORD (mata icon)
    // =========================================================
    @FXML
    void handleTogglePassword(ActionEvent event) {
        statusTampilPassword = !statusTampilPassword;

        if (statusTampilPassword) {
            txtPasswordVisible.setText(txtPassword.getText());
            txtPasswordVisible.setVisible(true);
            txtPasswordVisible.setManaged(true);
            txtPassword.setVisible(false);
            txtPassword.setManaged(false);
        } else {
            txtPassword.setText(txtPasswordVisible.getText());
            txtPassword.setVisible(true);
            txtPassword.setManaged(true);
            txtPasswordVisible.setVisible(false);
            txtPasswordVisible.setManaged(false);
        }
    }

    private String getPasswordText() {
        return statusTampilPassword ? txtPasswordVisible.getText() : txtPassword.getText();
    }

    // =========================================================
    // VALIDASI INPUT
    // =========================================================
    private boolean validasiInput(boolean wajibPassword) {
        StringBuilder pesan = new StringBuilder();

        if (isKosong(txtNamaLengkap.getText())) pesan.append("- Nama lengkap wajib diisi.\n");
        if (isKosong(txtEmail.getText())) pesan.append("- Email wajib diisi.\n");
        if (isKosong(txtNomorTelepon.getText())) pesan.append("- Nomor telepon wajib diisi.\n");
        if (isKosong(txtAlamatLengkap.getText())) pesan.append("- Alamat wajib diisi.\n");
        if (isKosong(txtUsername.getText())) pesan.append("- Username wajib diisi.\n");
        if (wajibPassword && isKosong(getPasswordText())) pesan.append("- Password wajib diisi.\n");

        if (pesan.length() > 0) {
            tampilkanAlert(Alert.AlertType.WARNING, "Data belum lengkap", pesan.toString());
            return false;
        }
        return true;
    }

    private boolean isKosong(String value) {
        return value == null || value.trim().isEmpty();
    }

    // =========================================================
    // HELPER ALERT
    // =========================================================
    private void tampilkanAlert(Alert.AlertType tipe, String judul, String pesan) {
        Alert alert = new Alert(tipe);
        alert.setTitle(judul);
        alert.setHeaderText(null);
        alert.setContentText(pesan);
        alert.showAndWait();
    }

    // LIHAT ID TERBARU
    private void setupFilterStatusListener() {
        cmbFilterStatus.valueProperty().addListener((obs, oldVal, newVal) -> terapkanFilter());
    }

    private void terapkanFilter() {
        if (filteredData == null) return;

        String keyword = txtCari.getText() == null ? "" : txtCari.getText().trim().toLowerCase();
        String statusPilihan = cmbFilterStatus.getValue();

        filteredData.setPredicate(pegawai -> {
            boolean cocokNama = keyword.isEmpty() || pegawai.getNamaPegawai().toLowerCase().contains(keyword);
            boolean cocokStatus = statusPilihan == null
                    || statusPilihan.equals("Semua Status")
                    || pegawai.getStatus().equalsIgnoreCase(statusPilihan);
            return cocokNama && cocokStatus;
        });

        updateInfoData();
    }

    private void setupPreviewIdListener() {
        cmbRole.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (mode == Mode.TAMBAH) {
                previewIdBaru();
            }
        });
    }

    private void previewIdBaru() {
        String kodeRole = ambilKodeRole();
        String query = "SELECT COALESCE(MAX(CAST(SUBSTRING(ID_Pegawai, 4, LEN(ID_Pegawai)) AS INT)), 0) AS MaxUrut " +
                "FROM Pegawai WHERE ID_Pegawai LIKE ?";

        try (PreparedStatement ps = dbConnection.getConnection().prepareStatement(query)) {
            ps.setString(1, kodeRole + "%");
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    int urutBerikutnya = rs.getInt("MaxUrut") + 1;
                    String idPrediksi = kodeRole + String.format("%03d", urutBerikutnya);
                    lblPreviewId.setText("Perkiraan ID: " + idPrediksi);
                }
            }
        } catch (SQLException e) {
            lblPreviewId.setText("Gagal memuat perkiraan ID");
        }
    }

    // =========================================================
    // MODEL DATA PEGAWAI (untuk TableView)
    // =========================================================
    public static class PegawaiModel {
        private final StringProperty idPegawai;
        private final StringProperty namaPegawai;
        private final StringProperty alamat;
        private final StringProperty noTelepon;
        private final StringProperty email;
        private final StringProperty username;
        private final StringProperty status;

        public PegawaiModel(String idPegawai, String namaPegawai, String alamat,
                            String noTelepon, String email, String username, String status) {
            this.idPegawai = new SimpleStringProperty(idPegawai);
            this.namaPegawai = new SimpleStringProperty(namaPegawai);
            this.alamat = new SimpleStringProperty(alamat);
            this.noTelepon = new SimpleStringProperty(noTelepon);
            this.email = new SimpleStringProperty(email);
            this.username = new SimpleStringProperty(username);
            this.status = new SimpleStringProperty(status);
        }

        public String getIdPegawai() { return idPegawai.get(); }
        public StringProperty idPegawaiProperty() { return idPegawai; }

        public String getNamaPegawai() { return namaPegawai.get(); }
        public StringProperty namaPegawaiProperty() { return namaPegawai; }

        public String getAlamat() { return alamat.get(); }
        public StringProperty alamatProperty() { return alamat; }

        public String getNoTelepon() { return noTelepon.get(); }
        public StringProperty noTeleponProperty() { return noTelepon; }

        public String getEmail() { return email.get(); }
        public StringProperty emailProperty() { return email; }

        public String getUsername() { return username.get(); }
        public StringProperty usernameProperty() { return username; }

        public String getStatus() { return status.get(); }
        public StringProperty statusProperty() { return status; }
    }
}