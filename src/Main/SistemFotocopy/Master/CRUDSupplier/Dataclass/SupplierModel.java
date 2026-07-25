package SistemFotocopy.Master.CRUDSupplier.Dataclass;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class SupplierModel {
    private final StringProperty id;
    private final StringProperty nama;
    private final StringProperty alamat;
    private final StringProperty telepon;
    private final StringProperty email;
    private final StringProperty status;

    public SupplierModel(String id, String nama, String alamat, String telepon, String email, String status) {
        this.id = new SimpleStringProperty(id);
        this.nama = new SimpleStringProperty(nama);
        this.alamat = new SimpleStringProperty(alamat);
        this.telepon = new SimpleStringProperty(telepon);
        this.email = new SimpleStringProperty(email);
        this.status = new SimpleStringProperty(status);
    }

    public StringProperty idProperty() { return id; }
    public StringProperty namaProperty() { return nama; }
    public StringProperty alamatProperty() { return alamat; }
    public StringProperty teleponProperty() { return telepon; }
    public StringProperty emailProperty() { return email; }
    public StringProperty statusProperty() { return status; }

    public String getId() { return id.get(); }
    public String getNama() { return nama.get(); }
    public String getAlamat() { return alamat.get(); }
    public String getTelepon() { return telepon.get(); }
    public String getEmail() { return email.get(); }
    public String getStatus() { return status.get(); }

    public void setId(String id) { this.id.set(id); }
    public void setNama(String nama) { this.nama.set(nama); }
    public void setAlamat(String alamat) { this.alamat.set(alamat); }
    public void setTelepon(String telepon) { this.telepon.set(telepon); }
    public void setEmail(String email) { this.email.set(email); }
    public void setStatus(String status) { this.status.set(status); }
}