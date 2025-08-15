package com.example.jobscraper.entity;

import com.example.jobscraper.entity.enums.ProcessingStatus;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "jobs")
@Data
@NoArgsConstructor
public class Job {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  private String positionName;
  private String jobPageUrl;
  private String laborFunction;
  private String location;
  private Long postedDateUnix;

  @Lob private String description;

  @Enumerated(EnumType.STRING)
  private ProcessingStatus status = ProcessingStatus.PENDING;

  @ManyToOne(cascade = CascadeType.ALL)
  @JoinColumn(name = "company_id")
  private Company company;

  @ManyToMany(cascade = CascadeType.ALL)
  @JoinTable(
      name = "job_tags",
      joinColumns = @JoinColumn(name = "job_id"),
      inverseJoinColumns = @JoinColumn(name = "tag_id"))
  private Set<Tag> tags = new HashSet<>();
}
