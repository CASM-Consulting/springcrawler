package com.casm.acled.crawler.management.checks;

import java.util.Arrays;
import java.util.function.Supplier;

import static com.casm.acled.crawler.management.checks.CheckStatus.FAIL;
import static com.casm.acled.crawler.management.checks.CheckStatus.PASS;
import static com.casm.acled.crawler.management.checks.CheckStatus.NOT_APPLICABLE;

/**
 * Created by Andrew D. Robertson on 02/11/2020.
 */
public class Check {

    private CheckStatus status;
    private String message;

    public Check(CheckStatus status, String message) {
        this.status = status;
        this.message = message;
    }

    public boolean isPass(){
        switch (status){
            case PASS:
            case NOT_APPLICABLE:
                return true;
            case FAIL:
                return false;
            default:
                throw new RuntimeException("Unrecognised check status");
        }
    }

    public String toTableString(){
        switch (status){
            case PASS:
                return "true";
            case FAIL:
                return message == null? "false" : message;
            case NOT_APPLICABLE:
                return message == null? "N/A" : "N/A (" + message + ")";
            default:
                throw new RuntimeException("Unrecognised check status");
        }
    }

    public CheckStatus status() {
        return status;
    }
    public void status(CheckStatus status) {
        this.status = status;
    }

    public void message(String message){
        this.message = message;
    }
    public String message(){
        return message;
    }

    public static Check success(){
        return new Check(CheckStatus.PASS, null);
    }

    public static Check notApplicable(){
        return notApplicable(null);
    }
    public static Check notApplicable(String message){
        return new Check(NOT_APPLICABLE, message);
    }

    public static Check failed(){
        return failed(null);
    }
    public static Check failed(String message){
        return new Check(FAIL, message);
    }

    /**
     * Generate a Check from a function that returns a CheckStatus.
     * A status can be pass, fail or not applicable.
     */
    public static Check status(Supplier<CheckStatus> check){
        try {
            return new Check(check.get(), null);
        } catch (Exception e){
            return failed(e.getMessage());
        }
    }

    /**
     * Generate a Check based on outcome of a function returning
     * a boolean.
     *
     * Returns a passed check on true, and failed on false or exception.
     * Uses the exception message as failure message.
     */
    public static Check bool(Supplier<Boolean> check){
        try {
            return new Check(check.get()? PASS : FAIL, null);
        } catch (Exception e){
            return failed(e.getMessage());
        }
    }

    /**
     * Check if a list of Checks pass.
     */
    public static boolean allPass(Check... checks){
        return Arrays.stream(checks)
                .allMatch(Check::isPass);
    }

    @Override
    public String toString() {
        return "Check{" +
                "status=" + status +
                ", message='" + message + '\'' +
                '}';
    }
}
