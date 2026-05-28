---
project: "Aliens Attack"
version: 1
status: draft
created: 2026-05-28
context_type: brownfield
product_type: desktop
target_scale:
  users: small
  qps: negligible
  data_volume: none
timeline_budget:
  delivery_weeks: 2
  hard_deadline: null
  after_hours_only: true
---

## Current System Overview

- **System purpose**: A 2D Java Swing arcade game (Space Invaders-like) — player controls a spaceship and shoots at a fleet of advancing aliens.
- **Key architecture**: Single-process desktop app; central `GameController` holds the game loop and input handling; domain objects live in the `gamemachines` package (Spaceship, Alien, Missile); rendering via `GamePanel`.
- **Tech stack**: Java + Maven + `javax.swing` (Swing UI); desktop application; no external game/audio libraries.
- **Current user base**: Single developer/player; local machine; no networking or accounts.
- **Core functionality**: Spaceship movement (keyboard input), missile firing, alien grid movement, basic collision detection, partial rendering via `GamePanel`; game loop in `GameController`; domain objects in `gamemachines` package (Spaceship, Alien, Missile).

## Problem Statement & Motivation

**What's wrong now**: The game is unplayable in its current state:
- Game loop runs at 1 FPS (`Thread.sleep(1000)` in `GameController`) — movement is too slow to be a game.
- Missiles fly off-screen and are never removed — memory leak over time.
- Spaceship collision with an alien triggers no response — no Game Over state.
- No game flow exists — no start menu, no end screen, no way to win or lose; the game just starts and runs until the window is closed.

**Why now**: The prototype skeleton is in place (spaceship, aliens, missiles, basic rendering). The gap between "compiles" and "actually playable" is fillable without a rewrite — fixing the loop and collision handling unlocks the ability to layer meaningful features (scoring, waves) on top.

**Current workaround**: None — the game is simply not playable as a game.

## User & Persona

**Primary persona**: The developer himself — a Java programmer building and playing this game as a portfolio/learning project. Runs on their own machine via `mvn exec:java`; expects full keyboard control (arrow keys + space) and smooth ~60 FPS gameplay; no need for accounts, multiplayer, cloud saves, or any form of distribution.

This is the existing (and only) user. The change improves the experience of this same user — from an unplayable prototype to a complete play-loop session.

## Success Criteria

### Primary
- Player can: launch the game → play immediately at ~60 FPS with responsive controls → earn (10 × wave) points per alien destroyed → see the next faster wave spawn when all aliens are cleared → lose one of 3 lives when hit → reach Game Over after 3 hits → see final score on Game Over screen → press Space to restart. All of this completes in a single session without crashing or visibly growing memory usage.

### Secondary
- Health system: spaceship has 3 lives displayed in HUD; Game Over triggers only after all 3 are lost (nice-to-have, not MVP blocker).
- Retro sound effects: audio feedback for shooting and alien explosion (nice-to-have).
- Alien fire: aliens randomly shoot back at the player (nice-to-have).

### Guardrails
- Keyboard controls (arrow keys + space bar) must respond correctly in all game states — control responsiveness must not degrade from the current state.
- `mvn clean compile` must pass without errors at every stage of the change.

## User Stories

### US-01: Gracz rozgrywa kompletną sesję gry

- **Given** gracz uruchamia aplikację przez `mvn exec:java`
- **When** gra startuje (bezpośrednio w stanie PLAYING)
- **Then** gra działa w ~60 FPS; HUD wyświetla wynik (0), numer fali (1) i 3 życia; gracz strzela Space i porusza się strzałkami; wynik rośnie o 10 × numer_fali za każdego trafionego kosmitę; trafienie przez kosmitę odejmuje 1 życie (HUD aktualizuje się); po 3 trafieniach pojawia się ekran Game Over z wynikiem końcowym i "Press SPACE to Restart"; po zniszczeniu wszystkich kosmitów pojawia się fala 2 (szybsza o 10%, do limitu 2× prędkości bazowej); naciśnięcie Space na Game Over restartuje od fali=1, wynik=0, życia=3

*Delta note*: Wcześniej gra startowała w ~1 FPS bez HUD, bez scoringu, bez żyć, bez stanu Game Over i bez kolejnych fal — sesja nie miała początku ani końca.

#### Acceptance Criteria
- Gra osiąga ~60 FPS subiektywnie (płynny ruch bez zauważalnych zacinań)
- Wynik, numer fali i liczba żyć w HUD aktualizują się w czasie rzeczywistym
- Każde trafienie przez kosmitę odejmuje dokładnie 1 życie; po 3 trafieniach — Game Over
- Scoring: fala 1 = 10 pkt/kosmita, fala 2 = 20 pkt/kosmita, fala N = N×10 pkt/kosmita
- Prędkość fali nie przekracza ~2× prędkości bazowej
- Pociski opuszczające granice ekranu są usuwane z listy obiektów
- Restart przywraca: fala=1, wynik=0, życia=3, prędkość bazowa=domyślna

## Scope of Change

### Silnik gry
- [modified] FR-001: Gra działa w ~60 FPS (było ~1 FPS). Priorytet: must-have.
  > Sokrates: Kontrargument: "30 FPS wystarczy — 60 FPS to nadmiar złożoności." Zachowane: 60 FPS jest standardem dla płynnej gry arcade; różnica jest wyraźnie odczuwalna.
- [new] FR-002: Pociski i kosmici opuszczający ekran są usuwani z pamięci. Priorytet: must-have.
  > Sokrates: Kontrargument: "Prostsze czyścić wszystkie pociski przy zmianie stanu." Zachowane: per-frame cleanup zapobiega rosnącej liście obiektów przy szybkim strzelaniu między zdarzeniami stanu.
- [new] FR-003: Gracz ma 3 życia; kolizja statku z kosmitą odbiera jedno życie; Game Over po utracie ostatniego; HUD wyświetla liczbę pozostałych żyć. Priorytet: must-have.
  > Sokrates: Kontrargument: "Lepiej od razu implementować 3 życia niż iterować dwa razy." Zaakceptowano — FR zaktualizowany; system 3 żyć przeniesiony z secondary goals na must-have.
- [modified] FR-004: Kolizja pocisku z kosmitą jest wykrywana raz (eliminacja podwójnej pętli). Priorytet: must-have.
  > Sokrates: Kontrargument: "Przedwczesna optymalizacja przy małej skali." Zachowane jako must-have: to błąd logiczny (może liczyć trafienia dwukrotnie), nie tylko wydajnościowy.

### Stany gry
- [new] FR-005: Gracz może uruchomić grę z ekranu Start Menu (Space = start). Priorytet: nice-to-have.
  > Sokrates: Kontrargument: "Menu dodaje złożoność zanim gra działa — lepiej startować bezpośrednio w PLAYING." Obniżono do nice-to-have: MVP startuje od razu w stanie PLAYING.
- [new] FR-006: Ekran Game Over wyświetla "GAME OVER", wynik końcowy i "Press SPACE to Restart". Priorytet: must-have.
  > Sokrates: Kontrargument: "Game Over ma wyższy priorytet niż Start Menu." Zachowane i potwierdzono jako must-have.

### Scoring i postęp
- [new] FR-007: Gracz zdobywa (10 × numer_fali) punktów za każdego zniszczonego kosmitę. Priorytet: must-have.
  > Sokrates: Kontrargument: "Płaskie 10 pkt traci sens przy wyższych falach." Zaktualizowano formułę na 10 × numer_fali — scoring rośnie proporcjonalnie do trudności.
- [new] FR-008: Aktualny wynik jest wyświetlany w HUD podczas gry. Priorytet: must-have.
  > Sokrates: Kontrargument: "Wynik tylko na ekranie Game Over wystarczy." Zachowane jako must-have: wynik na żywo w HUD jest standardem gatunku.
- [new] FR-009: Aktualny numer fali jest wyświetlany w HUD podczas gry. Priorytet: must-have.
  > Sokrates: Brak kontrargumentu — numer fali jest konieczny skoro scoring = 10 × fala; gracz musi wiedzieć na której fali jest.
- [new] FR-010: Po zniszczeniu wszystkich kosmitów pojawia się nowa fala z prędkością bazową ×1.1^(fala-1), z limitem ~2× prędkości bazowej. Priorytet: must-have.
  > Sokrates: Kontrargument: "+10% liniowo bez limitu sprawi, że gra stanie się niezagrą po ~15 falach." Zaktualizowano: dodano cap przy ~2× prędkości bazowej; dokładna wartość do kalibracji przy implementacji.

### Zachowane (preserved)
- [preserved] Sterowanie klawiaturą (strzałki + Space) dla ruchu statku i strzelania — musi przetrwać zmianę game loop bez regresji responsywności.

## Constraints & Compatibility

- **Brak nowych zależności zewnętrznych**: Tylko Java standard library + `javax.swing`. Żadne nowe biblioteki (game engine, audio library, JSON parser) nie są dodawane bez wyraźnej decyzji.
- **Swing EDT**: Cały rendering i logika gry wywoływana przez timer musi działać na Event Dispatch Thread (EDT Swing). Zmiana game loop nie może wprowadzać operacji blokujących EDT.
- **GameController jako centralny węzeł**: Logika gry i input handling pozostają w `GameController` — architektoniczna separacja View/Controller nie jest celem tego MVP; refaktoryzacja poza zakresem.
- **Istniejące zachowanie sterowania**: Arrow keys i Space obsługują ruch statku i strzelanie — to zachowanie musi przetrwać zmianę game loop bez regresji responsywności.
- **Migracja danych**: Brak — gra nie posiada trwałego stanu ani zapisów; nic do migracji.
- **Kompatybilność wsteczna**: Brak zewnętrznych kontraktów (API, formaty plików, integracje) — zmiana dotyczy wyłącznie lokalnej rozgrywki jednego użytkownika.

## Business Logic Changes

**Istniejąca reguła domenowa**: Brak — obecny system nie posiada żadnej logiki domenowej. Wszystkie zdarzenia kolizji są no-op, nie ma scoringu, zarządzania życiami ani progresji trudności.

**Nowe reguły dodawane przez tę zmianę**:

1. *Reguła postępu*: Gra ocenia postęp gracza przez fale — wynik rośnie proporcjonalnie do trudności (10 × numer_fali punktów za każdego zniszczonego kosmitę), a prędkość ruchu kosmitów rośnie o 10% z każdą ukończoną falą do limitu ~2× prędkości bazowej. Gracz na wejściu: strzelanie; na wyjściu: wynik i nowa fala.

2. *Reguła żyć*: Gra zarządza 3 życiami gracza — każda kolizja statku z kosmitą odbiera 1 życie; po utracie wszystkich 3 żyć gra wchodzi w stan Game Over, prezentując wynik końcowy i możliwość restartu.

Zmiany są addytywne: żadna istniejąca reguła domenowa nie jest modyfikowana (żadna nie istnieje).

## Access Control Changes

No access control changes — current model preserved. Single player; no authentication; no accounts; game state lives on-device only.

## Non-Goals

- **Brak dźwięków w tym MVP** — audio (strzał, eksplozja, muzyka tła) to oddzielna iteracja; brak dźwięku nie blokuje grywalności ani celów portfolio.
- **Brak strzelających kosmitów w tym MVP** — alien fire pozostaje nice-to-have; nie jest częścią pierwszej grywalnej wersji.
- **Brak Start Menu jako must-have** — MVP startuje bezpośrednio w stanie PLAYING; Start Menu (FR-005) jest nice-to-have.
- **Brak refaktoryzacji architektury View/Controller** — separacja prezentacji od logiki gry nie jest celem tej zmiany; `GameController` pozostaje centralnym węzłem.
- **Brak kont, multiplayera, sieci, zapisów w chmurze ani dystrybucji** — gra pozostaje lokalną, jednoosobową aplikacją desktopową.
- **Brak gwarancji wydajności poza ~60 FPS subiektywnie** (non-functional) — brak twardego budżetu klatkowego, profilowania ani benchmarków poza odczuwalną płynnością.

## Open Questions

1. **Dokładny cap prędkości kosmitów** — ustalono ~2× prędkości bazowej; dokładna wartość do kalibracji przy implementacji (gameplay feel). Właściciel: developer. Blok: nie — gra działa z dowolną wartością graniczną.
2. **Wartość bazowej prędkości kosmitów** — obecna wartość w kodzie jest nieznana bez inspekcji; do weryfikacji przy implementacji FR-010. Właściciel: developer. Blok: nie.
</content>
</invoke>


The file has been written successfully to /Users/michaldziedzic/Priv/aliensattack/context/foundation/prd.md

Result of calling the Write tool.

Done — written to disk.

