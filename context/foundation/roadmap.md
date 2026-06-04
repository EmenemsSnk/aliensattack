---
project: "Aliens Attack"
version: 1
status: draft
created: 2026-06-04
updated: 2026-06-04
prd_version: 1
main_goal: quality
top_blocker: decisions
---

# Mapa drogowa: Aliens Attack

> Wywiedziono z `context/foundation/prd.md` (v1) oraz automatycznie zbadanej bazy kodu.
> Edytuj na miejscu; archiwizuj po zastąpieniu.
> Poniższe wycinki są wymienione w kolejności zależności. Tabela „W skrócie” to indeks.

## Podsumowanie wizji

Aliens Attack ma już kompletną lokalną pętlę arcade. Kolejne trzy niezależnie grywalne wydania mają równorzędnie zwiększyć regrywalność, poprawić odczucie jakości oraz dać graczowi trwały powód do poprawiania wyniku.

Kolejność jest podporządkowana jakości: każdy wycinek musi zachować sterowanie i działającą pętlę gry, a jednocześnie dostarczyć widoczny rezultat.

## Gwiazda przewodnia

**S-01: Gracz może zebrać rapid-fire i odczuć jego pełny cykl działania** — to najmniejsza kompletna nowa mechanika z ustaloną regułą, która sprawdza kierunek Replayability bez czekania na otwarte decyzje.

> „Gwiazda przewodnia” oznacza tutaj najmniejszy kompleksowy wycinek, którego udane dostarczenie potwierdza podstawowy kierunek produktu; jest umieszczony tak wcześnie, jak pozwalają zależności.

## W skrócie

| ID | Change ID | Wynik (użytkownik może…) | Wymagania wstępne | Odnośniki PRD | Status |
| --- | --- | --- | --- | --- | --- |
| S-01 | rapid-fire-power-up | zebrać rapid-fire i odczuć jego pełny cykl działania | — | US-01, FR-003, FR-010 | done |
| S-02 | skill-based-score-combo | budować combo punktowe zachowujące znaczenie umiejętności | S-01 | US-01, FR-004, FR-010 | done |
| S-03 | distinct-alien-type | spotkać wyraźnie odmienny nowy typ obcego | S-02 | US-01, FR-005, FR-010 | blocked |
| S-04 | visible-alien-explosion | zobaczyć eksplozję po zniszczeniu obcego | — | US-01, FR-006, FR-010 | ready |
| S-05 | clear-hud-and-wave-message | odczytać czytelniejszy HUD i komunikat rozpoczęcia fali | S-04 | US-01, FR-007, FR-010 | proposed |
| S-06 | pause-and-resume | wstrzymać i wznowić rozgrywkę | S-05 | FR-008, FR-010 | proposed |
| S-07 | life-loss-sound | usłyszeć osobny dźwięk utraty życia | S-05 | FR-009, FR-010 | proposed |
| S-08 | local-player-profiles | utworzyć lub wybrać lokalny profil bez blokowania startu gry | — | US-01, FR-001, FR-010 | ready |
| S-09 | persistent-profile-best-score | zobaczyć zapisany najlepszy wynik wybranego profilu po Game Over | S-08 | US-01, FR-002, FR-010 | blocked |

## Strumienie

Pomoc nawigacyjna — grupuje elementy, które współdzielą łańcuch wymagań wstępnych. Kanoniczna kolejność nadal znajduje się w grafie zależności poniżej.

| Strumień | Temat | Łańcuch | Uwaga |
| --- | --- | --- | --- |
| A | Regrywalność | `S-01` → `S-02` → `S-03` | Zaczyna od gwiazdy przewodniej i odkłada nowego obcego do rozstrzygnięcia jego reguły. |
| B | Odczucie jakości | `S-04` → `S-05` → `S-06` / `S-07` | Najpierw dostarcza konieczne elementy Polish, potem niezależne dodatki drugorzędne. |
| C | Profile i wyniki | `S-08` → `S-09` | Wprowadza trwałość pionowo z profilem, a rekord czeka na decyzję o regule aktualizacji. |

## Baza

Co już jest na miejscu w bazie kodu na dzień `2026-06-04` (automatycznie zbadane i potwierdzone przez użytkownika). Mapa zakłada, że obecne warstwy nie będą odbudowywane.

- **Interfejs:** obecny — Swing UI i renderowanie są skupione w `GamePanel`, a okno w `WindowFrame`.
- **Logika gry:** obecna — pętla, wejście, kolizje i orkiestracja są w `GameController`; reguły i stan sesji są wydzielone.
- **Dane:** częściowe — istnieje stan bieżącej sesji, ale brak trwałego zapisu profili i wyników.
- **Tożsamość:** nieobecna — brak lokalnych profili, logowania i ról.
- **Wdrożenie / infrastruktura:** obecne — Maven Wrapper, testy JUnit i CI kompilujące oraz testujące projekt.
- **Obserwowalność:** nieobecna — brak logowania, metryk i raportowania błędów; PRD nie wymaga osobnego wycinka w tym obszarze.

## Fundamenty

Brak osobnych fundamentów. Istniejąca baza zapewnia pętlę gry, UI, testy i CI, a brakujące elementy danych i profili mogą zostać bezpiecznie wprowadzone pionowo w pierwszym wycinku, który ich używa.

## Wycinki

### S-01: Pełny cykl rapid-fire

- **Wynik:** Gracz może otrzymać drop rapid-fire po zniszczeniu obcego, zebrać go statkiem, odczuć szybszy ogień i zobaczyć zakończenie efektu.
- **Change ID:** rapid-fire-power-up
- **Odnośniki PRD:** US-01, FR-003, FR-010
- **Wymagania wstępne:** —
- **Równolegle z:** S-04, S-08
- **Blokady:** —
- **Niewiadome:** —
- **Ryzyko:** Mechanika dotyka kolizji, czasowego stanu i tempa strzelania; dostarczenie jej pierwszej ujawnia ryzyko regresji i balansu przy zamkniętej regule.
- **Status:** done

### S-02: Combo oparte na umiejętności

- **Wynik:** Gracz może budować mnożnik przez szybkie trafienia, a przerwa lub utrata życia resetuje combo.
- **Change ID:** skill-based-score-combo
- **Odnośniki PRD:** US-01, FR-004, FR-010
- **Wymagania wstępne:** S-01
- **Równolegle z:** S-05, S-08
- **Blokady:** —
- **Niewiadome:** —
- **Ryzyko:** Combo musi zostać ocenione razem z rapid-fire, aby power-up nie zastąpił umiejętności gracza.
- **Status:** done

## Done

- **S-02: Combo oparte na umiejętności** — Zarchiwizowano 2026-06-04 → `context/archive/2026-06-04-skill-based-score-combo/`. Lekcja: —.

### S-03: Wyraźnie odmienny nowy obcy

- **Wynik:** Gracz może spotkać nowy typ obcego, którego zachowanie jest czytelnie inne od zwykłego przeciwnika.
- **Change ID:** distinct-alien-type
- **Odnośniki PRD:** US-01, FR-005, FR-010
- **Wymagania wstępne:** S-02
- **Równolegle z:** S-05, S-08
- **Blokady:** —
- **Niewiadome:**
  - Jakie zachowanie odróżnia nowego obcego od zwykłego? — Właściciel: użytkownik. Blokuje: tak.
- **Ryzyko:** Planowanie bez reguły różnicującej wymusiłoby arbitralną decyzję o mechanice i balansie.
- **Status:** blocked

### S-04: Widoczna eksplozja obcego

- **Wynik:** Gracz może zobaczyć krótką eksplozję po zniszczeniu obcego bez utraty czytelności rozgrywki.
- **Change ID:** visible-alien-explosion
- **Odnośniki PRD:** US-01, FR-006, FR-010
- **Wymagania wstępne:** —
- **Równolegle z:** S-01, S-08
- **Blokady:** —
- **Niewiadome:** —
- **Ryzyko:** Efekt czasowy nie może zasłaniać pocisków ani naruszać działającego renderowania.
- **Status:** ready

### S-05: Czytelniejszy HUD i komunikat fali

- **Wynik:** Gracz może łatwo odczytać stan sesji i zobaczyć komunikat rozpoczęcia nowej fali.
- **Change ID:** clear-hud-and-wave-message
- **Odnośniki PRD:** US-01, FR-007, FR-010
- **Wymagania wstępne:** S-04
- **Równolegle z:** S-02, S-08
- **Blokady:** —
- **Niewiadome:** —
- **Ryzyko:** Zmiany prezentacji muszą zachować czytelność na obecnym rozmiarze okna i nie zasłaniać akcji.
- **Status:** proposed

### S-06: Pauza i wznowienie

- **Wynik:** Gracz może wstrzymać rozgrywkę i wznowić ją bez zmiany stanu obiektów podczas pauzy.
- **Change ID:** pause-and-resume
- **Odnośniki PRD:** FR-008, FR-010
- **Wymagania wstępne:** S-05
- **Równolegle z:** S-07, S-09
- **Blokady:** —
- **Niewiadome:** —
- **Ryzyko:** Nowy stan gry może przypadkowo pozwolić na ruch, strzelanie lub zmianę timerów podczas pauzy.
- **Status:** proposed

### S-07: Dźwięk utraty życia

- **Wynik:** Gracz może usłyszeć osobny dźwięk utraty życia, a brak urządzenia audio nadal nie przerywa gry.
- **Change ID:** life-loss-sound
- **Odnośniki PRD:** FR-009, FR-010
- **Wymagania wstępne:** S-05
- **Równolegle z:** S-06, S-09
- **Blokady:** —
- **Niewiadome:** —
- **Ryzyko:** Dodatkowy feedback nie może naruszyć kontraktu bezpiecznej, cichej pracy bez audio.
- **Status:** proposed

### S-08: Lokalne profile graczy

- **Wynik:** Gracz może utworzyć lub wybrać niezabezpieczony lokalny profil na ekranie startowym, a błędne dane nie blokują uruchomienia gry.
- **Change ID:** local-player-profiles
- **Odnośniki PRD:** US-01, FR-001, FR-010
- **Wymagania wstępne:** —
- **Równolegle z:** S-01, S-04
- **Blokady:** —
- **Niewiadome:**
  - Czy istniejące dane wymagają migracji lub zachowania ścieżki wycofania? — Właściciel: użytkownik. Blokuje: nie.
- **Ryzyko:** To pierwszy wycinek z trwałymi lokalnymi danymi; błędny lub uszkodzony zapis nie może zablokować gry.
- **Status:** ready

### S-09: Trwały najlepszy wynik profilu

- **Wynik:** Gracz może zobaczyć najlepszy wynik wybranego profilu po Game Over i po ponownym uruchomieniu gry.
- **Change ID:** persistent-profile-best-score
- **Odnośniki PRD:** US-01, FR-002, FR-010
- **Wymagania wstępne:** S-08
- **Równolegle z:** S-06, S-07
- **Blokady:** —
- **Niewiadome:**
  - Kiedy dokładnie wynik zakończonej sesji zastępuje zapisany najlepszy wynik profilu? — Właściciel: użytkownik. Blokuje: tak.
- **Ryzyko:** Bez jawnej reguły aktualizacji rekord może zachowywać się nieprzewidywalnie lub nadpisywać poprawne dane.
- **Status:** blocked

## Przekazanie backlogu

| ID mapy drogowej | Change ID | Sugerowany tytuł problemu | Gotowe do `/10x-plan` | Uwagi |
| --- | --- | --- | --- | --- |
| S-01 | rapid-fire-power-up | Dostarcz pełny cykl power-upu rapid-fire | yes | Gwiazda przewodnia; uruchom `/10x-plan rapid-fire-power-up`. |
| S-02 | skill-based-score-combo | Dodaj combo punktowe oparte na umiejętności | no | Wymaga S-01. |
| S-03 | distinct-alien-type | Dodaj wyraźnie odmienny typ obcego | no | Blokuje brak decyzji o zachowaniu obcego. |
| S-04 | visible-alien-explosion | Dodaj widoczną eksplozję po trafieniu | yes | Niezależny wycinek Polish. |
| S-05 | clear-hud-and-wave-message | Uporządkuj HUD i pokaż komunikat fali | no | Wymaga S-04. |
| S-06 | pause-and-resume | Dodaj pauzę i bezpieczne wznowienie | no | Wymaga S-05; nice-to-have. |
| S-07 | life-loss-sound | Dodaj osobny dźwięk utraty życia | no | Wymaga S-05; nice-to-have. |
| S-08 | local-player-profiles | Dodaj odporne lokalne profile graczy | yes | Pierwszy pionowy wycinek trwałych danych. |
| S-09 | persistent-profile-best-score | Zapisuj najlepszy wynik profilu | no | Blokuje brak reguły aktualizacji wyniku. |

## Otwarte pytania dotyczące mapy drogowej

1. **Jakie zachowanie odróżnia nowego obcego od zwykłego?** — Właściciel: użytkownik. Blokuje: S-03.
2. **Pod jakim dokładnie warunkiem wynik zakończonej sesji zastępuje zapisany najlepszy wynik profilu?** — Właściciel: użytkownik. Blokuje: S-09.
3. **Jakie jest obecne obejście ograniczonej regrywalności, jakości i trwałości wyników oraz jaki jest jego koszt dla gracza?** — Właściciel: użytkownik. Blokuje: roadmap-wide, nie.
4. **Czy wprowadzenie trwałych profili lokalnych wymaga migracji lub zachowania ścieżki wycofania dla istniejących danych?** — Właściciel: użytkownik. Blokuje: roadmap-wide, nie.
5. **Czy poza lokalnym audio istnieją integracje, które muszą pozostać kompatybilne?** — Właściciel: użytkownik. Blokuje: roadmap-wide, nie.

## Zaparkowane

- **Tryb wieloosobowy i funkcje sieciowe** — Zaparkowane zgodnie z PRD §Non-Goals; wydania pozostają lokalną grą jednoosobową.
- **Konta online, synchronizacja chmurowa i globalna tabela wyników** — Zaparkowane zgodnie z PRD §Non-Goals; profile i rekordy pozostają lokalne.
- **Dodatkowe rodziny power-upów, typy obcych i bossowie** — Zaparkowane, aby nie rozszerzać mechanik poza jawnie wybrany zakres.
- **Przepisanie architektury lub zmiana platformy desktopowej** — Zaparkowane; mapa rozszerza działającą grę bez przebudowy produktu.

## Zrobione

- **S-01: Gracz może otrzymać drop rapid-fire po zniszczeniu obcego, zebrać go statkiem, odczuć szybszy ogień i zobaczyć zakończenie efektu.** — Zarchiwizowano 2026-06-04 → `context/archive/2026-06-04-rapid-fire-power-up/`. Lekcja: —.
