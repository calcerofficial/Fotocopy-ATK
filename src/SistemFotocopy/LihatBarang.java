package SistemFotocopy;

import Database.DBConnection;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;

import java.net.URL;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;

public class LihatBarang implements Initializable {

    @FXML
    private Button btnNextPage;

    @FXML
    private Button btnPage1;

    @FXML
    private Button btnPrevPage;

    @FXML
    private TableColumn<ProdukModel, String> colIdBarang;

    @FXML
    private TableColumn<ProdukModel, String> colNamaBarang;

    @FXML
    private TableColumn<ProdukModel, String> colMerkProduk; // Tetap ada tapi bisa di-hidden

    @FXML
    private TableColumn<ProdukModel, Number> colHarga;

    @FXML
    private TableColumn<ProdukModel, Number> colStock;

    @FXML
    private TableColumn<ProdukModel, String> colStatusProduk;

    @FXML
    private Label lblInfoData;

    @FXML
    private Label lblProdukTersedia;

    @FXML
    private Label lblProdukTidakTersedia;

    @FXML
    private Label lblTotalProduk;

    @FXML
    private BorderPane rootPane;

    @FXML
    private TableView<ProdukModel> tblProduk;

    @FXML
    private TextField txtCari;

    // =========================================================
    // STATE
    // =========================================================
    private Connection connection;
    private final ObservableList<ProdukModel> masterData = FXCollections.observableArrayList();
    private FilteredList<ProdukModel> filteredData;

    private static final int ITEMS_PER_PAGE = 10;
    private int currentPage = 1;
    private int totalPages = 1;
    private List<ProdukModel> halamanSaatIni = new ArrayList<>();

    // =========================================================
    // INITIALIZE
    // =========================================================
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        try {
            connection = DBConnection.getConnection();
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Error", "Gagal koneksi ke database: " + e.getMessage());
        }

        setupTableColumns();
        setupSearchListener();
        loadData();
        hitungStatistik();

        // Set default button states
        btnPrevPage.setDisable(true);
        btnNextPage.setDisable(true);
        btnPage1.setText("Halaman 1");
    }

    // =========================================================
    // SETUP TABLE COLUMNS
    // =========================================================
    private void setupTableColumns() {
        colIdBarang.setCellValueFactory(cellData -> cellData.getValue().idProdukProperty());
        colNamaBarang.setCellValueFactory(cellData -> cellData.getValue().namaBarangProperty());

        // =========================================================
        // KOLOM MERK - HIDDEN (karena tidak ada di database)
        // =========================================================
        colMerkProduk.setCellValueFactory(cellData -> cellData.getValue().merkProperty());
        colMerkProduk.setVisible(false); // Sembunyikan kolom Merk

        colHarga.setCellValueFactory(cellData -> cellData.getValue().hargaProperty());
        colStock.setCellValueFactory(cellData -> cellData.getValue().stokProperty());
        colStatusProduk.setCellValueFactory(cellData -> cellData.getValue().statusProperty());

        // Format harga ke Rupiah
        colHarga.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(Number item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : formatRupiah(item.doubleValue()));
                setStyle("-fx-alignment: CENTER-RIGHT;");
            }
        });

        // Format stok
        colStock.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(Number item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(String.valueOf(item.intValue()));
                    setStyle("-fx-alignment: CENTER;");

                    // Warna merah jika stok 0
                    if (item.intValue() == 0) {
                        setStyle("-fx-text-fill: #EF4444; -fx-font-weight: bold; -fx-alignment: CENTER;");
                    } else if (item.intValue() <= 5) {
                        setStyle("-fx-text-fill: #F59E0B; -fx-font-weight: bold; -fx-alignment: CENTER;");
                    } else {
                        setStyle("-fx-text-fill: #16A34A; -fx-alignment: CENTER;");
                    }
                }
            }
        });

        // Warna status
        colStatusProduk.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item);
                    setStyle("-fx-alignment: CENTER;");
                    if (item.equalsIgnoreCase("tersedia")) {
                        setStyle("-fx-text-fill: #16A34A; -fx-font-weight: bold; -fx-alignment: CENTER;");
                    } else {
                        setStyle("-fx-text-fill: #EF4444; -fx-font-weight: bold; -fx-alignment: CENTER;");
                    }
                }
            }
        });
    }

    // =========================================================
    // LOAD DATA (HANYA KATEGORI BARANG)
    // =========================================================
    private void loadData() {
        masterData.clear();

        // =========================================================
        // HAPUS KOLOM MERK DARI QUERY
        // =========================================================
        String query = "SELECT ID_Produk, Nama_Barang, Harga, Stok, Status_Barang " +
                "FROM Produk WHERE Kategori_Produk = 'barang' ORDER BY Nama_Barang";

        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {

            while (rs.next()) {
                masterData.add(new ProdukModel(
                        rs.getString("ID_Produk"),
                        rs.getString("Nama_Barang"),
                        "-", // Merk tidak ada, pakai "-"
                        rs.getDouble("Harga"),
                        rs.getInt("Stok"),
                        rs.getString("Status_Barang")
                ));
            }

            filteredData = new FilteredList<>(masterData, p -> true);
            currentPage = 1;
            updatePagination();

        } catch (SQLException e) {
            e.printStackTrace();
            showAlert("Error", "Gagal memuat data barang: " + e.getMessage());
        }
    }

    // =========================================================
    // HITUNG STATISTIK
    // =========================================================
    private void hitungStatistik() {
        int total = 0;
        int tersedia = 0;
        int tidakTersedia = 0;

        String query = "SELECT " +
                "COUNT(*) AS Total, " +
                "SUM(CASE WHEN Status_Barang = 'tersedia' THEN 1 ELSE 0 END) AS Tersedia, " +
                "SUM(CASE WHEN Status_Barang != 'tersedia' THEN 1 ELSE 0 END) AS TidakTersedia " +
                "FROM Produk WHERE Kategori_Produk = 'barang'";

        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {
            if (rs.next()) {
                total = rs.getInt("Total");
                tersedia = rs.getInt("Tersedia");
                tidakTersedia = rs.getInt("TidakTersedia");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        lblTotalProduk.setText(String.valueOf(total));
        lblProdukTersedia.setText(String.valueOf(tersedia));
        lblProdukTidakTersedia.setText(String.valueOf(tidakTersedia));
    }

    // =========================================================
    // SEARCH LISTENER
    // =========================================================
    private void setupSearchListener() {
        txtCari.textProperty().addListener((obs, oldVal, newVal) -> {
            if (filteredData == null) return;
            String keyword = newVal == null ? "" : newVal.trim().toLowerCase();
            filteredData.setPredicate(produk ->
                    keyword.isEmpty() ||
                            produk.getNamaBarang().toLowerCase().contains(keyword) ||
                            produk.getIdProduk().toLowerCase().contains(keyword)
            );
            currentPage = 1;
            updatePagination();
        });
    }

    // =========================================================
    // PAGINATION
    // =========================================================
    private void updatePagination() {
        if (filteredData == null) return;

        List<ProdukModel> semuaData = new ArrayList<>(filteredData);
        int totalItems = semuaData.size();
        totalPages = (int) Math.ceil((double) totalItems / ITEMS_PER_PAGE);
        if (totalPages == 0) totalPages = 1;

        if (currentPage > totalPages) currentPage = totalPages;
        if (currentPage < 1) currentPage = 1;

        int fromIndex = (currentPage - 1) * ITEMS_PER_PAGE;
        int toIndex = Math.min(fromIndex + ITEMS_PER_PAGE, totalItems);

        if (totalItems == 0) {
            tblProduk.setItems(FXCollections.observableArrayList());
        } else {
            halamanSaatIni = semuaData.subList(fromIndex, toIndex);
            tblProduk.setItems(FXCollections.observableArrayList(halamanSaatIni));
        }

        // Update info
        btnPage1.setText("Halaman " + currentPage + " dari " + totalPages);
        btnPrevPage.setDisable(currentPage <= 1);
        btnNextPage.setDisable(currentPage >= totalPages);

        int start = totalItems == 0 ? 0 : fromIndex + 1;
        int end = Math.min(toIndex, totalItems);
        lblInfoData.setText("Menampilkan " + start + "-" + end + " dari " + totalItems + " data");
    }

    // =========================================================
    // PAGINATION HANDLERS
    // =========================================================
    @FXML
    void handleNextPage(ActionEvent event) {
        if (currentPage < totalPages) {
            currentPage++;
            updatePagination();
        }
    }

    @FXML
    void handlePrevPage(ActionEvent event) {
        if (currentPage > 1) {
            currentPage--;
            updatePagination();
        }
    }

    // =========================================================
    // HELPER METHODS
    // =========================================================
    private String formatRupiah(double nominal) {
        NumberFormat formatRp = NumberFormat.getNumberInstance(new Locale("in", "ID"));
        return "Rp " + formatRp.format(nominal);
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    // =========================================================
    // MODEL — PRODUK
    // =========================================================
    public static class ProdukModel {
        private final SimpleStringProperty idProduk;
        private final SimpleStringProperty namaBarang;
        private final SimpleStringProperty merk;
        private final SimpleDoubleProperty harga;
        private final SimpleIntegerProperty stok;
        private final SimpleStringProperty status;

        public ProdukModel(String idProduk, String namaBarang, String merk,
                           double harga, int stok, String status) {
            this.idProduk = new SimpleStringProperty(idProduk);
            this.namaBarang = new SimpleStringProperty(namaBarang);
            this.merk = new SimpleStringProperty(merk);
            this.harga = new SimpleDoubleProperty(harga);
            this.stok = new SimpleIntegerProperty(stok);
            this.status = new SimpleStringProperty(status);
        }

        public String getIdProduk() { return idProduk.get(); }
        public SimpleStringProperty idProdukProperty() { return idProduk; }

        public String getNamaBarang() { return namaBarang.get(); }
        public SimpleStringProperty namaBarangProperty() { return namaBarang; }

        public String getMerk() { return merk.get(); }
        public SimpleStringProperty merkProperty() { return merk; }

        public double getHarga() { return harga.get(); }
        public SimpleDoubleProperty hargaProperty() { return harga; }

        public int getStok() { return stok.get(); }
        public SimpleIntegerProperty stokProperty() { return stok; }

        public String getStatus() { return status.get(); }
        public SimpleStringProperty statusProperty() { return status; }
    }
}