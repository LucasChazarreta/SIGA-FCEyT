/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package ar.edu.unse.siga.persistence;

/**
 *
 * @author Luca
 */
import com.mysql.cj.jdbc.MysqlDataSource;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;
import javax.sql.DataSource;

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
            // Registra el driver de MySQL
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (IOException | ClassNotFoundException e) {
            throw new RuntimeException("Error inicializando DataSourceFactory", e);
        }
    }

    private DataSourceFactory() {
    }

    public static Connection getConnection() throws SQLException {
        String url = PROPS.getProperty("db.url");
        String user = PROPS.getProperty("db.user");
        String pass = PROPS.getProperty("db.password");
        return DriverManager.getConnection(url, user, pass);
    }

    // Para tests que no abren conexión todavía
    public static String getConfig(String key) {
        return PROPS.getProperty(key);
    }

    public static DataSource createDataSource() {
        if (dataSource == null) {
            MysqlDataSource ds = new MysqlDataSource();
            ds.setURL("jdbc:mysql://localhost:3306/siga"); // ajusta con tu DB
            ds.setUser("root");
            ds.setPassword("root"); // cambia según tu config
            dataSource = ds;
        }
        return dataSource;
    }
}
