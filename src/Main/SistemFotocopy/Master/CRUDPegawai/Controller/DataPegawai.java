package SistemFotocopy.Master.CRUDPegawai.Controller;

import Database.DBConnection;
import SistemFotocopy.Master.CRUDPegawai.Dataclass.PegawaiModel;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Comparator;

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
    @FXML private Button btnTogglePassword;

    // ===================== KONFIRMASI PASSWORD =====================
    @FXML private PasswordField txtKonfirmasiPassword;
    @FXML private TextField txtKonfirmasiPasswordVisible;
    @FXML private Button btnToggleKonfirmasiPassword;
    @FXML private Label lblErrorKonfirmasiPassword;

    // ===================== STATUS - SEMUA KOMPONEN =====================
    @FXML private Label lblStatusLabel;
    @FXML private Label lblStatusValue;
    @FXML private Button btnAktifkan;
    @FXML private Label lblStatusHint;

    // ===================== COMBOBOX ROLE =====================
    @FXML private ComboBox<String> cmbRole;

    // ===================== ERROR LABELS =====================
    @FXML private Label lblErrorNama;
    @FXML private Label lblErrorEmail;
    @FXML private Label lblErrorTelepon;
    @FXML private Label lblErrorAlamat;
    @FXML private Label lblErrorUsername;
    @FXML private Label lblErrorPassword;

    // ===================== BUTTONS =====================
    @FXML private Button btnSimpan;
    @FXML private Button btnUbah;
    @FXML private Button btnHapus;
    @FXML private Button btnBatal;

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
    @FXML private Label lblInfoEmail;

    private enum Mode { TAMBAH, UBAH }
    private Mode mode = Mode.TAMBAH;

    // VARIABEL UNTUK MENYIMPAN STATUS PEGAWAI YANG SEDANG DIPILIH
    private String statusPegawaiTerpilih = "";

    // ===================== PAGINATION =====================
    @FXML private Button btnPrevPage;
    @FXML private Button btnPage1;
    @FXML private Button btnNextPage;

    // Pagination variables
    private int currentPage = 1;
    private final int ITEMS_PER_PAGE = 10;
    private int totalPages = 1;

    // ===================== DASHBOARD CARDS =====================
    @FXML private Label lblTotalPegawai;
    @FXML private Label lblPegawaiAktif;
    @FXML private Label lblPegawaiNonAktif;

    // ===================== STATE =====================
    private final DBConnection dbConnection = new DBConnection();
    private final ObservableList<PegawaiModel> masterData = FXCollections.observableArrayList();
    private FilteredList<PegawaiModel> filteredData;
    private boolean statusTampilPassword = false;
    private boolean statusTampilKonfirmasiPassword = false;

    // =========================================================
    // INITIALIZE
    // =========================================================
    @FXML
    public void initialize() {
        lblInfoEmail.setText("Format: @gmail.com, @yahoo.com, @outlook.com, @icloud.com, @ac.id, @edu, @student.(kampus).ac.id");
        lblInfoEmail.setStyle("-fx-text-fill: #666; -fx-font-size: 10px; -fx-font-style: italic;");
        lblInfoEmail.setVisible(true);
        lblInfoEmail.setManaged(true);

        setupTableColumns();
        setupSearchListener();
        setupRowSelectionListener();
        setupButtonListeners();
        setupRoleComboBox();
        setupInputValidation();
        tampilkanData();
        hitungDashboard();
        resetForm();
        updatePaginationButtons();

        btnUbah.setDisable(true);
        btnHapus.setDisable(true);
        btnAktifkan.setVisible(false);
        btnAktifkan.setManaged(false);

        setupStatusSorting();
        hideAllStatusComponents();
    }

    // =========================================================
    // INPUT VALIDATION SETUP
    // =========================================================
    private void setupInputValidation() {
        // 1. NAMA LENGKAP - Hanya huruf dan spasi, minimal 4 karakter
        TextFormatter<String> namaFormatter = new TextFormatter<>(change -> {
            String newText = change.getControlNewText();
            if (newText.isEmpty()) {
                txtNamaLengkap.setStyle(null);
                return change;
            }
            if (!newText.matches("^[a-zA-Z\\s]*$")) {
                txtNamaLengkap.setStyle("-fx-border-color: red; -fx-border-width: 2px;");
                return null;
            }
            txtNamaLengkap.setStyle(null);
            return change;
        });
        txtNamaLengkap.setTextFormatter(namaFormatter);

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
            if (newText.length() > 13) {
                return null;
            }
            if (newText.length() >= 2) {
                if (!newText.substring(0, 2).equals("08")) {
                    txtNomorTelepon.setStyle("-fx-border-color: red; -fx-border-width: 2px;");
                    return null;
                }
            } else {
                if (!newText.matches("^0$")) {
                    txtNomorTelepon.setStyle("-fx-border-color: red; -fx-border-width: 2px;");
                    return null;
                }
            }
            if (!newText.matches("^[0-9]*$")) {
                txtNomorTelepon.setStyle("-fx-border-color: red; -fx-border-width: 2px;");
                return null;
            }
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

        // 5. USERNAME - HANYA HURUF, minimal 4 max 10
        TextFormatter<String> usernameFormatter = new TextFormatter<>(change -> {
            String newText = change.getControlNewText();
            if (newText.isEmpty()) {
                txtUsername.setStyle(null);
                return change;
            }
            if (newText.length() > 10) {
                return null;
            }
            if (!newText.matches("^[a-zA-Z]*$")) {
                txtUsername.setStyle("-fx-border-color: red; -fx-border-width: 2px;");
                return null;
            }
            txtUsername.setStyle(null);
            return change;
        });
        txtUsername.setTextFormatter(usernameFormatter);
    }

    // =========================================================
    // CHECK INPUT ERRORS
    // =========================================================
    private boolean checkInputErrors() {
        boolean hasError = false;

        String nama = txtNamaLengkap.getText();
        if (!nama.isEmpty() && nama.length() < 4) {
            txtNamaLengkap.setStyle("-fx-border-color: red; -fx-border-width: 2px;");
            showErrorLabel(lblErrorNama, "Nama lengkap minimal 4 karakter");
            hasError = true;
        } else if (!nama.isEmpty() && !nama.matches("^[a-zA-Z\\s]+$")) {
            txtNamaLengkap.setStyle("-fx-border-color: red; -fx-border-width: 2px;");
            showErrorLabel(lblErrorNama, "Nama hanya boleh berisi huruf dan spasi");
            hasError = true;
        } else {
            txtNamaLengkap.setStyle(null);
            hideErrorLabel(lblErrorNama);
        }

        String email = txtEmail.getText();
        if (!email.isEmpty() && !isValidEmail(email)) {
            txtEmail.setStyle("-fx-border-color: red; -fx-border-width: 2px;");
            showErrorLabel(lblErrorEmail, "Format email tidak valid. Gunakan domain yang diizinkan");
            hasError = true;
        } else {
            txtEmail.setStyle(null);
            hideErrorLabel(lblErrorEmail);
        }

        String telp = txtNomorTelepon.getText();
        if (!telp.isEmpty()) {
            if (!telp.startsWith("08")) {
                txtNomorTelepon.setStyle("-fx-border-color: red; -fx-border-width: 2px;");
                showErrorLabel(lblErrorTelepon, "Nomor telepon HARUS diawali 08");
                hasError = true;
            } else if (telp.length() < 10 || telp.length() > 13) {
                txtNomorTelepon.setStyle("-fx-border-color: red; -fx-border-width: 2px;");
                showErrorLabel(lblErrorTelepon, "Nomor telepon harus 10-13 digit");
                hasError = true;
            } else if (!telp.matches("^[0-9]+$")) {
                txtNomorTelepon.setStyle("-fx-border-color: red; -fx-border-width: 2px;");
                showErrorLabel(lblErrorTelepon, "Nomor telepon hanya boleh berisi angka");
                hasError = true;
            } else {
                txtNomorTelepon.setStyle(null);
                hideErrorLabel(lblErrorTelepon);
            }
        }

        String alamat = txtAlamatLengkap.getText();
        if (!alamat.isEmpty() && !alamat.matches("^[a-zA-Z0-9\\s.]+$")) {
            txtAlamatLengkap.setStyle("-fx-border-color: red; -fx-border-width: 2px;");
            showErrorLabel(lblErrorAlamat, "Alamat hanya boleh huruf, angka, spasi, dan titik");
            hasError = true;
        } else {
            txtAlamatLengkap.setStyle(null);
            hideErrorLabel(lblErrorAlamat);
        }

        String username = txtUsername.getText();
        if (!username.isEmpty()) {
            if (username.length() < 4 || username.length() > 10) {
                txtUsername.setStyle("-fx-border-color: red; -fx-border-width: 2px;");
                showErrorLabel(lblErrorUsername, "Username harus 4-10 karakter");
                hasError = true;
            } else if (!username.matches("^[a-zA-Z]+$")) {
                txtUsername.setStyle("-fx-border-color: red; -fx-border-width: 2px;");
                showErrorLabel(lblErrorUsername, "Username HANYA boleh huruf (tanpa angka/simbol)");
                hasError = true;
            } else {
                txtUsername.setStyle(null);
                hideErrorLabel(lblErrorUsername);
            }
        }

        return hasError;
    }

    // =========================================================
    // VALIDASI EMAIL - DOMAIN YANG DIIZINKAN
    // =========================================================
    private boolean isValidEmail(String email) {
        if (isKosong(email)) return false;

        email = email.trim().toLowerCase();

        String[] allowedDomains = {
                "@gmail.com",
                "@yahoo.com",
                "@outlook.com",
                "@icloud.com",
                "@ac.id",
                "@edu"
        };

        for (String domain : allowedDomains) {
            if (email.endsWith(domain)) {
                String prefix = email.substring(0, email.length() - domain.length());
                return !prefix.isEmpty() && prefix.matches("^[a-z0-9._-]+$");
            }
        }

        return email.matches("^[a-z0-9._-]+@student\\.[a-z]+\\.ac\\.id$");
    }

    // =========================================================
    // SETUP ROLE COMBOBOX
    // =========================================================
    private void setupRoleComboBox() {
        cmbRole.setItems(FXCollections.observableArrayList("Pegawai", "Admin"));
        cmbRole.setValue("Pegawai");

        cmbRole.setOnAction(event -> {
            if (mode == Mode.TAMBAH) {
                generateIdOtomatis();
            }
        });
    }

    private void generateIdOtomatis() {
        String role = cmbRole.getValue();
        String prefix = "";

        if ("Admin".equals(role)) {
            prefix = "ADM";
        } else if ("Pegawai".equals(role)) {
            prefix = "PGW";
        } else {
            return;
        }

        try {
            String query = "SELECT COALESCE(MAX(CAST(SUBSTRING(ID_Pegawai, 4, LEN(ID_Pegawai)) AS INT)), 0) AS MaxID " +
                    "FROM Pegawai WHERE ID_Pegawai LIKE ?";

            try (PreparedStatement ps = dbConnection.getConnection().prepareStatement(query)) {
                ps.setString(1, prefix + "%");
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        int maxId = rs.getInt("MaxID");
                        int newId = maxId + 1;
                        String idBaru = prefix + String.format("%03d", newId);
                        txtIdPegawai.setText(idBaru);
                    }
                }
            }
        } catch (SQLException e) {
            if ("Admin".equals(role)) {
                txtIdPegawai.setText("ADM001");
            } else {
                txtIdPegawai.setText("PGW001");
            }
            tampilkanAlert(Alert.AlertType.WARNING, "Peringatan",
                    "Gagal generate ID otomatis: " + e.getMessage() + "\nMenggunakan ID default.");
        }
    }

    // =========================================================
    // TABLE SETUP METHODS
    // =========================================================
    private void setupTableColumns() {
        colIdPegawai.setCellValueFactory(data -> data.getValue().idPegawaiProperty());
        colNamaPegawai.setCellValueFactory(data -> data.getValue().namaPegawaiProperty());
        colAlamat.setCellValueFactory(data -> data.getValue().alamatProperty());
        colNoTelepon.setCellValueFactory(data -> data.getValue().noTeleponProperty());
        colEmail.setCellValueFactory(data -> data.getValue().emailProperty());
        colUsername.setCellValueFactory(data -> data.getValue().usernameProperty());
        colStatus.setCellValueFactory(data -> data.getValue().statusProperty());
    }

    private void setupSearchListener() {
        txtCari.textProperty().addListener((obs, oldVal, newVal) -> {
            if (filteredData == null) return;
            String keyword = newVal == null ? "" : newVal.trim().toLowerCase();
            filteredData.setPredicate(pegawai ->
                    keyword.isEmpty() || pegawai.getNamaPegawai().toLowerCase().contains(keyword)
            );
            currentPage = 1;
            updatePaginationData();
            updateInfoData();
        });
    }

    // =========================================================
    // SETUP ROW SELECTION
    // =========================================================
    private void setupRowSelectionListener() {
        tblPegawai.getSelectionModel().selectedItemProperty().addListener((obs, oldSel, newSel) -> {
            if (newSel != null) {
                statusPegawaiTerpilih = newSel.getStatus();
                isiFormDariTabel(newSel);

                if ("NonAktif".equalsIgnoreCase(statusPegawaiTerpilih)) {
                    setAllFieldsDisable(true);
                    showAllStatusComponents("NonAktif");
                    btnUbah.setDisable(false);
                    btnHapus.setDisable(true);
                    lblInfoData.setText("⚠ Pegawai NonAktif - Klik tombol 'Aktifkan' untuk mengubah status.");
                } else {
                    setAllFieldsDisable(false);
                    hideAllStatusComponents();
                    btnUbah.setDisable(false);
                    btnHapus.setDisable(false);
                    lblInfoData.setText("");
                }
            } else {
                btnUbah.setDisable(true);
                btnHapus.setDisable(true);
                statusPegawaiTerpilih = "";
                setAllFieldsDisable(false);
                hideAllStatusComponents();
            }
        });
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
        lblStatusValue.setStyle("-fx-text-fill: #ff4444; -fx-font-weight: bold; -fx-font-size: 14px; -fx-background-color: #fff0f0; -fx-padding: 4 12; -fx-border-radius: 4; -fx-background-radius: 4;");

        btnAktifkan.setVisible(true);
        btnAktifkan.setManaged(true);
        btnAktifkan.setDisable(false);

        lblStatusHint.setVisible(true);
        lblStatusHint.setManaged(true);
    }

    private void hideAllStatusComponents() {
        lblStatusLabel.setVisible(false);
        lblStatusLabel.setManaged(false);

        lblStatusValue.setVisible(false);
        lblStatusValue.setManaged(false);
        lblStatusValue.setText("");

        btnAktifkan.setVisible(false);
        btnAktifkan.setManaged(false);
        btnAktifkan.setDisable(true);

        lblStatusHint.setVisible(false);
        lblStatusHint.setManaged(false);
        lblStatusHint.setText("");
    }

    // =========================================================
    // METHOD UNTUK SET DISABLE SEMUA FIELD
    // =========================================================
    private void setAllFieldsDisable(boolean disable) {
        txtNamaLengkap.setDisable(disable);
        txtEmail.setDisable(disable);
        txtNomorTelepon.setDisable(disable);
        txtUsername.setDisable(disable);
        txtAlamatLengkap.setDisable(disable);
        txtPassword.setDisable(disable);
        txtPasswordVisible.setDisable(disable);
        txtKonfirmasiPassword.setDisable(disable);
        txtKonfirmasiPasswordVisible.setDisable(disable);
        btnTogglePassword.setDisable(disable);
        btnToggleKonfirmasiPassword.setDisable(disable);

        if (mode == Mode.UBAH) {
            cmbRole.setDisable(true);
        } else {
            cmbRole.setDisable(false);
        }

        txtIdPegawai.setDisable(true);

        if (disable) {
            txtNamaLengkap.setStyle("-fx-opacity: 0.6;");
            txtEmail.setStyle("-fx-opacity: 0.6;");
            txtNomorTelepon.setStyle("-fx-opacity: 0.6;");
            txtUsername.setStyle("-fx-opacity: 0.6;");
            txtAlamatLengkap.setStyle("-fx-opacity: 0.6;");
            txtPassword.setStyle("-fx-opacity: 0.6;");
            txtPasswordVisible.setStyle("-fx-opacity: 0.6;");
            txtKonfirmasiPassword.setStyle("-fx-opacity: 0.6;");
            txtKonfirmasiPasswordVisible.setStyle("-fx-opacity: 0.6;");
        } else {
            txtNamaLengkap.setStyle(null);
            txtEmail.setStyle(null);
            txtNomorTelepon.setStyle(null);
            txtUsername.setStyle(null);
            txtAlamatLengkap.setStyle(null);
            txtPassword.setStyle(null);
            txtPasswordVisible.setStyle(null);
            txtKonfirmasiPassword.setStyle(null);
            txtKonfirmasiPasswordVisible.setStyle(null);
        }
    }

    private void setupButtonListeners() {
        btnUbah.setOnAction(this::handleUbahData);
        btnHapus.setOnAction(this::handleHapusData);
        btnBatal.setOnAction(event -> {
            resetForm();
            btnUbah.setDisable(true);
            btnHapus.setDisable(true);
        });
        btnAktifkan.setOnAction(this::handleAktifkanData);
    }

    // =========================================================
    // STATUS SORTING
    // =========================================================
    private void setupStatusSorting() {
        Comparator<PegawaiModel> statusComparator = (p1, p2) -> {
            String status1 = p1.getStatus();
            String status2 = p2.getStatus();

            if (status1 == null) status1 = "";
            if (status2 == null) status2 = "";

            int priority1 = getStatusPriority(status1);
            int priority2 = getStatusPriority(status2);

            int result = Integer.compare(priority1, priority2);

            if (result == 0) {
                result = status1.compareTo(status2);
            }

            return result;
        };

        if (filteredData != null) {
            filteredData.sorted(statusComparator);
        }
        masterData.sort(statusComparator);
    }

    private int getStatusPriority(String status) {
        if ("aktif".equalsIgnoreCase(status)) {
            return 0;
        } else if ("NonAktif".equalsIgnoreCase(status)) {
            return 1;
        } else {
            return 2;
        }
    }

    // =========================================================
    // PAGINATION METHODS
    // =========================================================
    private void updatePaginationData() {
        if (filteredData == null) return;

        Comparator<PegawaiModel> statusComparator = (p1, p2) -> {
            String status1 = p1.getStatus();
            String status2 = p2.getStatus();

            if (status1 == null) status1 = "";
            if (status2 == null) status2 = "";

            int priority1 = getStatusPriority(status1);
            int priority2 = getStatusPriority(status2);

            int result = Integer.compare(priority1, priority2);
            if (result == 0) {
                result = status1.compareTo(status2);
            }
            return result;
        };

        ObservableList<PegawaiModel> sortedList = FXCollections.observableArrayList(filteredData);
        sortedList.sort(statusComparator);

        int totalItems = sortedList.size();
        totalPages = (int) Math.ceil((double) totalItems / ITEMS_PER_PAGE);
        if (totalPages == 0) totalPages = 1;

        if (currentPage < 1) currentPage = 1;
        if (currentPage > totalPages) currentPage = totalPages;

        int fromIndex = (currentPage - 1) * ITEMS_PER_PAGE;
        int toIndex = Math.min(fromIndex + ITEMS_PER_PAGE, totalItems);

        if (totalItems == 0) {
            tblPegawai.setItems(FXCollections.observableArrayList());
        } else {
            ObservableList<PegawaiModel> pageData = FXCollections.observableArrayList(
                    sortedList.subList(fromIndex, toIndex)
            );
            tblPegawai.setItems(pageData);
        }

        updatePaginationButtons();
        updateInfoData();
    }

    private void updatePaginationButtons() {
        btnPage1.setText("Halaman " + currentPage + " dari " + totalPages);
        btnPrevPage.setDisable(currentPage <= 1);
        btnNextPage.setDisable(currentPage >= totalPages);
    }

    @FXML
    void handlePrevPage(ActionEvent event) {
        if (currentPage > 1) {
            currentPage--;
            updatePaginationData();
        }
    }

    @FXML
    void handleNextPage(ActionEvent event) {
        if (currentPage < totalPages) {
            currentPage++;
            updatePaginationData();
        }
    }

    // =========================================================
    // TAMPILKAN DATA
    // =========================================================
    private void tampilkanData() {
        masterData.clear();
        String query = "SELECT ID_Pegawai, Nama_Pegawai, Alamat, No_Telepon, Email, Username, Status_Pegawai " +
                "FROM v_TampilPegawaiTerurut ORDER BY UrutanStatus, Status_Pegawai, ID_Pegawai";

        try (java.sql.Statement st = dbConnection.getConnection().createStatement();
             ResultSet rs = st.executeQuery(query)) {
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

            Comparator<PegawaiModel> statusComparator = (p1, p2) -> {
                String status1 = p1.getStatus();
                String status2 = p2.getStatus();

                if (status1 == null) status1 = "";
                if (status2 == null) status2 = "";

                int priority1 = getStatusPriority(status1);
                int priority2 = getStatusPriority(status2);

                int result = Integer.compare(priority1, priority2);
                if (result == 0) {
                    result = status1.compareTo(status2);
                }
                return result;
            };
            masterData.sort(statusComparator);

            filteredData = new FilteredList<>(masterData, p -> true);
            currentPage = 1;
            updatePaginationData();
            updateInfoData();
        } catch (SQLException e) {
            tampilkanAlert(Alert.AlertType.ERROR, "Gagal memuat data", e.getMessage());
        }
    }

    private void updateInfoData() {
        int total = filteredData == null ? 0 : filteredData.size();
        int start = (currentPage - 1) * ITEMS_PER_PAGE + 1;
        int end = Math.min(start + ITEMS_PER_PAGE - 1, total);
        String displayRange = total == 0 ? "0" : start + "-" + end;

        if (!lblInfoData.getText().contains("⚠")) {
            lblInfoData.setText("Menampilkan " + displayRange + " dari " + total + " data");
        }
    }

    // =========================================================
    // DASHBOARD CARDS
    // =========================================================
    private void hitungDashboard() {
        try {
            String query = "SELECT " +
                    "dbo.f_TotalSemuaPegawai() AS Total, " +
                    "dbo.f_TotalPegawaiAktif() AS Aktif, " +
                    "dbo.f_TotalPegawaiNonAktif() AS NonAktif";

            try (java.sql.Statement st = dbConnection.getConnection().createStatement();
                 ResultSet rs = st.executeQuery(query)) {
                if (rs.next()) {
                    lblTotalPegawai.setText(String.valueOf(rs.getInt("Total")));
                    lblPegawaiAktif.setText(String.valueOf(rs.getInt("Aktif")));
                    lblPegawaiNonAktif.setText(String.valueOf(rs.getInt("NonAktif")));
                }
            }
        } catch (SQLException e) {
            tampilkanAlert(Alert.AlertType.ERROR, "Gagal menghitung dashboard", e.getMessage());
        }
    }

    // =========================================================
    // ISI FORM DARI TABEL
    // =========================================================
    private void isiFormDariTabel(PegawaiModel pegawai) {
        mode = Mode.UBAH;

        txtIdPegawai.setText(pegawai.getIdPegawai());
        txtNamaLengkap.setText(pegawai.getNamaPegawai());
        txtAlamatLengkap.setText(pegawai.getAlamat());
        txtNomorTelepon.setText(pegawai.getNoTelepon());
        txtEmail.setText(pegawai.getEmail());
        txtUsername.setText(pegawai.getUsername());

        String id = pegawai.getIdPegawai();
        if (id != null && id.startsWith("ADM")) {
            cmbRole.setValue("Admin");
        } else {
            cmbRole.setValue("Pegawai");
        }

        cmbRole.setDisable(true);

        txtPassword.clear();
        txtPasswordVisible.clear();
        txtKonfirmasiPassword.clear();
        txtKonfirmasiPasswordVisible.clear();
        txtPassword.setPromptText("Kosongkan jika tidak ganti password");
        txtPasswordVisible.setPromptText("Kosongkan jika tidak ganti password");
        txtKonfirmasiPassword.setPromptText("Kosongkan jika tidak ganti password");
        txtKonfirmasiPasswordVisible.setPromptText("Kosongkan jika tidak ganti password");

        btnSimpan.setDisable(true);

        hideAllErrorLabels();
    }

    // =========================================================
    // SIMPAN DATA
    // =========================================================
    @FXML
    void handleSimpanData(ActionEvent event) {
        if (checkInputErrors()) {
            tampilkanAlert(Alert.AlertType.WARNING, "Validasi Gagal", "Mohon perbaiki input yang ditandai merah");
            return;
        }

        if (!validasiInput(true)) return;

        String role = cmbRole.getValue();
        String roleCode = "PGW";
        if ("Admin".equals(role)) {
            roleCode = "ADM";
        }

        String query = "{call sp_TambahPegawai(?,?,?,?,?,?,?)}";

        try (CallableStatement cs = dbConnection.getConnection().prepareCall(query)) {
            cs.setString(1, txtNamaLengkap.getText().trim());
            cs.setString(2, txtAlamatLengkap.getText().trim());
            cs.setString(3, txtNomorTelepon.getText().trim());
            cs.setString(4, txtEmail.getText().trim());
            cs.setString(5, txtUsername.getText().trim());
            cs.setString(6, getPasswordText());
            cs.setString(7, roleCode);

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
            btnUbah.setDisable(true);
            btnHapus.setDisable(true);

        } catch (SQLException e) {
            tampilkanAlert(Alert.AlertType.ERROR, "Gagal menyimpan data", e.getMessage());
        }
    }

    // =========================================================
    // AKTIFKAN DATA
    // =========================================================
    @FXML
    void handleAktifkanData(ActionEvent event) {
        if (txtIdPegawai.getText() == null || txtIdPegawai.getText().isEmpty()) {
            tampilkanAlert(Alert.AlertType.WARNING, "Peringatan", "Pilih data pegawai yang akan diaktifkan!");
            return;
        }

        Alert konfirmasi = new Alert(Alert.AlertType.CONFIRMATION);
        konfirmasi.setTitle("Konfirmasi Aktivasi");
        konfirmasi.setHeaderText(null);
        konfirmasi.setContentText("Yakin ingin mengaktifkan pegawai dengan ID " + txtIdPegawai.getText() + " ?\nStatus akan berubah dari NonAktif menjadi Aktif.");

        konfirmasi.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                updateStatusPegawai(txtIdPegawai.getText().trim(), "Aktif");
            }
        });
    }

    // =========================================================
    // UBAH DATA
    // =========================================================
    @FXML
    void handleUbahData(ActionEvent event) {
        if (txtIdPegawai.getText() == null || txtIdPegawai.getText().isEmpty()) {
            tampilkanAlert(Alert.AlertType.WARNING, "Peringatan", "Pilih dulu data pegawai dari tabel yang mau diubah!");
            return;
        }

        String statusSekarang = getStatusDariDatabase(txtIdPegawai.getText().trim());

        if ("NonAktif".equalsIgnoreCase(statusSekarang)) {
            tampilkanAlert(Alert.AlertType.WARNING, "Tidak Bisa Ubah",
                    "Pegawai dengan status NonAktif tidak dapat diubah.\nGunakan tombol 'Aktifkan' untuk mengubah status.");
            return;
        }

        if (checkInputErrors()) {
            tampilkanAlert(Alert.AlertType.WARNING, "Validasi Gagal", "Mohon perbaiki input yang ditandai merah");
            return;
        }

        if (!validasiInput(false)) return;

        String status = getStatusDariDatabase(txtIdPegawai.getText().trim());
        if (status.isEmpty()) {
            status = "aktif";
        }

        String query = "{call sp_UpdatePegawai(?,?,?,?,?,?,?,?)}";

        try {
            String passwordFinal = getPasswordText();
            if (isKosong(passwordFinal)) {
                passwordFinal = "";
            }

            try (CallableStatement cs = dbConnection.getConnection().prepareCall(query)) {
                cs.setString(1, txtIdPegawai.getText().trim());
                cs.setString(2, txtNamaLengkap.getText().trim());
                cs.setString(3, txtAlamatLengkap.getText().trim());
                cs.setString(4, txtNomorTelepon.getText().trim());
                cs.setString(5, txtEmail.getText().trim());
                cs.setString(6, txtUsername.getText().trim());
                cs.setString(7, passwordFinal);
                cs.setString(8, status);
                cs.execute();
            }

            tampilkanAlert(Alert.AlertType.INFORMATION, "Sukses", "Data pegawai berhasil diubah.");

            tampilkanData();
            hitungDashboard();
            resetForm();
            btnUbah.setDisable(true);
            btnHapus.setDisable(true);

        } catch (SQLException e) {
            String errorMsg = e.getMessage();
            if (errorMsg.contains("NonAktif tidak dapat diubah")) {
                tampilkanAlert(Alert.AlertType.WARNING, "Tidak Bisa Update",
                        "Pegawai dengan status NonAktif tidak dapat diubah selain status.");
            } else {
                tampilkanAlert(Alert.AlertType.ERROR, "Gagal mengubah data", errorMsg);
            }
        }
    }

    // =========================================================
    // UPDATE STATUS PEGAWAI
    // =========================================================
    private void updateStatusPegawai(String idPegawai, String statusBaru) {
        String query = "UPDATE Pegawai SET Status_Pegawai = ? WHERE ID_Pegawai = ?";

        try (PreparedStatement ps = dbConnection.getConnection().prepareStatement(query)) {
            ps.setString(1, statusBaru);
            ps.setString(2, idPegawai);
            int rowsAffected = ps.executeUpdate();

            if (rowsAffected > 0) {
                tampilkanAlert(Alert.AlertType.INFORMATION, "Sukses",
                        "Pegawai dengan ID " + idPegawai + " berhasil diaktifkan.");
                tampilkanData();
                hitungDashboard();
                resetForm();
                btnUbah.setDisable(true);
                btnHapus.setDisable(true);
                hideAllStatusComponents();
            }
        } catch (SQLException e) {
            tampilkanAlert(Alert.AlertType.ERROR, "Gagal mengaktifkan pegawai", e.getMessage());
        }
    }

    // =========================================================
    // HAPUS DATA
    // =========================================================
    @FXML
    void handleHapusData(ActionEvent event) {
        if (txtIdPegawai.getText() == null || txtIdPegawai.getText().isEmpty()) {
            tampilkanAlert(Alert.AlertType.WARNING, "Peringatan", "Pilih dulu data pegawai dari tabel yang mau dihapus!");
            return;
        }

        String statusSekarang = getStatusDariDatabase(txtIdPegawai.getText().trim());
        if ("NonAktif".equalsIgnoreCase(statusSekarang)) {
            tampilkanAlert(Alert.AlertType.WARNING, "Tidak Bisa Hapus",
                    "Pegawai dengan status NonAktif tidak dapat dihapus.");
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
            btnUbah.setDisable(true);
            btnHapus.setDisable(true);

        } catch (SQLException e) {
            tampilkanAlert(Alert.AlertType.ERROR, "Gagal menghapus data", e.getMessage());
        }
    }

    // =========================================================
    // AMBIL STATUS DARI DATABASE
    // =========================================================
    private String getStatusDariDatabase(String idPegawai) {
        String query = "SELECT Status_Pegawai FROM Pegawai WHERE ID_Pegawai = ?";
        try (PreparedStatement ps = dbConnection.getConnection().prepareStatement(query)) {
            ps.setString(1, idPegawai);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("Status_Pegawai");
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return "";
    }

    // =========================================================
    // BATAL / RESET FORM
    // =========================================================
    @FXML
    void handleBatal(ActionEvent event) {
        resetForm();
        if (lblInfoEmail != null) {
            lblInfoEmail.setVisible(true);
        }

        btnSimpan.setDisable(false);
        btnUbah.setDisable(true);
        btnHapus.setDisable(true);
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
        txtKonfirmasiPassword.clear();
        txtKonfirmasiPasswordVisible.clear();
        txtPassword.setPromptText("Masukan Password...");
        txtPasswordVisible.setPromptText("Masukan Password...");
        txtKonfirmasiPassword.setPromptText("Konfirmasi Password...");
        txtKonfirmasiPasswordVisible.setPromptText("Konfirmasi Password...");

        cmbRole.setDisable(false);
        cmbRole.setValue("Pegawai");
        generateIdOtomatis();

        btnSimpan.setDisable(false);

        tblPegawai.getSelectionModel().clearSelection();
        statusPegawaiTerpilih = "";

        hideAllErrorLabels();

        txtNamaLengkap.setStyle(null);
        txtEmail.setStyle(null);
        txtNomorTelepon.setStyle(null);
        txtAlamatLengkap.setStyle(null);
        txtUsername.setStyle(null);

        setAllFieldsDisable(false);
        hideAllStatusComponents();

        if (!lblInfoData.getText().contains("⚠")) {
            updateInfoData();
        } else {
            lblInfoData.setText("");
        }
    }

    // =========================================================
    // ERROR LABEL HELPERS
    // =========================================================
    private void hideAllErrorLabels() {
        if (lblErrorNama != null) { lblErrorNama.setVisible(false); lblErrorNama.setText(""); }
        if (lblErrorEmail != null) { lblErrorEmail.setVisible(false); lblErrorEmail.setText(""); }
        if (lblErrorTelepon != null) { lblErrorTelepon.setVisible(false); lblErrorTelepon.setText(""); }
        if (lblErrorAlamat != null) { lblErrorAlamat.setVisible(false); lblErrorAlamat.setText(""); }
        if (lblErrorUsername != null) { lblErrorUsername.setVisible(false); lblErrorUsername.setText(""); }
        if (lblErrorPassword != null) { lblErrorPassword.setVisible(false); lblErrorPassword.setText(""); }
        if (lblErrorKonfirmasiPassword != null) { lblErrorKonfirmasiPassword.setVisible(false); lblErrorKonfirmasiPassword.setText(""); }
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
    // TOGGLE PASSWORD
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

    @FXML
    void handleToggleKonfirmasiPassword(ActionEvent event) {
        statusTampilKonfirmasiPassword = !statusTampilKonfirmasiPassword;

        if (statusTampilKonfirmasiPassword) {
            txtKonfirmasiPasswordVisible.setText(txtKonfirmasiPassword.getText());
            txtKonfirmasiPasswordVisible.setVisible(true);
            txtKonfirmasiPasswordVisible.setManaged(true);
            txtKonfirmasiPassword.setVisible(false);
            txtKonfirmasiPassword.setManaged(false);
        } else {
            txtKonfirmasiPassword.setText(txtKonfirmasiPasswordVisible.getText());
            txtKonfirmasiPassword.setVisible(true);
            txtKonfirmasiPassword.setManaged(true);
            txtKonfirmasiPasswordVisible.setVisible(false);
            txtKonfirmasiPasswordVisible.setManaged(false);
        }
    }

    private String getPasswordText() {
        return statusTampilPassword ? txtPasswordVisible.getText() : txtPassword.getText();
    }

    private String getKonfirmasiPasswordText() {
        return statusTampilKonfirmasiPassword ? txtKonfirmasiPasswordVisible.getText() : txtKonfirmasiPassword.getText();
    }

    // =========================================================
    // VALIDASI INPUT
    // =========================================================
    private boolean validasiInput(boolean wajibPassword) {
        StringBuilder pesan = new StringBuilder();

        if (cmbRole.getValue() == null) {
            pesan.append("- Role wajib dipilih.\n");
        }

        if (isKosong(txtNamaLengkap.getText())) {
            pesan.append("- Nama lengkap wajib diisi.\n");
            showErrorLabel(lblErrorNama, "Nama lengkap wajib diisi");
        } else {
            String nama = txtNamaLengkap.getText().trim();
            if (nama.length() < 4) {
                pesan.append("- Nama lengkap minimal 4 karakter.\n");
                showErrorLabel(lblErrorNama, "Nama lengkap minimal 4 karakter");
            } else if (!nama.matches("^[a-zA-Z\\s]+$")) {
                pesan.append("- Nama hanya boleh berisi huruf dan spasi.\n");
                showErrorLabel(lblErrorNama, "Nama hanya boleh berisi huruf dan spasi");
            } else if (isDataExist("Nama_Pegawai", nama)) {
                pesan.append("- Nama lengkap sudah terdaftar.\n");
                showErrorLabel(lblErrorNama, "Nama sudah terdaftar");
            } else {
                hideErrorLabel(lblErrorNama);
            }
        }

        if (isKosong(txtEmail.getText())) {
            pesan.append("- Email wajib diisi.\n");
            showErrorLabel(lblErrorEmail, "Email wajib diisi");
        } else {
            String email = txtEmail.getText().trim().toLowerCase();
            if (!isValidEmail(email)) {
                pesan.append("- Format email tidak valid. Gunakan domain: @gmail.com, @yahoo.com, @outlook.com, @icloud.com, @ac.id, @edu, @student.(kampus).ac.id\n");
                showErrorLabel(lblErrorEmail, "Format email tidak valid");
            } else if (isDataExist("Email", email)) {
                pesan.append("- Email sudah digunakan.\n");
                showErrorLabel(lblErrorEmail, "Email sudah digunakan");
            } else {
                hideErrorLabel(lblErrorEmail);
            }
        }

        if (isKosong(txtNomorTelepon.getText())) {
            pesan.append("- Nomor telepon wajib diisi.\n");
            showErrorLabel(lblErrorTelepon, "Nomor telepon wajib diisi");
        } else {
            String telepon = txtNomorTelepon.getText().trim();
            if (!telepon.startsWith("08")) {
                pesan.append("- Nomor telepon HARUS diawali '08'.\n");
                showErrorLabel(lblErrorTelepon, "Nomor telepon HARUS diawali '08'");
            } else if (telepon.length() < 10 || telepon.length() > 13) {
                pesan.append("- Nomor telepon harus 10-13 digit.\n");
                showErrorLabel(lblErrorTelepon, "Nomor telepon harus 10-13 digit");
            } else if (!telepon.matches("^[0-9]+$")) {
                pesan.append("- Nomor telepon hanya boleh berisi angka.\n");
                showErrorLabel(lblErrorTelepon, "Nomor telepon hanya boleh berisi angka");
            } else if (isDataExist("No_Telepon", telepon)) {
                pesan.append("- Nomor telepon sudah terdaftar.\n");
                showErrorLabel(lblErrorTelepon, "Nomor telepon sudah terdaftar");
            } else {
                hideErrorLabel(lblErrorTelepon);
            }
        }

        if (isKosong(txtUsername.getText())) {
            pesan.append("- Username wajib diisi.\n");
            showErrorLabel(lblErrorUsername, "Username wajib diisi");
        } else {
            String username = txtUsername.getText().trim();
            if (username.length() < 4) {
                pesan.append("- Username minimal 4 karakter.\n");
                showErrorLabel(lblErrorUsername, "Username minimal 4 karakter");
            } else if (username.length() > 10) {
                pesan.append("- Username maksimal 10 karakter.\n");
                showErrorLabel(lblErrorUsername, "Username maksimal 10 karakter");
            } else if (!username.matches("^[a-zA-Z]+$")) {
                pesan.append("- Username HANYA boleh huruf (tanpa angka/simbol).\n");
                showErrorLabel(lblErrorUsername, "Username HANYA boleh huruf");
            } else if (isDataExist("Username", username)) {
                pesan.append("- Username sudah dipakai.\n");
                showErrorLabel(lblErrorUsername, "Username sudah dipakai");
            } else {
                hideErrorLabel(lblErrorUsername);
            }
        }

        if (isKosong(txtAlamatLengkap.getText())) {
            pesan.append("- Alamat wajib diisi.\n");
            showErrorLabel(lblErrorAlamat, "Alamat wajib diisi");
        } else {
            String alamat = txtAlamatLengkap.getText().trim();
            if (!alamat.matches("^[a-zA-Z0-9\\s.]+$")) {
                pesan.append("- Alamat hanya boleh huruf, angka, spasi, dan titik (.).\n");
                showErrorLabel(lblErrorAlamat, "Alamat hanya boleh huruf, angka, spasi, dan titik (.)");
            } else {
                hideErrorLabel(lblErrorAlamat);
            }
        }

        String password = getPasswordText();
        if (wajibPassword) {
            if (isKosong(password)) {
                pesan.append("- Password wajib diisi.\n");
                showErrorLabel(lblErrorPassword, "Password wajib diisi");
            } else if (password.trim().length() < 4) {
                pesan.append("- Password minimal 4 karakter.\n");
                showErrorLabel(lblErrorPassword, "Password minimal 4 karakter");
            } else {
                hideErrorLabel(lblErrorPassword);
            }
        } else {
            if (!isKosong(password) && password.trim().length() < 4) {
                pesan.append("- Password minimal 4 karakter.\n");
                showErrorLabel(lblErrorPassword, "Password minimal 4 karakter");
            } else {
                hideErrorLabel(lblErrorPassword);
            }
        }

        if (!isKosong(password)) {
            String konfirmasi = getKonfirmasiPasswordText();
            if (isKosong(konfirmasi)) {
                pesan.append("- Konfirmasi password wajib diisi.\n");
                showErrorLabel(lblErrorKonfirmasiPassword, "Konfirmasi password wajib diisi");
            } else if (!password.equals(konfirmasi)) {
                pesan.append("- Password dan konfirmasi password tidak sama.\n");
                showErrorLabel(lblErrorKonfirmasiPassword, "Password tidak sama");
            } else {
                hideErrorLabel(lblErrorKonfirmasiPassword);
            }
        } else {
            hideErrorLabel(lblErrorKonfirmasiPassword);
        }

        if (pesan.length() > 0) {
            tampilkanAlert(Alert.AlertType.WARNING, "Data belum lengkap atau tidak valid", pesan.toString());
            return false;
        }
        return true;
    }

    private boolean isDataExist(String column, String value) {
        String currentId = txtIdPegawai.getText().trim();
        String query = "SELECT COUNT(*) FROM Pegawai WHERE " + column + " = ? AND ID_Pegawai != ?";

        try (PreparedStatement ps = dbConnection.getConnection().prepareStatement(query)) {
            ps.setString(1, value);
            ps.setString(2, currentId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
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
}