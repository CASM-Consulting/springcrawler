package com.casm.acled.dao.sql;

public class Jsonb extends Value {

    private final String name;
    private final String field;
    private final boolean asString;


    public Jsonb(String field) {
        this(field, true);
    }

    public Jsonb(String field, boolean asString) {
        this("data", field, asString);
    }

    public Jsonb(String name, String field, boolean asString) {
        this.name = name;
        this.field = field;
        this.asString = asString;
    }

    @Override
    public String sql() {

        if(asString) {
            return name+"->>'"+field+"'";
        } else {
            return name+"->'"+field+"'";
        }
    }

    public static String ref(String field) {
        return new Jsonb(field).sql();
    }

    public static String string(String field) {
        return new Jsonb(field,true).sql();
    }

    public static String ref(String table, String field) {
        return table + "." + new Jsonb(field).sql();
    }

    public static String string(String table, String field) {
        return table + "." + new Jsonb(field, true).sql();
    }

}
