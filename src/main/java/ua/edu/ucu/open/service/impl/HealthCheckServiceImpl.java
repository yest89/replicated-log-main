package ua.edu.ucu.open.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;
import ua.edu.ucu.open.model.HealthCheckStatus;
import ua.edu.ucu.open.service.HealthCheckService;

import java.util.ArrayList;
import java.util.List;

@RestController
@Slf4j
public class HealthCheckServiceImpl implements HealthCheckService {

    private RestTemplate restTemplate;
    private final static String URI_FOR_FIRST_SLAVE = "http://host.docker.internal:8091/api/v1/healthcheck";
    private final static String URI_FOR_SECOND_SLAVE = "http://host.docker.internal:8092/api/v1/healthcheck";

    private static final List<String> URIS = new ArrayList<>();

    @Autowired
    public HealthCheckServiceImpl(RestTemplateBuilder builder) {
        this.restTemplate = builder.build();
        URIS.add(URI_FOR_FIRST_SLAVE);
        URIS.add(URI_FOR_SECOND_SLAVE);
    }

    @Override
    public boolean healthCheck(int id) {
        String status = restTemplate.getForObject(URIS.get(id), String.class);
        return HealthCheckStatus.Healthy.name().equals(status);
    }
}
