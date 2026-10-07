package com.ena.mam.digitizedmedia;

import com.ena.mam.cassettecategory.CassetteCategory;
import com.ena.mam.staffmember.StaffMember;
import jakarta.persistence.*;

@Entity
@Table(
        name = "digitized_media",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_digitized_media_category_identifier",
                        columnNames = {
                                "identifier_category_id",
                                "identifier_number"
                        }
                )
        }
)
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

    @ManyToOne
    @JoinColumn(name = "identifier_category_id")
    private CassetteCategory cassetteCategory;

    @Column(name = "identifier_number")
    private Long identifierNumber;


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

    public Long getIdentifierNumber() {
        return identifierNumber;
    }

    public void setIdentifierNumber(Long identifierNumber) {
        this.identifierNumber = identifierNumber;
    }
}
