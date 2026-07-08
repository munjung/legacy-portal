package com.ktds.portal.approval;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * 결재 엔티티.
 *
 * [스멜] 빈약한 도메인 모델(Anemic Domain Model) — 데이터만 있고 행위가 없다.
 * [스멜] 원시 타입 집착(Primitive Obsession) — type, priority 가 여전히 int (이번 범위 밖).
 *        [리팩토링] status 는 ApprovalStatus enum 으로 전환함 — DB엔 ApprovalStatusConverter 가 기존과 동일하게 0/1/2/3/9 정수로 저장.
 *        type:   1=지출, 2=휴가, 3=구매, 4=기타
 *        priority: 1=낮음, 2=보통, 3=높음
 * [스멜] 캡슐화 부재 — 모든 필드에 public setter. 누구나 상태를 마음대로 바꿀 수 있다.
 */
@Entity
public class Approval {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;
    private String content;
    private int type;       // 1=지출 2=휴가 3=구매 4=기타 (의미를 주석으로만 설명 → enum 후보, 이번 범위 밖)
    // [리팩토링] int status(매직넘버 0/1/2/3/9) → ApprovalStatus enum. @Enumerated(STRING/ORDINAL) 금지 규칙 때문에
    // AttributeConverter 로 DB 컬럼은 그대로 정수 — @Enumerated 는 문자 저장(STRING)이거나 선언 순서에 의존(ORDINAL, 9와 어긋남)이라 못 씀.
    @Convert(converter = ApprovalStatusConverter.class)
    private ApprovalStatus status;
    private int priority;   // 1=낮음 2=보통 3=높음
    private Long drafterId;     // 기안자
    private Long approverId;    // 결재자
    private String rejectReason;
    private long amount;        // 지출/구매 금액
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public int getType() { return type; }
    public void setType(int type) { this.type = type; }
    public ApprovalStatus getStatus() { return status; }
    public void setStatus(ApprovalStatus status) { this.status = status; }
    public int getPriority() { return priority; }
    public void setPriority(int priority) { this.priority = priority; }
    public Long getDrafterId() { return drafterId; }
    public void setDrafterId(Long drafterId) { this.drafterId = drafterId; }
    public Long getApproverId() { return approverId; }
    public void setApproverId(Long approverId) { this.approverId = approverId; }
    public String getRejectReason() { return rejectReason; }
    public void setRejectReason(String rejectReason) { this.rejectReason = rejectReason; }
    public long getAmount() { return amount; }
    public void setAmount(long amount) { this.amount = amount; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
