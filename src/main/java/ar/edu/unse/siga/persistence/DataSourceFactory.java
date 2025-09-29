package ar.edu.unse.siga.persistence;

import com.mysql.cj.jdbc.MysqlDataSource;

import javax.sql.DataSource;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;

public final class DataSourceFactory {

    private static final Properties PROPS = new Properties();
    private static DataSource dataSource;

    static {
        try (InputStream in = DataSourceFactory.class.getClassLoader()
                .getResourceAsStream("application.properties")) {
            if (in == null) {
                throw new IllegalStateException("No se encontró application.properties en src/main/resources");
            }
            PROPS.load(in);
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (IOException | ClassNotFoundException e) {
            throw new RuntimeException("Error inicializando DataSourceFactory", e);
        }
    }

    private DataSourceFactory() {}

    /** DataSource único (usa application.properties) */
    public static DataSource createDataSource() {
        if (dataSource == null) {
            String url  = required("db.url");
            String user = required("db.user");
            String pass = required("db.password");

            MysqlDataSource ds = new MysqlDataSource();
            ds.setURL(url);
            ds.setUser(user);
            ds.setPassword(pass);
            dataSource = ds;
        }
        return dataSource;
    }

    /** Atajo si algún código quiere una Connection directamente */
    public static Connection getConnection() throws SQLException {
        return createDataSource().getConnection();
    }

    /** Para tests utilitarios */
    public static String getConfig(String key) {
        return PROPS.getProperty(key);
    }

    private static String required(String key) {
        String v = PROPS.getProperty(key);
        if (v == null || v.isBlank()) {
            throw new IllegalStateException("Falta propiedad: " + key);
        }
        return v.trim();
    }
}
