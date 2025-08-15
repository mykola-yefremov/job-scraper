package com.example.jobscraper.entity;

import com.example.jobscraper.entity.enums.ProcessingStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

@Entity
@Table(name = "jobs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
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

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "company_id")
  private Company company;

  @ManyToMany(cascade = {CascadeType.PERSIST, CascadeType.MERGE})
  @JoinTable(
      name = "job_tags",
      joinColumns = @JoinColumn(name = "job_id"),
      inverseJoinColumns = @JoinColumn(name = "tag_id"))
  private Set<Tag> tags = new HashSet<>();

  @CreationTimestamp private Instant createdAt;

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    Job job = (Job) o;
    return Objects.equals(jobPageUrl, job.jobPageUrl);
  }

  @Override
  public int hashCode() {
    return Objects.hash(jobPageUrl);
  }

  @Override
  public String toString() {
    return String.format("Job{id=%d, positionName='%s'}", id, positionName);
  }
}
