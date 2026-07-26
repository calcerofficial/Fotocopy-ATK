package SistemFotocopy.Transaksi.TransaksiPenjualan.Transaksi.Controller;

import Database.DBConnection;
import SistemFotocopy.Transaksi.TransaksiPenjualan.Transaksi.Dataclass.DataMesinStatus;
import SistemFotocopy.Transaksi.TransaksiPenjualan.Transaksi.Dataclass.DetailDataPenjualan;
import SistemFotocopy.Transaksi.TransaksiPenjualan.DetailPenjualan.Controller.DetailPenjualan;
import SistemFotocopy.UserSession;
import javafx.animation.PauseTransition;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.IOException;
import java.net.URL;
import java.sql.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ResourceBundle;

public class TransaksiPenjualan implements Initializable {

    @FXML
    private Button btnBatal, btnSimpan, btnTambah;

    @FXML
    private ComboBox<String> cbMetodePembayaran, cbNamaBarang, cbNamaLayanan;

    @FXML
    private TableColumn<DetailDataPenjualan, String> colHarga, colIdPenjualan, colJumlah, colNamaBarang;

    @FXML
    private TableColumn<DetailDataPenjualan, Void> colAksi;

    @FXML
    private DatePicker dpTanggal;

    @FXML
    private VBox emptyState;

    @FXML
    private Label lblProdukTerjual, lblTotalHarga, lblTotalPenjualan, lblTotalTransaksi;

    @FXML
    private TableView<DetailDataPenjualan> tblPenjualan;

    @FXML
    private TextField txtIdPegawai, txtIdPenjualan, txtJumlah, txtKembalian, txtUangBayar;

    @FXML
    private Label lblErrorUangBayar;

    private Connection conn;
    private ObservableList<DetailDataPenjualan> detailList = FXCollections.observableArrayList();
    private double totalHarga = 0;
    private int totalProdukTerjual = 0;
    private boolean isUpdating = false;

    private String idPegawai;

    // Data untuk struk
    private String strukIdPenjualan;
    private String strukPegawai;
    private String strukTanggal;
    private String strukMetode;
    private double strukTotalHarga;
    private double strukUangBayar;
    private double strukKembalian;
    private ObservableList<DetailDataPenjualan> strukDetailList;
    private String strukStatus;

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        conn = DBConnection.getConnection();

        dpTanggal.setValue(LocalDate.now());
        dpTanggal.setEditable(false);

        setPegawaiFromSession();
        generateIdPenjualan();

        cbMetodePembayaran.setItems(FXCollections.observableArrayList("Cash", "Transfer"));
        cbMetodePembayaran.setValue("Cash");

        loadProdukData();
        loadLayananData();
        setupTableColumns();
        setupMetodePembayaranListener();
        setupValidations();

        txtKembalian.setEditable(false);
        txtKembalian.setStyle("-fx-background-color: #f0f0f0;");

        btnTambah.setOnAction(e -> tambahItem());
        btnSimpan.setOnAction(e -> simpanTransaksi());
        btnBatal.setOnAction(e -> batalTransaksi());

        updateTotalHarga();
        updateStatisticsCards();

        if (lblErrorUangBayar != null) {
            lblErrorUangBayar.setVisible(false);
        }
    }

    // =========================================================
    // SESSION - AMBIL ID PEGAWAI
    // =========================================================
    private void setPegawaiFromSession() {
        UserSession session = UserSession.getInstance();
        idPegawai = session.getIdPegawai();

        if (idPegawai != null && !idPegawai.isEmpty()) {
            String namaPegawai = getNamaPegawai(idPegawai);
            txtIdPegawai.setText(namaPegawai);
        } else {
            txtIdPegawai.setText("ID Pegawai Tidak Ditemukan");
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

    private void generateIdPenjualan() {
        String id = "PJN" + String.format("%03d", getNextId());
        txtIdPenjualan.setText(id);
    }

    private int getNextId() {
        int nextId = 1;
        String query = "SELECT MAX(CAST(SUBSTRING(ID_Penjualan, 4, LEN(ID_Penjualan)) AS INT)) AS MaxID FROM Penjualan";
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

    // =========================================================
    // LOAD DATA
    // =========================================================
    private void loadProdukData() {
        ObservableList<String> produkList = FXCollections.observableArrayList();
        String query = "SELECT ID_Produk, Nama_Barang FROM Produk WHERE Kategori_Produk = 'barang' AND Status_Barang = 'tersedia' AND Stok > 0 ORDER BY Nama_Barang";
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {
            while (rs.next()) {
                produkList.add(rs.getString("ID_Produk") + " - " + rs.getString("Nama_Barang"));
            }
            cbNamaBarang.setItems(produkList);
        } catch (SQLException e) {
            e.printStackTrace();
            showAlert("Error", "Gagal memuat data produk: " + e.getMessage());
        }
    }

    private void loadLayananData() {
        ObservableList<String> layananList = FXCollections.observableArrayList();
        // PERBAIKAN PENTING: Hapus filter 'AND m.Status_Mesin = 'Aktif'' agar layanan tetap muncul meski ada mesin NonAktif
        String query =
                "SELECT DISTINCT p.ID_Produk, p.Nama_Barang " +
                        "FROM Produk p " +
                        "INNER JOIN DetailProdukMesin dpm ON p.ID_Produk = dpm.ID_Produk " +
                        "INNER JOIN Mesin m ON dpm.ID_Mesin = m.ID_Mesin " +
                        "WHERE p.Kategori_Produk = 'layanan' " +
                        "AND p.Status_Barang = 'tersedia' " +
                        "ORDER BY p.Nama_Barang";

        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {
            while (rs.next()) {
                layananList.add(rs.getString("ID_Produk") + " - " + rs.getString("Nama_Barang"));
            }
            cbNamaLayanan.setItems(layananList);
            if (!layananList.isEmpty()) {
                cbNamaLayanan.setValue(null);
            }
        } catch (SQLException e) {
            e.printStackTrace();
            showAlert("Error", "Gagal memuat data layanan: " + e.getMessage());
        }
    }

    // =========================================================
    // CEK STATUS MESIN LAYANAN
    // =========================================================
    private ObservableList<DataMesinStatus> getMesinStatusLayanan(String idProduk) {
        ObservableList<DataMesinStatus> mesinList = FXCollections.observableArrayList();
        String query =
                "SELECT " +
                        "    m.ID_Mesin, " +
                        "    m.Nama_Mesin, " +
                        "    m.Status_Mesin " +
                        "FROM DetailProdukMesin dpm " +
                        "INNER JOIN Mesin m ON dpm.ID_Mesin = m.ID_Mesin " +
                        "WHERE dpm.ID_Produk = ? " +
                        "GROUP BY m.ID_Mesin, m.Nama_Mesin, m.Status_Mesin";

        try (PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setString(1, idProduk);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    String idMesin = rs.getString("ID_Mesin");
                    String namaMesin = rs.getString("Nama_Mesin");
                    String statusMesin = rs.getString("Status_Mesin");
                    mesinList.add(new DataMesinStatus(idMesin, namaMesin, statusMesin, statusMesin));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return mesinList;
    }

    private boolean isLayananTersedia(String idProduk) {
        ObservableList<DataMesinStatus> mesinList = getMesinStatusLayanan(idProduk);

        // Jika tidak ada mesin yang terdaftar sama sekali
        if (mesinList.isEmpty()) {
            return false;
        }

        // Cek apakah ada setidaknya 1 mesin dengan status "Aktif"
        for (DataMesinStatus mesin : mesinList) {
            if ("Aktif".equalsIgnoreCase(mesin.getStatusMesin())) {
                return true; // Karena ada minimal 1 mesin aktif, layanan valid!
            }
        }
        return false;
    }

    private String getInfoMesinLayanan(String idProduk) {
        ObservableList<DataMesinStatus> mesinList = getMesinStatusLayanan(idProduk);
        StringBuilder info = new StringBuilder();

        if (mesinList.isEmpty()) {
            return "❌ Belum ada mesin yang terdaftar untuk layanan ini!";
        }

        int totalAktif = 0;
        int totalNonAktif = 0;

        for (DataMesinStatus mesin : mesinList) {
            if ("Aktif".equalsIgnoreCase(mesin.getStatusMesin())) {
                totalAktif++;
            } else {
                totalNonAktif++;
            }
        }

        info.append("📊 Status Mesin:\n");
        info.append("   • Total mesin terdaftar: ").append(mesinList.size()).append("\n");
        info.append("   • Mesin AKTIF siap pakai: ").append(totalAktif).append("\n");
        info.append("   • Mesin TIDAK AKTIF/Rusak: ").append(totalNonAktif).append("\n\n");

        info.append("📋 Detail Mesin:\n");
        for (DataMesinStatus mesin : mesinList) {
            String statusIcon = "🟢"; // Hijau = Aktif
            if (!"Aktif".equalsIgnoreCase(mesin.getStatusMesin())) {
                statusIcon = "🔴"; // Merah = NonAktif / Rusak
            }
            info.append("   ").append(statusIcon).append(" ").append(mesin.getNamaMesin())
                    .append(" (Status Database: ").append(mesin.getStatusMesin()).append(")\n");
        }

        return info.toString();
    }

    private boolean cekLayananMemilikiMesin(String idProduk) {
        // PERBAIKAN PENTING: Panggil fungsi getMesinStatusLayanan, jangan panggil dirinya sendiri (cegah infinite loop)
        ObservableList<DataMesinStatus> mesinList = getMesinStatusLayanan(idProduk);
        return !mesinList.isEmpty();
    }

    // =========================================================
    // SETUP TABLE & VALIDASI
    // =========================================================
    private void setupTableColumns() {
        colIdPenjualan.setCellValueFactory(new PropertyValueFactory<>("idPenjualan"));
        colNamaBarang.setCellValueFactory(new PropertyValueFactory<>("namaBarang"));
        colJumlah.setCellValueFactory(new PropertyValueFactory<>("jumlah"));
        colHarga.setCellValueFactory(new PropertyValueFactory<>("hargaFormatted"));

        colAksi.setCellFactory(param -> new TableCell<DetailDataPenjualan, Void>() {
            private final Button btnHapus = new Button("✕");

            {
                btnHapus.setStyle(
                        "-fx-background-color: #dc3545; " +
                                "-fx-text-fill: white; " +
                                "-fx-font-weight: bold; " +
                                "-fx-font-size: 12px; " +
                                "-fx-cursor: hand; " +
                                "-fx-background-radius: 4; " +
                                "-fx-padding: 4 8 4 8;"
                );
                btnHapus.setOnAction(event -> {
                    DetailDataPenjualan item = getTableView().getItems().get(getIndex());
                    if (item != null) {
                        totalHarga -= item.getJumlah() * item.getHarga();
                        totalProdukTerjual -= item.getJumlah();

                        detailList.remove(item);

                        updateTotalHarga();
                        tblPenjualan.refresh();

                        if (detailList.isEmpty()) {
                            emptyState.setVisible(true);
                        }

                        if ("Transfer".equals(cbMetodePembayaran.getValue())) {
                            String formatted = formatRupiah((long) totalHarga);
                            isUpdating = true;
                            txtUangBayar.setText(formatted);
                            isUpdating = false;
                            hitungKembalian((long) totalHarga);
                        }
                    }
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    setAlignment(Pos.CENTER);
                    setGraphic(btnHapus);
                }
            }
        });

        tblPenjualan.setItems(detailList);
    }

    private void setupMetodePembayaranListener() {
        cbMetodePembayaran.valueProperty().addListener((obs, oldVal, newVal) -> {
            if ("Transfer".equals(newVal)) {
                String formatted = formatRupiah((long) totalHarga);
                txtUangBayar.setText(formatted);
                txtUangBayar.setDisable(true);
                txtKembalian.setText("Rp 0");
                txtKembalian.setDisable(true);
                if (lblErrorUangBayar != null) {
                    lblErrorUangBayar.setVisible(false);
                }
            } else {
                txtUangBayar.setDisable(false);
                txtKembalian.setDisable(false);
                txtUangBayar.clear();
                txtKembalian.clear();
                if (lblErrorUangBayar != null) {
                    lblErrorUangBayar.setVisible(false);
                }
            }
        });
    }

    private void setupValidations() {
        txtJumlah.textProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal.matches("\\d*")) {
                txtJumlah.setText(newVal.replaceAll("[^\\d]", ""));
            }
            if (!newVal.isEmpty() && Integer.parseInt(newVal) == 0) {
                txtJumlah.setText("");
            }
        });

        txtUangBayar.textProperty().addListener((obs, oldVal, newVal) -> {
            if (isUpdating) return;

            String clean = newVal.replaceAll("[^\\d]", "");

            if (clean.isEmpty()) {
                isUpdating = true;
                txtUangBayar.setText("");
                isUpdating = false;
                hitungKembalian(0);
                if (lblErrorUangBayar != null) {
                    lblErrorUangBayar.setVisible(false);
                }
                return;
            }

            try {
                long number = Long.parseLong(clean);
                String formatted = formatRupiah(number);

                isUpdating = true;
                txtUangBayar.setText(formatted);
                txtUangBayar.positionCaret(formatted.length());
                isUpdating = false;

                if (number < (long) totalHarga) {
                    if (lblErrorUangBayar != null) {
                        lblErrorUangBayar.setText("⚠️ Uang bayar kurang dari total harga!");
                        lblErrorUangBayar.setStyle("-fx-text-fill: red; -fx-font-size: 12px;");
                        lblErrorUangBayar.setVisible(true);
                    }
                } else {
                    if (lblErrorUangBayar != null) {
                        lblErrorUangBayar.setVisible(false);
                    }
                }

                hitungKembalian(number);
            } catch (NumberFormatException e) {
                // Ignore
            }
        });
    }

    private String formatRupiah(long number) {
        return "Rp " + String.format("%,d", number).replace(',', '.');
    }

    private void hitungKembalian(long uangBayar) {
        long kembalian = uangBayar - (long) totalHarga;
        if (kembalian < 0) kembalian = 0;
        String formatted = formatRupiah(kembalian);
        isUpdating = true;
        txtKembalian.setText(formatted);
        isUpdating = false;
    }

    // =========================================================
    // TAMBAH ITEM
    // =========================================================
    private void tambahItem() {
        String selectedBarang = cbNamaBarang.getValue();
        String selectedLayanan = cbNamaLayanan.getValue();

        if (selectedBarang == null && selectedLayanan == null) {
            showAlert("Error", "Silakan pilih Nama Barang atau Layanan!");
            return;
        }

        String jumlahText = txtJumlah.getText();
        if (jumlahText == null || jumlahText.isEmpty()) {
            showAlert("Error", "Jumlah harus diisi!");
            return;
        }

        int jumlah = Integer.parseInt(jumlahText);
        if (jumlah <= 0) {
            showAlert("Error", "Jumlah harus lebih dari 0!");
            return;
        }

        double hargaLayanan = 0;
        double hargaBarang = 0;

        // Validasi Layanan
        if (selectedLayanan != null) {
            String idProduk = selectedLayanan.split(" - ")[0];
            String namaProduk = selectedLayanan.split(" - ")[1];

            boolean hasMesin = cekLayananMemilikiMesin(idProduk);
            if (!hasMesin) {
                showAlert("Error",
                        "❌ Layanan '" + namaProduk + "' belum terdaftar dengan mesin apapun!\n" +
                                "Harap daftarkan mesin terlebih dahulu.");
                return;
            }

            boolean isTersedia = isLayananTersedia(idProduk);
            if (!isTersedia) {
                String infoMesin = getInfoMesinLayanan(idProduk);
                showAlert("Layanan Tidak Tersedia",
                        "❌ Layanan '" + namaProduk + "' TIDAK TERSEDIA!\n\n" +
                                "Semua mesin yang terhubung dengan layanan ini sedang tidak dapat digunakan.\n\n" +
                                infoMesin);
                return;
            }

            String infoMesin = getInfoMesinLayanan(idProduk);

            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
            confirm.setTitle("Konfirmasi Layanan");
            confirm.setHeaderText("Layanan '" + namaProduk + "' tersedia");
            confirm.setContentText(
                    "Layanan ini dapat digunakan dengan mesin yang aktif.\n\n" +
                            infoMesin + "\n" +
                            "Harga: " + formatRupiah((long) getHargaProduk(idProduk)) + "\n" +
                            "Jumlah: " + jumlah + "\n\n" +
                            "Lanjutkan penambahan?"
            );

            ButtonType btnYa = new ButtonType("Ya, Tambahkan", ButtonBar.ButtonData.YES);
            ButtonType btnTidak = new ButtonType("Batal", ButtonBar.ButtonData.NO);
            confirm.getButtonTypes().setAll(btnYa, btnTidak);

            if (confirm.showAndWait().orElse(btnTidak) != btnYa) {
                return;
            }

            hargaLayanan = getHargaProduk(idProduk);
            if (hargaLayanan <= 0) {
                showAlert("Error", "Harga layanan '" + namaProduk + "' tidak valid!");
                return;
            }
        }

        // Validasi Barang
        if (selectedBarang != null) {
            String idProduk = selectedBarang.split(" - ")[0];
            String namaProduk = selectedBarang.split(" - ")[1];

            int stok = getStokProduk(idProduk);
            if (stok < jumlah) {
                showAlert("Error", "Stok '" + namaProduk + "' tidak mencukupi! Tersedia: " + stok);
                return;
            }

            hargaBarang = getHargaProduk(idProduk);
            if (hargaBarang <= 0) {
                showAlert("Error", "Harga barang '" + namaProduk + "' tidak valid!");
                return;
            }
        }

        // Tambahkan ke tabel
        if (selectedLayanan != null) {
            String idProduk = selectedLayanan.split(" - ")[0];
            String namaProduk = selectedLayanan.split(" - ")[1];

            DetailDataPenjualan data = new DetailDataPenjualan(txtIdPenjualan.getText(), idProduk, namaProduk, jumlah, hargaLayanan);
            detailList.add(data);
            totalHarga += jumlah * hargaLayanan;
            totalProdukTerjual += jumlah;
        }

        if (selectedBarang != null) {
            String idProduk = selectedBarang.split(" - ")[0];
            String namaProduk = selectedBarang.split(" - ")[1];

            DetailDataPenjualan data = new DetailDataPenjualan(txtIdPenjualan.getText(), idProduk, namaProduk, jumlah, hargaBarang);
            detailList.add(data);
            totalHarga += jumlah * hargaBarang;
            totalProdukTerjual += jumlah;
        }

        tblPenjualan.setItems(detailList);
        emptyState.setVisible(false);
        updateTotalHarga();

        cbNamaBarang.setValue(null);
        cbNamaLayanan.setValue(null);
        txtJumlah.clear();

        if ("Transfer".equals(cbMetodePembayaran.getValue())) {
            String formatted = formatRupiah((long) totalHarga);
            isUpdating = true;
            txtUangBayar.setText(formatted);
            isUpdating = false;
            hitungKembalian((long) totalHarga);
            if (lblErrorUangBayar != null) {
                lblErrorUangBayar.setVisible(false);
            }
        }

        showAlert("Sukses", "Item berhasil ditambahkan!");
    }

    private double getHargaProduk(String idProduk) {
        String query = "SELECT Harga FROM Produk WHERE ID_Produk = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setString(1, idProduk);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getDouble("Harga");
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    private int getStokProduk(String idProduk) {
        String query = "SELECT Stok FROM Produk WHERE ID_Produk = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setString(1, idProduk);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("Stok");
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    private void updateTotalHarga() {
        lblTotalHarga.setText("Rp " + String.format("%,d", (long) totalHarga).replace(',', '.'));
        lblProdukTerjual.setText(String.valueOf(totalProdukTerjual));
    }

    // =========================================================
    // STATISTICS
    // =========================================================
    private void updateStatisticsCards() {
        try {
            String query = "SELECT " +
                    "dbo.f_TotalTransaksiSemua() AS Transaksi, " +
                    "dbo.f_TotalPenjualanSemua() AS Penjualan";

            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(query)) {
                if (rs.next()) {
                    lblTotalTransaksi.setText(String.valueOf(rs.getInt("Transaksi")));
                    double total = rs.getDouble("Penjualan");
                    lblTotalPenjualan.setText("Rp " + String.format("%,d", (long) total).replace(',', '.'));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            // FALLBACK
            try {
                String q1 = "SELECT COUNT(*) FROM Penjualan WHERE Status_Penjualan='Lunas'";
                try (Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(q1)) {
                    if (rs.next()) lblTotalTransaksi.setText(String.valueOf(rs.getInt(1)));
                }
                String q2 = "SELECT ISNULL(SUM(Total_Harga),0) FROM Penjualan WHERE Status_Penjualan='Lunas'";
                try (Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(q2)) {
                    if (rs.next()) {
                        double total = rs.getDouble(1);
                        lblTotalPenjualan.setText("Rp " + String.format("%,d", (long) total).replace(',', '.'));
                    }
                }
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
        }
    }

    // =========================================================
    // SIMPAN TRANSAKSI
    // =========================================================
    private void simpanTransaksi() {
        if (detailList.isEmpty()) {
            showAlert("Error", "Tidak ada item yang dijual!");
            return;
        }

        // CEK STOK BARANG DAN LAYANAN
        for (DetailDataPenjualan data : detailList) {
            String idProduk = data.getIdProduk();
            int jumlah = data.getJumlah();

            String kategori = getKategoriProduk(idProduk);
            if ("Barang".equalsIgnoreCase(kategori)) {
                int stok = getStokProduk(idProduk);
                if (stok < jumlah) {
                    showAlert("Error", "Stok " + data.getNamaBarang() + " tidak mencukupi!\n" +
                            "Tersedia: " + stok + ", Dibutuhkan: " + jumlah);
                    return;
                }
            }

            if ("Layanan".equalsIgnoreCase(kategori)) {
                if (!isLayananTersedia(idProduk)) {
                    String infoMesin = getInfoMesinLayanan(idProduk);
                    showAlert("Layanan Tidak Tersedia",
                            "❌ Layanan '" + data.getNamaBarang() + "' tidak tersedia!\n\n" +
                                    "Mesin yang diperlukan sedang rusak atau tidak aktif.\n\n" +
                                    infoMesin);
                    return;
                }
            }
        }

        String metode = cbMetodePembayaran.getValue();
        if (metode == null) {
            showAlert("Error", "Silakan pilih Metode Pembayaran!");
            return;
        }

        double uangBayar = 0;
        if ("Cash".equals(metode)) {
            String uangBayarText = txtUangBayar.getText();
            if (uangBayarText == null || uangBayarText.isEmpty()) {
                showAlert("Error", "Uang Bayar harus diisi!");
                return;
            }
            String clean = uangBayarText.replaceAll("[^\\d]", "");
            if (clean.isEmpty()) {
                showAlert("Error", "Uang Bayar harus diisi!");
                return;
            }
            uangBayar = Double.parseDouble(clean);

            if (uangBayar < totalHarga) {
                if (lblErrorUangBayar != null) {
                    lblErrorUangBayar.setText("⚠️ Uang bayar kurang dari total harga!");
                    lblErrorUangBayar.setStyle("-fx-text-fill: red; -fx-font-size: 12px; -fx-font-weight: bold;");
                    lblErrorUangBayar.setVisible(true);
                }
                showAlert("Error", "Uang Bayar kurang dari Total Harga!");
                return;
            }
        } else {
            uangBayar = totalHarga;
        }

        if (lblErrorUangBayar != null) {
            lblErrorUangBayar.setVisible(false);
        }

        prosesSimpanTransaksi("Lunas", metode, uangBayar);
    }

    // =========================================================
    // BATAL TRANSAKSI
    // =========================================================
    private void batalTransaksi() {
        if (detailList.isEmpty()) {
            showAlert("Info", "Tidak ada item yang dibatalkan!");
            return;
        }

        Alert konfirmasi = new Alert(Alert.AlertType.CONFIRMATION);
        konfirmasi.setTitle("Konfirmasi Pembatalan");
        konfirmasi.setHeaderText("⚠️ Batalkan Transaksi?");
        konfirmasi.setContentText(
                "Transaksi akan disimpan dengan status BATAL PEMBAYARAN.\n" +
                        "Stok barang akan dikembalikan.\n\n" +
                        "Total item: " + detailList.size() + "\n" +
                        "Total harga: " + formatRupiah((long) totalHarga) + "\n\n" +
                        "Apakah Anda yakin?"
        );

        ButtonType btnYa = new ButtonType("Ya, Batalkan", ButtonBar.ButtonData.YES);
        ButtonType btnTidak = new ButtonType("Tidak", ButtonBar.ButtonData.NO);
        konfirmasi.getButtonTypes().setAll(btnYa, btnTidak);

        if (konfirmasi.showAndWait().orElse(btnTidak) == btnYa) {
            String metode = cbMetodePembayaran.getValue();
            if (metode == null) {
                metode = "Cash";
            }
            prosesBatalTransaksi(metode);
        }
    }

    // =========================================================
    // PROSES BATAL TRANSAKSI
    // =========================================================
    private void prosesBatalTransaksi(String metode) {
        StringBuilder detailString = new StringBuilder();
        for (DetailDataPenjualan data : detailList) {
            if (detailString.length() > 0) detailString.append("|");
            detailString.append(data.getIdProduk()).append(":")
                    .append(data.getJumlah()).append(":")
                    .append((long) data.getHarga());
        }

        try {
            String sql = "{call sp_TambahPenjualan(?, ?, ?, ?, ?, ?)}";
            try (CallableStatement cstmt = conn.prepareCall(sql)) {
                cstmt.setString(1, idPegawai);
                cstmt.setDate(2, Date.valueOf(dpTanggal.getValue()));
                cstmt.setString(3, metode);
                cstmt.setString(4, "Batal Pembayaran");
                cstmt.setDouble(5, 0);
                cstmt.setString(6, detailString.toString());

                cstmt.execute();

                showAlert("Info", "✅ Transaksi dibatalkan dengan status BATAL PEMBAYARAN");
                resetForm();
                updateStatisticsCards();
                generateIdPenjualan();

            }
        } catch (SQLException e) {
            e.printStackTrace();
            showAlert("Error", "Gagal membatalkan transaksi: " + e.getMessage());
        }
    }

    // =========================================================
    // PROSES SIMPAN TRANSAKSI
    // =========================================================
    private void prosesSimpanTransaksi(String status, String metode, double uangBayar) {
        strukIdPenjualan = txtIdPenjualan.getText();
        strukPegawai = txtIdPegawai.getText();
        strukTanggal = dpTanggal.getValue().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
        strukMetode = metode;
        strukTotalHarga = totalHarga;
        strukUangBayar = uangBayar;
        strukKembalian = Math.max(0, uangBayar - totalHarga);
        strukDetailList = FXCollections.observableArrayList(detailList);
        strukStatus = status;

        StringBuilder detailString = new StringBuilder();
        for (DetailDataPenjualan data : detailList) {
            if (detailString.length() > 0) detailString.append("|");
            detailString.append(data.getIdProduk()).append(":")
                    .append(data.getJumlah()).append(":")
                    .append((long) data.getHarga());
        }

        try {
            String sql = "{call sp_TambahPenjualan(?, ?, ?, ?, ?, ?)}";
            try (CallableStatement cstmt = conn.prepareCall(sql)) {
                cstmt.setString(1, idPegawai);
                cstmt.setDate(2, Date.valueOf(dpTanggal.getValue()));
                cstmt.setString(3, metode);
                cstmt.setString(4, status);
                cstmt.setDouble(5, uangBayar);
                cstmt.setString(6, detailString.toString());

                cstmt.execute();

                if ("Transfer".equals(metode)) {
                    showLoadingAndDetail();
                } else {
                    showDetailPenjualanFXML();
                }

                resetForm();
                updateStatisticsCards();
                generateIdPenjualan();

            }
        } catch (SQLException e) {
            e.printStackTrace();
            showAlert("Error", "Gagal menyimpan transaksi: " + e.getMessage());
        }
    }

    // =========================================================
    // TAMPILKAN LOADING
    // =========================================================
    private void showLoadingAndDetail() {
        Stage loadingStage = new Stage();
        loadingStage.initModality(Modality.APPLICATION_MODAL);
        loadingStage.setTitle("Menunggu Pembayaran");
        loadingStage.setResizable(false);

        VBox vbox = new VBox(20);
        vbox.setAlignment(Pos.CENTER);
        vbox.setPadding(new Insets(40));
        vbox.setStyle("-fx-background-color: white;");

        Label lblLoading = new Label("⏳ Menunggu Konfirmasi Pembayaran...");
        lblLoading.setFont(Font.font("Arial", FontWeight.BOLD, 16));

        ProgressIndicator progress = new ProgressIndicator();
        progress.setPrefSize(80, 80);

        vbox.getChildren().addAll(progress, lblLoading);

        Scene scene = new Scene(vbox, 400, 250);
        loadingStage.setScene(scene);
        loadingStage.show();

        PauseTransition pause = new PauseTransition(Duration.seconds(2));
        pause.setOnFinished(e -> {
            loadingStage.close();
            showDetailPenjualanFXML();
        });
        pause.play();
    }

    // =========================================================
    // TAMPILKAN DETAIL PENJUALAN FXML
    // =========================================================
    private void showDetailPenjualanFXML() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/LayoutSistemFotocopy/DetailPenjualan.fxml"));

            if (loader.getLocation() == null) {
                System.err.println("FXML tidak ditemukan di /LayoutSistemFotocopy/DetailPenjualan.fxml");
                showAlert("Error", "File DetailPenjualan.fxml tidak ditemukan!");
                return;
            }

            Parent root = loader.load();

            // PERBAIKAN PENTING: Ambil controller dari FXML yang baru saja di-load
            DetailPenjualan controller = loader.getController();

            // 1. Siapkan format String untuk Daftar Produk (agar muat di 1 label)
            StringBuilder produkBuilder = new StringBuilder();
            int totalQty = 0;
            int counter = 1;
            for (DetailDataPenjualan data : strukDetailList) {
                produkBuilder.append(counter).append(". ")
                        .append(data.getNamaBarang())
                        .append(" (")
                        .append(data.getJumlah())
                        .append(" x ")
                        .append(formatRupiah((long) data.getHarga()))
                        .append(")\n");
                totalQty += data.getJumlah();
                counter++;
            }

            // 2. Set data ke dalam controller
            controller.setData(
                    strukIdPenjualan,
                    strukTanggal,
                    strukPegawai,
                    produkBuilder.toString(), // Kirim daftar produk yang sudah diformat
                    String.valueOf(totalQty), // Total Kuantiti
                    strukMetode,
                    formatRupiah((long) strukTotalHarga),
                    formatRupiah((long) strukUangBayar),
                    formatRupiah((long) strukKembalian)
            );

            // 3. Tampilkan Stage
            Stage detailStage = new Stage();
            detailStage.setTitle("Detail Penjualan - " + strukIdPenjualan);
            Scene scene = new Scene(root);
            detailStage.setScene(scene);
            detailStage.setResizable(false);
            detailStage.initModality(Modality.APPLICATION_MODAL);
            detailStage.sizeToScene();
            detailStage.centerOnScreen();
            detailStage.showAndWait();

        } catch (IOException e) {
            e.printStackTrace();
            showAlert("Error", "Gagal menampilkan detail penjualan: " + e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Error", "Gagal membuka detail penjualan: " + e.getMessage());
        }
    }

    // =========================================================
    // HELPER
    // =========================================================
    private String getKategoriProduk(String idProduk) {
        String query = "SELECT Kategori_Produk FROM Produk WHERE ID_Produk = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setString(1, idProduk);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("Kategori_Produk");
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return "";
    }

    private void resetForm() {
        detailList.clear();
        tblPenjualan.setItems(detailList);
        totalHarga = 0;
        totalProdukTerjual = 0;
        updateTotalHarga();
        emptyState.setVisible(true);
        txtJumlah.clear();
        txtUangBayar.clear();
        txtKembalian.clear();
        cbNamaBarang.setValue(null);
        cbNamaLayanan.setValue(null);
        cbMetodePembayaran.setValue("Cash");
        txtUangBayar.setDisable(false);
        txtKembalian.setDisable(false);
        if (lblErrorUangBayar != null) {
            lblErrorUangBayar.setVisible(false);
        }
        generateIdPenjualan();
        loadProdukData();
        loadLayananData();
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}