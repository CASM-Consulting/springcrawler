package com.casm.acled.crawler.management.checks;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by Andrew D. Robertson on 02/11/2020.
 */
public class CheckList extends ArrayList<Check> {

    public CheckList(){}

    public CheckList(@NotNull Collection<? extends Check> c) {
        super(c);
    }

    /**
     * Return true if all check are passing.
     */
    public boolean isPass(){
        return stream()
                .allMatch(Check::isPass);
    }

    /**
     * Get an array of values for the checks for summary in a table.
     * If name is nonnull then it is prepended as first column.
     * If addPass is true, then add a final column detailing whether
     * all checks pass.
     */
    public String[] toTableRow(String name, boolean addPass){
        List<String> row = stream()
                    .map(Check::toTableString)
                    .collect(Collectors.toList());

        if (name != null){
            row.add(0, name);
        }

        if (addPass){
            row.add(String.valueOf(isPass()));
        }

        return row.toArray(new String[0]);
    }

    public static CheckList of(Check... checks){
        return new CheckList(Arrays.asList(checks));
    }
}
