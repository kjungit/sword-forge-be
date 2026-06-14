# GitHub Actions 설정 한글 설명

이 문서는 `Sword Forge` 백엔드 저장소에 현재 연결해 둔 GitHub Actions 자동화가 각각 무엇을 하는지 설명합니다.

짧게 말하면, 지금 자동화는 아래 목적을 가집니다.

- 코드가 깨졌는지 자동으로 테스트한다.
- 게임 데이터 JSON이 스키마와 서로 맞는지 검사한다.
- 강화/판매/경제 밸런스가 크게 망가지지 않았는지 시뮬레이션한다.
- 보안 취약점 가능성을 CodeQL로 검사한다.
- PR마다 리뷰어가 봐야 할 체크리스트를 자동으로 남긴다.
- `codex/**` 브랜치를 `dev` 브랜치로 PR 생성하도록 돕는다.

영문 버전은 `docs/GITHUB_ACTIONS.md`에 있습니다.

## 전체 구성

| 이름 | 파일 | 하는 일 |
| --- | --- | --- |
| CI | `.github/workflows/ci.yml` | Java 테스트와 게임 데이터 검증을 실행합니다. |
| Balance Simulation | `.github/workflows/balance.yml` | 강화/경제 밸런스 시뮬레이션을 실행하고 결과 파일을 업로드합니다. |
| CodeQL | `.github/workflows/codeql.yml` | Java/Kotlin 코드 보안 정적 분석을 실행합니다. |
| PR Review Checks | `.github/workflows/pr-review.yml` | dependency review를 실행하고 PR에 자동 리뷰 체크리스트 댓글을 남깁니다. |
| Auto PR | `.github/workflows/auto-pr.yml` | `codex/**` 브랜치에서 `dev`로 PR을 만들거나 기존 PR을 재사용합니다. |

추가로 아래 GitHub 설정 파일도 있습니다.

- `.github/pull_request_template.md`: PR을 올릴 때 테스트, 데이터 검증, 밸런스, 마이그레이션, API 문서 반영 여부를 확인하게 하는 템플릿입니다.
- `.github/CODEOWNERS`: 전체 파일의 코드 오너를 `@kjungit`으로 지정합니다.

## CI

파일:

```text
.github/workflows/ci.yml
```

실행되는 경우:

- `main`에 push
- `dev`에 push
- `codex/**`에 push
- `feature/**`에 push
- `main` 대상 PR
- `dev` 대상 PR

실행 내용:

1. 저장소 코드를 checkout합니다.
2. Java 17 Temurin을 설치합니다.
3. Python 3.12를 설치합니다.
4. `gradlew` 실행 권한을 줍니다.
5. Java 테스트를 실행합니다.

```bash
./gradlew test
```

6. 게임 데이터 JSON 검증을 실행합니다.

```bash
python3 scripts/validate_data_schemas.py
```

왜 필요한가:

- Spring Boot 코드가 깨졌는지 빠르게 잡습니다.
- 컨트롤러, 서비스, 보안, 동시성, 게임 플로우 테스트를 자동으로 돌립니다.
- `weapons.json`, `enhance_table.json`, `weapon_sale_prices.json` 같은 데이터 파일들이 스키마와 서로 맞는지 확인합니다.

즉, “이 PR이 최소한 서버 테스트와 데이터 검증은 통과했는가?”를 보는 가장 기본 체크입니다.

## Balance Simulation

파일:

```text
.github/workflows/balance.yml
```

실행되는 경우:

- `main` 대상 PR
- `dev` 대상 PR
- GitHub UI에서 수동 실행

실행 내용:

1. 저장소 코드를 checkout합니다.
2. Python 3.12를 설치합니다.
3. 강화/경제 시뮬레이터를 실행합니다.

```bash
python3 scripts/simulate_balance.py --runs 300 --max-attempts 1000
```

4. `outputs/balance_summary.md` 내용을 GitHub Actions summary에 붙입니다.
5. 아래 결과 파일을 artifact로 업로드합니다.

```text
outputs/balance_result.csv
outputs/balance_summary.md
```

왜 필요한가:

- 이 게임은 강화를 계속 시도하면서 도파민을 느끼는 구조입니다.
- 성공률, 파괴, 판매가, 강화 비용 중 하나가 어긋나면 초반 루프가 막힐 수 있습니다.
- 시뮬레이션은 `normal_01 -> epic_02` 같은 구간을 실제 확률 기반으로 여러 번 돌려, 돈이 너무 빨리 마르거나 진행이 비정상적으로 빡세지는지 확인합니다.

리뷰어는 이 결과를 보고 “이번 밸런스 변경이 유저를 멈추게 만들지는 않는가?”를 확인하면 됩니다.

## CodeQL

파일:

```text
.github/workflows/codeql.yml
```

실행되는 경우:

- `main`에 push
- `dev`에 push
- `main` 대상 PR
- `dev` 대상 PR
- GitHub UI에서 수동 실행

실행 내용:

1. 저장소 코드를 checkout합니다.
2. CodeQL을 Java/Kotlin 모드로 초기화합니다.
3. CodeQL autobuild를 실행합니다.
4. CodeQL 분석을 실행합니다.

왜 필요한가:

- 인증, 권한, CSRF, 데이터 접근, 요청 처리 같은 부분에서 보안상 위험한 패턴을 잡기 위한 자동 검사입니다.
- 이전에 CSRF 설정 문제처럼 보안 쪽은 작은 실수도 중요해서, CodeQL을 PR마다 돌려 두는 게 좋습니다.
- 결과는 GitHub code scanning 쪽에 올라갑니다.

필요 권한:

```yaml
security-events: write
```

## PR Review Checks

파일:

```text
.github/workflows/pr-review.yml
```

실행되는 경우:

- `main` 대상 PR
- `dev` 대상 PR

이 workflow는 두 가지 job으로 나뉩니다.

## Dependency review

사용 action:

```text
actions/dependency-review-action@v5
```

하는 일:

- PR에서 의존성이 변경됐는지 확인합니다.
- 위험한 dependency 변경이 있는지 검토합니다.
- 현재는 `continue-on-error: true`라서 advisory 성격입니다.

왜 advisory인가:

- GitHub Dependency graph 설정이 저장소에서 완전히 활성화되어야 더 강하게 쓸 수 있습니다.
- 지금은 PR을 막기보다는 “의존성 변경을 리뷰어에게 알려주는 장치”로 둔 상태입니다.

추천 GitHub 설정:

```text
Settings > Security > Code security and analysis > Dependency graph
```

## Post review checklist

사용 action:

```text
actions/github-script@v9
```

하는 일:

- PR에 자동 리뷰 체크리스트 댓글을 남깁니다.
- 같은 PR에 다시 실행되면 새 댓글을 계속 만들지 않고 기존 댓글을 업데이트합니다.

자동 댓글에서 확인하라고 알려주는 것:

- Java 테스트
- 게임 데이터 스키마 검증
- 밸런스 시뮬레이션 결과
- dependency 변경
- CodeQL 보안 분석
- 게임 의도
- 경제 밸런스
- API 호환성
- 마이그레이션 안정성

즉, 사람이 리뷰할 때 놓치기 쉬운 항목을 PR마다 자동으로 상기시켜 주는 장치입니다.

## Auto PR

파일:

```text
.github/workflows/auto-pr.yml
```

실행되는 경우:

- `codex/**` 브랜치에 push

하는 일:

1. 현재 push된 `codex/**` 브랜치에서 `dev`로 열린 PR이 이미 있는지 확인합니다.
2. 이미 있으면 그 PR URL을 출력하고 성공 처리합니다.
3. 없으면 아래 형태로 PR 생성을 시도합니다.

```text
base: dev
head: codex/<branch>
title: Auto PR: <branch>
```

4. GitHub Actions 토큰 권한 때문에 PR 생성이 막히면 warning만 남기고 성공 처리합니다.

왜 필요한가:

- Codex 작업 브랜치를 `dev`로 보내는 흐름을 자동화하기 위해서입니다.
- 같은 브랜치에서 PR을 중복 생성하지 않게 합니다.
- 저장소 설정상 GitHub Actions가 PR을 만들 권한이 없어도 전체 체크가 실패하지 않게 처리합니다.

추천 GitHub 설정:

```text
Settings > Actions > General > Workflow permissions
Allow GitHub Actions to create and approve pull requests
```

선택 secret:

```text
PR_AUTOMATION_TOKEN
```

이 secret은 별도 자동화 계정이나 fine-grained token으로 PR 생성 권한을 주고 싶을 때 사용합니다.

수동 대체 방법:

```bash
gh pr create --base dev --head codex/<branch>
```

현재 PR #2도 처음에는 GitHub Actions 토큰 권한 때문에 자동 생성이 실패했고, 이후 수동으로 생성했습니다. 그래서 Auto PR이 권한 부족 때문에 실패하지 않도록 warning 처리로 바꿔두었습니다.

## PR 템플릿

파일:

```text
.github/pull_request_template.md
```

PR을 올릴 때 확인하게 하는 항목:

- 로컬 또는 CI에서 테스트가 통과했는가
- 게임 데이터 스키마 검증이 통과했는가
- 경제/확률 데이터 변경 시 밸런스 시뮬레이션을 확인했는가
- 기존 세이브에 안전한 마이그레이션인가
- API 변경이 문서에 반영됐는가

리뷰 중점:

- 게임 규칙 변경
- 경제 source/sink 변경
- 보안 또는 유저 접근 권한 변경
- 세이브 데이터 호환성

## CODEOWNERS

파일:

```text
.github/CODEOWNERS
```

현재 설정:

```text
* @kjungit
```

의미:

- 저장소의 모든 파일 소유자를 `@kjungit`으로 지정합니다.
- GitHub branch protection을 설정하면 해당 owner 리뷰를 필수로 요구할 수 있습니다.

## 현재 머지 흐름

추천 흐름:

1. `codex/<topic>` 또는 `feature/<topic>` 브랜치에서 작업합니다.
2. 브랜치를 push합니다.
3. `codex/**` 브랜치라면 Auto PR이 `dev` 대상 PR 생성을 시도합니다.
4. 권한 문제로 Auto PR이 PR을 못 만들면 수동으로 PR을 만듭니다.
5. PR에서 아래 체크들이 실행됩니다.
   - CI
   - Balance Simulation
   - CodeQL
   - PR Review Checks
6. 자동 체크리스트 댓글을 확인합니다.
7. 문제 없으면 `dev`에 머지합니다.
8. `dev`가 안정화되면 그 뒤에 `main`으로 승격합니다.

## 아직 설정하지 않은 것

아래 항목들은 아직 일부러 넣지 않았습니다.

- 실제 프로덕션 배포 workflow
- release tag 생성 workflow
- DB 백업 workflow
- DB 마이그레이션 승인 workflow
- 프론트 Godot CI
- 운영 환경별 secret 배포
- 운영 모니터링/알림

이것들은 호스팅 대상과 배포 전략이 정해진 뒤 추가하는 게 맞습니다.

## 지금 기준으로 해 둔 것의 의미

현재 GitHub Actions는 “배포 자동화”까지 완성된 상태는 아닙니다.

대신 지금 단계에서 필요한 아래 기반은 갖춘 상태입니다.

- PR마다 코드가 깨졌는지 확인
- 게임 데이터가 깨졌는지 확인
- 강화 경제 밸런스가 크게 망가지지 않았는지 확인
- 보안 정적 분석 확인
- dependency 변경 확인
- 리뷰어가 봐야 할 항목 자동 안내
- Codex 작업 브랜치를 `dev`로 보내는 흐름 정리

즉, 현재 자동화의 목적은 “바로 운영 배포”가 아니라 “개발 중 실수 방지와 PR 리뷰 품질 확보”입니다.
