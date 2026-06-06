# Aliens Attack

Version 1.0.0 MVP: a classic 2D space shooter game built in Java 21 using Swing.

## Description
Take control of a spaceship and defend against waves of invading aliens. Move, hold fire, dodge alien projectiles, clear faster waves, and restart cleanly after Game Over.

## Features
- Player-controlled spaceship
- Start menu and Game Over restart flow
- Moving alien enemies and wave progression
- Hold-to-fire missile mechanics
- Alien projectiles
- Collision detection
- Score, wave, and lives HUD
- Retro sound effects and generated background music

## How to Play
- **Start screen Left/Right**: Select an existing local profile
- **Start screen N**: Create a new local profile
- **Enter**: Start with the selected profile or restart after Game Over
- **Arrow Keys during play**: Move the spaceship (Up, Down, Left, Right)
- **Spacebar**: Hold to fire missiles
- **P**: Pause or resume gameplay

Local profiles are stored in `profiles.tsv` in the working directory. Each profile keeps its own best score.

## Requirements
- Java 21
- Maven wrapper included

## How to Build and Run
```bash
./mvnw clean compile
./mvnw test
./mvnw exec:java -Dexec.mainClass="com.emenems.games.aliens.Main"
```
