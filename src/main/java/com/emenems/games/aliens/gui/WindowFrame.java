package com.emenems.games.aliens.gui;

import java.awt.Color;
import javax.swing.JFrame;
import javax.swing.JPanel;

public class WindowFrame extends JFrame {
    private static final int FRAME_HEIGHT = 750;
    private static final int FRAME_WIDTH = 1000;

    public WindowFrame(JPanel jPanel){
        initUI();
        add(jPanel);
    }

    private void initUI(){
        setBounds(100, 100, FRAME_WIDTH, FRAME_HEIGHT);
        setBackground(Color.BLACK);
        setResizable(false);
        setVisible(true);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    }
}
