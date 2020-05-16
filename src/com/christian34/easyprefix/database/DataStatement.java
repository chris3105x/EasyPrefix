package com.christian34.easyprefix.database;

import com.christian34.easyprefix.EasyPrefix;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;

/**
 * EasyPrefix 2020.
 *
 * @author Christian34
 */
public class DataStatement {
    private final Database database;
    private String sql;
    private PreparedStatement preparedStatement;
    private SQLException exception;

    public DataStatement(String sqlQuery) {
        this.sql = sqlQuery;
        this.database = EasyPrefix.getInstance().getSqlDatabase();
        try {
            sql = sql.replace("%p%", database.getTablePrefix());
            this.preparedStatement = database.getConnection().prepareStatement(sql);
        } catch(SQLException ignored) {
        }
    }

    public void setObject(int index, Object value) {
        try {
            if (value == null) {
                this.preparedStatement.setNull(index, Types.VARCHAR);
            } else {
                this.preparedStatement.setObject(index, value);
            }
        } catch(SQLException ex) {
            this.exception = ex;
        }
    }

    public boolean execute() {
        try {
            preparedStatement.executeUpdate();
            return true;
        } catch(SQLException ex) {
            this.exception = ex;
            return false;
        }
    }

    public SQLException getException() {
        return this.exception;
    }


}