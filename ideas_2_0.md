# Aliens Attack - Ideas 2.0

## Cel dokumentu

Ten plik zbiera sensowne kierunki rozwoju po obecnym MVP. Gra ma juz kompletna petle arcade: start, rozgrywke, fale, wynik, zycia, Game Over, restart, podstawowy audio feedback i strzelajacych obcych. Kolejny PRD powinien wiec odpowiedziec nie na pytanie "jak domknac MVP?", tylko "jaki typ wartosci dodajemy teraz?".

Najbardziej naturalne kierunki:

1. Replayability Pack - wiecej powodow, zeby zagrac kolejny raz.
2. Polish Pack - gra ma sprawiac wrazenie bardziej skonczonej.
3. High Scores Pack - lokalne wyniki i prosta trwalosc danych.
4. Refactor Pack - przygotowanie architektury pod dalszy rozwoj.
5. Distribution Pack - gra gotowa do pokazania innym bez IDE.

## 1. Replayability Pack

### Intencja

Dodac glebie rozgrywki bez zmiany gatunku i bez przebudowy calej gry. Obecnie gracz ma jedna podstawowa petle: ruszaj sie, strzelaj, unikaj pociskow, przechodz fale. Replayability Pack powinien sprawic, ze kolejne sesje beda mniej przewidywalne i bardziej satysfakcjonujace.

### Dlaczego to ma sens

To jest najbardziej produktowy kierunek po MVP. Nie wymaga kont, sieci, nowego UI ani dystrybucji. Rozwija to, co juz dziala: fale, przeciwnikow, pociski, wynik i ryzyko utraty zyc.

### Mozliwe funkcje

- Power-upy spadajace po zniszczeniu obcego:
  - szybszy strzal przez kilka sekund,
  - dodatkowe zycie,
  - podwojny strzal,
  - tarcza chroniaca przed jednym trafieniem.
- Rozne typy obcych:
  - zwykly obcy,
  - szybki obcy z mniejsza liczba punktow,
  - ciezki obcy wymagajacy dwoch trafien,
  - obcy strzelajacy czesciej.
- Combo / mnoznik punktow:
  - szybkie trafienia podbijaja mnoznik,
  - przerwa lub otrzymanie obrazen resetuje combo.
- Mini-boss co kilka fal:
  - wiekszy przeciwnik,
  - wiecej punktow,
  - prosty wzorzec ruchu lub ostrzalu.
- Lepsze skalowanie trudnosci:
  - nie tylko predkosc obcych, ale tez czestotliwosc strzalow, liczba obcych lub typy obcych.

### Minimalny zakres do PRD

Najmniejsza sensowna wersja:

- jeden typ power-upu,
- jeden nowy typ obcego,
- aktualizacja HUD, zeby pokazac aktywny efekt,
- testy dla reguly spawn power-upu i czasu trwania efektu.

### Ryzyka

- Latwo dodac za duzo mechanik naraz.
- Power-upy moga rozbic balans gry, jesli beda zbyt czeste albo zbyt mocne.
- `GameController` moze zaczac puchnac, jesli kazda mechanika trafi bezposrednio do niego.

### Dobry pierwszy wycinek

`power-up-rapid-fire`: po zniszczeniu obcego z mala szansa wypada power-up; zebranie go skraca cooldown strzalu gracza na okreslona liczbe tickow.

## 2. Polish Pack

### Intencja

Podniesc odczucie jakosci gry bez duzego zmieniania zasad. Ten kierunek dotyczy game feel: czy gra wyglada, brzmi i reaguje jak zamkniety, dopracowany projekt.

### Dlaczego to ma sens

Jesli gra ma byc portfolio albo projektem pokazywanym innym, polish moze dac wiekszy efekt niz kolejna mechanika. Obecny kod renderuje podstawowe elementy, ale mozna dodac wiecej informacji zwrotnej i czytelnosci.

### Mozliwe funkcje

- Ekran pauzy:
  - `P` zatrzymuje gre,
  - ekran pokazuje aktualny wynik i instrukcje powrotu.
- Lepszy start screen:
  - krotka instrukcja,
  - najlepszy wynik, jesli istnieje,
  - bardziej arcade'owy uklad tekstu.
- Efekty wizualne:
  - krotka eksplozja po trafieniu obcego,
  - miganie statku po utracie zycia,
  - drobny shake ekranu przy trafieniu,
  - animacja Game Over zamiast statycznego tekstu.
- Uporzadkowany HUD:
  - czytelniejszy kontrast,
  - ikony zyc zamiast samego tekstu,
  - aktywne komunikaty typu "Wave 3".
- Lepsze audio:
  - osobny dzwiek utraty zycia,
  - osobny dzwiek Game Over,
  - cichsza lub bardziej rytmiczna muzyka tla.

### Minimalny zakres do PRD

Najmniejsza sensowna wersja:

- pauza,
- efekt eksplozji po trafieniu,
- uporzadkowany HUD,
- dodatkowy dzwiek utraty zycia.

### Ryzyka

- Trudniej testowac automatycznie niz czysta logike.
- Efekty wizualne moga wprowadzic stan czasowy, ktory skomplikuje renderowanie.
- Latwo poswiecic duzo czasu na detale, ktore nie zmieniaja grywalnosci.

### Dobry pierwszy wycinek

`pause-and-visual-feedback`: dodac stan `PAUSED`, obsluge klawisza `P`, overlay pauzy i prosty tick-based efekt eksplozji dla zniszczonego obcego.

## 3. High Scores Pack

### Intencja

Dodac lokalna pamiec gry: gracz widzi, czy pobija swoje poprzednie wyniki. To daje prosty powod do powrotu i wprowadza pierwszy, ograniczony model danych.

### Dlaczego to ma sens

To technicznie czysty krok: lokalny zapis, odczyt, proste UI i latwe do testowania reguly. Nie wymaga backendu ani kont. Dobrze pasuje do desktopowej gry jednoosobowej.

### Mozliwe funkcje

- Najlepszy wynik lokalny:
  - zapis najwyzszego wyniku po Game Over,
  - wyswietlanie najlepszego wyniku w menu i na Game Over.
- Tabela top 5 / top 10:
  - wynik,
  - fala,
  - data sesji,
  - opcjonalnie inicjaly gracza.
- Prosty ekran high scores:
  - wejscie z menu startowego,
  - powrot do menu.
- Reset wynikow:
  - opcja wyczyszczenia lokalnych wynikow.
- Format pliku:
  - prosty plik tekstowy, CSV albo properties,
  - bez zewnetrznych zaleznosci runtime.

### Minimalny zakres do PRD

Najmniejsza sensowna wersja:

- jeden najlepszy wynik zapisany lokalnie,
- odczyt przy starcie gry,
- zapis po Game Over tylko jesli wynik jest lepszy,
- wyswietlenie najlepszego wyniku na Start Menu i Game Over.

### Ryzyka

- Trzeba zdecydowac, gdzie zapisywac plik lokalny.
- Trzeba obsluzyc uszkodzony lub brakujacy plik bez crasha.
- Dodanie wpisywania inicjalow zwieksza zakres UI i obslugi klawiatury.

### Dobry pierwszy wycinek

`local-best-score`: zapis i odczyt pojedynczego najlepszego wyniku w lokalnym pliku, z defensywna obsluga braku pliku lub blednych danych.

## 4. Refactor Pack

### Intencja

Zmniejszyc koszt dalszego rozwoju. Obecnie `GameController` jest centralnym miejscem dla inputu, tickow, kolizji, fal, scoringu, zyc, pociskow, dzwieku i stanu gry. To bylo pragmatyczne dla MVP, ale kolejne mechaniki beda coraz bardziej ryzykowne, jesli wszystko dalej bedzie dokladane w jednym miejscu.

### Dlaczego to ma sens

Ten kierunek nie dodaje graczowi natychmiastowej funkcji, ale moze byc najlepszy, jesli planujesz kilka kolejnych iteracji. Szczegolnie Replayability Pack bedzie latwiejszy, jesli najpierw wydzielisz czysta logike gry.

### Mozliwe obszary refaktoryzacji

- Wydzielenie `GameSession`:
  - wynik,
  - fala,
  - zycia,
  - stan gry,
  - reset sesji.
- Wydzielenie `WaveSpawner`:
  - generowanie pozycji obcych,
  - typy obcych w przyszlosci,
  - skalowanie trudnosci.
- Wydzielenie `CollisionSystem`:
  - pocisk gracza vs obcy,
  - pocisk obcego vs gracz,
  - obcy vs gracz,
  - obcy dociera do dolu planszy.
- Wydzielenie `GameRules`:
  - scoring,
  - predkosc fali,
  - limity pociskow,
  - cooldown strzalu.
- Ograniczenie roli `GameController`:
  - input,
  - timer,
  - koordynacja aktualizacji,
  - przekazanie stanu do panelu.

### Minimalny zakres do PRD

Najmniejsza sensowna wersja:

- wyciagnac reguly scoringu, predkosci fali i sesji do testowalnych klas,
- nie zmieniac zachowania gry,
- zachowac Swing i obecny sposob uruchamiania,
- przejsc wszystkie istniejace testy.

### Ryzyka

- Refaktor bez widocznej funkcji moze rozrosnac sie bez jasnego konca.
- Latwo przypadkowo zmienic gameplay podczas przenoszenia logiki.
- Za wczesne projektowanie abstrakcji moze utrudnic, a nie ulatwic dodawanie funkcji.

### Dobry pierwszy wycinek

`extract-game-session-and-rules`: wydzielic wynik, fale, zycia, reset oraz funkcje scoringu/predkosci do malych klas bez zmiany zachowania widocznego dla gracza.

## 5. Distribution Pack

### Intencja

Sprawic, aby gre mozna bylo latwo uruchomic poza IDE i poza komenda Maven. Ten kierunek zamienia projekt programistyczny w cos, co mozna pokazac innej osobie jako gotowa aplikacje.

### Dlaczego to ma sens

Jesli celem jest portfolio, sama gra w repozytorium to za malo. Osoba ogladajaca projekt powinna miec prosty sposob: pobieram release, uruchamiam, gram. To takze porzadkuje README, wersjonowanie i release process.

### Mozliwe funkcje

- Uruchamialny JAR:
  - `java -jar aliens-attack.jar`,
  - zasoby graficzne i audio dzialaja z artefaktu.
- Maven release build:
  - jedna komenda budujaca artefakt,
  - jasna nazwa pliku wynikowego.
- GitHub Release:
  - artefakt do pobrania,
  - krotki changelog,
  - instrukcja uruchomienia.
- Ikona aplikacji:
  - okno gry z nazwa i ikona,
  - opcjonalnie ikona dla release.
- Smoke test uruchomienia:
  - minimum: build artefaktu przechodzi w CI,
  - opcjonalnie: prosty test sprawdzajacy, ze zasoby sa dostepne z classpath.
- Dokumentacja dla gracza:
  - sterowanie,
  - cel gry,
  - wymagania Java,
  - troubleshooting.

### Minimalny zakres do PRD

Najmniejsza sensowna wersja:

- zbudowanie uruchamialnego JAR-a z zasobami,
- aktualizacja README o sposob uruchomienia artefaktu,
- workflow CI publikujacy artefakt builda albo instrukcja lokalnego buildu release.

### Ryzyka

- Pakowanie zasobow Swing/classpath moze ujawnic bledy, ktorych nie widac przy `mvn exec:java`.
- Pelny instalator dla macOS/Windows/Linux moze byc za duzym skokiem.
- Trzeba zdecydowac, czy wymagamy lokalnej Javy 21, czy probujemy dostarczac runtime razem z aplikacja.

### Dobry pierwszy wycinek

`playable-jar-release`: skonfigurowac build uruchamialnego JAR-a, upewnic sie, ze obrazy laduja sie z classpath, i opisac proces release w README.

## Porownanie kierunkow

| Kierunek | Wartosc dla gracza | Wartosc techniczna | Ryzyko zakresu | Dobry nastepny PRD, gdy... |
| --- | --- | --- | --- | --- |
| Replayability Pack | Wysoka | Srednia | Srednie | chcesz rozwijac gre jako gre |
| Polish Pack | Srednia/wysoka | Niska/srednia | Srednie | chcesz, zeby projekt wygladal na skonczony |
| High Scores Pack | Srednia | Srednia/wysoka | Niskie | chcesz pierwszy zapis danych i powod do powrotu |
| Refactor Pack | Niska bezposrednio | Wysoka | Srednie/wysokie | planujesz kilka kolejnych mechanik |
| Distribution Pack | Srednia | Srednia | Niskie/srednie | chcesz pokazac gre innym osobom |

## Rekomendowane opcje na kolejny PRD

### Opcja A: Replayability Pack jako glowny kierunek

Najlepszy wybor, jesli chcesz rozwijac Aliens Attack jako gre. PRD powinien byc mocno ograniczony: jeden power-up, jeden nowy typ obcego, jeden widoczny efekt w HUD. Nie dokladac od razu bossow, sklepu ulepszen i kampanii.

Proponowany tytul PRD:

> Aliens Attack 2.0 - Replayability Pack

Proponowana hipoteza:

> Jesli gra doda mala liczbe losowych, czytelnych zmiennych w trakcie sesji, to kolejne przejscia beda mniej przewidywalne i bardziej satysfakcjonujace bez przebudowy rdzenia gry.

### Opcja B: High Scores + Distribution jako maly pakiet portfolio

Najlepszy wybor, jesli celem jest pokazanie projektu komus innemu. Lokalny high score daje powod do ponownej gry, a uruchamialny JAR sprawia, ze projekt jest latwiejszy do oceny.

Proponowany tytul PRD:

> Aliens Attack 1.5 - Local Scores and Playable Release

Proponowana hipoteza:

> Jesli gra zapisuje najlepszy wynik i da sie uruchomic jako gotowy artefakt, to przechodzi z projektu deweloperskiego do malej gry, ktora mozna realnie pokazac innym.

### Opcja C: Refactor przed dalszymi mechanikami

Najlepszy wybor, jesli czujesz, ze dodawanie kolejnych funkcji do `GameController` zaczyna byc niewygodne. PRD powinien wtedy byc traktowany jako techniczny PRD brownfield z glownym guardrailem: zero zmian widocznego zachowania.

Proponowany tytul PRD:

> Aliens Attack - Gameplay Core Refactor

Proponowana hipoteza:

> Jesli oddzielimy czysta logike sesji, fal i kolizji od Swingowego kontrolera, to kolejne mechaniki bedzie mozna dodawac szybciej i z mniejszym ryzykiem regresji.

## Moja rekomendacja

Najbardziej sensowna kolejnosc:

1. High Scores Pack albo Distribution Pack, jesli chcesz szybko miec projekt bardziej pokazowy.
2. Replayability Pack, jesli chcesz od razu rozwijac gameplay.
3. Refactor Pack przed Replayability, jesli przy pierwszym planowaniu power-upow lub nowych obcych wyjdzie, ze `GameController` robi sie za ciasny.

Najbardziej zbalansowany nastepny PRD:

> Aliens Attack 1.5 - Local High Score and Playable Release

Dlaczego: to maly, konkretny krok, ktory daje wartosc graczowi i wartosc portfolio, a nie otwiera jeszcze duzego worka mechanik. Po nim Replayability Pack bedzie mial lepszy kontekst: gracz ma po co wracac, bo moze bic najlepszy wynik.
