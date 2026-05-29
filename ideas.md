# Aliens Attack - Project Ideas & Short Plan

## 1. Project Description
"Aliens Attack" is a simple 2D Java Swing-based arcade game similar to "Space Invaders". The player controls a spaceship and shoots at a fleet of advancing aliens. The current codebase has a basic game loop, simple graphics, and partial collision detection, but needs significant improvements for performance and playability.

## 2. Issues to Fix (Bugs)
- **Game Loop Frequency**: The current game loop runs at 1 frame per second (`Thread.sleep(1000)` in `GameController`), making the game extremely slow and unresponsive. It should be refactored to use a proper timer running at ~60 FPS (e.g., `Thread.sleep(16)`).
- **Out of Bounds Management**: Unimplemented `checkOutOfBoarder` function. Missiles fly off-screen and are never removed from memory, causing memory leaks over time.
- **Spaceship Collision Handling**: `checkCollisionsWithSpaceShip` currently has no logic for when the player gets hit (`if (optionalAlien.isPresent()) return;`). The game should trigger a "Game Over" state.
- **Double loop in collision logic**: `checkCollisionsWithMissile` has duplicated and inefficient collision checks for the same entities.

## 3. Code Refactoring Improvements
- **Decouple View and Controller**: Move rendering logic completely out of `GameController` and keep domain models clean of GUI-specific sizing logic (e.g., `GamePanel.DEFAULT_COMPONENT_SIZE`).
- **Use `javax.swing.Timer` vs `Thread.sleep`**: Replace the custom background thread with `javax.swing.Timer` for safer UI updating in Swing, or correctly synchronize a game loop thread (e.g., separating `update()` and `render()`).
- **Object Pooling**: Implement an object pool for `Missile` to avoid generating too many objects during rapid fire.
- **Magic Numbers**: Extract hardcoded positions, sizes, and speeds into static constants or a configuration file.

## 4. Possible New Features
- **Scoring System**: Add points for every destroyed alien and display the score on the UI.
- **Game States**: Introduce states like `MAIN_MENU`, `PLAYING`, `PAUSED`, and `GAME_OVER`.
- **Waves/Levels**: Instead of one static row of aliens, introduce progressing waves with increasing speed and difficulty.
- **Alien Firing**: Allow aliens to shoot back at the player.
- **Sound Effects**: Add retro sound effects for shooting, explosions, and background music.
- **Health System**: Give the spaceship 3 lives instead of 1-hit death.

## 5. Short Plan Towards PRD
1. **Stabilize Core Mechanics**: Fix the game loop, fix bounding boxes, and handle basic win/loss states.
2. **Refactor Architecture**: Clean up controller logic and component sizing.
3. **Draft Product Requirements Document (PRD)**: 
   - Define exact win/loss mechanics.
   - Design level progressions.
   - Specify visual and audio assets needed.
4. **Implement MVP Features**: Add scoring, game over screen, and endless alien waves based on the PRD.
