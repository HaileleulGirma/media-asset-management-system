package com.ena.mam.news;

import com.ena.mam.cameraman.Cameraman;
import com.ena.mam.location.Location;
import com.ena.mam.reporter.Reporter;
import com.ena.mam.staffmember.StaffMember;
import jakarta.persistence.*;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "news")
public class News {

    public News() {
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "news_id")
    private Long newsId;

    @Column(name = "title")
    private String title;

    @Column(name = "file_path")
    private String filePath;

    @Column(name = "number_of_files")
    private Integer numberOfFiles;

    @Column(name = "total_size")
    private Double totalSize;

    @Column(name = "news_date")
    private LocalDate newsDate;


    @ManyToMany
    @JoinTable(
            name = "news_cameraman",
            joinColumns = @JoinColumn(name = "news_id"),
            inverseJoinColumns = @JoinColumn(name = "cameraman_id")
    )
    private Set<Cameraman> cameramen = new HashSet<>();

    @ManyToMany
    @JoinTable(
            name = "news_reporter",
            joinColumns = @JoinColumn(name = "news_id"),
            inverseJoinColumns = @JoinColumn(name = "reporter_id")
    )
    private Set<Reporter> reporters = new HashSet<>();

    @ManyToOne
    @JoinColumn(name = "imported_by")
    private StaffMember importer;

    @ManyToOne
    @JoinColumn(name = "ingested_by")
    private StaffMember ingestor;

    @ManyToMany
    @JoinTable(
            name = "news_location",
            joinColumns = @JoinColumn(name = "news_id"),
            inverseJoinColumns = @JoinColumn(name = "location_id")
    )
    private Set<Location> locations = new HashSet<>();


    public Long getNewsId() {
        return newsId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public Integer getNumberOfFiles() {
        return numberOfFiles;
    }

    public void setNumberOfFiles(Integer numberOfFiles) {
        this.numberOfFiles = numberOfFiles;
    }

    public Double getTotalSize() {
        return totalSize;
    }

    public void setTotalSize(Double totalSize) {
        this.totalSize = totalSize;
    }

    public LocalDate getNewsDate() {
        return newsDate;
    }

    public void setNewsDate(LocalDate newsDate) {
        this.newsDate = newsDate;
    }


    public Set<Cameraman> getCameramen() {
        return cameramen;
    }

    public Set<Location> getLocations() {
        return locations;
    }

    public void setLocations(Set<Location> locations) {
        this.locations = locations;
    }

    public StaffMember getImporter() {
        return importer;
    }

    public void setImporter(StaffMember importer) {
        this.importer = importer;
    }

    public StaffMember getIngestor() {
        return ingestor;
    }

    public void setIngestor(StaffMember ingestor) {
        this.ingestor = ingestor;
    }

    public void setCameramen(Set<Cameraman> cameramen) {
        this.cameramen = cameramen;
    }

    public Set<Reporter> getReporters() {
        return reporters;
    }

    public void setReporters(Set<Reporter> reporters) {
        this.reporters = reporters;
    }


    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }
}