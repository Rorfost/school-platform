# Contributing to Rorfost School Platform

1. Read [AGENT.md](AGENT.md), [CODE_RULES.md](docs/engineering/CODE_RULES.md), and [SECURITY.md](SECURITY.md).
2. Create a focused feature branch from the current default branch (e.g. `feat/multi-tenancy`).
3. Keep changes tightly scoped, documented, and covered by automated tests.
4. Run frontend verification (`npm run format:check && npm run lint && npm run typecheck && npm run test && npm run build`) and backend tests (`mvn verify` or `mvn test`) before opening a pull request.
5. Use clear, conventional commit messages.

## Data Safety & Privacy
Never submit live credentials, production configurations, real student records, examination PINs, raw database dumps, or private school workbooks. Use synthetic fixtures and sanitized test data exclusively.

## Licensing Notice
This repository is publicly accessible for inspection and reference but is **not** licensed as open source. Contributions or pull requests do not grant rights to redistribute, deploy, sublicense, or commercially exploit this software outside the terms specified in [LICENSE](LICENSE) and [NOTICE](NOTICE). Contributor rights assignments and ownership terms remain subject to human review.
