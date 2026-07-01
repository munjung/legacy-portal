package com.ktds.portal.schedule;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * 일정 엔티티. [스멜] 빈약한 도메인 모델 + 원시 타입.
 * status: 0=예정, 1=확정, 9=취소
 */
@Entity
public class Schedule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;
    private int status;     // 0=예정 1=확정 9=취소 (숫자만 저장 → 의미는 주석에만)
    private Long ownerId;
    private LocalDateTime startAt;
    private LocalDateTime endAt;
    private String location;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public int getStatus() { return status; }
    public void setStatus(int status) { this.status = status; }
    public Long getOwnerId() { return ownerId; }
    public void setOwnerId(Long ownerId) { this.ownerId = ownerId; }
    public LocalDateTime getStartAt() { return startAt; }
    public void setStartAt(LocalDateTime startAt) { this.startAt = startAt; }
    public LocalDateTime getEndAt() { return endAt; }
    public void setEndAt(LocalDateTime endAt) { this.endAt = endAt; }
    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }
}
