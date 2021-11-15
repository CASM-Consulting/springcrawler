package com.casm.acled.dao.sql;

public class Literal extends Value {

    private final String value;

    public Literal(String value) {
        this.value = value;
    }

    @Override
    public String sql() {
        return value;
    }
}
