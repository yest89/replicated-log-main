package ua.edu.ucu.open.controller;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

@Getter
@Setter
public class LogRequest implements Serializable {
    private String log;
    private int writeConcern;
}
