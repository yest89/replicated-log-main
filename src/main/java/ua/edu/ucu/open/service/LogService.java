package ua.edu.ucu.open.service;

import ua.edu.ucu.open.exception.InconsistentException;
import ua.edu.ucu.open.exception.NoQuorumException;
import ua.edu.ucu.open.model.WriteConcern;

import java.util.List;

public interface LogService {

    List<String> getAll();
    void add(String log, WriteConcern writeConcern) throws InconsistentException, NoQuorumException;
}
