package com.example.jobscraper.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "tags")
@Data
@NoArgsConstructor
public class Tag {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(unique = true, nullable = false)
  private String name;

  public Tag(String name) {
    this.name = name;
  }
}
