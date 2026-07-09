package com.ktds.portal.approval.controller;

import com.ktds.portal.approval.dto.ApprovalResponse;
import com.ktds.portal.approval.dto.CreateApprovalRequest;
import com.ktds.portal.approval.dto.ProcessRequest;
import com.ktds.portal.approval.service.ApprovalService;
import org.springframework.web.bind.annotation.*;
import java.util.List;

/**
 * 결재 REST 컨트롤러.
 * [리팩토링] Map&lt;String,Object&gt; 요청 바디 → CreateApprovalRequest/ProcessRequest record DTO 로 분리.
 * [리팩토링] Approval 엔티티를 그대로 응답하던 것 → ApprovalResponse DTO 로 분리(엔티티 비노출).
 * [불변] 엔드포인트(URL·HTTP 메서드)·요청/응답 JSON 필드명·의미는 레거시와 동일 (action/type/priority 매직넘버 포함).
 */
@RestController
@RequestMapping("/api/approvals")
public class ApprovalController {

    private final ApprovalService service;

    public ApprovalController(ApprovalService service) {
        this.service = service;
    }

    @PostMapping
    public ApprovalResponse create(@RequestBody CreateApprovalRequest request) {
        return ApprovalResponse.from(service.create(
                request.title(),
                request.content(),
                request.type(),       // type: 1=지출 2=휴가 3=구매 4=기타 (이번 범위 밖 — 그대로 int)
                request.priority(),   // priority: 1=낮음 2=보통 3=높음 (이번 범위 밖 — 그대로 int)
                request.drafterId(),
                request.approverId(),
                request.amount(),
                request.urgent()
        ));
    }

    // action: 1=상신, 2=승인, 3=반려, 9=취소  (이번 범위 밖 — API 계약 그대로 유지)
    @PostMapping("/{id}/process")
    public void process(@PathVariable Long id, @RequestBody ProcessRequest request) {
        service.processApproval(
                id,
                request.userId(),
                request.action(),
                request.reason()
        );
    }

    @GetMapping("/drafts/{userId}")
    public List<ApprovalResponse> drafts(@PathVariable Long userId) {
        return service.myDrafts(userId).stream().map(ApprovalResponse::from).toList();
    }

    @GetMapping("/inbox/{userId}")
    public List<ApprovalResponse> inbox(@PathVariable Long userId) {
        return service.myInbox(userId).stream().map(ApprovalResponse::from).toList();
    }
}
