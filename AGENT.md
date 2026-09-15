# Working Agreement

This file applies to every developer and coding tool working in this repository.

## Before work

1. Read this file, the root README, the PRD, architecture, decisions, and code rules.
2. Read documentation relevant to the task (a lil bit from web and from Root/docs folder).
3. Check the current branch and `git status`.
4. Review relevant recent commits before changing existing code.

## Scope and decisions

Work only on the requested scope. Do not silently change architecture, add libraries for fashion, make speculative abstractions, or improve unrelated files. When a business requirement is absent, document the uncertainty or leave a clear TODO instead of inventing behavior.

## Excel rule

**Never infer or invent the result Excel structure.** Until the owner provides the actual workbook/template, do not implement Excel parsing business logic. See [RESULT_IMPORT.md](docs/data/RESULT_IMPORT.md).

## Gujarati UI and design

All user-facing text must be clear, natural, simple Gujarati unless explicitly approved otherwise. English remains appropriate for code, APIs, database names, logs, and developer documentation. Design for Standard 1–8 students, parents, and the principal: mobile-first, accessible, calm, clean, professional, and easy to understand. Avoid excessive gradients, glassmorphism, huge animated heroes, floating decoration, neon, unnecessary motion, and generic SaaS copy.

## Code quality

Prefer simple, explicit, readable, tested, cohesive, secure code. Avoid dead code, duplicated business logic, giant methods/components, and cleverness without a concrete benefit. Follow [CODE_RULES.md](docs/engineering/CODE_RULES.md).

Comments explain why, constraints, non-obvious behavior, security choices, or business rules—not line-by-line code. Use concise professional English. Never add automated-assistance attribution to commits, comments, source, or documentation.

## Commits and documentation

Make small, logical, professional commits after meaningful, tested units of work. Before committing, run checks relevant to changed areas. Update documentation in the same change when architecture, config, API, database, security, deployment, or workflows change.
