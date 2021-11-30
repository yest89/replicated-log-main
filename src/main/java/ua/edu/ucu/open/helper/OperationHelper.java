package ua.edu.ucu.open.helper;

import lombok.extern.slf4j.Slf4j;
import ua.edu.ucu.open.exception.InconsistentException;

import java.util.concurrent.ExecutionException;

@Slf4j
public class OperationHelper {
    public static void doWithRetry(int maxAttempts, boolean status, Operation operation) {
        for (int count = 0; count < maxAttempts; count++) {
            try {
                Thread.sleep(100);
                operation.act();
                count = maxAttempts - 1; //don't retry
            } catch (Exception e) {
                status = false;
                operation.handleException(e);
            }
        }
    }

    public static abstract class Operation {
        abstract public void act() throws ExecutionException, InterruptedException, InconsistentException;

        public void handleException(Exception cause) {
            //default impl: do nothing, log the exception, etc.
        }
    }
}