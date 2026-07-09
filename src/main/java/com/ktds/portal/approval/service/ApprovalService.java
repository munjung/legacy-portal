package com.ktds.portal.approval.service;

import com.ktds.portal.approval.domain.Approval;
import com.ktds.portal.approval.domain.ApprovalStatus;
import com.ktds.portal.approval.repository.ApprovalRepository;
import com.ktds.portal.common.FileAuditLogger;
import com.ktds.portal.common.SmtpMailSender;
import com.ktds.portal.user.User;
import com.ktds.portal.user.UserRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * 결재 서비스 — 이 클래스가 이 과정의 "주인공 안티패턴"이다.
 *
 * ============================ 의도적으로 심어둔 스멜 목록 ============================
 *  1. God Class            : 검증 + 영속화 + 메일 + 감사로그 + 포맷팅 + 권한판정을 혼자 다 한다.
 *  2. Long Method          : processApproval() 한 메서드가 100줄 이상, 중첩 if 6단계.
 *     [리팩토링] Guard Clause + Extract Method 로 submit/approve/reject/cancel/notifyApprover 로 분해함(아래 참조).
 *     processApproval 은 약 86줄 → 약 12줄(조율부만 남김). 남은 God Class·중복 메일 발송은 이번 범위 밖.
 *  3. Magic Number         : type 1~4, role 1~3, priority 1~3 이 흩어져 있다.
 *     [리팩토링] status(0/1/2/3/9)는 ApprovalStatus enum으로 전환함(아래 참조). type/role/priority는 이번 범위 밖.
 *  4. Duplicated Code      : 메일 본문 생성/감사 로그 기록이 메서드마다 복붙 되어 있다.
 *     [참고] submit() 의 결재자 메일 발송만 notifyApprover() 로 추출함 — approve/reject 의 드래프터 메일 발송은
 *            서로 다른 수신자·본문이라 이번 챕터(메서드 분해) 범위에서는 그대로 둔다(중복 제거는 별도 챕터 대상).
 *  5. Tight Coupling       : new SmtpMailSender(), new FileAuditLogger() 직접 생성(DI 없음).
 *  6. Feature Envy         : Approval 의 필드를 꺼내 서비스가 직접 상태/금액 규칙을 계산한다.
 *  7. Primitive Obsession  : 모든 분기를 int 비교로 처리한다.
 *  8. Long Parameter List  : create() 파라미터 8개.
 *  9. Poor Naming          : d, u, proc, tmp 같은 약어.
 *     [리팩토링] d→approval, u→actor, s→status, proc 제거(action 직접 사용)로 개선함 (아래 참조).
 * 10. Comment Smell        : 나쁜 이름을 주석으로 변명한다.
 * 11. No Tests             : 테스트가 단 한 개도 없다(안전망 부재).
 * =================================================================================
 */
@Service
public class ApprovalService {

    private final ApprovalRepository repo;
    private final UserRepository userRepo;

    // [스멜5] 강결합 — 협력 객체를 생성자 주입 없이 직접 new 한다. 테스트에서 갈아끼울 수 없다.
    private final SmtpMailSender mail = new SmtpMailSender();
    private final FileAuditLogger audit = new FileAuditLogger();

    public ApprovalService(ApprovalRepository repo, UserRepository userRepo) {
        this.repo = repo;
        this.userRepo = userRepo;
    }

    // [스멜8] 파라미터 8개.
    public Approval create(String title, String content, int type, int priority,
                           Long drafterId, Long approverId, long amount, boolean urgent) {
        Approval approval = new Approval();   // [리팩토링] d → approval (약어 제거, 의미 있는 이름)
        approval.setTitle(title);
        approval.setContent(content);
        approval.setType(type);
        approval.setPriority(urgent ? 3 : priority);   // priority(우선순위): 1 낮음·2 보통·3 높음  [스멜3: 3=높음, 왜 3이 높음? 코드만 봐선 모름]
        approval.setStatus(ApprovalStatus.DRAFT);   // [리팩토링] 매직넘버 0 → ApprovalStatus.DRAFT (DB엔 컨버터가 여전히 0 저장)
        approval.setDrafterId(drafterId);
        approval.setApproverId(approverId);
        approval.setAmount(amount);
        approval.setCreatedAt(LocalDateTime.now());
        approval.setUpdatedAt(LocalDateTime.now());
        repo.save(approval);

        // [스멜4] 감사 로그 기록 — 이 6줄이 submit/approve/reject/cancel 에도 복붙 되어 있다.
        String now = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        String line = "[" + now + "] APPROVAL CREATE id=" + approval.getId()
                + " by=" + drafterId + " type=" + approval.getType();
        audit.write(line);
        return approval;
    }

    /**
     * [리팩토링] 레거시 중첩 if-지옥(약 86줄, 중첩 4~5단계) → Guard Clause + Extract Method 로 분해.
     * (KT DS 클린코드&리팩토링 Day1 교재 7장 "메서드 수준 리팩토링" 그대로 적용)
     *
     * - processApproval 은 "누구를 부를지"만 결정하는 조율부로 슬림화 (action → submit/approve/reject/cancel 위임).
     * - public 시그니처(id, userId, action, reason)·리턴 타입·호출부는 절대 안 바꿈 (불변 규칙).
     * - 각 상태 전이 규칙(가드 조건)·DB 저장값·메일·감사로그는 원래 로직과 1:1 그대로 옮김 — 동작 변경 없음.
     *
     * action: 1=상신, 2=승인, 3=반려, 9=취소
     */
    public void processApproval(Long id, Long userId, int action, String reason) {
        Approval approval = repo.findById(id).orElse(null);
        User actor = userRepo.findById(userId).orElse(null);
        // [리팩토링] 분리돼 있던 두 개의 null 가드(조기 반환)를 하나로 통합 — 이후 본문은 not-null 전제로 진행
        if (approval == null || actor == null) {
            return;   // [스멜] 예외 대신 조용히 리턴 — 호출자는 실패를 알 수 없다. (레거시 동작 보존 — 재설계 단계에서 다룰 대상)
        }

        // [리팩토링] switch(action) 이 "누구를 부를지"만 결정 — 상태 전이 규칙은 각 메서드 안으로 이동
        switch (action) {
            case 1 -> submit(approval, actor);
            case 2 -> approve(approval, actor);
            case 3 -> reject(approval, actor, reason);
            case 9 -> cancel(approval, actor);
            // 1/2/3/9 외의 값은 레거시와 동일하게 아무 분기도 타지 않고 조용히 끝난다(default 없음 = 원래도 없던 동작)
        }
    }

    // [리팩토링] 메서드 추출 - 상신 처리만 하는 메서드. 가드 절로 "임시저장이 아니면 조기 반환".
    private void submit(Approval approval, User actor) {
        if (approval.getStatus() != ApprovalStatus.DRAFT) {   // 예: 이미 상신/승인된 문서면 여기서 조기 반환(원래 로직과 동일)
            return;
        }
        // [스멜6] 금액 기준 결재자 자동 상향 — 도메인 규칙이 서비스에 박혀 있다.
        if (approval.getType() == 1 && approval.getAmount() >= 1000000) {   // type 1=지출·2=휴가·3=구매·4=기타 → type==1(지출) && 100만원↑
            approval.setPriority(3);   // 3 = 높음
        }
        approval.setStatus(ApprovalStatus.SUBMITTED);
        approval.setUpdatedAt(LocalDateTime.now());
        repo.save(approval);
        notifyApprover(approval);   // [리팩토링] 메일 발송 블록을 메서드로 추출(교재 7-9 "메일 공통화")
        writeAudit("APPROVAL SUBMIT", approval.getId(), actor.getId());
    }

    // [리팩토링] 메서드 추출 - submit() 에서만 쓰는 결재자 알림 메일. 블록 1개 = 메서드 1개(이름이 주석 대체).
    private void notifyApprover(Approval approval) {
        User approver = userRepo.findById(approval.getApproverId()).orElse(null);
        if (approver != null) {
            String body = "안녕하세요 " + approver.getName() + "님,\n"
                    + "결재 요청이 도착했습니다.\n제목: " + approval.getTitle()
                    + "\n기안자ID: " + approval.getDrafterId();
            mail.send(approver.getEmail(), "[결재요청] " + approval.getTitle(), body);
        }
    }

    // [리팩토링] 메서드 추출 - 승인 처리. 중첩 if 3단(상태·본인·권한) → 가드 절 3개로 평탄화.
    private void approve(Approval approval, User actor) {
        if (approval.getStatus() != ApprovalStatus.SUBMITTED) {   // 상신 상태일 때만 승인 가능
            return;
        }
        if (approval.getApproverId() == null || !approval.getApproverId().equals(actor.getId())) {   // 지정 결재자 본인만
            return;
        }
        if (actor.getRole() < 2) {   // role 1=사원·2=팀장·3=임원 (role>=2 승인권한)  [스멜3: 숫자로 권한 판정]
            return;
        }
        approval.setStatus(ApprovalStatus.APPROVED);
        approval.setUpdatedAt(LocalDateTime.now());
        repo.save(approval);
        // [스멜4] 복붙된 메일 발송 — approve/reject 는 수신자·본문이 달라 이번 챕터에서는 추출하지 않음(위 클래스 주석 참조)
        User drafter = userRepo.findById(approval.getDrafterId()).orElse(null);
        if (drafter != null) {
            String body = "안녕하세요 " + drafter.getName() + "님,\n"
                    + "결재가 승인되었습니다.\n제목: " + approval.getTitle();
            mail.send(drafter.getEmail(), "[결재승인] " + approval.getTitle(), body);
        }
        writeAudit("APPROVAL APPROVE", approval.getId(), actor.getId());
    }

    // [리팩토링] 메서드 추출 - 반려 처리. approve() 와 동일한 가드 3단(스멜4 중복은 그대로 보존).
    private void reject(Approval approval, User actor, String reason) {
        if (approval.getStatus() != ApprovalStatus.SUBMITTED) {   // 상신 상태만 반려 가능
            return;
        }
        if (approval.getApproverId() == null || !approval.getApproverId().equals(actor.getId())) {   // 지정 결재자 본인만
            return;
        }
        if (actor.getRole() < 2) {   // role>=2 → 팀장 이상
            return;
        }
        approval.setStatus(ApprovalStatus.REJECTED);
        approval.setRejectReason(reason);
        approval.setUpdatedAt(LocalDateTime.now());
        repo.save(approval);
        User drafter = userRepo.findById(approval.getDrafterId()).orElse(null);
        if (drafter != null) {
            String body = "안녕하세요 " + drafter.getName() + "님,\n"
                    + "결재가 반려되었습니다.\n제목: " + approval.getTitle()
                    + "\n사유: " + reason;
            mail.send(drafter.getEmail(), "[결재반려] " + approval.getTitle(), body);
        }
        writeAudit("APPROVAL REJECT", approval.getId(), actor.getId());
    }

    // [리팩토링] 메서드 추출 - 취소 처리. 기안자 본인 + 아직 승인 전(DRAFT 또는 SUBMITTED)만 허용.
    private void cancel(Approval approval, User actor) {
        if (approval.getStatus() != ApprovalStatus.DRAFT && approval.getStatus() != ApprovalStatus.SUBMITTED) {
            return;
        }
        if (approval.getDrafterId() == null || !approval.getDrafterId().equals(actor.getId())) {
            return;
        }
        approval.setStatus(ApprovalStatus.CANCELED);
        approval.setUpdatedAt(LocalDateTime.now());
        repo.save(approval);
        writeAudit("APPROVAL CANCEL", approval.getId(), actor.getId());
    }

    // [스멜4] 그나마 추출했지만 create() 안에는 또 복붙이 남아 있다(불완전한 중복 제거).
    private void writeAudit(String act, Long id, Long userId) {
        String now = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        audit.write("[" + now + "] " + act + " id=" + id + " by=" + userId);
    }

    // [리팩토링] tmp 변수 + 5분기 if-else(스멜1 Feature Envy, 스멜10 Comment Smell) 제거 → ApprovalStatus.label() 에 위임.
    // "알수없음" 분기가 사라진 이유: 레거시 setStatus() 호출은 모두 0/1/2/3/9만 사용해 실제로는 도달 불가능한 방어 코드였고,
    // enum 도입으로 애초에 그 외의 값을 표현할 수 없어졌다(구조적으로 불가능 = 매직넘버 제거의 목적 그 자체).
    public String statusLabel(Approval approval) {
        return approval.getStatus().label();
    }

    // [스멜6] Feature Envy — Approval 데이터를 꺼내 금액 등급을 서비스가 계산.
    public String amountGrade(Approval approval) {   // [리팩토링] d → approval
        long a = approval.getAmount();   // a = amount(금액, 원)  [스멜9: 한 글자 약어 — 이번 범위 밖, 그대로 둠]
        if (a >= 10000000) return "S";   // [스멜3] 1000만원=S — 기준 숫자의 의미가 코드에 없음
        else if (a >= 1000000) return "A";   // 100만원=A
        else if (a >= 100000) return "B";    // 10만원=B
        else return "C";
    }

    public List<Approval> myDrafts(Long userId) {
        return repo.findByDrafterId(userId);
    }

    public List<Approval> myInbox(Long userId) {
        return repo.findByApproverId(userId);
    }
}
