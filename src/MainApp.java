import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class MainApp extends Application {

    @Override
    public void start(Stage stage) throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/LayoutSistemFotocopy/MenuUtama.fxml"));
        Parent root = loader.load();

        // Mengosongkan angka parameter ukuran agar Scene mengikuti resolusi root FXML/layar
        Scene scene = new Scene(root);

        stage.setTitle("Sistem Fotocopy - Login");
        stage.setScene(scene);
        stage.setFullScreen(true);
        stage.setFullScreenExitHint("");
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}