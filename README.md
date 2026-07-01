# 사내 업무 포털 (Legacy) — 클린코드 & 리팩토링 실습 코드

KT DS 「클린코드 & 리팩토링 실전」 2일 과정의 **관통 실습 코드베이스**입니다.
결재 / 공지 / 일정 세 도메인을 가진 Spring Boot 애플리케이션이며,
**의도적으로 나쁜 코드(코드 스멜)** 를 심어 두었습니다. 2일 동안 Claude Code로 이 코드를
단계적으로 리팩토링합니다.

## 기술 스택
- Java 21 / Spring Boot 3.2 / Spring Data JPA / JUnit 5
- 실행 DB = MariaDB(localhost:3306, db=portal) · 테스트 = H2 인메모리 자동

## 사전 준비
- JDK 21 이상
- Maven 설치 불필요 - 프로젝트의 **Maven Wrapper(mvnw)** 가 자동 처리
- MariaDB (Windows=WSL Ubuntu / Mac=Homebrew, 계정 portal/portal1234)
- Claude Code CLI + Anthropic API Key

## 실행 (Maven 설치 불필요)
```bat
:: 그냥 실행하면 DB 를 자동 선택한다 (DataSourceConfig.java)
::   - MariaDB 가 떠 있으면 → MariaDB 사용
::   - MariaDB 가 없으면     → H2 인메모리로 자동 실행 (설치 0, 입력도 0)
.\mvnw.cmd spring-boot:run
```
- 시작 로그에 `>> DB = MariaDB` 또는 `>> ... → H2` 가 찍혀 어느 DB 인지 바로 보인다.
- H2 로 떴을 때 데이터 조회: http://localhost:8080/h2-console (JDBC URL: jdbc:h2:mem:portal, 사용자 sa)
- 테스트: `.\mvnw.cmd test` (H2 인메모리 자동)

## 빠른 동작 확인 (예: 결재 상신)
```bash
# 1) 결재 생성 (기안자=1 김사원, 결재자=2 박팀장, 지출 120만원)
curl -X POST localhost:8080/api/approvals -H "Content-Type: application/json" \
  -d '{"title":"노트북 구매","content":"개발용","type":1,"priority":2,"drafterId":1,"approverId":2,"amount":1200000}'

# 2) 상신 (action=1)
curl -X POST localhost:8080/api/approvals/1/process -H "Content-Type: application/json" \
  -d '{"userId":1,"action":1}'

# 3) 승인 (action=2, 박팀장)
curl -X POST localhost:8080/api/approvals/1/process -H "Content-Type: application/json" \
  -d '{"userId":2,"action":2}'
```

## 초기 사용자
| id | 이름 | role | 의미 |
|----|------|------|------|
| 1 | 김사원 | 1 | 사원 |
| 2 | 박팀장 | 2 | 팀장 |
| 3 | 이임원 | 3 | 임원 |
| 4 | 최사원 | 1 | 사원 |

## 의도적으로 심어둔 코드 스멜 (요약)
| 위치 | 스멜 |
|------|------|
| `ApprovalService` | God Class, Long Method(`processApproval`), Magic Number, Tight Coupling, Feature Envy |
| `Approval`/`Notice`/`Schedule` | Anemic Domain Model, Primitive Obsession, 캡슐화 부재 |
| `NoticeService`/`ScheduleService` | 메일/감사로그 **중복 코드** |
| 전체 | 테스트 0개 (리팩토링 안전망 부재) |
| `ApprovalController` | 요청 DTO 부재, 매직넘버 action 노출 |

> 상세한 단계별 리팩토링 절차와 Claude Code 프롬프트는 상위 폴더의
> **`REFACTORING_GUIDE.md`** 를 참고하세요.
