package com.ktds.portal.approval.dto;

import com.ktds.portal.approval.domain.Approval;
import com.ktds.portal.approval.domain.ApprovalStatus;

import java.time.LocalDateTime;

/**
 * [리팩토링] ApprovalController 가 Approval 엔티티를 그대로 응답으로 내보내던 것을 응답 DTO 로 분리.
 * 이유: 엔티티를 API 에 직접 노출하면 영속성 구조 변경이 곧바로 API 계약 변경으로 번진다.
 * [불변] JSON 필드명·형태는 레거시(엔티티 직렬화 결과)와 동일 — status 는 ApprovalStatus 의 @JsonValue(getCode())
 * 덕분에 여전히 정수(0/1/2/3/9)로 직렬화된다.
 */
public record ApprovalResponse(
        Long id,
        String title,
        String content,
        int type,
        ApprovalStatus status,
        int priority,
        Long drafterId,
        Long approverId,
        String rejectReason,
        long amount,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static ApprovalResponse from(Approval approval) {
        return new ApprovalResponse(
                approval.getId(),
                approval.getTitle(),
                approval.getContent(),
                approval.getType(),
                approval.getStatus(),
                approval.getPriority(),
                approval.getDrafterId(),
                approval.getApproverId(),
                approval.getRejectReason(),
                approval.getAmount(),
                approval.getCreatedAt(),
                approval.getUpdatedAt()
        );
    }
}
