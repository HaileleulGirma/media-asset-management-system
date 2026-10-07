package com.ena.mam.cassettecut;

import com.ena.mam.cassettecategory.CassetteCategory;
import com.ena.mam.digitizedmedia.DigitizedMedia;
import jakarta.persistence.*;

import java.time.LocalDate;

@Entity
@Table(name = "cassette_cut")
public class CassetteCut {

    public CassetteCut() {
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
    private DigitizedMedia digitizedMedia;

    @ManyToOne
    @JoinColumn(name = "cassette_category_id")
    private CassetteCategory cassetteCategory;

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


    public DigitizedMedia getDigitizedMedia() {
        return digitizedMedia;
    }

    public void setDigitizedMedia(DigitizedMedia digitizedMedia) {
        this.digitizedMedia = digitizedMedia;
    }

    public CassetteCategory getCassetteCategory() {
        return cassetteCategory;
    }

    public void setCassetteCategory(CassetteCategory cassetteCategory) {
        this.cassetteCategory = cassetteCategory;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }
}
