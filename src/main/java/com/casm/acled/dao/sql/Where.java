package com.casm.acled.dao.sql;

import com.casm.acled.dao.util.SqlBinder;

import java.util.Arrays;
import java.util.List;

abstract public class Where implements SqlFragment {

    protected Where parent;

    protected void setParent(Where parent) {
        this.parent = parent;
    }

    @Override
    public String sql() {
        SqlBinder binder = SqlBinder.sql("");
        if(parent == null) {
            binder.append("WHERE");
        }
        whereSql(binder);
        return binder.bind();
    }

    protected abstract void whereSql(SqlBinder base);

    public abstract List<Object> collectValues();


    public static WhereValue equal(String a, Object b) {
        return new WhereValue(a, b, WhereValue.Operator.EQ);
    }
    public static WhereValue notEqual(String a, Object b) {
        return new WhereValue(a, b, WhereValue.Operator.NE);
    }
    public static WhereValue greater(String a, Object b) {
        return new WhereValue(a, b, WhereValue.Operator.GT);
    }
    public static WhereValue less(String a, Object b) {
        return new WhereValue(a, b, WhereValue.Operator.LT);
    }
    public static WhereValue greaterEqual(String a, Object b) {
        return new WhereValue(a, b, WhereValue.Operator.GE);
    }
    public static WhereValue lessEqual(String a, Object b) {
        return new WhereValue(a, b, WhereValue.Operator.LE);
    }

    public static And and(List<Where> wheres) {
        if(!wheres.isEmpty()) {
            return new And(wheres);
        } else {
            return null;
        }
    }

    public static And and(Where... wheres) {
        return and(Arrays.asList(wheres));
    }

    public static Or or(List<Where> wheres) {
        if(!wheres.isEmpty()) {
            return new Or(wheres);
        } else {
            return null;
        }
    }

    public static Or or(Where... wheres) {
        return or(Arrays.asList(wheres));
    }
}
