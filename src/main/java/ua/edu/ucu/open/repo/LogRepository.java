package ua.edu.ucu.open.repo;

import java.util.List;

public interface LogRepository {

    void add(String log, int ordinal);
    List<String> getAll();
}
