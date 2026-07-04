package SistemFotocopy;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;

public class DataPegawai {

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
    private Button btnTogglePassword;

    @FXML
    private Button btnUbah;

    @FXML
    private TableColumn<?, ?> colAlamat;

    @FXML
    private TableColumn<?, ?> colEmail;

    @FXML
    private TableColumn<?, ?> colIdPegawai;

    @FXML
    private TableColumn<?, ?> colNamaPegawai;

    @FXML
    private TableColumn<?, ?> colNoTelepon;

    @FXML
    private TableColumn<?, ?> colStatus;

    @FXML
    private TableColumn<?, ?> colUsername;

    @FXML
    private Label lblInfoData;

    @FXML
    private Label lblPegawaiAktif;

    @FXML
    private Label lblPegawaiNonAktif;

    @FXML
    private Label lblTotalPegawai;

    @FXML
    private BorderPane rootPane;

    @FXML
    private TableView<?> tblPegawai;

    @FXML
    private TextArea txtAlamatLengkap;

    @FXML
    private TextField txtCari;

    @FXML
    private TextField txtEmail;

    @FXML
    private TextField txtIdPegawai;

    @FXML
    private TextField txtNamaLengkap;

    @FXML
    private TextField txtNomorTelepon;

    @FXML
    private PasswordField txtPassword;

    @FXML
    private TextField txtPasswordVisible;

    @FXML
    private TextField txtStatus;

    @FXML
    private TextField txtUsername;

    @FXML
    void handleBatal(ActionEvent event) {

    }

    @FXML
    void handleHapusData(ActionEvent event) {

    }

    @FXML
    void handleNextPage(ActionEvent event) {

    }

    @FXML
    void handlePrevPage(ActionEvent event) {

    }

    @FXML
    void handleSimpanData(ActionEvent event) {

    }

    @FXML
    void handleTogglePassword(ActionEvent event) {

    }

    @FXML
    void handleUbahData(ActionEvent event) {

    }

}
