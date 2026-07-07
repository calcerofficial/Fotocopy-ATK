package SistemFotocopy;

import javafx.fxml.FXML;
import javafx.scene.chart.BarChart;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

public class DashboardKaryawan {

    @FXML
    private BarChart<?, ?> ChartPenjualan;

    @FXML
    private Label Pendapatan;

    @FXML
    private Label Pengeluaran;

    @FXML
    private Label Saldo;

    @FXML
    private VBox StatusMesin;

}
