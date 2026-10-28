package koneksi;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

/**
 * Membuka koneksi JDBC ke MySQL/MariaDB.
 * Alamat server dan akun dibaca dari config/database.properties,
 * sehingga password tidak ditulis di dalam source code.
 */
public final class KoneksiDB {

    public static final String DRIVER = "com.mysql.cj.jdbc.Driver";
    public static final Path FILE_KONFIGURASI =
            Paths.get("config", "database.properties");

    private static final String[] KUNCI_WAJIB = {
        "db.host", "db.port", "db.name", "db.user"
    };

    private KoneksiDB() {
    }

    public static Properties bacaKonfigurasi() throws SQLException {
        Path lokasi = FILE_KONFIGURASI.toAbsolutePath();
        Properties konfigurasi = new Properties();

        try (InputStream input = Files.newInputStream(lokasi)) {
            konfigurasi.load(input);
        } catch (IOException e) {
            throw new SQLException(
                    "File konfigurasi tidak ditemukan atau tidak dapat dibaca: "
                    + lokasi + "\nPastikan folder 'config' dan file 'database.properties' sudah ada di dalam folder utama project kamu!", e);
        }

        for (String kunci : KUNCI_WAJIB) {
            if (konfigurasi.getProperty(kunci, "").trim().isEmpty()) {
                throw new SQLException(
                        "Nilai " + kunci + " wajib diisi pada file " + lokasi);
            }
        }

        String port = konfigurasi.getProperty("db.port").trim();
        if (!port.matches("\\d{1,5}")) {
            throw new SQLException(
                    "Nilai db.port harus berupa angka, contoh 3306. Isi saat ini: "
                    + port);
        }
        int nomorPort = Integer.parseInt(port);
        if (nomorPort < 1 || nomorPort > 65535) {
            throw new SQLException("Nilai db.port harus antara 1 dan 65535.");
        }
        if ("ISI_PASSWORD_LOKAL".equals(konfigurasi.getProperty("db.password", ""))) {
            throw new SQLException("Ganti ISI_PASSWORD_LOKAL dengan password MySQL kamu "
                    + "pada config/database.properties.");
        }
        return konfigurasi;
    }

    public static String buatUrl(Properties konfigurasi) {
        return "jdbc:mysql://"
                + konfigurasi.getProperty("db.host").trim() + ":"
                + konfigurasi.getProperty("db.port").trim() + "/"
                + konfigurasi.getProperty("db.name").trim()
                + "?useUnicode=true&useJDBCCompliantTimezoneShift=true&useLegacyDatetimeCode=false&serverTimezone=UTC&connectTimeout=5000&socketTimeout=10000";
    }

    public static void muatDriver() throws SQLException {
        try {
            Class.forName(DRIVER);
        } catch (ClassNotFoundException e) {
            throw new SQLException(
                    "Driver JDBC " + DRIVER + " tidak ditemukan. "
                    + "Pastikan mysql-connector-j-8.0.33.jar sudah ditambahkan ke Libraries proyek.",
                    e);
        }
    }

    public static Connection getConnection() throws SQLException {
        Properties konfigurasi = bacaKonfigurasi();
        muatDriver();

        return DriverManager.getConnection(
                buatUrl(konfigurasi),
                konfigurasi.getProperty("db.user").trim(),
                konfigurasi.getProperty("db.password", ""));
    }
}