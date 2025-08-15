package com.example.jobscraper.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "companies")
@Data
@NoArgsConstructor
public class Company {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(unique = true, nullable = false)
  private String title;

  private String websiteUrl;
  private String logoUrl;
}
