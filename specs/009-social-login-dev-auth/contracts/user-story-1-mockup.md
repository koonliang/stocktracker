# Mockup: User Story 1 MVP

Frontend validation target for the first MVP of the enhanced `dev`-mode auth
screen.

## Goal

Provide a simple, reviewable design for User Story 1 before backend integration
is complete. The mockup focuses on the non-production sign-in entry surface.

## Layout

```text
+------------------------------------------------------+
| StockTracker                              DEV PREVIEW |
|                                                      |
| Sign in                                              |
| Access your portfolio, watchlists, and analysis.     |
|                                                      |
| [ Email __________________________________________ ] |
| [ Password _______________________________________ ] |
|                                                      |
| [ Sign in                                         ] |
|                                                      |
|                 or continue with                    |
|                                                      |
|                (G)            (f)                   |
|              Google        Facebook                 |
|                                                      |
| --------------------------------------------------  |
| Demo accounts                                        |
| Use a seeded demo profile for quick testing.         |
|                                                      |
| [ Demo User 1 ]  seeded portfolio data              |
| [ Demo User 2 ]  seeded portfolio data              |
| [ Create Demo User ]                                |
|                                                      |
| Create account        Forgot password?               |
+------------------------------------------------------+
```

## Interaction Notes

- The standard email/password form remains the primary action area in `dev` mode.
- Google and Facebook appear as icon-led affordances, not full-width provider
  buttons.
- Demo users appear below a separator and are clearly labeled as quick-entry
  seeded accounts.
- Selecting a demo user signs in immediately with no password prompt.
- The mockup is intentionally compact so desktop and mobile adaptation can be
  reviewed early.

## Validation Checklist

- The screen communicates that it is a non-production/dev environment.
- Social entry is visually distinct but secondary to the main sign-in form.
- Demo-user quick access is obvious and visibly different from normal accounts.
- The composition supports later integration of Vercel Analytics and Speed
  Insights without changing the auth-screen structure.
