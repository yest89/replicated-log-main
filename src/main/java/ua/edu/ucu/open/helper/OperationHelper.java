package ua.edu.ucu.open.helper;

import lombok.extern.slf4j.Slf4j;
import ua.edu.ucu.open.exception.HealthCheckException;
import ua.edu.ucu.open.exception.InconsistentException;

import java.util.concurrent.ExecutionException;

import static ua.edu.ucu.open.service.impl.HealthCheckJob.isHealthChecks;

@Slf4j
public class OperationHelper {
    public static void doWithRetry(int maxAttempts, Operation operation) {
        for (int count = 0; count < maxAttempts; count++) {
            try {
                Thread.sleep(100);
                operation.act();
                count = maxAttempts - 1; //don't retry
            } catch (HealthCheckException e) {
                isHealthChecks.set(Integer.parseInt(e.getMessage()), false);
                log.error("Slave id {} is down", Integer.parseInt(e.getMessage()));
                operation.handleException(e);
            } catch (Exception e) {
                operation.handleException(e);
            }
        }
    }

    public static abstract class Operation {
        abstract public void act() throws ExecutionException, InterruptedException, InconsistentException, HealthCheckException;

        public void handleException(Exception cause) {
            //default impl: do nothing, log the exception, etc.
        }
    }
}