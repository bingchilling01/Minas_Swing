package buscaminas;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.*;

public class Buscaminas extends JFrame {

    private JLabel banderas;
    private Scoreboard scoreboard;

    public Buscaminas() {
        initUI();
    }

    private void initUI() {
        setIconImage(Toolkit.getDefaultToolkit().getImage("res/icono.png"));
        banderas = new JLabel("");
        add(banderas, BorderLayout.SOUTH);

        add(new Logica(banderas), BorderLayout.CENTER);

        JButton scoreboardButton = new JButton("Ver Scoreboard");
        scoreboardButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                mostrarScoreboard();
            }
        });

        JPanel buttonPanel = new JPanel();
        buttonPanel.add(scoreboardButton);
        add(buttonPanel, BorderLayout.NORTH);

        setResizable(true);
        pack();

        setTitle("Buscaminas");
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    }

    public void mostrarScoreboard() {
        if (scoreboard == null) { 
            scoreboard = new Scoreboard();
        }

        scoreboard.actualizarScoreboard();
        scoreboard.setVisible(true);
    }

    public static void main(String[] args) {
        EventQueue.invokeLater(() -> {
            Buscaminas juego = new Buscaminas();
            juego.setVisible(true);
        });
    }
}
