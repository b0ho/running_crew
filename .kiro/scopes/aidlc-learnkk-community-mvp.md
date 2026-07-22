---
name: learnkk-community-mvp
depth: Standard
keywords: []
description: Composed standard-depth greenfield build for the LearnKK community MVP
skeleton: on
runner: true
---

# learnkk-community-mvp scope

A composed, Standard-depth workflow for building the LearnKK community MVP as a
greenfield product. It runs a lean ideation-through-construction spine: capture
intent, define scope, sketch and refine the UX, discover practices, analyze
requirements, design the application and its NFRs and infrastructure, then
generate, build, test, and wire up CI. The operation phase and the
discovery/decomposition overhead that only pays off at scale are skipped.

## Why these stages, why skip those

Because this is a greenfield community product with real UX and design weight,
the inception and construction passes stay EXECUTE — the MVP still needs
requirements, application/functional design, NFR and infrastructure design,
code, tests, and a CI pipeline. Mockups run in both rough and refined form
because the UX is a primary concern. Practices-discovery runs to establish
conventions for the new codebase.

The skips fall into three groups. Ideation overhead that does not pay off yet:
market-research and team-formation. Stages with no producer or consumer in a
greenfield MVP: reverse-engineering (nothing to reverse — no existing codebase)
and user-stories (requirements-analysis plus refined-mockups carry the persona
and acceptance narrative). The entire operation phase — deployment-pipeline,
environment-provisioning, deployment-execution, observability-setup,
incident-response, performance-validation, feedback-optimization — is skipped
wholesale; an MVP proves the product before it carries production operations
weight. Feasibility folds into application-design since the approach is a
standard pattern.

## Membership

Composed scope with empty keywords — it resolves only by `--scope
learnkk-community-mvp` and never participates in keyword inference. Making it
inferable is an explicit human choice at the gate.
