package SistemFotocopy;

import Database.DBConnection;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;

import java.sql.CallableStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class DataMesin {

    // =====================================================================
    // FXML BINDINGS
    // =====================================================================

    // Form fields
    @FXML private TextField txtIdMesin;
    @FXML private TextField txtNamaMesin;
    @FXML private TextField txtMerkMesin;

    // Buttons
    @FXML private Button BrtSimpan;
    @FXML private Button BtUbah;
    @FXML private Button BtHapus;
    @FXML private Button BtBatal;

    // Tabel Mesin
    @FXML private TableView<MesinModel>    tableMesin;
    @FXML private TableColumn<MesinModel, String> colIdMesin;
    @FXML private TableColumn<MesinModel, String> colNamaMesin;
    @FXML private TableColumn<MesinModel, String> colMerkMesin;
    @FXML private TableColumn<MesinModel, String> colStatusMesin;

    // Tabel Riwayat Maintenance
    @FXML private TableView<RiwayatModel>    tableRiwayat;
    @FXML private TableColumn<RiwayatModel, String> colRiwayatId;
    @FXML private TableColumn<RiwayatModel, String> colRiwayatTanggal;
    @FXML private TableColumn<RiwayatModel, String> colRiwayatKeterangan;

    // Search & info
    @FXML private TextField txtCariMesin;
    @FXML private Label     lblInfoData;

    // Stat cards
    @FXML private Label lblTotalMesin;
    @FXML private Label lblMesinAktif;
    @FXML private Label lblMesinNonAktif;

    // Pagination
    @FXML private Button btnPrev;
    @FXML private Button btnNext;
    @FXML private Label  lblPageInfo;

    // =====================================================================
    // STATE
    // =====================================================================

    private final DBConnection db = new DBConnection();

    // Master data untuk tabel mesin
    private final ObservableList<MesinModel> masterData    = FXCollections.observableArrayList();
    private FilteredList<MesinModel>         filteredData;
    private SortedList<MesinModel>           sortedData;

    // Pagination - 5 ITEMS PER PAGE
    private static final int ITEMS_PER_PAGE = 5;  // <-- DIUBAH JADI 5
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

        loadDataMesin();
        hitungStatCard();
        resetForm();
    }

    // =====================================================================
    // SETUP KOLOM TABEL
    // =====================================================================

    private void setupTableMesin() {
        colIdMesin.setCellValueFactory(d -> d.getValue().idMesinProperty());
        colNamaMesin.setCellValueFactory(d -> d.getValue().namaMesinProperty());
        colMerkMesin.setCellValueFactory(d -> d.getValue().merkMesinProperty());
        colStatusMesin.setCellValueFactory(d -> d.getValue().statusMesinProperty());

        // Warna status
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
            );
            currentPage = 1;
            refreshPagination();
        });
    }

    // =====================================================================
    // ROW SELECTION -> ISI FORM
    // =====================================================================

    private void setupRowSelection() {
        tableMesin.getSelectionModel().selectedItemProperty().addListener((obs, lama, baru) -> {
            if (baru != null) isiFormDariTabel(baru);
        });
    }

    private void isiFormDariTabel(MesinModel m) {
        txtIdMesin.setText(m.getIdMesin());
        txtNamaMesin.setText(m.getNamaMesin());
        txtMerkMesin.setText(m.getMerkMesin());

        // Aktifkan Ubah & Hapus, matikan Simpan
        BrtSimpan.setDisable(true);
        BtUbah.setDisable(false);
        BtHapus.setDisable(false);

        // Muat riwayat maintenance untuk mesin ini
        loadRiwayatMesin(m.getIdMesin());
    }

    // =====================================================================
    // LOAD DATA MESIN (READ) — pakai query langsung
    // =====================================================================

    private void loadDataMesin() {
        masterData.clear();

        String sql = "SELECT ID_Mesin, Nama_Mesin, Merk_Mesin, Status_Mesin " +
                "FROM Mesin ORDER BY ID_Mesin";

        try (ResultSet rs = db.stat.executeQuery(sql)) {
            while (rs.next()) {
                masterData.add(new MesinModel(
                        rs.getString("ID_Mesin"),
                        rs.getString("Nama_Mesin"),
                        rs.getString("Merk_Mesin"),
                        rs.getString("Status_Mesin")
                ));
            }

            // =============================================================
            // SORTING: Aktif di atas, NonAktif di bawah
            // =============================================================
            filteredData = new FilteredList<>(masterData, p -> true);

            // Buat SortedList dengan comparator khusus
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

        String sql = "SELECT ID_Mesin, " +
                "CONVERT(VARCHAR, Tanggal_Maintenance_Mesin, 103) AS Tanggal, " +
                "ISNULL(Keterangan_Perbaikan, Jenis_Kerusakan_Mesin) AS Keterangan " +
                "FROM Maintenance_Mesin " +
                "WHERE ID_Mesin = '" + idMesin + "' " +
                "ORDER BY Tanggal_Maintenance_Mesin DESC";

        try (ResultSet rs = db.stat.executeQuery(sql)) {
            while (rs.next()) {
                riwayatList.add(new RiwayatModel(
                        rs.getString("ID_Mesin"),
                        rs.getString("Tanggal"),
                        rs.getString("Keterangan")
                ));
            }
            tableRiwayat.setItems(riwayatList);

        } catch (SQLException e) {
            tampilAlert(Alert.AlertType.ERROR, "Gagal memuat riwayat", e.getMessage());
        }
    }

    // =====================================================================
    // STAT CARDS
    // =====================================================================

    private void hitungStatCard() {
        String sql = "SELECT " +
                "  COUNT(*) AS Total, " +
                "  SUM(CASE WHEN Status_Mesin = 'Aktif' THEN 1 ELSE 0 END) AS Aktif, " +
                "  SUM(CASE WHEN Status_Mesin = 'NonAktif' THEN 1 ELSE 0 END) AS NonAktif " +
                "FROM Mesin";

        try (ResultSet rs = db.stat.executeQuery(sql)) {
            if (rs.next()) {
                lblTotalMesin.setText(String.valueOf(rs.getInt("Total")));
                lblMesinAktif.setText(String.valueOf(rs.getInt("Aktif")));
                lblMesinNonAktif.setText(String.valueOf(rs.getInt("NonAktif")));
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
        int total      = semuaData.size();
        int totalPages = Math.max(1, (int) Math.ceil((double) total / ITEMS_PER_PAGE));

        // Pastikan currentPage tidak melebihi totalPages
        if (currentPage > totalPages) currentPage = totalPages;
        if (currentPage < 1)          currentPage = 1;

        int from = (currentPage - 1) * ITEMS_PER_PAGE;
        int to   = Math.min(from + ITEMS_PER_PAGE, total);

        halamanSaatIni = (from < total) ? semuaData.subList(from, to) : new ArrayList<>();

        tableMesin.setItems(FXCollections.observableArrayList(halamanSaatIni));

        // Update label
        lblInfoData.setText("Menampilkan " + halamanSaatIni.size() + " dari " + total + " data");
        lblPageInfo.setText("Hal " + currentPage + " / " + totalPages);

        // Aktifkan / nonaktifkan tombol
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
    // SIMPAN DATA — sp_TambahMesin
    // =====================================================================

    @FXML
    void OnSimpan(ActionEvent event) {
        if (!validasiForm()) return;

        String sql = "{call sp_TambahMesin(?, ?)}";
        try (CallableStatement cs = db.getConnection().prepareCall(sql)) {
            cs.setString(1, txtNamaMesin.getText().trim());
            cs.setString(2, txtMerkMesin.getText().trim());

            try (ResultSet rs = cs.executeQuery()) {
                if (rs.next()) {
                    String idBaru = rs.getString("ID_Mesin_Baru");
                    tampilAlert(Alert.AlertType.INFORMATION, "Berhasil",
                            "Mesin baru ditambahkan!\nID: " + idBaru + "\nStatus: Aktif");
                }
            }

            loadDataMesin();
            hitungStatCard();
            resetForm();

        } catch (SQLException e) {
            tampilAlert(Alert.AlertType.ERROR, "Gagal menyimpan", e.getMessage());
        }
    }

    // =====================================================================
    // UBAH DATA — sp_UpdateMesin
    // =====================================================================

    @FXML
    void OnUbah(ActionEvent event) {
        String id = txtIdMesin.getText().trim();
        if (id.isEmpty()) {
            tampilAlert(Alert.AlertType.WARNING, "Peringatan", "Pilih mesin dari tabel terlebih dahulu!");
            return;
        }
        if (!validasiForm()) return;

        // Tanya status baru via dialog pilihan
        List<String> opsi = List.of("Aktif", "NonAktif");
        ChoiceDialog<String> dialog = new ChoiceDialog<>("Aktif", opsi);
        dialog.setTitle("Pilih Status Mesin");
        dialog.setHeaderText(null);
        dialog.setContentText("Status Mesin untuk " + id + ":");

        dialog.showAndWait().ifPresent(statusBaru -> {
            String sql = "{call sp_UpdateMesin(?, ?, ?, ?)}";
            try (CallableStatement cs = db.getConnection().prepareCall(sql)) {
                cs.setString(1, id);
                cs.setString(2, txtNamaMesin.getText().trim());
                cs.setString(3, txtMerkMesin.getText().trim());
                cs.setString(4, statusBaru);
                cs.execute();

                tampilAlert(Alert.AlertType.INFORMATION, "Berhasil", "Data mesin " + id + " berhasil diubah.");

                loadDataMesin();
                hitungStatCard();
                resetForm();

            } catch (SQLException e) {
                tampilAlert(Alert.AlertType.ERROR, "Gagal mengubah data", e.getMessage());
            }
        });
    }

    // =====================================================================
    // HAPUS DATA — sp_DeleteMesinSoft
    // =====================================================================

    @FXML
    void OnHapus(ActionEvent event) {
        String id = txtIdMesin.getText().trim();
        if (id.isEmpty()) {
            tampilAlert(Alert.AlertType.WARNING, "Peringatan", "Pilih mesin dari tabel terlebih dahulu!");
            return;
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

        BrtSimpan.setDisable(false);
        BtUbah.setDisable(true);
        BtHapus.setDisable(true);

        tableRiwayat.setItems(FXCollections.observableArrayList());
        tableMesin.getSelectionModel().clearSelection();
    }

    // =====================================================================
    // VALIDASI INPUT
    // =====================================================================

    private boolean validasiForm() {
        StringBuilder sb = new StringBuilder();
        if (isKosong(txtNamaMesin.getText())) sb.append("- Nama Mesin wajib diisi.\n");
        if (isKosong(txtMerkMesin.getText()))  sb.append("- Merk Mesin wajib diisi.\n");

        if (sb.length() > 0) {
            tampilAlert(Alert.AlertType.WARNING, "Data belum lengkap", sb.toString());
            return false;
        }
        return true;
    }

    private boolean isKosong(String s) {
        return s == null || s.trim().isEmpty();
    }

    // =====================================================================
    // HELPER ALERT
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
            this.idMesin    = new SimpleStringProperty(id);
            this.namaMesin  = new SimpleStringProperty(nama);
            this.merkMesin  = new SimpleStringProperty(merk);
            this.statusMesin = new SimpleStringProperty(status);
        }

        public String getIdMesin()    { return idMesin.get(); }
        public StringProperty idMesinProperty() { return idMesin; }

        public String getNamaMesin()  { return namaMesin.get(); }
        public StringProperty namaMesinProperty() { return namaMesin; }

        public String getMerkMesin()  { return merkMesin.get(); }
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
            this.idMesin    = new SimpleStringProperty(idMesin);
            this.tanggal    = new SimpleStringProperty(tanggal);
            this.keterangan = new SimpleStringProperty(keterangan);
        }

        public StringProperty idMesinProperty()    { return idMesin; }
        public StringProperty tanggalProperty()    { return tanggal; }
        public StringProperty keteranganProperty() { return keterangan; }
    }
}