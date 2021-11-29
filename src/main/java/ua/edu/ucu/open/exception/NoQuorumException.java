package ua.edu.ucu.open.exception;

public class NoQuorumException extends Exception {
    public NoQuorumException(String errorMessage) {
        super(errorMessage);
    }
}
