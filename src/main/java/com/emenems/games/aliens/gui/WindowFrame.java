package com.emenems.games.aliens.gui;

import java.awt.Color;
import javax.swing.JFrame;
import javax.swing.JPanel;

public class WindowFrame extends JFrame {
    public WindowFrame(JPanel jPanel){
        add(jPanel);
        initUI();
    }

    private void initUI(){
        setBackground(Color.BLACK);
        setResizable(false);
        pack();
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setVisible(true);
    }
}
