package com.ena.mam.cassettecategory;

import jakarta.persistence.*;

@Entity
@Table(name = "cassette_category")
public class CassetteCategory {

    public CassetteCategory() {
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "cassette_category_id")
    private Long categoryId;

    @Column(name = "category_name")
    private String categoryName;

    @Version
    @Column(name = "version")
    private Long version;

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }

    public Long getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(Long categoryId) {
        this.categoryId = categoryId;
    }

    public String getCategoryName() {
        return categoryName;
    }

    public void setCategoryName(String categoryName) {
        this.categoryName = categoryName;
    }
}
