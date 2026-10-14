package com.csci318.recommendationservice.infrastructure;

import com.csci318.recommendationservice.domain.PCBuild;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface PCBuildRepository extends JpaRepository<PCBuild, String> {

    Optional<PCBuild> findByRecommendationId(String recommendationId);

    List<PCBuild> findByRequestIdIn(Collection<String> requestIds);
}
