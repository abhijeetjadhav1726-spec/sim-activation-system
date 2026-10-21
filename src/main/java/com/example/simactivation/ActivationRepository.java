package com.example.simactivation;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ActivationRepository extends JpaRepository<ActivationRecord, Long> {
    List<ActivationRecord> findByStatus(ActivationStatus status);

    @Query("""
            select a from ActivationRecord a
            where (:status is null or a.status = :status)
              and (:query is null
                   or lower(a.customerName) like lower(concat('%', :query, '%'))
                   or a.msisdn like concat('%', :query, '%')
                   or a.iccid like concat('%', :query, '%'))
            """)
    List<ActivationRecord> search(@Param("query") String query, @Param("status") ActivationStatus status);
}
