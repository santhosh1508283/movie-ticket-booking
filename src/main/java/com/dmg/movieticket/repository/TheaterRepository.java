package com.dmg.movieticket.repository;

import com.dmg.movieticket.entity.Theater;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TheaterRepository extends JpaRepository<Theater, Long> {

    List<Theater> findByCityId(Long cityId);

    List<Theater> findByNameContainingIgnoreCase(String name);

    boolean existsByNameIgnoreCaseAndCityId(String name, Long cityId);
}