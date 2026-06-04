# Aliens Attack - Ideas 2.0

## Cel dokumentu

Ten plik zbiera sensowne kierunki rozwoju po obecnym MVP. Gra ma juz kompletna petle arcade: start, rozgrywke, fale, wynik, zycia, Game Over, restart, podstawowy audio feedback i strzelajacych obcych. Kolejny PRD powinien wiec odpowiedziec nie na pytanie "jak domknac MVP?", tylko "jaki typ wartosci dodajemy teraz?".

Najbardziej naturalne kierunki:

1. Replayability Pack - wiecej powodow, zeby zagrac kolejny raz.
2. Polish Pack - gra ma sprawiac wrazenie bardziej skonczonej.
3. High Scores Pack - lokalne wyniki i prosta trwalosc danych.

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
