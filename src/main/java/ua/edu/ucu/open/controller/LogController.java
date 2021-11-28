package ua.edu.ucu.open.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ua.edu.ucu.open.exception.InconsistentException;
import ua.edu.ucu.open.model.WriteConcern;
import ua.edu.ucu.open.service.LogService;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/v1/logs")
@RequiredArgsConstructor
public class LogController {

    private final LogService logService;

    @RequestMapping(
            produces = {"application/json"},
            consumes = {"application/json"},
            method = RequestMethod.GET)
    public ResponseEntity<List<String>> getLogs() {
        return new ResponseEntity<>(logService.getAll(), HttpStatus.OK);
    }

    @RequestMapping(
            produces = {"application/json"},
            method = RequestMethod.POST)
    public ResponseEntity<Void> addLog(@RequestBody LogRequest logRequest) {
        try {
            logService.add(logRequest.getLog(), WriteConcern.enumFromConcern(logRequest.getWriteConcern()));
        } catch (InconsistentException e) {
            return new ResponseEntity<>(HttpStatus.SERVICE_UNAVAILABLE);
        }
        return new ResponseEntity<>(HttpStatus.OK);
    }
}
