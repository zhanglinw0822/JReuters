package com.puxtech.reuters.db;

import java.io.InputStream;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;

import javax.sql.DataSource;

import org.apache.commons.dbcp.BasicDataSourceFactory;

public enum DBConnUitl {
	INSTANCE;
	private InputStream inStream;
	private Properties pro = null;
	private DataSource datasource;

	private DBConnUitl() {
		inStream = Object.class.getResourceAsStream("/dbcp.properties");
		pro = new Properties();
		try {
			pro.load(inStream);
			datasource = BasicDataSourceFactory.createDataSource(pro);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public Connection GetConnection() {
		try {
			return datasource.getConnection();
		} catch (SQLException e) {
			e.printStackTrace();
			throw new RuntimeException("得到数据库连接失败！");
		}
	}

	public void free(ResultSet rs, Statement sta, Connection conn) {
		try {
			if (rs != null) {
				rs.close();
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			try {
				if (sta != null) {
					sta.close();
				}
			} catch (Exception e) {
				e.printStackTrace();
			} finally {
				if (conn != null) {
					try {
						conn.close();
					} catch (SQLException e) {
						e.printStackTrace();
					}
				}
			}
		}
	}
}