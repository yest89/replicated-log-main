package ua.edu.ucu.open.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;

@Getter
@RequiredArgsConstructor
public enum WriteConcern {

    ONLY_FROM_MASTER(1),
    MASTER_AND_ONE_SECONDARY(2),
    MASTER_AND_TWO_SECONDARIES(3);

    private final int writeConcern;

    public static WriteConcern enumFromConcern(int value) {
        return Arrays.stream(WriteConcern.values())
                .filter(type -> type.getWriteConcern() == value)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("There is no such write concern type: " + value));
    }
}
