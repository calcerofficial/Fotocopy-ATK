package SistemFotocopy;

import Database.DBConnection;
import javafx.animation.PauseTransition;           // ← TAMBAHKAN
import javafx.application.Platform;               // ← TAMBAHKAN
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.util.Duration;                      // ← TAMBAHKAN

import java.net.URL;
import java.sql.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.ResourceBundle;

public class MaintenanceMesin implements Initializable {

    @FXML
    private Button btnBatal, btnNextPage, btnPrevPage, btnSimpan;

    @FXML
    private ComboBox<String> cbJenisKerusakan, cbMesin;

    @FXML
    private TableColumn<MaintenanceData, String> colAksi, colDeskripsi, colIdMaintenance,
            colIdMesin, colIdPegawai, colJenisKerusakan, colStatus,
            colTanggalMaintenance, colTanggalSelesai, colBiaya, colKeterangan;

    @FXML
    private TextField txtTanggalMaintenance;

    @FXML
    private Label lblInfoData, lblMesinAktif, lblMesinRusak, lblMesinSelesai, lblTotalMesin;

    @FXML
    private TableView<MaintenanceData> tblMaintenance;

    @FXML
    private TextField txtCari, txtIdMaintenance, txtIdMaintenance1;

    @FXML
    private TextArea txtDeskripsi;

    @FXML
    private HBox hboxPagination;

    private Connection conn;

    // =============================================================
    // 🔥 TAMBAHKAN VARIABLE UNTUK AUTO REFRESH
    // =============================================================
    private PauseTransition refreshTimer;

    // Data untuk tabel
    private ObservableList<MaintenanceData> maintenanceDataList = FXCollections.observableArrayList();
    private ObservableList<MaintenanceData> currentPageData = FXCollections.observableArrayList();
    private ObservableList<String> mesinList = FXCollections.observableArrayList();

    // SortedList untuk sorting
    private SortedList<MaintenanceData> sortedData;

    // Pagination variables
    private int currentPage = 0;
    private int itemsPerPage = 5;
    private int totalPages = 0;
    private int totalItems = 0;

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

        txtTanggalMaintenance.setText(LocalDate.now().format(FORMATTER));
        txtTanggalMaintenance.setEditable(false);

        cbJenisKerusakan.setItems(FXCollections.observableArrayList(JENIS_KERUSAKAN));

        loadMesinData();
        setupTableColumns();
        loadMaintenanceData();
        updateStatisticsCards();
        setupValidations();
        setupSearch();

        btnSimpan.setOnAction(e -> simpanData());
        btnBatal.setOnAction(e -> clearForm());

        btnPrevPage.setOnAction(e -> handlePrevPage());
        btnNextPage.setOnAction(e -> handleNextPage());

        cbMesin.setOnAction(e -> validateMesinStatus());

        // =============================================================
        // 🔥 AUTO REFRESH SETIAP 5 DETIK
        // =============================================================
        setupAutoRefresh();
    }

    // =============================================================
    // 🔥 AUTO REFRESH METHOD
    // =============================================================
    private void setupAutoRefresh() {
        refreshTimer = new PauseTransition(Duration.seconds(5));
        refreshTimer.setOnFinished(e -> {
            Platform.runLater(() -> {
                loadMaintenanceData();
                updateStatisticsCards();
            });
            refreshTimer.playFromStart();
        });
        refreshTimer.play();
    }

    public void stopAutoRefresh() {
        if (refreshTimer != null) {
            refreshTimer.stop();
        }
    }

    // =============================================================
    // CEK SALDO KAS - PERBAIKAN (f_GetSaldoKas → f_SaldoKas)
    // =============================================================
    private double getSaldoKas() {
        String query = "SELECT dbo.f_SaldoKas() AS Saldo";  // ← PERBAIKAN
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {
            if (rs.next()) {
                return rs.getDouble("Saldo");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    // =============================================================
    // PAGINATION METHODS (SAMA)
    // =============================================================

    private void applyPagination() {
        if (maintenanceDataList == null) return;

        if (sortedData == null) {
            sortedData = new SortedList<>(maintenanceDataList, new Comparator<MaintenanceData>() {
                @Override
                public int compare(MaintenanceData o1, MaintenanceData o2) {
                    String status1 = o1.getStatus();
                    String status2 = o2.getStatus();

                    if (status1.equalsIgnoreCase(status2)) {
                        if (o1.getTanggalMaintenance() == null && o2.getTanggalMaintenance() == null) {
                            return 0;
                        }
                        if (o1.getTanggalMaintenance() == null) return 1;
                        if (o2.getTanggalMaintenance() == null) return -1;
                        return o2.getTanggalMaintenance().compareTo(o1.getTanggalMaintenance());
                    }

                    boolean isRusak1 = status1.equalsIgnoreCase("rusak");
                    boolean isRusak2 = status2.equalsIgnoreCase("rusak");

                    if (isRusak1 && !isRusak2) return -1;
                    if (!isRusak1 && isRusak2) return 1;

                    return 0;
                }
            });
        }

        totalItems = sortedData.size();
        totalPages = (int) Math.ceil((double) totalItems / itemsPerPage);
        if (totalPages == 0) totalPages = 1;

        if (currentPage >= totalPages) currentPage = totalPages - 1;
        if (currentPage < 0) currentPage = 0;

        int fromIndex = currentPage * itemsPerPage;
        int toIndex = Math.min(fromIndex + itemsPerPage, totalItems);

        currentPageData.clear();
        if (fromIndex < totalItems) {
            currentPageData.addAll(sortedData.subList(fromIndex, toIndex));
        }

        tblMaintenance.setItems(currentPageData);

        int startItem = totalItems > 0 ? fromIndex + 1 : 0;
        int endItem = Math.min(toIndex, totalItems);
        lblInfoData.setText("Menampilkan " + startItem + "-" + endItem + " dari " + totalItems + " data");

        btnPrevPage.setDisable(currentPage == 0);
        btnNextPage.setDisable(currentPage >= totalPages - 1);

        updatePageButtons();
    }

    private void updatePageButtons() {
        if (hboxPagination == null) return;

        hboxPagination.getChildren().clear();

        btnPrevPage.setStyle("-fx-background-color: transparent; -fx-text-fill: #475569; -fx-font-size: 16px; -fx-font-weight: bold; -fx-padding: 4 10; -fx-cursor: hand;");
        hboxPagination.getChildren().add(btnPrevPage);

        int maxButtons = 5;
        int startPage = Math.max(0, currentPage - 2);
        int endPage = Math.min(totalPages - 1, startPage + maxButtons - 1);

        if (endPage - startPage < maxButtons - 1 && startPage > 0) {
            startPage = Math.max(0, endPage - maxButtons + 1);
        }

        for (int i = startPage; i <= endPage; i++) {
            Button pageBtn = new Button(String.valueOf(i + 1));
            pageBtn.setPrefWidth(32);
            pageBtn.setPrefHeight(32);

            if (i == currentPage) {
                pageBtn.setStyle("-fx-background-color: #002F6B; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 4 12; -fx-background-radius: 4; -fx-cursor: hand; -fx-font-size: 13px;");
            } else {
                pageBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #3B82F6; -fx-font-weight: bold; -fx-padding: 4 12; -fx-cursor: hand; -fx-font-size: 13px;");
                pageBtn.setOnMouseEntered(e -> pageBtn.setStyle("-fx-background-color: #EFF6FF; -fx-text-fill: #3B82F6; -fx-font-weight: bold; -fx-padding: 4 12; -fx-cursor: hand; -fx-background-radius: 4; -fx-font-size: 13px;"));
                pageBtn.setOnMouseExited(e -> pageBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #3B82F6; -fx-font-weight: bold; -fx-padding: 4 12; -fx-cursor: hand; -fx-font-size: 13px;"));
            }

            final int pageIndex = i;
            pageBtn.setOnAction(e -> {
                currentPage = pageIndex;
                applyPagination();
            });

            hboxPagination.getChildren().add(pageBtn);
        }

        btnNextPage.setStyle("-fx-background-color: transparent; -fx-text-fill: #475569; -fx-font-size: 16px; -fx-font-weight: bold; -fx-padding: 4 10; -fx-cursor: hand;");
        hboxPagination.getChildren().add(btnNextPage);

        hboxPagination.setAlignment(Pos.CENTER);
        hboxPagination.setSpacing(4);
    }

    @FXML
    void handlePrevPage() {
        if (currentPage > 0) {
            currentPage--;
            applyPagination();
        }
    }

    @FXML
    void handleNextPage() {
        if (currentPage < totalPages - 1) {
            currentPage++;
            applyPagination();
        }
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

    private void validateMesinStatus() {
        String selected = cbMesin.getValue();
        if (selected == null || selected.isEmpty()) {
            btnSimpan.setDisable(false);
            return;
        }

        String idMesin = selected.split(" - ")[0];

        String query = "SELECT dbo.f_CekMesinRusak(?) AS IsRusak";
        try (PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setString(1, idMesin);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next() && rs.getBoolean("IsRusak")) {
                    btnSimpan.setDisable(true);
                    showAlert("Peringatan", "Mesin ini sedang dalam status RUSAK!\n" +
                            "Harap selesaikan maintenance terlebih dahulu sebelum menambah maintenance baru.");
                } else {
                    btnSimpan.setDisable(false);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void loadMesinData() {
        mesinList.clear();
        String query = "SELECT ID_Mesin, Nama_Mesin FROM v_MesinAktif ORDER BY Nama_Mesin";
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

        colStatus.setCellFactory(col -> new TableCell<MaintenanceData, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item);
                    if (item.equalsIgnoreCase("selesai")) {
                        setStyle("-fx-text-fill: #10B981; -fx-font-weight: bold;");
                    } else if (item.equalsIgnoreCase("rusak")) {
                        setStyle("-fx-text-fill: #EF4444; -fx-font-weight: bold;");
                    } else {
                        setStyle("-fx-text-fill: #6B7280;");
                    }
                }
            }
        });

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

    // =============================================================
    // LOAD DATA MAINTENANCE - PAKAI VIEW ✅
    // =============================================================
    private void loadMaintenanceData() {
        maintenanceDataList.clear();
        sortedData = null;

        String query = "SELECT * FROM v_TampilMaintenanceMesin ORDER BY Tanggal_Maintenance_Mesin DESC";

        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {
            while (rs.next()) {
                MaintenanceData data = new MaintenanceData(
                        rs.getString("ID_Maintenance_Mesin"),
                        rs.getString("Nama_Mesin"),
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

            currentPage = 0;
            applyPagination();

        } catch (SQLException e) {
            e.printStackTrace();
            showAlert("Error", "Gagal memuat data maintenance: " + e.getMessage());
        }
    }

    // =============================================================
    // UPDATE STATISTICS CARDS - PAKAI UDF ✅
    // =============================================================
    private void updateStatisticsCards() {
        try {
            String query = "SELECT " +
                    "dbo.f_TotalMesin() AS Total, " +
                    "dbo.f_MesinAktif() AS Aktif, " +
                    "dbo.f_MesinRusak() AS Rusak, " +
                    "dbo.f_MesinSelesai() AS Selesai";

            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(query)) {
                if (rs.next()) {
                    lblTotalMesin.setText(String.valueOf(rs.getInt("Total")));
                    lblMesinAktif.setText(String.valueOf(rs.getInt("Aktif")));
                    lblMesinRusak.setText(String.valueOf(rs.getInt("Rusak")));
                    lblMesinSelesai.setText(String.valueOf(rs.getInt("Selesai")));
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

    // =============================================================
    // SEARCH - PAKAI UDF ✅
    // =============================================================
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
        sortedData = null;

        String query = "SELECT * FROM dbo.f_CariMaintenance(?) ORDER BY Tanggal_Maintenance_Mesin DESC";

        try (PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setString(1, keyword);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    MaintenanceData data = new MaintenanceData(
                            rs.getString("ID_Maintenance_Mesin"),
                            rs.getString("Nama_Mesin"),
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

                currentPage = 0;
                applyPagination();
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

            LocalDate tanggalMaintenance = LocalDate.parse(txtTanggalMaintenance.getText(), FORMATTER);
            String status = "rusak";

            String sql = "{call sp_TambahMaintenanceMesin(?, ?, ?, ?, ?, ?, ?, ?, ?)}";
            try (CallableStatement cstmt = conn.prepareCall(sql)) {
                cstmt.setString(1, idMesin);
                cstmt.setString(2, idPegawai);
                cstmt.setString(3, jenisKerusakan);
                cstmt.setString(4, deskripsi);
                cstmt.setDate(5, Date.valueOf(tanggalMaintenance));
                cstmt.setNull(6, Types.DATE);
                cstmt.setDouble(7, 0);
                cstmt.setNull(8, Types.VARCHAR);
                cstmt.setString(9, status);

                cstmt.execute();

                showAlert("Sukses", "Data Maintenance berhasil disimpan!");
                clearForm();
                loadMaintenanceData();
                updateStatisticsCards();
                generateIdMaintenance();
                btnSimpan.setDisable(false);
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

        if (txtTanggalMaintenance.getText() == null || txtTanggalMaintenance.getText().isEmpty()) {
            showAlert("Error", "Tanggal Maintenance harus diisi!");
            return false;
        }

        return true;
    }

    private void clearForm() {
        cbMesin.setValue(null);
        cbJenisKerusakan.setValue(null);
        txtDeskripsi.clear();
        txtTanggalMaintenance.setText(LocalDate.now().format(FORMATTER));
        generateIdMaintenance();
        btnSimpan.setDisable(false);
    }

    private String formatRupiah(double amount) {
        return String.format("Rp. %,.0f", amount);
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    // =============================================================
// SHOW POPUP SELESAI - PERBAIKAN
// =============================================================
    private void showPopupSelesai(MaintenanceData data) {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Selesaikan Maintenance");
        dialog.setHeaderText("Form Penyelesaian Maintenance - " + data.getIdMaintenance());

        ButtonType btnSelesai = new ButtonType("Selesai", ButtonBar.ButtonData.OK_DONE);
        ButtonType btnBatal = new ButtonType("Batal", ButtonBar.ButtonData.CANCEL_CLOSE);
        dialog.getDialogPane().getButtonTypes().addAll(btnSelesai, btnBatal);

        GridPane grid = new GridPane();
        grid.setHgap(15);
        grid.setVgap(12);
        grid.setPadding(new Insets(20, 20, 20, 20));
        grid.setPrefWidth(600); // Sedikit dilebarkan

        // Mengatur lebar kolom agar rapi
        javafx.scene.layout.ColumnConstraints col1 = new javafx.scene.layout.ColumnConstraints();
        col1.setPrefWidth(150);
        javafx.scene.layout.ColumnConstraints col2 = new javafx.scene.layout.ColumnConstraints();
        col2.setHgrow(javafx.scene.layout.Priority.ALWAYS);
        grid.getColumnConstraints().addAll(col1, col2);

        TextField txtIdMaintenancePopup = new TextField(data.getIdMaintenance());
        txtIdMaintenancePopup.setEditable(false);
        txtIdMaintenancePopup.setStyle("-fx-background-color: #f0f0f0; -fx-opacity: 1.0;");

        TextField txtMesinPopup = new TextField(data.getIdMesin());
        txtMesinPopup.setEditable(false);
        txtMesinPopup.setStyle("-fx-background-color: #f0f0f0; -fx-opacity: 1.0;");

        TextField txtPegawaiPopup = new TextField(data.getNamaPegawai() != null ? data.getNamaPegawai() : data.getIdPegawai());
        txtPegawaiPopup.setEditable(false);
        txtPegawaiPopup.setStyle("-fx-background-color: #f0f0f0; -fx-opacity: 1.0;");

        TextField txtNamaTeknisiPopup = new TextField();
        txtNamaTeknisiPopup.setPromptText("Masukan Nama Teknisi...");
        txtNamaTeknisiPopup.setStyle("-fx-background-color: white;");
        txtNamaTeknisiPopup.setEditable(true);
        // Hanya menerima huruf dan spasi
        txtNamaTeknisiPopup.textProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal.matches("[a-zA-Z\\s]*")) {
                txtNamaTeknisiPopup.setText(newVal.replaceAll("[^a-zA-Z\\s]", ""));
            }
        });

        TextField txtJenisPopup = new TextField(data.getJenisKerusakan());
        txtJenisPopup.setEditable(false);
        txtJenisPopup.setStyle("-fx-background-color: #f0f0f0; -fx-opacity: 1.0;");

        TextArea txtDeskripsiPopup = new TextArea(data.getDeskripsi());
        txtDeskripsiPopup.setEditable(false);
        txtDeskripsiPopup.setStyle("-fx-background-color: #f0f0f0; -fx-opacity: 1.0;");
        txtDeskripsiPopup.setPrefHeight(60);
        txtDeskripsiPopup.setWrapText(true);

        TextField txtTanggalMainPopup = new TextField(data.getTanggalMaintenance() != null ?
                data.getTanggalMaintenance().format(FORMATTER) : "");
        txtTanggalMainPopup.setEditable(false);
        txtTanggalMainPopup.setStyle("-fx-background-color: #f0f0f0; -fx-opacity: 1.0;");

        TextField txtTanggalSelesaiPopup = new TextField(LocalDate.now().format(FORMATTER));
        txtTanggalSelesaiPopup.setEditable(false);
        txtTanggalSelesaiPopup.setStyle("-fx-background-color: #f0f0f0; -fx-opacity: 1.0;");

        TextField txtBiayaPopup = new TextField();
        txtBiayaPopup.setPromptText("Masukan Biaya...");
        txtBiayaPopup.setStyle("-fx-background-color: white;");
        txtBiayaPopup.setEditable(true);

        txtBiayaPopup.textProperty().addListener((obs, oldVal, newVal) -> {
            String clean = newVal.replaceAll("[^\\d]", "");
            if (clean.isEmpty()) {
                txtBiayaPopup.setText("");
                return;
            }
            long number = Long.parseLong(clean);
            String formatted = "Rp " + String.format("%,d", number).replace(',', '.');
            txtBiayaPopup.setText(formatted);
            txtBiayaPopup.positionCaret(formatted.length());
        });

        TextArea txtKeteranganPopup = new TextArea();
        txtKeteranganPopup.setPromptText("Masukan Keterangan...");
        txtKeteranganPopup.setPrefHeight(60);
        txtKeteranganPopup.setWrapText(true);
        txtKeteranganPopup.setStyle("-fx-background-color: white;");
        txtKeteranganPopup.setEditable(true);
        txtKeteranganPopup.textProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal.matches("[a-zA-Z0-9 .,!?]*")) {
                txtKeteranganPopup.setText(newVal.replaceAll("[^a-zA-Z0-9 .,!?]", ""));
            }
        });

        TextField txtStatusPopup = new TextField("selesai");
        txtStatusPopup.setEditable(false);
        txtStatusPopup.setStyle("-fx-background-color: #f0f0f0; -fx-text-fill: #10B981; -fx-font-weight: bold; -fx-opacity: 1.0;");

        int row = 0;
        grid.add(new Label("ID Maintenance:"), 0, row);
        grid.add(txtIdMaintenancePopup, 1, row++);
        grid.add(new Label("ID Mesin:"), 0, row);
        grid.add(txtMesinPopup, 1, row++);
        grid.add(new Label("ID Pegawai:"), 0, row);
        grid.add(txtPegawaiPopup, 1, row++);
        grid.add(new Label("Nama Teknisi:"), 0, row);
        grid.add(txtNamaTeknisiPopup, 1, row++);
        grid.add(new Label("Jenis Kerusakan:"), 0, row);
        grid.add(txtJenisPopup, 1, row++);
        grid.add(new Label("Deskripsi:"), 0, row);
        grid.add(txtDeskripsiPopup, 1, row++);
        grid.add(new Label("Tanggal Maintenance:"), 0, row);
        grid.add(txtTanggalMainPopup, 1, row++);
        grid.add(new Label("Tanggal Selesai:"), 0, row);
        grid.add(txtTanggalSelesaiPopup, 1, row++);
        grid.add(new Label("Biaya Maintenance:"), 0, row);
        grid.add(txtBiayaPopup, 1, row++);
        grid.add(new Label("Keterangan:"), 0, row);
        grid.add(txtKeteranganPopup, 1, row++);
        grid.add(new Label("Status:"), 0, row);
        grid.add(txtStatusPopup, 1, row++);

        dialog.getDialogPane().setContent(grid);

        Button btnSelesaiButton = (Button) dialog.getDialogPane().lookupButton(btnSelesai);
        btnSelesaiButton.setDisable(true);

        txtNamaTeknisiPopup.textProperty().addListener((obs, oldVal, newVal) -> {
            validatePopupForm(txtNamaTeknisiPopup, txtBiayaPopup, txtKeteranganPopup, btnSelesaiButton);
        });

        txtBiayaPopup.textProperty().addListener((obs, oldVal, newVal) -> {
            String cleanNumber = newVal.replaceAll("[^\\d]", "");
            long biayaValue = cleanNumber.isEmpty() ? 0 : Long.parseLong(cleanNumber);
            txtBiayaPopup.setUserData(biayaValue);
            validatePopupForm(txtNamaTeknisiPopup, txtBiayaPopup, txtKeteranganPopup, btnSelesaiButton);
        });

        txtKeteranganPopup.textProperty().addListener((obs, oldVal, newVal) -> {
            validatePopupForm(txtNamaTeknisiPopup, txtBiayaPopup, txtKeteranganPopup, btnSelesaiButton);
        });

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == btnSelesai) {
                try {
                    String namaTeknisi = txtNamaTeknisiPopup.getText();
                    String biayaText = txtBiayaPopup.getText();
                    String keteranganText = txtKeteranganPopup.getText();

                    if (namaTeknisi == null || namaTeknisi.trim().isEmpty()) {
                        showAlert("Error", "Nama Teknisi harus diisi!");
                        return null;
                    }

                    if (biayaText == null || biayaText.isEmpty()) {
                        showAlert("Error", "Biaya Maintenance harus diisi!");
                        return null;
                    }

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

                    double saldoKas = getSaldoKas();

                    if (saldoKas < biaya) {
                        Alert errorAlert = new Alert(Alert.AlertType.ERROR);
                        errorAlert.setTitle("Saldo Tidak Cukup");
                        errorAlert.setHeaderText("❌ Maintenance Gagal Diselesaikan");
                        errorAlert.setContentText(
                                "Saldo kas tidak mencukupi untuk membayar maintenance!\n\n" +
                                        "💰 Saldo Kas Saat Ini: " + formatRupiah(saldoKas) + "\n" +
                                        "🔧 Biaya Maintenance: " + formatRupiah(biaya) + "\n" +
                                        "🔴 Kekurangan: " + formatRupiah(biaya - saldoKas) + "\n\n" +
                                        "⚠️ Maintenance tidak dapat diselesaikan.\n" +
                                        "Silakan tambahkan pendapatan terlebih dahulu."
                        );
                        errorAlert.showAndWait();
                        return null;
                    }

                    String idMaintenance = data.getIdMaintenance();
                    LocalDate tanggalSelesai = LocalDate.now();

                    // Gabungkan Nama Teknisi ke Keterangan
                    String keteranganGabungan = "Teknisi: " + namaTeknisi.trim() + " - " + keteranganText;

                    // =========================================================
                    // 🔥 PERBAIKAN: PAKAI 4 PARAMETER (SESUAI SP)
                    // =========================================================
                    String sql = "{call sp_UpdateMaintenanceMesin(?, ?, ?, ?)}";
                    try (CallableStatement cstmt = conn.prepareCall(sql)) {
                        cstmt.setString(1, idMaintenance);
                        cstmt.setDate(2, Date.valueOf(tanggalSelesai));
                        cstmt.setDouble(3, biaya);
                        cstmt.setString(4, keteranganGabungan);
                        cstmt.execute();

                        showAlert("Sukses", "✅ Maintenance berhasil diselesaikan!\n" +
                                "Biaya: " + formatRupiah(biaya) + "\n" +
                                "💰 Saldo Kas Saat Ini: " + formatRupiah(getSaldoKas()));
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

    private void validatePopupForm(TextField txtNamaTeknisi, TextField txtBiaya, TextArea txtKeterangan, Button btnSelesai) {
        String namaTeknisi = txtNamaTeknisi.getText();
        String biayaText = txtBiaya.getText();
        String keteranganText = txtKeterangan.getText();

        boolean valid = true;

        if (namaTeknisi == null || namaTeknisi.trim().isEmpty()) {
            valid = false;
        }

        if (biayaText == null || biayaText.isEmpty()) {
            valid = false;
        } else {
            try {
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