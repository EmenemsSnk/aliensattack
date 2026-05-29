---
project: "Aliens Attack"
version: 1
status: draft
created: 2026-05-29
updated: 2026-05-29
prd_version: 1
main_goal: low-complexity
top_blocker: none
---

# Mapa drogowa: Aliens Attack

> Wywiedziono z `context/foundation/prd.md` (v1) + automatycznie zbadana baza kodu.
> Edytuj na miejscu; archiwizuj po zastąpieniu.
> Poniższe wycinki są wymienione w kolejności zależności. Tabela „W skrócie" to indeks.

## Podsumowanie wizji

Aliens Attack to lokalna, jednoosobowa gra arcade 2D (typu Space Invaders) na Java Swing. Obecny prototyp kompiluje się, ale jest niegrywalny: pętla gry chodzi w ~1 FPS, pociski wylatujące poza ekran nie są usuwane (wyciek pamięci), kolizja statku z kosmitą nie wywołuje reakcji, a gra nie ma początku ani końca. Główna hipoteza tej zmiany: **lukę między „kompiluje się" a „daje się grać" można wypełnić bez przepisywania** — naprawiając pętlę i kolizje, a następnie nakładając scoring, fale, życia i Game Over.

## Gwiazda północna

**S-01: Płynna grywalna pętla** — gracz może płynnie grać w ~60 FPS (ruch, strzał, trafienia), co zamienia niegrywalny prototyp w grę i dowodzi, że reszta MVP daje się dołożyć bez przepisywania.

> „Gwiazda północna" oznacza tu najmniejszy kompletny, widoczny dla gracza wycinek, którego pomyślne dostarczenie potwierdza główną hipotezę produktu — umieszczony tak wcześnie, jak pozwalają Wymagania wstępne, bo wszystko inne ma znaczenie dopiero, gdy rdzeń gry działa płynnie. S-01 nie ma żadnych Wymagań wstępnych, więc startuje od razu.

## W skrócie

| ID   | Change ID                  | Wynik (gracz może…)                                                                 | Wymagania wstępne | Odnośniki PRD                       | Status   |
| ---- | -------------------------- | ----------------------------------------------------------------------------------- | ----------------- | ----------------------------------- | -------- |
| F-01 | build-tooling-baseline     | (fundament) build zapięty (release=21), harness JUnit 5, Maven wrapper i CI zielone | —                 | Constraints & Compatibility, Guardrail | done    |
| S-01 | smooth-playable-loop       | grać płynnie w ~60 FPS — ruch, strzał, trafienia raz, brak wycieku obiektów          | —                 | US-01, FR-001, FR-002, FR-004       | done     |
| S-02 | score-and-wave-progression | zdobywać 10×fala pkt, widzieć wynik i falę w HUD, dostać szybszą falę po wyczyszczeniu | S-01              | US-01, FR-007, FR-008, FR-009, FR-010 | done     |
| S-03 | lives-gameover-restart     | tracić życia przy kolizji, dotrzeć do Game Over z wynikiem i zrestartować spacją      | S-02, S-01        | US-01, FR-003, FR-006               | done     |
| S-04 | wave-boundaries-and-hit-feedback | grać z poprawnymi granicami planszy, falami bez nakładania obcych, efektem utraty życia i porażką, gdy obcy nie zostaną wybici | S-03              | US-01, FR-002, FR-003, FR-010       | done     |
| S-05 | post-mvp-arcade-feel       | uruchamiać grę z menu startowego, słyszeć retro feedback i mierzyć się ze strzelającymi obcymi | S-04              | FR-005, Secondary goals, Non-Goals  | planned  |

## Strumienie

Pomoc nawigacyjna — grupuje elementy współdzielące łańcuch Wymagań wstępnych. Kanoniczna kolejność nadal jest w grafie zależności poniżej; ta tabela to proponowana kolejność czytania w równoległych ścieżkach.

| Strumień | Temat              | Łańcuch                      | Uwaga                                                                                  |
| -------- | ------------------ | ---------------------------- | -------------------------------------------------------------------------------------- |
| A        | Grywalna pętla     | `S-01` → `S-02` → `S-03` → `S-04` | Główna ścieżka must-have; kotwiczy gwiazdę północną, domyka pełną sesję gry i utwardza reguły planszy/fali. |
| B        | Utwardzenie buildu | `F-01`                       | Równolegle z całym Strumieniem A; przy celu „niska złożoność" nie blokuje grania, ale daje automatyczną weryfikację guardraila kompilacji. |
| C        | Post-MVP polish    | `S-05`                       | Świadomie odłożone usprawnienia z PRD/shape-notes, sensowne po ukończeniu MVP.            |

## Baza

Co już jest na miejscu w bazie kodu na dzień `2026-05-29` (zbadane z plików).
Poniższe fundamenty zakładają, że jest to obecne i NIE odbudowują tego.

- **Frontend (UI/rendering):** obecny — Swing `GamePanel` / `WindowFrame`; ładowanie obrazów z `src/main/resources/images/`.
- **Backend / API:** nieobecny — aplikacja jednoprocesowa desktop, brak serwera (Non-Goal: brak sieci/kont).
- **Dane:** nieobecny — brak trwałego stanu i zapisów; nic do migracji (PRD: „Migracja danych: brak").
- **Uwierzytelnianie:** nieobecny — pojedynczy gracz, brak kont/auth (PRD Access Control: brak zmian, z założenia).
- **Wdrożenie / infra + build:** nieobecny — `pom.xml` szczątkowy (brak pinu kompilatora, JUnit, pluginów), brak Maven wrappera (`mvnw`), brak CI (`.github/workflows`). Zdecydowane w PRD *Constraints* + `build-tooling-plan.md`, ale **niewdrożone** → adresuje F-01.
- **Obserwowalność:** nieobecny — brak logowania/metryk/śledzenia błędów; niewymagane dla lokalnej gry jednoosobowej.

## Fundamenty

### F-01: Utwardzenie buildu i tooling

- **Wynik:** (fundament) `pom.xml` zapięty na `maven.compiler.release=21` + UTF-8 + jawne pluginy (surefire, exec); JUnit 5 w zakresie `test`; Maven wrapper (`mvnw`) i `.editorconfig` w repo; GitHub Actions zielone na push/PR (`./mvnw clean compile` + `./mvnw test`); kaskada spójności dokumentów wykonana (znika „Java 8 / no deps").
- **Change ID:** build-tooling-baseline
- **Odnośniki PRD:** Constraints & Compatibility (Java 21 LTS; JUnit 5 w `test`; GitHub Actions), Success Criteria → Guardrails (`mvn clean compile` musi przejść na każdym etapie)
- **Odblokowuje:** automatyczną ścieżkę weryfikacji (CI uruchamia guardrail kompilacji + harness JUnit 5 dla czystych metod, które „jadą razem" z S-01/S-02/S-03 — np. formuła scoringu FR-007, progresja fal FR-010, kolizja FR-004); nowoczesną składnię Java 21; kaskadę spójności dokumentów (CLAUDE.md / stack-assessment / health-check)
- **Wymagania wstępne:** —
- **Równolegle z:** S-01, S-02, S-03
- **Blokady:** —
- **Niewiadome:** —
- **Ryzyko:** „jak" jest już rozpisane w `build-tooling-plan.md`, więc niskie ryzyko wykonawcze. Główne ryzyko to dryf dokumentów — „Java 8" / „bez nowych zależności" jest asertowane w wielu plikach; aktualizacja jednego bez pozostałych tworzy sprzeczności (Krok 5 planu). Przy celu „niska złożoność" sekwencjonowane równolegle do rozgrywki, nie jako twarda brama — gra kompiluje się i działa na obecnym `pom.xml`.
- **Status:** done

## Wycinki

### S-01: Płynna grywalna pętla

- **Wynik:** gracz może płynnie grać w ~60 FPS — porusza statkiem strzałkami i strzela spacją bez odczuwalnego lagu, pociski trafiają kosmitów dokładnie raz, a pociski/kosmici opuszczający ekran są usuwani (brak rosnącej listy obiektów).
- **Change ID:** smooth-playable-loop
- **Odnośniki PRD:** US-01, FR-001 (~60 FPS), FR-002 (usuwanie obiektów poza ekranem), FR-004 (kolizja wykrywana raz), NFR (reakcja na klawisz ≤1 klatka ~16ms)
- **Wymagania wstępne:** —
- **Równolegle z:** F-01
- **Blokady:** —
- **Niewiadome:** —
- **Ryzyko:** rdzeń zmiany — zamiana wątku `Thread.sleep(1000)` (off-EDT, ~1 FPS) na `javax.swing.Timer` na EDT. Ryzyko regresji responsywności sterowania (dziś `keyTyped` i `keyPressed` oba wołają `makeAction` → podwójne wywołanie) oraz operacji blokujących na EDT. Sekwencjonowane pierwsze, bo bez grywalnej pętli żaden kolejny wycinek nie ma się gdzie zadziać.
- **Status:** done

### S-02: Wynik i progresja fal

- **Wynik:** gracz zdobywa 10×numer_fali punktów za każdego zniszczonego kosmitę, widzi w HUD bieżący wynik i numer fali w czasie rzeczywistym, a po wyczyszczeniu wszystkich kosmitów pojawia się nowa fala szybsza o 10% (×1.1^(fala−1), z capem ~2× prędkości bazowej).
- **Change ID:** score-and-wave-progression
- **Odnośniki PRD:** US-01, FR-007 (scoring 10×fala), FR-008 (wynik w HUD), FR-009 (numer fali w HUD), FR-010 (nowa, szybsza fala z capem)
- **Wymagania wstępne:** S-01
- **Równolegle z:** F-01
- **Blokady:** —
- **Niewiadome:**
  - Dokładny cap prędkości kosmitów (~2× bazowej) — Właściciel: developer. Blokuje: nie (gra działa z dowolną wartością graniczną; kalibracja gameplay-feel przy implementacji).
  - Wartość bazowej prędkości kosmitów (do odczytania z kodu) — Właściciel: developer. Blokuje: nie.
- **Ryzyko:** dwie niewiadome to strojenie odczuwalności (gameplay feel), nie luki planistyczne — żadna nie blokuje. Ryzyko: formuła scoringu/prędkości wpleciona w `GameController` (centralny węzeł, refaktor poza zakresem) — czyste metody do wyjęcia tylko jeśli nie wymusza separacji View/Controller.
- **Status:** done

### S-03: Życia, Game Over i restart

- **Wynik:** gracz traci 1 z 3 żyć przy każdej kolizji statku z kosmitą (HUD aktualizuje liczbę żyć), po utracie ostatniego życia widzi ekran „GAME OVER" z wynikiem końcowym i „Press SPACE to Restart", a naciśnięcie spacji restartuje grę od fala=1, wynik=0, życia=3, prędkość bazowa=domyślna.
- **Change ID:** lives-gameover-restart
- **Odnośniki PRD:** US-01, FR-003 (3 życia, utrata życia przy kolizji, Game Over po ostatnim, życia w HUD), FR-006 (ekran Game Over: wynik końcowy + restart)
- **Wymagania wstępne:** S-02, S-01
- **Równolegle z:** F-01
- **Blokady:** —
- **Niewiadome:** —
- **Ryzyko:** domyka pętlę sesji (przegrana → restart). Zależy od S-02, bo ekran Game Over wyświetla wynik końcowy, a restart zeruje wynik+falę — sekwencjonowane po S-02, by uniknąć dostarczenia ekranu Game Over bez wyniku i późniejszego przerabiania go. Zależy od S-01 (wykrycie kolizji statek↔kosmita, dziś no-op `return`).
- **Status:** done

### S-04: Granice planszy, fale i feedback trafienia

- **Wynik:** gracz nie może wyprowadzić statku poza planszę; obcy w każdej fali startują w zróżnicowanych odstępach i wysokościach, nie nachodzą na siebie, a ich startowa pozycja Y nie przekracza 1/5 wysokości planszy od góry; po kolizji statku z obcym widać krótką reakcję wizualną utraty życia; jeśli obcy z danej fali nie zostaną zabici i przejdą przez warunek porażki fali, gra kończy się komunikatem, że obcy wygrali.
- **Change ID:** wave-boundaries-and-hit-feedback
- **Odnośniki PRD:** US-01, FR-002 (kosmici opuszczający ekran), FR-003 (utrata życia przy kolizji), FR-010 (nowe fale)
- **Wymagania wstępne:** S-03
- **Równolegle z:** —
- **Blokady:** —
- **Niewiadome:**
  - Dokładny warunek „obcy wygrali" — Właściciel: developer. Propozycja domyślna: Game Over, gdy którykolwiek obcy dotrze do dolnej granicy planszy albo strefy statku. Blokuje: nie (do doprecyzowania w planie/implementacji).
  - Minimalny odstęp między obcymi przy generowaniu fali — Właściciel: developer. Propozycja domyślna: co najmniej rozmiar sprite'a + margines bezpieczeństwa, walidowany prostokątem kolizji. Blokuje: nie.
- **Ryzyko:** dotyka reguł ruchu, generowania fal, kolizji i stanu Game Over, więc ryzykiem jest niejawne sprzężenie w `GameController`. Warto utrzymać logikę granic i generowania jako małe, testowalne metody bez zmiany architektury View/Controller. Efekt wizualny utraty życia powinien być krótkim stanem renderowania wypychanym do `GamePanel` przez istniejący kanał HUD/state, bez blokowania EDT.
- **Status:** done

### S-05: Post-MVP arcade feel

- **Wynik:** gracz widzi ekran startowy i rozpoczyna rozgrywkę spacją, dostaje retro feedback dźwiękowy dla strzału/eksplozji, a obcy losowo strzelają w stronę statku, dzięki czemu ukończone MVP zyskuje pełniejszy arcade feel.
- **Change ID:** post-mvp-arcade-feel
- **Odnośniki PRD:** FR-005 (Start Menu jako nice-to-have), Success Criteria → Secondary (retro sound effects, alien fire), Non-Goals (elementy odłożone poza MVP)
- **Wymagania wstępne:** S-04
- **Równolegle z:** —
- **Blokady:** —
- **Niewiadome:**
  - Czy dźwięk ma używać wyłącznie Java standard library i lokalnych zasobów WAV — Właściciel: developer. Propozycja domyślna: tak, bez nowych zależności runtime.
  - Parametry ostrzału obcych (częstotliwość, prędkość pocisku, limit pocisków na ekranie) — Właściciel: developer. Blokuje: nie; wymaga kalibracji gameplay-feel.
- **Ryzyko:** to mieszanka trzech świadomie odłożonych usprawnień, więc największe ryzyko to rozlanie zakresu. Jeśli plan okaże się zbyt duży, pierwszym podziałem powinno być: Start Menu osobno, audio osobno, alien fire osobno. Dźwięk nie może dodawać nowych zależności runtime bez jawnej decyzji, a alien fire powinien reuse'ować istniejące reguły pocisków/kolizji zamiast tworzyć drugi silnik pocisków.
- **Status:** planned

## Przekazanie backlogu

| ID mapy drogowej | Change ID                  | Sugerowany tytuł problemu                          | Gotowe do `/10x-plan` | Uwagi                                                  |
| ---------------- | -------------------------- | -------------------------------------------------- | --------------------- | ------------------------------------------------------ |
| F-01             | build-tooling-baseline     | Zapięcie buildu: release=21, JUnit 5, mvnw, CI     | done                  | Zaimplementowane: `context/changes/build-tooling-baseline/plan.md` |
| S-01             | smooth-playable-loop       | Płynna grywalna pętla 60 FPS (Timer na EDT)        | yes                   | Gwiazda północna — `/10x-plan smooth-playable-loop`    |
| S-02             | score-and-wave-progression | Scoring, HUD wyniku/fali i progresja fal           | no                    | Po S-01                                                |
| S-03             | lives-gameover-restart     | Życia, ekran Game Over i restart spacją            | no                    | Po S-02                                                |
| S-04             | wave-boundaries-and-hit-feedback | Granice planszy, poprawny spawn fal, feedback utraty życia i porażka po niepowstrzymanej fali | yes | Po S-03; nowy slice dla zauważonych poprawek gameplayu |
| S-05             | post-mvp-arcade-feel       | Start Menu, retro dźwięki i strzelający obcy       | yes                   | Po S-04; świadomie odłożone z PRD/shape-notes          |

## Otwarte pytania dotyczące mapy drogowej

1. **Dokładny cap prędkości kosmitów (~2× bazowej)** — Właściciel: developer. Blokuje: tylko `S-02` (kalibracja przy implementacji; nie blokuje planowania — Blok: nie).
2. **Wartość bazowej prędkości kosmitów** — Właściciel: developer. Blokuje: tylko `S-02` (do odczytania z kodu przy implementacji FR-010; Blok: nie).
3. **Warunek porażki fali / „obcy wygrali"** — Właściciel: developer. Blokuje: tylko `S-04` (domyślnie: obcy dociera do dolnej granicy planszy albo strefy statku; do doprecyzowania w planie).
4. **Minimalny odstęp i strategia losowania pozycji obcych** — Właściciel: developer. Blokuje: tylko `S-04` (domyślnie: brak nakładania prostokątów + margines bezpieczeństwa; start w górnej 1/5 planszy).
5. **Parametry post-MVP audio i alien fire** — Właściciel: developer. Blokuje: tylko `S-05` (kalibracja; bez nowych zależności runtime jako domyślne założenie).

(Pytania 1–2 pochodzą z PRD `## Open Questions`; pytania 3–5 dodano po ukończeniu MVP jako niewiadome dla nowych wycinków. Żadne nie podnosi statusu wycinka do `blocked`.)

## Zaparkowane

- **Start Menu (Space = start), dźwięki i strzelający kosmici** — Przeniesione z zaparkowanych do `S-05: post-mvp-arcade-feel`, bo MVP jest domknięte i można wrócić do świadomie odłożonych usprawnień.
- **Refaktoryzacja View/Controller** — Dlaczego zaparkowane: PRD §Non-Goals + Constraints — `GameController` pozostaje centralnym węzłem; separacja prezentacji poza zakresem.
- **Konta / multiplayer / sieć / zapisy w chmurze / dystrybucja** — Dlaczego zaparkowane: PRD §Non-Goals — gra pozostaje lokalną aplikacją jednoosobową.
- **Twarde gwarancje wydajności poza ~60 FPS subiektywnie** — Dlaczego zaparkowane: PRD §Non-Goals — brak budżetu klatkowego, profilowania ani benchmarków.

## Zrobione

- **S-04: gracz nie może wyprowadzić statku poza planszę; obcy w każdej fali startują w zróżnicowanych odstępach i wysokościach, nie nachodzą na siebie, a ich startowa pozycja Y nie przekracza 1/5 wysokości planszy od góry; po kolizji statku z obcym widać krótką reakcję wizualną utraty życia; jeśli obcy z danej fali nie zostaną zabici i przejdą przez warunek porażki fali, gra kończy się komunikatem, że obcy wygrali.** — Zarchiwizowano 2026-05-29 → `context/archive/2026-05-29-wave-boundaries-and-hit-feedback/`. Lekcja: —.
- **S-03: tracić życia przy kolizji, dotrzeć do Game Over z wynikiem i zrestartować spacją** — Zarchiwizowano 2026-05-29 → `context/archive/2026-05-29-lives-gameover-restart/`. Lekcja: —.
- **F-01: (fundament) build zapięty (release=21), harness JUnit 5, Maven wrapper i CI zielone** — Zarchiwizowano 2026-05-29 → `context/archive/2026-05-29-build-tooling-baseline/`. Lekcja: —.
- **S-01: gracz może płynnie grać w ~60 FPS — porusza statkiem strzałkami i strzela spacją bez odczuwalnego lagu, pociski trafiają kosmitów dokładnie raz, a pociski/kosmici opuszczający ekran są usuwani (brak rosnącej listy obiektów).** — Zarchiwizowano 2026-05-29 → `context/archive/2026-05-29-smooth-playable-loop/`. Lekcja: —.
- **S-02: gracz zdobywa 10×numer_fali punktów za każdego zniszczonego kosmitę, widzi w HUD bieżący wynik i numer fali w czasie rzeczywistym, a po wyczyszczeniu wszystkich kosmitów pojawia się nowa fala szybsza o 10% (×1.1^(fala−1), z capem ~2× prędkości bazowej).** — Zarchiwizowano 2026-05-29 → `context/archive/2026-05-29-score-and-wave-progression/`. Lekcja: —.
