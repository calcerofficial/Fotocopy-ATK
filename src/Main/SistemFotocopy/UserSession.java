package SistemFotocopy;

/**
 * Singleton class untuk menyimpan data session user yang sedang login.
 */
public class UserSession {

    private static UserSession instance;

    private String idPegawai;
    private String username;
    private String namaPegawai;  // ← TAMBAHKAN INI

    private UserSession() {}

    public static UserSession getInstance() {
        if (instance == null) {
            instance = new UserSession();
        }
        return instance;
    }

    public void setSession(String idPegawai, String username) {
        this.idPegawai = idPegawai;
        this.username = username;
        this.namaPegawai = namaPegawai;  // ← SIMPAN NAMA LENGKAP
    }

    public String getIdPegawai() {
        return idPegawai;
    }

    public String getUsername() {
        return username;
    }

    public String getNamaPegawai() {  // ← GETTER NAMA LENGKAP
        return namaPegawai;
    }

    public void clearSession() {
        this.idPegawai = null;
        this.username = null;
        this.namaPegawai = null;  // ← CLEAR JUGA
    }
}