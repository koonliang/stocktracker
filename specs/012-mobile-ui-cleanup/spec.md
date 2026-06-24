# Feature Specification: Mobile UI Declutter and Navigation Improvement

**Feature Branch**: `012-mobile-ui-cleanup`  
**Created**: 2026-06-24  
**Status**: Draft  
**Input**: User description: "Unnecessary details cluttering UI for mobile view. Prominent details must be visible on the page in mobile view without scrolling. Dashboard Page: Remove 'Add a symbol' and Allocation section. Trade Page: Hide Manual Entry and Import section. Returns Page: Remove FIFO/LIFO. Chart should be seen without scrolling. Alerts Page: Hide Create section. Bottom Navigation: Maybe just show icon, currently selected navigation should be more visible."

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Dashboard At-A-Glance (Priority: P1)

A user opens the app on their phone and immediately sees their most important portfolio information — total value, daily change, and top holdings — without scrolling or interacting with any UI elements. The screen is uncluttered by secondary actions.

**Why this priority**: The dashboard is the entry point for every session. If key metrics are buried, users lose trust in the app's usability. This is the highest-impact change.

**Independent Test**: Open the dashboard on a phone. Verify that portfolio summary (total value, daily gain/loss, percentage change) and the holdings list are visible without any scrolling or tapping.

**Acceptance Scenarios**:

1. **Given** a user opens the app, **When** the Dashboard page loads, **Then** portfolio total value, daily change amount, and daily change percentage are visible above the fold without scrolling
2. **Given** the Dashboard loads, **When** the user views the screen, **Then** the "Add a symbol" shortcut button is NOT visible on the main dashboard view
3. **Given** the Dashboard loads, **When** the user views the screen, **Then** the Allocation chart/section is NOT visible on the main dashboard view
4. **Given** a user wants to add a symbol, **When** they tap a floating action button or a dedicated "+" icon, **Then** the add-symbol flow is accessible
5. **Given** a user wants to view portfolio allocation, **When** they navigate to a dedicated section or swipe/tap a tab, **Then** the Allocation view is accessible

---

### User Story 2 - Trade Page Streamlined Entry (Priority: P2)

A user navigating to the Trade page sees their recent trade history and a clear action trigger to add a new trade, without Manual Entry or Import forms cluttering the initial view.

**Why this priority**: The Trade page's primary read use case (viewing history) is obscured by heavy form UI. Hiding forms behind an action preserves readability while keeping functionality accessible.

**Independent Test**: Navigate to Trade page. Verify that the Manual Entry and Import forms are not displayed by default, but can be revealed through a clear user action.

**Acceptance Scenarios**:

1. **Given** a user navigates to the Trade page, **When** the page loads, **Then** Manual Entry and Import forms are hidden and only trade history / summary is shown
2. **Given** the Trade page is loaded, **When** the user taps the floating action button (FAB) or a clearly labelled trigger, **Then** options for "Manual Entry" and "Import" are presented (e.g., bottom sheet, modal, or expandable menu)
3. **Given** the user selects "Manual Entry" from the revealed options, **When** they proceed, **Then** the manual trade entry form opens
4. **Given** the user selects "Import", **When** they proceed, **Then** the import flow opens
5. **Given** the Trade page loads, **When** viewed on a phone, **Then** recent trades are visible above the fold without scrolling

---

### User Story 3 - Returns Chart Without Scrolling (Priority: P2)

A user opens the Returns page and sees the performance chart immediately, without any scrolling required. FIFO/LIFO calculation method is not shown on the main view.

**Why this priority**: The chart is the primary value of the Returns page. Users need to see performance trends at a glance; calculation methodology is an advanced/settings-level concern.

**Independent Test**: Navigate to Returns page on a phone. Verify the performance chart is fully visible without scrolling, and FIFO/LIFO selector/label is not present.

**Acceptance Scenarios**:

1. **Given** a user navigates to the Returns page, **When** the page loads, **Then** the performance chart is fully visible without any vertical scrolling
2. **Given** the Returns page loads, **When** the user views the screen, **Then** no FIFO/LIFO selector, label, or toggle is displayed
3. **Given** the Returns page is visible, **When** the user views it, **Then** key return metrics (e.g., total return, percentage gain/loss) are shown above or alongside the chart without scrolling
4. **Given** the chart is displayed, **When** the user taps a date range option, **Then** the chart updates without navigating away or requiring scroll

---

### User Story 4 - Alerts Page with Hidden Create Form (Priority: P3)

A user opens the Alerts page and sees their existing alerts list clearly. The Create Alert form is not shown by default and is accessible via a clear action trigger.

**Why this priority**: Most Alerts page sessions are for reviewing or managing existing alerts, not creating new ones. The creation form should not dominate the view.

**Independent Test**: Navigate to the Alerts page. Verify existing alerts are visible without scrolling and the Create Alert form is hidden until triggered.

**Acceptance Scenarios**:

1. **Given** a user navigates to the Alerts page, **When** the page loads, **Then** the Create Alert form is hidden
2. **Given** the Alerts page is loaded, **When** the user taps the FAB or a "+" button, **Then** the Create Alert form or modal is revealed
3. **Given** the Alerts page loads, **When** the user views the screen, **Then** existing alerts are listed and visible without scrolling
4. **Given** the user creates a new alert via the triggered form, **When** they submit, **Then** the form closes and the new alert appears in the list

---

### User Story 5 - Icon-Only Bottom Navigation with Clear Active State (Priority: P1)

A user navigating between pages sees a clean icon-only bottom navigation bar. The currently active page is immediately and clearly distinguishable from inactive tabs.

**Why this priority**: Navigation is used on every screen, every session. A cramped or visually ambiguous navigation bar degrades the entire app experience.

**Independent Test**: Navigate between all pages. Verify: only icons are shown (no labels), the active tab is clearly distinct (color, fill, or indicator), and all tabs are tappable.

**Acceptance Scenarios**:

1. **Given** a user is on any page, **When** they view the bottom navigation bar, **Then** only icons are displayed with no text labels
2. **Given** a user is on the Dashboard, **When** they view the navigation bar, **Then** the Dashboard icon is visually distinct from all other icons (e.g., filled icon, accent color, indicator dot/bar)
3. **Given** a user taps a different navigation icon, **When** the new page loads, **Then** the newly selected icon becomes visually active and the previous icon returns to inactive state
4. **Given** the Alerts page has unread or active alerts, **When** the user views the navigation bar, **Then** a badge count is displayed on the Alerts icon
5. **Given** the navigation bar is rendered, **When** viewed on small screen widths, **Then** all navigation icons remain fully visible and tappable within the safe area

---

### Edge Cases

- What happens when the user has no trades/holdings? The dashboard should still show a clear empty state without exposing the hidden "Add a symbol" button in the main view.
- How does the app handle the FAB on pages where create is hidden? The FAB must always be accessible and not obscured by content when the page scrolls.
- What if a user is mid-form (Manual Entry) and navigates away via the bottom bar? The system should prompt to confirm or save draft before discarding.
- What happens on very small screen heights (e.g., iPhone SE)? The chart on Returns must still render without requiring scroll; layout must adapt.
- What if there are no existing alerts? The Alerts page should show an empty state with a prompt directing the user to the FAB.

## Requirements *(mandatory)*

### Functional Requirements

**Dashboard Page**

- **FR-001**: The "Add a symbol" action MUST be removed from the main dashboard view and accessible only through a secondary interaction (e.g., FAB or header icon)
- **FR-002**: The Allocation section MUST be removed from the main dashboard view; it MAY be accessible via a dedicated tab, drawer, or separate page
- **FR-003**: Portfolio summary metrics (total value, total daily change, daily change percentage) MUST be displayed above the fold on the dashboard without requiring scroll
- **FR-004**: The holdings list MUST appear on the dashboard below the summary metrics

**Trade Page**

- **FR-005**: The Manual Entry form MUST be hidden by default on the Trade page
- **FR-006**: The Import section MUST be hidden by default on the Trade page
- **FR-007**: A floating action button (FAB) or equivalent prominent trigger MUST be present on the Trade page to reveal Manual Entry and Import options
- **FR-008**: Selecting "Manual Entry" from the trigger MUST open the entry form (bottom sheet or modal)
- **FR-009**: Selecting "Import" from the trigger MUST open the import flow (bottom sheet or modal)
- **FR-010**: Recent trade history MUST be visible above the fold when the Trade page loads

**Returns Page**

- **FR-011**: The FIFO/LIFO calculation method selector or label MUST be removed from the Returns page view
- **FR-012**: The performance chart MUST be rendered fully visible without requiring any vertical scrolling
- **FR-013**: Key return summary metrics (total return, percentage gain/loss) MUST be visible on the same screen as the chart without scrolling
- **FR-014**: Date range selection controls for the chart MUST be compact and not push the chart below the fold

**Alerts Page**

- **FR-015**: The Create Alert form MUST be hidden by default when the Alerts page loads
- **FR-016**: A FAB or "+" button MUST be present on the Alerts page to trigger the Create Alert form or modal
- **FR-017**: The existing alerts list MUST be visible above the fold when the Alerts page loads
- **FR-018**: After creating an alert through the triggered form, the form MUST close and the new alert MUST appear in the list

**Bottom Navigation**

- **FR-019**: The bottom navigation bar MUST display icons only, with no text labels
- **FR-020**: The currently active navigation item MUST have a visually distinct style compared to inactive items (e.g., filled icon, accent color highlight, or active indicator bar)
- **FR-021**: The navigation bar MUST display a badge count on the Alerts icon when there are active or unread alerts
- **FR-022**: All navigation icons MUST be fully visible and tappable within the device safe area on all supported screen sizes

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Users can see their portfolio total value, daily change, and at least 3 holdings on the Dashboard without any scrolling on a standard phone screen
- **SC-002**: Users can initiate a trade entry within 2 taps from the Trade page (tap FAB → tap "Manual Entry")
- **SC-003**: The Returns chart is fully visible on page load without scrolling on all phones with screen height ≥ 667px
- **SC-004**: Users can view all existing alerts on the Alerts page without scrolling when fewer than 5 alerts exist
- **SC-005**: The active navigation tab is identifiable at a glance; in user testing, 95% of users correctly identify the active page from the navigation bar alone
- **SC-006**: Navigation between any two pages requires exactly 1 tap on the bottom navigation bar
- **SC-007**: Time-to-first-meaningful-view on Dashboard decreases by eliminating above-the-fold clutter (user perceives less visual noise on the first screen)

## Assumptions

- The Allocation section removed from Dashboard will remain accessible elsewhere (e.g., a dedicated Portfolio or Analysis page); it is not being deleted from the app entirely
- FIFO/LIFO is a settings-level preference, not a per-session choice; it will be moved to app settings rather than removed entirely
- The FAB pattern (floating action button) is consistent with the app's existing design language or will be introduced as the standard for hidden-form reveals across the app
- All changes are scoped to mobile view (phones); tablet or desktop layouts are out of scope for this feature
- Existing navigation icons are already available as assets; this feature does not require new icon design
- Badge counts for Alerts use existing alert state data already tracked by the system
- The bottom navigation currently shows icons with labels; this change removes labels while keeping the same icons
