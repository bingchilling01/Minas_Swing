package buscaminas;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Random;
import java.util.Vector;

import javax.swing.*;
import java.sql.*;

import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;

import org.apache.commons.validator.routines.EmailValidator;

class Scoreboard extends JFrame {

    private JTable scoreboardTable;

    public Scoreboard() {
        initUI();
        actualizarScoreboard();
    }

    private void initUI() {
        setTitle("Scoreboard 😃"); // Agregando la carita del buscaminas
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLocationRelativeTo(null);

        scoreboardTable = new JTable();
        scoreboardTable.setEnabled(false);

        // Personalizar la apariencia de las celdas
        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(JLabel.CENTER);
        scoreboardTable.setDefaultRenderer(Object.class, centerRenderer);

        JScrollPane scrollPane = new JScrollPane(scoreboardTable);
        add(scrollPane);

        // Establecer el aspecto de la tabla
        setTableAppearance();

        pack();
    }

    private void setTableAppearance() {
        scoreboardTable.getTableHeader().setFont(new Font("Arial", Font.BOLD, 14));
        scoreboardTable.setFont(new Font("Arial", Font.PLAIN, 12));
        scoreboardTable.setRowHeight(20);
        scoreboardTable.setSelectionBackground(new Color(220, 220, 220));
    }

    public void actualizarScoreboard() {
        try (Connection conexion = DriverManager.getConnection("jdbc:mysql://localhost/buscaminas", "root", "");
             Statement statement = conexion.createStatement()) {

            ResultSet resultSet = statement.executeQuery("SELECT nombre, dificultad, puntuacion FROM leaderboard");
            Vector<String> columnNames = new Vector<>();
            columnNames.add("Nombre");
            columnNames.add("Dificultad");
            columnNames.add("Puntuación");

            Vector<Vector<Object>> data = new Vector<>();

            while (resultSet.next()) {
                Vector<Object> row = new Vector<>();
                row.add(resultSet.getString("nombre"));
                row.add(resultSet.getString("dificultad"));
                row.add(resultSet.getInt("puntuacion"));
                data.add(row);
            }

            DefaultTableModel model = new DefaultTableModel(data, columnNames);
            scoreboardTable.setModel(model);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            Scoreboard scoreboard = new Scoreboard();
            scoreboard.setVisible(true);
        });
    }
}

public class Logica extends JPanel {

    private final int MOSAICOS = 13;
    private final int tamCelda = 15;

    private final int COVER_FOR_CELL = 10;
    private final int MARK_FOR_CELL = 10;
    private final int EMPTY_CELL = 0;
    private final int celdaMina = 9;
    private final int COVERED_MINE_CELL = celdaMina + COVER_FOR_CELL;
    private final int MARKED_MINE_CELL = COVERED_MINE_CELL + MARK_FOR_CELL;

    private final int MINA = 9;
    private final int CUBIERTA = 10;
    private final int BANDERA = 11;
    private final int noBANDERA = 12;

    private int numMinas;
    private int filas;
    private int columnas;

    private int ancho;
    private int alto;
    
    private int dificultad;
    private String[] stringDificultad = { "UltraMegaSuperFácil",  "Fácil", "Normal", "Difícil" };
    
    private int puntuacion;

    private int[] field;
    private boolean inGame;
    private int minasRestantes;
    private Image[] img;

    private int celdasTotales;
    private final JLabel labelInfo;
    
    private final String rutaRecursos = "res/";
    
    private String nombre, email, fecha;
    
    private ZonedDateTime ahora = ZonedDateTime.now();

    public Logica(JLabel labelBP) {
    	puntuacion = 0;
        this.labelInfo = labelBP;
        iniciarJuego();
    }
    
    private void jugarDeNuevo(String perdido) {
        int opcion = JOptionPane.showOptionDialog(null,
                perdido + "¿Qué quieres hacer?",
                "Fin del juego", JOptionPane.DEFAULT_OPTION,
                JOptionPane.PLAIN_MESSAGE, null,
                new Object[]{"Jugar de nuevo", "Cambiar dificultad", "Salir"}, "Jugar de nuevo");

        if (opcion == 0) {
            nuevoJuego();
            revalidate();
            repaint();
        } else if (opcion == 1) {
        	cambiarDificultad();
        } else {
            System.exit(0);
        }
    }

    private void cambiarDificultad() {
        dificultad = JOptionPane.showOptionDialog(null, "Selecciona la dificultad", "Buscaminas", JOptionPane.DEFAULT_OPTION, JOptionPane.PLAIN_MESSAGE, null, stringDificultad, stringDificultad[0]);;
        switch (dificultad) {
            case 0:
                filas = 10;
                columnas = 10;
                numMinas = 2;
                labelInfo.setFont(new Font("", Font.BOLD, 9));
                break;
            case 1:
                filas = 10;
                columnas = 10;
                numMinas = 15;
                labelInfo.setFont(new Font("", Font.BOLD, 9));
                break;
            case 2:
                filas = 14;
                columnas = 14;
                numMinas = 25;
                break;
            case 3:
                filas = 17;
                columnas = 17;
                numMinas = 45;
                break;
            default:
                System.exit(0);
                break;
        }

        // Actualiza el tamaño del JFrame
        JFrame frame = (JFrame) SwingUtilities.getWindowAncestor(this);
        ancho = columnas * tamCelda + 18;
        alto = filas * tamCelda + 88;
        frame.setSize(ancho, alto);

        // Vuelve a iniciar el juego
        nuevoJuego();

        // Revalida y repinta el panel del juego
        revalidate();
        repaint();
    }
    
    private void actualizarPuntos() {
    	puntuacion += 10;
    	labelInfo.setText("Banderitas: " + Integer.toString(minasRestantes) + " / Puntuación: " + Integer.toString(puntuacion));
    }
    
    private void insertarGanador() {
    	try {
    		Class.forName("com.mysql.cj.jdbc.Driver");
    		Connection conexion = DriverManager.getConnection("jdbc:mysql://localhost/buscaminas", "root", "");
    		Statement insertarNuevo = conexion.createStatement();
    		switch(dificultad) {
    		case 0:
    			ejecutarSentenciaInsercion(insertarNuevo, stringDificultad[0]);
    			break;
    		case 1:
    			ejecutarSentenciaInsercion(insertarNuevo, stringDificultad[1]);
    			break;
    		case 2:
    			ejecutarSentenciaInsercion(insertarNuevo, stringDificultad[2]);
    			break;
    		case 3:
    			ejecutarSentenciaInsercion(insertarNuevo, stringDificultad[3]);
    			break;
    		}
    		insertarNuevo.close();
    		conexion.close();
    	} catch (Exception ex) {
    		JOptionPane.showMessageDialog(this, "Error al conectarse a la base de datos, no se ha podido insertar los datos.", "Error", JOptionPane.ERROR_MESSAGE);
    	}
    }
    
    private void ejecutarSentenciaInsercion(Statement insertarNuevo, String dificultad) {
    	try {
    		// La base de datos tiene las siguientes columnas: (ID(INT AUTO_INCREMENT), nombre (VARCHAR(80)), email (VARCHAR(80)), fecha (VARCHAR(18)), dificultad (VARCHAR(40), puntuacion (INT (11)))
			insertarNuevo.executeUpdate("INSERT INTO leaderboard VALUES(0, '" + nombre + "', '" + email + "', '" + fecha + "', '" + dificultad + "', " + puntuacion + ");" );
		} catch (SQLException e) {
			e.printStackTrace();
		}
    }
    
    private void playSonidos(String fichAudio) {
    	try {
    		File soundFile = new File(rutaRecursos + "sonidos/" + fichAudio);
            Clip clip = AudioSystem.getClip();
            clip.open(AudioSystem.getAudioInputStream(soundFile));
            clip.start();
    	} catch (Exception exc) {
    		exc.printStackTrace();
    	}
    }

    private void iniciarJuego() {
    	
        dificultad = JOptionPane.showOptionDialog(null, "Selecciona la dificultad", "Buscaminas", JOptionPane.DEFAULT_OPTION, JOptionPane.PLAIN_MESSAGE, null, stringDificultad, stringDificultad[0]);
        switch (dificultad) {
        
		case 0: {
			filas = 10;
			columnas = 10;
			numMinas = 2;
			labelInfo.setFont(new Font("", Font.BOLD, 9));
			break;
		}
		
		case 1: {
			filas = 10;
			columnas = 10;
			numMinas = 15;
			labelInfo.setFont(new Font("", Font.BOLD, 9));
			break;
		}
		
		case 2: {
			filas = 14;
			columnas = 14;
			numMinas = 25;
			break;
		}
		
		case 3: {
			filas = 17;
			columnas = 17;
			numMinas = 45;
			break;
		}
		
		default: {
			System.exit(0);
			break;
		}
		}
        ancho = columnas * tamCelda + 1;
        alto = filas * tamCelda + 1;

        setPreferredSize(new Dimension(ancho, alto));

        img = new Image[MOSAICOS];

        for (int i = 0; i < MOSAICOS; i++) {

            String rutaMosaicos = rutaRecursos + "mosaicos/" + i + ".png";
            img[i] = (new ImageIcon(rutaMosaicos)).getImage();
        }

        addMouseListener(new MinesAdapter());
        nuevoJuego();
    }

    private void nuevoJuego() {
    	
    	playSonidos("init.wav");

        int celda;

        Random aleatorio = new Random();
        inGame = true;
        minasRestantes = numMinas;

        celdasTotales = filas * columnas;
        field = new int[celdasTotales];

        for (int i = 0; i < celdasTotales; i++) {
            field[i] = COVER_FOR_CELL;
        }

        labelInfo.setText("Banderitas: " + Integer.toString(minasRestantes) + " / Puntuación: " + Integer.toString(puntuacion));

        int i = 0;

        while (i < numMinas) {

            int posicion = (int) (celdasTotales * aleatorio.nextDouble());

            if ((posicion < celdasTotales) && (field[posicion] != COVERED_MINE_CELL)) {
            	
                int current_col = posicion % columnas;
                field[posicion] = COVERED_MINE_CELL;
                i++;

                if (current_col > 0) {
                    celda = posicion - 1 - columnas;
                    if (celda >= 0) {
                        if (field[celda] != COVERED_MINE_CELL) {
                            field[celda] += 1;
                        }
                    }
                    celda = posicion - 1;
                    if (celda >= 0) {
                        if (field[celda] != COVERED_MINE_CELL) {
                            field[celda] += 1;
                        }
                    }

                    celda = posicion + columnas - 1;
                    if (celda < celdasTotales) {
                        if (field[celda] != COVERED_MINE_CELL) {
                            field[celda] += 1;
                        }
                    }
                }

                celda = posicion - columnas;
                if (celda >= 0) {
                    if (field[celda] != COVERED_MINE_CELL) {
                        field[celda] += 1;
                    }
                }

                celda = posicion + columnas;
                if (celda < celdasTotales) {
                    if (field[celda] != COVERED_MINE_CELL) {
                        field[celda] += 1;
                    }
                }

                if (current_col < (columnas - 1)) {
                    celda = posicion - columnas + 1;
                    if (celda >= 0) {
                        if (field[celda] != COVERED_MINE_CELL) {
                            field[celda] += 1;
                        }
                    }
                    celda = posicion + columnas + 1;
                    if (celda < celdasTotales) {
                        if (field[celda] != COVERED_MINE_CELL) {
                            field[celda] += 1;
                        }
                    }
                    celda = posicion + 1;
                    if (celda < celdasTotales) {
                        if (field[celda] != COVERED_MINE_CELL) {
                            field[celda] += 1;
                        }
                    }
                }
            }
        }
    }

    private void find_empty_cells(int j) {

        int current_col = j % columnas;
        int cell;
        
        actualizarPuntos();
        
        if (current_col > 0) {
            cell = j - columnas - 1;
            if (cell >= 0) {
                if (field[cell] > celdaMina) {
                    field[cell] -= COVER_FOR_CELL;
                    if (field[cell] == EMPTY_CELL) {
                        find_empty_cells(cell);
                    }
                }
            }

            cell = j - 1;
            if (cell >= 0) {
                if (field[cell] > celdaMina) {
                    field[cell] -= COVER_FOR_CELL;
                    if (field[cell] == EMPTY_CELL) {
                        find_empty_cells(cell);
                    }
                }
            }

            cell = j + columnas - 1;
            if (cell < celdasTotales) {
                if (field[cell] > celdaMina) {
                    field[cell] -= COVER_FOR_CELL;
                    if (field[cell] == EMPTY_CELL) {
                        find_empty_cells(cell);
                    }
                }
            }
        }

        cell = j - columnas;
        if (cell >= 0) {
            if (field[cell] > celdaMina) {
                field[cell] -= COVER_FOR_CELL;
                if (field[cell] == EMPTY_CELL) {
                    find_empty_cells(cell);
                }
            }
        }

        cell = j + columnas;
        if (cell < celdasTotales) {
            if (field[cell] > celdaMina) {
                field[cell] -= COVER_FOR_CELL;
                if (field[cell] == EMPTY_CELL) {
                    find_empty_cells(cell);
                }
            }
        }

        if (current_col < (columnas - 1)) {
            cell = j - columnas + 1;
            if (cell >= 0) {
                if (field[cell] > celdaMina) {
                    field[cell] -= COVER_FOR_CELL;
                    if (field[cell] == EMPTY_CELL) {
                        find_empty_cells(cell);
                    }
                }
            }

            cell = j + columnas + 1;
            if (cell < celdasTotales) {
                if (field[cell] > celdaMina) {
                    field[cell] -= COVER_FOR_CELL;
                    if (field[cell] == EMPTY_CELL) {
                        find_empty_cells(cell);
                    }
                }
            }

            cell = j + 1;
            if (cell < celdasTotales) {
                if (field[cell] > celdaMina) {
                    field[cell] -= COVER_FOR_CELL;
                    if (field[cell] == EMPTY_CELL) {
                        find_empty_cells(cell);
                    }
                }
            }
        }

    }

    @Override
    public void paintComponent(Graphics g) {

        int uncover = 0;

        for (int i = 0; i < filas; i++) {

            for (int j = 0; j < columnas; j++) {

                int cell = field[(i * columnas) + j];

                if (inGame && cell == celdaMina) {
                    inGame = false;
                }

                if (!inGame) {

                    if (cell == COVERED_MINE_CELL) {
                        cell = MINA;
                    } else if (cell == MARKED_MINE_CELL) {
                        cell = BANDERA;
                    } else if (cell > COVERED_MINE_CELL) {
                        cell = noBANDERA;
                    } else if (cell > celdaMina) {
                        cell = CUBIERTA;
                    }

                } else {

                    if (cell > COVERED_MINE_CELL) {
                        cell = BANDERA;
                    } else if (cell > celdaMina) {
                        cell = CUBIERTA;
                        uncover++;
                    }
                }

                g.drawImage(img[cell], (j * tamCelda),
                        (i * tamCelda), this);
            }
        }

        if (uncover == 0 && inGame) {
            inGame = false;
            playSonidos("ganar.wav");
            labelInfo.setText("¡¡¡Has GANAO!!!");
            fecha = ahora.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));

            boolean nombreValido = false;
            while (!nombreValido) {
                nombre = JOptionPane.showInputDialog(this, "¡¡HAS GANAO!!, introduce tus iniciales (3 letras): ", "Ganador", JOptionPane.QUESTION_MESSAGE);

                // Verificar que el nombre tenga exactamente 3 letras
                if (nombre != null && nombre.length() == 3) {
                    nombreValido = true;
                } else {
                    JOptionPane.showMessageDialog(this, "El nombre debe tener exactamente 3 letras.", "Error", JOptionPane.ERROR_MESSAGE);
                }
            }

            boolean emailValido = false;
            while (!emailValido) {
                email = JOptionPane.showInputDialog(this, "Introduce tu correo: ", "Ganador", JOptionPane.QUESTION_MESSAGE);

                // Validar el formato del correo electrónico usando Apache Commons Validator
                if (email != null && validarCorreoElectronico(email)) {
                    emailValido = true;
                } else {
                    JOptionPane.showMessageDialog(this, "Por favor, introduce un correo electrónico válido.", "Error", JOptionPane.ERROR_MESSAGE);
                }
            }

            if (nombre != null && email != null) {
                insertarGanador();
            }

            puntuacion = 0;

            jugarDeNuevo("");
        }
    }
    
    private boolean validarCorreoElectronico(String email) {
        EmailValidator validator = EmailValidator.getInstance();
        return validator.isValid(email);
    }

    private class MinesAdapter extends MouseAdapter {

        @Override
        public void mousePressed(MouseEvent e) {

            int x = e.getX();
            int y = e.getY();

            int cCol = x / tamCelda;
            int cRow = y / tamCelda;

            boolean doRepaint = false;

            if (!inGame) {
                nuevoJuego();
                repaint();
            }

            if ((x < columnas * tamCelda) && (y < filas * tamCelda)) {

                if (e.getButton() == MouseEvent.BUTTON3) {

                    if (field[(cRow * columnas) + cCol] > celdaMina) {

                        doRepaint = true;

                        if (field[(cRow * columnas) + cCol] <= COVERED_MINE_CELL) {

                            if (minasRestantes > 0) {
                                field[(cRow * columnas) + cCol] += MARK_FOR_CELL;
                                minasRestantes--;
                                playSonidos("bandera.wav");
                                
                                labelInfo.setText("Banderitas: " + Integer.toString(minasRestantes) + " / Puntuación: " + Integer.toString(puntuacion));
                            } else {
                            	playSonidos("sinBandera.wav");
                                labelInfo.setText("No te quedan banderitas" + " / Puntuación: " + Integer.toString(puntuacion));
                            }
                        } else {
                            field[(cRow * columnas) + cCol] -= MARK_FOR_CELL;
                            minasRestantes++;
                            playSonidos("devBandera.wav");
                            labelInfo.setText("Banderitas: " + Integer.toString(minasRestantes) + " / Puntuación: " + Integer.toString(puntuacion));
                        }
                    }

                } else {

                    if (field[(cRow * columnas) + cCol] > COVERED_MINE_CELL) {
                        return;
                    }

                    if ((field[(cRow * columnas) + cCol] > celdaMina) && (field[(cRow * columnas) + cCol] < MARKED_MINE_CELL)) {
                    	
                    	// Quito esto porque todo el mundo que gane tendría la misma puntuación
                        // actualizarPuntos();
                    	playSonidos("boton.wav");
                    	
                        field[(cRow * columnas) + cCol] -= COVER_FOR_CELL;
                        doRepaint = true;

                        if (field[(cRow * columnas) + cCol] == celdaMina) {
                        	playSonidos("boom.wav");
                        	labelInfo.setText("BOOM, has pisado una mina :(");
                            puntuacion = 0;
                            inGame = false;
                            
                            jugarDeNuevo("Has perdido. ");
                            
                        }

                        if (field[(cRow * columnas) + cCol] == EMPTY_CELL) {
                            find_empty_cells((cRow * columnas) + cCol);
                        }
                    }
                }

                if (doRepaint) {
                    repaint();
                }
            }
        }
    }
}
