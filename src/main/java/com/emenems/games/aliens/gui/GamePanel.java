package com.emenems.games.aliens.gui;

import com.emenems.games.aliens.gamemachines.Alien;
import com.emenems.games.aliens.gamemachines.Missile;
import com.emenems.games.aliens.gamemachines.Spaceship;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import javax.swing.ImageIcon;
import javax.swing.JPanel;
import javax.swing.Timer;

public class GamePanel extends JPanel {
    public static final int MINIMUM_BORDER_VALUE = 10;
    public static final int PANEL_HEIGHT = 750;
    public static final int PANEL_WIDTH = 1000;
    public static final Dimension DEFAULT_DIMENSION = new Dimension(25, 25);
    public static final int DEFAULT_COMPONENT_SIZE = 25;
    private Image space;
    private Image alienImage;
    private Image missileImage;
    private Image spaceshipImage;
    private Spaceship spaceship;
    private List<Missile> missiles;
    private List<Alien> aliens;

    public GamePanel(Spaceship spaceship, List<Missile> missiles, List<Alien> aliens) {
        this.spaceship = spaceship;
        this.missiles =  missiles;
        this.aliens =  aliens;
        initBoard();
    }

    private void initBoard() {
        loadImages();
        setMaximumSize(new Dimension(PANEL_WIDTH, PANEL_HEIGHT));
        setFocusable(true);
    }

    private void loadImages() {
        space = new ImageIcon(getClass().getResource("/images/space.jpeg")).getImage();
        alienImage = new ImageIcon(getClass().getResource("/images/alien.png")).getImage();
        missileImage = new ImageIcon(getClass().getResource("/images/missile.gif")).getImage();
        spaceshipImage = new ImageIcon(getClass().getResource("/images/spaceship.png")).getImage();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        g.drawImage(space,0,0,null);
        drawSpaceship(g);
        drawAliens(g);
        drawMissiles(g);
        Toolkit.getDefaultToolkit().sync();
    }

    private void drawMissiles(Graphics graphics) {
        missiles.forEach(missile -> graphics.drawImage(missileImage, missile.getX(),
            missile.getY(), DEFAULT_COMPONENT_SIZE, DEFAULT_COMPONENT_SIZE, this));
    }

    private void drawAliens(Graphics graphics) {
        aliens.forEach(alien -> graphics.drawImage(alienImage, alien.getX(),
            alien.getY(), DEFAULT_COMPONENT_SIZE, DEFAULT_COMPONENT_SIZE, this));

    }

    private void drawSpaceship(Graphics graphics) {
        graphics.drawImage(spaceshipImage, spaceship.getX(), spaceship.getY(), DEFAULT_COMPONENT_SIZE, DEFAULT_COMPONENT_SIZE, this);
    }

}
