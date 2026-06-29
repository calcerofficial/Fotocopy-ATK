package SistemFotocopy;

import javafx.fxml.FXML;
import javafx.scene.chart.BarChart;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;

public class Dashboard {

    @FXML
    private Label HasilKaryawanAktif;

    @FXML
    private Label HasilPendapat;

    @FXML
    private Label HasilPengeluaran;

    @FXML
    private Label HasilSaldo;

    @FXML
    private Label HasilStock;

    @FXML
    private Label PrestaseKeuntungan;

    @FXML
    private Label TotalOmsetBulan;

    @FXML
    private TableColumn<?, ?> colMiniBarang;

    @FXML
    private TableColumn<?, ?> colMiniStock;

    @FXML
    private TableView<?> miniStockTable;

    @FXML
    private BarChart<?, ?> salesBarChart;

}
