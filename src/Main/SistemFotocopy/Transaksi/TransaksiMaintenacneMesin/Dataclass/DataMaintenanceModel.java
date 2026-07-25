package SistemFotocopy.Transaksi.TransaksiMaintenacneMesin.Dataclass;

import java.time.LocalDate;

public class DataMaintenanceModel {
    private String idMaintenance;
    private String idMesin;
    private String idPegawai;
    private String jenisKerusakan;
    private String deskripsi;
    private LocalDate tanggalMaintenance;
    private LocalDate tanggalSelesai;
    private String status;
    private double biaya;
    private String keterangan;
    private String namaPegawai;
    private String aksi;

    public DataMaintenanceModel(String idMaintenance, String idMesin, String idPegawai,
                                String jenisKerusakan, String deskripsi,
                                LocalDate tanggalMaintenance, LocalDate tanggalSelesai,
                                String status, double biaya, String keterangan, String namaPegawai) {
        this.idMaintenance = idMaintenance;
        this.idMesin = idMesin;
        this.idPegawai = idPegawai;
        this.jenisKerusakan = jenisKerusakan;
        this.deskripsi = deskripsi;
        this.tanggalMaintenance = tanggalMaintenance;
        this.tanggalSelesai = tanggalSelesai;
        this.status = status;
        this.biaya = biaya;
        this.keterangan = keterangan;
        this.namaPegawai = namaPegawai;

        if (status != null && !status.equals("selesai")) {
            this.aksi = "Selesai";
        } else {
            this.aksi = "-";
        }
    }

    // === GETTERS ===
    public String getIdMaintenance() { return idMaintenance; }
    public String getIdMesin() { return idMesin; }
    public String getIdPegawai() { return idPegawai; }
    public String getJenisKerusakan() { return jenisKerusakan; }
    public String getDeskripsi() { return deskripsi; }
    public LocalDate getTanggalMaintenance() { return tanggalMaintenance; }
    public LocalDate getTanggalSelesai() { return tanggalSelesai; }
    public String getStatus() { return status; }
    public String getAksi() { return aksi; }
    public double getBiaya() { return biaya; }
    public String getKeterangan() { return keterangan; }
    public String getNamaPegawai() { return namaPegawai; }

    // === SETTERS ===
    public void setIdMaintenance(String idMaintenance) { this.idMaintenance = idMaintenance; }
    public void setIdMesin(String idMesin) { this.idMesin = idMesin; }
    public void setIdPegawai(String idPegawai) { this.idPegawai = idPegawai; }
    public void setJenisKerusakan(String jenisKerusakan) { this.jenisKerusakan = jenisKerusakan; }
    public void setDeskripsi(String deskripsi) { this.deskripsi = deskripsi; }
    public void setTanggalMaintenance(LocalDate tanggalMaintenance) { this.tanggalMaintenance = tanggalMaintenance; }
    public void setTanggalSelesai(LocalDate tanggalSelesai) { this.tanggalSelesai = tanggalSelesai; }
    public void setStatus(String status) { this.status = status; }
    public void setBiaya(double biaya) { this.biaya = biaya; }
    public void setKeterangan(String keterangan) { this.keterangan = keterangan; }
    public void setNamaPegawai(String namaPegawai) { this.namaPegawai = namaPegawai; }
    public void setAksi(String aksi) { this.aksi = aksi; }

    public String getBiayaFormatted() {
        return String.format("Rp %,d", (long) biaya);
    }
}