package com.casm.acled.entities.validation;

import com.google.common.collect.ImmutableList;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import java.util.Arrays;
import java.util.List;

public class ValidationMessage {

    public enum Level {
        PASS,
        DEBUG,
        INFO,
        WARN,
        ERROR,
        FATAL
    }

    public final Boolean pass;
    public final Level level;
    public final String title;
    public final String message;
    public final String suggestion;
    public final List<String> causes;

    public ValidationMessage(Boolean pass, Level level, String title, String message, String suggestion, String... causes) {
        this.pass = pass;
        this.level = level;
        this.title = title;
        this.message = message;
        this.suggestion = suggestion;
        this.causes = ImmutableList.copyOf(Arrays.asList(causes));

    }

    public static ValidationMessage of(Boolean pass, Level level, String title, String message, String suggestion, String... causes) {
        return new ValidationMessage(pass, level, title, message, suggestion, causes);
    }

    public static ValidationMessage pass() {
        return of(true, Level.PASS, null, null, null);
    }

    public static ValidationMessage debug(String title, String message, String suggestion, String... causes) {
        return of(true, Level.DEBUG, title, message, suggestion, causes);
    }

    public static ValidationMessage info(String title, String message, String suggestion, String... causes) {
        return of(true, Level.INFO, title, message, suggestion, causes);
    }

    public static ValidationMessage warn(String title, String message, String suggestion, String... causes) {
        return of(true, Level.WARN, title, message, suggestion, causes);
    }

    public static ValidationMessage error(String title, String message, String suggestion, String... causes) {
        return of(true, Level.ERROR, title, message, suggestion, causes);
    }

    public static ValidationMessage fatal(String title, String message, String suggestion, String... causes) {
        return of(true, Level.FATAL, title, message, suggestion, causes);
    }


    @Override
    public String toString()  {
        StringBuilder sb = new StringBuilder();
        sb.append("level").append(" : ").append(level).append(" ");
        sb.append("title").append(" : ").append(title).append(" ");
        sbhttps://github.com/tdlib/td/blob/master/example/java/org/drinkless/tdlib/Client.java.append("suggestion").append(" : ").append(suggestion);
        if(message!=null)sb.append("message").append(" : ").append(message).append(" ");
        if(!causes.isEmpty())sb.append("causes").append(" : ").append(causes).append(" ");

        return sb.toString();
    }
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;

        if (o == null || getClass() != o.getClass()) return false;

        ValidationMessage that = (ValidationMessage) o;

        return new EqualsBuilder()
                .append(pass, that.pass)
                .append(level, that.level)
                .append(title, that.title)
                .append(message, that.message)
                .append(suggestion, that.suggestion)
                .append(causes, that.causes)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37)
                .append(pass)
                .append(level)
                .append(title)
                .append(message)
                .append(suggestion)
                .append(causes)
                .toHashCode();
    }
}


