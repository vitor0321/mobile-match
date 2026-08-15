# Phase 2 Plan — Create Match, MyMatches, Profile (20-25 hours)

## Overview

Phase 2 expands the games product with 3 new screens completing the bottom navigation:
1. **➕ Create Match** — Form to create new match
2. **⚽ MyMatches** — User's active & past matches  
3. **👤 Profile** — User profile + preferences

## Architecture

### Create Match Flow (5-7h)

**Domain:**
- `CreateMatchUseCase(venue, sport, datetime, players, price, ...): Result<String>`

**UI Components:**
- `CreateMatchStep` — Main form screen
- `VenueSearch` — ComposableComposable to find venue (or enter free-form)
- `SportSelector` — Sport picker (radio group or chips)
- `DateTimePicker` — Start time + duration
- `PlayersAndPriceForm` — Players count, price per player

**Data:**
- `CreateMatchRequest` data class
- API call to Firestore: `matches/{matchId}`
- Update user's `users/{userId}/createdMatches` subcollection

**Screens in tab order:**
```
CreateMatchStep:
  - Header: "Criar Partida"
  - Form sections:
    * Venue (text + search)
    * Sport (chips)
    * Date (date picker)
    * Time (time picker)
    * Duration (dropdown: 60/90/120 min)
    * Total Players (slider: 2-20)
    * Price (text input, nullable)
  - Submit button: "Criar e Publicar"
  - Success: Navigate to MyMatches tab + snackbar
  - Error: Snackbar with message
```

### MyMatches Screen (6-8h)

**Tab segments:**
- "Ativas" (Active) — Ongoing/upcoming matches
- "Passadas" (Past) — Completed matches

**Per match card:**
- Status badge (OPEN, FULL, FINISHED, CANCELLED)
- Match details (sport, venue, datetime)
- Your role: Organizer or Joined Player
- Action buttons:
  - If organizer: Cancel | Edit | View Players
  - If joined: Leave Match | Report Issue

**Data flow:**
- Query `matches/` where `organizerId == currentUserId`
- Query `users/{userId}/joinedMatches` 
- Combine into single "MyMatches" view with tabs for active/past
- Sort by datetime (desc for active, desc for past)

**Screens:**
```
MyMatchesStep:
  - Tabs: [Ativas] [Passadas]
  - Empty state per tab: "Nenhuma partida..."
  - List of match cards with role-specific actions
```

### Profile Screen (4-5h)

**Sections:**
1. **User Header**
   - Avatar (placeholder or from identity product)
   - Name + email
   - Rating stars + review count

2. **Stats**
   - Matches played
   - Matches organized
   - Join rate (%)
   - Cancellation rate (%)

3. **Preferences**
   - Favorite sports (checkboxes)
   - Preferred neighborhood/area
   - Availability (time slots?)
   - Notification settings

4. **Actions**
   - Edit Profile (name, bio)
   - Change Sports Preferences
   - View Reviews
   - Support / Help
   - Logout

**Data:**
- `UserProfile` model in Firestore: `users/{userId}`
- `UserStats` computed from `matches/` queries
- Preferences persisted in GamesPreferences (reuse from Phase 1)

**Screens:**
```
ProfileStep:
  - Header: User info + avatar
  - Section: Stats (4 cards)
  - Section: Preferences (toggles)
  - Section: Actions (links)
```

## Implementation Order

### Week 1: Create Match
1. Domain: `CreateMatchUseCase` + `CreateMatchRequest`
2. UI: Form components (venue, sport, datetime, players, price)
3. ScreenModel: `CreateMatchStepModel` with validation
4. Integration: Wire into tab 2 (➕ Create)
5. Strings: Create match labels (pt/en)

### Week 2: MyMatches
1. Domain: `GetMyMatchesUseCase`, `GetJoinedMatchesUseCase`
2. UI: Tab segments, match card with role-specific actions
3. ScreenModel: `MyMatchesStepModel` with combined flows
4. Actions: Leave, Cancel, Edit (basic stubs for now)
5. Strings: MyMatches labels (pt/en)

### Week 3: Profile
1. Domain: `GetUserProfileUseCase`, `GetUserStatsUseCase`
2. UI: Header, stats cards, preferences section
3. ScreenModel: `ProfileStepModel` with user data flow
4. Edit profile (placeholder for identity integration)
5. Strings: Profile labels (pt/en)

## Technical Details

### Validation

**CreateMatchStep:**
- Venue: not empty
- Sport: required
- DateTime: must be in future
- Players: 2-20
- Price: optional, but if set must be > 0

### Error Handling

All use cases return `Result<T>`:
- Network errors → show snackbar with message
- Validation errors → show inline error
- Firebase permission denied → redirect to login (SessionHolder)

### Firestore Schema Extensions

Existing:
```
matches/{matchId} {
  id, sport, venue, datetime, players, price, organizer*
}
```

New collections:
```
users/{userId}/createdMatches/{matchId} (doc ref only)
users/{userId}/joinedMatches/{matchId} (doc ref only)
users/{userId}/profile { name, avatar, bio, rating, ... }
users/{userId}/stats { matchesPlayed, matchesOrganized, ... } (computed)
```

### State Management Pattern (Unchanged from Phase 1)

Each screen follows:
- `ScreenModel` (Voyager)
- `State` data class
- `Event` sealed class
- `Effect` sealed class for side-effects (snackbars, navigation)
- Repository/UseCase injection via Koin

### Strings (Lyricist)

Add to `CreateMatchStrings`, `MyMatchesStrings`, `ProfileStrings`:
- Screen titles
- Form labels
- Button labels
- Empty states
- Error messages
- Tab names
- Status badges

## Out of Scope (Phase 3+)

- Real push notifications
- Advanced search/filtering (recommendations)
- Match chat/messaging
- Detailed review system
- Payment integration
- Calendar view
- Real-time player list updates

## Success Criteria

- [ ] Create Match: Form validates, saves to Firestore, navigates to MyMatches
- [ ] MyMatches: Shows active + past matches, can leave/cancel
- [ ] Profile: Shows user stats + preferences, can edit
- [ ] All strings localized (pt/en)
- [ ] Android + iOS compiling
- [ ] No crashes
- [ ] Tab navigation works for all 5 tabs

## Estimated Timeline

**20-25 hours total:**
- Create Match: 6h
- MyMatches: 7h
- Profile: 5h
- Integration + Testing: 2-4h
- Strings + Cleanup: 2-3h

## Branch Strategy

```bash
# Phase 2 on developer branch
git checkout developer
git pull origin main

# Create features as needed
git checkout -b feature/create-match
git checkout -b feature/my-matches
git checkout -b feature/profile

# Merge to developer, then final PR to main
```
