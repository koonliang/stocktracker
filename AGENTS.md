<!-- SPECKIT START -->
For additional context about technologies to be used, project structure,
shell commands, and other important information, read
`specs/011-transaction-toasts-live-seed/plan.md`.
<!-- SPECKIT END -->

Backend test naming:
- Unit tests end with `*Test` and must not use `@QuarkusTest` or Testcontainers.
- Integration tests end with `*IT` and may use `@QuarkusTest`, `@QuarkusTestResource`, and Testcontainers.
