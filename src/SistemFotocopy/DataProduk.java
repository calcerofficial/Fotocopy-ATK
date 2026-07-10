package SistemFotocopy;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.text.NumberFormat;
import java.util.Locale;

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
    @FXML private TableColumn<Produk, String> colHarga;
    @FXML private TableColumn<Produk, String> colStock;
    @FXML private TableColumn<Produk, String> colStatusProduk; // Pastikan ini terhubung ke FXML

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
    private ObservableList<Produk> filteredList = FXCollections.observableArrayList();
    private Connection conn;

    // Variabel Pagination
    private int currentPage = 1;
    private final int rowsPerPage = 10;

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

        // Kondisi Awal Tombol Form
        btnSimpan.setDisable(false);
        btnUbah.setDisable(true);
        btnHapus.setDisable(true);

        txtIdBarang.setDisable(true);
        cmbStatus.setDisable(true);

        // --- BINDING KOLOM TABEL ---
        colIdBarang.setCellValueFactory(cellData -> cellData.getValue().idProdukProperty());
        colNamaBarang.setCellValueFactory(cellData -> cellData.getValue().namaProdukProperty());
        colMerkProduk.setCellValueFactory(cellData -> cellData.getValue().merkProperty());
        colKategoriProduk.setCellValueFactory(cellData -> cellData.getValue().kategoriProperty());

        // Kembalikan mapping kolom status asli bawaan properti model Produk agar nilainya langsung keluar
        colStatusProduk.setCellValueFactory(cellData -> cellData.getValue().statusProperty());

        // Kustomisasi Kolom Harga (Format Rp.)
        colHarga.setCellValueFactory(cellData -> {
            double harga = cellData.getValue().getHarga();
            return new javafx.beans.property.SimpleStringProperty(formatRupiah(harga));
        });

        // Kustomisasi Kolom Stok (Jika layanan otomatis jadi '-')
        colStock.setCellValueFactory(cellData -> {
            String kat = cellData.getValue().getKategori();
            if ("layanan".equalsIgnoreCase(kat)) {
                return new javafx.beans.property.SimpleStringProperty("-");
            } else {
                return new javafx.beans.property.SimpleStringProperty(String.valueOf(cellData.getValue().getStok()));
            }
        });

        // --- SET ITEMS DROPDOWN ---
        cmbKategoriProduk.setItems(FXCollections.observableArrayList("Barang", "Layanan"));

        // PERBAIKAN UTAMA: Huruf T diganti kapital ("Tersedia") agar singkron dengan database!
        cmbStatus.setItems(FXCollections.observableArrayList("Tersedia", "NonTersedia"));

        // --- FORMAT RUPIAH INPUT HARGA ---
        txtHargaBarang.textProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue == null || newValue.isEmpty()) return;

            String cleanString = newValue.replaceAll("[^0-9]", "");
            if (cleanString.isEmpty()) {
                txtHargaBarang.setText("");
                return;
            }

            try {
                double parsed = Double.parseDouble(cleanString);
                String formatted = formatRupiah(parsed);

                javafx.application.Platform.runLater(() -> {
                    txtHargaBarang.setText(formatted);
                    txtHargaBarang.positionCaret(formatted.length());
                });
            } catch (Exception ignored) {}
        });

        // --- LISTENER COMBOBOX KATEGORI ---
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

        // --- LISTENER KLIK TABEL (EDIT MODE) ---
        tblProduk.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            if (newSelection != null) {
                btnSimpan.setDisable(true);
                btnUbah.setDisable(false);
                btnHapus.setDisable(false);

                txtIdBarang.setText(newSelection.getIdProduk());
                txtNamaBarang.setText(newSelection.getNamaProduk());
                txtMerk.setText(newSelection.getMerk());
                cmbKategoriProduk.setValue(newSelection.getKategori());
                txtHargaBarang.setText(formatRupiah(newSelection.getHarga()));

                cmbStatus.setValue(newSelection.getStatus());
                cmbStatus.setDisable(false);

                if ("layanan".equalsIgnoreCase(newSelection.getKategori())) {
                    txtStockBarang.setText("-");
                    txtStockBarang.setDisable(true);
                    txtMerk.setDisable(true);
                } else {
                    txtStockBarang.setText(String.valueOf(newSelection.getStok()));
                    txtStockBarang.setDisable(false);
                    txtMerk.setDisable(false);
                }
            }
        });

        txtCari.textProperty().addListener((observable, oldValue, newValue) -> {
            cariDataProduk(newValue);
        });
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
                    if ("Tersedia".equalsIgnoreCase(status) || "tersedia".equalsIgnoreCase(status)) tersedia++;
                    else tidakTersedia++;
                }

                filteredList.setAll(listProduk);
                currentPage = 1;
                updateTableAndPagination();

                lblTotalProduk.setText(String.valueOf(total));
                lblProdukTersedia.setText(String.valueOf(tersedia));
                lblProdukTidakTersedia.setText(String.valueOf(tidakTersedia));
            }
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Error Database", "Gagal memuat data: " + e.getMessage());
        }
    }

    private void updateTableAndPagination() {
        int totalRows = filteredList.size();
        int maxPage = (int) Math.ceil((double) totalRows / rowsPerPage);
        if (maxPage == 0) maxPage = 1;

        if (currentPage > maxPage) currentPage = maxPage;
        if (currentPage < 1) currentPage = 1;

        int fromIndex = (currentPage - 1) * rowsPerPage;
        int toIndex = Math.min(fromIndex + rowsPerPage, totalRows);

        ObservableList<Produk> pageItems = FXCollections.observableArrayList();
        if (totalRows > 0) {
            pageItems.setAll(filteredList.subList(fromIndex, toIndex));
        }
        tblProduk.setItems(pageItems);

        if (totalRows == 0) {
            lblInfoData.setText("Menampilkan 0 dari 0 data");
        } else {
            lblInfoData.setText("Menampilkan " + (fromIndex + 1) + "-" + toIndex + " dari " + totalRows + " data");
        }

        btnPage1.setText(String.valueOf(currentPage));
        btnPrevPage.setDisable(currentPage == 1);
        btnNextPage.setDisable(currentPage == maxPage || totalRows == 0);
    }

    @FXML
    void handleNextPage(ActionEvent event) {
        currentPage++;
        updateTableAndPagination();
    }

    @FXML
    void handlePrevPage(ActionEvent event) {
        currentPage--;
        updateTableAndPagination();
    }

    @FXML
    void handleSimpanData(ActionEvent event) {
        String sqlProcedure = "{CALL sp_TambahProduk(?, ?, ?, ?, ?)}";
        try {
            if (conn == null || conn.isClosed()) koneksi();

            try (CallableStatement cs = conn.prepareCall(sqlProcedure)) {
                cs.setString(1, txtNamaBarang.getText());
                cs.setString(2, cmbKategoriProduk.getValue());
                cs.setDouble(3, hilangkanFormatRupiah(txtHargaBarang.getText()));
                cs.setString(4, txtStockBarang.getText());
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

        if (("Tersedia".equalsIgnoreCase(statusLama) || "tersedia".equalsIgnoreCase(statusLama))
                && "NonTersedia".equalsIgnoreCase(statusBaru)) {
            showAlert(Alert.AlertType.WARNING, "Peringatan", "Untuk menonaktifkan produk, silakan pakai tombol 'Hapus Data'!");
            cmbStatus.setValue(statusLama);
            return;
        }

        String sqlProcedure = "{CALL sp_UpdateProduk(?, ?, ?, ?, ?, ?)}";
        try {
            if (conn == null || conn.isClosed()) koneksi();

            try (CallableStatement cs = conn.prepareCall(sqlProcedure)) {
                cs.setString(1, txtIdBarang.getText());
                cs.setString(2, txtNamaBarang.getText());
                cs.setDouble(3, hilangkanFormatRupiah(txtHargaBarang.getText()));
                cs.setString(4, txtStockBarang.getText());
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
            showAlert(Alert.AlertType.WARNING, "Peringatan", "Pilih data produk di tabel!");
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

        btnSimpan.setDisable(false);
        btnUbah.setDisable(true);
        btnHapus.setDisable(true);
    }

    private void cariDataProduk(String keyword) {
        if (keyword == null || keyword.isEmpty()) {
            filteredList.setAll(listProduk);
        } else {
            filteredList.clear();
            for (Produk p : listProduk) {
                if (p.getNamaProduk().toLowerCase().contains(keyword.toLowerCase()) ||
                        p.getIdProduk().toLowerCase().contains(keyword.toLowerCase()) ||
                        p.getMerk().toLowerCase().contains(keyword.toLowerCase())) {
                    filteredList.add(p);
                }
            }
        }
        currentPage = 1;
        updateTableAndPagination();
    }

    private String formatRupiah(double nilai) {
        NumberFormat nf = NumberFormat.getCurrencyInstance(new Locale("id", "ID"));
        return nf.format(nilai).replaceAll(",00", "");
    }

    private double hilangkanFormatRupiah(String textRupiah) {
        if (textRupiah == null || textRupiah.isEmpty()) return 0;
        String clean = textRupiah.replaceAll("[^0-9]", "");
        return clean.isEmpty() ? 0 : Double.parseDouble(clean);
    }

    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}