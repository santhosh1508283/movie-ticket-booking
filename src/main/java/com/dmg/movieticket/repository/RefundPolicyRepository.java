package com.dmg.movieticket.repository;

import com.dmg.movieticket.entity.RefundPolicy;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RefundPolicyRepository extends JpaRepository<RefundPolicy, Long> {

    List<RefundPolicy> findByActiveTrueOrderByHoursBeforeShowDesc();

    boolean existsByHoursBeforeShow(Integer hoursBeforeShow);
}