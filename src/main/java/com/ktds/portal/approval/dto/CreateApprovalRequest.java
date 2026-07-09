package com.ktds.portal.approval.dto;

/**
 * [리팩토링] ApprovalController.create() 가 받던 Map&lt;String,Object&gt; 요청 바디를 record DTO 로 대체.
 * 이유: Map 은 키 철자가 틀려도 컴파일이 통과하고 실행 시점에야 터진다 — record 는 필드가 없으면 컴파일 에러로 즉시 드러난다.
 * [불변] JSON 필드명·의미(type/priority 매직넘버, amount/urgent 기본값)는 레거시와 동일 — 계약(엔드포인트·요청 형식)은 바꾸지 않는다.
 *
 * type: 1=지출 2=휴가 3=구매 4=기타, priority: 1=낮음 2=보통 3=높음 (이번 범위 밖 — 그대로 int 유지)
 */
public record CreateApprovalRequest(
        String title,
        String content,
        int type,
        int priority,
        Long drafterId,
        Long approverId,
        long amount,
        boolean urgent
) {
}
