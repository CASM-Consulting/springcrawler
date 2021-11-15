package com.casm.acled.dao.sql;

import com.casm.acled.dao.util.SqlBinder;

public class Join implements SqlFragment {

    private final String table;
    private final String toTable;
    private final String toOn;
    private final String joinOn;
    private final String type;


    public Join(String type, String table, String toTable, String toOn, String joinOn) {
        this.type = type;
        this.table = table;
        this.toTable = toTable;
        this.toOn = toOn;
        this.joinOn = joinOn;
    }

    @Override
    public String sql() {

        String sql = SqlBinder.sql("${type} JOIN ${table} ON ${toTable}.${toOn} = ${table}.${joinOn}")
        .bind("type", type)
        .bind("table", table)
        .bind("toTable", toTable)
        .bind("toOn", toOn)
        .bind("joinOn", joinOn)
        .bind();

        return sql;
    }

    public static Join left(String table, String toTable, String toOn, String joinOn) {
        return new Join("LEFT", table, toTable, toOn, joinOn);
    }

}
