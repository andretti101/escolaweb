# Original User Request

## 2026-09-27T23:44:58Z

# Teamwork Project Prompt — Draft

> Status: Launched
> Goal: Craft prompt → get user approval → delegate to teamwork_preview
> Requested team: Small, focused team

This is a single self-contained fix; keep it small and focused. Fix missing characters (not just accents, but entire missing characters/letters like 'a' in 'obrigatrio' -> 'obrigatório' or 'i' in 'diviso' -> 'divisão') in user-facing strings (like validation messages and exception messages) across the Model, DTO, and Service layers. Do not alter any core logic.

Working directory: c:/escolaweb
Integrity mode: demo

## Requirements

### R1. Fix DTO Layer
Find and restore missing Portuguese characters and accents in all DTO classes (e.g. `@NotNull` and `@Size` message attributes).

### R2. Fix Model Layer
Find and restore missing Portuguese characters and accents in all Entity and Enum classes (e.g. `@NotBlank` validation messages).

### R3. Fix Service Layer
Find and restore missing Portuguese characters and accents in all Service implementations (e.g. `IllegalArgumentException` or `RuntimeException` messages).

## Acceptance Criteria

### Compilation
- [ ] The project successfully compiles using `./mvnw clean compile` after all changes.

### Integrity
- [ ] `git diff` shows ONLY modifications inside string literals (messages) in the specified layers. No structural code, variables, or annotations have been added or removed.
