package com.casm.acled.dao.sql;

import com.casm.acled.dao.util.SqlBinder;

import java.util.List;

abstract public class Not extends Where {

    protected final Where not;

    protected Not(Where not) {
        this.not = not;
    }

    @Override
    protected void whereSql(SqlBinder binder) {
        binder.append("(NOT ");
        binder.append(not.sql());
        binder.append(")");
    }

    @Override
    public List<Object> collectValues() {
        return not.collectValues();
    }

}
