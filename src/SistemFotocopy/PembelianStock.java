package SistemFotocopy;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;

public class PembelianStock {

    @FXML
    private Button btnBayar;

    @FXML
    private Button btnNextPage;

    @FXML
    private Button btnPage1;

    @FXML
    private Button btnPrevPage;

    @FXML
    private TableColumn<?, ?> colAksi;

    @FXML
    private TableColumn<?, ?> colHarga;

    @FXML
    private TableColumn<?, ?> colMerkBarang;

    @FXML
    private TableColumn<?, ?> colNamaBarang;

    @FXML
    private TableColumn<?, ?> colStok;

    @FXML
    private Label lblInfoData;

    @FXML
    private Label lblTotalHarga;

    @FXML
    private BorderPane rootPane;

    @FXML
    private TableView<?> tblKatalog;

    @FXML
    private TextField txtCari;

    @FXML
    private VBox vboxItems;

    @FXML
    void handleBayar(ActionEvent event) {

    }

}
