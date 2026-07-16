package SistemFotocopy;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseEvent;
import javafx.scene.shape.SVGPath;
import javafx.stage.Stage;
import Database.DBConnection;

import java.io.IOException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class MenuLogin {

    @FXML
    private SVGPath IconMata;

    @FXML
    private TextField UserField;

    @FXML
    private Label emailErrorLabel;

    @FXML
    private Button loginButton;

    @FXML
    private Label passwordErrorLabel;

    @FXML
    private PasswordField passwordField;

    @FXML
    private TextField passwordTextField;

    private boolean statusTampilPassword = false;

    private DBConnection dbConnection = new DBConnection();

    @FXML
    public void initialize() {
        IconMata.setOnMouseEntered(event -> {
            IconMata.setStyle("-fx-cursor: hand; -fx-fill: #003D9B;");
        });

        IconMata.setOnMouseExited(event -> {
            if (!statusTampilPassword) {
                IconMata.setStyle("-fx-fill: #718096;");
            } else {
                IconMata.setStyle("-fx-fill: #003D9B;");
            }
        });
    }

    @FXML
    void OnMouseKlikMata(MouseEvent event) {
        togglePasswordVisibility(event);
    }

    @FXML
    void OnactionLogin(ActionEvent event) {
        String username = UserField.getText();
        String password = statusTampilPassword ? passwordTextField.getText() : passwordField.getText();

        emailErrorLabel.setVisible(false);
        passwordErrorLabel.setVisible(false);

        if (username.isEmpty()) {
            showError(emailErrorLabel, "Username wajib diisi!");
            return;
        }

        if (password.isEmpty()) {
            showError(passwordErrorLabel, "Password wajib diisi!");
            return;
        }

        cekLoginKeDatabase(username, password);
    }

    // ===== LOGIN KE DATABASE =====
    private void cekLoginKeDatabase(String username, String password) {
        try {
            // ===== SELECT Status_Pegawai DAN ID_Pegawai =====
            String query = "SELECT Status_Pegawai, ID_Pegawai FROM Pegawai WHERE Username = ? AND Password = ?";

            PreparedStatement preparedStatement = dbConnection.getConnection().prepareStatement(query);
            preparedStatement.setString(1, username);
            preparedStatement.setString(2, password);

            ResultSet hasilQuery = preparedStatement.executeQuery();

            if (hasilQuery.next()) {
                // ===== STATUS DAN ID =====
                String status = hasilQuery.getString("Status_Pegawai");
                String idPegawai = hasilQuery.getString("ID_Pegawai");

                System.out.println("Login Berhasil! Selamat datang, " + username + " (Status: " + status + ", ID: " + idPegawai + ")");

                hasilQuery.close();
                preparedStatement.close();

                if (status.equalsIgnoreCase("aktif")) {
                    if (idPegawai.startsWith("ADM")) {
                        System.out.println("Login sebagai ADMIN!");
                        pindahKeHalaman("/LayoutSistemFotocopy/MenuUtama.fxml", "Dashboard Admin");
                    } else {
                        System.out.println("Login sebagai PEGAWAI!");
                        pindahKeHalaman("/LayoutSistemFotocopy/MenuUtamaKaryawan.fxml", "Dashboard Pegawai");
                    }
                } else {
                    showError(emailErrorLabel, "Akun tidak aktif! Status: " + status);
                }
            } else {
                showError(emailErrorLabel, "Username atau Password salah!");
            }

        } catch (Exception e) {
            e.printStackTrace();
            showError(emailErrorLabel, "Data tidak terdaftar di database!");
        }
    }

    private void pindahKeHalaman(String pathFXML, String judul) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(pathFXML));
            Parent root = loader.load();

            Stage stage = (Stage) loginButton.getScene().getWindow();

            javafx.geometry.Rectangle2D screenBounds = javafx.stage.Screen.getPrimary().getVisualBounds();
            Scene scene = new Scene(root, screenBounds.getWidth(), screenBounds.getHeight());

            stage.setScene(scene);
            stage.setTitle(judul);
            stage.setMaximized(true);
            stage.setResizable(false);
            stage.show();

        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("Gagal membuka halaman: " + pathFXML);
            showError(emailErrorLabel, "Gagal membuka halaman!");
        }
    }

    @FXML
    void togglePasswordVisibility(MouseEvent event) {
        statusTampilPassword = !statusTampilPassword;

        if (statusTampilPassword) {
            passwordTextField.setText(passwordField.getText());
            passwordTextField.setVisible(true);
            passwordTextField.setManaged(true);
            passwordField.setVisible(false);
            passwordField.setManaged(false);
            IconMata.setStyle("-fx-fill: #003D9B;");
        } else {
            passwordField.setText(passwordTextField.getText());
            passwordField.setVisible(true);
            passwordField.setManaged(true);
            passwordTextField.setVisible(false);
            passwordTextField.setManaged(false);
            IconMata.setStyle("-fx-fill: #718096;");
        }
    }

    private void showError(Label label, String message) {
        label.setText(message);
        label.setManaged(true);
        label.setVisible(true);
    }
}