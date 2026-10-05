package com.ena.mam.cassettecut;

import jakarta.persistence.*;

import java.time.LocalDate;

@Entity
@Table(name = "cassette_cut")
public class CasseteCut {

    public CasseteCut() {
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "cassette_cut_id")
    private Long cutId;

    @Column(name = "cut_title")
    private String cutTitle;

    @Column(name = "cut_date")
    private LocalDate cutDate;

    @ManyToOne
    @JoinColumn(name = "digitized_media_id")
    private Long digitizedMediaId;

    @ManyToOne
    @JoinColumn(name = "cassette_category_id")
    private Long cassetteCategoryId;

    @Version
    @Column(name = "version")
    private Long version;

    public Long getCutId() {
        return cutId;
    }

    public void setCutId(Long cutId) {
        this.cutId = cutId;
    }

    public String getCutTitle() {
        return cutTitle;
    }

    public void setCutTitle(String cutTitle) {
        this.cutTitle = cutTitle;
    }

    public LocalDate getCutDate() {
        return cutDate;
    }

    public void setCutDate(LocalDate cutDate) {
        this.cutDate = cutDate;
    }

    public Long getDigitizedMediaId() {
        return digitizedMediaId;
    }

    public void setDigitizedMediaId(Long digitizedMediaId) {
        this.digitizedMediaId = digitizedMediaId;
    }

    public Long getCassetteCategoryId() {
        return cassetteCategoryId;
    }

    public void setCassetteCategoryId(Long cassetteCategoryId) {
        this.cassetteCategoryId = cassetteCategoryId;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }
}
