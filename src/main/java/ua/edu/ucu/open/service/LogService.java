package ua.edu.ucu.open.service;

import ua.edu.ucu.open.exception.InconsistentException;

import java.util.List;

public interface LogService {

    List<String> getAll();
    void add(String log) throws InconsistentException;
}
