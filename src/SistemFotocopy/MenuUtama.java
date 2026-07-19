package SistemFotocopy;

import Database.DBConnection;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.SVGPath;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class MenuUtama {

    @FXML
    private SVGPath IconNavigasi;

    @FXML
    private SVGPath IconPesan;

    @FXML
    private Button btDashboard;

    @FXML
    private Button btKelolaData;

    @FXML
    private Button btKembali;

    @FXML
    private Button btPembelianStock;

    @FXML
    private Button btMaintenanceMesin;

    @FXML
    private Button btLihatTransaksi;

    @FXML
    private SVGPath chevronLihatTransaksi;

    @FXML
    private VBox submenuLihatTransaksi;

    @FXML
    private SVGPath chevronKelolaData;

    @FXML
    private StackPane contentArea;

    @FXML
    private VBox submenuKelolaData;

    @FXML
    private Label lblDashboardText;

    @FXML
    private SVGPath iconDashboard;

    @FXML
    private Label headerTitle;

    @FXML
    private Label lblNamaUser;  // ← TAMBAHKAN INI

    private boolean isSubmenuVisible = false;
    private boolean isSubmenuTransaksiVisible = false;
    private Button activeButton = null;

    private static final String ICON_DASHBOARD = "M3 13h8V3H3v10zm0 8h8v-6H3v6zm10 0h8V11h-8v10zm0-18v6h8V3h-8z";
    private static final String ICON_PEGAWAI = "M6 2c-1.1 0-1.99.9-1.99 2L4 20c0 1.1.89 2 1.99 2H18c1.1 0 2-.9 2-2V8l-6-6H6zm7 7V3.5L18.5 9H13z";
    private static final String ICON_PRODUK = "M19 6h-2c0-2.76-2.24-5-5-5S7 3.24 7 6H5c-1.1 0-2 .9-2 2v12c0 1.1.9 2 2 2h14c1.1 0 2-.9 2-2V8c0-1.1-.9-2-2-2zm-7-3c1.66 0 3 1.34 3 3H9c0-1.66 1.34-3 3-3zm0 10c-2.76 0-5-2.24-5-5h2c0 1.66 1.34 3 3 3s3-1.34 3-3h2c0 2.76-2.24 5-5 5z";
    private static final String ICON_MESIN = "M19.14 12.94c.04-.3.06-.61.06-.94 0-.32-.02-.64-.07-.94l2.03-1.58c.18-.14.23-.41.12-.61l-1.92-3.32c-.12-.22-.37-.29-.59-.22l-2.39.96c-.5-.38-1.03-.7-1.62-.94l-.36-2.54c-.04-.24-.24-.41-.48-.41h-3.84c-.24 0-.43.17-.47.41l-.36 2.54c-.59.24-1.13.57-1.62.94l-2.39-.96c-.22-.08-.47 0-.59.22L2.74 8.87c-.12.21-.08.47.12.61l2.03 1.58c-.05.3-.09.63-.09.94s.02.64.07.94l-2.03 1.58c-.18.14-.23.41-.12.61l1.92 3.32c.12.22.37.29.59.22l2.39-.96c.5.38 1.03.7 1.62.94l.36 2.54c.05.24.24.41.48.41h3.84c.24 0 .44-.17.47-.41l.36-2.54c.59-.24 1.13-.56 1.62-.94l2.39.96c.22.08.47 0 .59-.22l1.92-3.32c.12-.22.07-.47-.12-.61l-2.01-1.58z";
    private static final String ICON_SUPPLIER = "M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm0 3c1.66 0 3 1.34 3 3s-1.34 3-3 3-3-1.34-3-3 1.34-3 3-3zm0 14.2c-2.5 0-4.71-1.28-6-3.22.03-1.99 4-3.08 6-3.08 1.99 0 5.97 1.09 6 3.08-1.29 1.94-3.5 3.22-6 3.22z";
    private static final String ICON_MAINTENANCE = "M19 6h-2c0-2.76-2.24-5-5-5S7 3.24 7 6H5c-1.1 0-2 .9-2 2v12c0 1.1.9 2 2 2h14c1.1 0 2-.9 2-2V8c0-1.1-.9-2-2-2zm-7-3c1.66 0 3 1.34 3 3H9c0-1.66 1.34-3 3-3zm0 10c-2.76 0-5-2.24-5-5h2c0 1.66 1.34 3 3 3s3-1.34 3-3h2c0 2.76-2.24 5-5 5z";
    private static final String ICON_STOCK = "M19 6h-2c0-2.76-2.24-5-5-5S7 3.24 7 6H5c-1.1 0-2 .9-2 2v12c0 1.1.9 2 2 2h14c1.1 0 2-.9 2-2V8c0-1.1-.9-2-2-2zm-7-3c1.66 0 3 1.34 3 3H9c0-1.66 1.34-3 3-3zm0 10c-2.76 0-5-2.24-5-5h2c0 1.66 1.34 3 3 3s3-1.34 3-3h2c0 2.76-2.24 5-5 5z";

    @FXML
    void OnActionDasbord(ActionEvent event) {
        setActiveButton(btDashboard);
        loadContent("/css/LayoutSistemFotocopy/DashboardContent.fxml");
        updateHeader("Dashboard", ICON_DASHBOARD);
        updateHeaderTitle("Dashboard Admin");
    }

    @FXML
    void OnActionDataMesin(ActionEvent event) {
        setActiveButton(btKelolaData);
        loadContent("/css/LayoutSistemFotocopy/DataMesin.fxml");
        updateHeader("Data Mesin", ICON_MESIN);
        updateHeaderTitle("Kelola Data - Mesin");
    }

    @FXML
    void OnActionDataPegawai(ActionEvent event) {
        setActiveButton(btKelolaData);
        loadContent("/css/LayoutSistemFotocopy/DataPegawai.fxml");
        updateHeader("Data Pegawai", ICON_PEGAWAI);
        updateHeaderTitle("Kelola Data - Pegawai");
    }

    @FXML
    void OnActionDataProduk(ActionEvent event) {
        setActiveButton(btKelolaData);
        loadContent("/css/LayoutSistemFotocopy/DataProduk.fxml");
        updateHeader("Data Produk", ICON_PRODUK);
        updateHeaderTitle("Kelola Data - Produk");
    }

    @FXML
    void OnActionDataSupplier(ActionEvent event) {
        setActiveButton(btKelolaData);
        loadContent("/css/LayoutSistemFotocopy/DataSupplier.fxml");
        updateHeader("Data Supplier", ICON_SUPPLIER);
        updateHeaderTitle("Kelola Data - Supplier");
    }

    @FXML
    void OnActionKembali(ActionEvent event) {
        try {
            URL resource = getClass().getResource("/LayoutSistemFotocopy/MenuLogin.fxml");
            if (resource == null) {
                throw new IOException("File MenuLogin.fxml tidak ditemukan");
            }
            Parent loginView = FXMLLoader.load(resource);
            Stage stage = (Stage) btKembali.getScene().getWindow();

            javafx.geometry.Rectangle2D screenBounds = javafx.stage.Screen.getPrimary().getVisualBounds();
            Scene scene = new Scene(loginView, screenBounds.getWidth(), screenBounds.getHeight());

            stage.setScene(scene);
            stage.setTitle("Login");
            stage.setMaximized(true);
            stage.setResizable(false);
            stage.setFullScreenExitHint("");
            stage.setFullScreen(true);
            stage.show();

        } catch (IOException e) {
            e.printStackTrace();
            showError("Gagal kembali ke halaman login: " + e.getMessage());
        }
    }

    @FXML
    void OnActionMaintenanceMesin(ActionEvent event) {
        setActiveButton(btMaintenanceMesin);
        loadContent("/css/LayoutSistemFotocopy/MaintenanceMesin.fxml");
        updateHeader("Maintenance Mesin", ICON_MAINTENANCE);
        updateHeaderTitle("Maintenance Mesin");
    }

    @FXML
    void OnActionPembelianStock(ActionEvent event) {
        setActiveButton(btPembelianStock);
        loadContent("/css/LayoutSistemFotocopy/TransaksiPembelianStock.fxml");
        updateHeader("Pembelian Stock", ICON_STOCK);
        updateHeaderTitle("Transaksi - Pembelian Stock");
    }

    @FXML
    void OnActionToggleKelolaData(ActionEvent event) {
        isSubmenuVisible = !isSubmenuVisible;
        submenuKelolaData.setVisible(isSubmenuVisible);
        submenuKelolaData.setManaged(isSubmenuVisible);

        if (isSubmenuVisible) {
            chevronKelolaData.setRotate(180);
            setActiveButton(btKelolaData);
            updateHeader("Kelola Data", ICON_PEGAWAI);
            updateHeaderTitle("Kelola Data");
        } else {
            chevronKelolaData.setRotate(0);
        }
    }

    @FXML
    void OnActionToggleLihatTransaksi(ActionEvent event) {
        isSubmenuTransaksiVisible = !isSubmenuTransaksiVisible;
        submenuLihatTransaksi.setVisible(isSubmenuTransaksiVisible);
        submenuLihatTransaksi.setManaged(isSubmenuTransaksiVisible);

        if (isSubmenuTransaksiVisible) {
            chevronLihatTransaksi.setRotate(180);
            setActiveButton(btLihatTransaksi);
            updateHeader("Lihat Transaksi", "M14 2H6c-1.1 0-1.99.9-1.99 2L4 20c0 1.1.89 2 1.99 2H18c1.1 0 2-.9 2-2V8l-6-6zm2 16H8v-2h8v2zm0-4H8v-2h8v2zm-3-5V3.5L18.5 9H13z");
            updateHeaderTitle("Lihat Transaksi");
        } else {
            chevronLihatTransaksi.setRotate(0);
        }
    }

    @FXML
    void OnActionTransaksiPenjualan(ActionEvent event) {
        setActiveButton(btLihatTransaksi);
        loadContent("/css/LayoutSistemFotocopy/DataPenjualan.fxml");
        updateHeader("Transaksi Penjualan", "M14 2H6c-1.1 0-1.99.9-1.99 2L4 20c0 1.1.89 2 1.99 2H18c1.1 0 2-.9 2-2V8l-6-6zm2 16H8v-2h8v2zm0-4H8v-2h8v2zm-3-5V3.5L18.5 9H13z");
        updateHeaderTitle("Lihat Transaksi - Penjualan");
    }

    @FXML
    void OnActionTransaksiPembelianStock(ActionEvent event) {
        setActiveButton(btLihatTransaksi);
        loadContent("/css/LayoutSistemFotocopy/DataPembelianStock.fxml");
        updateHeader("Transaksi Pembelian Stock", "M14 2H6c-1.1 0-1.99.9-1.99 2L4 20c0 1.1.89 2 1.99 2H18c1.1 0 2-.9 2-2V8l-6-6zm2 16H8v-2h8v2zm0-4H8v-2h8v2zm-3-5V3.5L18.5 9H13z");
        updateHeaderTitle("Lihat Transaksi - Pembelian Stock");
    }

    @FXML
    void OnMouseKlikNavigasi(MouseEvent event) {
        System.out.println("Navigasi diklik");
    }

    @FXML
    void OnMouseKlikPesan(MouseEvent event) {
        System.out.println("Pesan diklik");
    }

    private void setActiveButton(Button button) {
        resetButtonStyles();

        if (button != null) {
            button.getStyleClass().remove("nav-btn");
            button.getStyleClass().add("nav-btn-active");
            activeButton = button;
        }
    }

    private void resetButtonStyles() {
        resetButtonStyle(btDashboard);
        resetButtonStyle(btKelolaData);
        resetButtonStyle(btPembelianStock);
        resetButtonStyle(btMaintenanceMesin);
        resetButtonStyle(btLihatTransaksi);

        if (submenuKelolaData != null) {
            for (Node node : submenuKelolaData.getChildren()) {
                if (node instanceof Button) {
                    Button btn = (Button) node;
                    btn.getStyleClass().remove("submenu-btn-active");
                    if (!btn.getStyleClass().contains("submenu-btn")) {
                        btn.getStyleClass().add("submenu-btn");
                    }
                }
            }
        }

        if (submenuLihatTransaksi != null) {
            for (Node node : submenuLihatTransaksi.getChildren()) {
                if (node instanceof Button) {
                    Button btn = (Button) node;
                    btn.getStyleClass().remove("submenu-btn-active");
                    if (!btn.getStyleClass().contains("submenu-btn")) {
                        btn.getStyleClass().add("submenu-btn");
                    }
                }
            }
        }
    }

    private void resetButtonStyle(Button button) {
        if (button != null) {
            button.getStyleClass().remove("nav-btn-active");
            if (!button.getStyleClass().contains("nav-btn")) {
                button.getStyleClass().add("nav-btn");
            }
        }
    }

    private void updateHeader(String title, String iconContent) {
        if (lblDashboardText != null) {
            lblDashboardText.setText(title);
        }

        if (iconDashboard != null && iconContent != null) {
            iconDashboard.setContent(iconContent);
            iconDashboard.setStyle("-fx-fill: #004596;");
        }
    }

    private void updateHeaderTitle(String title) {
        if (headerTitle != null) {
            headerTitle.setText(title);
        }
    }

    // =========================================================
    // SET NAMA USER DARI SESSION
    // =========================================================
    private void setNamaUser() {
        UserSession session = UserSession.getInstance();
        String nama = session.getNamaPegawai();  // ← AMBIL NAMA LENGKAP

        if (lblNamaUser != null) {
            if (nama != null && !nama.isEmpty()) {
                lblNamaUser.setText(nama);
                System.out.println("✅ Nama user: " + nama);
            } else {
                // FALLBACK: ambil dari database
                String id = session.getIdPegawai();
                if (id != null && !id.isEmpty()) {
                    String namaDb = getNamaPegawai(id);
                    if (namaDb != null && !namaDb.isEmpty()) {
                        lblNamaUser.setText(namaDb);
                    } else {
                        lblNamaUser.setText("Guest");
                    }
                } else {
                    lblNamaUser.setText("Guest");
                }
            }
        }
    }

    private String getNamaPegawai(String idPegawai) {
        String nama = "";
        String query = "SELECT Nama_Pegawai FROM Pegawai WHERE ID_Pegawai = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setString(1, idPegawai);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    nama = rs.getString("Nama_Pegawai");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return nama;
    }

    private void loadContent(String fxmlPath) {
        try {
            URL resource = getClass().getResource(fxmlPath);

            if (resource == null && fxmlPath.startsWith("/css/")) {
                String alternativePath = fxmlPath.replace("/css/", "/");
                resource = getClass().getResource(alternativePath);
                System.out.println("Mencoba path alternatif: " + alternativePath);
            }

            if (resource == null) {
                String fileName = fxmlPath.substring(fxmlPath.lastIndexOf("/") + 1);
                File file = new File("resources/css/LayoutSistemFotocopy/" + fileName);
                if (file.exists()) {
                    resource = file.toURI().toURL();
                    System.out.println("File ditemukan di: " + file.getAbsolutePath());
                }
            }

            if (resource == null) {
                StringBuilder availableFiles = new StringBuilder();
                File resourcesDir = new File("resources/css/LayoutSistemFotocopy");
                if (resourcesDir.exists() && resourcesDir.isDirectory()) {
                    File[] files = resourcesDir.listFiles();
                    if (files != null) {
                        for (File f : files) {
                            if (f.getName().endsWith(".fxml")) {
                                availableFiles.append("\n- ").append(f.getName());
                            }
                        }
                    }
                }

                throw new IOException("File FXML tidak ditemukan: " + fxmlPath +
                        "\nFile yang tersedia:" + availableFiles);
            }

            Parent content = FXMLLoader.load(resource);
            contentArea.getChildren().clear();
            contentArea.getChildren().add(content);

            System.out.println("Berhasil memuat: " + resource);

        } catch (IOException e) {
            e.printStackTrace();
            showError("Gagal memuat: " + fxmlPath + "\n\nError: " + e.getMessage());
        }
    }

    private void showError(String message) {
        Label errorLabel = new Label(message);
        errorLabel.setStyle("-fx-text-fill: red; -fx-font-size: 14px; -fx-padding: 20px; -fx-wrap-text: true;");
        contentArea.getChildren().clear();
        contentArea.getChildren().add(errorLabel);
    }

    @FXML
    public void initialize() {
        System.out.println("=== INITIALIZE MenuUtama ===");
        System.out.println("Working Directory: " + System.getProperty("user.dir"));

        // SET NAMA USER
        setNamaUser();

        submenuKelolaData.setVisible(false);
        submenuKelolaData.setManaged(false);

        submenuLihatTransaksi.setVisible(false);
        submenuLihatTransaksi.setManaged(false);

        setActiveButton(btDashboard);
        updateHeader("Dashboard", ICON_DASHBOARD);
        updateHeaderTitle("Dashboard Admin");

        try {
            loadContent("/css/LayoutSistemFotocopy/DashboardContent.fxml");
        } catch (Exception e) {
            System.err.println("Gagal memuat dashboard default: " + e.getMessage());
            showError("Dashboard tidak tersedia\nSilakan periksa file DashboardContent.fxml");
        }
    }
}