package com.example.jobscraper.repository;

import com.example.jobscraper.entity.Company;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CompanyRepository extends JpaRepository<Company, Long> {
  Optional<Company> findByTitle(String title);
}
