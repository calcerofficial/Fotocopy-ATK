package SistemFotocopy;

import Database.DBConnection;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
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
import javafx.util.Duration;

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
    private TableColumn<ProdukModel, String> colMerkProduk;

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
    private int currentPage = 0;
    private int totalPages = 1;
    private int totalItems = 0;
    private List<ProdukModel> halamanSaatIni = new ArrayList<>();

    private PauseTransition refreshTimer;
    private boolean isRefreshing = false;

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

        btnPrevPage.setOnAction(e -> handlePrevPage(e));
        btnNextPage.setOnAction(e -> handleNextPage(e));

        setupAutoRefresh();
    }

    // =========================================================
    // AUTO REFRESH
    // =========================================================
    private void setupAutoRefresh() {
        refreshTimer = new PauseTransition(Duration.seconds(5));
        refreshTimer.setOnFinished(e -> {
            refreshData();
            refreshTimer.playFromStart();
        });
        refreshTimer.play();
    }

    private void refreshData() {
        if (isRefreshing) return;
        isRefreshing = true;

        Platform.runLater(() -> {
            try {
                int currentPageTemp = currentPage;
                String currentKeyword = txtCari.getText();

                reloadDataFromDatabase();
                hitungStatistik();

                if (currentKeyword != null && !currentKeyword.trim().isEmpty() && filteredData != null) {
                    String keyword = currentKeyword.trim().toLowerCase();
                    filteredData.setPredicate(produk ->
                            produk.getNamaBarang().toLowerCase().contains(keyword) ||
                                    produk.getIdProduk().toLowerCase().contains(keyword)
                    );
                }

                List<ProdukModel> semuaData = new ArrayList<>(filteredData);
                totalItems = semuaData.size();
                totalPages = (int) Math.ceil((double) totalItems / ITEMS_PER_PAGE);
                if (totalPages == 0) totalPages = 1;

                if (currentPageTemp >= totalPages) {
                    currentPage = totalPages - 1;
                } else {
                    currentPage = currentPageTemp;
                }
                if (currentPage < 0) currentPage = 0;

                applyPaginationWithoutReset();

            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                isRefreshing = false;
            }
        });
    }

    // =========================================================
    // RELOAD DATA - TAMPILKAN APA ADANYA DARI DATABASE
    // =========================================================
    private void reloadDataFromDatabase() {
        masterData.clear();

        String query = "SELECT ID_Produk, Nama_Barang, Harga, Stok, Status_Barang " +
                "FROM v_LihatBarang ORDER BY ID_Produk";

        System.out.println("📊 Query Lihat Barang: " + query);

        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {

            int count = 0;
            while (rs.next()) {
                String idProduk = rs.getString("ID_Produk");
                String namaBarang = rs.getString("Nama_Barang");
                double harga = rs.getDouble("Harga");
                int stok = rs.getInt("Stok");
                String status = rs.getString("Status_Barang");

                // =========================================================
                // TAMPILKAN APA ADANYA DARI DATABASE
                // TIDAK ADA PERUBAHAN STATUS!
                // =========================================================
                System.out.println("✅ Produk: " + idProduk + " | " + namaBarang +
                        " | Stok: " + stok + " | Status: " + status);

                masterData.add(new ProdukModel(
                        idProduk,
                        namaBarang,
                        "-",
                        harga,
                        stok,
                        status
                ));
                count++;
            }

            System.out.println("📊 Total data dimuat: " + count);

            if (filteredData == null) {
                filteredData = new FilteredList<>(masterData, p -> true);
            } else {
                filteredData = new FilteredList<>(masterData, p -> true);
            }

        } catch (SQLException e) {
            e.printStackTrace();
            showAlert("Error", "Gagal memuat data barang: " + e.getMessage());
            loadDataFallback();
        }
    }

    // =========================================================
    // FALLBACK: Query langsung ke tabel Produk
    // =========================================================
    private void loadDataFallback() {
        try {
            String query = "SELECT ID_Produk, Nama_Barang, Harga, Stok, Status_Barang " +
                    "FROM Produk WHERE Kategori_Produk = 'Barang' ORDER BY ID_Produk";

            try (Statement stmt = connection.createStatement();
                 ResultSet rs = stmt.executeQuery(query)) {

                while (rs.next()) {
                    String idProduk = rs.getString("ID_Produk");
                    String namaBarang = rs.getString("Nama_Barang");
                    double harga = rs.getDouble("Harga");
                    int stok = rs.getInt("Stok");
                    String status = rs.getString("Status_Barang");

                    // TAMPILKAN APA ADANYA
                    masterData.add(new ProdukModel(
                            idProduk,
                            namaBarang,
                            "-",
                            harga,
                            stok,
                            status
                    ));
                }

                if (filteredData == null) {
                    filteredData = new FilteredList<>(masterData, p -> true);
                } else {
                    filteredData = new FilteredList<>(masterData, p -> true);
                }
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }

    // =========================================================
    // SETUP TABLE COLUMNS
    // =========================================================
    private void setupTableColumns() {
        colIdBarang.setCellValueFactory(cellData -> cellData.getValue().idProdukProperty());
        colNamaBarang.setCellValueFactory(cellData -> cellData.getValue().namaBarangProperty());
        colMerkProduk.setCellValueFactory(cellData -> cellData.getValue().merkProperty());
        colMerkProduk.setVisible(false);

        colHarga.setCellValueFactory(cellData -> cellData.getValue().hargaProperty());
        colStock.setCellValueFactory(cellData -> cellData.getValue().stokProperty());
        colStatusProduk.setCellValueFactory(cellData -> cellData.getValue().statusProperty());

        colHarga.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(Number item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : formatRupiah(item.doubleValue()));
                setStyle("-fx-alignment: CENTER-RIGHT;");
            }
        });

        colStock.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(Number item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    int stok = item.intValue();
                    setText(String.valueOf(stok));
                    setStyle("-fx-alignment: CENTER;");

                    if (stok == 0) {
                        setStyle("-fx-text-fill: #EF4444; -fx-font-weight: bold; -fx-alignment: CENTER;");
                    } else if (stok <= 5) {
                        setStyle("-fx-text-fill: #F59E0B; -fx-font-weight: bold; -fx-alignment: CENTER;");
                    } else {
                        setStyle("-fx-text-fill: #16A34A; -fx-alignment: CENTER;");
                    }
                }
            }
        });

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
    // LOAD DATA PERTAMA KALI
    // =========================================================
    private void loadData() {
        reloadDataFromDatabase();

        if (filteredData == null) {
            filteredData = new FilteredList<>(masterData, p -> true);
        }

        currentPage = 0;
        applyPaginationWithoutReset();
    }

    // =========================================================
    // HITUNG STATISTIK
    // =========================================================
    private void hitungStatistik() {
        try {
            String query = "SELECT " +
                    "dbo.f_TotalProdukBarang() AS Total, " +
                    "dbo.f_TotalProdukBarangTersedia() AS Tersedia, " +
                    "dbo.f_TotalProdukBarangNonTersedia() AS NonTersedia";

            try (Statement stmt = connection.createStatement();
                 ResultSet rs = stmt.executeQuery(query)) {
                if (rs.next()) {
                    int total = rs.getInt("Total");
                    int tersedia = rs.getInt("Tersedia");
                    int tidakTersedia = rs.getInt("NonTersedia");

                    System.out.println("📊 Statistik Barang: Total=" + total +
                            ", Tersedia=" + tersedia +
                            ", TidakTersedia=" + tidakTersedia);

                    lblTotalProduk.setText(String.valueOf(total));
                    lblProdukTersedia.setText(String.valueOf(tersedia));
                    lblProdukTidakTersedia.setText(String.valueOf(tidakTersedia));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();

            // FALLBACK
            try {
                String query = "SELECT " +
                        "COUNT(*) AS Total, " +
                        "SUM(CASE WHEN LOWER(Status_Barang) = 'tersedia' AND Stok > 0 THEN 1 ELSE 0 END) AS Tersedia, " +
                        "SUM(CASE WHEN LOWER(Status_Barang) != 'tersedia' OR Stok = 0 THEN 1 ELSE 0 END) AS TidakTersedia " +
                        "FROM Produk WHERE Kategori_Produk = 'Barang'";

                try (Statement stmt = connection.createStatement();
                     ResultSet rs = stmt.executeQuery(query)) {
                    if (rs.next()) {
                        lblTotalProduk.setText(String.valueOf(rs.getInt("Total")));
                        lblProdukTersedia.setText(String.valueOf(rs.getInt("Tersedia")));
                        lblProdukTidakTersedia.setText(String.valueOf(rs.getInt("TidakTersedia")));
                    }
                }
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
        }
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
                            produk.getIdProduk().toLowerCase().contains(keyword) ||
                            produk.getStatus().toLowerCase().contains(keyword)
            );
            currentPage = 0;
            applyPaginationWithoutReset();
        });
    }

    // =========================================================
    // PAGINATION
    // =========================================================
    private void applyPaginationWithoutReset() {
        if (filteredData == null) return;

        List<ProdukModel> semuaData = new ArrayList<>(filteredData);
        totalItems = semuaData.size();

        totalPages = (int) Math.ceil((double) totalItems / ITEMS_PER_PAGE);
        if (totalPages == 0) totalPages = 1;

        if (currentPage >= totalPages) currentPage = totalPages - 1;
        if (currentPage < 0) currentPage = 0;

        int fromIndex = currentPage * ITEMS_PER_PAGE;
        int toIndex = Math.min(fromIndex + ITEMS_PER_PAGE, totalItems);

        if (totalItems == 0) {
            tblProduk.setItems(FXCollections.observableArrayList());
        } else {
            halamanSaatIni = semuaData.subList(fromIndex, toIndex);
            tblProduk.setItems(FXCollections.observableArrayList(halamanSaatIni));
        }

        int startItem = totalItems > 0 ? fromIndex + 1 : 0;
        int endItem = Math.min(toIndex, totalItems);
        lblInfoData.setText("Menampilkan " + startItem + "-" + endItem + " dari " + totalItems + " data");
        btnPage1.setText("Halaman " + (currentPage + 1) + " / " + totalPages);

        btnPrevPage.setDisable(currentPage == 0);
        btnNextPage.setDisable(currentPage >= totalPages - 1);
    }

    // =========================================================
    // PAGINATION HANDLERS
    // =========================================================
    @FXML
    void handlePrevPage(ActionEvent event) {
        if (currentPage > 0) {
            currentPage--;
            applyPaginationWithoutReset();
        }
    }

    @FXML
    void handleNextPage(ActionEvent event) {
        if (currentPage < totalPages - 1) {
            currentPage++;
            applyPaginationWithoutReset();
        }
    }

    // =========================================================
    // HELPER METHODS
    // =========================================================
    public void refreshManual() {
        refreshData();
    }

    public void stopAutoRefresh() {
        if (refreshTimer != null) {
            refreshTimer.stop();
        }
    }

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