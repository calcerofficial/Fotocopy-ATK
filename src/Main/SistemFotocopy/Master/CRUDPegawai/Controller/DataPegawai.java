package SistemFotocopy.Master.CRUDPegawai.Controller;

import Database.DBConnection;
import SistemFotocopy.Master.CRUDPegawai.Dataclass.PegawaiModel;
import javafx.animation.PauseTransition;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.util.Duration;

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
    // SETUP ROLE COMBOBOX
    // =========================================================
    private void setupRoleComboBox() {
        cmbRole.setItems(FXCollections.observableArrayList("Pegawai", "Admin"));
        cmbRole.setValue("Pegawai");

        cmbRole.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (mode == Mode.TAMBAH) {
                generateIdOtomatis();
            }
        });
    }

    // =========================================================
    // GENERATE ID OTOMATIS (BERDASARKAN ROLE YANG DIPILIH)
    // =========================================================
    private void generateIdOtomatis() {
        String role = cmbRole.getValue();
        String prefix = "Admin".equals(role) ? "ADM" : "PGW";

        String query = "SELECT TOP 1 ID_Pegawai FROM Pegawai WHERE ID_Pegawai LIKE ? ORDER BY ID_Pegawai DESC";

        try (PreparedStatement ps = dbConnection.getConnection().prepareStatement(query)) {
            ps.setString(1, prefix + "%");
            try (ResultSet rs = ps.executeQuery()) {
                int nextNumber = 1;
                if (rs.next()) {
                    String lastId = rs.getString("ID_Pegawai");
                    String numberPart = lastId.substring(prefix.length());
                    try {
                        nextNumber = Integer.parseInt(numberPart) + 1;
                    } catch (NumberFormatException nfe) {
                        nextNumber = 1;
                    }
                }
                String idBaru = prefix + String.format("%03d", nextNumber);
                txtIdPegawai.setText(idBaru);
            }
        } catch (SQLException e) {
            txtIdPegawai.setText("");
            e.printStackTrace();
        }
    }

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

        setupInputFilters();
        setupLiveValidation();

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
    // FLASH MERAH KETIKA KARAKTER DITOLAK OLEH TEXT FORMATTER
    // =========================================================
    private void flashInvalidInput(Control control, Label errorLabel, String message, Runnable revalidate) {
        setRedBorder(control);
        showErrorLabel(errorLabel, message);

        PauseTransition pause = new PauseTransition(Duration.millis(400));
        pause.setOnFinished(e -> revalidate.run());
        pause.play();
    }

    // =========================================================
// FILTER INPUT (BLOKIR KARAKTER TERLARANG SAAT DIKETIK)
// =========================================================
    private void setupInputFilters() {

        // --- NAMA LENGKAP: hanya huruf dan spasi ---
        txtNamaLengkap.setTextFormatter(new TextFormatter<String>(change -> {
            String newText = change.getControlNewText();
            if (newText.matches("^[a-zA-Z\\s]*$")) {
                return change;
            }
            flashInvalidInput(txtNamaLengkap, lblErrorNama, "Karakter tidak diizinkan", this::validateNamaLive);
            return null;
        }));

        // --- EMAIL: hanya huruf, angka, dan simbol @._- ---
        txtEmail.setTextFormatter(new TextFormatter<String>(change -> {
            String newText = change.getControlNewText();
            if (newText.matches("^[a-zA-Z0-9@._-]*$")) {
                return change;
            }
            flashInvalidInput(txtEmail, lblErrorEmail, "Karakter tidak diizinkan", this::validateEmailLive);
            return null;
        }));

        // --- NOMOR TELEPON: harus "08", hanya angka, maksimal 13 digit ---
        txtNomorTelepon.setTextFormatter(new TextFormatter<String>(change -> {
            String newText = change.getControlNewText();

            // Regex: kosong ATAU "0" ATAU "08" diikuti 0-11 digit angka
            if (newText.isEmpty() || newText.matches("^0$") || newText.matches("^08\\d{0,11}$")) {
                return change;
            }

            flashInvalidInput(txtNomorTelepon, lblErrorTelepon, "Harus diawali '08' & maksimal 13 digit", this::validateTeleponLive);
            return null;
        }));

        // --- ALAMAT: hanya huruf, angka, spasi, dan titik ---
        txtAlamatLengkap.setTextFormatter(new TextFormatter<String>(change -> {
            String newText = change.getControlNewText();
            if (newText.matches("^[a-zA-Z0-9\\s.]*$")) {
                return change;
            }
            flashInvalidInput(txtAlamatLengkap, lblErrorAlamat, "Karakter tidak diizinkan", this::validateAlamatLive);
            return null;
        }));

        // --- USERNAME: hanya huruf dan angka ---
        txtUsername.setTextFormatter(new TextFormatter<String>(change -> {
            String newText = change.getControlNewText();
            if (newText.matches("^[a-zA-Z0-9]*$")) {
                return change;
            }
            flashInvalidInput(txtUsername, lblErrorUsername, "Karakter tidak diizinkan", this::validateUsernameLive);
            return null;
        }));
    }

    // =========================================================
    // LIVE COLOR VALIDATION (REAL-TIME MERAH, ORANYE, HIJAU)
    // =========================================================
    private void setupLiveValidation() {
        txtNamaLengkap.textProperty().addListener((obs, oldVal, newVal) -> validateNamaLive());
        txtEmail.textProperty().addListener((obs, oldVal, newVal) -> validateEmailLive());
        txtNomorTelepon.textProperty().addListener((obs, oldVal, newVal) -> validateTeleponLive());
        txtUsername.textProperty().addListener((obs, oldVal, newVal) -> validateUsernameLive());
        txtAlamatLengkap.textProperty().addListener((obs, oldVal, newVal) -> validateAlamatLive());

        // --- PASSWORD ---
        txtPassword.textProperty().addListener((obs, oldVal, newVal) -> validatePasswordLive());
        txtPasswordVisible.textProperty().addListener((obs, oldVal, newVal) -> validatePasswordLive());

        // --- KONFIRMASI PASSWORD ---
        txtKonfirmasiPassword.textProperty().addListener((obs, oldVal, newVal) -> validateKonfirmasiPasswordLive());
        txtKonfirmasiPasswordVisible.textProperty().addListener((obs, oldVal, newVal) -> validateKonfirmasiPasswordLive());
    }

    // =========================================================
    // VALIDASI LIVE PER FIELD (DIPAKAI LISTENER & FLASH REVALIDATE)
    // =========================================================
    private void validateNamaLive() {
        String newVal = txtNamaLengkap.getText();
        if (newVal == null || newVal.isEmpty()) {
            resetBorder(txtNamaLengkap);
            hideErrorLabel(lblErrorNama);
            return;
        }
        if (!newVal.matches("^[a-zA-Z\\s]*$")) {
            setRedBorder(txtNamaLengkap);
            showErrorLabel(lblErrorNama, "Hanya huruf dan spasi");
        } else if (newVal.length() < 4) {
            setOrangeBorder(txtNamaLengkap);
            showErrorLabel(lblErrorNama, "Minimal 4 karakter");
        } else {
            setGreenBorder(txtNamaLengkap);
            hideErrorLabel(lblErrorNama);
        }
    }

    private void validateEmailLive() {
        String newVal = txtEmail.getText();
        if (newVal == null || newVal.isEmpty()) {
            resetBorder(txtEmail);
            hideErrorLabel(lblErrorEmail);
            return;
        }
        String valLower = newVal.toLowerCase();
        if (!valLower.matches("^[a-z0-9@._-]*$")) {
            setRedBorder(txtEmail);
            showErrorLabel(lblErrorEmail, "Hanya huruf, angka, dan simbol (@._-)");
        } else if (!valLower.contains("@") || valLower.split("@")[0].length() < 3) {
            setOrangeBorder(txtEmail);
            showErrorLabel(lblErrorEmail, "Minimal 3 karakter sebelum @");
        } else if (!valLower.contains("@") || valLower.split("@").length != 2 || !isValidEmailDomain(valLower)) {
            setOrangeBorder(txtEmail);
            showErrorLabel(lblErrorEmail, "Domain tidak diizinkan");
        } else {
            setGreenBorder(txtEmail);
            hideErrorLabel(lblErrorEmail);
        }
    }

    private void validateTeleponLive() {
        String newVal = txtNomorTelepon.getText();
        if (newVal == null || newVal.isEmpty()) {
            resetBorder(txtNomorTelepon);
            hideErrorLabel(lblErrorTelepon);
            return;
        }
        if (!newVal.matches("^[0-9]*$")) {
            setRedBorder(txtNomorTelepon);
            showErrorLabel(lblErrorTelepon, "Hanya angka");
        } else if (newVal.length() > 13) {
            setRedBorder(txtNomorTelepon);
            showErrorLabel(lblErrorTelepon, "Maksimal 13 digit");
        } else if (newVal.length() < 10) {
            setOrangeBorder(txtNomorTelepon);
            showErrorLabel(lblErrorTelepon, "Minimal 10 digit");
        } else if (!newVal.startsWith("08")) {
            setRedBorder(txtNomorTelepon);
            showErrorLabel(lblErrorTelepon, "Harus diawali '08'");
        } else {
            setGreenBorder(txtNomorTelepon);
            hideErrorLabel(lblErrorTelepon);
        }
    }

    private void validateUsernameLive() {
        String newVal = txtUsername.getText();
        if (newVal == null || newVal.isEmpty()) {
            resetBorder(txtUsername);
            hideErrorLabel(lblErrorUsername);
            return;
        }
        if (!newVal.matches("^[a-zA-Z0-9]*$")) {
            setRedBorder(txtUsername);
            showErrorLabel(lblErrorUsername, "Hanya huruf dan angka");
        } else if (newVal.length() > 50) {
            setRedBorder(txtUsername);
            showErrorLabel(lblErrorUsername, "Maksimal 50 karakter");
        } else if (newVal.length() < 4) {
            setOrangeBorder(txtUsername);
            showErrorLabel(lblErrorUsername, "Minimal 4 karakter");
        } else {
            setGreenBorder(txtUsername);
            hideErrorLabel(lblErrorUsername);
        }
    }

    private void validateAlamatLive() {
        String newVal = txtAlamatLengkap.getText();
        if (newVal == null || newVal.isEmpty()) {
            resetBorder(txtAlamatLengkap);
            hideErrorLabel(lblErrorAlamat);
            return;
        }
        if (!newVal.matches("^[a-zA-Z0-9\\s.]*$")) {
            setRedBorder(txtAlamatLengkap);
            showErrorLabel(lblErrorAlamat, "Huruf, angka, spasi, dan titik (.)");
        } else {
            setGreenBorder(txtAlamatLengkap);
            hideErrorLabel(lblErrorAlamat);
        }
    }

    // =========================================================
    // LIVE VALIDATION - PASSWORD & KONFIRMASI PASSWORD
    // =========================================================
    private void validatePasswordLive() {
        String value = getPasswordText();
        Control aktif = statusTampilPassword ? txtPasswordVisible : txtPassword;

        if (value.isEmpty()) {
            resetBorder(txtPassword);
            resetBorder(txtPasswordVisible);
            hideErrorLabel(lblErrorPassword);
        } else if (value.length() < 4) {
            setOrangeBorder(aktif);
            showErrorLabel(lblErrorPassword, "Minimal 4 karakter");
        } else if (value.length() > 50) {
            setRedBorder(aktif);
            showErrorLabel(lblErrorPassword, "Maksimal 50 karakter");
        } else {
            setGreenBorder(aktif);
            hideErrorLabel(lblErrorPassword);
        }

        // Re-cek konfirmasi setiap kali password berubah
        validateKonfirmasiPasswordLive();
    }

    private void validateKonfirmasiPasswordLive() {
        String password = getPasswordText();
        String konfirmasi = getKonfirmasiPasswordText();
        Control aktif = statusTampilKonfirmasiPassword ? txtKonfirmasiPasswordVisible : txtKonfirmasiPassword;

        if (konfirmasi.isEmpty()) {
            resetBorder(txtKonfirmasiPassword);
            resetBorder(txtKonfirmasiPasswordVisible);
            hideErrorLabel(lblErrorKonfirmasiPassword);
        } else if (!konfirmasi.equals(password)) {
            setRedBorder(aktif);
            showErrorLabel(lblErrorKonfirmasiPassword, "Password tidak sama");
        } else {
            setGreenBorder(aktif);
            hideErrorLabel(lblErrorKonfirmasiPassword);
        }
    }

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
        resetBorder(txtNamaLengkap);
        resetBorder(txtEmail);
        resetBorder(txtNomorTelepon);
        resetBorder(txtAlamatLengkap);
        resetBorder(txtUsername);
        resetBorder(txtPassword);
        resetBorder(txtPasswordVisible);
        resetBorder(txtKonfirmasiPassword);
        resetBorder(txtKonfirmasiPasswordVisible);
    }

    // =========================================================
    // SIMPAN DATA
    // =========================================================
    @FXML
    void handleSimpanData(ActionEvent event) {
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
        resetBorder(txtNamaLengkap);
        resetBorder(txtEmail);
        resetBorder(txtNomorTelepon);
        resetBorder(txtAlamatLengkap);
        resetBorder(txtUsername);
        resetBorder(txtPassword);
        resetBorder(txtPasswordVisible);
        resetBorder(txtKonfirmasiPassword);
        resetBorder(txtKonfirmasiPasswordVisible);

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

        validatePasswordLive();
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

        validateKonfirmasiPasswordLive();
    }

    private String getPasswordText() {
        return statusTampilPassword ? txtPasswordVisible.getText() : txtPassword.getText();
    }

    private String getKonfirmasiPasswordText() {
        return statusTampilKonfirmasiPassword ? txtKonfirmasiPasswordVisible.getText() : txtKonfirmasiPassword.getText();
    }

    // =========================================================
    // VALIDASI DOMAIN EMAIL
    // =========================================================
    private boolean isValidEmailDomain(String email) {
        if (isKosong(email)) return false;

        if (!email.contains("@")) {
            return false;
        }

        String[] parts = email.split("@");
        if (parts.length != 2) {
            return false;
        }

        String domain = parts[1];

        String[] allowedDomains = {
                "gmail.com",
                "yahoo.com",
                "outlook.com",
                "icloud.com",
                "ac.id",
                "edu"
        };

        for (String allowedDomain : allowedDomains) {
            if (domain.equals(allowedDomain)) {
                return true;
            }
        }

        if (domain.matches("^student\\.[a-z]+\\.ac\\.id$")) {
            return true;
        }

        return false;
    }

    // =========================================================
    // VALIDASI INPUT (SAAT TOMBOL SIMPAN DI TEKAN)
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

        // ==================== VALIDASI EMAIL ====================
        if (isKosong(txtEmail.getText())) {
            pesan.append("- Email wajib diisi.\n");
            showErrorLabel(lblErrorEmail, "Email wajib diisi");
        } else {
            String email = txtEmail.getText().trim().toLowerCase();

            if (isDataExist("Email", email)) {
                pesan.append("- Email sudah digunakan.\n");
                showErrorLabel(lblErrorEmail, "Email sudah digunakan");
            } else if (email.split("@")[0].length() < 3) {
                pesan.append("- Bagian depan email (sebelum @) minimal 3 karakter.\n");
                showErrorLabel(lblErrorEmail, "Minimal 3 karakter sebelum @");
            } else if (!email.matches("^[a-z0-9@._-]+$")) {
                pesan.append("- Email hanya boleh huruf, angka, dan simbol (@ . _ -).\n");
                showErrorLabel(lblErrorEmail, "Hanya huruf, angka, dan simbol (@._-)");
            } else if (!isValidEmailDomain(email)) {
                pesan.append("- Format domain tidak valid. Gunakan: @gmail.com, @yahoo.com, @outlook.com, @icloud.com, @ac.id, @edu, @student.(kampus).ac.id\n");
                showErrorLabel(lblErrorEmail, "Domain tidak diizinkan");
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
            } else if (username.length() > 50) {
                pesan.append("- Username maksimal 50 karakter.\n");
                showErrorLabel(lblErrorUsername, "Username maksimal 50 karakter");
            } else if (!username.matches("^[a-zA-Z0-9]+$")) {
                pesan.append("- Username hanya boleh huruf dan angka.\n");
                showErrorLabel(lblErrorUsername, "Username hanya boleh huruf dan angka");
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
            } else if (password.length() < 6) {
                pesan.append("- Password minimal 6 karakter.\n");
                showErrorLabel(lblErrorPassword, "Password minimal 6 karakter");
            } else if (password.length() > 50) {
                pesan.append("- Password maksimal 50 karakter.\n");
                showErrorLabel(lblErrorPassword, "Password maksimal 50 karakter");
            } else {
                hideErrorLabel(lblErrorPassword);
            }
        } else if (!isKosong(password)) {
            if (password.length() < 6) {
                pesan.append("- Password minimal 6 karakter.\n");
                showErrorLabel(lblErrorPassword, "Password minimal 6 karakter");
            } else if (password.length() > 50) {
                pesan.append("- Password maksimal 50 karakter.\n");
                showErrorLabel(lblErrorPassword, "Password maksimal 50 karakter");
            } else {
                hideErrorLabel(lblErrorPassword);
            }
        } else {
            hideErrorLabel(lblErrorPassword);
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