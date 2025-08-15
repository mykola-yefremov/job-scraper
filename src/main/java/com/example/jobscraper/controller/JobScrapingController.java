package com.example.jobscraper.controller;

import com.example.jobscraper.entity.Job;
import com.example.jobscraper.service.JobScrapingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/jobs")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class JobScrapingController {

  private final JobScrapingService jobScrapingService;

  @PostMapping("/scrape")
  public ResponseEntity<Map<String, Object>> scrapeJobs(@RequestParam String jobFunction) {
    Map<String, Object> response = new HashMap<>();

    try {
      List<Job> jobs = jobScrapingService.scrapeJobsByFunction(jobFunction);

      response.put("success", true);
      response.put("totalJobs", jobs.size());
      response.put("jobFunction", jobFunction);
      response.put("message", "Successfully scraped " + jobs.size() + " jobs");

      return ResponseEntity.ok(response);

    } catch (Exception e) {
      response.put("success", false);
      response.put("totalJobs", 0);
      response.put("jobFunction", jobFunction);
      response.put("message", "Failed to scrape jobs: " + e.getMessage());

      return ResponseEntity.status(500).body(response);
    }
  }

  @GetMapping("/functions")
  public ResponseEntity<List<String>> getAvailableJobFunctions() {
    List<String> functions =
        Arrays.asList(
            "Software Engineering",
            "Product Management",
            "Marketing",
            "Sales",
            "Operations",
            "Data Science",
            "Design",
            "Business Development",
            "Finance",
            "Customer Success");

    return ResponseEntity.ok(functions);
  }

  @GetMapping
  public ResponseEntity<List<Job>> getAllJobs() {
    try {
      List<Job> jobs = jobScrapingService.getAllJobs();
      return ResponseEntity.ok(jobs);
    } catch (Exception e) {
      return ResponseEntity.status(500).body(new ArrayList<>());
    }
  }

  @GetMapping("/function/{jobFunction}")
  public ResponseEntity<List<Job>> getJobsByFunction(@PathVariable String jobFunction) {
    try {
      List<Job> jobs = jobScrapingService.getJobsByFunction(jobFunction);
      return ResponseEntity.ok(jobs);
    } catch (Exception e) {
      return ResponseEntity.status(500).body(new ArrayList<>());
    }
  }

  @GetMapping("/export")
  public ResponseEntity<String> exportToSql() {
    try {
      String sqlDump = jobScrapingService.exportToSql();
      return ResponseEntity.ok(sqlDump);
    } catch (Exception e) {
      return ResponseEntity.status(500).body("Export failed: " + e.getMessage());
    }
  }
}
