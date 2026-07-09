package com.ktds.portal.approval.dto;

/**
 * [리팩토링] ApprovalController.process() 가 받던 Map&lt;String,Object&gt; 요청 바디를 record DTO 로 대체.
 * [불변] action 매직넘버(1=상신 2=승인 3=반려 9=취소)와 reason 생략 시 빈 문자열 기본값은 레거시와 동일하게 유지.
 */
public record ProcessRequest(
        Long userId,
        int action,
        String reason
) {
    // [리팩토링] 레거시의 body.getOrDefault("reason", "") 를 컴팩트 생성자로 이관 — reason 생략 시 빈 문자열(레거시 동작 보존)
    public ProcessRequest {
        if (reason == null) {
            reason = "";
        }
    }
}
