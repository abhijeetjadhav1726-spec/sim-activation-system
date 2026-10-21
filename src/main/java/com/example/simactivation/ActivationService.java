package com.example.simactivation;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ActivationService {
    private static final Map<ActivationStatus, Map<ActivationStatus, String>> ALLOWED_TRANSITIONS =
            new EnumMap<>(ActivationStatus.class);

    static {
        ALLOWED_TRANSITIONS.put(ActivationStatus.DRAFT, Map.of(ActivationStatus.SUBMITTED, "AGENT"));
        ALLOWED_TRANSITIONS.put(ActivationStatus.SUBMITTED, Map.of(ActivationStatus.IN_REVIEW, "SUPERVISOR"));
        ALLOWED_TRANSITIONS.put(ActivationStatus.IN_REVIEW, Map.of(
                ActivationStatus.ACTIVATED, "ADMIN",
                ActivationStatus.REJECTED, "SUPERVISOR"));
    }

    private final ActivationRepository repository;

    public ActivationService(ActivationRepository repository) {
        this.repository = repository;
    }

    public List<ActivationRecord> search(String query, ActivationStatus status) {
        String term = query == null || query.isBlank() ? null : query.trim();
        return repository.search(term, status);
    }

    public ActivationRecord get(long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Activation record not found"));
    }

    public ActivationRecord create(ActivationRecord record) {
        return repository.save(record);
    }

    public ActivationRecord update(long id, ActivationRequest input, String role) {
        ActivationRecord existing = get(id);
        requireRole(role, "AGENT");
        if (existing.getStatus() != ActivationStatus.DRAFT) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Only draft records can be edited");
        }
        existing.updateDetails(input.customerName(), input.msisdn(), input.iccid());
        return repository.save(existing);
    }

    public ActivationRecord transition(long id, ActivationStatus target, String role) {
        ActivationRecord record = get(id);
        Map<ActivationStatus, String> allowedTargets = ALLOWED_TRANSITIONS.get(record.getStatus());
        if (allowedTargets == null || !allowedTargets.containsKey(target)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Invalid status transition");
        }
        requireRole(role, allowedTargets.get(target));
        record.setStatus(target);
        return repository.save(record);
    }

    private void requireRole(String role, String requiredRole) {
        if (!requiredRole.equals(role)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Role is not allowed to edit this record");
        }
    }
}
