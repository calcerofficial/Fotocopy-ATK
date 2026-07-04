package SistemFotocopy;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;

public class DataProduk {

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
    private ComboBox<?> cmbKategoriProduk;

    @FXML
    private TableColumn<?, ?> colHarga;

    @FXML
    private TableColumn<?, ?> colIdBarang;

    @FXML
    private TableColumn<?, ?> colKategoriProduk;

    @FXML
    private TableColumn<?, ?> colMerkProduk;

    @FXML
    private TableColumn<?, ?> colNamaBarang;

    @FXML
    private TableColumn<?, ?> colStatusProduk;

    @FXML
    private TableColumn<?, ?> colStock;

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
    private TableView<?> tblProduk;

    @FXML
    private TextField txtCari;

    @FXML
    private TextField txtHargaBarang;

    @FXML
    private TextField txtIdBarang;

    @FXML
    private TextField txtMerk;

    @FXML
    private TextField txtNamaBarang;

    @FXML
    private TextField txtStatus;

    @FXML
    private TextField txtStockBarang;

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
