#!/usr/bin/env bun

import {
  existsSync,
  mkdirSync,
  readFileSync,
  readdirSync,
  statSync,
  writeFileSync,
} from "node:fs";
import { dirname, isAbsolute, join, relative, resolve } from "node:path";
import {
  activeIntent,
  activeSpace,
  loadStageGraph,
  parseCheckboxes,
  recordDir,
  resolveProjectDir,
} from "./aidlc-lib.ts";
import { AIDLC_VERSION } from "./aidlc-version.ts";

type StageStatus =
  | "completed"
  | "in-progress"
  | "awaiting-approval"
  | "revising"
  | "pending"
  | "skipped";

type ArtifactLink = {
  name: string;
  href: string;
};

type DashboardStage = {
  slug: string;
  number: string;
  name: string;
  phase: string;
  status: StageStatus;
  purpose: string;
  condition: string;
  leadAgent: string;
  supportAgents: string[];
  inputs: string;
  outputs: string;
  artifacts: ArtifactLink[];
};

const PHASES = ["initialization", "ideation", "inception", "construction", "operation"] as const;

const PHASE_META: Record<string, { label: string; description: string }> = {
  initialization: {
    label: "Initialization",
    description: "워크스페이스와 상태 추적 기반을 준비합니다.",
  },
  ideation: {
    label: "Ideation",
    description: "문제, Scope, 타당성, 팀과 초기 경험을 합의합니다.",
  },
  inception: {
    label: "Inception",
    description: "요구사항을 설계·Unit of Work·실행 계획으로 구체화합니다.",
  },
  construction: {
    label: "Construction",
    description: "Bolt별 설계, 구현, 테스트와 CI를 수행합니다.",
  },
  operation: {
    label: "Operation",
    description: "배포, 관측성, 운영 검증과 피드백 루프를 완성합니다.",
  },
};

const STAGE_NAMES: Record<string, string> = {
  "workspace-scaffold": "Workspace Scaffold",
  "workspace-detection": "Workspace Detection",
  "state-init": "State Initialization",
  "intent-capture": "Intent Capture",
  "market-research": "Market Research",
  feasibility: "Feasibility",
  "scope-definition": "Scope Definition",
  "team-formation": "Team Formation",
  "rough-mockups": "Rough Mockups",
  "approval-handoff": "Approval Handoff",
  "reverse-engineering": "Reverse Engineering",
  "practices-discovery": "Practices Discovery",
  "requirements-analysis": "Requirements Analysis",
  "user-stories": "User Stories",
  "refined-mockups": "Refined Mockups",
  "application-design": "Application Design",
  "units-generation": "Units Generation",
  "delivery-planning": "Delivery Planning",
  "functional-design": "Functional Design",
  "nfr-requirements": "NFR Requirements",
  "nfr-design": "NFR Design",
  "infrastructure-design": "Infrastructure Design",
  "code-generation": "Code Generation",
  "build-and-test": "Build and Test",
  "ci-pipeline": "CI Pipeline",
  "deployment-pipeline": "Deployment Pipeline",
  "environment-provisioning": "Environment Provisioning",
  "deployment-execution": "Deployment Execution",
  "observability-setup": "Observability Setup",
  "incident-response": "Incident Response",
  "performance-validation": "Performance Validation",
  "feedback-optimization": "Feedback Optimization",
};

const STAGE_PURPOSES: Record<string, string> = {
  "workspace-scaffold": "Intent별 기록, 산출물, 검증 디렉터리를 준비합니다.",
  "workspace-detection": "기존 코드와 기술 환경을 탐지해 신규 개발인지 기존 시스템 변경인지 판별합니다.",
  "state-init": "Scope와 실행 Stage를 확정하고 추적 가능한 워크플로 상태를 생성합니다.",
  "intent-capture": "해결할 문제, 목표, 성공 기준과 이해관계자를 명확히 합니다.",
  "market-research": "시장·대안·경쟁 환경을 조사해 제품 판단의 근거를 만듭니다.",
  feasibility: "기술·사업·운영 제약과 위험을 검토해 실행 가능성을 평가합니다.",
  "scope-definition": "포함 Scope, 제외 Scope와 Intent 백로그를 확정합니다.",
  "team-formation": "필요 역량과 협업팀 구성을 정하고 역할 공백을 확인합니다.",
  "rough-mockups": "핵심 사용자 흐름과 화면 구조를 빠르게 시각화합니다.",
  "approval-handoff": "Ideation 결과의 정합성을 검증하고 Inception 진입 승인을 받습니다.",
  "reverse-engineering": "기존 시스템의 구조, 동작과 변경 영향을 분석합니다.",
  "practices-discovery": "브랜치, 테스트, 리뷰, 배포 등 팀의 작업 방식을 확인합니다.",
  "requirements-analysis": "기능·비기능 요구사항, 제약, 가정과 제외 Scope를 정리합니다.",
  "user-stories": "요구사항을 사용자 가치와 검증 가능한 인수 기준으로 전환합니다.",
  "refined-mockups": "스토리와 요구사항을 반영해 상세 상호작용과 접근성을 구체화합니다.",
  "application-design": "컴포넌트, 서비스, 인터페이스와 주요 아키텍처 결정을 설계합니다.",
  "units-generation": "시스템을 Unit of Work로 분해하고 의존성 DAG와 스토리 매핑을 만듭니다.",
  "delivery-planning": "Unit of Work를 Bolt로 묶고 순서, 위험, 외부 의존성과 담당 협업팀을 정합니다.",
  "functional-design": "현재 Unit of Work의 도메인 모델, 규칙, 로직과 사용자 화면 컴포넌트를 상세 설계합니다.",
  "nfr-requirements": "현재 Unit of Work의 보안, 성능, 신뢰성, 확장성 목표를 정량화합니다.",
  "nfr-design": "비기능 요구사항을 만족할 구조와 기술적 전략을 설계합니다.",
  "infrastructure-design": "실행 환경, 네트워크, 데이터, IaC와 배포 토폴로지를 설계합니다.",
  "code-generation": "승인된 설계를 실제 애플리케이션 코드와 테스트로 구현합니다.",
  "build-and-test": "빌드와 테스트를 실행해 구현의 수렴 여부와 품질을 검증합니다.",
  "ci-pipeline": "반복 가능한 자동 빌드·테스트·품질 검증 파이프라인을 구성합니다.",
  "deployment-pipeline": "승격, 승인, 롤백을 포함한 배포 자동화 경로를 준비합니다.",
  "environment-provisioning": "애플리케이션이 실행될 환경과 필요한 리소스를 프로비저닝합니다.",
  "deployment-execution": "승인된 릴리스를 대상 환경에 배포하고 결과를 확인합니다.",
  "observability-setup": "로그, 메트릭, 트레이싱, 대시보드와 알림을 구성합니다.",
  "incident-response": "장애 등급, 대응 절차, 책임과 복구 방식을 준비합니다.",
  "performance-validation": "실제 부하 조건에서 성능 목표 충족 여부를 검증합니다.",
  "feedback-optimization": "운영 신호와 사용자 피드백을 다음 개선 사이클로 연결합니다.",
};

const GLOSSARY_KO = [
  { term: "Phase", definition: "AI-DLC의 최상위 과정 묶음입니다. Initialization, Ideation, Inception, Construction, Operation의 다섯 Phase로 구성됩니다." },
  { term: "Stage", definition: "Phase 안에서 독립된 목적과 산출물을 가지는 하나의 작업 절차입니다." },
  { term: "Scope", definition: "어떤 Stage를 실행하고 어느 정도 깊이로 수행할지 결정하는 설정입니다." },
  { term: "Bolt", definition: "하나 또는 소수의 연관된 Unit of Work를 대상으로 Construction Stage 3.1~3.5를 한 번 수행하는 배포 가능한 작업 묶음입니다." },
  { term: "Walking Skeleton", definition: "모든 핵심 연동 지점을 가장 얇게 관통하여 아키텍처가 실제로 동작함을 증명하는 첫 번째 Bolt입니다." },
  { term: "Ladder Prompt", definition: "Walking Skeleton 승인 뒤 남은 Bolt를 자율 실행할지 매번 승인받을지 결정하는 한 번의 질문입니다." },
  { term: "Parallel Batch", definition: "선행 의존성이 충족되고 서로 의존하지 않아 동시에 실행할 수 있는 Bolt 묶음입니다." },
  { term: "Unit of Work", definition: "독립적으로 구현할 수 있는 기능 패키지이며 Construction Phase의 반복 단위입니다." },
  { term: "Service", definition: "API 서버, 작업자, 프런트엔드 앱처럼 독립적으로 배포되는 프로세스나 컨테이너입니다." },
  { term: "Module", definition: "패키지나 네임스페이스처럼 Service 내부의 코드 조직 경계입니다." },
  { term: "Component", definition: "클래스, 함수 묶음, UI 요소처럼 Module 안에서 특정 책임을 수행하는 논리적 구성 요소입니다." },
  { term: "Planning", definition: "질문, 분석, 설계를 수행하여 Markdown 산출물을 만드는 Stage들입니다." },
  { term: "Generation", definition: "실행 가능한 코드와 빌드·테스트 결과를 만드는 Stage들입니다." },
  { term: "Depth", definition: "산출물과 질문의 깊이를 Minimal, Standard, Comprehensive 중 하나로 조절하는 설정입니다." },
  { term: "Artifact", definition: "활성 Intent 기록 디렉터리에 저장되는 버전 관리 대상 Markdown 결정·설계·분석 문서입니다." },
  { term: "Guardrail", definition: "반복해서 지켜야 할 행동 규칙으로 활성 Space의 memory 디렉터리에 저장됩니다." },
  { term: "AI-DLC", definition: "AI 주도 개발 생명주기(AI-Driven Development Life Cycle)를 의미하며 이 프로젝트가 구현하는 방법론입니다." },
];

const STATUS_META: Record<StageStatus, { label: string; className: string }> = {
  completed: { label: "완료", className: "complete" },
  "in-progress": { label: "진행 중", className: "active" },
  "awaiting-approval": { label: "승인 대기", className: "approval" },
  revising: { label: "수정 중", className: "revising" },
  pending: { label: "대기", className: "pending" },
  skipped: { label: "건너뜀", className: "skipped" },
};

function field(content: string, name: string): string {
  const escaped = name.replace(/[.*+?^${}()|[\]\\]/g, "\\$&");
  return content.match(new RegExp(`^- \\*\\*${escaped}\\*\\*:\\s*(.*)$`, "m"))?.[1]?.trim() ?? "";
}

function escapeHtml(value: string): string {
  return value
    .replaceAll("&", "&amp;")
    .replaceAll("<", "&lt;")
    .replaceAll(">", "&gt;")
    .replaceAll('"', "&quot;")
    .replaceAll("'", "&#039;");
}

function encodeRelativePath(value: string): string {
  return value.split(/[\\/]/).map((part) => encodeURIComponent(part)).join("/");
}

function jsonForScript(value: unknown): string {
  return JSON.stringify(value)
    .replaceAll("<", "\\u003c")
    .replaceAll(">", "\\u003e")
    .replaceAll("&", "\\u0026");
}

function argValue(name: string): string | undefined {
  const index = process.argv.indexOf(name);
  return index >= 0 ? process.argv[index + 1] : undefined;
}

function artifactFiles(
  recordPath: string,
  phase: string,
  slug: string,
  produces: string[],
): string[] {
  const allowed = new Set(produces);
  if (allowed.size === 0) return [];
  const direct = join(recordPath, phase, slug);
  const candidates = [direct];

  if (phase === "construction") {
    const constructionDir = join(recordPath, phase);
    if (existsSync(constructionDir)) {
      for (const entry of readdirSync(constructionDir)) {
        const nested = join(constructionDir, entry, slug);
        if (existsSync(nested)) candidates.push(nested);
      }
    }
  }

  const files = new Set<string>();
  for (const candidate of candidates) {
    if (!existsSync(candidate) || !statSync(candidate).isDirectory()) continue;
    for (const entry of readdirSync(candidate)) {
      const fullPath = join(candidate, entry);
      const artifactName = entry.endsWith(".md") ? entry.slice(0, -3) : "";
      if (statSync(fullPath).isFile() && allowed.has(artifactName)) {
        files.add(fullPath);
      }
    }
  }
  return [...files].sort();
}

function progressPercent(completed: number, total: number): number {
  return total === 0 ? 0 : Math.round((completed / total) * 100);
}

function localizedCondition(execution: string): string {
  return execution === "ALWAYS"
    ? "현재 Scope의 실행 계획에 포함되는 필수 Stage입니다."
    : "Project Type과 Scope의 실행 조건이 충족될 때 수행합니다.";
}

function localizedInputs(consumes: Array<{ artifact: string }>): string {
  const names = consumes.map((item) => item.artifact).filter(Boolean);
  return names.length ? `입력 산출물: ${names.join(", ")}` : "이전 Stage에서 승인된 상태와 사용자 입력";
}

function localizedOutputs(produces: string[]): string {
  return produces.length ? `생성 산출물: ${produces.join(", ")}` : "상태 정보와 감사 기록 갱신";
}

function localizedConfig(value: string): string {
  const labels: Record<string, string> = {
    Greenfield: "Greenfield",
    Brownfield: "Brownfield",
    Comprehensive: "Comprehensive",
    Standard: "Standard",
    Minimal: "Minimal",
    enterprise: "Enterprise",
    feature: "Feature",
    mvp: "MVP",
    poc: "PoC",
    bugfix: "Bugfix",
    refactor: "Refactor",
    infra: "Infra",
    "security-patch": "Security Patch",
    workshop: "Workshop",
  };
  return labels[value] ?? value;
}

function renderStage(stage: DashboardStage, isCurrent: boolean): string {
  const status = STATUS_META[stage.status];
  const artifactLinks = stage.artifacts.length
    ? `<div class="artifact-list">${stage.artifacts
      .map((artifact) => `<a href="${artifact.href}" target="_blank">${escapeHtml(artifact.name)}</a>`)
      .join("")}</div>`
    : `<p class="empty">아직 생성된 산출물이 없습니다.</p>`;
  const agents = [stage.leadAgent, ...stage.supportAgents].filter(Boolean);

  return `
    <article class="stage-card ${status.className}${isCurrent ? " current" : ""}"
      data-stage="${escapeHtml(stage.slug)}" data-phase="${escapeHtml(stage.phase)}"
      data-status="${stage.status}" data-search="${escapeHtml(`${stage.number} ${stage.name} ${stage.slug} ${stage.purpose}`.toLowerCase())}">
      <div class="stage-marker" aria-hidden="true"></div>
      <div class="stage-main">
        <div class="stage-heading">
          <span class="stage-number">${escapeHtml(stage.number)}</span>
          <div>
            <h3>${escapeHtml(stage.name)}</h3>
            <p class="slug">${escapeHtml(stage.slug)}</p>
          </div>
          <span class="badge ${status.className}">${status.label}</span>
        </div>
        <p class="purpose">${escapeHtml(stage.purpose)}</p>
        <div class="stage-actions">
          <details>
          <summary>세부 정보와 산출물 <span class="artifact-count">${stage.artifacts.length}</span></summary>
          <div class="detail-grid">
            <div><h4>실행 조건</h4><p data-detail="condition">${escapeHtml(stage.condition || "Scope 설정에 따라 실행")}</p></div>
            <div><h4>담당</h4><p data-detail="agents">${escapeHtml(agents.join(" · ") || "orchestrator")}</p></div>
            <div><h4>입력</h4><p data-detail="inputs">${escapeHtml(stage.inputs || "이전 Stage의 승인된 컨텍스트")}</p></div>
            <div><h4>예상 출력</h4><p data-detail="outputs">${escapeHtml(stage.outputs || "상태와 감사 기록")}</p></div>
          </div>
          <h4 class="artifact-title">현재 산출물</h4>
          ${artifactLinks}
          </details>
          <button class="guide-button" type="button" data-guide="${escapeHtml(stage.slug)}">Stage 상세 설명</button>
        </div>
      </div>
    </article>`;
}

function renderPhase(phase: string, stages: DashboardStage[], currentStage: string): string {
  const executable = stages.filter((stage) => stage.status !== "skipped");
  const completed = executable.filter((stage) => stage.status === "completed").length;
  const percent = progressPercent(completed, executable.length);
  const active = stages.some((stage) =>
    ["in-progress", "awaiting-approval", "revising"].includes(stage.status)
  );
  const meta = PHASE_META[phase] ?? { label: phase, description: "" };

  return `
    <section class="phase-section" data-phase="${phase}">
      <div class="phase-header">
        <div>
          <span class="eyebrow">${escapeHtml(meta.label)}</span>
          <h2>${escapeHtml(meta.description)}</h2>
        </div>
        <div class="phase-header-actions">
          <div class="phase-score ${active ? "active" : ""}">
            <strong>${percent}%</strong>
            <span>${completed}/${executable.length} 완료</span>
          </div>
          <button class="phase-toggle" type="button" aria-expanded="true">
            <span>접기</span><span class="chevron" aria-hidden="true">⌃</span>
          </button>
        </div>
      </div>
      <div class="phase-content">
        <div class="phase-progress" aria-label="${meta.label} ${percent}% 완료">
          <span style="width:${percent}%"></span>
        </div>
        <div class="stage-list">
          ${stages.map((stage) => renderStage(stage, stage.slug === currentStage)).join("")}
        </div>
      </div>
    </section>`;
}

function buildDashboard(): void {
  const projectDir = resolveProjectDir(argValue("--project-dir"));
  const space = activeSpace(projectDir);
  const intent = activeIntent(projectDir, space);
  if (!intent) {
    throw new Error("활성 Intent를 찾을 수 없습니다. 먼저 /aidlc를 시작하거나 Intent를 선택하세요.");
  }

  const recordPath = recordDir(projectDir, intent, space);
  if (!recordPath) throw new Error("활성 Intent의 record 경로를 찾을 수 없습니다.");
  const statePath = join(recordPath, "aidlc-state.md");
  const state = readFileSync(statePath, "utf8");
  const outputArg = argValue("--output") ?? join("aidlc", "dashboard.html");
  const outputPath = isAbsolute(outputArg) ? outputArg : resolve(projectDir, outputArg);
  const outputDir = dirname(outputPath);
  mkdirSync(outputDir, { recursive: true });

  const checkboxes = new Map(parseCheckboxes(state).map((item) => [item.slug, item]));
  const graph = loadStageGraph();
  const stages: DashboardStage[] = graph
    .filter((entry) => checkboxes.has(entry.slug))
    .map((entry) => {
      const checkbox = checkboxes.get(entry.slug)!;
      const status: StageStatus = checkbox.suffix.startsWith("SKIP")
        ? "skipped"
        : checkbox.state;
      const artifacts = artifactFiles(
        recordPath,
        entry.phase,
        entry.slug,
        entry.produces ?? [],
      ).map((filePath) => ({
        name: relative(recordPath, filePath).replaceAll("\\", "/"),
        href: encodeRelativePath(relative(outputDir, filePath)),
      }));
      return {
        slug: entry.slug,
        number: entry.number,
        name: STAGE_NAMES[entry.slug] ?? entry.name,
        phase: entry.phase,
        status,
        purpose: STAGE_PURPOSES[entry.slug] ?? entry.condition ?? "이 Stage에 정의된 결과를 생성합니다.",
        condition: localizedCondition(entry.execution),
        leadAgent: entry.lead_agent,
        supportAgents: entry.support_agents,
        inputs: localizedInputs(entry.consumes ?? []),
        outputs: localizedOutputs(entry.produces ?? []),
        artifacts,
      };
    });

  const executable = stages.filter((stage) => stage.status !== "skipped");
  const completed = executable.filter((stage) => stage.status === "completed").length;
  const remaining = executable.filter((stage) => stage.status === "pending").length;
  const overallPercent = progressPercent(completed, executable.length);
  const currentStageSlug = field(state, "Current Stage");
  const nextStageSlug = field(state, "Next Stage");
  const currentStage = stages.find((stage) => stage.slug === currentStageSlug);
  const nextStage = stages.find((stage) => stage.slug === nextStageSlug);
  const lifecyclePhase = field(state, "Lifecycle Phase").toLowerCase();
  const nextAction = currentStage
    ? `${currentStage.name}${
      currentStage.status === "awaiting-approval"
        ? " 승인 여부 결정"
        : currentStage.status === "revising"
          ? " 수정 사항 반영"
          : " 수행"
    }`
    : "Workflow 완료 확인";
  const pendingArtifacts = field(state, "Pending Artifacts") || "none";
  const generatedAt = new Date().toISOString();
  const lastUpdated = field(state, "Last Updated");
  const stateHref = encodeRelativePath(relative(outputDir, statePath));
  const phaseSections = PHASES
    .map((phase) => renderPhase(phase, stages.filter((stage) => stage.phase === phase), currentStageSlug))
    .join("");

  const html = `<!doctype html>
<html lang="ko">
<head>
  <meta charset="utf-8">
  <meta name="viewport" content="width=device-width, initial-scale=1">
  <meta name="color-scheme" content="light">
  <title>AI-DLC 진행 현황 · ${escapeHtml(intent)}</title>
  <style>
    :root {
      --ink: #17221d;
      --muted: #657269;
      --paper: #f4f1e9;
      --surface: #fffdf8;
      --line: #dcd8cc;
      --forest: #164b38;
      --forest-2: #287356;
      --mint: #d9eadf;
      --lime: #dce870;
      --amber: #d99032;
      --rose: #b95e55;
      --shadow: 0 18px 50px rgba(42, 54, 47, .09);
    }
    * { box-sizing: border-box; }
    html { scroll-behavior: smooth; }
    body {
      margin: 0;
      color: var(--ink);
      background:
        radial-gradient(circle at 85% 0%, rgba(220, 232, 112, .2), transparent 25rem),
        var(--paper);
      font-family: Inter, Pretendard, -apple-system, BlinkMacSystemFont, "Segoe UI", sans-serif;
      line-height: 1.55;
    }
    button, input { font: inherit; }
    a { color: inherit; }
    .shell { width: min(1240px, calc(100% - 40px)); margin: 0 auto; padding: 34px 0 72px; }
    .topbar { display: flex; justify-content: space-between; align-items: center; gap: 20px; margin-bottom: 26px; }
    .brand { display: flex; align-items: center; gap: 12px; font-weight: 800; letter-spacing: -.02em; }
    .brand-mark {
      display: grid; place-items: center; width: 38px; height: 38px; border-radius: 12px;
      background: var(--forest); color: var(--lime); font-size: 13px; box-shadow: var(--shadow);
    }
    .top-meta { display: flex; align-items: center; gap: 10px; color: var(--muted); font-size: 13px; }
    .live-dot { width: 8px; height: 8px; border-radius: 50%; background: #39a36f; box-shadow: 0 0 0 5px rgba(57,163,111,.12); }
    .live-dot.offline { background: #a4aaa6; box-shadow: 0 0 0 5px rgba(120,128,123,.1); }
    .live-dot.syncing { background: var(--amber); box-shadow: 0 0 0 5px rgba(217,144,50,.13); }
    .hero {
      display: grid; grid-template-columns: minmax(0, 1.6fr) minmax(280px, .75fr);
      gap: 18px; padding: 34px; border-radius: 28px; color: white; background: var(--forest);
      box-shadow: var(--shadow); overflow: hidden; position: relative;
    }
    .hero::after {
      content: ""; position: absolute; width: 360px; height: 360px; right: -150px; top: -170px;
      border: 70px solid rgba(220,232,112,.16); border-radius: 50%;
    }
    .kicker { color: var(--lime); font-weight: 800; font-size: 12px; letter-spacing: .13em; text-transform: uppercase; }
    .hero h1 { margin: 8px 0 8px; font-size: clamp(31px, 5vw, 58px); line-height: 1.02; letter-spacing: -.055em; }
    .hero-copy { max-width: 680px; margin: 0; color: rgba(255,255,255,.72); font-size: 16px; }
    .hero-tags { display: flex; flex-wrap: wrap; gap: 8px; margin-top: 24px; }
    .hero-tag { padding: 7px 11px; border: 1px solid rgba(255,255,255,.18); border-radius: 999px; font-size: 12px; color: rgba(255,255,255,.86); }
    .progress-orb {
      align-self: center; justify-self: center; position: relative; display: grid; place-items: center;
      width: 190px; aspect-ratio: 1; border-radius: 50%;
      background: conic-gradient(var(--lime) ${overallPercent}%, rgba(255,255,255,.12) 0);
      z-index: 1;
    }
    .progress-orb::before { content: ""; position: absolute; inset: 14px; border-radius: 50%; background: var(--forest); }
    .orb-content { z-index: 1; text-align: center; }
    .orb-content strong { display: block; font-size: 48px; line-height: 1; letter-spacing: -.06em; }
    .orb-content span { color: rgba(255,255,255,.65); font-size: 12px; }
    .metric-grid { display: grid; grid-template-columns: repeat(4, 1fr); gap: 14px; margin: 18px 0; }
    .metric { padding: 18px 20px; background: var(--surface); border: 1px solid var(--line); border-radius: 18px; box-shadow: 0 8px 30px rgba(42,54,47,.045); }
    .metric span { display: block; color: var(--muted); font-size: 12px; margin-bottom: 4px; }
    .metric strong { font-size: 24px; letter-spacing: -.035em; }
    .now {
      display: grid; grid-template-columns: 1.15fr .85fr; gap: 18px; margin: 18px 0 34px;
    }
    .panel { padding: 25px; background: var(--surface); border: 1px solid var(--line); border-radius: 22px; }
    .panel.primary { border: 0; background: var(--lime); }
    .eyebrow { display: block; margin-bottom: 7px; color: var(--forest-2); font-size: 11px; font-weight: 900; letter-spacing: .14em; text-transform: uppercase; }
    .panel h2 { margin: 0 0 6px; font-size: 24px; line-height: 1.22; letter-spacing: -.035em; }
    .panel p { margin: 0; color: #4f5b53; }
    .action {
      display: flex; align-items: center; justify-content: space-between; gap: 16px;
      margin-top: 20px; padding-top: 16px; border-top: 1px solid rgba(22,75,56,.18);
    }
    .action code { font-size: 13px; font-weight: 800; color: var(--forest); }
    .step-index { display: grid; grid-template-columns: repeat(5, 1fr); gap: 7px; margin-top: 18px; }
    .step-index a { height: 8px; border-radius: 10px; background: #e1ded4; text-decoration: none; }
    .step-index a.done { background: var(--forest-2); }
    .step-index a.current { background: var(--amber); }
    .toolbar {
      position: sticky; top: 12px; z-index: 10; display: flex; flex-wrap: wrap; align-items: center;
      justify-content: space-between; gap: 12px; margin-bottom: 25px; padding: 12px;
      background: rgba(255,253,248,.9); border: 1px solid var(--line); border-radius: 16px;
      backdrop-filter: blur(14px); box-shadow: 0 8px 30px rgba(42,54,47,.06);
    }
    .filters { display: flex; gap: 5px; }
    .filter, .utility {
      border: 0; padding: 8px 12px; border-radius: 10px; color: var(--muted);
      background: transparent; cursor: pointer; font-weight: 700; font-size: 13px;
    }
    .filter.selected { color: white; background: var(--forest); }
    .utility.strong { color: white; background: var(--forest-2); }
    .utility:disabled { cursor: not-allowed; opacity: .5; }
    .search {
      width: min(300px, 100%); padding: 9px 12px; color: var(--ink);
      background: #f6f3ec; border: 1px solid transparent; border-radius: 10px; outline: 0;
    }
    .search:focus { border-color: var(--forest-2); box-shadow: 0 0 0 3px rgba(40,115,86,.12); }
    .intent-switcher {
      display: flex; align-items: center; gap: 6px; padding-left: 8px;
      border-left: 1px solid var(--line);
    }
    .intent-switcher label { color: var(--muted); font-size: 11px; font-weight: 800; }
    .intent-switcher select {
      max-width: 220px; padding: 8px 28px 8px 10px; border: 1px solid var(--line);
      border-radius: 9px; color: var(--ink); background: #f6f3ec; font-size: 12px;
    }
    .intent-switcher select:disabled { opacity: .6; }
    .phase-section { margin-top: 42px; scroll-margin-top: 90px; }
    .phase-header { display: flex; justify-content: space-between; gap: 20px; align-items: end; }
    .phase-header h2 { max-width: 720px; margin: 0; font-size: 22px; letter-spacing: -.025em; }
    .phase-score { display: flex; gap: 8px; align-items: baseline; color: var(--muted); }
    .phase-score strong { color: var(--ink); font-size: 20px; }
    .phase-score span { font-size: 12px; }
    .phase-score.active strong { color: var(--amber); }
    .phase-header-actions { display: flex; align-items: center; gap: 12px; }
    .phase-toggle {
      display: inline-flex; align-items: center; gap: 7px; padding: 7px 10px;
      border: 1px solid var(--line); border-radius: 9px; color: var(--forest);
      background: var(--surface); cursor: pointer; font-size: 12px; font-weight: 800;
    }
    .phase-toggle:hover { border-color: var(--forest-2); background: var(--mint); }
    .phase-toggle .chevron { transition: transform .2s; }
    .phase-section.collapsed .phase-toggle .chevron { transform: rotate(180deg); }
    .phase-section.collapsed .phase-content { display: none; }
    .phase-progress { height: 3px; margin: 15px 0 14px; overflow: hidden; background: #dfdcd2; border-radius: 4px; }
    .phase-progress span { display: block; height: 100%; background: var(--forest-2); border-radius: inherit; }
    .stage-list { display: grid; gap: 9px; }
    .stage-card {
      position: relative; display: grid; grid-template-columns: 26px 1fr; min-width: 0;
      padding: 18px 20px 18px 12px; border: 1px solid var(--line); border-radius: 17px;
      background: var(--surface); transition: transform .2s, border-color .2s, opacity .2s;
    }
    .stage-card:hover { transform: translateY(-1px); border-color: #b9c5bc; }
    .stage-card.current { border-color: var(--amber); box-shadow: 0 0 0 3px rgba(217,144,50,.12); }
    .stage-card.skipped { opacity: .58; }
    .stage-marker { width: 10px; height: 10px; margin: 8px 0 0 2px; border-radius: 50%; background: #d5d4ce; border: 2px solid var(--surface); box-shadow: 0 0 0 1px #d5d4ce; }
    .stage-card.complete .stage-marker { background: var(--forest-2); box-shadow: 0 0 0 1px var(--forest-2); }
    .stage-card.active .stage-marker { background: var(--amber); box-shadow: 0 0 0 5px rgba(217,144,50,.13); }
    .stage-heading { display: grid; grid-template-columns: 46px minmax(0,1fr) auto; gap: 11px; align-items: center; }
    .stage-number { color: var(--muted); font-family: ui-monospace, SFMono-Regular, Menlo, monospace; font-size: 13px; }
    .stage-heading h3 { margin: 0; font-size: 16px; letter-spacing: -.018em; }
    .slug { margin: 0 !important; color: #8b958d !important; font-family: ui-monospace, SFMono-Regular, Menlo, monospace; font-size: 11px; }
    .badge { padding: 5px 9px; border-radius: 999px; background: #ecebe5; color: #69726c; font-size: 11px; font-weight: 800; white-space: nowrap; }
    .badge.complete { color: var(--forest); background: var(--mint); }
    .badge.active { color: #70410c; background: #f5d7a8; }
    .badge.approval { color: #754d0a; background: #f3df91; }
    .badge.revising { color: #7f3731; background: #f3d4d0; }
    .purpose { margin: 10px 0 0 !important; color: #4f5b53 !important; font-size: 14px; }
    details { margin-top: 12px; }
    .stage-actions { display: flex; align-items: start; justify-content: space-between; gap: 14px; }
    .stage-actions details { min-width: 0; flex: 1; }
    .guide-button {
      margin-top: 9px; padding: 7px 10px; border: 1px solid #cbd7ce; border-radius: 9px;
      color: var(--forest); background: #f7faf7; cursor: pointer; font-size: 11px; font-weight: 800;
      white-space: nowrap;
    }
    .guide-button:hover { border-color: var(--forest-2); background: var(--mint); }
    summary { width: fit-content; color: var(--forest-2); cursor: pointer; font-size: 12px; font-weight: 800; }
    summary span { display: inline-grid; place-items: center; min-width: 20px; height: 20px; margin-left: 5px; padding: 0 5px; border-radius: 10px; background: var(--mint); }
    .detail-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 10px; margin-top: 14px; }
    .detail-grid > div { padding: 13px; border-radius: 12px; background: #f5f2eb; }
    h4 { margin: 0 0 4px; font-size: 11px; text-transform: uppercase; letter-spacing: .08em; color: var(--muted); }
    .detail-grid p { font-size: 12px; overflow-wrap: anywhere; }
    .artifact-title { margin-top: 15px; }
    .artifact-list { display: flex; flex-wrap: wrap; gap: 7px; margin-top: 7px; }
    .artifact-list a { max-width: 100%; padding: 6px 9px; overflow: hidden; text-overflow: ellipsis; border-radius: 8px; background: var(--mint); color: var(--forest); font-size: 11px; text-decoration: none; white-space: nowrap; }
    .artifact-list a:hover { text-decoration: underline; }
    .artifact-list button {
      max-width: 100%; padding: 6px 9px; overflow: hidden; text-overflow: ellipsis;
      border: 0; border-radius: 8px; background: var(--mint); color: var(--forest);
      cursor: pointer; font-size: 11px; white-space: nowrap;
    }
    .artifact-list button:hover { text-decoration: underline; }
    .empty { font-size: 12px; color: var(--muted) !important; }
    .modal {
      position: fixed; inset: 0; z-index: 100; display: none; place-items: center;
      padding: 28px; background: rgba(18,31,25,.58); backdrop-filter: blur(8px);
    }
    .modal.open { display: grid; }
    .modal-card {
      display: grid; grid-template-rows: auto minmax(0,1fr); width: min(920px, 100%);
      max-height: min(86vh, 920px); overflow: hidden; border-radius: 22px;
      background: var(--surface); box-shadow: 0 28px 90px rgba(10,24,17,.28);
    }
    .modal-head {
      display: flex; justify-content: space-between; align-items: center; gap: 18px;
      padding: 18px 22px; border-bottom: 1px solid var(--line);
    }
    .modal-head h2 { margin: 0; font-size: 20px; letter-spacing: -.025em; }
    .modal-close {
      width: 34px; height: 34px; border: 0; border-radius: 50%; color: var(--muted);
      background: #eceae3; cursor: pointer; font-size: 20px;
    }
    .modal-body { overflow: auto; padding: 24px; }
    .document { max-width: 760px; margin: 0 auto; color: #35423b; }
    .document h1 { margin: 0 0 20px; color: var(--ink); font-size: 30px; letter-spacing: -.04em; }
    .document h2 { margin: 30px 0 10px; padding-top: 18px; border-top: 1px solid var(--line); color: var(--ink); font-size: 21px; }
    .document h3 { margin: 22px 0 8px; color: var(--forest); font-size: 16px; }
    .document h4, .document h5, .document h6 { margin: 18px 0 7px; color: var(--ink); font-size: 14px; letter-spacing: 0; text-transform: none; }
    .document p, .document li { font-size: 14px; line-height: 1.65; }
    .document ul, .document ol { margin: 10px 0; padding-left: 24px; }
    .document blockquote { margin: 14px 0; padding: 10px 14px; border-left: 4px solid var(--forest-2); border-radius: 0 8px 8px 0; color: #526159; background: #f2f5f1; }
    .document blockquote p { margin: 3px 0; }
    .document hr { margin: 24px 0; border: 0; border-top: 1px solid var(--line); }
    .document code { padding: 2px 5px; border-radius: 5px; background: #eceae3; font-family: ui-monospace, SFMono-Regular, Menlo, monospace; font-size: .9em; }
    .document pre { overflow: auto; padding: 15px; border-radius: 12px; color: #e9f1ec; background: #173027; }
    .document pre code { padding: 0; color: inherit; background: transparent; }
    .document .table-scroll { max-width: 100%; margin: 14px 0; overflow-x: auto; border-radius: 10px; }
    .document table { width: 100%; border-collapse: collapse; font-size: 13px; }
    .document th, .document td { padding: 9px; border: 1px solid var(--line); text-align: left; vertical-align: top; }
    .document th { background: #f0eee7; }
    .glossary-tools { display: flex; gap: 10px; margin-bottom: 18px; }
    .glossary-tools input { width: 100%; padding: 10px 12px; border: 1px solid var(--line); border-radius: 10px; }
    .term-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 10px; }
    .term { padding: 16px; border: 1px solid var(--line); border-radius: 14px; background: #faf8f2; }
    .term h3 { margin: 0 0 5px; color: var(--forest); font-size: 15px; }
    .term p { margin: 0; color: #536058; font-size: 13px; }
    .connection-note { margin-top: 14px; padding: 12px; border-radius: 10px; background: #f3efe4; color: var(--muted); font-size: 12px; }
    .footer { display: flex; justify-content: space-between; gap: 20px; margin-top: 58px; padding-top: 20px; border-top: 1px solid var(--line); color: var(--muted); font-size: 12px; }
    .hidden { display: none !important; }
    @media (max-width: 760px) {
      .shell { width: min(100% - 24px, 1240px); padding-top: 18px; }
      .top-meta span:not(.live-dot) { display: none; }
      .hero { grid-template-columns: 1fr; padding: 25px; }
      .progress-orb { width: 150px; justify-self: start; }
      .metric-grid { grid-template-columns: 1fr 1fr; }
      .now { grid-template-columns: 1fr; }
      .toolbar { align-items: stretch; }
      .search { width: 100%; order: -1; }
      .intent-switcher { padding-left: 0; border-left: 0; }
      .intent-switcher select { min-width: 0; flex: 1; }
      .phase-header { align-items: start; }
      .phase-header-actions { align-items: end; flex-direction: column; }
      .phase-header h2 { font-size: 18px; }
      .stage-heading { grid-template-columns: 38px minmax(0,1fr); }
      .badge { grid-column: 2; justify-self: start; }
      .detail-grid { grid-template-columns: 1fr; }
      .stage-actions { display: block; }
      .term-grid { grid-template-columns: 1fr; }
      .modal { padding: 10px; }
      .modal-card { max-height: 94vh; }
      .footer { flex-direction: column; }
    }
    @media print {
      body { background: white; }
      .shell { width: 100%; padding: 0; }
      .toolbar, .top-meta { display: none; }
      .hero { box-shadow: none; }
      .stage-card { break-inside: avoid; }
    }
  </style>
</head>
<body>
  <main class="shell">
    <header class="topbar">
      <div class="brand"><span class="brand-mark">AID</span><span>AI-DLC 진행 현황</span></div>
      <div class="top-meta"><span class="live-dot offline" id="live-dot"></span><span id="connection-status">생성 시점 · <time data-time="${generatedAt}">${generatedAt}</time></span></div>
    </header>

    <section class="hero">
      <div>
        <span class="kicker" id="scope-label">${escapeHtml(space)} Space · ${escapeHtml(localizedConfig(field(state, "Scope")))} Scope</span>
        <h1 id="intent-name">${escapeHtml(intent)}</h1>
        <p class="hero-copy">현재 <strong id="current-stage-name">${escapeHtml(currentStage?.name ?? currentStageSlug)}</strong> Stage를 진행하고 있습니다. 승인된 산출물과 다음 작업을 한 화면에서 확인하세요.</p>
        <div class="hero-tags">
          <span class="hero-tag" id="project-type">${escapeHtml(localizedConfig(field(state, "Project Type")))}</span>
          <span class="hero-tag" id="depth-label">Depth ${escapeHtml(localizedConfig(field(state, "Depth")))}</span>
          <span class="hero-tag" id="test-label">Test Strategy ${escapeHtml(localizedConfig(field(state, "Test Strategy")))}</span>
          <span class="hero-tag">AI-DLC v${AIDLC_VERSION}</span>
        </div>
      </div>
      <div class="progress-orb" id="progress-orb" role="img" aria-label="전체 진행률 ${overallPercent}%">
        <div class="orb-content"><strong id="overall-percent">${overallPercent}%</strong><span id="overall-count">${completed} / ${executable.length} Stage</span></div>
      </div>
    </section>

    <section class="metric-grid" aria-label="Workflow 요약">
      <div class="metric"><span>현재 Phase</span><strong id="metric-phase">${escapeHtml(PHASE_META[lifecyclePhase]?.label ?? lifecyclePhase)}</strong></div>
      <div class="metric"><span>완료 Stage</span><strong id="metric-completed">${completed}</strong></div>
      <div class="metric"><span>남은 Stage</span><strong id="metric-remaining">${remaining}</strong></div>
      <div class="metric"><span>현재 담당</span><strong id="metric-agent">${escapeHtml(currentStage?.leadAgent.replace("aidlc-", "").replace("-agent", "") ?? field(state, "Active Agent"))}</strong></div>
    </section>

    <section class="now">
      <article class="panel primary">
        <span class="eyebrow">지금 해야 할 일</span>
        <h2 id="next-action">${escapeHtml(nextAction)}</h2>
        <p id="current-purpose">${escapeHtml(currentStage?.purpose ?? "현재 Stage의 정의된 작업을 완료하고 승인을 요청합니다.")}</p>
        <div class="action">
          <code>/aidlc --resume</code>
          <span id="pending-artifacts">${pendingArtifacts === "none" ? "누락 산출물 없음" : `대기 산출물: ${escapeHtml(pendingArtifacts)}`}</span>
        </div>
      </article>
      <article class="panel">
        <span class="eyebrow">다음 Stage</span>
        <h2 id="next-stage-name">${escapeHtml(nextStage?.name ?? "Workflow 완료")}</h2>
        <p id="next-stage-purpose">${escapeHtml(nextStage?.purpose ?? "현재 계획의 모든 Stage를 마무리하고 결과를 확인합니다.")}</p>
        <nav class="step-index" aria-label="Phase 바로가기">
          ${PHASES.map((phase) => {
            const phaseStages = stages.filter((stage) => stage.phase === phase && stage.status !== "skipped");
            const done = phaseStages.length > 0 && phaseStages.every((stage) => stage.status === "completed");
            const current = phase === lifecyclePhase;
            return `<a href="#${phase}" class="${done ? "done" : ""} ${current ? "current" : ""}" title="${PHASE_META[phase].label}"></a>`;
          }).join("")}
        </nav>
      </article>
    </section>

    <div class="toolbar" aria-label="Stage 필터">
      <div class="filters">
        <button class="filter selected" data-filter="all">전체</button>
        <button class="filter" data-filter="todo">할 일</button>
        <button class="filter" data-filter="completed">완료</button>
      </div>
      <input class="search" type="search" placeholder="Stage 또는 설명 검색" aria-label="Stage 검색">
      <div class="intent-switcher">
        <label for="intent-select">Intent</label>
        <select id="intent-select" disabled>
          <option>${escapeHtml(intent)}</option>
        </select>
      </div>
      <button class="utility" id="glossary-button">용어사전</button>
      <button class="utility" id="refresh-dashboard">새로고침</button>
      <button class="utility" id="go-current">현재 작업으로 이동</button>
      <button class="utility" id="collapse-complete">완료 Phase 접기</button>
      <button class="utility" id="expand-all">모든 상세 펼치기</button>
      <button class="utility" id="connect-folder">프로젝트 읽기 권한</button>
    </div>

    <div id="workflow">
      ${phaseSections.replaceAll('class="phase-section"', (match) => match)}
    </div>

    <footer class="footer">
      <span>마지막 상태 변경: <time id="last-updated" data-time="${escapeHtml(lastUpdated)}">${escapeHtml(lastUpdated)}</time></span>
      <span id="data-mode"><a href="${stateHref}" target="_blank">원본 aidlc-state.md 열기</a> · 새로고침 버튼을 누르면 최신 로컬 상태를 읽습니다.</span>
    </footer>
  </main>
  <div class="modal" id="content-modal" role="dialog" aria-modal="true" aria-labelledby="modal-title">
    <div class="modal-card">
      <header class="modal-head">
        <h2 id="modal-title">상세 정보</h2>
        <button class="modal-close" type="button" aria-label="닫기">×</button>
      </header>
      <div class="modal-body" id="modal-body"></div>
    </div>
  </div>
  <script>
    const stagePurposes = ${jsonForScript(STAGE_PURPOSES)};
    const stageNames = ${jsonForScript(STAGE_NAMES)};
    const phaseMeta = ${jsonForScript(PHASE_META)};
    const glossaryTerms = ${jsonForScript(GLOSSARY_KO)};
    const phaseOrder = ${JSON.stringify([...PHASES])};
    document.querySelectorAll(".phase-section").forEach((section, index) => {
      section.id = phaseOrder[index];
    });
    document.querySelectorAll("time[data-time]").forEach((time) => {
      const date = new Date(time.dataset.time);
      if (!Number.isNaN(date.getTime())) {
        time.textContent = new Intl.DateTimeFormat("ko-KR", {
          dateStyle: "medium", timeStyle: "short"
        }).format(date);
      }
    });

    function setPhaseCollapsed(section, collapsed, remember = true) {
      section.classList.toggle("collapsed", collapsed);
      const button = section.querySelector(".phase-toggle");
      button.setAttribute("aria-expanded", String(!collapsed));
      button.querySelector("span").textContent = collapsed ? "펼치기" : "접기";
      if (remember) {
        try {
          localStorage.setItem("aidlc-phase-" + section.dataset.phase, collapsed ? "collapsed" : "expanded");
        } catch {
          // Browser storage is a convenience only.
        }
      }
    }

    document.querySelectorAll(".phase-section").forEach((section) => {
      let collapsed = false;
      try {
        collapsed = localStorage.getItem("aidlc-phase-" + section.dataset.phase) === "collapsed";
      } catch {
        // Keep phases expanded when browser storage is unavailable.
      }
      setPhaseCollapsed(section, collapsed, false);
      section.querySelector(".phase-toggle").addEventListener("click", () => {
        setPhaseCollapsed(section, !section.classList.contains("collapsed"));
      });
    });

    const collapseCompleteButton = document.querySelector("#collapse-complete");
    let completedPhasesCollapsed = true;
    try {
      completedPhasesCollapsed =
        localStorage.getItem("aidlc-completed-phases-v2") !== "expanded";
    } catch {
      // Completed phases stay collapsed by default without browser storage.
    }

    function phaseIsComplete(section) {
      const stages = [...section.querySelectorAll(".stage-card")]
        .filter((card) => card.dataset.status !== "skipped");
      return stages.length > 0 && stages.every((card) => card.dataset.status === "completed");
    }

    function applyCompletedPhaseCollapse() {
      document.querySelectorAll(".phase-section").forEach((section) => {
        if (phaseIsComplete(section)) setPhaseCollapsed(section, completedPhasesCollapsed);
      });
      collapseCompleteButton.textContent = completedPhasesCollapsed
        ? "완료 Phase 펼치기"
        : "완료 Phase 접기";
      try {
        localStorage.setItem(
          "aidlc-completed-phases-v2",
          completedPhasesCollapsed ? "collapsed" : "expanded"
        );
      } catch {
        // Browser storage is a convenience only.
      }
    }

    collapseCompleteButton.addEventListener("click", () => {
      completedPhasesCollapsed = !completedPhasesCollapsed;
      applyCompletedPhaseCollapse();
    });
    applyCompletedPhaseCollapse();

    const modal = document.querySelector("#content-modal");
    const modalTitle = document.querySelector("#modal-title");
    const modalBody = document.querySelector("#modal-body");
    const connectButton = document.querySelector("#connect-folder");
    const intentSelect = document.querySelector("#intent-select");
    const liveDot = document.querySelector("#live-dot");
    const connectionStatus = document.querySelector("#connection-status");
    const liveState = {
      root: null,
      graph: [],
      space: "",
      intent: "",
      recordHandle: null,
      refreshing: false,
      stateModified: 0
    };

    function escapeMarkup(value) {
      return String(value)
        .replaceAll("&", "&amp;")
        .replaceAll("<", "&lt;")
        .replaceAll(">", "&gt;")
        .replaceAll('"', "&quot;")
        .replaceAll("'", "&#039;");
    }

    function inlineMarkdown(value) {
      return escapeMarkup(value)
        .replace(/\\*\\*(.+?)\\*\\*/g, "<strong>$1</strong>")
        .replace(/\\\`([^\\\`]+)\\\`/g, "<code>$1</code>");
    }

    function renderMarkdown(markdown) {
      const source = String(markdown || "").replace(/^---\\s*\\n[\\s\\S]*?\\n---\\s*\\n/, "");
      const lines = source.split(/\\r?\\n/);
      const output = [];
      let listType = "";
      let codeOpen = false;
      let codeLines = [];
      let paragraphLines = [];
      const closeList = () => {
        if (listType) output.push("</" + listType + ">");
        listType = "";
      };
      const closeParagraph = () => {
        if (paragraphLines.length) {
          output.push("<p>" + inlineMarkdown(paragraphLines.join(" ")) + "</p>");
          paragraphLines = [];
        }
      };
      const closeFlow = () => {
        closeParagraph();
        closeList();
      };
      const tableCells = (line) => line.trim()
        .replace(/^\\|/, "")
        .replace(/\\|$/, "")
        .split(/(?<!\\\\)\\|/)
        .map((cell) => cell.trim().replace(/\\\\\\|/g, "|"));
      const isTableDivider = (line) => {
        const cells = tableCells(line);
        return cells.length > 0 && cells.every((cell) => /^:?-{3,}:?$/.test(cell));
      };
      for (let index = 0; index < lines.length; index += 1) {
        const line = lines[index];
        if (/^\\s*\\\`\\\`\\\`/.test(line)) {
          closeFlow();
          if (!codeOpen) {
            codeOpen = true;
            codeLines = [];
          } else {
            output.push("<pre><code>" + escapeMarkup(codeLines.join("\\n")) + "</code></pre>");
            codeOpen = false;
          }
          continue;
        }
        if (codeOpen) {
          codeLines.push(line);
          continue;
        }

        if (
          line.trim().includes("|") &&
          index + 1 < lines.length &&
          isTableDivider(lines[index + 1])
        ) {
          closeFlow();
          const headers = tableCells(line);
          index += 2;
          const rows = [];
          while (index < lines.length && lines[index].trim().includes("|")) {
            rows.push(tableCells(lines[index]));
            index += 1;
          }
          index -= 1;
          output.push(
            '<div class="table-scroll"><table><thead><tr>' +
            headers.map((cell) => "<th>" + inlineMarkdown(cell) + "</th>").join("") +
            "</tr></thead><tbody>" +
            rows.map((row) =>
              "<tr>" + headers.map((_, cellIndex) =>
                "<td>" + inlineMarkdown(row[cellIndex] || "") + "</td>"
              ).join("") + "</tr>"
            ).join("") +
            "</tbody></table></div>"
          );
          continue;
        }

        const heading = line.match(/^(#{1,6})\\s+(.+)$/);
        if (heading) {
          closeFlow();
          const level = heading[1].length;
          output.push("<h" + level + ">" + inlineMarkdown(heading[2]) + "</h" + level + ">");
          continue;
        }

        if (/^\\s*(?:-{3,}|\\*{3,}|_{3,})\\s*$/.test(line)) {
          closeFlow();
          output.push("<hr>");
          continue;
        }

        const quote = line.match(/^\\s*>\\s?(.*)$/);
        if (quote) {
          closeFlow();
          const quoteLines = [quote[1]];
          while (index + 1 < lines.length) {
            const nextQuote = lines[index + 1].match(/^\\s*>\\s?(.*)$/);
            if (!nextQuote) break;
            quoteLines.push(nextQuote[1]);
            index += 1;
          }
          output.push("<blockquote>" + quoteLines.map((item) =>
            "<p>" + inlineMarkdown(item) + "</p>"
          ).join("") + "</blockquote>");
          continue;
        }

        const bullet = line.match(/^\\s*[-*+]\\s+(.+)$/);
        const ordered = line.match(/^\\s*\\d+[.)]\\s+(.+)$/);
        if (bullet || ordered) {
          closeParagraph();
          const nextListType = ordered ? "ol" : "ul";
          if (listType !== nextListType) {
            closeList();
            output.push("<" + nextListType + ">");
            listType = nextListType;
          }
          let item = (bullet || ordered)[1];
          const task = item.match(/^\\[([ xX])\\]\\s*(.*)$/);
          if (task) {
            item = '<span aria-hidden="true">' +
              (task[1].toLowerCase() === "x" ? "☑" : "☐") +
              "</span> " + inlineMarkdown(task[2]);
          } else {
            item = inlineMarkdown(item);
          }
          output.push("<li>" + item + "</li>");
          continue;
        }

        if (!line.trim()) {
          closeFlow();
          continue;
        }

        closeList();
        paragraphLines.push(line.trim());
      }
      closeFlow();
      if (codeOpen) output.push("<pre><code>" + escapeMarkup(codeLines.join("\\n")) + "</code></pre>");
      return '<article class="document">' + output.join("") + "</article>";
    }

    function openModal(title, html) {
      modalTitle.textContent = title;
      modalBody.innerHTML = html;
      modal.classList.add("open");
      document.body.style.overflow = "hidden";
      modal.querySelector(".modal-close").focus();
    }

    function closeModal() {
      modal.classList.remove("open");
      document.body.style.overflow = "";
    }

    modal.querySelector(".modal-close").addEventListener("click", closeModal);
    modal.addEventListener("click", (event) => {
      if (event.target === modal) closeModal();
    });
    document.addEventListener("keydown", (event) => {
      if (event.key === "Escape" && modal.classList.contains("open")) closeModal();
    });

    async function childDirectory(parent, name) {
      try {
        return await parent.getDirectoryHandle(name);
      } catch {
        return null;
      }
    }

    async function childFile(parent, name) {
      try {
        return await parent.getFileHandle(name);
      } catch {
        return null;
      }
    }

    async function directoryAt(root, parts) {
      let directory = root;
      for (const part of parts) {
        directory = await childDirectory(directory, part);
        if (!directory) return null;
      }
      return directory;
    }

    async function fileAt(root, parts) {
      const directory = await directoryAt(root, parts.slice(0, -1));
      return directory ? childFile(directory, parts[parts.length - 1]) : null;
    }

    async function readTextAt(root, parts) {
      const handle = await fileAt(root, parts);
      if (!handle) return null;
      const file = await handle.getFile();
      return { text: await file.text(), modified: file.lastModified, handle };
    }

    function stateField(content, name) {
      const prefix = "- **" + name + "**:";
      const line = content.split(/\\r?\\n/).find((item) => item.startsWith(prefix));
      return line ? line.slice(prefix.length).trim() : "";
    }

    function parseStateStages(content) {
      const stages = new Map();
      const pattern = /^- \\[([ xSR?-])\\] (\\S+)\\s*—\\s*(.*)$/gm;
      let match;
      while ((match = pattern.exec(content)) !== null) {
        let status = "pending";
        if (match[3].trim().startsWith("SKIP") || match[1] === "S") status = "skipped";
        else if (match[1] === "x") status = "completed";
        else if (match[1] === "-") status = "in-progress";
        else if (match[1] === "?") status = "awaiting-approval";
        else if (match[1] === "R") status = "revising";
        stages.set(match[2], { status, suffix: match[3].trim() });
      }
      return stages;
    }

    function statusPresentation(status) {
      const values = {
        completed: ["완료", "complete"],
        "in-progress": ["진행 중", "active"],
        "awaiting-approval": ["승인 대기", "approval"],
        revising: ["수정 중", "revising"],
        pending: ["대기", "pending"],
        skipped: ["건너뜀", "skipped"]
      };
      return values[status] || values.pending;
    }

    function localizedConfigValue(value) {
      const values = {
        Greenfield: "Greenfield",
        Brownfield: "Brownfield",
        Comprehensive: "Comprehensive",
        Standard: "Standard",
        Minimal: "Minimal",
        enterprise: "Enterprise",
        feature: "Feature",
        mvp: "MVP",
        poc: "PoC",
        bugfix: "Bugfix",
        refactor: "Refactor",
        infra: "Infra",
        "security-patch": "Security Patch",
        workshop: "Workshop"
      };
      return values[value] || value;
    }

    function setText(selector, value) {
      const element = document.querySelector(selector);
      if (element) element.textContent = value;
    }

    function updateTime(element, iso) {
      if (!element || !iso) return;
      const date = new Date(iso);
      element.dataset.time = iso;
      element.textContent = Number.isNaN(date.getTime())
        ? iso
        : new Intl.DateTimeFormat("ko-KR", { dateStyle: "medium", timeStyle: "short" }).format(date);
    }

    function updateDashboard(content, graph, context) {
      const stateStages = parseStateStages(content);
      const graphBySlug = new Map(graph.map((stage) => [stage.slug, stage]));
      const currentSlug = stateField(content, "Current Stage");
      const nextSlug = stateField(content, "Next Stage");
      const current = graphBySlug.get(currentSlug);
      const next = graphBySlug.get(nextSlug);

      document.querySelectorAll(".stage-card").forEach((card) => {
        const stateStage = stateStages.get(card.dataset.stage);
        const graphStage = graphBySlug.get(card.dataset.stage);
        if (!stateStage) return;
        const presentation = statusPresentation(stateStage.status);
        card.dataset.status = stateStage.status;
        card.classList.remove("complete", "active", "approval", "revising", "pending", "skipped", "current");
        card.classList.add(presentation[1]);
        card.classList.toggle("current", card.dataset.stage === currentSlug);
        const badge = card.querySelector(".badge");
        badge.className = "badge " + presentation[1];
        badge.textContent = presentation[0];
        if (graphStage) {
          const agents = [graphStage.lead_agent].concat(graphStage.support_agents || []).filter(Boolean);
          const values = {
            condition: graphStage.execution === "ALWAYS"
              ? "현재 Scope의 실행 계획에 포함되는 필수 Stage입니다."
              : "Project Type과 Scope의 실행 조건이 충족될 때 수행합니다.",
            agents: agents.join(" · ") || "orchestrator",
            inputs: graphStage.consumes && graphStage.consumes.length
              ? "입력 산출물: " + graphStage.consumes.map((item) => item.artifact).join(", ")
              : "이전 Stage에서 승인된 상태와 사용자 입력",
            outputs: graphStage.produces && graphStage.produces.length
              ? "생성 산출물: " + graphStage.produces.join(", ")
              : "상태 정보와 감사 기록 갱신"
          };
          Object.entries(values).forEach(([key, value]) => {
            const target = card.querySelector('[data-detail="' + key + '"]');
            if (target) target.textContent = value;
          });
        }
      });

      const executable = [...stateStages.values()].filter((stage) => stage.status !== "skipped");
      const completedCount = executable.filter((stage) => stage.status === "completed").length;
      const remainingCount = executable.filter((stage) => stage.status === "pending").length;
      const percent = executable.length ? Math.round(completedCount / executable.length * 100) : 0;
      const phase = stateField(content, "Lifecycle Phase").toLowerCase();
      const pending = stateField(content, "Pending Artifacts") || "none";

      const currentStatus = stateStages.get(currentSlug)?.status;
      const currentName = stageNames[currentSlug] || (current && current.name) || currentSlug;
      const nextName = stageNames[nextSlug] || (next && next.name) || "Workflow 완료";
      const actionSuffix = currentStatus === "awaiting-approval"
        ? " 승인 여부 결정"
        : currentStatus === "revising" ? " 수정 사항 반영" : " 수행";

      setText("#scope-label", context.space + " Space · " + localizedConfigValue(stateField(content, "Scope")) + " Scope");
      setText("#intent-name", context.intent);
      setText("#project-type", localizedConfigValue(stateField(content, "Project Type")));
      setText("#depth-label", "Depth " + localizedConfigValue(stateField(content, "Depth")));
      setText("#test-label", "Test Strategy " + localizedConfigValue(stateField(content, "Test Strategy")));
      setText("#current-stage-name", currentName);
      setText("#overall-percent", percent + "%");
      setText("#overall-count", completedCount + " / " + executable.length + " Stage");
      setText("#metric-phase", phaseMeta[phase] ? phaseMeta[phase].label : phase);
      setText("#metric-completed", String(completedCount));
      setText("#metric-remaining", String(remainingCount));
      setText("#metric-agent", current
        ? current.lead_agent.replace("aidlc-", "").replace("-agent", "")
        : stateField(content, "Active Agent"));
      setText("#next-action", currentName + actionSuffix);
      setText("#current-purpose", stagePurposes[currentSlug] || (current && current.condition) || "현재 Stage의 정의된 작업을 완료합니다.");
      setText("#pending-artifacts", pending === "none" ? "누락 산출물 없음" : "대기 산출물: " + pending);
      setText("#next-stage-name", nextName);
      setText("#next-stage-purpose", stagePurposes[nextSlug] || (next && next.condition) || "현재 계획의 모든 Stage를 마무리합니다.");
      const orb = document.querySelector("#progress-orb");
      orb.style.background = "conic-gradient(var(--lime) " + percent + "%, rgba(255,255,255,.12) 0)";
      orb.setAttribute("aria-label", "전체 진행률 " + percent + "%");
      updateTime(document.querySelector("#last-updated"), stateField(content, "Last Updated"));

      document.querySelectorAll(".phase-section").forEach((section) => {
        const cards = [...section.querySelectorAll(".stage-card")];
        const activeCards = cards.filter((card) => card.dataset.status !== "skipped");
        const doneCards = activeCards.filter((card) => card.dataset.status === "completed");
        const phasePercent = activeCards.length ? Math.round(doneCards.length / activeCards.length * 100) : 0;
        const score = section.querySelector(".phase-score");
        score.querySelector("strong").textContent = phasePercent + "%";
        score.querySelector("span").textContent = doneCards.length + "/" + activeCards.length + " 완료";
        score.classList.toggle("active", section.dataset.phase === phase);
        section.querySelector(".phase-progress span").style.width = phasePercent + "%";
      });
      if (completedPhasesCollapsed) applyCompletedPhaseCollapse();
      applyFilters();
    }

    async function findActiveContext(root) {
      const spaceCursor = await readTextAt(root, ["aidlc", "active-space"]);
      const space = spaceCursor && spaceCursor.text.trim() ? spaceCursor.text.trim() : "default";
      const intents = await directoryAt(root, ["aidlc", "spaces", space, "intents"]);
      if (!intents) throw new Error("aidlc/spaces/" + space + "/intents를 찾을 수 없습니다.");
      const intentCursor = await childFile(intents, "active-intent");
      let intent = "";
      if (intentCursor) intent = (await (await intentCursor.getFile()).text()).trim();
      if (!intent) {
        const candidates = [];
        for await (const [name, handle] of intents.entries()) {
          if (handle.kind !== "directory") continue;
          if (await childFile(handle, "aidlc-state.md")) candidates.push(name);
        }
        if (candidates.length === 1) intent = candidates[0];
      }
      if (!intent) throw new Error("활성 Intent를 판별할 수 없습니다. /aidlc intent로 선택하세요.");
      const recordHandle = await childDirectory(intents, intent);
      if (!recordHandle) throw new Error("활성 Intent의 기록 디렉터리를 찾을 수 없습니다.");
      return { space, intent, recordHandle };
    }

    async function listIntentRecords(root, space) {
      const intents = await directoryAt(root, ["aidlc", "spaces", space, "intents"]);
      if (!intents) return [];
      let registry = [];
      const registryHandle = await childFile(intents, "intents.json");
      if (registryHandle) {
        try {
          registry = JSON.parse(await (await registryHandle.getFile()).text());
        } catch {
          registry = [];
        }
      }
      const registryByDir = new Map(registry.filter((item) => item.dirName).map((item) => [item.dirName, item]));
      const records = [];
      for await (const [name, handle] of intents.entries()) {
        if (handle.kind !== "directory" || !(await childFile(handle, "aidlc-state.md"))) continue;
        const item = registryByDir.get(name);
        const statusLabels = { "in-flight": "진행 중", completed: "완료", parked: "보류", blocked: "중단" };
        records.push({
          dirName: name,
          label: item
            ? (item.slug || name) + " · " + (statusLabels[item.status] || item.status || "상태 미확인")
            : name
        });
      }
      return records.sort((a, b) => a.label.localeCompare(b.label, "ko"));
    }

    async function populateIntentOptions(root, space, selectedIntent, activeIntent) {
      const records = await listIntentRecords(root, space);
      const previous = intentSelect.value;
      const preservePendingSelection =
        previous && previous !== liveState.intent && records.some((record) => record.dirName === previous);
      const selected = preservePendingSelection ? previous : selectedIntent;
      intentSelect.replaceChildren();
      records.forEach((record) => {
        const option = document.createElement("option");
        option.value = record.dirName;
        option.textContent = record.label + (record.dirName === activeIntent ? " · AI-DLC 활성" : "");
        option.selected = record.dirName === selected;
        intentSelect.append(option);
      });
      intentSelect.disabled = records.length < 2;
    }

    async function refreshLive(force) {
      if (!liveState.root || liveState.refreshing) return;
      liveState.refreshing = true;
      try {
        const activeContext = await findActiveContext(liveState.root);
        const selectedIntent = liveState.intent || activeContext.intent;
        const intents = await directoryAt(liveState.root, [
          "aidlc", "spaces", activeContext.space, "intents"
        ]);
        let recordHandle = intents ? await childDirectory(intents, selectedIntent) : null;
        const context = recordHandle
          ? { space: activeContext.space, intent: selectedIntent, recordHandle }
          : activeContext;
        await populateIntentOptions(
          liveState.root,
          context.space,
          context.intent,
          activeContext.intent
        );
        const stateHandle = await childFile(context.recordHandle, "aidlc-state.md");
        if (!stateHandle) throw new Error("aidlc-state.md를 찾을 수 없습니다.");
        const stateFile = await stateHandle.getFile();
        const contextChanged = context.space !== liveState.space || context.intent !== liveState.intent;
        if (!force && !contextChanged && stateFile.lastModified === liveState.stateModified) {
          document.querySelectorAll(".stage-card details[open]").forEach((details) => {
            refreshArtifacts(details.closest(".stage-card"));
          });
          return;
        }
        const graphResult = await readTextAt(liveState.root, [".kiro", "tools", "data", "stage-graph.json"]);
        if (!graphResult) throw new Error(".kiro stage graph를 찾을 수 없습니다.");
        const graph = JSON.parse(graphResult.text).filter((stage) => stage.enabled !== false);
        const stateText = await stateFile.text();
        liveState.graph = graph;
        liveState.space = context.space;
        liveState.intent = context.intent;
        liveState.recordHandle = context.recordHandle;
        liveState.stateModified = stateFile.lastModified;
        updateDashboard(stateText, graph, context);
        liveDot.className = "live-dot";
        connectButton.textContent = "읽기 권한 연결됨";
        const synced = new Intl.DateTimeFormat("ko-KR", { timeStyle: "medium" }).format(new Date());
        connectionStatus.textContent = "로컬 상태 읽음 · " + synced;
        document.querySelector("#data-mode").textContent = "새로고침 버튼을 누르면 선택한 Intent의 최신 로컬 상태를 읽습니다.";
      } catch (error) {
        liveDot.className = "live-dot syncing";
        connectionStatus.textContent = "동기화 오류 · " + (error instanceof Error ? error.message : String(error));
      } finally {
        liveState.refreshing = false;
      }
    }

    function openDatabase() {
      return new Promise((resolve, reject) => {
        const request = indexedDB.open("aidlc-dashboard", 1);
        request.onupgradeneeded = () => request.result.createObjectStore("handles");
        request.onsuccess = () => resolve(request.result);
        request.onerror = () => reject(request.error);
      });
    }

    async function saveRootHandle(handle) {
      const database = await openDatabase();
      await new Promise((resolve, reject) => {
        const transaction = database.transaction("handles", "readwrite");
        transaction.objectStore("handles").put(handle, "project-root");
        transaction.oncomplete = resolve;
        transaction.onerror = () => reject(transaction.error);
      });
      database.close();
    }

    async function savedRootHandle() {
      const database = await openDatabase();
      const handle = await new Promise((resolve, reject) => {
        const request = database.transaction("handles").objectStore("handles").get("project-root");
        request.onsuccess = () => resolve(request.result || null);
        request.onerror = () => reject(request.error);
      });
      database.close();
      return handle;
    }

    async function connectRoot(handle, requestAccess) {
      let permission = await handle.queryPermission({ mode: "read" });
      if (permission !== "granted" && requestAccess) {
        permission = await handle.requestPermission({ mode: "read" });
      }
      if (permission !== "granted") return false;
      if (!(await childDirectory(handle, "aidlc")) || !(await childDirectory(handle, ".kiro"))) {
        throw new Error("AI-DLC 프로젝트 루트를 선택해야 합니다.");
      }
      liveState.root = handle;
      try {
        await saveRootHandle(handle);
      } catch {
        // Some file:// browser profiles do not persist handles. Live reading still works.
      }
      await refreshLive(true);
      document.querySelectorAll(".stage-card details[open]").forEach((details) => {
        refreshArtifacts(details.closest(".stage-card"));
      });
      return true;
    }

    connectButton.addEventListener("click", async () => {
      try {
        if (!window.showDirectoryPicker) {
          throw new Error("이 브라우저는 로컬 폴더 연결을 지원하지 않습니다. Chrome 또는 Edge에서 열어주세요.");
        }
        const handle = await window.showDirectoryPicker({ mode: "read", id: "aidlc-project" });
        await connectRoot(handle, true);
      } catch (error) {
        if (error && error.name === "AbortError") return;
        openModal("프로젝트 연결", '<article class="document"><h1>연결할 수 없습니다</h1><p>' +
          escapeMarkup(error instanceof Error ? error.message : String(error)) +
          '</p><div class="connection-note">프로젝트의 최상위 폴더를 선택해야 합니다. 대시보드는 읽기 권한만 사용하며 AI-DLC 활성 Intent를 변경하지 않습니다.</div></article>');
      }
    });

    intentSelect.addEventListener("change", async () => {
      const target = intentSelect.value;
      if (!liveState.root || !target || target === liveState.intent) return;
      const previousIntent = liveState.intent;
      intentSelect.disabled = true;
      try {
        const intents = await directoryAt(liveState.root, [
          "aidlc", "spaces", liveState.space, "intents"
        ]);
        if (!intents || !(await childDirectory(intents, target))) {
          throw new Error("선택한 Intent의 기록 디렉터리를 찾을 수 없습니다.");
        }
        liveState.intent = target;
        liveState.stateModified = 0;
        await refreshLive(true);
        connectionStatus.textContent = "대시보드 보기 전환 · " + target;
      } catch (error) {
        liveState.intent = previousIntent;
        intentSelect.value = previousIntent;
        openModal("대시보드 보기 전환", '<article class="document"><h1>전환할 수 없습니다</h1><p>' +
          escapeMarkup(error instanceof Error ? error.message : String(error)) +
          '</p><div class="connection-note">이 기능은 대시보드 보기만 전환하며 active-intent 파일은 수정하지 않습니다.</div></article>');
      } finally {
        intentSelect.disabled = intentSelect.options.length < 2;
      }
    });

    document.querySelectorAll("[data-guide]").forEach((button) => {
      button.addEventListener("click", () => {
        const slug = button.dataset.guide;
        const card = document.querySelector('[data-stage="' + slug + '"]');
        const graphStage = liveState.graph.find((stage) => stage.slug === slug);
        const phase = card ? card.dataset.phase : graphStage?.phase;
        const name = stageNames[slug] || (card ? card.querySelector("h3").textContent : slug);
        const purpose = stagePurposes[slug] || "이 Stage에 정의된 결과를 생성합니다.";
        const status = card ? statusPresentation(card.dataset.status)[0] : "상태 미확인";
        const agents = graphStage
          ? [graphStage.lead_agent].concat(graphStage.support_agents || []).filter(Boolean).join(" · ")
          : card?.querySelector('[data-detail="agents"]')?.textContent || "orchestrator";
        const inputs = card?.querySelector('[data-detail="inputs"]')?.textContent || "이전 Stage에서 승인된 상태와 사용자 입력";
        const outputs = card?.querySelector('[data-detail="outputs"]')?.textContent || "상태 정보와 감사 기록 갱신";
        const phaseDescription = phaseMeta[phase]?.description || "정의된 AI-DLC 절차를 수행합니다.";
        const html = '<article class="document">' +
          '<h1>' + escapeMarkup(name) + '</h1>' +
          '<p><code>' + escapeMarkup(slug) + '</code> · 현재 상태: <strong>' + escapeMarkup(status) + '</strong></p>' +
          '<h2>Stage 목적</h2><p>' + escapeMarkup(purpose) + '</p>' +
          '<h2>전체 과정에서의 역할</h2><p>' + escapeMarkup(phaseDescription) + ' 이 Stage는 앞선 결정을 구체적인 다음 산출물로 연결합니다.</p>' +
          '<h2>진행 방법</h2><ul>' +
          '<li>입력 산출물과 현재 Scope의 결정 사항을 먼저 확인합니다.</li>' +
          '<li>모호함과 충돌을 질문으로 해소하고 필요한 분석 또는 설계를 수행합니다.</li>' +
          '<li>정의된 산출물을 작성한 뒤 센서와 검토 절차로 완전성을 확인합니다.</li>' +
          '<li>필요한 Approval Gate를 통과하면 다음 Stage로 진행합니다.</li>' +
          '</ul>' +
          '<h2>담당</h2><p>' + escapeMarkup(agents) + '</p>' +
          '<h2>입력</h2><p>' + escapeMarkup(inputs) + '</p>' +
          '<h2>완료 시 결과</h2><p>' + escapeMarkup(outputs) + '</p>' +
          '</article>';
        openModal(name + " · 상세 설명", html);
      });
    });

    document.querySelector("#glossary-button").addEventListener("click", () => {
      const cards = glossaryTerms.map((item) =>
        '<article class="term" data-term="' + escapeMarkup((item.term + " " + item.definition).toLowerCase()) +
        '"><h3>' + escapeMarkup(item.term) + '</h3><p>' + inlineMarkdown(item.definition) + "</p></article>"
      ).join("");
      openModal("AI-DLC 용어사전",
        '<div class="glossary-tools"><input id="term-search" type="search" placeholder="용어 또는 정의 검색"></div>' +
        '<div class="term-grid">' + cards + "</div>");
      const termSearch = document.querySelector("#term-search");
      termSearch.addEventListener("input", () => {
        const query = termSearch.value.trim().toLowerCase();
        modalBody.querySelectorAll(".term").forEach((term) => {
          term.classList.toggle("hidden", query && !term.dataset.term.includes(query));
        });
      });
      termSearch.focus();
    });

    async function collectArtifactFiles(card) {
      if (!liveState.recordHandle) return [];
      const phase = card.dataset.phase;
      const slug = card.dataset.stage;
      const graphStage = liveState.graph.find((stage) => stage.slug === slug);
      const allowed = new Set(graphStage?.produces || []);
      if (!allowed.size) return [];
      const results = [];
      const direct = await directoryAt(liveState.recordHandle, [phase, slug]);
      if (direct) {
        for await (const [name, handle] of direct.entries()) {
          const artifactName = name.endsWith(".md") ? name.slice(0, -3) : "";
          if (handle.kind === "file" && allowed.has(artifactName)) {
            results.push({ name: phase + "/" + slug + "/" + name, handle });
          }
        }
      }
      if (phase === "construction") {
        const construction = await childDirectory(liveState.recordHandle, phase);
        if (construction) {
          for await (const [unit, unitHandle] of construction.entries()) {
            if (unitHandle.kind !== "directory") continue;
            const stageDirectory = await childDirectory(unitHandle, slug);
            if (!stageDirectory) continue;
            for await (const [name, handle] of stageDirectory.entries()) {
              const artifactName = name.endsWith(".md") ? name.slice(0, -3) : "";
              if (handle.kind === "file" && allowed.has(artifactName)) {
                results.push({ name: phase + "/" + unit + "/" + slug + "/" + name, handle });
              }
            }
          }
        }
      }
      return results.sort((a, b) => a.name.localeCompare(b.name));
    }

    async function refreshArtifacts(card) {
      if (!liveState.root) return;
      const files = await collectArtifactFiles(card);
      let list = card.querySelector(".artifact-list");
      if (!list) {
        card.querySelector(".empty")?.remove();
        list = document.createElement("div");
        list.className = "artifact-list";
        card.querySelector(".artifact-title").after(list);
      }
      list.replaceChildren();
      files.forEach((item) => {
        const button = document.createElement("button");
        button.type = "button";
        button.textContent = item.name;
        button.addEventListener("click", async () => {
          const file = await item.handle.getFile();
          openModal(item.name, renderMarkdown(await file.text()));
        });
        list.append(button);
      });
      if (!files.length) {
        const empty = document.createElement("p");
        empty.className = "empty";
        empty.textContent = "아직 생성된 산출물이 없습니다.";
        card.querySelector(".artifact-title").after(empty);
      }
      card.querySelector(".artifact-count").textContent = String(files.length);
    }

    document.querySelectorAll(".stage-card details").forEach((details) => {
      details.addEventListener("toggle", () => {
        if (details.open && liveState.root) refreshArtifacts(details.closest(".stage-card"));
      });
    });

    (async () => {
      if (!window.showDirectoryPicker || !window.indexedDB) {
        connectButton.disabled = !window.showDirectoryPicker;
        return;
      }
      try {
        const handle = await savedRootHandle();
        if (handle && await handle.queryPermission({ mode: "read" }) === "granted") {
          await connectRoot(handle, false);
        }
      } catch {
        // A saved handle is only a convenience. Manual connection remains available.
      }
    })();

    let activeFilter = "all";
    const search = document.querySelector(".search");
    function applyFilters() {
      const query = search.value.trim().toLowerCase();
      document.querySelectorAll(".stage-card").forEach((card) => {
        const status = card.dataset.status;
        const matchesFilter = activeFilter === "all"
          || (activeFilter === "completed" && status === "completed")
          || (activeFilter === "todo" && !["completed", "skipped"].includes(status));
        const matchesSearch = !query || card.dataset.search.includes(query);
        card.classList.toggle("hidden", !(matchesFilter && matchesSearch));
      });
      document.querySelectorAll(".phase-section").forEach((section) => {
        const hasVisible = [...section.querySelectorAll(".stage-card")]
          .some((card) => !card.classList.contains("hidden"));
        section.classList.toggle("hidden", !hasVisible);
      });
    }
    document.querySelector("#go-current").addEventListener("click", () => {
      const current = document.querySelector(".stage-card.current");
      if (!current) {
        openModal("현재 작업", '<article class="document"><h1>진행 중인 Stage를 찾을 수 없습니다</h1><p>프로젝트를 연결하거나 AI-DLC 상태를 확인해 주세요.</p></article>');
        return;
      }
      activeFilter = "all";
      search.value = "";
      document.querySelectorAll(".filter").forEach((item) => {
        item.classList.toggle("selected", item.dataset.filter === "all");
      });
      applyFilters();
      const phase = current.closest(".phase-section");
      if (phase) setPhaseCollapsed(phase, false);
      current.scrollIntoView({ behavior: "smooth", block: "center" });
      current.animate(
        [{ transform: "scale(1)" }, { transform: "scale(1.012)" }, { transform: "scale(1)" }],
        { duration: 650, easing: "ease-out" }
      );
    });
    document.querySelector("#refresh-dashboard").addEventListener("click", async (event) => {
      const button = event.currentTarget;
      if (!liveState.root) {
        connectButton.click();
        return;
      }
      button.disabled = true;
      button.textContent = "새로고침 중…";
      try {
        await refreshLive(true);
      } finally {
        button.disabled = false;
        button.textContent = "새로고침";
      }
    });
    document.querySelectorAll(".filter").forEach((button) => {
      button.addEventListener("click", () => {
        document.querySelectorAll(".filter").forEach((item) => item.classList.remove("selected"));
        button.classList.add("selected");
        activeFilter = button.dataset.filter;
        applyFilters();
      });
    });
    search.addEventListener("input", applyFilters);

    let expanded = false;
    document.querySelector("#expand-all").addEventListener("click", (event) => {
      expanded = !expanded;
      document.querySelectorAll(".stage-card details").forEach((details) => {
        details.open = expanded;
      });
      event.currentTarget.textContent = expanded ? "모든 상세 접기" : "모든 상세 펼치기";
    });
  </script>
</body>
</html>`;

  writeFileSync(outputPath, html.replace(/[ \t]+$/gm, ""), "utf8");
  process.stdout.write(`${outputPath}\n`);
}

try {
  buildDashboard();
} catch (error) {
  const message = error instanceof Error ? error.message : String(error);
  process.stderr.write(`aidlc-dashboard: ${message}\n`);
  process.exit(1);
}
