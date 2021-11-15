package com.casm.acled.dao.sql;

import com.casm.acled.dao.util.SqlBinder;

import java.util.List;
import java.util.stream.Collectors;

public class And extends Where {

    protected final List<Where> wheres;

    private static final String AND = "AND";

    protected And(List<Where> wheres) {
        this.wheres = wheres;
        this.wheres.forEach(w -> w.setParent(this));
    }


    @Override
    protected void whereSql(SqlBinder binder) {
        binder.append("(");
        for(int i = 0;i < wheres.size(); ++i){
            Where where = wheres.get(i);
            binder.append(where.sql());
            if(i<wheres.size()-1) {
                binder.append(AND);
            }
        }
        binder.append(")");
    }

    @Override
    public List<Object> collectValues() {
        List<Object> values = wheres.stream().flatMap(w->w.collectValues().stream()).collect(Collectors.toList());
        return values;
    }

}
