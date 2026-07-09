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

public class DataProduk {

    @FXML private Button btnBatal;
    @FXML private Button btnHapus;
    @FXML private Button btnNextPage;
    @FXML private Button btnPage1;
    @FXML private Button btnPrevPage;
    @FXML private Button btnSimpan;
    @FXML private Button btnUbah;
    @FXML private ComboBox<String> cmbKategoriProduk;

    // SEKARANG STATUS MENGGUNAKAN COMBOBOX BIAR BISA DIPILIH & DI-UPDATE LANGSUNG
    @FXML private ComboBox<String> cmbStatus;

    @FXML private TableView<Produk> tblProduk;
    @FXML private TableColumn<Produk, String> colIdBarang;
    @FXML private TableColumn<Produk, String> colNamaBarang;
    @FXML private TableColumn<Produk, String> colMerkProduk;
    @FXML private TableColumn<Produk, String> colKategoriProduk;
    @FXML private TableColumn<Produk, Double> colHarga;
    @FXML private TableColumn<Produk, Integer> colStock;
    @FXML private TableColumn<Produk, String> colStatusProduk;

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
    private Connection conn;

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

        // ID Barang dikunci mati sejak awal karena di-generate otomatis oleh database
        txtIdBarang.setDisable(true);

        // ComboBox Status dikunci default (hanya menyala saat mengedit data dari tabel)
        cmbStatus.setDisable(true);

        colIdBarang.setCellValueFactory(cellData -> cellData.getValue().idProdukProperty());
        colNamaBarang.setCellValueFactory(cellData -> cellData.getValue().namaProdukProperty());
        colMerkProduk.setCellValueFactory(cellData -> cellData.getValue().merkProperty());
        colKategoriProduk.setCellValueFactory(cellData -> cellData.getValue().kategoriProperty());
        colHarga.setCellValueFactory(cellData -> cellData.getValue().hargaProperty().asObject());
        colStock.setCellValueFactory(cellData -> cellData.getValue().stokProperty().asObject());
        colStatusProduk.setCellValueFactory(cellData -> cellData.getValue().statusProperty());

        // Set isi pilihan dropdown kategori & status sesuai aturan CHECK CONSTRAINT database kamu
        cmbKategoriProduk.setItems(FXCollections.observableArrayList("Barang", "Layanan"));
        cmbStatus.setItems(FXCollections.observableArrayList("Tersedia", "NonTersedia"));

        // --- LISTENER COMBOBOX KATEGORI (TAMBAH BARU) ---
        cmbKategoriProduk.getSelectionModel().selectedItemProperty().addListener((options, oldValue, newValue) -> {
            if (newValue != null && tblProduk.getSelectionModel().getSelectedItem() == null) {
                generateIdOtomatis();

                // Kalau tambah baru, status otomatis di-handle oleh default value SP database (tersedia)
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

        // --- LISTENER KLIK BARIS TABEL (UNTUK UPDATE DATA) ---
        tblProduk.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            if (newSelection != null) {
                txtIdBarang.setText(newSelection.getIdProduk());
                txtNamaBarang.setText(newSelection.getNamaProduk());
                txtMerk.setText(newSelection.getMerk());
                cmbKategoriProduk.setValue(newSelection.getKategori());
                txtHargaBarang.setText(String.valueOf(newSelection.getHarga()));
                txtStockBarang.setText(String.valueOf(newSelection.getStok()));

                // Masukkan data status lama ke ComboBox Status, dan AKTIFKAN kuncinya agar bisa diganti!
                cmbStatus.setValue(newSelection.getStatus());
                cmbStatus.setDisable(false);

                if ("Layanan".equalsIgnoreCase(newSelection.getKategori())) {
                    txtStockBarang.setDisable(true);
                    txtMerk.setDisable(true);
                } else {
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
                    if ("Tersedia".equalsIgnoreCase(status)) tersedia++;
                    else tidakTersedia++;
                }
                tblProduk.setItems(listProduk);

                lblTotalProduk.setText(String.valueOf(total));
                lblProdukTersedia.setText(String.valueOf(tersedia));
                lblProdukTidakTersedia.setText(String.valueOf(tidakTersedia));
            }
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Error Database", "Gagal memuat data: " + e.getMessage());
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
                cs.setDouble(3, Double.parseDouble(txtHargaBarang.getText()));
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
        // 1. Ambil data produk lama yang sedang dipilih di tabel
        Produk produkTerpilih = tblProduk.getSelectionModel().getSelectedItem();
        if (produkTerpilih == null) {
            showAlert(Alert.AlertType.WARNING, "Peringatan", "Pilih data produk di tabel yang ingin diubah!");
            return;
        }

        // 2. Ambil nilai status lama dan status baru yang dipilih di ComboBox
        String statusLama = produkTerpilih.getStatus(); // misal: "tersedia" atau "NonTersedia"
        String statusBaru = cmbStatus.getValue();       // pilihan baru dari user

        // 3. VALIDASI SAKTI: Mencegah perubahan dari 'tersedia' ke 'NonTersedia' lewat tombol Ubah
        if ("Tersedia".equalsIgnoreCase(statusLama) && "NonTersedia".equalsIgnoreCase(statusBaru)) {
            showAlert(Alert.AlertType.WARNING, "Peringatan",
                    "Untuk mengubah status menjadi 'NonTersedia', silahkan gunakan tombol 'Hapus Data'!");

            // Kembalikan pilihan ComboBox ke status semula agar tidak membingungkan
            cmbStatus.setValue(statusLama);
            return; // Batalkan proses update ke database
        }

        // 4. Jika validasi lolos, lanjutkan eksekusi Stored Procedure seperti biasa
        String sqlProcedure = "{CALL sp_UpdateProduk(?, ?, ?, ?, ?, ?)}";
        try {
            if (conn == null || conn.isClosed()) koneksi();

            try (CallableStatement cs = conn.prepareCall(sqlProcedure)) {
                cs.setString(1, txtIdBarang.getText());
                cs.setString(2, txtNamaBarang.getText());
                cs.setDouble(3, Double.parseDouble(txtHargaBarang.getText()));
                cs.setString(4, txtStockBarang.getText());
                cs.setString(5, txtMerk.getText());
                cs.setString(6, statusBaru); // Mengirim status baru yang valid

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
        // Kunci kembali dropdown status saat menekan tombol batal
        cmbStatus.setDisable(true);
    }

    private void cariDataProduk(String keyword) {
        if (keyword == null || keyword.isEmpty()) {
            tblProduk.setItems(listProduk);
            return;
        }

        ObservableList<Produk> filteredList = FXCollections.observableArrayList();
        for (Produk p : listProduk) {
            if (p.getNamaProduk().toLowerCase().contains(keyword.toLowerCase()) ||
                    p.getIdProduk().toLowerCase().contains(keyword.toLowerCase()) ||
                    p.getMerk().toLowerCase().contains(keyword.toLowerCase())) {
                filteredList.add(p);
            }
        }
        tblProduk.setItems(filteredList);
    }

    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    @FXML void handleNextPage(ActionEvent event) {}
    @FXML void handlePrevPage(ActionEvent event) {}
}