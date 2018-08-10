package org.jetscript.tools;

import java.sql.Connection;
import java.sql.DriverManager;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Map;

import javax.script.ScriptException;

import org.apache.commons.dbutils.QueryRunner;
import org.apache.commons.dbutils.handlers.MapHandler;
import org.apache.commons.dbutils.handlers.MapListHandler;

@SuppressWarnings("unused")
public class DbClient {

	private String driver;
    private String url;
    private String user;
    private String pwd;
    private Connection conn;

    private DbClient(String driver, String url, String user, String pwd) throws Exception {
        Class.forName(driver);
        this.conn = DriverManager.getConnection(url, user, pwd);
    }

    public List<Map<String, Object>> query (String sql, Object... params) throws ScriptException {
        QueryRunner query = new QueryRunner();
        try {
            return query.query(conn, sql, new MapListHandler(), params);
        } catch (Exception e) {
            throw new ScriptException(e.getMessage());
        }
    }

    public Map<String,Object> insert(String sql, Object... params) throws ScriptException {
        QueryRunner query = new QueryRunner();
        try {
            return query.insert(conn, sql, new MapHandler(), params);
        } catch (Exception e) {
            throw new ScriptException(e.getMessage());
        }
    }

    public Integer update(String sql, Object... params) throws ScriptException {
        try {
            QueryRunner query = new QueryRunner();
            return query.update(conn, sql, new MapHandler(), params);
        }
        catch (Exception e) {
            throw new ScriptException(e.getMessage());
        }
    }

    public void close() {
        if (conn == null) return;
        try {
            conn.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static class Factory {

        public Factory() { }

        // a little bit of wonkiness needed right now because of class loader issues in OSGi and Nashorn land
        public DbClient connect(String driver,String url,String user, String pwd) throws ScriptException {
            try {
                return new DbClient(driver, url, user, pwd);
            } catch (Exception e) {
                throw new ScriptException(e.getMessage());
            }
        }
    }

}
