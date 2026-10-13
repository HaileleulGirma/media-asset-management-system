package com.ena.mam.production;

import jakarta.persistence.*;

@Entity
@Table(name = "production")
public class Production {
    public Production() {
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "production_id")
    private Long productionId;

    @Column(name = "title")
    private String title;

    @Column(name = "file_path")
    private String filePath;

    @Column(name = "file_size_gb")
    private Double fileSizeGb;

    @Column(name = "version")
    private Long version;

    public Long getProductionId() {
        return productionId;
    }

    public void setProductionId(Long productionId) {
        this.productionId = productionId;
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

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }
}
