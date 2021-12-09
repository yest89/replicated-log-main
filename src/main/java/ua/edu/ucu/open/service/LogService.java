package ua.edu.ucu.open.service;

import ua.edu.ucu.open.exception.InconsistentException;
import ua.edu.ucu.open.grpc.ReplicatedLogClient;
import ua.edu.ucu.open.model.WriteConcern;

import java.util.List;
import java.util.concurrent.CountDownLatch;

public interface LogService {

    List<String> getAll();

    void add(String log, WriteConcern writeConcern) throws InconsistentException;

    void sendLogMessage(String logMessage, CountDownLatch latch, ReplicatedLogClient slave);
}
