package com.csci318.recommendationservice.infrastructure;

import com.csci318.recommendationservice.domain.BuildRequest;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BuildRequestRepository extends JpaRepository<BuildRequest, String> {

    List<BuildRequest> findByUserId(String userId);
}
