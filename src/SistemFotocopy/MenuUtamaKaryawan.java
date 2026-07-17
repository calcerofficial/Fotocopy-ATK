package SistemFotocopy;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.SVGPath;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

public class MenuUtamaKaryawan implements Initializable {

    @FXML
    private SVGPath IconNavigasi;

    @FXML
    private SVGPath IconPesan;

    @FXML
    private Button btDashboard;

    @FXML
    private Button btPenjualan;

    @FXML
    private Button btHasilPenjualan;

    @FXML
    private Button btDataBarang;

    @FXML
    private Button btMaintenanceMesin;

    @FXML
    private Button btKembali;

    @FXML
    private StackPane contentArea;

    @FXML
    private Label lblDashboardText;

    @FXML
    private SVGPath iconDashboard;

    @FXML
    private Label headerTitle;

    private Button activeButton = null;

    // Icon untuk Dashboard
    private static final String ICON_DASHBOARD = "M3 13h8V3H3v10zm0 8h8v-6H3v6zm10 0h8V11h-8v10zm0-18v6h8V3h-8z";
    // Icon untuk Penjualan
    private static final String ICON_PENJUALAN = "M20 6h-2c0-2.76-2.24-5-5-5S8 3.24 8 6H6c-1.1 0-2 .9-2 2v12c0 1.1.9 2 2 2h14c1.1 0 2-.9 2-2V8c0-1.1-.9-2-2-2zm-7-3c1.66 0 3 1.34 3 3H10c0-1.66 1.34-3 3-3zm0 10c-2.76 0-5-2.24-5-5h2c0 1.66 1.34 3 3 3s3-1.34 3-3h2c0 2.76-2.24 5-5 5z";
    // Icon untuk Hasil Penjualan
    private static final String ICON_HASIL_PENJUALAN = "M19 3H5c-1.1 0-2 .9-2 2v14c0 1.1.9 2 2 2h14c1.1 0 2-.9 2-2V5c0-1.1-.9-2-2-2zM9 17H7v-7h2v7zm4 0h-2V7h2v10zm4 0h-2v-4h2v4z";
    // Icon untuk Data Barang
    private static final String ICON_DATA_BARANG = "M4 6h16v2H4V6zm0 5h16v2H4v-2zm0 5h10v2H4v-2z";
    // Icon untuk Maintenance Mesin
    private static final String ICON_MAINTENANCE = "M19 6h-2c0-2.76-2.24-5-5-5S7 3.24 7 6H5c-1.1 0-2 .9-2 2v12c0 1.1.9 2 2 2h14c1.1 0 2-.9 2-2V8c0-1.1-.9-2-2-2zm-7-3c1.66 0 3 1.34 3 3H9c0-1.66 1.34-3 3-3zm0 10c-2.76 0-5-2.24-5-5h2c0 1.66 1.34 3 3 3s3-1.34 3-3h2c0 2.76-2.24 5-5 5z";

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        System.out.println("=== INITIALIZE MenuUtamaKaryawan ===");
        System.out.println("Working Directory: " + System.getProperty("user.dir"));

        // Set Dashboard sebagai default
        setActiveButton(btDashboard);
        updateHeader("Dashboard", ICON_DASHBOARD);
        updateHeaderTitle("Dashboard Karyawan");

        try {
            loadContent("DashboardKaryawan.fxml");
        } catch (Exception e) {
            System.err.println("Gagal memuat dashboard default: " + e.getMessage());
            showError("Dashboard tidak tersedia\nSilakan periksa file DashboardKaryawan.fxml");
        }
    }

    @FXML
    void OnActionDasbord(ActionEvent event) {
        setActiveButton(btDashboard);
        loadContent("DashboardKaryawan.fxml");
        updateHeader("Dashboard", ICON_DASHBOARD);
        updateHeaderTitle("Dashboard Karyawan");
    }

    @FXML
    void OnActionPenjualan(ActionEvent event) {
        setActiveButton(btPenjualan);
        loadContent("TransaksiPenjualan.fxml");
        updateHeader("Penjualan", ICON_PENJUALAN);
        updateHeaderTitle("Transaksi - Penjualan");
    }

    @FXML
    void OnActionHasilPenjualan(ActionEvent event) {
        setActiveButton(btHasilPenjualan);
        loadContent("DataPenjualan.fxml");
        updateHeader("Hasil Penjualan", ICON_HASIL_PENJUALAN);
        updateHeaderTitle("Hasil Penjualan");
    }

    @FXML
    void OnActionDataBarang(ActionEvent event) {
        setActiveButton(btDataBarang);
        loadContent("LihatBarang.fxml");
        updateHeader("Data Barang", ICON_DATA_BARANG);
        updateHeaderTitle("Data Barang");
    }

    @FXML
    void OnActionMaintenanceMesin(ActionEvent event) {
        setActiveButton(btMaintenanceMesin);
        loadContent("MaintenanceMesin.fxml");
        updateHeader("Maintenance Mesin", ICON_MAINTENANCE);
        updateHeaderTitle("Maintenance Mesin");
    }

    @FXML
    void OnActionKembali(ActionEvent event) {
        try {
            URL resource = getClass().getResource("/css/LayoutSistemFotocopy/MenuLogin.fxml");
            if (resource == null) {
                resource = getClass().getResource("/LayoutSistemFotocopy/MenuLogin.fxml");
            }
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
        resetButtonStyle(btPenjualan);
        resetButtonStyle(btHasilPenjualan);
        resetButtonStyle(btDataBarang);
        resetButtonStyle(btMaintenanceMesin);
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

    /**
     * METHOD LOADCONTENT - Mencari file di multiple lokasi
     */
    private void loadContent(String fxmlName) {
        try {
            URL resource = null;

            // 1. Coba di folder resources/LayoutSistemFotocopy/
            String[] paths = {
                    "/css/LayoutSistemFotocopy/" + fxmlName,
                    "/LayoutSistemFotocopy/" + fxmlName,
                    "LayoutSistemFotocopy/" + fxmlName,
                    fxmlName,
                    "/SistemFotocopy/" + fxmlName,
                    "SistemFotocopy/" + fxmlName
            };

            for (String path : paths) {
                resource = getClass().getResource(path);
                if (resource != null) {
                    System.out.println("File ditemukan di: " + path);
                    break;
                }
            }

            // 2. Jika masih null, coba dengan file sistem
            if (resource == null) {
                String[] dirs = {
                        "resources/LayoutSistemFotocopy/",
                        "src/main/resources/LayoutSistemFotocopy/",
                        "resources/css/LayoutSistemFotocopy/",
                        "src/main/resources/css/LayoutSistemFotocopy/"
                };

                for (String dir : dirs) {
                    File file = new File(dir + fxmlName);
                    if (file.exists()) {
                        resource = file.toURI().toURL();
                        System.out.println("File ditemukan di: " + file.getAbsolutePath());
                        break;
                    }
                }
            }

            if (resource == null) {
                // Tampilkan daftar file yang tersedia
                StringBuilder availableFiles = new StringBuilder();
                String[] dirs = {
                        "resources/LayoutSistemFotocopy/",
                        "src/main/resources/LayoutSistemFotocopy/",
                        "resources/css/LayoutSistemFotocopy/"
                };

                for (String dir : dirs) {
                    File resourcesDir = new File(dir);
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
                }

                throw new IOException("File FXML tidak ditemukan: " + fxmlName +
                        "\nFile yang tersedia:" + availableFiles);
            }

            // Muat file FXML
            FXMLLoader loader = new FXMLLoader(resource);
            Parent content = loader.load();

            // Update UI
            contentArea.getChildren().clear();
            contentArea.getChildren().add(content);

            System.out.println("Berhasil memuat: " + resource);

        } catch (IOException e) {
            e.printStackTrace();
            showError("Gagal memuat: " + fxmlName + "\n\nError: " + e.getMessage());
        }
    }

    private void showError(String message) {
        Label errorLabel = new Label(message);
        errorLabel.setStyle("-fx-text-fill: red; -fx-font-size: 14px; -fx-padding: 20px; -fx-wrap-text: true;");
        contentArea.getChildren().clear();
        contentArea.getChildren().add(errorLabel);
    }
}