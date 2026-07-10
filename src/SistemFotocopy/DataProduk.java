package SistemFotocopy;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.beans.property.SimpleStringProperty;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class DataProduk {

    @FXML private Button btnBatal;
    @FXML private Button btnHapus;
    @FXML private Button btnNextPage;
    @FXML private Button btnPage1;
    @FXML private Button btnPrevPage;
    @FXML private Button btnSimpan;
    @FXML private Button btnUbah;
    @FXML private ComboBox<String> cmbKategoriProduk;
    @FXML private ComboBox<String> cmbStatus;

    @FXML private TableView<Produk> tblProduk;
    @FXML private TableColumn<Produk, String> colIdBarang;
    @FXML private TableColumn<Produk, String> colNamaBarang;
    @FXML private TableColumn<Produk, String> colMerkProduk;
    @FXML private TableColumn<Produk, String> colKategoriProduk;
    @FXML private TableColumn<Produk, String> colHarga;  // Diubah ke String untuk format "RP "
    @FXML private TableColumn<Produk, String> colStock;  // Diubah ke String untuk format "-"

    @FXML private Label lblInfoData;
    @FXML private Label lblProdukTersedia;
    @FXML private Label lblProdukTidakTersedia;
    @FXML private Label lblTotalProduk;
    @FXML private BorderPane rootPane;

    @FXML private TextField txtCari;
    @FXML private TextField txtHargaBarang;
    @FXML private TextField txtIdBarang;
    @FXML private TextField txtMerk;
    @FXML private TextField txtNamaBarang;
    @FXML private TextField txtStockBarang;

    private ObservableList<Produk> listProduk = FXCollections.observableArrayList();
    private ObservableList<Produk> filteredProduk = FXCollections.observableArrayList();
    private Connection conn;

    // Variabel Pagination (Maks 10 data per tabel)
    private int currentPage = 1;
    private final int limitPerPage = 10;

    private void koneksi() {
        try {
            String url = "jdbc:sqlserver://kelompok-5.database.windows.net:1433;database=FotoCopyATK;user=hilmi;password=Kelompok5;trustServerCertificate=true;";
            conn = DriverManager.getConnection(url);
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Error Koneksi", "Gagal terhubung ke database: " + e.getMessage());
        }
    }

    @FXML
    public void initialize() {
        koneksi();

        txtIdBarang.setDisable(true);
        cmbStatus.setDisable(true);

        // Kondisi Awal: Tombol Simpan aktif, sedangkan Ubah & Hapus mati (Kondisi Tambah Data Baru)
        btnSimpan.setDisable(false);
        btnUbah.setDisable(true);
        btnHapus.setDisable(true);

        // Mapping Kolom Tabel
        colIdBarang.setCellValueFactory(cellData -> cellData.getValue().idProdukProperty());
        colNamaBarang.setCellValueFactory(cellData -> cellData.getValue().namaProdukProperty());
        colMerkProduk.setCellValueFactory(cellData -> cellData.getValue().merkProperty());
        colKategoriProduk.setCellValueFactory(cellData -> cellData.getValue().kategoriProperty());

        // Kustomisasi Tampilan Harga (Otomatis "RP " di depan angka)
        colHarga.setCellValueFactory(cellData -> {
            double harga = cellData.getValue().getHarga();
            return new SimpleStringProperty(formatRupiah(harga));
        });

        // Kustomisasi Tampilan Stok (Jika bernilai <= 0 atau kategori Layanan, tampilkan "-")
        colStock.setCellValueFactory(cellData -> {
            if ("Layanan".equalsIgnoreCase(cellData.getValue().getKategori())) {
                return new SimpleStringProperty("-");
            }
            int stok = cellData.getValue().getStok();
            return new SimpleStringProperty(stok <= 0 ? "-" : String.valueOf(stok));
        });

        cmbKategoriProduk.setItems(FXCollections.observableArrayList("Barang", "Layanan"));
        cmbStatus.setItems(FXCollections.observableArrayList("Tersedia", "NonTersedia"));

        // Format Otomatis Inputan Harga saat mengetik (Menambahkan "RP " di textfield)
        txtHargaBarang.textProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue == null || newValue.isEmpty()) return;

            String cleanString = newValue.replaceAll("[^\\d]", "");
            if (!cleanString.isEmpty()) {
                double parsed = Double.parseDouble(cleanString);
                String formatted = formatRupiah(parsed);

                // Mencegah infinite loop listener
                javafx.application.Platform.runLater(() -> {
                    txtHargaBarang.setText(formatted);
                    txtHargaBarang.end();
                });
            }
        });

        // Listener Pilihan Kategori
        cmbKategoriProduk.getSelectionModel().selectedItemProperty().addListener((options, oldValue, newValue) -> {
            if (newValue != null && tblProduk.getSelectionModel().getSelectedItem() == null) {
                generateIdOtomatis();
                cmbStatus.setValue("Tersedia");
                cmbStatus.setDisable(true);

                if ("Layanan".equalsIgnoreCase(newValue)) {
                    txtStockBarang.setText("-");
                    txtMerk.setText("-");
                    txtStockBarang.setDisable(true);
                    txtMerk.setDisable(true);
                } else {
                    txtStockBarang.clear();
                    txtMerk.clear();
                    txtStockBarang.setDisable(false);
                    txtMerk.setDisable(false);
                }
            }
        });

        loadDataProduk();

        // Listener Klik Baris Tabel (Mode Update / Hapus)
        tblProduk.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            if (newSelection != null) {
                // Ketika baris diklik: Tombol Simpan dinonaktifkan, Ubah & Hapus diaktifkan
                btnSimpan.setDisable(true);
                btnUbah.setDisable(false);
                btnHapus.setDisable(false);

                txtIdBarang.setText(newSelection.getIdProduk());
                txtNamaBarang.setText(newSelection.getNamaProduk());
                txtMerk.setText(newSelection.getMerk());
                cmbKategoriProduk.setValue(newSelection.getKategori());
                txtHargaBarang.setText(formatRupiah(newSelection.getHarga()));

                if ("Layanan".equalsIgnoreCase(newSelection.getKategori())) {
                    txtStockBarang.setText("-");
                    txtStockBarang.setDisable(true);
                    txtMerk.setDisable(true);
                } else {
                    txtStockBarang.setText(String.valueOf(newSelection.getStok()));
                    txtStockBarang.setDisable(false);
                    txtMerk.setDisable(false);
                }

                cmbStatus.setValue(newSelection.getStatus());
                cmbStatus.setDisable(false);
            }
        });

        txtCari.textProperty().addListener((observable, oldValue, newValue) -> {
            cariDataProduk(newValue);
        });
    }

    private String formatRupiah(double nilai) {
        return String.format("RP %,.0f", nilai).replace(',', '.');
    }

    private double dapatkanAngkaMurni(String teksRupiah) {
        if (teksRupiah == null || teksRupiah.isEmpty()) return 0;
        String clean = teksRupiah.replaceAll("[^\\d]", "");
        return clean.isEmpty() ? 0 : Double.parseDouble(clean);
    }

    private void generateIdOtomatis() {
        String query = "SELECT MAX(CAST(SUBSTRING(ID_Produk, 4, LEN(ID_Produk)) AS INT)) AS max_angka FROM Produk";
        try {
            if (conn == null || conn.isClosed()) koneksi();
            try (PreparedStatement ps = conn.prepareStatement(query);
                 ResultSet rs = ps.executeQuery()) {

                if (rs.next() && rs.getObject("max_angka") != null) {
                    int nextNumber = rs.getInt("max_angka") + 1;
                    txtIdBarang.setText(String.format("PDK%03d", nextNumber));
                } else {
                    txtIdBarang.setText("PDK001");
                }
            }
        } catch (Exception e) {
            System.out.println("Gagal generate ID otomatis: " + e.getMessage());
        }
    }

    private void loadDataProduk() {
        listProduk.clear();
        String query = "SELECT * FROM v_TampilSemuaProduk " +
                "ORDER BY CASE WHEN Status_Barang = 'NonTersedia' THEN 2 ELSE 1 END ASC, ID_Produk ASC";

        try {
            if (conn == null || conn.isClosed()) koneksi();

            try (PreparedStatement ps = conn.prepareStatement(query);
                 ResultSet rs = ps.executeQuery()) {

                int total = 0, tersedia = 0, tidakTersedia = 0;

                while (rs.next()) {
                    String id = rs.getString("ID_Produk");
                    String nama = rs.getString("Nama_Barang");
                    String kategori = rs.getString("Kategori_Produk");
                    double harga = rs.getDouble("Harga");
                    int stok = 0;

                    try {
                        stok = rs.getInt("Stok");
                    } catch (Exception ignored) {}

                    String merk = rs.getString("Merk_Barang");
                    String status = rs.getString("Status_Barang");

                    listProduk.add(new Produk(id, nama, merk, kategori, harga, stok, status));

                    total++;
                    if ("Tersedia".equalsIgnoreCase(status)) tersedia++;
                    else tidakTersedia++;
                }

                filteredProduk.setAll(listProduk);
                updateTableContent();

                lblTotalProduk.setText(String.valueOf(total));
                lblProdukTersedia.setText(String.valueOf(tersedia));
                lblProdukTidakTersedia.setText(String.valueOf(tidakTersedia));
            }
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Error Database", "Gagal memuat data: " + e.getMessage());
        }
    }

    // Fungsi Utama Logika Pagination (Maksimal 10 Data per Tabel)
    private void updateTableContent() {
        int totalData = filteredProduk.size();

        if (totalData == 0) {
            tblProduk.setItems(FXCollections.observableArrayList());
            lblInfoData.setText("Menampilkan 0 dari 0 data");
            btnPrevPage.setDisable(true);
            btnNextPage.setDisable(true);
            btnPage1.setText("1");
            return;
        }

        // Hitung total halaman yang tersedia
        int totalPages = (int) Math.ceil((double) totalData / limitPerPage);
        if (currentPage > totalPages) currentPage = totalPages;
        if (currentPage < 1) currentPage = 1;

        // Hitung indeks subList data
        int fromIndex = (currentPage - 1) * limitPerPage;
        int toIndex = Math.min(fromIndex + limitPerPage, totalData);

        // Ambil data untuk halaman aktif saat ini
        ObservableList<Produk> pageItems = FXCollections.observableArrayList(filteredProduk.subList(fromIndex, toIndex));
        tblProduk.setItems(pageItems);

        // --- PERUBAHAN FORMAT TEKS DI SINI ---
        // Menghitung berapa banyak data yang tampil di halaman aktif tersebut
        int dataTampilDiHalamanIni = toIndex - fromIndex;
        lblInfoData.setText("Menampilkan " + dataTampilDiHalamanIni + " dari " + totalData + " data");

        // Update nomor tombol halaman tengah
        btnPage1.setText(String.valueOf(currentPage));

        // Atur kondisi aktif/mati tombol panah < dan >
        btnPrevPage.setDisable(currentPage == 1);
        btnNextPage.setDisable(currentPage == totalPages || totalPages == 0);
    }

    @FXML
    void handlePrevPage(ActionEvent event) {
        if (currentPage > 1) {
            currentPage--;
            updateTableContent();
        }
    }

    @FXML
    void handleNextPage(ActionEvent event) {
        int totalPages = (int) Math.ceil((double) filteredProduk.size() / limitPerPage);
        if (currentPage < totalPages) {
            currentPage++;
            updateTableContent();
        }
    }

    @FXML
    void handleSimpanData(ActionEvent event) {
        String sqlProcedure = "{CALL sp_TambahProduk(?, ?, ?, ?, ?)}";
        try {
            if (conn == null || conn.isClosed()) koneksi();

            try (CallableStatement cs = conn.prepareCall(sqlProcedure)) {
                cs.setString(1, txtNamaBarang.getText());
                cs.setString(2, cmbKategoriProduk.getValue());
                cs.setDouble(3, dapatkanAngkaMurni(txtHargaBarang.getText()));

                String stok = txtStockBarang.getText();
                cs.setString(4, "-".equals(stok) ? "0" : stok);
                cs.setString(5, txtMerk.getText());

                cs.execute();
                showAlert(Alert.AlertType.INFORMATION, "Sukses", "Produk baru berhasil ditambahkan!");
                loadDataProduk();
                handleBatal(null);
            }
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Error Tambah", "Gagal menyimpan data: " + e.getMessage());
        }
    }

    @FXML
    void handleUbahData(ActionEvent event) {
        Produk produkTerpilih = tblProduk.getSelectionModel().getSelectedItem();
        if (produkTerpilih == null) {
            showAlert(Alert.AlertType.WARNING, "Peringatan", "Pilih data produk di tabel yang ingin diubah!");
            return;
        }

        String statusLama = produkTerpilih.getStatus();
        String statusBaru = cmbStatus.getValue();

        if ("Tersedia".equalsIgnoreCase(statusLama) && "NonTersedia".equalsIgnoreCase(statusBaru)) {
            showAlert(Alert.AlertType.WARNING, "Peringatan",
                    "Untuk mengubah status menjadi 'NonTersedia', silahkan gunakan tombol 'Hapus Data'!");
            cmbStatus.setValue(statusLama);
            return;
        }

        String sqlProcedure = "{CALL sp_UpdateProduk(?, ?, ?, ?, ?, ?)}";
        try {
            if (conn == null || conn.isClosed()) koneksi();

            try (CallableStatement cs = conn.prepareCall(sqlProcedure)) {
                cs.setString(1, txtIdBarang.getText());
                cs.setString(2, txtNamaBarang.getText());
                cs.setDouble(3, dapatkanAngkaMurni(txtHargaBarang.getText()));

                String stok = txtStockBarang.getText();
                cs.setString(4, "-".equals(stok) ? "0" : stok);
                cs.setString(5, txtMerk.getText());
                cs.setString(6, statusBaru);

                cs.execute();
                showAlert(Alert.AlertType.INFORMATION, "Sukses", "Data produk berhasil diperbarui!");
                loadDataProduk();
                handleBatal(null);
            }
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Error Update", "Gagal mengubah data: " + e.getMessage());
        }
    }

    @FXML
    void handleHapusData(ActionEvent event) {
        Produk produkTerpilih = tblProduk.getSelectionModel().getSelectedItem();
        if (produkTerpilih == null) {
            showAlert(Alert.AlertType.WARNING, "Peringatan", "Pilih data produk di tabel yang ingin dinonaktifkan!");
            return;
        }

        String sqlProcedure = "{CALL sp_DeleteProdukSoft(?)}";
        try {
            if (conn == null || conn.isClosed()) koneksi();

            try (CallableStatement cs = conn.prepareCall(sqlProcedure)) {
                cs.setString(1, produkTerpilih.getIdProduk());
                cs.execute();

                showAlert(Alert.AlertType.INFORMATION, "Sukses", "Produk berhasil dinonaktifkan!");
                loadDataProduk();
                handleBatal(null);
            }
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Error Delete", "Gagal menonaktifkan produk: " + e.getMessage());
        }
    }

    @FXML
    void handleBatal(ActionEvent event) {
        tblProduk.getSelectionModel().clearSelection();
        txtIdBarang.clear();
        txtNamaBarang.clear();
        txtMerk.clear();
        cmbKategoriProduk.setValue(null);
        txtHargaBarang.clear();
        txtStockBarang.clear();
        cmbStatus.setValue(null);

        txtStockBarang.setDisable(false);
        txtMerk.setDisable(false);
        cmbStatus.setDisable(true);

        // Mengembalikan kondisi tombol ke mode Tambah Baru
        btnSimpan.setDisable(false);
        btnUbah.setDisable(true);
        btnHapus.setDisable(true);

        currentPage = 1;
        updateTableContent();
    }

    private void cariDataProduk(String keyword) {
        if (keyword == null || keyword.isEmpty()) {
            filteredProduk.setAll(listProduk);
        } else {
            ObservableList<Produk> temp = FXCollections.observableArrayList();
            for (Produk p : listProduk) {
                if (p.getNamaProduk().toLowerCase().contains(keyword.toLowerCase()) ||
                        p.getIdProduk().toLowerCase().contains(keyword.toLowerCase()) ||
                        p.getMerk().toLowerCase().contains(keyword.toLowerCase())) {
                    temp.add(p);
                }
            }
            filteredProduk.setAll(temp);
        }
        currentPage = 1; // Reset ke halaman pertama saat melakukan pencarian
        updateTableContent();
    }

    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}