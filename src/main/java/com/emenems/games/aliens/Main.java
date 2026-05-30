package com.emenems.games.aliens;

import com.emenems.games.aliens.controller.GameController;
import com.emenems.games.aliens.gamemachines.Alien;
import com.emenems.games.aliens.gamemachines.AlienMissile;
import com.emenems.games.aliens.gamemachines.Missile;
import com.emenems.games.aliens.gamemachines.Spaceship;
import com.emenems.games.aliens.gui.GamePanel;
import com.emenems.games.aliens.gui.WindowFrame;
import java.awt.EventQueue;
import java.util.ArrayList;
import java.util.List;

public class Main {
    public static void main(String[] args){
        Spaceship spaceship = new Spaceship(
            (GamePanel.PANEL_WIDTH - GamePanel.DEFAULT_COMPONENT_SIZE) / 2,
            GamePanel.PANEL_HEIGHT - GamePanel.DEFAULT_COMPONENT_SIZE - 20
        );
        List<Missile> missiles = new ArrayList<>();
        List<AlienMissile> alienMissiles = new ArrayList<>();
        List<Alien> aliens = new ArrayList<>();
        GamePanel gamePanel = new GamePanel(spaceship, missiles, alienMissiles, aliens);

        EventQueue.invokeLater(() -> {
            WindowFrame windowFrame = new WindowFrame(gamePanel);
            windowFrame.show();
            GameController controller =  new GameController(spaceship, missiles, alienMissiles, aliens,gamePanel);
            controller.initialize();
        });
    }
}
