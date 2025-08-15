package com.example.jobscraper.repository;

import com.example.jobscraper.entity.Job;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface JobRepository extends JpaRepository<Job, Long> {
  Optional<Job> findByJobPageUrl(String jobPageUrl);
}
