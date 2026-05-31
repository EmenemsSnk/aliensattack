---
project: "Aliens Attack"
version: 1
status: draft
created: 2026-05-31
updated: 2026-05-31
prd_version: 1
main_goal: quality
top_blocker: none
---

# Mapa drogowa: Aliens Attack

> Wywiedziono z `context/foundation/prd.md` (v1) + automatycznie zbadana baza kodu.
> Edytuj na miejscu; archiwizuj po zastąpieniu.
> Poniższe wycinki są wymienione w kolejności zależności. Tabela "W skrócie" to indeks.

## Podsumowanie wizji

Aliens Attack ma już kompletne MVP gry desktopowej, ale `GameController` skupia zbyt wiele odpowiedzialności: stan sesji, reguły scoringu i fali, wejście, pętlę gry oraz koordynację widoku. Ta mapa drogowa sekwencjonuje refaktor tak, aby przygotować kod pod późniejszy Replayability Pack bez zmiany widocznego zachowania gry.

Główny cel sekwencjonowania to jakość: najpierw utrzymujemy bezpieczeństwo regresji, potem wydzielamy najmniejszą regułę, a dopiero później ruszamy granicę sesji, która ma większe ryzyko rozproszenia stanu.

## Gwiazda północna

**S-03: Developer może pracować z wydzieloną sesją gry bez zmiany zachowania gracza** — to najmniejszy pełny wycinek, który udowadnia główną hipotezę PRD: gameplay foundation jest czystszy, a start, ruch, strzały, fale, score, lives, Game Over i restart pozostają bez regresji.

> Gwiazda północna oznacza tutaj najmniejszy kompleksowy wycinek, którego pomyślne dostarczenie udowadnia główną hipotezę PRD; jest umieszczony tak wcześnie, jak pozwalają na to wymagania wstępne.

## W skrócie

| ID   | Change ID                    | Wynik (użytkownik może...)                                         | Wymagania wstępne | Odnośniki PRD       | Status   |
| ---- | ---------------------------- | ------------------------------------------------------------------ | ----------------- | ------------------- | -------- |
| S-01 | lock-refactor-safety-baseline | Developer może potwierdzić bezpieczny baseline refaktoru           | —                 | FR-004              | done     |
| S-02 | extract-game-rules            | Developer może pracować z wydzielonymi regułami scoringu i fali    | S-01              | FR-002              | proposed |
| S-03 | extract-game-session          | Developer może pracować z wydzieloną sesją gry bez zmiany gameplayu | S-02              | US-01, FR-001, FR-003 | proposed |

## Baza

Co już jest na miejscu w bazie kodu na dzień `2026-05-31` (automatycznie zbadane + potwierdzone przez użytkownika). Poniższe wycinki zakładają, że są one obecne i nie odbudowują ich.

- **Frontend / UI:** obecny — desktopowy UI w Swing (`GamePanel`, `WindowFrame`, `Main`).
- **Backend / API:** nieobecny — lokalna gra desktopowa, brak serwera i tras API.
- **Dane:** nieobecne — brak bazy danych, ORM, migracji i seedów; PRD nie wymaga warstwy danych.
- **Uwierzytelnianie:** nieobecne — zgodne z PRD: lokalna gra bez kont, logowania i ról.
- **Wdrożenie / infrastruktura:** obecne — GitHub Actions uruchamia compile i testy.
- **Obserwowalność:** nieobecna — brak logowania i metryk; PRD nie wymaga tej warstwy.

## Fundamenty

Brak osobnych fundamentów. Build, testy i CI są już obecne, a PRD nie wymaga nowych warstw przekrojowych przed rozpoczęciem pierwszego wycinka.

## Wycinki

### S-01: Zablokuj baseline bezpieczeństwa refaktoru

- **Wynik:** Developer może potwierdzić bezpieczny baseline refaktoru przed przenoszeniem logiki.
- **Change ID:** lock-refactor-safety-baseline
- **Odnośniki PRD:** FR-004
- **Wymagania wstępne:** —
- **Równolegle z:** —
- **Blokady:** —
- **Niewiadome:** —
- **Ryzyko:** PRD wprost ostrzega, że istniejące testy mogą nie pokrywać każdego widocznego zachowania, więc pierwszy wycinek powinien ustalić krótki, powtarzalny sygnał braku regresji przed refaktorem.
- **Status:** done

### S-02: Wydziel reguły scoringu i skalowania fali

- **Wynik:** Developer może pracować z wydzielonymi regułami scoringu i skalowania fali.
- **Change ID:** extract-game-rules
- **Odnośniki PRD:** FR-002
- **Wymagania wstępne:** S-01
- **Równolegle z:** —
- **Blokady:** —
- **Niewiadome:** —
- **Ryzyko:** To najmniejszy refaktor domenowy; ogranicza ryzyko zbyt sztywnej abstrakcji, bo dotyczy obecnych reguł i nie projektuje przyszłych mechanik.
- **Status:** proposed

### S-03: Wydziel sesję gry bez zmiany gameplayu

- **Wynik:** Developer może pracować z wydzieloną sesją gry obejmującą score, wave, lives, game state i reset, a gracz widzi ten sam gameplay co przed refaktorem.
- **Change ID:** extract-game-session
- **Odnośniki PRD:** US-01, FR-001, FR-003
- **Wymagania wstępne:** S-02
- **Równolegle z:** —
- **Blokady:** —
- **Niewiadome:** —
- **Ryzyko:** To właściwa walidacja PRD, ale ryzykuje rozproszenie resetu i stanu gry, więc powinna nastąpić po małym wycinku reguł i po zablokowaniu baseline'u.
- **Status:** proposed

## Przekazanie backlogu

| ID mapy drogowej | Change ID                    | Sugerowany tytuł problemu                         | Gotowe do `/10x-plan` | Uwagi |
| ---------------- | ---------------------------- | ------------------------------------------------- | --------------------- | ----- |
| S-01             | lock-refactor-safety-baseline | Zablokuj baseline bezpieczeństwa refaktoru        | yes                   | Pierwszy ruch; zabezpiecza FR-004 przed refaktorem. |
| S-02             | extract-game-rules            | Wydziel reguły scoringu i skalowania fali         | no                    | Zależy od S-01. |
| S-03             | extract-game-session          | Wydziel sesję gry bez zmiany gameplayu            | no                    | Gwiazda północna; zależy od S-02. |

Ta tabela to czyste przekazanie do backlogu. Zawiera jeden wiersz dla każdego elementu mapy drogowej i wskazuje, który change id można przekazać do `/10x-plan`.

## Otwarte pytania dotyczące mapy drogowej

- None.

## Zaparkowane

- **Power-upy i nowe typy obcych** — Dlaczego zaparkowane: należą do późniejszego Replayability Pack, nie do tego PRD.
- **Rewrite renderowania / prezentacji** — Dlaczego zaparkowane: PRD skupia się na gameplay foundation, nie na architekturze prezentacji.
- **High scores, zapis danych, dystrybucja artefaktu i release packaging** — Dlaczego zaparkowane: pozostają odłożone do późniejszej wersji.
- **Zmiany balansu gameplayu** — Dlaczego zaparkowane: PRD wymaga, aby scoring, wave progression, lives, movement, shooting i Game Over pozostały widocznie bez zmian.

## Zrobione

- **S-01: Developer może potwierdzić bezpieczny baseline refaktoru przed przenoszeniem logiki** — Zarchiwizowano 2026-05-31 → `context/archive/2026-05-31-lock-refactor-safety-baseline/`. Lekcja: —.
