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
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Comparator;

public class DataSupplier {

    @FXML private Button btnBatal;
    @FXML private Button btnHapus;
    @FXML private Button btnNextPage;
    @FXML private Button btnPage1;
    @FXML private Button btnPrevPage;
    @FXML private Button btnSimpan;
    @FXML private Button btnUbah;

    @FXML private TableColumn<SupplierModel, String> colAlamat;
    @FXML private TableColumn<SupplierModel, String> colEmail;
    @FXML private TableColumn<SupplierModel, String> colIdSupplier;
    @FXML private TableColumn<SupplierModel, String> colNamaSupplier;
    @FXML private TableColumn<SupplierModel, String> colNoTelepon;
    @FXML private TableColumn<SupplierModel, String> colStatus;

    // ERROR LABELS
    @FXML private Label lblErrorNama;
    @FXML private Label lblErrorEmail;
    @FXML private Label lblErrorTelepon;
    @FXML private Label lblErrorAlamat;

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

    private DBConnection db = new DBConnection();

    private int currentPage = 1;
    private final int rowsPerPage = 10;
    private int totalPages = 1;

    private final ObservableList<SupplierModel> masterData = FXCollections.observableArrayList();
    private FilteredList<SupplierModel> filteredData;

    private enum Mode { TAMBAH, UBAH }
    private Mode mode = Mode.TAMBAH;

    // =========================================================
    // INITIALIZE
    // =========================================================
    @FXML
    public void initialize() {
        setupTableColumns();
        setupSearchListener();
        setupRowSelectionListener();
        setupButtonListeners();
        setupInputValidation();
        loadData();
        updateDashboard();
        resetForm();
        updatePaginationButtons();

        btnUbah.setDisable(true);
        btnHapus.setDisable(true);

        // Sort status
        setupStatusSorting();
    }

    // =========================================================
    // TABLE SETUP
    // =========================================================
    private void setupTableColumns() {
        colIdSupplier.setCellValueFactory(data -> data.getValue().idSupplierProperty());
        colNamaSupplier.setCellValueFactory(data -> data.getValue().namaSupplierProperty());
        colAlamat.setCellValueFactory(data -> data.getValue().alamatProperty());
        colNoTelepon.setCellValueFactory(data -> data.getValue().noTeleponProperty());
        colEmail.setCellValueFactory(data -> data.getValue().emailProperty());
        colStatus.setCellValueFactory(data -> data.getValue().statusProperty());
    }

    private void setupSearchListener() {
        txtCari.textProperty().addListener((obs, oldVal, newVal) -> {
            if (filteredData == null) return;
            String keyword = newVal == null ? "" : newVal.trim().toLowerCase();
            filteredData.setPredicate(supplier ->
                    keyword.isEmpty() ||
                            supplier.getNamaSupplier().toLowerCase().contains(keyword) ||
                            supplier.getIdSupplier().toLowerCase().contains(keyword) ||
                            supplier.getEmail().toLowerCase().contains(keyword)
            );
            currentPage = 1;
            updatePaginationData();
            updateInfoData();
        });
    }

    private void setupRowSelectionListener() {
        tblSupplier.getSelectionModel().selectedItemProperty().addListener((obs, oldSel, newSel) -> {
            if (newSel != null) {
                isiFormDariTabel(newSel);
                btnUbah.setDisable(false);
                btnHapus.setDisable(false);
            } else {
                btnUbah.setDisable(true);
                btnHapus.setDisable(true);
            }
        });
    }

    private void setupButtonListeners() {
        btnUbah.setOnAction(this::handleUbahData);
        btnHapus.setOnAction(this::handleHapusData);
        btnBatal.setOnAction(event -> {
            resetForm();
            btnUbah.setDisable(true);
            btnHapus.setDisable(true);
        });
    }

    // =========================================================
    // STATUS SORTING
    // =========================================================
    private void setupStatusSorting() {
        Comparator<SupplierModel> statusComparator = (p1, p2) -> {
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
    // INPUT VALIDATION - FINAL
    // =========================================================
    private void setupInputValidation() {
        // 1. NAMA SUPPLIER - Hanya huruf dan spasi, minimal 4 karakter
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
    }

    // =========================================================
    // CHECK INPUT ERRORS
    // =========================================================
    private boolean checkInputErrors() {
        boolean hasError = false;

        // Cek Nama Supplier
        String nama = txtNamaSupplier.getText();
        if (!nama.isEmpty() && nama.length() < 4) {
            txtNamaSupplier.setStyle("-fx-border-color: red; -fx-border-width: 2px;");
            showErrorLabel(lblErrorNama, "Nama lengkap minimal 4 karakter");
            hasError = true;
        } else if (!nama.isEmpty() && !nama.matches("^[a-zA-Z\\s]+$")) {
            txtNamaSupplier.setStyle("-fx-border-color: red; -fx-border-width: 2px;");
            showErrorLabel(lblErrorNama, "Nama hanya boleh berisi huruf dan spasi");
            hasError = true;
        } else {
            txtNamaSupplier.setStyle(null);
            hideErrorLabel(lblErrorNama);
        }

        // Cek Email
        String email = txtEmail.getText();
        if (!email.isEmpty() && !email.matches("^[a-z0-9@._-]+$")) {
            txtEmail.setStyle("-fx-border-color: red; -fx-border-width: 2px;");
            showErrorLabel(lblErrorEmail, "Email hanya boleh huruf kecil, angka, @, ., _, -");
            hasError = true;
        } else {
            txtEmail.setStyle(null);
            hideErrorLabel(lblErrorEmail);
        }

        // Cek Telepon
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

        // Cek Alamat
        String alamat = txtAlamatLengkap.getText();
        if (!alamat.isEmpty() && !alamat.matches("^[a-zA-Z0-9\\s.]+$")) {
            txtAlamatLengkap.setStyle("-fx-border-color: red; -fx-border-width: 2px;");
            showErrorLabel(lblErrorAlamat, "Alamat hanya boleh huruf, angka, spasi, dan titik");
            hasError = true;
        } else {
            txtAlamatLengkap.setStyle(null);
            hideErrorLabel(lblErrorAlamat);
        }

        return hasError;
    }

    // =========================================================
    // ERROR LABEL HELPERS
    // =========================================================
    private void hideAllErrorLabels() {
        if (lblErrorNama != null) { lblErrorNama.setVisible(false); lblErrorNama.setText(""); }
        if (lblErrorEmail != null) { lblErrorEmail.setVisible(false); lblErrorEmail.setText(""); }
        if (lblErrorTelepon != null) { lblErrorTelepon.setVisible(false); lblErrorTelepon.setText(""); }
        if (lblErrorAlamat != null) { lblErrorAlamat.setVisible(false); lblErrorAlamat.setText(""); }
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
    // LOAD DATA & PAGINATION
    // =========================================================
    private void loadData() {
        masterData.clear();
        String query = "SELECT ID_Supplier, Nama_Supplier, Alamat, No_Telepon, Email, Status_Supplier " +
                "FROM Supplier ORDER BY " +
                "CASE WHEN Status_Supplier = 'aktif' THEN 0 " +
                "     WHEN Status_Supplier = 'NonAktif' THEN 1 " +
                "     ELSE 2 END, Status_Supplier, ID_Supplier";

        try (PreparedStatement ps = db.getConnection().prepareStatement(query);
             ResultSet rs = ps.executeQuery()) {

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

            Comparator<SupplierModel> statusComparator = (p1, p2) -> {
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
            showAlert(Alert.AlertType.ERROR, "Gagal memuat data", e.getMessage());
        }
    }

    private void updatePaginationData() {
        if (filteredData == null) return;

        Comparator<SupplierModel> statusComparator = (p1, p2) -> {
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

        ObservableList<SupplierModel> sortedList = FXCollections.observableArrayList(filteredData);
        sortedList.sort(statusComparator);

        int totalItems = sortedList.size();
        totalPages = (int) Math.ceil((double) totalItems / rowsPerPage);
        if (totalPages == 0) totalPages = 1;

        if (currentPage < 1) currentPage = 1;
        if (currentPage > totalPages) currentPage = totalPages;

        int fromIndex = (currentPage - 1) * rowsPerPage;
        int toIndex = Math.min(fromIndex + rowsPerPage, totalItems);

        if (totalItems == 0) {
            tblSupplier.setItems(FXCollections.observableArrayList());
        } else {
            ObservableList<SupplierModel> pageData = FXCollections.observableArrayList(
                    sortedList.subList(fromIndex, toIndex)
            );
            tblSupplier.setItems(pageData);
        }

        updatePaginationButtons();
        updateInfoData();
    }

    private void updatePaginationButtons() {
        btnPage1.setText("Halaman " + currentPage + " dari " + totalPages);
        btnPrevPage.setDisable(currentPage <= 1);
        btnNextPage.setDisable(currentPage >= totalPages);
    }

    private void updateInfoData() {
        int total = filteredData == null ? 0 : filteredData.size();
        int start = (currentPage - 1) * rowsPerPage + 1;
        int end = Math.min(start + rowsPerPage - 1, total);
        String displayRange = total == 0 ? "0" : start + "-" + end;
        lblInfoData.setText("Menampilkan " + displayRange + " dari " + total + " data");
    }

    // =========================================================
    // DASHBOARD
    // =========================================================
    private void updateDashboard() {
        String query = "SELECT " +
                "COUNT(*) AS Total, " +
                "SUM(CASE WHEN Status_Supplier = 'aktif' THEN 1 ELSE 0 END) AS Aktif, " +
                "SUM(CASE WHEN Status_Supplier = 'NonAktif' THEN 1 ELSE 0 END) AS Nonaktif " +
                "FROM Supplier";

        try (PreparedStatement ps = db.getConnection().prepareStatement(query);
             ResultSet rs = ps.executeQuery()) {

            if (rs.next()) {
                lblTotalSupplier.setText(String.valueOf(rs.getInt("Total")));
                lblSupplierAktif.setText(String.valueOf(rs.getInt("Aktif")));
                lblSupplierNonaktif.setText(String.valueOf(rs.getInt("Nonaktif")));
            }
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Gagal menghitung dashboard", e.getMessage());
        }
    }

    // =========================================================
    // GENERATE ID
    // =========================================================
    private void generateIdOtomatis() {
        String query = "SELECT COALESCE(MAX(CAST(SUBSTRING(ID_Supplier, 4, LEN(ID_Supplier)) AS INT)), 0) AS MaxID " +
                "FROM Supplier WHERE ID_Supplier LIKE 'SPR%'";

        try (PreparedStatement ps = db.getConnection().prepareStatement(query);
             ResultSet rs = ps.executeQuery()) {

            if (rs.next()) {
                int maxId = rs.getInt("MaxID");
                int newId = maxId + 1;
                txtIdSupplier.setText("SPR" + String.format("%03d", newId));
            }
        } catch (SQLException e) {
            txtIdSupplier.setText("SPR001");
        }
    }

    // =========================================================
    // ISI FORM DARI TABEL
    // =========================================================
    private void isiFormDariTabel(SupplierModel supplier) {
        mode = Mode.UBAH;

        txtIdSupplier.setText(supplier.getIdSupplier());
        txtNamaSupplier.setText(supplier.getNamaSupplier());
        txtAlamatLengkap.setText(supplier.getAlamat());
        txtNomorTelepon.setText(supplier.getNoTelepon());
        txtEmail.setText(supplier.getEmail());

        btnSimpan.setDisable(true);

        hideAllErrorLabels();
    }

    // =========================================================
    // BUTTON HANDLERS
    // =========================================================
    @FXML
    void handleNextPage(ActionEvent event) {
        if (currentPage < totalPages) {
            currentPage++;
            updatePaginationData();
        }
    }

    @FXML
    void handlePrevPage(ActionEvent event) {
        if (currentPage > 1) {
            currentPage--;
            updatePaginationData();
        }
    }

    @FXML
    void handleSimpanData(ActionEvent event) {
        if (checkInputErrors()) {
            showAlert(Alert.AlertType.WARNING, "Validasi Gagal", "Mohon perbaiki input yang ditandai merah");
            return;
        }

        if (!validasiInput()) return;

        // Cek duplikat
        String queryCek = "SELECT COUNT(*) FROM Supplier WHERE Nama_Supplier = ? OR Email = ? OR No_Telepon = ?";
        try (PreparedStatement ps = db.getConnection().prepareStatement(queryCek)) {
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

        String query = "{call sp_TambahSupplier(?,?,?,?)}";

        try (CallableStatement cs = db.getConnection().prepareCall(query)) {
            cs.setString(1, txtNamaSupplier.getText().trim());
            cs.setString(2, txtAlamatLengkap.getText().trim());
            cs.setString(3, txtNomorTelepon.getText().trim());
            cs.setString(4, txtEmail.getText().trim());

            cs.execute();

            showAlert(Alert.AlertType.INFORMATION, "Sukses", "Supplier baru berhasil ditambahkan!");
            loadData();
            updateDashboard();
            resetForm();
            btnUbah.setDisable(true);
            btnHapus.setDisable(true);

        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Gagal menyimpan data", e.getMessage());
        }
    }

    @FXML
    void handleUbahData(ActionEvent event) {
        if (txtIdSupplier.getText() == null || txtIdSupplier.getText().isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Peringatan", "Pilih dulu data supplier dari tabel yang mau diubah!");
            return;
        }

        // Cek jika status NonAktif, tidak bisa diubah
        SupplierModel selected = tblSupplier.getSelectionModel().getSelectedItem();
        if (selected != null && "NonAktif".equalsIgnoreCase(selected.getStatus())) {
            showAlert(Alert.AlertType.WARNING, "Peringatan", "Supplier dengan status NonAktif tidak dapat diubah!");
            return;
        }

        if (checkInputErrors()) {
            showAlert(Alert.AlertType.WARNING, "Validasi Gagal", "Mohon perbaiki input yang ditandai merah");
            return;
        }

        if (!validasiInput()) return;

        String query = "{call sp_UpdateSupplier(?,?,?,?,?)}";

        try (CallableStatement cs = db.getConnection().prepareCall(query)) {
            cs.setString(1, txtIdSupplier.getText().trim());
            cs.setString(2, txtNamaSupplier.getText().trim());
            cs.setString(3, txtAlamatLengkap.getText().trim());
            cs.setString(4, txtNomorTelepon.getText().trim());
            cs.setString(5, txtEmail.getText().trim());

            cs.execute();

            showAlert(Alert.AlertType.INFORMATION, "Sukses", "Data supplier berhasil diubah.");
            loadData();
            updateDashboard();
            resetForm();
            btnUbah.setDisable(true);
            btnHapus.setDisable(true);

        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Gagal mengubah data", e.getMessage());
        }
    }

    @FXML
    void handleHapusData(ActionEvent event) {
        if (txtIdSupplier.getText() == null || txtIdSupplier.getText().isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Peringatan", "Pilih dulu data supplier dari tabel yang mau dihapus!");
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
            loadData();
            updateDashboard();
            resetForm();
            btnUbah.setDisable(true);
            btnHapus.setDisable(true);

        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Gagal menghapus data", e.getMessage());
        }
    }

    @FXML
    void handleBatal(ActionEvent event) {
        resetForm();
        btnUbah.setDisable(true);
        btnHapus.setDisable(true);
    }

    // =========================================================
    // RESET FORM
    // =========================================================
    private void resetForm() {
        mode = Mode.TAMBAH;

        txtIdSupplier.clear();
        txtNamaSupplier.clear();
        txtEmail.clear();
        txtNomorTelepon.clear();
        txtAlamatLengkap.clear();

        btnSimpan.setDisable(false);

        tblSupplier.getSelectionModel().clearSelection();

        hideAllErrorLabels();

        // Reset style
        txtNamaSupplier.setStyle(null);
        txtEmail.setStyle(null);
        txtNomorTelepon.setStyle(null);
        txtAlamatLengkap.setStyle(null);

        generateIdOtomatis();
    }

    // =========================================================
    // VALIDASI INPUT
    // =========================================================
    private boolean validasiInput() {
        StringBuilder pesan = new StringBuilder();

        // Validasi Nama Supplier
        if (isKosong(txtNamaSupplier.getText())) {
            pesan.append("- Nama supplier wajib diisi.\n");
            showErrorLabel(lblErrorNama, "Nama supplier wajib diisi");
        } else {
            String nama = txtNamaSupplier.getText().trim();
            if (nama.length() < 4) {
                pesan.append("- Nama supplier minimal 4 karakter.\n");
                showErrorLabel(lblErrorNama, "Nama supplier minimal 4 karakter");
            } else if (!nama.matches("^[a-zA-Z\\s]+$")) {
                pesan.append("- Nama hanya boleh berisi huruf dan spasi.\n");
                showErrorLabel(lblErrorNama, "Nama hanya boleh berisi huruf dan spasi");
            } else {
                hideErrorLabel(lblErrorNama);
            }
        }

        // Validasi Email
        if (isKosong(txtEmail.getText())) {
            pesan.append("- Email wajib diisi.\n");
            showErrorLabel(lblErrorEmail, "Email wajib diisi");
        } else {
            String email = txtEmail.getText().trim();
            if (!email.matches("^[a-z0-9@._-]+$")) {
                pesan.append("- Format email tidak valid (hanya huruf kecil, angka, @, ., _, -).\n");
                showErrorLabel(lblErrorEmail, "Format email tidak valid");
            } else {
                hideErrorLabel(lblErrorEmail);
            }
        }

        // Validasi Nomor Telepon
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
            } else {
                hideErrorLabel(lblErrorTelepon);
            }
        }

        // Validasi Alamat
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

        if (pesan.length() > 0) {
            showAlert(Alert.AlertType.WARNING, "Data belum lengkap atau tidak valid", pesan.toString());
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
    private void showAlert(Alert.AlertType tipe, String judul, String pesan) {
        Alert alert = new Alert(tipe);
        alert.setTitle(judul);
        alert.setHeaderText(null);
        alert.setContentText(pesan);
        alert.showAndWait();
    }

    // =========================================================
    // MODEL DATA SUPPLIER
    // =========================================================
    public static class SupplierModel {
        private final StringProperty idSupplier;
        private final StringProperty namaSupplier;
        private final StringProperty alamat;
        private final StringProperty noTelepon;
        private final StringProperty email;
        private final StringProperty status;

        public SupplierModel(String idSupplier, String namaSupplier, String alamat,
                             String noTelepon, String email, String status) {
            this.idSupplier = new SimpleStringProperty(idSupplier);
            this.namaSupplier = new SimpleStringProperty(namaSupplier);
            this.alamat = new SimpleStringProperty(alamat);
            this.noTelepon = new SimpleStringProperty(noTelepon);
            this.email = new SimpleStringProperty(email);
            this.status = new SimpleStringProperty(status);
        }

        public String getIdSupplier() { return idSupplier.get(); }
        public StringProperty idSupplierProperty() { return idSupplier; }

        public String getNamaSupplier() { return namaSupplier.get(); }
        public StringProperty namaSupplierProperty() { return namaSupplier; }

        public String getAlamat() { return alamat.get(); }
        public StringProperty alamatProperty() { return alamat; }

        public String getNoTelepon() { return noTelepon.get(); }
        public StringProperty noTeleponProperty() { return noTelepon; }

        public String getEmail() { return email.get(); }
        public StringProperty emailProperty() { return email; }

        public String getStatus() { return status.get(); }
        public StringProperty statusProperty() { return status; }
    }
}