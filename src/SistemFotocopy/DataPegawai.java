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
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.sql.PreparedStatement;
import java.sql.CallableStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Comparator;
import java.util.regex.Pattern;

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
    @FXML private TextField txtStatus;

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

    // =========================================================
    // INITIALIZE
    // =========================================================
    @FXML
    public void initialize() {
        setupTableColumns();
        setupSearchListener();
        setupRowSelectionListener();
        setupButtonListeners();
        setupRealTimeValidation();

        cmbStatus.setItems(FXCollections.observableArrayList("aktif", "NonAktif"));

        tampilkanData();
        hitungDashboard();
        resetForm();
        updatePaginationButtons();

        // Set default button states
        btnUbah.setDisable(true);
        btnHapus.setDisable(true);

        // Enable sorting on status column
        setupStatusSorting();
    }

    // =========================================================
    // REAL-TIME VALIDATION
    // =========================================================
    private void setupRealTimeValidation() {
        // Validasi Nama Lengkap (hanya huruf dan spasi)
        setupFieldValidation(txtNamaLengkap,
                "^[a-zA-Z\\s]{3,100}$",
                "Nama harus terdiri dari 3-100 karakter huruf dan spasi"
        );

        // Validasi Email
        setupFieldValidation(txtEmail,
                "^[A-Za-z0-9+_.-]+@(.+)$",
                "Format email tidak valid (contoh: nama@domain.com)"
        );

        // Validasi Nomor Telepon (angka, minimal 10 digit)
        setupFieldValidation(txtNomorTelepon,
                "^[0-9]{10,15}$",
                "Nomor telepon harus 10-15 digit angka"
        );

        // Validasi Username (huruf, angka, underscore, minimal 4 karakter)
        setupFieldValidation(txtUsername,
                "^[a-zA-Z0-9_]{4,20}$",
                "Username harus 4-20 karakter (huruf, angka, underscore)"
        );

        // Validasi Alamat (minimal 5 karakter)
        setupFieldValidation(txtAlamatLengkap,
                "^.{5,500}$",
                "Alamat harus minimal 5 karakter"
        );
    }

    private void setupFieldValidation(TextField textField, String regex, String errorMessage) {
        // Create a TextFormatter with validation
        TextFormatter<String> formatter = new TextFormatter<>(change -> {
            // Remove error style when user starts typing
            textField.getStyleClass().remove("error-field");
            textField.setTooltip(null);

            // Allow empty for optional fields
            if (change.getText() == null || change.getText().isEmpty()) {
                return change;
            }

            // Check if the new text matches the pattern
            String newText = change.getControlNewText();
            if (newText.isEmpty()) {
                return change;
            }

            // Validate against pattern (allow partial input during typing)
            Pattern pattern = Pattern.compile(regex);
            if (pattern.matcher(newText).matches()) {
                return change;
            }

            // If doesn't match, show error but still allow typing
            return change;
        });

        textField.setTextFormatter(formatter);

        // Add focus listener to validate when focus lost
        textField.focusedProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal) { // Focus lost
                validateFieldOnBlur(textField, regex, errorMessage);
            }
        });

        // Add key listener to validate on typing
        textField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (!textField.isFocused()) return;

            String text = newVal != null ? newVal.trim() : "";
            if (text.isEmpty()) {
                // Empty is allowed for optional fields
                textField.getStyleClass().remove("error-field");
                textField.setTooltip(null);
                return;
            }

            // Validate the current text
            Pattern pattern = Pattern.compile(regex);
            if (!pattern.matcher(text).matches()) {
                // Show error style
                textField.getStyleClass().add("error-field");
                Tooltip tooltip = new Tooltip(errorMessage);
                textField.setTooltip(tooltip);
            } else {
                textField.getStyleClass().remove("error-field");
                textField.setTooltip(null);
            }
        });
    }

    private void setupFieldValidation(TextArea textArea, String regex, String errorMessage) {
        // Similar validation for TextArea
        textArea.focusedProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal) { // Focus lost
                validateFieldOnBlur(textArea, regex, errorMessage);
            }
        });

        textArea.textProperty().addListener((obs, oldVal, newVal) -> {
            if (!textArea.isFocused()) return;

            String text = newVal != null ? newVal.trim() : "";
            if (text.isEmpty()) {
                textArea.getStyleClass().remove("error-field");
                textArea.setTooltip(null);
                return;
            }

            Pattern pattern = Pattern.compile(regex);
            if (!pattern.matcher(text).matches()) {
                textArea.getStyleClass().add("error-field");
                Tooltip tooltip = new Tooltip(errorMessage);
                textArea.setTooltip(tooltip);
            } else {
                textArea.getStyleClass().remove("error-field");
                textArea.setTooltip(null);
            }
        });
    }

    private void validateFieldOnBlur(TextField textField, String regex, String errorMessage) {
        String text = textField.getText() != null ? textField.getText().trim() : "";
        if (text.isEmpty()) {
            textField.getStyleClass().remove("error-field");
            textField.setTooltip(null);
            return;
        }

        Pattern pattern = Pattern.compile(regex);
        if (!pattern.matcher(text).matches()) {
            textField.getStyleClass().add("error-field");
            Tooltip tooltip = new Tooltip(errorMessage);
            textField.setTooltip(tooltip);

            // Optional: Show alert for critical errors
            if (textField == txtEmail || textField == txtUsername) {
                showValidationAlert("Validasi Gagal", errorMessage);
            }
        } else {
            textField.getStyleClass().remove("error-field");
            textField.setTooltip(null);
        }
    }

    private void validateFieldOnBlur(TextArea textArea, String regex, String errorMessage) {
        String text = textArea.getText() != null ? textArea.getText().trim() : "";
        if (text.isEmpty()) {
            textArea.getStyleClass().remove("error-field");
            textArea.setTooltip(null);
            return;
        }

        Pattern pattern = Pattern.compile(regex);
        if (!pattern.matcher(text).matches()) {
            textArea.getStyleClass().add("error-field");
            Tooltip tooltip = new Tooltip(errorMessage);
            textArea.setTooltip(tooltip);
        } else {
            textArea.getStyleClass().remove("error-field");
            textArea.setTooltip(null);
        }
    }

    private void showValidationAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

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
            currentPage = 1; // Reset to first page when searching
            updatePaginationData();
            updateInfoData();
        });
    }

    private void setupRowSelectionListener() {
        tblPegawai.getSelectionModel().selectedItemProperty().addListener((obs, oldSel, newSel) -> {
            if (newSel != null) {
                isiFormDariTabel(newSel);
                // Enable Ubah and Hapus buttons when a row is selected
                btnUbah.setDisable(false);
                btnHapus.setDisable(false);
            } else {
                btnUbah.setDisable(true);
                btnHapus.setDisable(true);
            }
        });
    }

    private void setupButtonListeners() {
        // Ubah button - now properly enabled
        btnUbah.setOnAction(this::handleUbahData);
        // Hapus button - now properly enabled
        btnHapus.setOnAction(this::handleHapusData);
        // Batal button - disable Ubah/Hapus
        btnBatal.setOnAction(event -> {
            resetForm();
            btnUbah.setDisable(true);
            btnHapus.setDisable(true);
        });
    }

    // =========================================================
    // STATUS SORTING - NonAktif always at bottom
    // =========================================================
    private void setupStatusSorting() {
        // Add comparator to sort status: aktif first, then NonAktif
        Comparator<PegawaiModel> statusComparator = (p1, p2) -> {
            String status1 = p1.getStatus();
            String status2 = p2.getStatus();

            // If both are null or empty, treat as equal
            if (status1 == null) status1 = "";
            if (status2 == null) status2 = "";

            // Define priority: "aktif" = 0, "NonAktif" = 1, others = 2
            int priority1 = getStatusPriority(status1);
            int priority2 = getStatusPriority(status2);

            // First sort by priority
            int result = Integer.compare(priority1, priority2);

            // If same priority, sort alphabetically
            if (result == 0) {
                result = status1.compareTo(status2);
            }

            return result;
        };

        // Apply comparator to filtered data
        filteredData.sorted(statusComparator);

        // Also apply to master data when new data is added
        masterData.sort(statusComparator);
    }

    private int getStatusPriority(String status) {
        if ("aktif".equalsIgnoreCase(status)) {
            return 0; // Highest priority (top)
        } else if ("NonAktif".equalsIgnoreCase(status)) {
            return 1; // Medium priority (bottom)
        } else {
            return 2; // Lowest priority (very bottom)
        }
    }

    // =========================================================
    // PAGINATION METHODS
    // =========================================================
    private void updatePaginationData() {
        if (filteredData == null) return;

        // Apply sorting to filtered data
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

        // Create a sorted list from filtered data
        ObservableList<PegawaiModel> sortedList = FXCollections.observableArrayList(filteredData);
        sortedList.sort(statusComparator);

        int totalItems = sortedList.size();
        totalPages = (int) Math.ceil((double) totalItems / ITEMS_PER_PAGE);
        if (totalPages == 0) totalPages = 1;

        // Ensure current page is valid
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
    // TAMPILKAN DATA (READ)
    // =========================================================
    private void tampilkanData() {
        masterData.clear();
        String query = "SELECT ID_Pegawai, Nama_Pegawai, Alamat, No_Telepon, Email, Username, Status_Pegawai " +
                "FROM v_TampilSemuaPegawai ORDER BY " +
                "CASE WHEN Status_Pegawai = 'aktif' THEN 0 " +
                "     WHEN Status_Pegawai = 'NonAktif' THEN 1 " +
                "     ELSE 2 END, Status_Pegawai, ID_Pegawai";

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

            // Apply sorting to master data
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
            currentPage = 1; // Reset to first page
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
        lblInfoData.setText("Menampilkan " + displayRange + " dari " + total + " data");
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
            cs.setString(7, "PGW"); // default role Pegawai biasa

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
            btnUbah.setDisable(true);
            btnHapus.setDisable(true);

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
            btnUbah.setDisable(true);
            btnHapus.setDisable(true);

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
        txtPassword.setPromptText("Masukan Password...");
        txtPasswordVisible.setPromptText("Masukan Password...");

        cmbStatus.setValue("aktif");
        cmbStatus.setDisable(true);

        btnSimpan.setDisable(false);

        tblPegawai.getSelectionModel().clearSelection();

        // Clear error styles
        clearErrorStyles();
    }

    private void clearErrorStyles() {
        txtNamaLengkap.getStyleClass().remove("error-field");
        txtEmail.getStyleClass().remove("error-field");
        txtNomorTelepon.getStyleClass().remove("error-field");
        txtAlamatLengkap.getStyleClass().remove("error-field");
        txtUsername.getStyleClass().remove("error-field");
        txtNamaLengkap.setTooltip(null);
        txtEmail.setTooltip(null);
        txtNomorTelepon.setTooltip(null);
        txtAlamatLengkap.setTooltip(null);
        txtUsername.setTooltip(null);
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
    // VALIDASI INPUT (Enhanced with field validation)
    // =========================================================
    private boolean validasiInput(boolean wajibPassword) {
        StringBuilder pesan = new StringBuilder();

        // Check each field with validation
        if (isKosong(txtNamaLengkap.getText())) {
            pesan.append("- Nama lengkap wajib diisi.\n");
        } else if (!txtNamaLengkap.getText().matches("^[a-zA-Z\\s]{3,100}$")) {
            pesan.append("- Nama lengkap harus 3-100 karakter huruf dan spasi.\n");
        }

        if (isKosong(txtEmail.getText())) {
            pesan.append("- Email wajib diisi.\n");
        } else if (!txtEmail.getText().matches("^[A-Za-z0-9+_.-]+@(.+)$")) {
            pesan.append("- Format email tidak valid (contoh: nama@domain.com).\n");
        }

        if (isKosong(txtNomorTelepon.getText())) {
            pesan.append("- Nomor telepon wajib diisi.\n");
        } else if (!txtNomorTelepon.getText().matches("^[0-9]{10,15}$")) {
            pesan.append("- Nomor telepon harus 10-15 digit angka.\n");
        }

        if (isKosong(txtAlamatLengkap.getText())) {
            pesan.append("- Alamat wajib diisi.\n");
        } else if (!txtAlamatLengkap.getText().matches("^.{5,500}$")) {
            pesan.append("- Alamat harus minimal 5 karakter.\n");
        }

        if (isKosong(txtUsername.getText())) {
            pesan.append("- Username wajib diisi.\n");
        } else if (!txtUsername.getText().matches("^[a-zA-Z0-9_]{4,20}$")) {
            pesan.append("- Username harus 4-20 karakter (huruf, angka, underscore).\n");
        }

        if (wajibPassword && isKosong(getPasswordText())) {
            pesan.append("- Password wajib diisi.\n");
        }

        if (pesan.length() > 0) {
            tampilkanAlert(Alert.AlertType.WARNING, "Data belum lengkap atau tidak valid", pesan.toString());
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