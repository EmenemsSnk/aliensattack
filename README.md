# Aliens Attack

Version 2.0.0: a classic 2D space shooter game built in Java 21 using Swing.

## Description
Take control of a spaceship and defend against waves of invading aliens. Build score combos, collect power-ups, face special aliens and periodic boss waves, keep your best score across sessions, and restart cleanly after Game Over.

## Features
- Player-controlled spaceship
- Start menu and Game Over restart flow
- Moving alien enemies with wave progression and increasing speed
- Special two-hit alien type with shield visual
- Boss wave every 5th wave: a high-HP horizontal-patrol boss with burst firing and a health bar
- Hold-to-fire missile mechanics with rapid-fire power-up
- Score combo multiplier: hit streaks increase the multiplier
- Power-ups: Rapid Fire, Extra Life, Shield, Speed Boost (rare drops from aliens; guaranteed from boss kills)
- Alien projectiles with weighted firing
- Collision detection
- Score, wave, lives, and active-effect HUD
- Wave-start banner
- Alien explosion animations
- Pause and resume (P key)
- Distinct sound on life loss
- Local player profiles with persistent best score and Top 5 leaderboard
- Retro sound effects and generated background music

## How to Play
- **Start screen Left/Right**: Select an existing local profile
- **Start screen N**: Create a new local profile
- **Enter**: Start with the selected profile or restart after Game Over
- **Arrow Keys during play**: Move the spaceship (Up, Down, Left, Right)
- **Spacebar**: Hold to fire missiles
- **P**: Pause or resume gameplay

Local profiles are stored in `profiles.tsv` in the working directory. Each profile keeps its own best score, and Game Over shows a local Top 5 profile ranking.

## Requirements
- Java 21
- Maven wrapper included

## How to Build and Run
```bash
./mvnw clean compile
./mvnw test
./mvnw exec:java -Dexec.mainClass="com.emenems.games.aliens.Main"
```
