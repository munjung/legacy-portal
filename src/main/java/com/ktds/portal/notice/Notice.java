package com.ktds.portal.notice;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * 공지 엔티티. [스멜] 빈약한 도메인 모델 + 원시 타입(status int).
 * status: 0=임시, 1=게시, 9=내림
 */
@Entity
public class Notice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;
    private String content;
    private int status;     // 0=임시 1=게시 9=내림 (숫자만 저장 → 의미는 주석에만)
    private int category;   // 1=일반 2=긴급 3=인사 (enum 후보)
    private Long writerId;
    private boolean pinned;
    private LocalDateTime createdAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public int getStatus() { return status; }
    public void setStatus(int status) { this.status = status; }
    public int getCategory() { return category; }
    public void setCategory(int category) { this.category = category; }
    public Long getWriterId() { return writerId; }
    public void setWriterId(Long writerId) { this.writerId = writerId; }
    public boolean isPinned() { return pinned; }
    public void setPinned(boolean pinned) { this.pinned = pinned; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
