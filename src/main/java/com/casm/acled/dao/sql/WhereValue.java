package com.casm.acled.dao.sql;

import com.casm.acled.dao.util.SqlBinder;
import com.google.common.collect.ImmutableList;

import java.util.List;

public class WhereValue extends Where {

    private final String field;
    private final Object value;
    private final Operator operator;

    protected enum Operator {
        EQ("="),
        NE("!="),
        GT(">"),
        LT("<"),
        GE(">="),
        LE("<=");
        private String op;
        Operator(String op) {
            this.op = op;
        }
    }

    public WhereValue(String field, Object value, Operator operator) {
        this.field = field;
        this.value = value;
        this.operator = operator;
    }

    @Override
    public void whereSql(SqlBinder binder) {

        String op = operator.op;

        binder.append("${a}")
                .append(op)
                .append("?")
                .bind("a", field);
    }

    public String field() {
        return field;
    }

    public Object value() {
        return value;
    }

    @Override
    public List<Object> collectValues() {
        return ImmutableList.of(value);
    }
}
