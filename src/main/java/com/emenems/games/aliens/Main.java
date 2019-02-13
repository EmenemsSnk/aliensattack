package com.emenems.games.aliens;

import com.emenems.games.aliens.controller.GameController;
import com.emenems.games.aliens.gamemachines.Alien;
import com.emenems.games.aliens.gamemachines.Missile;
import com.emenems.games.aliens.gamemachines.Spaceship;
import com.emenems.games.aliens.gui.GamePanel;
import com.emenems.games.aliens.gui.WindowFrame;
import java.awt.EventQueue;
import java.util.ArrayList;
import java.util.List;

public class Main {
    public static void main(String[] args){
        Spaceship spaceship = new Spaceship(500,680);
        List<Missile> missiles = new ArrayList<>();
        List<Alien> aliens = new ArrayList<>();
        GamePanel gamePanel = new GamePanel(spaceship, missiles, aliens);

        EventQueue.invokeLater(() -> {
            WindowFrame windowFrame = new WindowFrame(gamePanel);
            windowFrame.show();
            GameController controller =  new GameController(spaceship, missiles, aliens,gamePanel);
            controller.initialize();
        });
    }
}
