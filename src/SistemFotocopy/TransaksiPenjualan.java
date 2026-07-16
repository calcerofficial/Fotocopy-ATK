package SistemFotocopy;

import Database.DBConnection;
import javafx.animation.PauseTransition;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Duration;

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
    private TableColumn<DetailData, String> colHarga, colIdPenjualan, colJumlah, colNamaBarang;

    @FXML
    private DatePicker dpTanggal;

    @FXML
    private VBox emptyState;

    @FXML
    private Label lblProdukTerjual, lblTotalHarga, lblTotalPenjualan, lblTotalTransaksi;

    @FXML
    private TableView<DetailData> tblPenjualan;

    @FXML
    private TextField txtIdPegawai, txtIdPenjualan, txtJumlah, txtKembalian, txtUangBayar;

    @FXML
    private Label lblErrorUangBayar;

    private Connection conn;
    private ObservableList<DetailData> detailList = FXCollections.observableArrayList();
    private double totalHarga = 0;
    private int totalProdukTerjual = 0;
    private boolean isUpdating = false;

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
        setupTableColumns();
        setupMetodePembayaranListener();
        setupValidations();

        txtKembalian.setEditable(false);
        txtKembalian.setStyle("-fx-background-color: #f0f0f0;");

        btnTambah.setOnAction(e -> tambahItem());
        btnSimpan.setOnAction(e -> simpanTransaksi());
        btnBatal.setOnAction(e -> resetForm());

        updateTotalHarga();
        updateStatisticsCards();

        if (lblErrorUangBayar != null) {
            lblErrorUangBayar.setVisible(false);
        }
    }

    private void setPegawaiFromSession() {
        UserSession session = UserSession.getInstance();
        String idPegawai = session.getIdPegawai();
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

    private void loadProdukData() {
        ObservableList<String> produkList = FXCollections.observableArrayList();
        String query = "SELECT ID_Produk, Nama_Barang FROM Produk WHERE Status_Barang = 'tersedia' AND Stok > 0 ORDER BY Nama_Barang";
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

    private void setupTableColumns() {
        colIdPenjualan.setCellValueFactory(new PropertyValueFactory<>("idPenjualan"));
        colNamaBarang.setCellValueFactory(new PropertyValueFactory<>("namaBarang"));
        colJumlah.setCellValueFactory(new PropertyValueFactory<>("jumlah"));
        colHarga.setCellValueFactory(new PropertyValueFactory<>("hargaFormatted"));

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

    private void tambahItem() {
        if (cbNamaBarang.getValue() == null) {
            showAlert("Error", "Silakan pilih Nama Barang!");
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

        String selected = cbNamaBarang.getValue();
        String idProduk = selected.split(" - ")[0];
        String namaProduk = selected.split(" - ")[1];

        double harga = getHargaProduk(idProduk);
        if (harga <= 0) {
            showAlert("Error", "Produk tidak valid!");
            return;
        }

        int stok = getStokProduk(idProduk);
        if (stok < jumlah) {
            showAlert("Error", "Stok tidak mencukupi! Tersedia: " + stok);
            return;
        }

        DetailData data = new DetailData(txtIdPenjualan.getText(), idProduk, namaProduk, jumlah, harga);
        detailList.add(data);
        tblPenjualan.setItems(detailList);

        emptyState.setVisible(false);

        totalHarga += jumlah * harga;
        totalProdukTerjual += jumlah;
        updateTotalHarga();

        cbNamaBarang.setValue(null);
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

    private void updateStatisticsCards() {
        try {
            String queryTotalPenjualan = "SELECT ISNULL(SUM(Total_Harga), 0) AS Total FROM Penjualan WHERE Status_Penjualan = 'Lunas' AND CAST(Tanggal_Penjualan AS DATE) = CAST(GETDATE() AS DATE)";
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(queryTotalPenjualan)) {
                if (rs.next()) {
                    lblTotalPenjualan.setText("Rp " + String.format("%,d", (long) rs.getDouble("Total")).replace(',', '.'));
                }
            }

            String queryTotalTransaksi = "SELECT COUNT(*) AS Total FROM Penjualan WHERE Status_Penjualan = 'Lunas' AND CAST(Tanggal_Penjualan AS DATE) = CAST(GETDATE() AS DATE)";
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(queryTotalTransaksi)) {
                if (rs.next()) {
                    lblTotalTransaksi.setText(String.valueOf(rs.getInt("Total")));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // ==========================================
    // SIMPAN TRANSAKSI - FINAL
    // ==========================================
    private void simpanTransaksi() {
        if (detailList.isEmpty()) {
            showAlert("Error", "Tidak ada item yang dijual!");
            return;
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

        StringBuilder detailString = new StringBuilder();
        for (DetailData data : detailList) {
            if (detailString.length() > 0) detailString.append("|");
            detailString.append(data.getIdProduk()).append(":")
                    .append(data.getJumlah()).append(":")
                    .append((long) data.getHarga());
        }

        String status = "Lunas";

        try {
            String sql = "{call sp_TambahPenjualan(?, ?, ?, ?, ?, ?)}";
            try (CallableStatement cstmt = conn.prepareCall(sql)) {
                cstmt.setString(1, txtIdPegawai.getText());
                cstmt.setDate(2, Date.valueOf(dpTanggal.getValue()));
                cstmt.setString(3, metode);
                cstmt.setString(4, status);
                cstmt.setDouble(5, uangBayar);
                cstmt.setString(6, detailString.toString());

                // Eksekusi SP
                boolean hasResult = cstmt.execute();

                // ==========================================
                // TAMPILKAN SESUAI METODE (SETELAH SP BERHASIL)
                // ==========================================
                if ("Transfer".equals(metode)) {
                    showLoadingAndSuccess();
                } else {
                    showStruk();
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

    // ==========================================
    // SHOW LOADING AND SUCCESS - FINAL
    // ==========================================
    private void showLoadingAndSuccess() {
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

        // Tunggu 3 detik lalu tutup loading dan tampilkan struk
        PauseTransition pause = new PauseTransition(Duration.seconds(3));
        pause.setOnFinished(e -> {
            loadingStage.close();

            // Tampilkan alert sukses
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Sukses");
            alert.setHeaderText(null);
            alert.setContentText("✅ Pembayaran Berhasil!");
            alert.showAndWait();

            // Tampilkan struk
            showStruk();
        });
        pause.play();
    }

    // ==========================================
    // SHOW STRUK - FINAL
    // ==========================================
    private void showStruk() {
        Stage strukStage = new Stage();
        strukStage.initModality(Modality.APPLICATION_MODAL);
        strukStage.setTitle("Struk Penjualan");
        strukStage.setResizable(false);

        VBox vbox = new VBox(10);
        vbox.setAlignment(Pos.CENTER);
        vbox.setPadding(new Insets(30));
        vbox.setStyle("-fx-background-color: white;");

        Label title = new Label("=== COPYSTREAM PRO ===");
        title.setFont(Font.font("Arial", FontWeight.BOLD, 18));

        Label subTitle = new Label("Struk Penjualan");
        subTitle.setFont(Font.font("Arial", 14));

        Separator separator = new Separator();

        VBox detailBox = new VBox(5);
        detailBox.setAlignment(Pos.CENTER_LEFT);
        detailBox.setPadding(new Insets(10, 0, 10, 0));

        Label lblId = new Label("ID Nota   : " + txtIdPenjualan.getText());
        Label lblTanggal = new Label("Tanggal   : " + dpTanggal.getValue().format(FORMATTER));
        Label lblPegawai = new Label("Kasir     : " + txtIdPegawai.getText());
        Label lblMetode = new Label("Metode    : " + cbMetodePembayaran.getValue());

        detailBox.getChildren().addAll(lblId, lblTanggal, lblPegawai, lblMetode);

        Separator sep2 = new Separator();

        VBox itemBox = new VBox(3);
        itemBox.setAlignment(Pos.CENTER_LEFT);

        for (DetailData data : detailList) {
            Label item = new Label(data.getJumlah() + "x " + data.getNamaBarang() + " @ " + formatRupiah((long) data.getHarga()));
            itemBox.getChildren().add(item);
        }

        Separator sep3 = new Separator();

        Label total = new Label("Total: " + formatRupiah((long) totalHarga));
        total.setFont(Font.font("Arial", FontWeight.BOLD, 16));

        Button btnTutup = new Button("Tutup");
        btnTutup.setStyle("-fx-background-color: #00357F; -fx-text-fill: white; -fx-padding: 10 30; -fx-cursor: hand;");
        btnTutup.setOnAction(e -> strukStage.close());

        vbox.getChildren().addAll(title, subTitle, separator, detailBox, sep2, itemBox, sep3, total, btnTutup);

        Scene scene = new Scene(vbox, 400, 550);
        strukStage.setScene(scene);
        strukStage.show();
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
        cbMetodePembayaran.setValue("Cash");
        txtUangBayar.setDisable(false);
        txtKembalian.setDisable(false);
        if (lblErrorUangBayar != null) {
            lblErrorUangBayar.setVisible(false);
        }
        generateIdPenjualan();
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    // ==========================================
    // HELPER CLASS DETAIL DATA
    // ==========================================
    public static class DetailData {
        private String idPenjualan, idProduk, namaBarang;
        private int jumlah;
        private double harga;

        public DetailData(String idPenjualan, String idProduk, String namaBarang, int jumlah, double harga) {
            this.idPenjualan = idPenjualan;
            this.idProduk = idProduk;
            this.namaBarang = namaBarang;
            this.jumlah = jumlah;
            this.harga = harga;
        }

        public String getIdPenjualan() { return idPenjualan; }
        public String getIdProduk() { return idProduk; }
        public String getNamaBarang() { return namaBarang; }
        public int getJumlah() { return jumlah; }
        public double getHarga() { return harga; }

        public String getHargaFormatted() {
            return "Rp " + String.format("%,d", (long) harga).replace(',', '.');
        }
    }
}