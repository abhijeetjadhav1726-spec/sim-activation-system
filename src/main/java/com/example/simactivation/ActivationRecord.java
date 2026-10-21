package com.example.simactivation;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.Instant;

@Entity
@Table(name = "activation_records")
public class ActivationRecord {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Size(max = 100)
    @Column(nullable = false, length = 100)
    private String customerName;

    @NotBlank
    @Pattern(regexp = "[0-9]{10,15}", message = "must contain 10 to 15 digits")
    @Column(nullable = false, unique = true, length = 15)
    private String msisdn;

    @NotBlank
    @Pattern(regexp = "[0-9]{18,22}", message = "must contain 18 to 22 digits")
    @Column(nullable = false, unique = true, length = 22)
    private String iccid;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ActivationStatus status = ActivationStatus.DRAFT;

    @Column(nullable = false)
    private Instant createdAt = Instant.now();

    @Column(nullable = false)
    private Instant updatedAt = Instant.now();

    protected ActivationRecord() {
    }

    public ActivationRecord(String customerName, String msisdn, String iccid) {
        this.customerName = customerName;
        this.msisdn = msisdn;
        this.iccid = iccid;
    }

    public void updateDetails(String customerName, String msisdn, String iccid) {
        this.customerName = customerName;
        this.msisdn = msisdn;
        this.iccid = iccid;
        this.updatedAt = Instant.now();
    }

    public void setStatus(ActivationStatus status) {
        this.status = status;
        this.updatedAt = Instant.now();
    }

    public Long getId() { return id; }
    public String getCustomerName() { return customerName; }
    public String getMsisdn() { return msisdn; }
    public String getIccid() { return iccid; }
    public ActivationStatus getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
