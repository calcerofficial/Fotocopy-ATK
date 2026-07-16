package SistemFotocopy;

import Database.DBConnection;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.BorderPane;
import java.sql.Statement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class DataSupplier {

    @FXML
    private Button btnBatal;

    @FXML
    private Button btnHapus;

    @FXML
    private Button btnNextPage;

    @FXML
    private Button btnPage1;

    @FXML
    private Button btnPrevPage;

    @FXML
    private Button btnSimpan;

    @FXML
    private Button btnUbah;

    @FXML
    private TableColumn<Supplier, String> colAlamat;

    @FXML
    private TableColumn<Supplier, String> colEmail;

    @FXML
    private TableColumn<Supplier, String> colIdSupplier;

    @FXML
    private TableColumn<Supplier, String> colNamaSupplier;

    @FXML
    private TableColumn<Supplier, String> colNoTelepon;

    @FXML
    private TableColumn<Supplier, String> colStatus;

    @FXML
    private Label lblInfoData;

    @FXML
    private Label lblSupplierAktif;

    @FXML
    private Label lblSupplierNonaktif;

    @FXML
    private Label lblTotalSupplier;

    @FXML
    private BorderPane rootPane;

    @FXML
    private TableView<Supplier> tblSupplier;

    @FXML
    private TextArea txtAlamatLengkap;

    @FXML
    private TextField txtCari;

    @FXML
    private TextField txtEmail;

    @FXML
    private TextField txtIdSupplier;

    @FXML
    private TextField txtNamaSupplier;

    @FXML
    private TextField txtNomorTelepon;

    // HAPUS atau COMMENT deklarasi ini karena tidak ada di FXML
    // @FXML
    // private ComboBox<String> cbStatus;

    private DBConnection db = new DBConnection();

    private int currentPage = 1;

    private final int rowsPerPage = 10;

    private void loadData() {
        ObservableList<Supplier> list = FXCollections.observableArrayList();

        int offset = (currentPage - 1) * rowsPerPage;

        String query = "SELECT * FROM Supplier " +
                "ORDER BY (CASE WHEN Status_Supplier = 'NonAktif' THEN 1 ELSE 0 END) ASC, ID_Supplier ASC " +
                "OFFSET " + offset + " ROWS " +
                "FETCH NEXT " + rowsPerPage + " ROWS ONLY";

        try (Statement st = db.getConnection().createStatement();
             ResultSet rs = st.executeQuery(query)) {

            while (rs.next()) {
                list.add(new Supplier(
                        rs.getString("ID_Supplier"),
                        rs.getString("Nama_Supplier"),
                        rs.getString("Alamat"),
                        rs.getString("No_Telepon"),
                        rs.getString("Email"),
                        rs.getString("Status_Supplier")
                ));
            }
            tblSupplier.setItems(list);
            lblTotalSupplier.setText(String.valueOf(list.size()));
        } catch (SQLException e) {
            System.out.println("Gagal load data: " + e);
        }
    }

    private void updateDashboard() {
        String query = "SELECT " +
                "(SELECT COUNT(*) FROM Supplier) AS Total, " +
                "(SELECT COUNT(*) FROM Supplier WHERE Status_Supplier='aktif') AS Aktif, " +
                "(SELECT COUNT(*) FROM Supplier WHERE Status_Supplier='NonAktif') AS Nonaktif";

        try (Statement st = db.getConnection().createStatement();
             ResultSet rs = st.executeQuery(query)) {

            if (rs.next()) {
                lblTotalSupplier.setText(rs.getString("Total"));
                lblSupplierAktif.setText(rs.getString("Aktif"));
                lblSupplierNonaktif.setText(rs.getString("Nonaktif"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public class Supplier {
        private String id, nama, alamat, telepon, email, status;

        public Supplier(String id, String nama, String alamat, String telepon, String email, String status) {
            this.id = id;
            this.nama = nama;
            this.alamat = alamat;
            this.telepon = telepon;
            this.email = email;
            this.status = status;
        }

        public String getId() { return id; }
        public String getNama() { return nama; }
        public String getAlamat() { return alamat; }
        public String getTelepon() { return telepon; }
        public String getEmail() { return email; }
        public String getStatus() { return status; }
    }

    private void generateIdOtomatis() {
        String query = "SELECT TOP 1 ID_Supplier FROM Supplier ORDER BY ID_Supplier DESC";

        try (Statement st = db.getConnection().createStatement();
             ResultSet rs = st.executeQuery(query)) {

            if (rs.next()) {
                String lastId = rs.getString("ID_Supplier");
                int angka = Integer.parseInt(lastId.substring(3));
                int nextAngka = angka + 1;
                txtIdSupplier.setText("SPR" + String.format("%03d", nextAngka));
            } else {
                txtIdSupplier.setText("SPR001");
            }
        } catch (Exception e) {
            txtIdSupplier.setText("SPR001");
            System.out.println("Error saat generate ID: " + e.getMessage());
        }
    }

    @FXML
    public void initialize() {
        // Menghubungkan kolom tabel dengan variabel di class Supplier
        colIdSupplier.setCellValueFactory(new PropertyValueFactory<>("id"));
        colNamaSupplier.setCellValueFactory(new PropertyValueFactory<>("nama"));
        colAlamat.setCellValueFactory(new PropertyValueFactory<>("alamat"));
        colNoTelepon.setCellValueFactory(new PropertyValueFactory<>("telepon"));
        colEmail.setCellValueFactory(new PropertyValueFactory<>("email"));
        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));

        loadData();
        generateIdOtomatis();
        updateDashboard();

        // HAPUS semua kode yang berkaitan dengan cbStatus
        // cbStatus.getItems().addAll("aktif", "NonAktif");
        // cbStatus.setValue("aktif");
        // cbStatus.setDisable(true);

        txtCari.textProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue == null || newValue.isEmpty()) {
                loadData();
            } else {
                searchData(newValue);
            }
        });

        // Listener untuk mengisi form saat baris tabel diklik
        tblSupplier.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null) {
                txtIdSupplier.setText(newValue.getId());
                txtNamaSupplier.setText(newValue.getNama());
                txtAlamatLengkap.setText(newValue.getAlamat());
                txtNomorTelepon.setText(newValue.getTelepon());
                txtEmail.setText(newValue.getEmail());

                btnSimpan.setDisable(true);

                // HAPUS setting cbStatus
                // cbStatus.setValue(newValue.getStatus());
                // cbStatus.setDisable(false);
            }
        });
    }

    @FXML
    void handleNextPage(ActionEvent event) {
        currentPage++;
        loadData();

        if (tblSupplier.getItems().isEmpty()) {
            currentPage--;
            loadData();
        }
    }

    @FXML
    void handlePrevPage(ActionEvent event) {
        if (currentPage > 1) {
            currentPage--;
            loadData();
        }
    }

    @FXML
    void handleBatal(ActionEvent event) {
        txtIdSupplier.clear();
        txtNamaSupplier.clear();
        txtEmail.clear();
        txtNomorTelepon.clear();
        txtAlamatLengkap.clear();

        btnSimpan.setDisable(false);

        // HAPUS setting cbStatus
        // cbStatus.setValue("aktif");
        // cbStatus.setDisable(true);

        generateIdOtomatis();
        loadData();
        updateDashboard();
    }

    @FXML
    void handleHapusData(ActionEvent event) {
        if (txtIdSupplier.getText().isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Peringatan", "Pilih data yang ingin dihapus terlebih dahulu.");
            return;
        }

        String query = "{call sp_DeleteSupplierSoft(?)}";

        try (java.sql.CallableStatement cs = db.getConnection().prepareCall(query)) {
            cs.setString(1, txtIdSupplier.getText());
            cs.execute();

            loadData();
            updateDashboard();
            handleBatal(event);

            showAlert(Alert.AlertType.INFORMATION, "Sukses", "Data berhasil dinonaktifkan.");
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Gagal", e.getMessage());
        }
    }

    @FXML
    void handleSimpanData(ActionEvent event) {
        // validasi data gaboleh sama
        String queryCek = "SELECT COUNT(*) FROM Supplier WHERE Nama_Supplier = ? OR Email = ? OR No_Telepon = ?";
        try (java.sql.PreparedStatement ps = db.getConnection().prepareStatement(queryCek)) {
            ps.setString(1, txtNamaSupplier.getText().trim());
            ps.setString(2, txtEmail.getText().trim());
            ps.setString(3, txtNomorTelepon.getText().trim());

            ResultSet rs = ps.executeQuery();
            if (rs.next() && rs.getInt(1) > 0) {
                showAlert(Alert.AlertType.WARNING, "Data Duplikat", "Nama, Email, atau No. Telepon sudah terdaftar!");
                return;
            }
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Error", e.getMessage());
            return;
        }

        String query = "{call sp_TambahSupplier(?,?,?,?)}";

        try (java.sql.CallableStatement cs = db.getConnection().prepareCall(query)) {
            cs.setString(1, txtNamaSupplier.getText());
            cs.setString(2, txtAlamatLengkap.getText());
            cs.setString(3, txtNomorTelepon.getText());
            cs.setString(4, txtEmail.getText());

            cs.execute();

            loadData();
            handleBatal(event);
            updateDashboard();
            generateIdOtomatis();

            showAlert(Alert.AlertType.INFORMATION, "Sukses", "Data Supplier berhasil disimpan.");

        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Gagal", e.getMessage());
        }
    }

    @FXML
    void handleUbahData(ActionEvent event) {
        String query = "{call sp_UpdateSupplier(?,?,?,?,?,?)}";

        try (java.sql.CallableStatement cs = db.getConnection().prepareCall(query)) {
            cs.setString(1, txtIdSupplier.getText().trim());
            cs.setString(2, txtNamaSupplier.getText().trim());
            cs.setString(3, txtAlamatLengkap.getText().trim());
            cs.setString(4, txtNomorTelepon.getText().trim());
            cs.setString(5, txtEmail.getText().trim());

            // Status default ke 'aktif' karena tidak ada ComboBox
            cs.setString(6, "aktif");

            cs.execute();

            loadData();
            handleBatal(event);
            updateDashboard();

            showAlert(Alert.AlertType.INFORMATION, "Sukses", "Data berhasil diubah.");

        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Gagal Ubah", e.getMessage());
        }
    }

    @FXML
    private void searchData(String keyword) {
        ObservableList<Supplier> list = FXCollections.observableArrayList();

        String query = "SELECT * FROM Supplier WHERE " +
                "ID_Supplier LIKE '%" + keyword + "%' OR " +
                "Nama_Supplier LIKE '%" + keyword + "%' OR " +
                "Alamat LIKE '%" + keyword + "%' OR " +
                "Email LIKE '%" + keyword + "%' OR " +
                "No_Telepon LIKE '%" + keyword + "%' OR " +
                "Status_Supplier LIKE '%" + keyword + "%'" +
                "ORDER BY (CASE WHEN Status_Supplier = 'NonAktif' THEN 1 ELSE 0 END) ASC, ID_Supplier ASC";

        try (Statement st = db.getConnection().createStatement();
             ResultSet rs = st.executeQuery(query)) {

            while (rs.next()) {
                list.add(new Supplier(
                        rs.getString("ID_Supplier"),
                        rs.getString("Nama_Supplier"),
                        rs.getString("Alamat"),
                        rs.getString("No_Telepon"),
                        rs.getString("Email"),
                        rs.getString("Status_Supplier")
                ));
            }
            tblSupplier.setItems(list);
        } catch (SQLException e) {
            System.out.println("Gagal mencari data: " + e.getMessage());
        }
    }

    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}