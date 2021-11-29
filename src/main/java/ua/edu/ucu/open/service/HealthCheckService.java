package ua.edu.ucu.open.service;

public interface HealthCheckService {
    boolean healthCheckForFirstSlave();
    boolean healthCheckForSecondSlave();
}
