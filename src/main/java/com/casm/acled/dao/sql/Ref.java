package com.casm.acled.dao.sql;

public class Ref extends Value {

    private final String name;
    private final String table;

    public Ref(String table, String name) {
        this.table = table;
        this.name = name;
    }

    public String sql() {
        return table == null ? name : table + "." + name;
    }

    public static Ref of(String name ) {
        return new Ref(null, name);
    }

    public static Ref table(String table, String name ) {
        return new Ref(table, name);
    }

}
