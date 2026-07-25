package SistemFotocopy.Transaksi.TransaksiPenjualan.Transaksi.Dataclass;

public class DataMesinStatus {
    private String idMesin;
    private String namaMesin;
    private String statusMesin;
    private String statusOperasional;

    public DataMesinStatus(String idMesin, String namaMesin, String statusMesin, String statusOperasional) {
        this.idMesin = idMesin;
        this.namaMesin = namaMesin;
        this.statusMesin = statusMesin;
        this.statusOperasional = statusOperasional;
    }

    public String getIdMesin() { return idMesin; }
    public String getNamaMesin() { return namaMesin; }
    public String getStatusMesin() { return statusMesin; }
    public String getStatusOperasional() { return statusOperasional; }

    public void setIdMesin(String idMesin) { this.idMesin = idMesin; }
    public void setNamaMesin(String namaMesin) { this.namaMesin = namaMesin; }
    public void setStatusMesin(String statusMesin) { this.statusMesin = statusMesin; }
    public void setStatusOperasional(String statusOperasional) { this.statusOperasional = statusOperasional; }
}