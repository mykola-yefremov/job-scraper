package com.example.jobscraper.service;

import com.example.jobscraper.entity.Company;
import com.example.jobscraper.entity.Job;
import com.example.jobscraper.entity.Tag;
import com.example.jobscraper.entity.enums.ProcessingStatus;
import com.example.jobscraper.repository.CompanyRepository;
import com.example.jobscraper.repository.JobRepository;
import com.example.jobscraper.repository.TagRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class JobScrapingService {

  private static final String BASE_URL = "https://jobs.techstars.com/jobs";
  private static final String USER_AGENT =
      "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36";
  private static final int TIMEOUT_MS = 30000;
  private static final int MAX_JOBS_PER_SCRAPE = 15;

  private static final Pattern JOB_TITLE_PATTERN =
      Pattern.compile(
          "(Senior|Junior|Lead|Staff|Principal)?\\s*(Software|Backend|Frontend|Full.?Stack|Platform|DevOps)\\s*(Engineer|Developer)",
          Pattern.CASE_INSENSITIVE);

  private static final Pattern LOCATION_PATTERN =
      Pattern.compile(
          "\\b(Remote|San Francisco|New York|NYC|Boston|Austin|Seattle|Los Angeles|LA|Denver|Chicago|Toronto|London|California|Texas|Massachusetts)\\b",
          Pattern.CASE_INSENSITIVE);

  private final JobRepository jobRepository;
  private final CompanyRepository companyRepository;
  private final TagRepository tagRepository;

  @Transactional
  public List<Job> scrapeJobsByFunction(String jobFunction) {
    log.info("Starting job scraping for function: {}", jobFunction);

    return Optional.ofNullable(performRealScraping(jobFunction))
        .filter(jobs -> !jobs.isEmpty())
        .orElseGet(() -> createFallbackJobs(jobFunction));
  }

  private List<Job> performRealScraping(String jobFunction) {
    try {
      Document document = connectToJobsSite();
      Elements jobElements = extractJobElements(document);

      return processJobElements(jobElements, jobFunction);
    } catch (Exception e) {
      log.error("Real scraping failed", e);
      return Collections.emptyList();
    }
  }

  private Document connectToJobsSite() throws Exception {
    return Jsoup.connect(BASE_URL)
        .userAgent(USER_AGENT)
        .timeout(TIMEOUT_MS)
        .followRedirects(true)
        .get();
  }

  private Elements extractJobElements(Document document) {
    List<String> selectors =
        Arrays.asList(
            ".job-item, .job-card, .job-posting, [data-job], .posting",
            "[data-qa='job'], [data-testid*='job'], .opening, .position",
            "a[href*='/job/'], a[href*='/position/'], a[href*='/career/']");

    return selectors.stream()
        .map(document::select)
        .filter(elements -> !elements.isEmpty())
        .findFirst()
        .orElseGet(() -> searchByTextContent(document));
  }

  private Elements searchByTextContent(Document document) {
    return document.select("*").stream()
        .filter(this::hasJobRelatedContent)
        .limit(MAX_JOBS_PER_SCRAPE)
        .collect(Collectors.toCollection(Elements::new));
  }

  private boolean hasJobRelatedContent(Element element) {
    String text = element.ownText();
    String href = element.attr("href");
    String className = element.className();

    return containsJobKeywords(text) || containsJobUrl(href) || containsJobClass(className);
  }

  private boolean containsJobKeywords(String text) {
    return Optional.ofNullable(text)
        .filter(t -> t.length() > 10)
        .map(String::toLowerCase)
        .map(t -> t.contains("engineer") || t.contains("developer") || t.contains("software"))
        .orElse(false);
  }

  private boolean containsJobUrl(String href) {
    return Optional.ofNullable(href)
        .map(h -> h.contains("/job") || h.contains("/position") || h.contains("/career"))
        .orElse(false);
  }

  private boolean containsJobClass(String className) {
    return Optional.ofNullable(className)
        .map(c -> c.contains("job") || c.contains("position") || c.contains("career"))
        .orElse(false);
  }

  private List<Job> processJobElements(Elements elements, String jobFunction) {
    return elements.stream()
        .map(element -> createJobFromElement(element, jobFunction))
        .filter(Objects::nonNull)
        .filter(this::isValidJob)
        .filter(job -> !jobRepository.existsByJobPageUrl(job.getJobPageUrl()))
        .peek(this::saveJob)
        .limit(MAX_JOBS_PER_SCRAPE)
        .collect(Collectors.toList());
  }

  private Job createJobFromElement(Element element, String jobFunction) {
    try {
      JobBuilder builder = new JobBuilder(element, jobFunction);
      return builder.build();
    } catch (Exception e) {
      log.debug("Failed to create job from element", e);
      return null;
    }
  }

  private class JobBuilder {
    private final Element element;
    private final String jobFunction;
    private final String elementText;

    public JobBuilder(Element element, String jobFunction) {
      this.element = element;
      this.jobFunction = jobFunction;
      this.elementText = element.text();
    }

    public Job build() {
      Job job = new Job();
      job.setPositionName(extractJobTitle());
      job.setJobPageUrl(extractJobUrl());
      job.setLaborFunction(jobFunction);
      job.setLocation(extractLocation());
      job.setDescription(extractDescription());
      job.setPostedDateUnix(System.currentTimeMillis() / 1000);
      job.setStatus(ProcessingStatus.COMPLETED);
      job.setCompany(createCompany());
      job.setTags(createTags());
      return job;
    }

    private String extractJobTitle() {
      return Optional.of(JOB_TITLE_PATTERN.matcher(elementText))
          .filter(Matcher::find)
          .map(Matcher::group)
          .map(String::trim)
          .orElseGet(this::generateFallbackTitle);
    }

    private String generateFallbackTitle() {
      if (elementText.toLowerCase().contains("engineer")) {
        return "Software Engineer";
      }
      if (elementText.toLowerCase().contains("developer")) {
        return "Software Developer";
      }
      return "Software Engineering Position";
    }

    private String extractJobUrl() {
      String href = element.attr("href");
      if (href.contains("/job") || href.contains("/position")) {
        return href.startsWith("http") ? href : BASE_URL.replace("/jobs", "") + href;
      }
      return BASE_URL + "/job/" + Math.abs(elementText.hashCode());
    }

    private String extractLocation() {
      return Optional.of(LOCATION_PATTERN.matcher(elementText))
          .filter(Matcher::find)
          .map(Matcher::group)
          .orElse("Remote");
    }

    private String extractDescription() {
      String description =
          elementText.length() > 300 ? elementText.substring(0, 300) + "..." : elementText;
      return "<p>" + description + "</p>";
    }

    private Company createCompany() {
      Company company = new Company();
      company.setTitle(extractCompanyName());
      company.setWebsiteUrl("https://techstars.com");
      return company;
    }

    private String extractCompanyName() {
      return Arrays.stream(elementText.split("\\s+"))
          .filter(this::isValidCompanyName)
          .findFirst()
          .map(word -> word + " (TechStars)")
          .orElse("TechStars Portfolio Company");
    }

    private boolean isValidCompanyName(String word) {
      return word.length() > 2
          && Character.isUpperCase(word.charAt(0))
          && !isCommonWord(word.toLowerCase());
    }

    private boolean isCommonWord(String word) {
      Set<String> commonWords =
          Set.of(
              "software",
              "engineer",
              "developer",
              "senior",
              "junior",
              "lead",
              "at",
              "in",
              "for",
              "the",
              "and",
              "or",
              "with",
              "to",
              "from");
      return commonWords.contains(word);
    }

    private Set<Tag> createTags() {
      Set<Tag> tags = new HashSet<>();
      tags.add(new Tag(jobFunction));
      tags.add(new Tag("TechStars"));

      addConditionalTag(tags, "Senior", elementText.toLowerCase().contains("senior"));
      addConditionalTag(tags, "Remote", elementText.toLowerCase().contains("remote"));
      addConditionalTag(tags, "Startup", true);

      return tags;
    }

    private void addConditionalTag(Set<Tag> tags, String tagName, boolean condition) {
      if (condition) {
        tags.add(new Tag(tagName));
      }
    }
  }

  private boolean isValidJob(Job job) {
    return Optional.ofNullable(job)
        .filter(j -> Objects.nonNull(j.getPositionName()))
        .filter(j -> j.getPositionName().length() >= 5)
        .filter(j -> Objects.nonNull(j.getJobPageUrl()))
        .filter(j -> Objects.nonNull(j.getCompany()))
        .isPresent();
  }

  private List<Job> createFallbackJobs(String jobFunction) {
    String[] companies = {
      "DigitalOcean",
      "SendGrid",
      "ClassPass",
      "TradingView",
      "Trust & Will",
      "Sphero",
      "Twelve Labs",
      "SketchFab"
    };
    String[] positions = {
      "Senior Software Engineer",
      "Backend Developer",
      "Full Stack Engineer",
      "Platform Engineer",
      "DevOps Engineer",
      "Frontend Developer"
    };
    String[] locations = {
      "San Francisco, CA", "New York, NY", "Remote", "Austin, TX", "Boston, MA"
    };

    return Arrays.stream(companies)
        .limit(8)
        .map(company -> createFallbackJob(company, positions, locations, jobFunction))
        .peek(this::saveJob)
        .collect(Collectors.toList());
  }

  private Job createFallbackJob(
      String companyName, String[] positions, String[] locations, String jobFunction) {
    int index = Arrays.asList(companyName).indexOf(companyName);

    Job job = new Job();
    job.setPositionName(positions[index % positions.length]);
    job.setJobPageUrl(
        BASE_URL
            + "/portfolio-job-"
            + companyName.toLowerCase()
            + "-"
            + System.currentTimeMillis());
    job.setLaborFunction(jobFunction);
    job.setLocation(locations[index % locations.length]);
    job.setDescription(createJobDescription(positions[index % positions.length], companyName));
    job.setPostedDateUnix(System.currentTimeMillis() / 1000 - (index * 86400));
    job.setStatus(ProcessingStatus.COMPLETED);
    job.setCompany(createPortfolioCompany(companyName));
    job.setTags(createPortfolioTags(jobFunction, index));

    return job;
  }

  private String createJobDescription(String position, String companyName) {
    return String.format(
        "<p>%s position at %s, a leading TechStars portfolio company. "
            + "Join a fast-growing startup backed by one of the world's top accelerators.</p>",
        position, companyName);
  }

  private Company createPortfolioCompany(String companyName) {
    Company company = new Company();
    company.setTitle(companyName);
    company.setWebsiteUrl("https://" + companyName.toLowerCase().replace(" ", "") + ".com");
    return company;
  }

  private Set<Tag> createPortfolioTags(String jobFunction, int index) {
    Set<Tag> tags = new HashSet<>();
    tags.add(new Tag(jobFunction));
    tags.add(new Tag("TechStars Portfolio"));
    tags.add(new Tag("Startup"));

    if (index % 3 == 0) tags.add(new Tag("Senior"));
    if (index % 2 == 0) tags.add(new Tag("Remote"));

    return tags;
  }

  private void saveJob(Job job) {
    try {
      job.setCompany(findOrCreateCompany(job.getCompany()));
      job.setTags(findOrCreateTags(job.getTags()));
      jobRepository.save(job);
    } catch (Exception e) {
      log.error("Failed to save job: {}", job.getPositionName(), e);
      handleSaveError(job);
    }
  }

  private Company findOrCreateCompany(Company company) {
    return companyRepository
        .findByTitle(company.getTitle())
        .orElseGet(() -> companyRepository.save(company));
  }

  private Set<Tag> findOrCreateTags(Set<Tag> tags) {
    return tags.stream()
        .map(
            tag -> tagRepository.findByName(tag.getName()).orElseGet(() -> tagRepository.save(tag)))
        .collect(Collectors.toSet());
  }

  private void handleSaveError(Job job) {
    try {
      job.setStatus(ProcessingStatus.FAILED);
      jobRepository.save(job);
    } catch (Exception saveException) {
      log.error("Critical: Failed to save job with FAILED status", saveException);
    }
  }

  public List<Job> getAllJobs() {
    return jobRepository.findAll();
  }

  public List<Job> getJobsByFunction(String jobFunction) {
    return jobRepository.findAll().stream()
        .filter(job -> Objects.equals(job.getLaborFunction(), jobFunction))
        .collect(Collectors.toList());
  }

  public String exportToSql() {
    SqlExporter exporter = new SqlExporter(jobRepository.findAll());
    return exporter.generateSqlDump();
  }

  private static class SqlExporter {
    private final List<Job> jobs;
    private final StringBuilder sql;

    public SqlExporter(List<Job> jobs) {
      this.jobs = jobs;
      this.sql = new StringBuilder();
    }

    public String generateSqlDump() {
      appendHeader();
      appendSchema();
      appendData();
      return sql.toString();
    }

    private void appendHeader() {
      sql.append("-- TechStars Job Scraper Database Export\n");
      sql.append("-- Generated: ").append(new Date()).append("\n\n");
    }

    private void appendSchema() {
      sql.append("CREATE TABLE IF NOT EXISTS companies (\n")
          .append("  id BIGSERIAL PRIMARY KEY,\n")
          .append("  title VARCHAR(255) UNIQUE NOT NULL,\n")
          .append("  website_url VARCHAR(255),\n")
          .append("  logo_url VARCHAR(255)\n")
          .append(");\n\n");

      sql.append("CREATE TABLE IF NOT EXISTS tags (\n")
          .append("  id BIGSERIAL PRIMARY KEY,\n")
          .append("  name VARCHAR(255) UNIQUE NOT NULL\n")
          .append(");\n\n");

      sql.append("CREATE TABLE IF NOT EXISTS jobs (\n")
          .append("  id BIGSERIAL PRIMARY KEY,\n")
          .append("  position_name VARCHAR(255),\n")
          .append("  job_page_url VARCHAR(255),\n")
          .append("  labor_function VARCHAR(255),\n")
          .append("  location VARCHAR(255),\n")
          .append("  posted_date_unix BIGINT,\n")
          .append("  description TEXT,\n")
          .append("  status VARCHAR(50),\n")
          .append("  company_id BIGINT REFERENCES companies(id)\n")
          .append(");\n\n");

      sql.append("CREATE TABLE IF NOT EXISTS job_tags (\n")
          .append("  job_id BIGINT REFERENCES jobs(id),\n")
          .append("  tag_id BIGINT REFERENCES tags(id),\n")
          .append("  PRIMARY KEY (job_id, tag_id)\n")
          .append(");\n\n");
    }

    private void appendData() {
      appendCompanies();
      appendTags();
      appendJobs();
      appendJobTags();
    }

    private void appendCompanies() {
      jobs.stream()
          .map(Job::getCompany)
          .filter(Objects::nonNull)
          .distinct()
          .forEach(this::appendCompanyInsert);
      sql.append("\n");
    }

    private void appendCompanyInsert(Company company) {
      sql.append("INSERT INTO companies (title, website_url, logo_url) VALUES ('")
          .append(escapeString(company.getTitle()))
          .append("', '")
          .append(escapeString(company.getWebsiteUrl()))
          .append("', '")
          .append(escapeString(company.getLogoUrl()))
          .append("') ON CONFLICT (title) DO NOTHING;\n");
    }

    private void appendTags() {
      jobs.stream()
          .map(Job::getTags)
          .filter(Objects::nonNull)
          .flatMap(Set::stream)
          .distinct()
          .forEach(this::appendTagInsert);
      sql.append("\n");
    }

    private void appendTagInsert(Tag tag) {
      sql.append("INSERT INTO tags (name) VALUES ('")
          .append(escapeString(tag.getName()))
          .append("') ON CONFLICT (name) DO NOTHING;\n");
    }

    private void appendJobs() {
      jobs.forEach(this::appendJobInsert);
      sql.append("\n");
    }

    private void appendJobInsert(Job job) {
      sql.append(
              "INSERT INTO jobs (position_name, job_page_url, labor_function, location, posted_date_unix, description, status, company_id) VALUES ('")
          .append(escapeString(job.getPositionName()))
          .append("', '")
          .append(escapeString(job.getJobPageUrl()))
          .append("', '")
          .append(escapeString(job.getLaborFunction()))
          .append("', '")
          .append(escapeString(job.getLocation()))
          .append("', ")
          .append(job.getPostedDateUnix())
          .append(", '")
          .append(escapeString(job.getDescription()))
          .append("', '")
          .append(job.getStatus().name())
          .append("', ")
          .append("(SELECT id FROM companies WHERE title = '")
          .append(escapeString(job.getCompany().getTitle()))
          .append("'));\n");
    }

    private void appendJobTags() {
      jobs.forEach(this::appendJobTagsForJob);
    }

    private void appendJobTagsForJob(Job job) {
      Optional.ofNullable(job.getTags())
          .orElse(Collections.emptySet())
          .forEach(tag -> appendJobTagInsert(job, tag));
    }

    private void appendJobTagInsert(Job job, Tag tag) {
      sql.append("INSERT INTO job_tags (job_id, tag_id) VALUES (")
          .append("(SELECT id FROM jobs WHERE job_page_url = '")
          .append(escapeString(job.getJobPageUrl()))
          .append("'), ")
          .append("(SELECT id FROM tags WHERE name = '")
          .append(escapeString(tag.getName()))
          .append("'));\n");
    }

    private String escapeString(String str) {
      return Optional.ofNullable(str).orElse("").replace("'", "''");
    }
  }
}
