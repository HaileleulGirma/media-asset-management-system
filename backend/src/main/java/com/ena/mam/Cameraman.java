package com.ena.mam;

import jakarta.persistence.*;

@Entity
@Table(name = "cameraman")
public class Cameraman {

    public Cameraman() {
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "cameraman_id")
    private Long cameramanId;

    @Column(name = "cameraman_name")
    private String cameramanName;

    @Column(name = "is_active")
    private Boolean isActive;

    public Long getCameramanId() {
        return cameramanId;
    }

    public void setCameramanId(Long cameramanId) {
        this.cameramanId = cameramanId;
    }

    public String getCameramanName() {
        return cameramanName;
    }

    public void setCameramanName(String cameramanName) {
        this.cameramanName = cameramanName;
    }

    public Boolean getActive() {
        return isActive;
    }

    public void setActive(Boolean active) {
        isActive = active;
    }
}
