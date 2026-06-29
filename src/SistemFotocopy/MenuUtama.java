package SistemFotocopy;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.SVGPath;

public class MenuUtama implements Initializable {

    @FXML
    private StackPane contentArea;

    @FXML
    private ComboBox<String> bkKelolaData;

    @FXML
    private Button btDashboard;

    @FXML
    private Button btPembelianStock;

    @FXML
    private Button btKembali;

    @FXML
    private SVGPath IconNavigasi;

    @FXML
    private SVGPath IconPesan;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        loadPage("DashboardContent.fxml");
        setButtonActive(btDashboard);
    }

    private void loadPage(String fxmlFile) {
        try {
            URL fxmlLocation = getClass().getResource("/LayoutSistemFotocopy/" + fxmlFile);
            if (fxmlLocation == null) {
                throw new IOException("File FXML tidak ditemukan di folder LayoutSistemFotocopy: " + fxmlFile);
            }
            Parent node = FXMLLoader.load(fxmlLocation);
            contentArea.getChildren().setAll(node);
        } catch (IOException e) {
            System.err.println("Gagal memuat halaman: " + fxmlFile);
            e.printStackTrace();
        }
    }


    private void setButtonActive(Button clickedButton) {
        btDashboard.getStyleClass().removeAll("nav-btn-active");
        if (!btDashboard.getStyleClass().contains("nav-btn")) {
            btDashboard.getStyleClass().add("nav-btn");
        }

        btPembelianStock.getStyleClass().removeAll("nav-btn-active");
        if (!btPembelianStock.getStyleClass().contains("nav-btn")) {
            btPembelianStock.getStyleClass().add("nav-btn");
        }

        if (clickedButton != null) {
            clickedButton.getStyleClass().removeAll("nav-btn");
            clickedButton.getStyleClass().add("nav-btn-active");
        }
    }

    @FXML
    void OnActionDasbord(ActionEvent event) {
        loadPage("DashboardContent.fxml");
        setButtonActive(btDashboard);
    }

    @FXML
    void OnActionPembelianStock(ActionEvent event) {
        loadPage("PembelianStok.fxml");
        setButtonActive(btPembelianStock);
    }

    @FXML
    void OnActionKelolaData(ActionEvent event) {
        String pilihan = bkKelolaData.getValue();
        if (pilihan != null) {
            setButtonActive(null);

            switch (pilihan) {
                case "Data Barang":    loadPage("DataBarang.fxml"); break;
                case "Data Transaksi": loadPage("DataTransaksi.fxml"); break;
                case "Data Karyawan":  loadPage("DataKaryawan.fxml"); break;
            }
        }
    }

    @FXML
    void OnActionKembali(ActionEvent event) {
        try {
            Parent loginRoot = FXMLLoader.load(getClass().getResource("/LayoutSistemFotocopy/MenuLogin.fxml"));
            btKembali.getScene().setRoot(loginRoot);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    void OnMouseKlikNavigasi(MouseEvent event) {
        System.out.println("Sidebar Navigasi klik");
    }

    @FXML
    void OnMouseKlikPesan(MouseEvent event) {
        System.out.println("Icon Pesan klik");
    }
}