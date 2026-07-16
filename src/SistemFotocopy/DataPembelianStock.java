package SistemFotocopy;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;

public class DataPembelianStock {

    @FXML
    private Button btnNext;

    @FXML
    private Button btnPrev;

    @FXML
    private TableColumn<?, ?> colIdPembelian;

    @FXML
    private TableColumn<?, ?> colMetode;

    @FXML
    private TableColumn<?, ?> colPegawai;

    @FXML
    private TableColumn<?, ?> colStatus;

    @FXML
    private TableColumn<?, ?> colSupplier;

    @FXML
    private TableColumn<?, ?> colTanggal;

    @FXML
    private TableColumn<?, ?> colTotal;

    @FXML
    private Label lblBelumLunas;

    @FXML
    private Label lblInfoData;

    @FXML
    private Label lblLunas;

    @FXML
    private Label lblPageInfo;

    @FXML
    private Label lblTotalTransaksi;

    @FXML
    private TableView<?> tablePembelian;

    @FXML
    private TextField txtCari;

}
