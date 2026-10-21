package com.example.simactivation;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ActivationRequest(
        @NotBlank @Size(max = 100) String customerName,
        @NotBlank @Pattern(regexp = "[0-9]{10,15}", message = "must contain 10 to 15 digits") String msisdn,
        @NotBlank @Pattern(regexp = "[0-9]{18,22}", message = "must contain 18 to 22 digits") String iccid) {
}
