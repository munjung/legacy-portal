package com.ktds.portal.approval;

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
 *  3. Magic Number         : type 1~4, role 1~3, priority 1~3 이 흩어져 있다.
 *     [리팩토링] status(0/1/2/3/9)는 ApprovalStatus enum으로 전환함(아래 참조). type/role/priority는 이번 범위 밖.
 *  4. Duplicated Code      : 메일 본문 생성/감사 로그 기록이 메서드마다 복붙 되어 있다.
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
     * 결재 처리 — 상신/승인/반려/취소를 action 코드로 분기한다.
     * [스멜2] 이 메서드 하나가 모든 일을 한다. [스멜1][스멜6] 규칙 계산을 서비스가 떠안는다.
     *
     * action: 1=상신, 2=승인, 3=반려, 9=취소
     */
    public void processApproval(Long id, Long userId, int action, String reason) {
        Approval approval = repo.findById(id).orElse(null);   // [리팩토링] d → approval
        if (approval == null) {
            // [스멜] 예외 대신 조용히 리턴 — 호출자는 실패를 알 수 없다. (레거시 동작 보존 — 재설계 단계에서 다룰 대상)
            return;
        }
        User actor = userRepo.findById(userId).orElse(null);   // [리팩토링] u → actor (요청을 수행하는 사용자라는 의미를 드러냄)
        if (actor == null) {
            return;
        }

        ApprovalStatus status = approval.getStatus();     // [리팩토링] int s → ApprovalStatus status (매직넘버 제거, 이하 전부 enum 비교)

        // [스멜2] 거대한 if-지옥(구조는 유지 — 이번 범위는 이름·매직넘버만). action 비교만 proc 없이 직접 수행.
        if (action == 1) {            // action==1 → 상신 (숫자 1을 외워야 의미를 앎)
            // 상신: 임시저장(DRAFT)일 때만 가능
            if (status == ApprovalStatus.DRAFT) {   // [리팩토링] status==0 → status == ApprovalStatus.DRAFT
                // [스멜6] 금액 기준 결재자 자동 상향 — 도메인 규칙이 서비스에 박혀 있다.
                if (approval.getType() == 1 && approval.getAmount() >= 1000000) {   // type 1=지출·2=휴가·3=구매·4=기타 → type==1(지출) && 100만원↑
                    approval.setPriority(3);   // 3 = 높음
                }
                approval.setStatus(ApprovalStatus.SUBMITTED);   // [리팩토링] 매직넘버 1 → ApprovalStatus.SUBMITTED
                approval.setUpdatedAt(LocalDateTime.now());
                repo.save(approval);
                // [스멜4] 메일 발송 — 본문 생성 로직이 곳곳에 복붙.
                User approver = userRepo.findById(approval.getApproverId()).orElse(null);
                if (approver != null) {
                    String body = "안녕하세요 " + approver.getName() + "님,\n"
                            + "결재 요청이 도착했습니다.\n제목: " + approval.getTitle()
                            + "\n기안자ID: " + approval.getDrafterId();
                    mail.send(approver.getEmail(), "[결재요청] " + approval.getTitle(), body);
                }
                writeAudit("APPROVAL SUBMIT", approval.getId(), userId);
            }
        } else if (action == 2) {     // action==2 → 승인
            // 승인: 상신(SUBMITTED) 상태 + 본인이 결재자 + 권한(role>=2) 일 때만
            if (status == ApprovalStatus.SUBMITTED) {   // [리팩토링] status==1 → status == ApprovalStatus.SUBMITTED
                if (approval.getApproverId() != null && approval.getApproverId().equals(userId)) {
                    if (actor.getRole() >= 2) {   // role 1=사원·2=팀장·3=임원 (role>=2 승인권한)  [스멜3: 숫자로 권한 판정]
                        approval.setStatus(ApprovalStatus.APPROVED);   // [리팩토링] 매직넘버 2 → ApprovalStatus.APPROVED
                        approval.setUpdatedAt(LocalDateTime.now());
                        repo.save(approval);
                        // [스멜4] 또 복붙된 메일 발송
                        User drafter = userRepo.findById(approval.getDrafterId()).orElse(null);
                        if (drafter != null) {
                            String body = "안녕하세요 " + drafter.getName() + "님,\n"
                                    + "결재가 승인되었습니다.\n제목: " + approval.getTitle();
                            mail.send(drafter.getEmail(), "[결재승인] " + approval.getTitle(), body);
                        }
                        writeAudit("APPROVAL APPROVE", approval.getId(), userId);
                    }
                }
            }
        } else if (action == 3) {     // action==3 → 반려
            // 반려: 상신(SUBMITTED) 상태만 가능
            if (status == ApprovalStatus.SUBMITTED) {   // [리팩토링] status==1 → status == ApprovalStatus.SUBMITTED
                if (approval.getApproverId() != null && approval.getApproverId().equals(userId)) {
                    if (actor.getRole() >= 2) {   // role>=2 → 팀장 이상 (위 승인 분기와 똑같은 판정 복붙)
                        approval.setStatus(ApprovalStatus.REJECTED);   // [리팩토링] 매직넘버 3 → ApprovalStatus.REJECTED
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
                        writeAudit("APPROVAL REJECT", approval.getId(), userId);
                    }
                }
            }
        } else if (action == 9) {     // action==9 → 취소 (왜 9? 4~8 은 비워둔 규칙 없는 번호)
            // 취소: 기안자 본인 + 아직 승인 전(DRAFT 또는 SUBMITTED)
            if (status == ApprovalStatus.DRAFT || status == ApprovalStatus.SUBMITTED) {   // [리팩토링] 0/1 비교 → enum 비교
                if (approval.getDrafterId() != null && approval.getDrafterId().equals(userId)) {
                    approval.setStatus(ApprovalStatus.CANCELED);   // [리팩토링] 매직넘버 9 → ApprovalStatus.CANCELED
                    approval.setUpdatedAt(LocalDateTime.now());
                    repo.save(approval);
                    writeAudit("APPROVAL CANCEL", approval.getId(), userId);
                }
            }
        }
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
