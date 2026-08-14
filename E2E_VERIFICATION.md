# E2E Verification Guide — Phase 1

## Prerequisites

- Android emulator running (API level 33+)
- Firebase project configured with Firestore
- `google-services.json` in place at `app/`

## Running E2E Tests

### 1. Build and Install APK

```bash
./gradlew :app:assembleDebug
adb install app/build/outputs/apk/debug/app-debug.apk
```

APK location: `app/build/outputs/apk/debug/app-debug.apk`

### 2. Launch App

```bash
adb shell am start -n com.walcker.match/.MainActivity
```

### 3. Log In (if needed)

The app shows an auth gate. Use a test account to proceed to the authenticated shell.

### 4. Verify GameListStep (Home Tab)

**Expected behavior:**
- Home tab (🏠) loads list of open matches from Firestore
- Each match displays: sport, venue name, neighborhood, time, players/slots, price
- Matches load asynchronously (shows loading indicator briefly)
- Empty state displays if no matches in range

**Test actions:**
1. Verify list appears after brief load
2. Pull-to-refresh (swipe down) to reload from Firestore
3. Select a sport filter from chips (e.g., "Futebol")
4. Adjust radius slider (5–50 km)
5. Verify list filters correctly

### 5. Verify SearchStep (Search Tab)

**Expected behavior:**
- Search tab (🔍) shows input field with placeholder "Buscar por quadra, bairro ou esporte"
- Type in the search field to filter cached matches
- Empty state shows "Nenhuma partida encontrada para..." when no results

**Test actions:**
1. Navigate to Search tab
2. Type "Futebol" — results filter client-side
3. Type "Downtown" — filters by neighborhood
4. Clear the field — shows all cached matches
5. Try nonsense query — verify empty state message

### 6. Verify Join Match

**Expected behavior:**
- Tap "ENTRAR NO JOGO" button on any match card
- Success: Snackbar shows "Você entrou no jogo."
- Failure: Snackbar shows "Não foi possível entrar no jogo."
- GameList refreshes after successful join (shows updated player count)

**Test actions:**
1. From Home tab, tap join on any match
2. Verify snackbar and list update
3. From Search tab, tap join — verify snackbar

### 7. Verify Strings Localization

All UI should display Portuguese (pt-BR):
- Titles: "Vagas abertas" (Home), "Buscar partidas" (Search)
- Buttons: "ENTRAR NO JOGO"
- Labels: "Raio: X km", "Todos", "Nenhuma vaga aberta..."
- Snackbars: "Você entrou no jogo." (success), "Não foi possível..." (failure)

If English appears, verify `MatchDefaultLanguageTag` is set to `pt-BR` in `core/strings`.

### 8. Verify Navigation

5-tab bottom bar should switch between:
1. 🏠 Home — GameListStep (working)
2. 🔍 Search — SearchStep (working)
3. ➕ Create — Placeholder (coming in Phase 1)
4. ⚽ Matches — Placeholder (coming in Phase 1)
5. 👤 Profile — Placeholder (coming in Phase 1)

## Checklist

- [ ] App installs without errors
- [ ] Home tab loads matches from Firestore
- [ ] Search tab filters matches client-side
- [ ] Sport filter chips work
- [ ] Radius slider filters results
- [ ] Join match button works + snackbar shows
- [ ] All strings are in Portuguese
- [ ] Navigation bar switches tabs
- [ ] No crashes or ANRs

## Troubleshooting

**App won't load matches:**
- Verify Firebase project credentials in `google-services.json`
- Verify Firestore has data in `matches/{matchId}` collection
- Check logcat: `adb logcat | grep walcker`

**Search doesn't filter:**
- Verify app is authenticated (check SessionHolder state)
- Check that cache has data from initial load

**Strings show in English:**
- Verify `LocalMatchLanguageTag.current` resolves to `pt-BR`
- Check `rememberGamesStrings()` is called in composables

## Next Steps

After E2E verification passes, Phase 1 is complete. Phase 2 can begin with:
- Create Match flow
- MyMatches screen
- User Profile screen
