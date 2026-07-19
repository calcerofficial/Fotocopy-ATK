package SistemFotocopy;

import Database.DBConnection;
import javafx.application.Platform;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class DataMesin {

    // =====================================================================
    // FXML BINDINGS
    // =====================================================================

    @FXML private TextField txtIdMesin;
    @FXML private TextField txtNamaMesin;
    @FXML private TextField txtMerkMesin;
    @FXML private ComboBox<String> cbNamaLayanan;

    @FXML private Label lblErrorNama;
    @FXML private Label lblErrorMerk;
    @FXML private Label lblErrorMerk1;

    @FXML private Button BrtSimpan;
    @FXML private Button BtUbah;
    @FXML private Button BtHapus;
    @FXML private Button BtBatal;

    @FXML private TableView<MesinModel> tableMesin;
    @FXML private TableColumn<MesinModel, String> colIdMesin;
    @FXML private TableColumn<MesinModel, String> colNamaMesin;
    @FXML private TableColumn<MesinModel, String> colMerkMesin;
    @FXML private TableColumn<MesinModel, String> colStatusMesin;
    @FXML private TableColumn<MesinModel, Void> colStatusMesin1;

    @FXML private TableView<RiwayatModel> tableRiwayat;
    @FXML private TableColumn<RiwayatModel, String> colRiwayatId;
    @FXML private TableColumn<RiwayatModel, String> colRiwayatTanggal;
    @FXML private TableColumn<RiwayatModel, String> colRiwayatKeterangan;

    @FXML private TextField txtCariMesin;
    @FXML private Label lblInfoData;

    @FXML private Label lblTotalMesin;
    @FXML private Label lblMesinAktif;
    @FXML private Label lblMesinNonAktif;

    @FXML private Button btnPrev;
    @FXML private Button btnNext;
    @FXML private Label lblPageInfo;

    private final DBConnection db = new DBConnection();

    private final ObservableList<MesinModel> masterData = FXCollections.observableArrayList();
    private FilteredList<MesinModel> filteredData;
    private SortedList<MesinModel> sortedData;

    private static final int ITEMS_PER_PAGE = 5;
    private int currentPage = 1;
    private List<MesinModel> halamanSaatIni = new ArrayList<>();

    // =====================================================================
    // INITIALIZE
    // =====================================================================

    @FXML
    public void initialize() {
        setupTableMesin();
        setupTableRiwayat();
        setupSearch();
        setupRowSelection();
        setupInputValidation();
        setupKolomAksi();

        loadDataMesin();
        loadNamaLayanan();
        hitungStatCard();
        resetForm();

        Platform.runLater(() -> {
            if (!tableMesin.getItems().isEmpty()) {
                MesinModel mesinPertama = tableMesin.getItems().get(0);
                System.out.println("=== AUTO LOAD RIWAYAT ===");
                System.out.println("ID Mesin Pertama: " + mesinPertama.getIdMesin());
                loadRiwayatMesin(mesinPertama.getIdMesin());

                // Optional: select row pertama biar form terisi
                tableMesin.getSelectionModel().select(0);
            }
        });
    }

    // =====================================================================
    // SETUP KOLOM TABEL
    // =====================================================================

    private void setupTableMesin() {
        colIdMesin.setCellValueFactory(d -> d.getValue().idMesinProperty());
        colNamaMesin.setCellValueFactory(d -> d.getValue().namaMesinProperty());
        colMerkMesin.setCellValueFactory(d -> d.getValue().merkMesinProperty());
        colStatusMesin.setCellValueFactory(d -> d.getValue().statusMesinProperty());

        colStatusMesin.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item);
                    if (item.equalsIgnoreCase("Aktif")) {
                        setStyle("-fx-text-fill: #16A34A; -fx-font-weight: bold;");
                    } else {
                        setStyle("-fx-text-fill: #BA1A1A; -fx-font-weight: bold;");
                    }
                }
            }
        });
    }

    private void setupTableRiwayat() {
        colRiwayatId.setCellValueFactory(d -> d.getValue().idMesinProperty());
        colRiwayatTanggal.setCellValueFactory(d -> d.getValue().tanggalProperty());
        colRiwayatKeterangan.setCellValueFactory(d -> d.getValue().keteranganProperty());
    }

    // =====================================================================
    // KOLOM AKSI
    // =====================================================================

    private void setupKolomAksi() {
        colStatusMesin1.setCellFactory(col -> new TableCell<>() {
            private final Button btnDetail = new Button("Detail");
            {
                btnDetail.getStyleClass().add("btn-detail-aksi");
                btnDetail.setOnAction(e -> {
                    MesinModel mesin = getTableView().getItems().get(getIndex());
                    bukaJendelaDetailMesin(mesin);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setAlignment(Pos.CENTER);
                setGraphic(empty ? null : btnDetail);
            }
        });
    }

    private void bukaJendelaDetailMesin(MesinModel mesin) {
        TableView<DetailProdukMesinModel> tableDetail = new TableView<>();

        TableColumn<DetailProdukMesinModel, String> colNamaProduk = new TableColumn<>("Nama Layanan/Produk");
        colNamaProduk.setCellValueFactory(d -> d.getValue().namaProdukProperty());
        colNamaProduk.setPrefWidth(200);

        TableColumn<DetailProdukMesinModel, String> colKategori = new TableColumn<>("Kategori");
        colKategori.setCellValueFactory(d -> d.getValue().kategoriProperty());
        colKategori.setPrefWidth(110);

        TableColumn<DetailProdukMesinModel, String> colMerk = new TableColumn<>("Merk");
        colMerk.setCellValueFactory(d -> d.getValue().merkBarangProperty());
        colMerk.setPrefWidth(120);

        TableColumn<DetailProdukMesinModel, Number> colHarga = new TableColumn<>("Harga");
        colHarga.setCellValueFactory(d -> d.getValue().hargaProperty());
        colHarga.setPrefWidth(110);

        tableDetail.getColumns().addAll(colNamaProduk, colKategori, colMerk, colHarga);
        tableDetail.setItems(loadDetailProdukMesin(mesin.getIdMesin()));
        tableDetail.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        Label lblJudul = new Label("Detail Mesin: " + mesin.getNamaMesin() + " (" + mesin.getIdMesin() + ")");
        lblJudul.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");

        VBox root = new VBox(15, lblJudul, tableDetail);
        root.setPadding(new Insets(20));

        Stage stage = new Stage();
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.setTitle("Detail Produk/Layanan Mesin");
        stage.setScene(new Scene(root, 560, 400));
        stage.show();
    }

    private ObservableList<DetailProdukMesinModel> loadDetailProdukMesin(String idMesin) {
        ObservableList<DetailProdukMesinModel> list = FXCollections.observableArrayList();
        String sql = "SELECT p.Nama_Barang, p.Kategori_Produk, p.Merk_Barang, p.Harga " +
                "FROM DetailProdukMesin dpm " +
                "JOIN Produk p ON dpm.ID_Produk = p.ID_Produk " +
                "WHERE dpm.ID_Mesin = ? " +
                "ORDER BY p.Nama_Barang";

        try (PreparedStatement ps = db.getConnection().prepareStatement(sql)) {
            ps.setString(1, idMesin);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(new DetailProdukMesinModel(
                            rs.getString("Nama_Barang"),
                            rs.getString("Kategori_Produk"),
                            rs.getString("Merk_Barang"),
                            rs.getDouble("Harga")
                    ));
                }
            }
        } catch (SQLException e) {
            tampilAlert(Alert.AlertType.ERROR, "Gagal memuat detail mesin", e.getMessage());
        }
        return list;
    }

    // =====================================================================
    // NAMA LAYANAN
    // =====================================================================

    private void loadNamaLayanan() {
        ObservableList<String> daftarLayanan = FXCollections.observableArrayList();
        String sql = "SELECT Nama_Barang FROM Produk " +
                "WHERE LOWER(Kategori_Produk) = 'layanan' " +
                "ORDER BY Nama_Barang";

        try (java.sql.Statement st = db.getConnection().createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                daftarLayanan.add(rs.getString("Nama_Barang"));
            }
            cbNamaLayanan.setItems(daftarLayanan);
        } catch (SQLException e) {
            tampilAlert(Alert.AlertType.ERROR, "Gagal memuat data layanan", e.getMessage());
        }
    }

    private void simpanDetailProdukMesin(String idMesin, String namaProduk) {
        String sql = "INSERT INTO DetailProdukMesin (ID_Produk, ID_Mesin) " +
                "SELECT p.ID_Produk, ? FROM Produk p " +
                "WHERE p.Nama_Barang = ? " +
                "AND NOT EXISTS (" +
                "  SELECT 1 FROM DetailProdukMesin d " +
                "  WHERE d.ID_Produk = p.ID_Produk AND d.ID_Mesin = ?" +
                ")";

        try (PreparedStatement ps = db.getConnection().prepareStatement(sql)) {
            ps.setString(1, idMesin);
            ps.setString(2, namaProduk);
            ps.setString(3, idMesin);
            ps.executeUpdate();
        } catch (SQLException e) {
            tampilAlert(Alert.AlertType.ERROR, "Gagal menyimpan relasi layanan", e.getMessage());
        }
    }

    // =====================================================================
    // CEK STATUS MAINTENANCE
    // =====================================================================

    private boolean isMesinRusak(String idMesin) {
        String sql = "SELECT dbo.f_CekMesinRusak(?) AS IsRusak";

        try (PreparedStatement ps = db.getConnection().prepareStatement(sql)) {
            ps.setString(1, idMesin);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getBoolean("IsRusak");
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    // =====================================================================
    // INPUT VALIDATION
    // =====================================================================

    private void setupInputValidation() {
        TextFormatter<String> namaFormatter = new TextFormatter<>(change -> {
            String newText = change.getControlNewText();
            if (newText.isEmpty()) {
                txtNamaMesin.setStyle(null);
                return change;
            }
            if (!newText.matches("^[a-zA-Z0-9\\s]*$")) {
                txtNamaMesin.setStyle("-fx-border-color: red; -fx-border-width: 2px;");
                return null;
            }
            txtNamaMesin.setStyle(null);
            return change;
        });
        txtNamaMesin.setTextFormatter(namaFormatter);

        TextFormatter<String> merkFormatter = new TextFormatter<>(change -> {
            String newText = change.getControlNewText();
            if (newText.isEmpty()) {
                txtMerkMesin.setStyle(null);
                return change;
            }
            if (!newText.matches("^[a-zA-Z0-9\\s]*$")) {
                txtMerkMesin.setStyle("-fx-border-color: red; -fx-border-width: 2px;");
                return null;
            }
            txtMerkMesin.setStyle(null);
            return change;
        });
        txtMerkMesin.setTextFormatter(merkFormatter);
    }

    // =====================================================================
    // CHECK INPUT ERRORS
    // =====================================================================

    private boolean checkInputErrors() {
        boolean hasError = false;

        String nama = txtNamaMesin.getText();
        if (!nama.isEmpty() && !nama.matches("^[a-zA-Z0-9\\s]+$")) {
            txtNamaMesin.setStyle("-fx-border-color: red; -fx-border-width: 2px;");
            showErrorLabel(lblErrorNama, "Nama hanya boleh huruf, angka, dan spasi");
            hasError = true;
        } else {
            txtNamaMesin.setStyle(null);
            hideErrorLabel(lblErrorNama);
        }

        String merk = txtMerkMesin.getText();
        if (!merk.isEmpty() && !merk.matches("^[a-zA-Z0-9\\s]+$")) {
            txtMerkMesin.setStyle("-fx-border-color: red; -fx-border-width: 2px;");
            showErrorLabel(lblErrorMerk, "Merk hanya boleh huruf, angka, dan spasi");
            hasError = true;
        } else {
            txtMerkMesin.setStyle(null);
            hideErrorLabel(lblErrorMerk);
        }

        return hasError;
    }

    // =====================================================================
    // ERROR LABEL HELPERS
    // =====================================================================

    private void hideAllErrorLabels() {
        if (lblErrorNama != null) { lblErrorNama.setVisible(false); lblErrorNama.setText(""); }
        if (lblErrorMerk != null) { lblErrorMerk.setVisible(false); lblErrorMerk.setText(""); }
        if (lblErrorMerk1 != null) { lblErrorMerk1.setVisible(false); lblErrorMerk1.setText(""); }
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

    // =====================================================================
    // SEARCH LISTENER
    // =====================================================================

    private void setupSearch() {
        txtCariMesin.textProperty().addListener((obs, lama, baru) -> {
            if (filteredData == null) return;
            String kw = baru == null ? "" : baru.trim().toLowerCase();
            filteredData.setPredicate(m ->
                    kw.isEmpty()
                            || m.getNamaMesin().toLowerCase().contains(kw)
                            || m.getIdMesin().toLowerCase().contains(kw)
                            || m.getMerkMesin().toLowerCase().contains(kw)
                            || m.getStatusMesin().toLowerCase().contains(kw)
            );
            currentPage = 1;
            refreshPagination();
        });
    }

    // =====================================================================
    // ROW SELECTION
    // =====================================================================

    private void setupRowSelection() {
        tableMesin.getSelectionModel().selectedItemProperty().addListener((obs, lama, baru) -> {
            if (baru != null) {
                isiFormDariTabel(baru);
                hideAllErrorLabels();
            }
        });
    }

    private void isiFormDariTabel(MesinModel m) {
        txtIdMesin.setText(m.getIdMesin());
        txtNamaMesin.setText(m.getNamaMesin());
        txtMerkMesin.setText(m.getMerkMesin());

        txtNamaMesin.setStyle(null);
        txtMerkMesin.setStyle(null);

        BrtSimpan.setDisable(true);
        BtUbah.setDisable(false);
        BtHapus.setDisable(false);

        System.out.println("=== KLIK MESIN ===");
        System.out.println("ID Mesin: " + m.getIdMesin());
        loadRiwayatMesin(m.getIdMesin());
    }

    // =====================================================================
    // LOAD DATA MESIN
    // =====================================================================

    private void loadDataMesin() {
        masterData.clear();

        String sql = "SELECT ID_Mesin, Nama_Mesin, Merk_Mesin, Status_Mesin " +
                "FROM v_TampilSemuaMesin ORDER BY ID_Mesin";

        try (java.sql.Statement st = db.getConnection().createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                masterData.add(new MesinModel(
                        rs.getString("ID_Mesin"),
                        rs.getString("Nama_Mesin"),
                        rs.getString("Merk_Mesin"),
                        rs.getString("Status_Mesin")
                ));
            }

            filteredData = new FilteredList<>(masterData, p -> true);

            sortedData = new SortedList<>(filteredData, new Comparator<MesinModel>() {
                @Override
                public int compare(MesinModel o1, MesinModel o2) {
                    String status1 = o1.getStatusMesin();
                    String status2 = o2.getStatusMesin();

                    if (status1.equalsIgnoreCase(status2)) {
                        return o1.getIdMesin().compareTo(o2.getIdMesin());
                    }

                    boolean isAktif1 = status1.equalsIgnoreCase("Aktif");
                    boolean isAktif2 = status2.equalsIgnoreCase("Aktif");

                    if (isAktif1 && !isAktif2) return -1;
                    if (!isAktif1 && isAktif2) return 1;

                    return 0;
                }
            });

            currentPage = 1;
            refreshPagination();

        } catch (SQLException e) {
            tampilAlert(Alert.AlertType.ERROR, "Gagal memuat data mesin", e.getMessage());
        }
    }

    // =====================================================================
    // LOAD RIWAYAT MAINTENANCE
    // =====================================================================

    private void loadRiwayatMesin(String idMesin) {
        ObservableList<RiwayatModel> riwayatList = FXCollections.observableArrayList();

        System.out.println("=== LOAD RIWAYAT ===");
        System.out.println("ID Mesin: " + idMesin);

        String sql = "SELECT " +
                "mm.ID_Mesin, " +
                "CONVERT(VARCHAR, mm.Tanggal_Maintenance_Mesin, 103) AS Tanggal, " +
                "ISNULL(mm.Keterangan_Perbaikan, mm.Jenis_Kerusakan_Mesin) AS Keterangan " +
                "FROM Maintenance_Mesin mm " +
                "WHERE mm.ID_Mesin = ? " +
                "ORDER BY mm.Tanggal_Maintenance_Mesin DESC";

        try (PreparedStatement ps = db.getConnection().prepareStatement(sql)) {
            ps.setString(1, idMesin);
            System.out.println("Parameter: " + idMesin);

            ResultSet rs = ps.executeQuery();
            int count = 0;
            while (rs.next()) {
                count++;
                riwayatList.add(new RiwayatModel(
                        rs.getString("ID_Mesin"),
                        rs.getString("Tanggal"),
                        rs.getString("Keterangan")
                ));
                System.out.println("Data ke-" + count + ": " + rs.getString("Keterangan"));
            }
            rs.close();

            System.out.println("Total data ditemukan: " + count);
            tableRiwayat.setItems(riwayatList);

            if (riwayatList.isEmpty()) {
                Label emptyLabel = new Label("Belum ada riwayat maintenance untuk mesin ini");
                emptyLabel.setStyle("-fx-text-fill: #888; -fx-font-style: italic;");
                tableRiwayat.setPlaceholder(emptyLabel);
            } else {
                tableRiwayat.setPlaceholder(null);
            }

        } catch (SQLException e) {
            System.out.println("ERROR: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // =====================================================================
    // STAT CARDS
    // =====================================================================

    private void hitungStatCard() {
        try {
            String query = "SELECT " +
                    "dbo.f_TotalSemuaMesin() AS Total, " +
                    "dbo.f_TotalMesinAktif() AS Aktif, " +
                    "dbo.f_TotalMesinNonAktif() AS NonAktif";

            try (java.sql.Statement st = db.getConnection().createStatement();
                 ResultSet rs = st.executeQuery(query)) {
                if (rs.next()) {
                    lblTotalMesin.setText(String.valueOf(rs.getInt("Total")));
                    lblMesinAktif.setText(String.valueOf(rs.getInt("Aktif")));
                    lblMesinNonAktif.setText(String.valueOf(rs.getInt("NonAktif")));
                }
            }
        } catch (SQLException e) {
            tampilAlert(Alert.AlertType.ERROR, "Gagal menghitung statistik", e.getMessage());
        }
    }

    // =====================================================================
    // PAGINATION
    // =====================================================================

    private void refreshPagination() {
        if (sortedData == null) return;

        List<MesinModel> semuaData = new ArrayList<>(sortedData);
        int total = semuaData.size();
        int totalPages = Math.max(1, (int) Math.ceil((double) total / ITEMS_PER_PAGE));

        if (currentPage > totalPages) currentPage = totalPages;
        if (currentPage < 1) currentPage = 1;

        int from = (currentPage - 1) * ITEMS_PER_PAGE;
        int to = Math.min(from + ITEMS_PER_PAGE, total);

        halamanSaatIni = (from < total) ? semuaData.subList(from, to) : new ArrayList<>();

        tableMesin.setItems(FXCollections.observableArrayList(halamanSaatIni));

        lblInfoData.setText("Menampilkan " + halamanSaatIni.size() + " dari " + total + " data");
        lblPageInfo.setText("Hal " + currentPage + " / " + totalPages);

        btnPrev.setDisable(currentPage <= 1);
        btnNext.setDisable(currentPage >= totalPages);
    }

    @FXML
    void handlePrevPage(ActionEvent event) {
        if (currentPage > 1) {
            currentPage--;
            refreshPagination();
        }
    }

    @FXML
    void handleNextPage(ActionEvent event) {
        if (sortedData == null) return;
        int totalPages = Math.max(1, (int) Math.ceil((double) sortedData.size() / ITEMS_PER_PAGE));
        if (currentPage < totalPages) {
            currentPage++;
            refreshPagination();
        }
    }

    // =====================================================================
    // SIMPAN DATA
    // =====================================================================

    @FXML
    void OnSimpan(ActionEvent event) {
        if (checkInputErrors()) {
            tampilAlert(Alert.AlertType.WARNING, "Validasi Gagal", "Mohon perbaiki input yang ditandai merah");
            return;
        }

        if (!validasiForm()) return;

        String sql = "{call sp_TambahMesin(?, ?)}";
        try (CallableStatement cs = db.getConnection().prepareCall(sql)) {
            cs.setString(1, txtNamaMesin.getText().trim());
            cs.setString(2, txtMerkMesin.getText().trim());

            String idBaru = null;
            try (ResultSet rs = cs.executeQuery()) {
                if (rs.next()) {
                    idBaru = rs.getString("ID_Mesin_Baru");
                    tampilAlert(Alert.AlertType.INFORMATION, "Berhasil",
                            "Mesin baru ditambahkan!\nID: " + idBaru + "\nStatus: Aktif");
                }
            }

            String namaLayanan = cbNamaLayanan.getValue();
            if (idBaru != null && namaLayanan != null && !namaLayanan.isEmpty()) {
                simpanDetailProdukMesin(idBaru, namaLayanan);
            }

            loadDataMesin();
            hitungStatCard();
            resetForm();

        } catch (SQLException e) {
            tampilAlert(Alert.AlertType.ERROR, "Gagal menyimpan", e.getMessage());
        }
    }

    // =====================================================================
    // UBAH DATA
    // =====================================================================

    @FXML
    void OnUbah(ActionEvent event) {
        String id = txtIdMesin.getText().trim();
        if (id.isEmpty()) {
            tampilAlert(Alert.AlertType.WARNING, "Peringatan", "Pilih mesin dari tabel terlebih dahulu!");
            return;
        }

        if (checkInputErrors()) {
            tampilAlert(Alert.AlertType.WARNING, "Validasi Gagal", "Mohon perbaiki input yang ditandai merah");
            return;
        }

        if (!validasiForm()) return;

        List<String> opsi = List.of("Aktif", "NonAktif");
        ChoiceDialog<String> dialog = new ChoiceDialog<>("Aktif", opsi);
        dialog.setTitle("Pilih Status Mesin");
        dialog.setHeaderText(null);
        dialog.setContentText("Status Mesin untuk " + id + ":");

        dialog.showAndWait().ifPresent(statusBaru -> {
            if (statusBaru.equalsIgnoreCase("Aktif") && isMesinRusak(id)) {
                Alert warningAlert = new Alert(Alert.AlertType.WARNING);
                warningAlert.setTitle("Tidak Dapat Mengubah Status");
                warningAlert.setHeaderText("❌ Mesin Sedang Rusak");
                warningAlert.setContentText(
                        "Mesin " + id + " sedang dalam status RUSAK di tabel Maintenance!\n\n" +
                                "⚠️ Mesin yang sedang rusak tidak dapat diubah menjadi AKTIF.\n" +
                                "Harap selesaikan maintenance terlebih dahulu di menu Maintenance Mesin.\n\n" +
                                "Status yang diperbolehkan: NonAktif"
                );
                warningAlert.showAndWait();
                return;
            }

            String sql = "{call sp_UpdateMesin(?, ?, ?, ?)}";
            try (CallableStatement cs = db.getConnection().prepareCall(sql)) {
                cs.setString(1, id);
                cs.setString(2, txtNamaMesin.getText().trim());
                cs.setString(3, txtMerkMesin.getText().trim());
                cs.setString(4, statusBaru);
                cs.execute();

                String namaLayanan = cbNamaLayanan.getValue();
                if (namaLayanan != null && !namaLayanan.isEmpty()) {
                    simpanDetailProdukMesin(id, namaLayanan);
                }

                tampilAlert(Alert.AlertType.INFORMATION, "Berhasil", "Data mesin " + id + " berhasil diubah menjadi: " + statusBaru);

                loadDataMesin();
                hitungStatCard();
                resetForm();

            } catch (SQLException e) {
                tampilAlert(Alert.AlertType.ERROR, "Gagal mengubah data", e.getMessage());
            }
        });
    }

    // =====================================================================
    // HAPUS DATA
    // =====================================================================

    @FXML
    void OnHapus(ActionEvent event) {
        String id = txtIdMesin.getText().trim();
        if (id.isEmpty()) {
            tampilAlert(Alert.AlertType.WARNING, "Peringatan", "Pilih mesin dari tabel terlebih dahulu!");
            return;
        }

        if (isMesinRusak(id)) {
            Alert warningAlert = new Alert(Alert.AlertType.WARNING);
            warningAlert.setTitle("Peringatan");
            warningAlert.setHeaderText("⚠️ Mesin Sedang Rusak");
            warningAlert.setContentText(
                    "Mesin " + id + " sedang dalam status RUSAK!\n\n" +
                            "Menonaktifkan mesin yang sedang rusak tidak disarankan.\n" +
                            "Harap selesaikan maintenance terlebih dahulu.\n\n" +
                            "Apakah Anda tetap ingin melanjutkan?"
            );

            ButtonType btnYa = new ButtonType("Ya, Lanjutkan", ButtonBar.ButtonData.OK_DONE);
            ButtonType btnTidak = new ButtonType("Tidak, Batal", ButtonBar.ButtonData.CANCEL_CLOSE);
            warningAlert.getButtonTypes().setAll(btnYa, btnTidak);

            var result = warningAlert.showAndWait();
            if (result.isEmpty() || result.get() == btnTidak) {
                return;
            }
        }

        Alert konfirmasi = new Alert(Alert.AlertType.CONFIRMATION);
        konfirmasi.setTitle("Konfirmasi Nonaktifkan");
        konfirmasi.setHeaderText(null);
        konfirmasi.setContentText("Yakin ingin menonaktifkan mesin " + id + "?\n(Status akan berubah menjadi NonAktif)");

        konfirmasi.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) eksekusiHapus(id);
        });
    }

    private void eksekusiHapus(String idMesin) {
        String sql = "{call sp_DeleteMesinSoft(?)}";
        try (CallableStatement cs = db.getConnection().prepareCall(sql)) {
            cs.setString(1, idMesin);
            cs.execute();

            tampilAlert(Alert.AlertType.INFORMATION, "Berhasil", "Mesin " + idMesin + " berhasil dinonaktifkan.");

            loadDataMesin();
            hitungStatCard();
            resetForm();

        } catch (SQLException e) {
            tampilAlert(Alert.AlertType.ERROR, "Gagal menonaktifkan mesin", e.getMessage());
        }
    }

    // =====================================================================
    // BATAL / RESET
    // =====================================================================

    @FXML
    void OnBatal(ActionEvent event) {
        resetForm();
    }

    private void resetForm() {
        txtIdMesin.clear();
        txtNamaMesin.clear();
        txtMerkMesin.clear();
        cbNamaLayanan.getSelectionModel().clearSelection();

        txtNamaMesin.setStyle(null);
        txtMerkMesin.setStyle(null);

        BrtSimpan.setDisable(false);
        BtUbah.setDisable(true);
        BtHapus.setDisable(true);

        tableRiwayat.setItems(FXCollections.observableArrayList());
        tableMesin.getSelectionModel().clearSelection();

        hideAllErrorLabels();
    }

    // =====================================================================
    // VALIDASI INPUT
    // =====================================================================

    private boolean validasiForm() {
        StringBuilder sb = new StringBuilder();

        if (isKosong(txtNamaMesin.getText())) {
            sb.append("- Nama Mesin wajib diisi.\n");
            showErrorLabel(lblErrorNama, "Nama Mesin wajib diisi");
        } else {
            String nama = txtNamaMesin.getText().trim();
            if (!nama.matches("^[a-zA-Z0-9\\s]+$")) {
                sb.append("- Nama Mesin hanya boleh huruf, angka, dan spasi.\n");
                showErrorLabel(lblErrorNama, "Nama hanya boleh huruf, angka, dan spasi");
            } else {
                hideErrorLabel(lblErrorNama);
            }
        }

        if (isKosong(txtMerkMesin.getText())) {
            sb.append("- Merk Mesin wajib diisi.\n");
            showErrorLabel(lblErrorMerk, "Merk Mesin wajib diisi");
        } else {
            String merk = txtMerkMesin.getText().trim();
            if (!merk.matches("^[a-zA-Z0-9\\s]+$")) {
                sb.append("- Merk Mesin hanya boleh huruf, angka, dan spasi.\n");
                showErrorLabel(lblErrorMerk, "Merk hanya boleh huruf, angka, dan spasi");
            } else {
                hideErrorLabel(lblErrorMerk);
            }
        }

        if (cbNamaLayanan.getValue() == null || cbNamaLayanan.getValue().isEmpty()) {
            sb.append("- Nama Layanan wajib dipilih.\n");
            showErrorLabel(lblErrorMerk1, "Nama Layanan wajib dipilih");
        } else {
            hideErrorLabel(lblErrorMerk1);
        }

        if (sb.length() > 0) {
            tampilAlert(Alert.AlertType.WARNING, "Data belum lengkap atau tidak valid", sb.toString());
            return false;
        }
        return true;
    }

    private boolean isKosong(String s) {
        return s == null || s.trim().isEmpty();
    }

    // =====================================================================
    // HELPER ALERT - PERBAIKAN ✅
    // =====================================================================

    private void tampilAlert(Alert.AlertType tipe, String judul, String pesan) {
        Alert a = new Alert(tipe);
        a.setTitle(judul);
        a.setHeaderText(null);
        a.setContentText(pesan);
        a.showAndWait();
    }

    // =====================================================================
    // MODEL — Mesin
    // =====================================================================

    public static class MesinModel {
        private final StringProperty idMesin;
        private final StringProperty namaMesin;
        private final StringProperty merkMesin;
        private final StringProperty statusMesin;

        public MesinModel(String id, String nama, String merk, String status) {
            this.idMesin = new SimpleStringProperty(id);
            this.namaMesin = new SimpleStringProperty(nama);
            this.merkMesin = new SimpleStringProperty(merk);
            this.statusMesin = new SimpleStringProperty(status);
        }

        public String getIdMesin() { return idMesin.get(); }

        public StringProperty idMesinProperty() { return idMesin; }

        public String getNamaMesin() { return namaMesin.get(); }

        public StringProperty namaMesinProperty() { return namaMesin; }

        public String getMerkMesin() { return merkMesin.get(); }

        public StringProperty merkMesinProperty() { return merkMesin; }

        public String getStatusMesin() { return statusMesin.get(); }

        public StringProperty statusMesinProperty() { return statusMesin; }
    }

    // =====================================================================
    // MODEL — Riwayat Maintenance
    // =====================================================================

    public static class RiwayatModel {
        private final StringProperty idMesin;
        private final StringProperty tanggal;
        private final StringProperty keterangan;

        public RiwayatModel(String idMesin, String tanggal, String keterangan) {
            this.idMesin = new SimpleStringProperty(idMesin);
            this.tanggal = new SimpleStringProperty(tanggal);
            this.keterangan = new SimpleStringProperty(keterangan);
        }

        public StringProperty idMesinProperty() { return idMesin; }

        public StringProperty tanggalProperty() { return tanggal; }

        public StringProperty keteranganProperty() { return keterangan; }
    }

    // =====================================================================
    // MODEL — Detail Produk Mesin
    // =====================================================================

    public static class DetailProdukMesinModel {
        private final StringProperty namaProduk;
        private final StringProperty kategori;
        private final StringProperty merkBarang;
        private final DoubleProperty harga;

        public DetailProdukMesinModel(String namaProduk, String kategori, String merkBarang, double harga) {
            this.namaProduk = new SimpleStringProperty(namaProduk);
            this.kategori = new SimpleStringProperty(kategori);
            this.merkBarang = new SimpleStringProperty(merkBarang);
            this.harga = new SimpleDoubleProperty(harga);
        }

        public StringProperty namaProdukProperty() { return namaProduk; }

        public StringProperty kategoriProperty() { return kategori; }

        public StringProperty merkBarangProperty() { return merkBarang; }

        public DoubleProperty hargaProperty() { return harga; }
    }
}