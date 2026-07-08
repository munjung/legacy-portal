package com.ktds.portal.approval;

import com.ktds.portal.user.User;
import com.ktds.portal.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

/**
 * [특성화 테스트 / 리팩토링 전 안전망]
 * ApprovalService.processApproval(id, userId, action, reason) 의 "현재" 동작을 그대로 고정한다.
 * 옳고 그름을 판단하지 않는다 — 레거시가 지금 이렇게 동작한다는 사실(observable behavior)만 기록한다.
 * 리팩토링 후에도 이 6개 테스트는 수정 없이 그대로 green 이어야 한다 (CLAUDE.md 검증 원칙).
 *
 * @DataJpaTest 는 Entity/Repository 만 스캔하므로, @Service 인 ApprovalService 는
 * @Import 로 직접 컨텍스트에 포함시킨다 (new ApprovalService(...) 로 직접 생성하지 않는다).
 */
@DataJpaTest
@Import(ApprovalService.class)
class ApprovalProcessCharacterizationTest {

    @Autowired
    private ApprovalService approvalService;

    @Autowired
    private ApprovalRepository approvalRepository;

    @Autowired
    private UserRepository userRepository;

    private User 사용자_저장(String name, String email, int role) {
        return userRepository.save(new User(name, email, role, "테스트팀"));
    }

    // 1) 상신 → 승인
    @Test
    void 상신_후_승인하면_상태가_승인으로_바뀐다() {
        User 기안자 = 사용자_저장("김기안", "drafter1@ktds.com", 1);
        User 결재자 = 사용자_저장("박결재", "approver1@ktds.com", 2);
        Approval 문서 = approvalService.create("출장비 정산", "내용", 1, 2,
                기안자.getId(), 결재자.getId(), 300_000L, false);

        approvalService.processApproval(문서.getId(), 기안자.getId(), 1, null);
        assertThat(approvalRepository.findById(문서.getId()).orElseThrow().getStatus()).isEqualTo(1);

        approvalService.processApproval(문서.getId(), 결재자.getId(), 2, null);
        assertThat(approvalRepository.findById(문서.getId()).orElseThrow().getStatus()).isEqualTo(2);
    }

    // 2) 반려
    @Test
    void 상신_후_반려하면_상태와_반려사유가_저장된다() {
        User 기안자 = 사용자_저장("최기안", "drafter2@ktds.com", 1);
        User 결재자 = 사용자_저장("이결재", "approver2@ktds.com", 2);
        Approval 문서 = approvalService.create("구매 요청", "내용", 3, 2,
                기안자.getId(), 결재자.getId(), 200_000L, false);

        approvalService.processApproval(문서.getId(), 기안자.getId(), 1, null);
        approvalService.processApproval(문서.getId(), 결재자.getId(), 3, "예산 초과");

        Approval 결과 = approvalRepository.findById(문서.getId()).orElseThrow();
        assertThat(결과.getStatus()).isEqualTo(3);
        assertThat(결과.getRejectReason()).isEqualTo("예산 초과");
    }

    // 3) 취소
    @Test
    void 상신_상태에서_기안자가_취소하면_상태가_취소로_바뀐다() {
        User 기안자 = 사용자_저장("정기안", "drafter3@ktds.com", 1);
        User 결재자 = 사용자_저장("한결재", "approver3@ktds.com", 2);
        Approval 문서 = approvalService.create("휴가 신청", "내용", 2, 1,
                기안자.getId(), 결재자.getId(), 0L, false);

        approvalService.processApproval(문서.getId(), 기안자.getId(), 1, null);
        approvalService.processApproval(문서.getId(), 기안자.getId(), 9, null);

        assertThat(approvalRepository.findById(문서.getId()).orElseThrow().getStatus()).isEqualTo(9);
    }

    // 4) 권한없는 승인 (지정된 결재자가 아닌 사용자가 시도)
    @Test
    void 지정된_결재자가_아닌_사용자가_승인을_시도하면_조용히_무시된다() {
        User 기안자 = 사용자_저장("오기안", "drafter4@ktds.com", 1);
        User 결재자 = 사용자_저장("서결재", "approver4@ktds.com", 2);
        User 제3자 = 사용자_저장("남임원", "outsider@ktds.com", 3);
        Approval 문서 = approvalService.create("기타 요청", "내용", 4, 1,
                기안자.getId(), 결재자.getId(), 0L, false);

        approvalService.processApproval(문서.getId(), 기안자.getId(), 1, null);
        approvalService.processApproval(문서.getId(), 제3자.getId(), 2, null);

        assertThat(approvalRepository.findById(문서.getId()).orElseThrow().getStatus()).isEqualTo(1);
    }

    // 5) 없는 id
    @Test
    void 존재하지_않는_문서_id로_처리하면_예외없이_조용히_무시된다() {
        long 존재하지_않는_id = 999_999L;

        assertDoesNotThrow(() -> approvalService.processApproval(존재하지_않는_id, 1L, 1, null));
        assertThat(approvalRepository.findById(존재하지_않는_id)).isEmpty();
    }

    // 6) 재승인
    @Test
    void 이미_승인된_문서를_다시_승인해도_상태는_그대로_유지된다() {
        User 기안자 = 사용자_저장("윤기안", "drafter5@ktds.com", 1);
        User 결재자 = 사용자_저장("장결재", "approver5@ktds.com", 2);
        Approval 문서 = approvalService.create("지출 결의", "내용", 1, 1,
                기안자.getId(), 결재자.getId(), 50_000L, false);

        approvalService.processApproval(문서.getId(), 기안자.getId(), 1, null);
        approvalService.processApproval(문서.getId(), 결재자.getId(), 2, null);
        approvalService.processApproval(문서.getId(), 결재자.getId(), 2, null);

        assertThat(approvalRepository.findById(문서.getId()).orElseThrow().getStatus()).isEqualTo(2);
    }
}
