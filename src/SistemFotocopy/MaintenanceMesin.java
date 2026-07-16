package SistemFotocopy;

import Database.DBConnection;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.GridPane;

import java.net.URL;
import java.sql.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ResourceBundle;

public class MaintenanceMesin implements Initializable {

    @FXML
    private Button btnBatal, btnNextPage, btnPage1, btnPrevPage, btnSimpan;

    @FXML
    private ComboBox<String> cbJenisKerusakan, cbMesin;

    @FXML
    private TableColumn<MaintenanceData, String> colAksi, colDeskripsi, colIdMaintenance,
            colIdMesin, colIdPegawai, colJenisKerusakan, colStatus,
            colTanggalMaintenance, colTanggalSelesai, colBiaya, colKeterangan;

    @FXML
    private DatePicker dpTanggalMaintenance;

    @FXML
    private Label lblInfoData, lblMesinAktif, lblMesinRusak, lblMesinSelesai, lblTotalMesin;

    @FXML
    private TableView<MaintenanceData> tblMaintenance;

    @FXML
    private TextField txtCari, txtIdMaintenance, txtIdMaintenance1;

    @FXML
    private TextArea txtDeskripsi;

    private Connection conn;
    private ObservableList<MaintenanceData> maintenanceDataList = FXCollections.observableArrayList();
    private ObservableList<String> mesinList = FXCollections.observableArrayList();

    private static final String[] JENIS_KERUSAKAN = {
            "rusak_berat", "rusak_ringan", "perbaikan_rutin",
            "service_berkala", "gangguan_fungsi", "komponen_aus", "error_sistem"
    };

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        conn = DBConnection.getConnection();

        generateIdMaintenance();
        setPegawaiFromSession();

        dpTanggalMaintenance.setValue(LocalDate.now());
        dpTanggalMaintenance.setEditable(false);

        cbJenisKerusakan.setItems(FXCollections.observableArrayList(JENIS_KERUSAKAN));

        loadMesinData();
        setupTableColumns();
        loadMaintenanceData();
        updateStatisticsCards();
        setupValidations();
        setupSearch();

        btnSimpan.setOnAction(e -> simpanData());
        btnBatal.setOnAction(e -> clearForm());
    }

    private void generateIdMaintenance() {
        String id = "MMS" + String.format("%03d", getNextId());
        txtIdMaintenance.setText(id);
    }

    private int getNextId() {
        int nextId = 1;
        String query = "SELECT MAX(CAST(SUBSTRING(ID_Maintenance_Mesin, 4, LEN(ID_Maintenance_Mesin)) AS INT)) AS MaxID FROM Maintenance_Mesin";
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {
            if (rs.next()) {
                nextId = rs.getInt("MaxID") + 1;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return nextId;
    }

    private void setPegawaiFromSession() {
        UserSession session = UserSession.getInstance();
        String idPegawai = session.getIdPegawai();
        if (idPegawai != null && !idPegawai.isEmpty()) {
            String namaPegawai = getNamaPegawai(idPegawai);
            txtIdMaintenance1.setText(idPegawai + " - " + namaPegawai);
        } else {
            txtIdMaintenance1.setText("ID Pegawai Tidak Ditemukan");
        }
    }

    private String getNamaPegawai(String idPegawai) {
        String nama = "";
        String query = "SELECT Nama_Pegawai FROM Pegawai WHERE ID_Pegawai = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setString(1, idPegawai);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    nama = rs.getString("Nama_Pegawai");
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return nama;
    }

    private void loadMesinData() {
        mesinList.clear();
        String query = "SELECT ID_Mesin, Nama_Mesin FROM Mesin WHERE Status_Mesin = 'Aktif'";
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {
            while (rs.next()) {
                String idMesin = rs.getString("ID_Mesin");
                String namaMesin = rs.getString("Nama_Mesin");
                mesinList.add(idMesin + " - " + namaMesin);
            }
            cbMesin.setItems(mesinList);
        } catch (SQLException e) {
            e.printStackTrace();
            showAlert("Error", "Gagal memuat data mesin: " + e.getMessage());
        }
    }

    private void setupTableColumns() {
        colIdMaintenance.setCellValueFactory(new PropertyValueFactory<>("idMaintenance"));
        colIdMesin.setCellValueFactory(new PropertyValueFactory<>("idMesin"));
        colIdPegawai.setCellValueFactory(new PropertyValueFactory<>("idPegawai"));
        colJenisKerusakan.setCellValueFactory(new PropertyValueFactory<>("jenisKerusakan"));
        colDeskripsi.setCellValueFactory(new PropertyValueFactory<>("deskripsi"));
        colTanggalMaintenance.setCellValueFactory(new PropertyValueFactory<>("tanggalMaintenance"));
        colTanggalSelesai.setCellValueFactory(new PropertyValueFactory<>("tanggalSelesai"));
        colBiaya.setCellValueFactory(new PropertyValueFactory<>("biayaFormatted"));
        colKeterangan.setCellValueFactory(new PropertyValueFactory<>("keterangan"));
        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));

        colAksi.setCellValueFactory(new PropertyValueFactory<>("aksi"));

        colAksi.setCellFactory(col -> new TableCell<MaintenanceData, String>() {
            private final Button btnSelesai = new Button("Selesai");

            {
                btnSelesai.setStyle(
                        "-fx-background-color: #10B981; " +
                                "-fx-text-fill: white; " +
                                "-fx-font-weight: bold; " +
                                "-fx-font-size: 11px; " +
                                "-fx-padding: 4 10; " +
                                "-fx-background-radius: 4; " +
                                "-fx-cursor: hand;"
                );
                btnSelesai.setOnAction(event -> {
                    MaintenanceData data = getTableView().getItems().get(getIndex());
                    if (data != null && !"selesai".equals(data.getStatus())) {
                        showPopupSelesai(data);
                    }
                });
            }

            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null || "-".equals(item)) {
                    setGraphic(null);
                    setText(null);
                } else {
                    setGraphic(btnSelesai);
                    setText(null);
                }
            }
        });
    }

    private void loadMaintenanceData() {
        maintenanceDataList.clear();
        String query =
                "SELECT " +
                        "    M.ID_Maintenance_Mesin, " +
                        "    (M.ID_Mesin + ' - ' + ME.Nama_Mesin) AS Mesin, " +
                        "    M.ID_Mesin, " +
                        "    M.ID_Pegawai, " +
                        "    M.Jenis_Kerusakan_Mesin, " +
                        "    M.Deskripsi_Kerusakan, " +
                        "    M.Tanggal_Maintenance_Mesin, " +
                        "    M.Tanggal_Selesai_Mesin, " +
                        "    M.Biaya_Maintenance_Mesin, " +
                        "    M.Keterangan_Perbaikan, " +
                        "    M.Status_Maintenance, " +
                        "    P.Nama_Pegawai " +
                        "FROM Maintenance_Mesin M " +
                        "INNER JOIN Mesin ME ON M.ID_Mesin = ME.ID_Mesin " +
                        "INNER JOIN Pegawai P ON M.ID_Pegawai = P.ID_Pegawai " +
                        "ORDER BY M.Tanggal_Maintenance_Mesin DESC";

        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {
            while (rs.next()) {
                MaintenanceData data = new MaintenanceData(
                        rs.getString("ID_Maintenance_Mesin"),
                        rs.getString("Mesin"),
                        rs.getString("ID_Pegawai"),
                        rs.getString("Jenis_Kerusakan_Mesin"),
                        rs.getString("Deskripsi_Kerusakan"),
                        rs.getDate("Tanggal_Maintenance_Mesin") != null ?
                                rs.getDate("Tanggal_Maintenance_Mesin").toLocalDate() : null,
                        rs.getDate("Tanggal_Selesai_Mesin") != null ?
                                rs.getDate("Tanggal_Selesai_Mesin").toLocalDate() : null,
                        rs.getString("Status_Maintenance"),
                        rs.getDouble("Biaya_Maintenance_Mesin"),
                        rs.getString("Keterangan_Perbaikan"),
                        rs.getString("Nama_Pegawai")
                );
                maintenanceDataList.add(data);
            }
            tblMaintenance.setItems(maintenanceDataList);
            updatePaginationInfo();
        } catch (SQLException e) {
            e.printStackTrace();
            showAlert("Error", "Gagal memuat data maintenance: " + e.getMessage());
        }
    }

    private void updateStatisticsCards() {
        try {
            String queryTotal = "SELECT COUNT(*) AS Total FROM Mesin";
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(queryTotal)) {
                if (rs.next()) {
                    lblTotalMesin.setText(String.valueOf(rs.getInt("Total")));
                }
            }

            String queryAktif = "SELECT COUNT(*) AS Total FROM Mesin WHERE Status_Mesin = 'Aktif'";
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(queryAktif)) {
                if (rs.next()) {
                    lblMesinAktif.setText(String.valueOf(rs.getInt("Total")));
                }
            }

            String queryRusak = "SELECT COUNT(*) AS Total FROM Maintenance_Mesin WHERE Status_Maintenance = 'rusak'";
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(queryRusak)) {
                if (rs.next()) {
                    lblMesinRusak.setText(String.valueOf(rs.getInt("Total")));
                }
            }

            String querySelesai = "SELECT COUNT(*) AS Total FROM Maintenance_Mesin WHERE Status_Maintenance = 'selesai'";
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(querySelesai)) {
                if (rs.next()) {
                    lblMesinSelesai.setText(String.valueOf(rs.getInt("Total")));
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void setupValidations() {
        txtDeskripsi.textProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal.matches("[a-zA-Z0-9 .,!?]*")) {
                txtDeskripsi.setText(newVal.replaceAll("[^a-zA-Z0-9 .,!?]", ""));
            }
        });
    }

    private void setupSearch() {
        txtCari.textProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal == null || newVal.isEmpty()) {
                loadMaintenanceData();
                return;
            }
            searchMaintenance(newVal);
        });
    }

    private void searchMaintenance(String keyword) {
        maintenanceDataList.clear();
        String query =
                "SELECT " +
                        "    M.ID_Maintenance_Mesin, " +
                        "    (M.ID_Mesin + ' - ' + ME.Nama_Mesin) AS Mesin, " +
                        "    M.ID_Mesin, " +
                        "    M.ID_Pegawai, " +
                        "    M.Jenis_Kerusakan_Mesin, " +
                        "    M.Deskripsi_Kerusakan, " +
                        "    M.Tanggal_Maintenance_Mesin, " +
                        "    M.Tanggal_Selesai_Mesin, " +
                        "    M.Biaya_Maintenance_Mesin, " +
                        "    M.Keterangan_Perbaikan, " +
                        "    M.Status_Maintenance, " +
                        "    P.Nama_Pegawai " +
                        "FROM Maintenance_Mesin M " +
                        "INNER JOIN Mesin ME ON M.ID_Mesin = ME.ID_Mesin " +
                        "INNER JOIN Pegawai P ON M.ID_Pegawai = P.ID_Pegawai " +
                        "WHERE M.ID_Maintenance_Mesin LIKE ? OR (M.ID_Mesin + ' - ' + ME.Nama_Mesin) LIKE ?";

        try (PreparedStatement pstmt = conn.prepareStatement(query)) {
            String searchPattern = "%" + keyword + "%";
            pstmt.setString(1, searchPattern);
            pstmt.setString(2, searchPattern);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    MaintenanceData data = new MaintenanceData(
                            rs.getString("ID_Maintenance_Mesin"),
                            rs.getString("Mesin"),
                            rs.getString("ID_Pegawai"),
                            rs.getString("Jenis_Kerusakan_Mesin"),
                            rs.getString("Deskripsi_Kerusakan"),
                            rs.getDate("Tanggal_Maintenance_Mesin") != null ?
                                    rs.getDate("Tanggal_Maintenance_Mesin").toLocalDate() : null,
                            rs.getDate("Tanggal_Selesai_Mesin") != null ?
                                    rs.getDate("Tanggal_Selesai_Mesin").toLocalDate() : null,
                            rs.getString("Status_Maintenance"),
                            rs.getDouble("Biaya_Maintenance_Mesin"),
                            rs.getString("Keterangan_Perbaikan"),
                            rs.getString("Nama_Pegawai")
                    );
                    maintenanceDataList.add(data);
                }
                tblMaintenance.setItems(maintenanceDataList);
                updatePaginationInfo();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void simpanData() {
        if (!validateForm()) {
            return;
        }

        try {
            String mesinSelected = cbMesin.getValue();
            String idMesin = mesinSelected.split(" - ")[0];

            String idPegawai = txtIdMaintenance1.getText().split(" - ")[0];
            String jenisKerusakan = cbJenisKerusakan.getValue();
            String deskripsi = txtDeskripsi.getText();
            LocalDate tanggalMaintenance = dpTanggalMaintenance.getValue();
            // Status otomatis 'rusak'
            String status = "rusak";

            String sql = "{call sp_TambahMaintenanceMesin(?, ?, ?, ?, ?, ?, ?, ?, ?, ?)}";
            try (CallableStatement cstmt = conn.prepareCall(sql)) {
                cstmt.setString(1, idMesin);
                cstmt.setString(2, idPegawai);
                cstmt.setString(3, jenisKerusakan);
                cstmt.setString(4, deskripsi);
                cstmt.setDate(5, Date.valueOf(tanggalMaintenance));
                cstmt.setDate(6, null);
                cstmt.setDouble(7, 0);
                cstmt.setString(8, null);
                cstmt.setString(9, null);
                cstmt.setString(10, status);

                cstmt.execute();

                showAlert("Sukses", "Data Maintenance berhasil disimpan!");
                clearForm();
                loadMaintenanceData();
                updateStatisticsCards();
                generateIdMaintenance();
            }
        } catch (SQLException e) {
            e.printStackTrace();
            showAlert("Error", "Gagal menyimpan data: " + e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Error", "Terjadi kesalahan: " + e.getMessage());
        }
    }

    private boolean validateForm() {
        if (cbMesin.getValue() == null || cbMesin.getValue().isEmpty()) {
            showAlert("Error", "Silakan pilih ID Mesin!");
            return false;
        }

        if (cbJenisKerusakan.getValue() == null || cbJenisKerusakan.getValue().isEmpty()) {
            showAlert("Error", "Silakan pilih Jenis Kerusakan!");
            return false;
        }

        if (txtDeskripsi.getText() == null || txtDeskripsi.getText().trim().isEmpty()) {
            showAlert("Error", "Deskripsi Kerusakan harus diisi!");
            return false;
        }

        if (dpTanggalMaintenance.getValue() == null) {
            showAlert("Error", "Tanggal Maintenance harus diisi!");
            return false;
        }

        return true;
    }

    private void clearForm() {
        cbMesin.setValue(null);
        cbJenisKerusakan.setValue(null);
        txtDeskripsi.clear();
        dpTanggalMaintenance.setValue(LocalDate.now());
        generateIdMaintenance();
    }

    private void updatePaginationInfo() {
        int total = maintenanceDataList.size();
        lblInfoData.setText("Menampilkan " + total + " dari " + total + " data");
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showPopupSelesai(MaintenanceData data) {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Selesaikan Maintenance");
        dialog.setHeaderText("Form Penyelesaian Maintenance - " + data.getIdMaintenance());

        ButtonType btnSelesai = new ButtonType("Selesai", ButtonBar.ButtonData.OK_DONE);
        ButtonType btnBatal = new ButtonType("Batal", ButtonBar.ButtonData.CANCEL_CLOSE);
        dialog.getDialogPane().getButtonTypes().addAll(btnSelesai, btnBatal);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(12);
        grid.setPadding(new Insets(20, 20, 20, 20));
        grid.setPrefWidth(500);

        // Field - Semua readonly/disable
        TextField txtIdMaintenancePopup = new TextField(data.getIdMaintenance());
        txtIdMaintenancePopup.setDisable(true);
        txtIdMaintenancePopup.setStyle("-fx-background-color: #f0f0f0;");

        TextField txtMesinPopup = new TextField(data.getIdMesin());
        txtMesinPopup.setDisable(true);
        txtMesinPopup.setStyle("-fx-background-color: #f0f0f0;");

        TextField txtPegawaiPopup = new TextField(data.getNamaPegawai() != null ? data.getNamaPegawai() : data.getIdPegawai());
        txtPegawaiPopup.setDisable(true);
        txtPegawaiPopup.setStyle("-fx-background-color: #f0f0f0;");

        TextField txtJenisPopup = new TextField(data.getJenisKerusakan());
        txtJenisPopup.setDisable(true);
        txtJenisPopup.setStyle("-fx-background-color: #f0f0f0;");

        TextArea txtDeskripsiPopup = new TextArea(data.getDeskripsi());
        txtDeskripsiPopup.setDisable(true);
        txtDeskripsiPopup.setStyle("-fx-background-color: #f0f0f0;");
        txtDeskripsiPopup.setPrefHeight(60);
        txtDeskripsiPopup.setWrapText(true);

        TextField txtTanggalMainPopup = new TextField(data.getTanggalMaintenance() != null ?
                data.getTanggalMaintenance().format(FORMATTER) : "");
        txtTanggalMainPopup.setDisable(true);
        txtTanggalMainPopup.setStyle("-fx-background-color: #f0f0f0;");

        DatePicker dpTanggalSelesaiPopup = new DatePicker(LocalDate.now());
        dpTanggalSelesaiPopup.setEditable(false);
        dpTanggalSelesaiPopup.setStyle("-fx-background-color: white;");

        // ==========================================
        // FIELD BIAYA - OTOMATIS FORMAT Rp DAN TITIK
        // ==========================================
        TextField txtBiayaPopup = new TextField();
        txtBiayaPopup.setPromptText("Masukan Biaya...");
        txtBiayaPopup.setStyle("-fx-background-color: white; -fx-alignment: center-right;");

        // Listener untuk format biaya otomatis
        txtBiayaPopup.textProperty().addListener((obs, oldVal, newVal) -> {
            // Hanya angka
            String clean = newVal.replaceAll("[^\\d]", "");

            if (clean.isEmpty()) {
                txtBiayaPopup.setText("");
                return;
            }

            // Ubah ke Long untuk menghindari overflow
            long number = Long.parseLong(clean);

            // Format dengan titik ribuan dan tambahan "Rp "
            String formatted = "Rp " + String.format("%,d", number).replace(',', '.');

            // Set text tanpa memicu listener berulang
            txtBiayaPopup.setText(formatted);

            // Posisi kursor di akhir
            txtBiayaPopup.positionCaret(formatted.length());
        });

        // Keterangan
        TextArea txtKeteranganPopup = new TextArea();
        txtKeteranganPopup.setPromptText("Masukan Keterangan...");
        txtKeteranganPopup.setPrefHeight(60);
        txtKeteranganPopup.setWrapText(true);
        txtKeteranganPopup.setStyle("-fx-background-color: white;");
        txtKeteranganPopup.textProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal.matches("[a-zA-Z0-9 .,!?]*")) {
                txtKeteranganPopup.setText(newVal.replaceAll("[^a-zA-Z0-9 .,!?]", ""));
            }
        });

        TextField txtStatusPopup = new TextField("selesai");
        txtStatusPopup.setDisable(true);
        txtStatusPopup.setStyle("-fx-background-color: #f0f0f0; -fx-text-fill: #10B981; -fx-font-weight: bold;");

        int row = 0;
        grid.add(new Label("ID Maintenance:"), 0, row);
        grid.add(txtIdMaintenancePopup, 1, row++);
        grid.add(new Label("ID Mesin:"), 0, row);
        grid.add(txtMesinPopup, 1, row++);
        grid.add(new Label("ID Pegawai:"), 0, row);
        grid.add(txtPegawaiPopup, 1, row++);
        grid.add(new Label("Jenis Kerusakan:"), 0, row);
        grid.add(txtJenisPopup, 1, row++);
        grid.add(new Label("Deskripsi:"), 0, row);
        grid.add(txtDeskripsiPopup, 1, row++);
        grid.add(new Label("Tanggal Maintenance:"), 0, row);
        grid.add(txtTanggalMainPopup, 1, row++);
        grid.add(new Label("Tanggal Selesai:"), 0, row);
        grid.add(dpTanggalSelesaiPopup, 1, row++);
        grid.add(new Label("Biaya Maintenance:"), 0, row);
        grid.add(txtBiayaPopup, 1, row++);
        grid.add(new Label("Keterangan:"), 0, row);
        grid.add(txtKeteranganPopup, 1, row++);
        grid.add(new Label("Status:"), 0, row);
        grid.add(txtStatusPopup, 1, row++);

        dialog.getDialogPane().setContent(grid);

        Button btnSelesaiButton = (Button) dialog.getDialogPane().lookupButton(btnSelesai);
        btnSelesaiButton.setDisable(true);

        // Validasi biaya (ambil angka bersih tanpa Rp dan titik)
        txtBiayaPopup.textProperty().addListener((obs, oldVal, newVal) -> {
            // Ambil angka bersih dari formatted text
            String cleanNumber = newVal.replaceAll("[^\\d]", "");
            long biayaValue = cleanNumber.isEmpty() ? 0 : Long.parseLong(cleanNumber);

            // Simpan angka bersih ke userData untuk validasi
            txtBiayaPopup.setUserData(biayaValue);

            validatePopupForm(txtBiayaPopup, txtKeteranganPopup, btnSelesaiButton);
        });

        txtKeteranganPopup.textProperty().addListener((obs, oldVal, newVal) -> {
            validatePopupForm(txtBiayaPopup, txtKeteranganPopup, btnSelesaiButton);
        });

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == btnSelesai) {
                try {
                    // Ambil angka bersih dari formatted text
                    String biayaText = txtBiayaPopup.getText();
                    String keteranganText = txtKeteranganPopup.getText();

                    if (biayaText == null || biayaText.isEmpty()) {
                        showAlert("Error", "Biaya Maintenance harus diisi!");
                        return null;
                    }

                    // Ambil angka bersih (hilangkan Rp dan titik)
                    String cleanBiaya = biayaText.replaceAll("[^\\d]", "");
                    double biaya = Double.parseDouble(cleanBiaya);

                    if (biaya < 1000) {
                        showAlert("Error", "Biaya Maintenance minimal Rp 1.000!");
                        return null;
                    }
                    if (biaya % 1000 != 0) {
                        showAlert("Error", "Biaya Maintenance harus kelipatan 1.000!");
                        return null;
                    }

                    if (keteranganText == null || keteranganText.trim().length() < 5) {
                        showAlert("Error", "Keterangan minimal 5 karakter!");
                        return null;
                    }

                    String idMaintenance = data.getIdMaintenance();
                    LocalDate tanggalSelesai = dpTanggalSelesaiPopup.getValue();

                    String sql = "{call sp_UpdateMaintenanceMesin(?, ?, ?, ?, ?)}";
                    try (CallableStatement cstmt = conn.prepareCall(sql)) {
                        cstmt.setString(1, idMaintenance);
                        cstmt.setDate(2, Date.valueOf(tanggalSelesai));
                        cstmt.setDouble(3, biaya);
                        cstmt.setString(4, null);
                        cstmt.setString(5, keteranganText);

                        cstmt.execute();
                        showAlert("Sukses", "Maintenance berhasil diselesaikan!");
                        loadMaintenanceData();
                        updateStatisticsCards();
                    }
                } catch (SQLException e) {
                    e.printStackTrace();
                    showAlert("Error", "Gagal menyelesaikan maintenance: " + e.getMessage());
                } catch (Exception e) {
                    e.printStackTrace();
                    showAlert("Error", "Terjadi kesalahan: " + e.getMessage());
                }
            }
            return null;
        });

        dialog.showAndWait();
    }

    private void validatePopupForm(TextField txtBiaya, TextArea txtKeterangan, Button btnSelesai) {
        String biayaText = txtBiaya.getText();
        String keteranganText = txtKeterangan.getText();

        boolean valid = true;

        if (biayaText == null || biayaText.isEmpty()) {
            valid = false;
        } else {
            try {
                // Ambil angka bersih dari formatted text
                String cleanBiaya = biayaText.replaceAll("[^\\d]", "");
                double biaya = Double.parseDouble(cleanBiaya);
                if (biaya < 1000 || biaya % 1000 != 0) {
                    valid = false;
                }
            } catch (NumberFormatException e) {
                valid = false;
            }
        }

        if (keteranganText == null || keteranganText.trim().length() < 5) {
            valid = false;
        }

        btnSelesai.setDisable(!valid);
    }


    // ==========================================
    // HELPER CLASS MAINTENANCE DATA
    // ==========================================
    public static class MaintenanceData {
        private String idMaintenance, idMesin, idPegawai, jenisKerusakan, deskripsi, status, aksi, keterangan;
        private LocalDate tanggalMaintenance, tanggalSelesai;
        private double biaya;
        private String namaPegawai;

        public MaintenanceData(String idMaintenance, String idMesin, String idPegawai,
                               String jenisKerusakan, String deskripsi,
                               LocalDate tanggalMaintenance, LocalDate tanggalSelesai,
                               String status, double biaya, String keterangan, String namaPegawai) {
            this.idMaintenance = idMaintenance;
            this.idMesin = idMesin;
            this.idPegawai = idPegawai;
            this.jenisKerusakan = jenisKerusakan;
            this.deskripsi = deskripsi;
            this.tanggalMaintenance = tanggalMaintenance;
            this.tanggalSelesai = tanggalSelesai;
            this.status = status;
            this.biaya = biaya;
            this.keterangan = keterangan;
            this.namaPegawai = namaPegawai;

            if (status != null && !status.equals("selesai")) {
                this.aksi = "Selesai";
            } else {
                this.aksi = "-";
            }
        }

        public String getIdMaintenance() { return idMaintenance; }
        public String getIdMesin() { return idMesin; }
        public String getIdPegawai() { return idPegawai; }
        public String getJenisKerusakan() { return jenisKerusakan; }
        public String getDeskripsi() { return deskripsi; }
        public LocalDate getTanggalMaintenance() { return tanggalMaintenance; }
        public LocalDate getTanggalSelesai() { return tanggalSelesai; }
        public String getStatus() { return status; }
        public String getAksi() { return aksi; }
        public double getBiaya() { return biaya; }
        public String getKeterangan() { return keterangan; }
        public String getNamaPegawai() { return namaPegawai; }

        public String getBiayaFormatted() {
            return String.format("Rp %,d", (long) biaya);
        }
    }
}