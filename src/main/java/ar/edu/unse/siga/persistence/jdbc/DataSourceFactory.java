package ar.edu.unse.siga.persistence.jdbc;

import com.mysql.cj.jdbc.MysqlDataSource;

import javax.sql.DataSource;
import java.io.InputStream;
import java.util.Properties;

public final class DataSourceFactory {

    private DataSourceFactory() {}

    public static DataSource createFromProperties() {
        try {
            Properties props = new Properties();
            try (InputStream in = Thread.currentThread()
                    .getContextClassLoader()
                    .getResourceAsStream("application.properties")) {
                if (in == null) {
                    throw new IllegalStateException("No se encontró application.properties en resources");
                }
                props.load(in);
            }
            String url = required(props, "db.url");
            String user = required(props, "db.user");
            String pass = required(props, "db.password");

            MysqlDataSource ds = new MysqlDataSource();
            ds.setURL(url);
            ds.setUser(user);
            ds.setPassword(pass);
            return ds;
        } catch (Exception e) {
            throw new RuntimeException("Error creando DataSource", e);
        }
    }

    private static String required(Properties p, String k) {
        String v = p.getProperty(k);
        if (v == null || v.isBlank()) {
            throw new IllegalStateException("Falta propiedad: " + k);
        }
        return v.trim();
    }
}


