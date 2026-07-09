package com.ktds.portal.approval.domain;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * [리팩토링] 레거시 Approval.status 매직넘버(0/1/2/3/9) → 여기서 enum 으로 (Enum 도입).
 * 이유: int 였을 때는 잘못된 값(4, 100 등)도 컴파일이 통과했다 — enum 은 존재하는 상수만 대입 가능해
 *      "있을 수 없는 상태"를 컴파일 타임에 차단한다.
 *
 * [불변] DB 저장값은 그대로 정수 0/1/2/3/9 다 — ApprovalStatusConverter 가 매핑한다.
 *       @Enumerated(STRING)/@Enumerated(ORDINAL) 은 쓰지 않는다(문자 저장·순번 밀림 방지, CLAUDE.md 규칙).
 * [불변] JSON 응답도 기존과 동일하게 정수로 노출된다(@JsonValue) — index.html 이 a.status===0 처럼
 *       정수 비교를 하므로 API 응답 형식을 바꾸지 않는다.
 */
public enum ApprovalStatus {
    DRAFT(0),       // 임시저장
    SUBMITTED(1),   // 상신
    APPROVED(2),    // 승인
    REJECTED(3),    // 반려
    CANCELED(9);    // 취소 — 레거시가 왜 4~8을 비우고 9를 썼는지는 알 수 없다(값 자체만 보존)

    private final int code;

    ApprovalStatus(int code) {
        this.code = code;
    }

    @JsonValue
    public int getCode() {
        return code;
    }

    @JsonCreator
    public static ApprovalStatus fromCode(int code) {
        for (ApprovalStatus status : values()) {
            if (status.code == code) {
                return status;
            }
        }
        throw new IllegalArgumentException("알 수 없는 결재 상태 코드: " + code);
    }

    // [리팩토링] ApprovalService.statusLabel() 의 tmp 변수 + 5분기 if-else 를 여기로 이관 — enum 이 자기 라벨을 직접 소유(Feature Envy 완화)
    public String label() {
        return switch (this) {
            case DRAFT -> "임시저장";
            case SUBMITTED -> "상신";
            case APPROVED -> "승인";
            case REJECTED -> "반려";
            case CANCELED -> "취소";
        };
    }
}
