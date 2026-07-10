package SistemFotocopy;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.SVGPath;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;

public class MenuUtamaKaryawan {

    @FXML
    private SVGPath IconNavigasi;

    @FXML
    private SVGPath IconPesan;

    @FXML
    private Button btDashboard;

    @FXML
    private Button btDataBarang;

    @FXML
    private Button btDetailPenjualan;

    @FXML
    private Button btKembali;

    @FXML
    private Button btPenjualan;

    @FXML
    private Button btStatusMesin;

    @FXML
    private StackPane contentArea;

    @FXML
    void OnActionDasbord(ActionEvent event) {

    }

    @FXML
    void OnActionDataBarang(ActionEvent event) {

    }

    @FXML
    void OnActionDetailPenjualan(ActionEvent event) {

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
            stage.show();

        } catch (IOException e) {
            e.printStackTrace();
            showError("Gagal kembali ke halaman login: " + e.getMessage());
        }
    }

    @FXML
    void OnActionPenjualan(ActionEvent event) {

    }

    @FXML
    void OnActionStatusMesin(ActionEvent event) {

    }

    @FXML
    void OnMouseKlikNavigasi(MouseEvent event) {

    }

    @FXML
    void OnMouseKlikPesan(MouseEvent event) {

    }

    private void showError(String message) {
        Label errorLabel = new Label(message);
        errorLabel.setStyle("-fx-text-fill: red; -fx-font-size: 14px; -fx-padding: 20px;");
        contentArea.getChildren().clear();
        contentArea.getChildren().add(errorLabel);
    }

}
