package com.example.simactivation;

import jakarta.validation.Valid;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class ActivationController {
    private final ActivationService service;
    private final ActivationRepository repository;

    public ActivationController(ActivationService service, ActivationRepository repository) {
        this.service = service;
        this.repository = repository;
    }

    @GetMapping("/activations")
    public List<ActivationRecord> search(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) ActivationStatus status) {
        return service.search(query, status);
    }

    @GetMapping("/activations/{id}")
    public ActivationRecord get(@PathVariable long id) {
        return service.get(id);
    }

    @PostMapping("/activations")
    @ResponseStatus(HttpStatus.CREATED)
    public ActivationRecord create(@Valid @RequestBody ActivationRequest input) {
        return service.create(new ActivationRecord(input.customerName(), input.msisdn(), input.iccid()));
    }

    @PutMapping("/activations/{id}")
    public ActivationRecord update(
            @PathVariable long id,
            @Valid @RequestBody ActivationRequest input,
            @RequestHeader(value = "X-Actor-Role", required = false) String role) {
        return service.update(id, input, role);
    }

    @PostMapping("/activations/{id}/status")
    public ActivationRecord transition(
            @PathVariable long id,
            @Valid @RequestBody StatusChange input,
            @RequestHeader(value = "X-Actor-Role", required = false) String role) {
        return service.transition(id, input.status(), role);
    }

    @GetMapping("/dashboard")
    public Map<String, Long> dashboard() {
        Map<ActivationStatus, Long> statusCounts = new EnumMap<>(ActivationStatus.class);
        for (ActivationStatus status : ActivationStatus.values()) {
            statusCounts.put(status, 0L);
        }
        repository.findAll().forEach(record -> statusCounts.merge(record.getStatus(), 1L, Long::sum));
        Map<String, Long> counts = new LinkedHashMap<>();
        statusCounts.forEach((status, count) -> counts.put(status.name(), count));
        return counts;
    }

    public record StatusChange(@jakarta.validation.constraints.NotNull ActivationStatus status) {}
}
