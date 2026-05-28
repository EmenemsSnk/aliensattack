# Product Requirements Document (PRD) / Implementation Plan: Aliens Attack MVP

This document outlines the implementation plan for stabilizing the "Aliens Attack" game and adding core MVP features.

## Goal Description
Transform the current buggy, 1-FPS prototype into a fully playable, endless 2D space shooter. This phase focuses on stabilizing the core mechanics (60 FPS loop, proper collision, cleanup) and introducing an end-to-end game flow (Start Menu -> Playing -> Game Over) with scoring and endless progressive waves.

## User Review Required
> [!IMPORTANT]  
> Please review the proposed Game States flow and the Endless Wave mechanics below. Let me know if you want the difficulty progression (speed vs. spawn rate) to follow a specific mathematical curve, or if a simple linear progression per wave is acceptable for this MVP.

## Proposed Changes

---

### Core Mechanics & Engine Refactor
#### [MODIFY] [GameController.java](file:///Users/michaldziedzic/Priv/aliensattack/src/main/java/com/emenems/games/aliens/controller/GameController.java)
- **Game Loop:** Replace the inefficient background thread (`Thread.sleep(1000)`) with a `javax.swing.Timer` running at ~16ms (approx. 60 FPS).
- **Collision & Memory Management:** 
  - Fix `checkCollisionsWithSpaceShip` to trigger the "Game Over" state when hit.
  - Optimize the double loop in `checkCollisionsWithMissile`.
  - Add logic to remove missiles and aliens that fly out of the screen bounds to prevent memory leaks.

---

### MVP Features (Game Flow, Waves, Scoring)
#### [NEW] [GameState.java](file:///Users/michaldziedzic/Priv/aliensattack/src/main/java/com/emenems/games/aliens/GameState.java)
- Already exists but needs expansion or usage. Ensure it properly tracks `START_MENU`, `PLAYING`, and `GAME_OVER`.

#### [MODIFY] [GameController.java](file:///Users/michaldziedzic/Priv/aliensattack/src/main/java/com/emenems/games/aliens/controller/GameController.java)
- **State Management:** Intercept key inputs based on the current state (e.g., Spacebar starts the game from the menu, fires missiles while playing, and restarts the game from the Game Over screen).
- **Scoring System:** Implement a score counter that increments by 10 points for every alien destroyed.
- **Endless Waves:** When the `aliens` list is empty, immediately spawn a new wave. Increase the base speed of the aliens by a small factor (e.g., 10%) each wave to create an endless progression of difficulty.

---

### UI & Rendering
#### [MODIFY] [GamePanel.java](file:///Users/michaldziedzic/Priv/aliensattack/src/main/java/com/emenems/games/aliens/gui/GamePanel.java)
- **Start Menu Rendering:** Draw the game title and "Press SPACE to Start" on a black background when in `START_MENU` state.
- **HUD Rendering:** Draw the current score in the top-left corner during the `PLAYING` state.
- **Game Over Rendering:** Draw "GAME OVER", the final score, and "Press SPACE to Restart" centered on the screen when in the `GAME_OVER` state.
- **Decoupling:** Ensure `GamePanel` only reads state from `GameController` or domain objects, rather than dictating bounds sizes via `DEFAULT_COMPONENT_SIZE` to the models.

## Verification Plan

### Automated / Manual Tests
- **Build Verification:** Run `mvn clean compile` to ensure no syntax errors.
- **Performance:** Ensure the game window runs smoothly without stuttering or memory inflation over 5 minutes of rapid firing.
- **Mechanics Testing:** 
  - Verify missing an alien doesn't crash the game.
  - Verify colliding with an alien triggers Game Over.
  - Verify killing all aliens triggers the next, faster wave.
  - Verify pressing Spacebar on Game Over correctly resets the score and restarts the wave.
