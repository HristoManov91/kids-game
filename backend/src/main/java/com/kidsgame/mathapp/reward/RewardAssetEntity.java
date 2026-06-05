package com.kidsgame.mathapp.reward;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "reward_assets")
public class RewardAssetEntity {
    @Id
    @Column(length = 120)
    private String id;

    @Column(nullable = false, length = 180)
    private String fileName;

    @Column(nullable = false, length = 80)
    private String contentType;

    @Column(nullable = false, columnDefinition = "bytea")
    private byte[] data;

    @Column(nullable = false)
    private int byteSize;

    @Column(nullable = false, length = 64)
    private String checksumSha256;

    @Column(nullable = false, length = 40)
    private String source;

    @Column(nullable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    protected RewardAssetEntity() {
    }

    public RewardAssetEntity(String id, String fileName, String contentType, byte[] data, String checksumSha256, String source) {
        this.id = id;
        replace(fileName, contentType, data, checksumSha256, source);
    }

    @PrePersist
    void prePersist() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = Instant.now();
    }

    public void replace(String fileName, String contentType, byte[] data, String checksumSha256, String source) {
        this.fileName = fileName;
        this.contentType = contentType;
        this.data = data;
        this.byteSize = data.length;
        this.checksumSha256 = checksumSha256;
        this.source = source;
    }

    public String getId() {
        return id;
    }

    public String getFileName() {
        return fileName;
    }

    public String getContentType() {
        return contentType;
    }

    public byte[] getData() {
        return data;
    }

    public int getByteSize() {
        return byteSize;
    }

    public String getChecksumSha256() {
        return checksumSha256;
    }

    public String getSource() {
        return source;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
