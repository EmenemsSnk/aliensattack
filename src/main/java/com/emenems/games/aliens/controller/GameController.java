package com.emenems.games.aliens.controller;

import com.emenems.games.aliens.gamemachines.Alien;
import com.emenems.games.aliens.gamemachines.GameObject;
import com.emenems.games.aliens.gamemachines.Missile;
import com.emenems.games.aliens.gamemachines.Spaceship;
import com.emenems.games.aliens.gui.GamePanel;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.swing.Timer;

public class GameController implements ActionListener, Runnable{
    private Spaceship spaceship;
    private List<Missile> missiles;
    private List<Alien> aliens;
    private GamePanel gamePanel;
    private Timer timer;
    Thread thread;
    public GameController(Spaceship spaceship, List<Missile> missiles, List<Alien> aliens, GamePanel gamePanel) {
        this.spaceship = spaceship;
        this.missiles =  missiles;
        this.aliens =  aliens;
        this.gamePanel = gamePanel;
    }

    public void initialize(){
        thread = new Thread(this);
        thread.start();
        generateSpaceObjects();
        gamePanel.addKeyListener(new KeyAdapter() {
            @Override
            public void keyTyped(KeyEvent e) {
                super.keyTyped(e);
                makeAction(e);
            }

            @Override
            public void keyPressed(KeyEvent e) {
                super.keyPressed(e);
                makeAction(e);
            }
        });
    }

    private void generateSpaceObjects() {
        aliens.add(new Alien(10, 30));
        aliens.add(new Alien(100, 30));
        aliens.add(new Alien(200, 30));
        aliens.add(new Alien(300, 30));
        aliens.add(new Alien(400, 30));
        aliens.add(new Alien(500, 30));
        aliens.add(new Alien(600, 30));
        aliens.add(new Alien(700, 30));
        aliens.add(new Alien(800, 30));
        aliens.add(new Alien(900, 30));
    }

    private void makeAction(KeyEvent e){
        if (e.getKeyCode() == KeyEvent.VK_SPACE) {
            missiles.add(new Missile(spaceship.getX(), spaceship.getY() - GamePanel.DEFAULT_COMPONENT_SIZE));
        } else if (e.getKeyCode() == KeyEvent.VK_LEFT) {
            spaceship.moveLeft();
        } else if (e.getKeyCode() == KeyEvent.VK_RIGHT) {
            spaceship.moveRight();
        } else if (e.getKeyCode() == KeyEvent.VK_UP) {
            spaceship.moveUp();
        } else if (e.getKeyCode() == KeyEvent.VK_DOWN) {
            spaceship.moveDown();
        }
        gamePanel.repaint();
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        aliens.forEach(Alien::move);
        missiles.forEach(Missile::move);
        checkCollisions();

        gamePanel.repaint();
    }

    private void checkCollisions() {
        checkCollisionsWithMissile();
        checkCollisionsWithSpaceShip();
    }

    private void checkCollisionsWithSpaceShip() {
        Rectangle spaceshipArea = new Rectangle(spaceship.getX(), spaceship.getY(), GamePanel.DEFAULT_COMPONENT_SIZE, GamePanel.DEFAULT_COMPONENT_SIZE);

        Optional<Alien> optionalAlien = aliens.stream()
            .filter(alien -> {
                Rectangle alienArea = new Rectangle(alien.getX(), alien.getY(), GamePanel.DEFAULT_COMPONENT_SIZE,
                    GamePanel.DEFAULT_COMPONENT_SIZE);
                return spaceshipArea.intersects(alienArea);
            })
            .findFirst();
        if (optionalAlien.isPresent())
            return;
    }

    private void checkCollisionsWithMissile() {
        List<Alien> aliens2Remove = aliens.stream().filter(alien -> {
            Rectangle alienArea = new Rectangle(alien.getX(), alien.getY(), GamePanel.DEFAULT_COMPONENT_SIZE,
                GamePanel.DEFAULT_COMPONENT_SIZE);
            List<Missile> toExtract = missiles.stream().filter(missile -> {
                Rectangle missileArea = new Rectangle(missile.getX(), missile.getY(), GamePanel.DEFAULT_COMPONENT_SIZE,
                    GamePanel.DEFAULT_COMPONENT_SIZE);
                return alienArea.intersects(missileArea);
            })
            .collect(Collectors.toList());
            missiles.removeAll(toExtract);

            return !toExtract.isEmpty();
        })
        .collect(Collectors.toList());

        aliens.removeAll(aliens2Remove);

        for (Alien alien : aliens) {
            Rectangle alienArea = new Rectangle(alien.getX(), alien.getY(), GamePanel.DEFAULT_COMPONENT_SIZE,
                GamePanel.DEFAULT_COMPONENT_SIZE);
            missiles.removeIf(missile -> {
                Rectangle missileArea = new Rectangle(missile.getX(), missile.getY(), GamePanel.DEFAULT_COMPONENT_SIZE,
                    GamePanel.DEFAULT_COMPONENT_SIZE);
                return alienArea.intersects(missileArea);
            });
        }
    }

/*  TODO
    private void checkOutOfBoarder(List<GameObject> gameObjects){
        gameObjects.removeIf(gameObject -> gameObject.getX() > GamePanel.PANEL_WIDTH && gameObject.getY() > GamePanel.PANEL_HEIGHT);
    }*/

    @Override
    public void run() {
        while(true){
            try {
                Thread.sleep(1000);
                aliens.forEach(Alien::move);
                missiles.forEach(Missile::move);
                checkCollisions();

                gamePanel.repaint();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

        }
    }
}
