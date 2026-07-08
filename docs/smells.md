# smells.md — 진단 결과 종합 인벤토리

[4-4](./4-4.%20코드베이스%20구조%20파악.md)~[4-12](./4-12.%20우선순위%20매트릭스.md)에서 나온 진단 결과를 리팩토링 백로그의 근거 자료로 쓸 수 있게 하나의 표로 모은 문서다. 새로운 탐지는 없으며, 각 항목은 원본 문서의 파일:라인 근거를 그대로 인용한다.

> **주의**: [4-10](./4-10.%20도출%20스멜%20-%20심각도%20표.md)의 "검증 상태"는 아직 전부 `검토 필요` 상태다(팀 합의 전). 이 인벤토리의 영향도·우선순위는 AI 1차 판단이며, 실제 착수 전 4-10에서 검증·합의를 거치는 것이 원칙이다.

## 스멜 인벤토리

| # | 스멜 | 위치(파일:라인) | 개수 | 영향도 | 난이도 | 예상 공수 | 착수 단계(4-12) | 근거 문서 |
|---|---|---|---|---|---|---|---|---|
| 1 | No Tests | 프로젝트 전체 (테스트 코드 0개) | 전체 | 치명적 | 중 | 2~3일 | 1단계 | 4-10 |
| 2 | Tight Coupling (직접 `new`) | `ApprovalService.java:37-38`, `NoticeService.java:24-25`, `ScheduleService.java:20` | 5 | 중 | 하 | 0.5일 | 2단계 | 4-9, 4-10 |
| 3 | Duplicated Code — 감사 로그 조립 | `ApprovalService.java:62-65,163-166`, `NoticeService.java:44-45,69-70`, `ScheduleService.java:61-63,76-77` | 6 | 상 | 중 | 1~2일 (전체 Duplicated Code 묶음) | 3단계 | 4-5, 4-10 |
| 4 | Duplicated Code — 메일 본문 생성 | `ApprovalService.java:103-106,121-123,140-143`, `NoticeService.java:64-66` | 4 | 상 | 중 | 〃 | 3단계 | 4-5, 4-10 |
| 5 | Duplicated Code — 권한 판정 `role>=2` | `ApprovalService.java:114,133`, `NoticeService.java:56` | 3 | 상 | 중 | 〃 | 3단계 | 4-5, 4-10 |
| 6 | Duplicated Code — 직접 `new` (2번과 동일 지점, 강결합·중복 양쪽에서 언급) | 위 2번과 동일 | 5 | 상 | 중 | 〃 | 2·3단계 | 4-5, 4-9 |
| 7 | Duplicated Code — status→라벨 변환 | `ApprovalService.java:169-179`, `NoticeService.java:76-82` | 2 | 상 | 중 | 〃 | 3단계 | 4-5, 4-10 |
| 8 | Magic Number — 금액 임계값 중복 정의 (버그 위험) | `ApprovalService.java:94`(100만원), `ApprovalService.java:184-186`(1000만/100만/10만원) | 4개 리터럴 | 상(하위 위험) | 중 | 2일 (전체 Magic Number 묶음) | 5단계 | 4-8, 4-10 |
| 9 | Magic Number — 결재 status (0/1/2/3/9) | `Approval.java:26`, `ApprovalService.java` 전역 | 5개 값 | 중 | 중 | 〃 | 5단계 | 4-8 |
| 10 | Magic Number — 결재 action 코드 (API 계약에 노출) | `ApprovalController.java:36`, `ApprovalService.java:90,110,129,147` | 4개 값 | 중 | 중 | 〃 | 5단계 | 4-8 |
| 11 | Magic Number — 공지 status/category | `Notice.java:19-20`, `NoticeService.java` | 6개 값 | 중 | 중 | 〃 | 5단계 | 4-8 |
| 12 | Magic Number — 일정 status | `Schedule.java:18`, `ScheduleService.java` | 3개 값 | 중 | 중 | 〃 | 5단계 | 4-8 |
| 13 | Magic Number — role 등급 및 임계값 `role>=2` | `User.java:19`, `DataSeeder.java:24-27` | 3개 값 | 중 | 중 | 〃 | 5단계 | 4-8 |
| 14 | Magic Number — priority | `Approval.java:27`, `ApprovalService.java:52,95` | 3개 값 | 하 | 중 | 〃 | 5단계 | 4-8 |
| 15 | God Class | `ApprovalService` 전체 (`ApprovalService.java`) | 클래스 1개, 책임 6개 | 상 | 상 | 3~4일 | 4단계 | 4-4, 4-10 |
| 16 | Long Method | `ApprovalService.processApproval()` (`ApprovalService.java:75-160`) | 86줄, 중첩 깊이 6 | 상 | 상 | 1~2일 (God Class 작업에 포함) | 4단계 | 4-7, 4-10 |
| 17 | Feature Envy | `ApprovalService.statusLabel()/amountGrade()`(`ApprovalService.java:169-188`), `NoticeService.statusLabel()`(`NoticeService.java:76-82`) | 3개 메서드 | 중 | 하 | 0.5일 | 6단계 | 4-4, 4-5, 4-10 |
| 18 | Long Parameter List | `ApprovalService.create()` (`ApprovalService.java:46-47`) | 파라미터 8개 | 하 | 하 | 0.5일 | 7단계 | 4-4, 4-10 |
| 19 | Poor Naming | `ApprovalService`(`d,u,s,proc,tmp`), `ScheduleService`(`flag1,s,sc`) 전반 | 다수 | 하 | 하 | 별도 공수 없음 | 7단계 | 4-4, 4-10 |
| 20 | Comment Smell | 매직넘버·나쁜 이름을 변명하는 주석 다수 | 다수 | 하 | 하 | 별도 공수 없음 | 7단계 | 4-8, 4-10 |

## 근거 문서 목록

- [4-4. 코드베이스 구조 파악](./4-4.%20코드베이스%20구조%20파악.md) — 패키지·클래스 책임
- [4-5. 중복 코드 탐지](./4-5.%20중복%20코드%20탐지.md) — 중복 6유형
- [4-7. 긴 메서드 탐지](./4-7.%20긴%20메서드%20탐지.md) — 50줄 초과·중첩 깊이
- [4-8. 매직넘버 탐지](./4-8.%20매직넘버%20탐지.md) — status/action/type/category/role/priority/금액 임계값
- [4-9. 강결합 탐지](./4-9.%20강결합%20탐지.md) — 직접 `new` 5곳
- [4-10. 도출 스멜 - 심각도 표](./4-10.%20도출%20스멜%20-%20심각도%20표.md) — 검증·합의 기준선
- [4-11. 심각도 스코어링](./4-11.%20심각도%20스코어링.md) — 영향도×수정난이도 매트릭스
- [4-12. 우선순위 매트릭스](./4-12.%20우선순위%20매트릭스.md) — 착수 우선순위 로드맵(공수 포함)

## 참고

이 문서는 백로그 작성을 위한 **입력 자료**다. 실제 작업 항목화·순서·완료 조건은 리팩토링 백로그 문서에서 정한다. 모든 작업은 CLAUDE.md 불변 규칙(API 엔드포인트·public 메서드 시그니처·요청/응답 형식·DB 저장값·관찰 가능한 동작 보존)을 지키며 진행한다.
