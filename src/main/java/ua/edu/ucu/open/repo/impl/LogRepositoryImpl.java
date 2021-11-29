package ua.edu.ucu.open.repo.impl;

import org.springframework.stereotype.Repository;
import ua.edu.ucu.open.repo.LogRepository;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentSkipListMap;

@Repository
public class LogRepositoryImpl implements LogRepository {

    private final Map<Integer, String> storage = new ConcurrentSkipListMap<>();

    @Override
    public void add(String log, int ordinal) {
        storage.putIfAbsent(ordinal, log);
    }

    @Override
    public List<String> getAll() {
        List<String> result = new ArrayList<>();
        for (int i = 0; i < storage.size(); i++) {
            if (storage.containsKey(i)) {
                result.add(storage.get(i));
            }
            else {
                break;
            }
        }
        return Collections.unmodifiableList(result);
    }
}
