package koneksi;

import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Properties;

/**
 * Program uji koneksi JDBC bertahap (Praktikum 6).
 * Jalankan dengan klik kanan file ini > Run File (Shift+F6).
 */
public class UjiKoneksi {

    public static void main(String[] args) {
        int tahap = 0;
        Connection koneksiTerpakai = null;

        System.out.println("=== UJI KONEKSI JDBC - PRAKTIKUM 6 ===");

        try {
            tahap = 1;
            System.out.println();
            System.out.println("[TAHAP 1] Membaca file konfigurasi");
            Properties konfigurasi = KoneksiDB.bacaKonfigurasi();
            String password = konfigurasi.getProperty("db.password", "");
            System.out.println("  File     : "
                    + KoneksiDB.FILE_KONFIGURASI.toAbsolutePath());
            System.out.println("  URL      : " + KoneksiDB.buatUrl(konfigurasi));
            System.out.println("  User     : "
                    + konfigurasi.getProperty("db.user").trim());
            System.out.println("  Password : "
                    + (password.isEmpty() ? "(KOSONG)" : "(terisi, tidak ditampilkan)"));
            System.out.println("  -> BERHASIL");

            tahap = 2;
            System.out.println();
            System.out.println("[TAHAP 2] Memuat driver JDBC");
            KoneksiDB.muatDriver();
            System.out.println("  Driver   : " + KoneksiDB.DRIVER);
            System.out.println("  -> BERHASIL");

            tahap = 3;
            System.out.println();
            System.out.println("[TAHAP 3] Membuka koneksi ke server database");
            long mulai = System.currentTimeMillis();

            try (Connection con = KoneksiDB.getConnection()) {
                koneksiTerpakai = con;
                long durasi = System.currentTimeMillis() - mulai;
                DatabaseMetaData meta = con.getMetaData();

                System.out.println("  Waktu    : " + durasi + " ms");
                System.out.println("  Server   : " + meta.getDatabaseProductName()
                        + " " + meta.getDatabaseProductVersion());
                System.out.println("  Driver   : " + meta.getDriverName()
                        + " " + meta.getDriverVersion());
                System.out.println("  Database : " + con.getCatalog());
                System.out.println("  Akun     : " + meta.getUserName());
                System.out.println("  -> BERHASIL");

                tahap = 4;
                System.out.println();
                System.out.println("[TAHAP 4] Menjalankan query verifikasi");
                String sql = "SELECT COUNT(*) AS jumlah FROM barang";

                try (PreparedStatement ps = con.prepareStatement(sql);
                        ResultSet rs = ps.executeQuery()) {
                    rs.next();
                    System.out.println("  Query    : " + sql);
                    System.out.println("  Hasil    : " + rs.getInt("jumlah") + " barang");
                }
                System.out.println("  -> BERHASIL");
            }

            tahap = 5;
            System.out.println();
            System.out.println("[TAHAP 5] Menutup koneksi");
            System.out.println("  Koneksi sudah ditutup? " + koneksiTerpakai.isClosed());
            System.out.println("  -> BERHASIL");

            System.out.println();
            System.out.println("=== SEMUA TAHAP BERHASIL ===");

        } catch (SQLException e) {
            laporkanGagal(tahap, e);
        }
    }

    private static void laporkanGagal(int tahap, SQLException e) {
        Throwable akar = e;
        while (akar.getCause() != null) {
            akar = akar.getCause();
        }

        System.out.println("  -> GAGAL");
        System.out.println();
        System.out.println("=== GAGAL PADA TAHAP " + tahap + " ===");
        System.out.println("SQLState       : " + e.getSQLState());
        System.out.println("Kode error     : " + e.getErrorCode());
        System.out.println("Pesan          : " + e.getMessage());
        System.out.println("Penyebab dasar : " + akar.getClass().getSimpleName()
                + " - " + akar.getMessage());
        System.out.println("Saran          : " + beriSaran(tahap, e, akar));
    }

    private static String beriSaran(int tahap, SQLException e, Throwable akar) {
        String state = e.getSQLState() == null ? "" : e.getSQLState();
        int kode = e.getErrorCode();

        if (tahap == 1) {
            return "Periksa folder config, nama file database.properties, dan isinya.";
        }
        if (tahap == 2) {
            return "Tambahkan JAR driver ke Libraries proyek ini.";
        }
        if (akar instanceof UnknownHostException) {
            return "Nama host tidak dikenal. Periksa db.host.";
        }
        if (akar instanceof SocketTimeoutException) {
            return "Server tidak menjawab dalam 5 detik. Periksa db.host, jaringan, atau firewall.";
        }
        if (akar instanceof ConnectException) {
            return "Koneksi ditolak. Pastikan MySQL di XAMPP sudah Start dan db.port benar.";
        }
        if (state.startsWith("28")) {
            return "Username atau password salah, atau akun belum dibuat untuk host ini.";
        }
        if (kode == 1049) {
            return "Database tidak ada. Periksa db.name.";
        }
        if (kode == 1044) {
            return "Akun tidak punya akses ke database ini, atau db.name salah ketik.";
        }
        if (kode == 1146) {
            return "Tabel barang tidak ditemukan. Periksa struktur database.";
        }
        if (kode == 1142) {
            return "Akun tidak punya izin SELECT. Minta dosen memeriksa GRANT.";
        }
        if (state.startsWith("08")) {
            return "Gangguan komunikasi dengan server. Periksa layanan, host, dan port.";
        }
        return "Baca pesan di atas dan catat pada tabel gangguan.";
    }
}
