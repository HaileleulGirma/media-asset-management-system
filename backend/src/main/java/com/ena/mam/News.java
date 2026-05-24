package com.ena.mam;

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

    private String title;

    @Column(name = "number_of_files")
    private Integer numberOfFiles;

    @Column(name = "total_size")
    private Double totalSize;

    @Column(name = "news_date")
    private LocalDate newsDate;

    private String location;

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

    @ManyToMany
    @JoinTable(
            name = "news_importer",
            joinColumns = @JoinColumn(name = "news_id"),
            inverseJoinColumns = @JoinColumn(name = "imported_by")
    )
    private Set<StaffMember> importers = new HashSet<>();

    @ManyToMany
    @JoinTable(
            name = "news_ingestor",
            joinColumns = @JoinColumn(name = "news_id"),
            inverseJoinColumns = @JoinColumn(name = "ingested_by")
    )
    private Set<StaffMember> ingestors = new HashSet<>();


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

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public Set<Cameraman> getCameramen() {
        return cameramen;
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

    public Set<StaffMember> getImporters() {
        return importers;
    }

    public void setImporters(Set<StaffMember> importers) {
        this.importers = importers;
    }

    public Set<StaffMember> getIngestors() {
        return ingestors;
    }

    public void setIngestors(Set<StaffMember> ingestors) {
        this.ingestors = ingestors;
    }
}