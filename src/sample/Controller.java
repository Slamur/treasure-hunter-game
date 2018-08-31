package sample;

import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.paint.Color;

import java.awt.*;
import java.net.URL;
import java.util.*;
import java.util.List;

public class Controller implements Initializable {

    private final static Random rnd = new Random();

    private final static double CANVAS_COEFF = 0.5;

    @FXML
    Button joystickButton;

    @FXML
    Canvas labyrinthCanvas;

    @FXML
    Label scoreLabel;

    @FXML
    Label timeLabel;

    private boolean[][] labyrinth;
    private int rows, columns;
    private double cellHeight, cellWidth;

    private final static int[][] steps = {
            {-1, 0}, {1, 0}, {0, -1}, {0, 1}
    };

    private Point heroPosition, enemyPosition;
    private Point exitPosition, goldPosition;
    private boolean goldTaken;

    private static final Color EMPTY_COLOR = Color.WHITE, WALL_COLOR = Color.DARKGRAY;
    private static final Color HERO_COLOR = Color.BLUE, ENEMY_COLOR = Color.DARKRED;
    private static final Color EXIT_COLOR = Color.BLACK, GOLD_COLOR = Color.GOLD;

    private int score;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        initCanvas();
        clearCanvas();

        initButtons();

        this.score = 0;

        startGame();
    }

    private void startGame() {
        clearCanvas();

        initLabyrinth();
        drawLabyrinth();

        initHero();
        initEnemy();

        initExit();
        initGold();

        drawScore();

        drawObjects();
    }

    private void drawScore() {
        scoreLabel.setText("" + score);
    }

    private void winGame() {
        ++score;
        startGame();
    }

    private void endGame() {
        score = 0;
        startGame();
    }

    private void clearObjects() {
        for (Point position : new Point[] { heroPosition, enemyPosition, enemyPosition, goldPosition }) {
            clearCell(position);
        }
    }

    private void drawObjects() {
        drawHero();
        drawEnemy();
        drawGold();
        drawExit();
    }

    private void initCanvas() {
        Dimension size = Toolkit.getDefaultToolkit().getScreenSize();

        labyrinthCanvas.setWidth(size.width * CANVAS_COEFF);
        labyrinthCanvas.setHeight(size.height * CANVAS_COEFF);
    }

    private void clearCanvas() {
        GraphicsContext graphics = labyrinthCanvas.getGraphicsContext2D();
        graphics.setFill(Color.WHITE);
        graphics.fillRect(0, 0, labyrinthCanvas.getWidth(), labyrinthCanvas.getHeight());
    }

    private void initLabyrinth() {
        this.labyrinth = generateLabyrinth(20, 40, 0.7);

        this.rows = labyrinth.length;
        this.columns = labyrinth[0].length;

        this.cellHeight = labyrinthCanvas.getHeight() / rows;
        this.cellWidth = labyrinthCanvas.getWidth() / columns;
    }

    private static boolean[][] generateLabyrinth(int rows, int columns, double emptyProbability) {
        boolean[][] labyrinth = new boolean[rows + 2][columns + 2];

        Arrays.fill(labyrinth[0], true);
        for (int i = 1; i <= rows; ++i) {
            labyrinth[i][0] = true;
            for (int j = 1; j <= columns; ++j) {
                labyrinth[i][j] = (rnd.nextDouble() > emptyProbability);
            }
            labyrinth[i][columns + 1] = true;
        }
        Arrays.fill(labyrinth[rows + 1], true);

        return labyrinth;
    }

    private void drawLabyrinth() {
        GraphicsContext graphics = labyrinthCanvas.getGraphicsContext2D();

        clearCanvas();

        graphics.setFill(WALL_COLOR);
        for (int i = 0; i < rows; ++i) {
            for (int j = 0; j < columns; ++j) {
                if (labyrinth[i][j]) drawCell(graphics, i, j);
            }
        }
    }

    private void clearCell(Point point) {
        drawCell(point, labyrinth[point.x][point.y] ? WALL_COLOR : EMPTY_COLOR);
    }

    private void drawCell(Point point, Color color) {
        drawCell(point.x, point.y, color);
    }

    private void drawCell(int i, int j, Color color) {
        GraphicsContext graphics = labyrinthCanvas.getGraphicsContext2D();
        drawCell(graphics, i, j, color);
    }

    private void drawCell(GraphicsContext graphics, int i, int j, Color color) {
        graphics.setFill(color);
        drawCell(graphics, i, j);
    }

    private void drawCell(GraphicsContext graphics, int i, int j) {
        graphics.fillRect(j * cellWidth, i * cellHeight, cellWidth, cellHeight);
    }

    private void initHero() {
        DSU dsu = calculateComponents();

        this.heroPosition = calculateStartPosition(dsu);
    }

    private void initEnemy() {
        this.enemyPosition = calculateEnemyStartPosition();
    }

    private void initExit() {
        List<Point> queue = (List<Point>)heroBfs()[0];

        int exitIndex = rnd.nextInt(queue.size());
        this.exitPosition = queue.get(exitIndex);
    }

    private void initGold() {
        List<Point> queue = (List<Point>)heroBfs()[0];

        for (goldPosition = null; goldPosition == null; ) {
            int goldIndex = rnd.nextInt(queue.size());
            this.goldPosition = queue.get(goldIndex);

            if (heroPosition.equals(goldPosition) || exitPosition.equals(goldPosition)) {
                goldPosition = null;
            }
        }

        goldTaken = false;
    }

    private static boolean checkIndex(int index, int size) {
        return 0 <= index && index < size;
    }

    private static boolean checkCell(int x, int rows, int y, int columns) {
        return checkIndex(x, rows) && checkIndex(y, columns);
    }

    private boolean checkCell(int x, int y) {
        return checkCell(x, rows, y, columns);
    }

    private DSU calculateComponents() {
        int size = rows * columns;

        DSU dsu = new DSU(size);

        for (int i = 0; i < rows; ++i) {
            for (int j = 0; j < columns; ++j) {
                if (labyrinth[i][j]) continue;

                for (int[] step : steps) {
                    int x = i + step[0], y = j + step[1];

                    if (!checkCell(x, y)) continue;
                    if (labyrinth[x][y]) continue;

                    dsu.union(i * columns + j, x * columns + y);
                }
            }
        }

        return dsu;
    }

    private Point calculateStartPosition(DSU dsu) {
        int maxSize = 0;
        for (int i = 0, v = 0; i < rows; ++i) {
            for (int j = 0; j < columns; ++j, ++v) {
                if (labyrinth[i][j]) continue;
                maxSize = Math.max(maxSize, dsu.size(v));
            }
        }

        List<Integer> possibleStarts = new ArrayList<>();

        for (int i = 0, v = 0; i < rows; ++i) {
            for (int j = 0; j < columns; ++j, ++v) {
                if (labyrinth[i][j]) continue;

                if (dsu.size(v) == maxSize) {
                    possibleStarts.add(v);
                }
            }
        }

        int start = possibleStarts.get(rnd.nextInt(possibleStarts.size()));

        return new Point(start / columns, start % columns);
    }

    private Point calculateEnemyStartPosition() {
        Object[] bfsResult = heroBfs();

        List<Point> queue = (List<Point>) bfsResult[0];
        int[][] distances = (int[][]) bfsResult[1];

        int size = queue.size();
        Point lastCell = queue.get(size - 1);
        int maxDistance = distances[lastCell.x][lastCell.y];

        int firstMaxIndex = size;
        while (firstMaxIndex > 0) {
            Point next = queue.get(firstMaxIndex - 1);
            if (distances[next.x][next.y] == maxDistance) --firstMaxIndex;
            else break;
        }

        int startIndex = rnd.nextInt(size - firstMaxIndex) + firstMaxIndex;
        return queue.get(startIndex);
    }

    private void drawHero() {
        drawPlayer(heroPosition, HERO_COLOR);
    }

    private void drawEnemy() {
        drawPlayer(enemyPosition, ENEMY_COLOR);
    }

    private void drawPlayer(Point playerPosition, Color playerColor) {
        drawCell(playerPosition, playerColor);
    }

    private void drawExit() {
        if (!enemyPosition.equals(exitPosition)) {
            drawCell(exitPosition, EXIT_COLOR);
        }
    }

    private void drawGold() {
        if (!goldTaken && !heroPosition.equals(goldPosition) && !enemyPosition.equals(goldPosition)) {
            drawCell(goldPosition, GOLD_COLOR);
        }
    }

    private static final KeyCode[] CODES = {
        KeyCode.UP, KeyCode.DOWN, KeyCode.LEFT, KeyCode.RIGHT
    };

    private void initButtons() {
        EventHandler<KeyEvent> heroMoveEventHandler = event -> {
            KeyCode code = event.getCode();

            int direction = -1;
            for (int i = 0; i < CODES.length; ++i) {
                if (CODES[i].equals(code)) {
                    direction = i;
                }
            }

            if (direction == -1) return;

            clearObjects();

            if (moveHero(direction)) return;
            if (moveEnemy()) return;

            drawObjects();
        };

        joystickButton.setOnKeyPressed(heroMoveEventHandler);
    }

    private boolean moveHero(int direction) {
        int nextX = heroPosition.x + steps[direction][0];
        int nextY = heroPosition.y + steps[direction][1];

        if (!checkCell(nextX, nextY)) return false;
        if (labyrinth[nextX][nextY]) return false;

        heroPosition.x = nextX;
        heroPosition.y = nextY;

        if (!goldTaken && heroPosition.equals(goldPosition)) {
            goldTaken = true;
        }

        if (goldTaken && heroPosition.equals(exitPosition)) {
            winGame();
            return true;
        }

        if (enemyPosition.equals(heroPosition)) {
            endGame();
            return true;
        }

        return false;
    }

    private Object[] heroBfs() {
        List<Point> queue = new ArrayList<>();

        int[][] distances = new int[rows][columns];
        for (int[] d1 : distances) {
            Arrays.fill(d1, -1);
        }

        queue.add(heroPosition);
        distances[heroPosition.x][heroPosition.y] = 0;

        for (int i = 0; i < queue.size(); ++i) {
            Point from = queue.get(i);

            for (int[] step : steps) {
                int toX = from.x + step[0];
                int toY = from.y + step[1];

                if (!checkCell(toX, toY)) continue;
                if (labyrinth[toX][toY]) continue;
                if (distances[toX][toY] != -1) continue;

                distances[toX][toY] = distances[from.x][from.y] + 1;
                queue.add(new Point(toX, toY));
            }
        }

        return new Object[] {
            queue, distances
        };
    }

    private boolean moveEnemy() {
        Object[] bfsResult = heroBfs();

        int[][] distances = (int[][]) bfsResult[1];

        List<Point> bestPositions = new ArrayList<>();
        bestPositions.add(enemyPosition);

        int bestDistance = distances[enemyPosition.x][enemyPosition.y];

        for (int[] step : steps) {
            int nextX = enemyPosition.x + step[0];
            int nextY = enemyPosition.y + step[1];

            if (!checkCell(nextX, nextY)) continue;
            if (labyrinth[nextX][nextY]) continue;

            int distance = distances[nextX][nextY];
            if (bestDistance > distance) {
                bestDistance = distance;
                bestPositions.clear();
            }

            if (bestDistance == distance){
                bestPositions.add(new Point(nextX, nextY));
            }
        }

        int nextIndex = rnd.nextInt(bestPositions.size());
        enemyPosition = bestPositions.get(nextIndex);

        if (heroPosition.equals(enemyPosition)) {
            endGame();
            return true;
        }

        return false;
    }
}
