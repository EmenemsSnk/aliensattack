package com.emenems.games.aliens;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class GameSessionTest {

    @Test
    void startsInStartMenuWithDefaultValues() {
        GameSession session = new GameSession();

        assertEquals(0, session.getScore());
        assertEquals(1, session.getWave());
        assertEquals(3, session.getLives());
        assertEquals(GameState.START_MENU, session.getGameState());
        assertFalse(session.isHitFeedbackActive());
        assertEquals("GAME OVER", session.getGameOverTitle());
    }

    @Test
    void startOrRestartResetsSessionToFreshPlayingState() {
        GameSession session = new GameSession();
        session.startOrRestart();
        session.advanceWave();
        session.addAlienKills(2);
        session.loseLife();
        session.enterAlienInvasionGameOver();

        session.startOrRestart();

        assertEquals(0, session.getScore());
        assertEquals(1, session.getWave());
        assertEquals(3, session.getLives());
        assertEquals(GameState.PLAYING, session.getGameState());
        assertFalse(session.isHitFeedbackActive());
        assertEquals("GAME OVER", session.getGameOverTitle());
    }

    @Test
    void alienKillScoreUsesCurrentWaveRule() {
        GameSession session = new GameSession();
        session.startOrRestart();

        session.addAlienKills(2);
        session.advanceWave();
        session.addAlienKills(1);

        assertEquals(40, session.getScore());
    }

    @Test
    void advanceWaveIncrementsWave() {
        GameSession session = new GameSession();

        session.advanceWave();

        assertEquals(2, session.getWave());
    }

    @Test
    void losingLifeActivatesTemporaryHitFeedback() {
        GameSession session = new GameSession();
        session.startOrRestart();

        session.loseLife();

        assertEquals(2, session.getLives());
        assertEquals(GameState.PLAYING, session.getGameState());
        assertTrue(session.isHitFeedbackActive());

        for (int tick = 0; tick < 18; tick++) {
            session.tickHitFeedback();
        }

        assertFalse(session.isHitFeedbackActive());
    }

    @Test
    void thirdLifeLossEntersDefaultGameOver() {
        GameSession session = new GameSession();
        session.startOrRestart();

        session.loseLife();
        session.loseLife();
        session.loseLife();

        assertEquals(0, session.getLives());
        assertEquals(GameState.GAME_OVER, session.getGameState());
        assertEquals("GAME OVER", session.getGameOverTitle());
    }

    @Test
    void alienInvasionEntersAliensWinGameOver() {
        GameSession session = new GameSession();
        session.startOrRestart();

        session.enterAlienInvasionGameOver();

        assertEquals(GameState.GAME_OVER, session.getGameState());
        assertEquals("ALIENS WIN", session.getGameOverTitle());
    }
}
