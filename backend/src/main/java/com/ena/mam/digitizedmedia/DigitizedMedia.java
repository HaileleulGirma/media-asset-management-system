package com.ena.mam.digitizedmedia;

import com.ena.mam.cassettecategory.CassetteCategory;
import com.ena.mam.staffmember.StaffMember;
import jakarta.persistence.*;

@Entity
@Table(name = "digitized_media")
public class DigitizedMedia {

    public DigitizedMedia() {
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "digitized_media_id")
    private Long digitizedMediaId;

    @Column(name = "title")
    private String title;

    @Column(name = "file_path")
    private String filePath;

    public CassetteCategory getCassetteCategory() {
        return cassetteCategory;
    }

    public void setCassetteCategory(CassetteCategory cassetteCategory) {
        this.cassetteCategory = cassetteCategory;
    }

    @ManyToOne
    @JoinColumn(name = "category_id")
    private CassetteCategory cassetteCategory;

    @ManyToOne
    @JoinColumn(name = "imported_by")
    private StaffMember importer;

    @Version
    @Column(name = "version")
    private Long version;

    public Long getDigitizedMediaId() {
        return digitizedMediaId;
    }

    public void setDigitizedMediaId(Long digitizedMediaId) {
        this.digitizedMediaId = digitizedMediaId;
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

    public StaffMember getImporter() {
        return importer;
    }

    public void setImporter(StaffMember importer) {
        this.importer = importer;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }
}
