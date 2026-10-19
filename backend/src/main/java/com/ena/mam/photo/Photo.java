package com.ena.mam.photo;

import com.ena.mam.cameraman.Cameraman;
import com.ena.mam.staffmember.StaffMember;
import jakarta.persistence.*;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "photo")
public class Photo {
    public Photo() {
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "photo_id")
    private Long photoId;

    @ManyToMany
    @JoinTable(
            name = "photo_cameraman",
            joinColumns = @JoinColumn(name = "photo_id"),
            inverseJoinColumns = @JoinColumn(name = "cameraman_id")
    )
    private Set<Cameraman> cameramen = new HashSet<>();

    @Column(name = "title")
    private String title;

    @Column(name = "file_path")
    private String filePath;

    @Column(name = "number_of_files")
    private Integer numberOfFiles;

    @Column(name = "file_size_mb")
    private Double fileSizeMb;

    @Column(name = "photo_date")
    private LocalDate photoDate;

    @ManyToOne
    @JoinColumn(name = "imported_by")
    private StaffMember importer;

    @Version
    @Column(name = "version")
    private Long version;

    public Long getPhotoId() {
        return photoId;
    }

    public void setPhotoId(Long photoId) {
        this.photoId = photoId;
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

    public Integer getNumberOfFiles() {
        return numberOfFiles;
    }

    public void setNumberOfFiles(Integer numberOfFiles) {
        this.numberOfFiles = numberOfFiles;
    }

    public Double getFileSizeMb() {
        return fileSizeMb;
    }

    public void setFileSizeMb(Double fileSizeMb) {
        this.fileSizeMb = fileSizeMb;
    }

    public LocalDate getPhotoDate() {
        return photoDate;
    }

    public void setPhotoDate(LocalDate photoDate) {
        this.photoDate = photoDate;
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

    public Set<Cameraman> getCameramen() {
        return cameramen;
    }

    public void setCameramen(Set<Cameraman> cameramen) {
        this.cameramen = cameramen;
    }
}
