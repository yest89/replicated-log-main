package ua.edu.ucu.open.repo.impl;

import org.springframework.stereotype.Repository;
import ua.edu.ucu.open.repo.LogRepository;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Repository
public class LogRepositoryImpl implements LogRepository {

    private final List<String> storage = new ArrayList<>();

    @Override
    public void add(String log) {
        storage.add(log);
    }

    @Override
    public List<String> getAll() {
        return Collections.unmodifiableList(storage);
    }
}
