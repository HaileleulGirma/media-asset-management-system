package com.ena.mam.production;

import jakarta.persistence.*;

import java.time.LocalDate;

@Entity
@Table(name = "production")
public class Production {
    public Production() {
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "production_id")
    private Long productionId;

    @Column(name = "production_date")
    private LocalDate productionDate;

    @Column(name = "title")
    private String title;

    @Column(name = "file_path")
    private String filePath;

    @Column(name = "file_size_gb")
    private Double fileSizeGb;

    @Column(name = "production_duration_sec")
    private Integer productionDurationSec;

    @Column(name = "version")
    private Long version;

    public Long getProductionId() {
        return productionId;
    }

    public void setProductionId(Long productionId) {
        this.productionId = productionId;
    }

    public LocalDate getProductionDate() {
        return productionDate;
    }

    public void setProductionDate(LocalDate productionDate) {
        this.productionDate = productionDate;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public Double getFileSizeGb() {
        return fileSizeGb;
    }

    public void setFileSizeGb(Double fileSizeGb) {
        this.fileSizeGb = fileSizeGb;
    }

    public Integer getProductionDurationSec() {
        return productionDurationSec;
    }

    public void setProductionDurationSec(Integer productionDurationSec) {
        this.productionDurationSec = productionDurationSec;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }
}
