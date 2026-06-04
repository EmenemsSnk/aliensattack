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
        assertEquals(1, session.getComboMultiplier());
        assertEquals(0, session.getComboTicks());
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
        assertEquals(1, session.getComboMultiplier());
        assertEquals(0, session.getComboTicks());
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
    void consecutiveKillBatchesImmediatelyIncreaseComboAndRefreshTimer() {
        GameSession session = new GameSession();
        session.startOrRestart();

        session.addAlienKills(1);

        assertEquals(10, session.getScore());
        assertEquals(1, session.getComboMultiplier());
        assertEquals(GameSession.COMBO_DURATION_TICKS, session.getComboTicks());

        session.tickCombo();
        session.addAlienKills(2);

        assertEquals(50, session.getScore());
        assertEquals(2, session.getComboMultiplier());
        assertEquals(GameSession.COMBO_DURATION_TICKS, session.getComboTicks());
    }

    @Test
    void comboMultiplierCapsAtFiveAndKillBatchAdvancesOnlyOnce() {
        GameSession session = new GameSession();
        session.startOrRestart();

        for (int event = 0; event < 7; event++) {
            session.addAlienKills(event == 1 ? 2 : 1);
        }

        assertEquals(5, session.getComboMultiplier());
        assertEquals(270, session.getScore());
    }

    @Test
    void comboExpiresAndNextKillStartsFreshChain() {
        GameSession session = new GameSession();
        session.startOrRestart();
        session.addAlienKills(1);
        session.addAlienKills(1);

        for (int tick = 0; tick < GameSession.COMBO_DURATION_TICKS; tick++) {
            session.tickCombo();
        }

        assertFalse(session.isComboActive());
        assertEquals(1, session.getComboMultiplier());
        assertEquals(0, session.getComboTicks());

        session.addAlienKills(1);

        assertEquals(40, session.getScore());
        assertEquals(1, session.getComboMultiplier());
    }

    @Test
    void zeroKillBatchDoesNotStartAdvanceOrRefreshCombo() {
        GameSession session = new GameSession();
        session.startOrRestart();

        session.addAlienKills(0);

        assertFalse(session.isComboActive());
        assertEquals(0, session.getScore());

        session.addAlienKills(1);
        session.tickCombo();
        session.addAlienKills(0);

        assertEquals(1, session.getComboMultiplier());
        assertEquals(GameSession.COMBO_DURATION_TICKS - 1, session.getComboTicks());
        assertEquals(10, session.getScore());
    }

    @Test
    void lifeLossWaveAdvanceAndRestartResetCombo() {
        GameSession session = new GameSession();
        session.startOrRestart();
        session.addAlienKills(1);
        session.addAlienKills(1);

        session.loseLife();

        assertEquals(1, session.getComboMultiplier());
        assertEquals(0, session.getComboTicks());

        session.addAlienKills(1);
        session.addAlienKills(1);
        session.advanceWave();

        assertEquals(1, session.getComboMultiplier());
        assertEquals(0, session.getComboTicks());

        session.addAlienKills(1);
        session.addAlienKills(1);
        session.startOrRestart();

        assertEquals(1, session.getComboMultiplier());
        assertEquals(0, session.getComboTicks());
    }

    @Test
    void rapidFireLifecycleDoesNotChangeCombo() {
        GameSession session = new GameSession();
        session.startOrRestart();
        session.addAlienKills(1);
        session.addAlienKills(1);

        session.activateRapidFire();
        session.tickRapidFire();

        assertEquals(2, session.getComboMultiplier());
        assertEquals(GameSession.COMBO_DURATION_TICKS, session.getComboTicks());
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

    @Test
    void rapidFireActivationRefreshesAndExpiresAfterConfiguredDuration() {
        GameSession session = new GameSession();
        session.startOrRestart();

        session.activateRapidFire();

        assertTrue(session.isRapidFireActive());
        assertEquals(GameSession.RAPID_FIRE_DURATION_TICKS, session.getRapidFireTicks());

        session.tickRapidFire();
        session.activateRapidFire();

        assertEquals(GameSession.RAPID_FIRE_DURATION_TICKS, session.getRapidFireTicks());

        for (int tick = 0; tick < GameSession.RAPID_FIRE_DURATION_TICKS; tick++) {
            session.tickRapidFire();
        }

        assertFalse(session.isRapidFireActive());
        assertEquals(0, session.getRapidFireTicks());
    }

    @Test
    void rapidFireSurvivesWaveAdvanceButLifeLossAndRestartClearIt() {
        GameSession session = new GameSession();
        session.startOrRestart();
        session.activateRapidFire();

        session.advanceWave();

        assertTrue(session.isRapidFireActive());
        assertEquals(GameSession.RAPID_FIRE_DURATION_TICKS, session.getRapidFireTicks());

        session.loseLife();

        assertFalse(session.isRapidFireActive());
        assertEquals(0, session.getRapidFireTicks());

        session.activateRapidFire();
        session.startOrRestart();

        assertFalse(session.isRapidFireActive());
        assertEquals(0, session.getRapidFireTicks());
    }
}
