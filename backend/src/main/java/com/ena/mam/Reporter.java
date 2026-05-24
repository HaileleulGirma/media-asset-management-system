package com.ena.mam;

import jakarta.persistence.*;

@Entity
@Table(name = "reporter")
public class Reporter {

    public Reporter() {
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "reporter_id")
    private Long reporterId;

    @Column(name = "reporter_name")
    private String reporterName;

    @Column(name = "is_active")
    private Boolean isActive;

    public Long getReporterId() {
        return reporterId;
    }

    public String getReporterName() {
        return reporterName;
    }

    public void setReporterName(String reporterName) {
        this.reporterName = reporterName;
    }

    public Boolean getActive() {
        return isActive;
    }

    public void setActive(Boolean active) {
        isActive = active;
    }
}