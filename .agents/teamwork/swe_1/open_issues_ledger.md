# Open Issues Ledger

- [Round 1 Implementer] Runtime validation triggering over HTTP was not verified against a running PostgreSQL database because PostgreSQL is not running on localhost:5432 in this environment.
- [Round 1 Implementer] Spring integration tests require an active PostgreSQL instance at localhost:5432 and therefore cannot execute in offline/database-less CI environments without a test profile/mock datasource.
- [Round 1 Implementer] Reviewers should verify that API endpoints consuming YearConclusionRequestDTO return the exact restored Portuguese strings in the errors array when given empty/invalid payloads, and test that AcademicYearConclusionServiceImpl.concludeYear and ReportCardServiceImpl.findStudentPerformanceByYear throw their expected exception messages when invoked with invalid state against a running database.
