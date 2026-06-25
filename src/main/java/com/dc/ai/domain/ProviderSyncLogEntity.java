package com.dc.ai.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.Instant;

@TableName("ai_provider_sync_log")
public class ProviderSyncLogEntity {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("provider_id")
    private Long providerId;

    @TableField(exist = false)
    private ProviderEntity provider;

    @TableField("sync_status")
    private String syncStatus;

    @TableField("message")
    private String message;

    @TableField("synced_at")
    private Instant syncedAt = Instant.now();

    public ProviderSyncLogEntity() {
    }

    public ProviderSyncLogEntity(ProviderEntity provider, String syncStatus, String message) {
        setProvider(provider);
        this.syncStatus = syncStatus;
        this.message = message;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getProviderId() {
        return providerId;
    }

    public void setProviderId(Long providerId) {
        this.providerId = providerId;
    }

    public ProviderEntity getProvider() {
        return provider;
    }

    public void setProvider(ProviderEntity provider) {
        this.provider = provider;
        if (provider != null) {
            this.providerId = provider.getId();
        }
    }

    public String getSyncStatus() {
        return syncStatus;
    }

    public void setSyncStatus(String syncStatus) {
        this.syncStatus = syncStatus;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Instant getSyncedAt() {
        return syncedAt;
    }

    public void setSyncedAt(Instant syncedAt) {
        this.syncedAt = syncedAt;
    }
}
