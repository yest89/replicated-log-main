package ua.edu.ucu.open.exception;

public class HealthCheckException extends Exception {
    public HealthCheckException(String errorMessage) {
        super(errorMessage);
    }
}