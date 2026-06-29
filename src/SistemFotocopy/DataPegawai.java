package SistemFotocopy;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;

public class DataPegawai {

    @FXML
    private TableColumn<?, ?> Alamat;

    @FXML
    private TableColumn<?, ?> Email;

    @FXML
    private TableColumn<?, ?> IDPegawai;

    @FXML
    private TableColumn<?, ?> NamaPegawai;

    @FXML
    private TableColumn<?, ?> Password;

    @FXML
    private TableColumn<?, ?> Status;

    @FXML
    private TableView<?> TabelDataKaryawan;

    @FXML
    private TableColumn<?, ?> Telepon;

    @FXML
    private TextField TxtCari;

    @FXML
    private TableColumn<?, ?> Username;

    @FXML
    private Button btPegawaiAktif;

    @FXML
    private Button btPegawaiNonAktif;

    @FXML
    private Button btSemuaPegawai;

    @FXML
    private Button btTambahKaryawan;

    @FXML
    void OnActionBtPegawaiAktif(ActionEvent event) {

    }

    @FXML
    void OnActionBtPegawaiNonAktif(ActionEvent event) {

    }

    @FXML
    void OnActionBtTambah(ActionEvent event) {

    }

    @FXML
    void OnActionbtSemuaPegawai(ActionEvent event) {

    }

}
