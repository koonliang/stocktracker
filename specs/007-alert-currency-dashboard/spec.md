# Feature Specification: Alert Notifications and Currency Views

**Feature Branch**: `007-alert-currency-dashboard`  
**Created**: 2026-06-17  
**Status**: Draft  
**Input**: User description: "notification dialog with the list of alerts triggered; tag each transaction with a currency; user can choose to view dashboard and performance using user-chosen base currency"

## Clarifications

### Session 2026-06-17

- Q: When should a triggered alert notification be re-armed for another notification? → A: Once per crossing; re-arm only after the condition clears and crosses again.
- Q: How should older cash-only transactions without an instrument currency be backfilled? → A: Backfill with the user's current base currency.
- Q: How should converted values behave when an exact-date FX rate is unavailable? → A: Use the latest prior FX rate and mark the value as stale.
- Q: Which FX date should be used for converted transactions and current holdings? → A: Use transaction-date FX for transactions and valuation-date FX for holdings.
- Q: How long should triggered-alert notification history be retained? → A: Keep notification history until the user deletes it or deletes the alert.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Approve Notification Dialog Mockup (Priority: P1)

As an investor, I want to see and approve a frontend mockup of the triggered-alert notification dialog before the feature is built so the alert review experience is clear and usable.

**Why this priority**: The notification dialog is the primary interaction for this feature. A mockup gives stakeholders a shared target for layout, content hierarchy, and expected actions before deeper implementation work begins.

**Independent Test**: Review the mockup with representative triggered alerts, no-alert content, unread counts, and read actions, then confirm that the dialog communicates what triggered and what the user can do next.

**Acceptance Scenarios**:

1. **Given** there are triggered alerts to display, **When** the notification dialog mockup is reviewed, **Then** the mockup shows a dialog title, unread count, list of triggered alerts, symbol, trigger condition, observed value, trigger time, read state, and actions to mark one or all notifications as read.
2. **Given** there are no triggered alerts, **When** the notification dialog mockup is reviewed, **Then** the mockup shows a clear empty state without implying that alerts are broken or unavailable.
3. **Given** the notification dialog contains more triggered alerts than fit comfortably on screen, **When** the mockup is reviewed, **Then** the mockup shows how the user can scan or scroll the list while keeping close and read actions understandable.

**Frontend Mockup**:

```text
+--------------------------------------------------+
| Triggered Alerts                         X       |
| 3 unread                                          |
|                                                  |
| [Unread] AAPL  Price above 200.00 USD             |
|          Observed 203.18 USD    Today, 10:42      |
|          Mark read                               |
|                                                  |
| [Unread] TSLA  Price below 175.00 USD             |
|          Observed 172.64 USD    Today, 10:39      |
|          Mark read                               |
|                                                  |
| [Read]   MSFT  Daily change above 5%              |
|          Observed +5.4%         Yesterday, 15:58   |
|                                                  |
| No more triggered alerts                         |
|                                                  |
|                         Mark all read   Close     |
+--------------------------------------------------+
```

---

### User Story 2 - Review Triggered Alerts (Priority: P2)

As an investor tracking live prices, I want a notification dialog that lists the alerts that have been triggered so I can quickly understand which holdings or watchlist items need attention.

**Why this priority**: Alerts lose value if users cannot see what fired, when it fired, and why. This is the most direct user-facing outcome of alert monitoring.

**Independent Test**: Create multiple active alerts, simulate prices that trigger at least two of them, open the notification dialog, and verify that the triggered alerts are listed with enough context to act on them.

**Acceptance Scenarios**:

1. **Given** a user has active alerts and one or more alerts are triggered, **When** the user opens the notification dialog, **Then** the dialog shows each triggered alert with symbol, trigger condition, observed value, trigger time, and read/unread status.
2. **Given** a user has reviewed triggered alerts in the notification dialog, **When** the user marks one alert notification as read, **Then** that notification is no longer counted as unread while remaining available in the dialog history.
3. **Given** no alerts have been triggered for the user, **When** the user opens the notification dialog, **Then** the dialog clearly shows that there are no triggered alerts to review.
4. **Given** a user has historical triggered alert notifications, **When** the user opens the notification dialog history, **Then** notifications remain available until the user deletes them or deletes the associated alert.

---

### User Story 3 - Record Transaction Currency (Priority: P3)

As an investor entering or importing portfolio activity, I want each transaction to include the currency used for that transaction so that cost, proceeds, fees, dividends, and cash movements remain accurate across markets.

**Why this priority**: Currency is foundational for accurate portfolio valuation and performance. Without transaction currency, converted dashboard and performance numbers can be misleading.

**Independent Test**: Add and import transactions for instruments in different markets, assign currencies to each transaction, and verify that the transaction record preserves and displays the selected currency.

**Acceptance Scenarios**:

1. **Given** a user creates a buy, sell, dividend, fee, deposit, withdrawal, or similar transaction, **When** the user saves the transaction, **Then** the transaction includes a valid currency and displays monetary values using that currency.
2. **Given** an instrument has a known trading currency, **When** a user starts a new transaction for that instrument, **Then** the transaction currency defaults to the instrument currency while still allowing the user to choose another supported currency.
3. **Given** a user imports transactions with currency values, **When** the import is completed, **Then** each imported row retains its transaction currency or reports a row-level validation issue if the currency is missing or unsupported.

---

### User Story 4 - View Dashboard and Performance in Base Currency (Priority: P4)

As an investor with assets and transactions in multiple currencies, I want to choose a base currency for dashboard and performance views so I can evaluate my portfolio in the currency that matters to me.

**Why this priority**: Base-currency views make multi-currency portfolios comparable and understandable, but they depend on captured transaction currencies and available conversion rates.

**Independent Test**: Set a base currency, hold positions and transactions in at least two different currencies, and verify that dashboard totals and performance results are shown in the selected base currency with the selected preference preserved.

**Acceptance Scenarios**:

1. **Given** a user chooses a base currency, **When** the user views the dashboard, **Then** portfolio totals, gains/losses, and cash-related values are shown in that base currency.
2. **Given** a user chooses a base currency, **When** the user views performance, **Then** returns, contributions, realized results, and income summaries use that base currency for monetary comparisons.
3. **Given** a user changes the base currency from one supported currency to another, **When** dashboard and performance views refresh, **Then** all converted monetary values update consistently and the preference is used in future sessions.

### Edge Cases

- If an alert condition remains satisfied across multiple refreshes, the user receives one notification for that crossing; the alert re-arms only after the condition clears and later crosses the threshold again.
- If an alert triggers more than once before the user opens the dialog, the user can distinguish repeated notifications only when they represent separate threshold crossings.
- If a user deletes an alert, the notification history for that alert is also removed.
- If multiple alerts trigger at the same refresh time, all triggered alerts appear in a deterministic order, such as newest first with symbol as a tie-breaker.
- If a transaction currency is missing from older data, the system assigns the instrument currency where it can be inferred; older cash-only transactions without an instrument currency are assigned the user's current base currency.
- If a selected base currency does not have an exact-date conversion rate for a required date, the affected dashboard or performance value uses the latest prior FX rate and is clearly marked as stale.
- If a user has no multi-currency holdings, the dashboard and performance views still respect the chosen base currency and show values without unnecessary conversion warnings.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: System MUST provide a reviewable frontend mockup for the triggered-alert notification dialog before implementation planning is finalized.
- **FR-002**: The notification dialog mockup MUST show dialog title, unread count, triggered alert list, empty state, close action, individual mark-read action, and mark-all-read action.
- **FR-003**: The notification dialog mockup MUST show each alert row with symbol, alert condition, observed value, trigger time, and read/unread state.
- **FR-004**: System MUST provide a notification dialog that lists triggered alerts for the signed-in user.
- **FR-005**: Triggered alert notifications MUST include symbol, alert condition, threshold or target value, observed value at trigger time, trigger timestamp, and read/unread state.
- **FR-006**: Users MUST be able to open and close the notification dialog from the main application experience.
- **FR-007**: Users MUST be able to mark individual triggered alert notifications as read.
- **FR-008**: Users MUST be able to mark all visible triggered alert notifications as read.
- **FR-009**: System MUST keep triggered alert notifications scoped to the user who owns the alert.
- **FR-010**: System MUST preserve a notification history for triggered alerts so users can review prior triggers after marking them as read.
- **FR-011**: System MUST create at most one notification per alert threshold crossing and MUST re-arm that alert for another notification only after the condition clears and crosses the threshold again.
- **FR-012**: System MUST retain triggered alert notification history until the user deletes the notification or deletes the associated alert.
- **FR-013**: System MUST require every new portfolio transaction to have a valid transaction currency.
- **FR-014**: System MUST support transaction currency for buys, sells, dividends, fees, deposits, withdrawals, and other transaction types that contain monetary amounts.
- **FR-015**: System MUST default transaction currency from the related instrument currency when that currency is known.
- **FR-016**: Users MUST be able to choose a supported transaction currency when creating or editing a transaction.
- **FR-017**: System MUST validate imported transaction currency values and report row-level issues for missing or unsupported currencies.
- **FR-018**: System MUST display each transaction's monetary amounts using that transaction's currency wherever transactions are listed or reviewed.
- **FR-019**: System MUST preserve transaction currency when transactions are exported and re-imported.
- **FR-020**: Users MUST be able to choose and save a base currency for dashboard and performance views.
- **FR-021**: System MUST use the saved base currency when presenting dashboard monetary totals, including portfolio value, gain/loss, income, fees, and cash-related values.
- **FR-022**: System MUST use the saved base currency when presenting performance monetary values, including realized results, contributions, income summaries, and per-holding monetary contribution.
- **FR-023**: System MUST convert transaction-based monetary values using the transaction date's FX rate and current holding values using the dashboard or performance view's valuation-date FX rate.
- **FR-024**: System MUST keep percentage returns mathematically consistent with the converted monetary values shown in the selected base currency.
- **FR-025**: System MUST use the latest prior FX rate when an exact-date conversion rate is unavailable and MUST clearly mark affected converted values as stale.
- **FR-026**: System MUST persist the user's base-currency preference across sessions.
- **FR-027**: System MUST keep each user's base-currency preference separate from other users.
- **FR-028**: System MUST backfill existing transactions created before transaction currency was required by using the related instrument currency where it can be inferred and the user's current base currency for cash-only transactions without an instrument currency.

### Key Entities *(include if feature involves data)*

- **Triggered Alert Notification**: A user-visible record that an alert fired, including alert ownership, symbol, condition, threshold, observed value, trigger time, read state, history state, and the crossing event that produced it; retained until the user deletes it or deletes the associated alert.
- **Portfolio Transaction**: A recorded portfolio activity with monetary values and a required transaction currency, related to an instrument where applicable and the owning user; older cash-only records are backfilled with the user's current base currency.
- **Currency**: A supported ISO-style currency choice used for transactions and base-currency views.
- **Base Currency Preference**: A per-user setting that determines the currency used for dashboard and performance monetary displays.
- **Converted Portfolio Metric**: A dashboard or performance value displayed in the user's selected base currency, with transaction-date FX for transaction-based values, valuation-date FX for current holdings, and conversion status indicating current, stale, or unavailable conversion data where relevant.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Stakeholders can review the notification dialog mockup and confirm the intended content, empty state, and read actions in under 5 minutes.
- **SC-002**: Users can open the notification dialog and identify which alerts triggered in under 10 seconds after the dialog opens.
- **SC-003**: 100% of triggered alert notifications shown in the dialog include symbol, condition, observed value, trigger time, and read/unread state.
- **SC-004**: Users can mark all unread triggered alert notifications as read in no more than two user actions.
- **SC-005**: 100% of newly created transactions contain a valid currency before they can be saved.
- **SC-006**: At least 95% of imported transaction rows with valid currency data complete without manual correction.
- **SC-007**: Users can change the dashboard and performance base currency in under 30 seconds.
- **SC-008**: Dashboard and performance monetary values reflect the selected base currency consistently across at least 99% of supported multi-currency view states.
- **SC-009**: At least 90% of users reviewing a mixed-currency portfolio can correctly identify the selected base currency and whether any values are unavailable or stale due to conversion data.

## Assumptions

- Supported currencies follow the currencies already available for instruments, transactions, and exchange-rate conversion in the live-data analytics feature.
- In-app notifications are in scope; email, SMS, and push delivery are out of scope for this feature.
- Existing live quote, alert evaluation, instrument currency, and currency conversion capabilities are available from the live-data analytics work.
- Existing signed-in user boundaries apply to alerts, notifications, transactions, and preferences.
- Existing transactions without an explicit currency can usually be backfilled from the related instrument currency; transactions that cannot be inferred require user review.
