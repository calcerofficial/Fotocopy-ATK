package SistemFotocopy;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;

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
    private TableColumn<?, ?> colAlamat;

    @FXML
    private TableColumn<?, ?> colEmail;

    @FXML
    private TableColumn<?, ?> colIdSupplier;

    @FXML
    private TableColumn<?, ?> colNamaSupplier;

    @FXML
    private TableColumn<?, ?> colNoTelepon;

    @FXML
    private TableColumn<?, ?> colStatus;

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
    private TableView<?> tblSupplier;

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

    @FXML
    private TextField txtStatus;

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
    void handleUbahData(ActionEvent event) {

    }

}
