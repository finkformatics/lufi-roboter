package dev.lukasfink.robotprogrammer.components;

import dev.lukasfink.robotprogrammer.flow.RobotInstruction;
import javafx.animation.*;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Point2D;
import javafx.scene.Group;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.media.AudioClip;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;

import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

public class Robot extends Group {

    private static final int ROBOT_IMAGE_WIDTH = 150;
    private static final int ROBOT_IMAGE_HEIGHT = 150;

    private static final int ROBOT_GROUP_WIDTH = 200;
    private static final int ROBOT_GROUP_HEIGHT = 200;

    private static final int ROBOT_TIRE_WIDTH = 64;
    private static final int ROBOT_TIRE_HEIGHT = 64;

    private static final int TIRE_IMAGE_COUNT = 30;

    private static Image[] tires;

    private enum Direction {
        UP,
        DOWN,
        LEFT,
        RIGHT
    }

    public enum State {
        IDLE,
        RUNNING,
        PAUSED
    }

    private final List<RobotStateListener> stateListeners;

    private final Queue<RobotInstruction> commandQueue;

    private Point2D robotPosition;

    private Direction direction;

    private Transition currentTransition;

    private Point2D startPosition;

    private State state;

    private Image robotImage;
    private Image robotBlinkingImage;

    private ImageView background;

    private ImageView tireFL;
    private ImageView tireFR;
    private ImageView tireRL;
    private ImageView tireRR;

    private Timeline timelineFL;
    private Timeline timelineFR;
    private Timeline timelineRL;
    private Timeline timelineRR;

    private double simulationScale;

    private AudioClip simulationMelody;

    public Robot(Image robotImage) {
        if (tires == null) {
            tires = new Image[TIRE_IMAGE_COUNT];
            for (int i = 0; i < TIRE_IMAGE_COUNT; i++) {
                String filePath = String.format("tire%02d.png", i);
                tires[i] = new Image(Objects.requireNonNull(getClass().getResourceAsStream(filePath)));
            }
        }

        this.robotImage = robotImage;
        this.robotBlinkingImage = new Image(Objects.requireNonNull(getClass().getResourceAsStream("robot_blinking.png")));

        robotPosition = new Point2D(0, 0);

        background = new ImageView(robotImage);
        background.setFitWidth(ROBOT_IMAGE_WIDTH);
        background.setFitHeight(ROBOT_IMAGE_HEIGHT);
        background.setTranslateX(ROBOT_GROUP_WIDTH / 2f - ROBOT_IMAGE_WIDTH / 2f);
        background.setTranslateY(ROBOT_GROUP_HEIGHT / 2f - ROBOT_IMAGE_HEIGHT / 2f);

        getChildren().add(background);
        tireFL = new ImageView(tires[0]);
        timelineFL = new Timeline();
        addTire(tireFL, timelineFL, 6, 32);
        tireFR = new ImageView(tires[0]);
        timelineFR = new Timeline();
        addTire(tireFR, timelineFR, ROBOT_GROUP_WIDTH - ROBOT_TIRE_WIDTH - 6, 32);
        tireRL = new ImageView(tires[0]);
        timelineRL = new Timeline();
        addTire(tireRL, timelineRL, 6, ROBOT_GROUP_HEIGHT - ROBOT_TIRE_HEIGHT - 32);
        tireRR = new ImageView(tires[0]);
        timelineRR = new Timeline();
        addTire(tireRR, timelineRR, ROBOT_GROUP_WIDTH - ROBOT_TIRE_WIDTH - 6, ROBOT_GROUP_HEIGHT - ROBOT_TIRE_HEIGHT - 32);

        stateListeners = new LinkedList<>();
        commandQueue = new ConcurrentLinkedQueue<>();
        direction = Direction.UP;
        state = State.IDLE;

        simulationScale = 1d;

        simulationMelody = new AudioClip(Objects.requireNonNull(getClass().getResource("robot_melody.mp3")).toString());
    }

    public void setSimulationScale(double scale, double centerX, double centerY) {
        Animation.Status currentTransitionStatus = null;
        if (currentTransition != null) {
            currentTransitionStatus = currentTransition.getStatus();
            if (currentTransition.getStatus() == Animation.Status.RUNNING) {
                currentTransition.pause();
            }
        }

        this.simulationScale = scale;

        setScaleX(scale);
        setScaleY(scale);

        if (currentTransition != null) {
            currentTransition.setRate(scale);

            if (currentTransitionStatus != null) {
                if (currentTransitionStatus == Animation.Status.RUNNING) {
                    currentTransition.play();
                }
            }
        }
    }

    private void addTire(final ImageView tire, Timeline timeline, double x, double y) {
        getChildren().add(tire);

        timeline.setCycleCount(Timeline.INDEFINITE);
        Duration frameTime = Duration.ZERO;
        for (int i = 0; i < TIRE_IMAGE_COUNT; i++) {
            int finalI = i;
            timeline.getKeyFrames().add(new KeyFrame(frameTime, event -> tire.setImage(tires[finalI])));
            frameTime = frameTime.add(Duration.millis(10));
        }

        tire.setFitWidth(ROBOT_TIRE_WIDTH);
        tire.setFitHeight(ROBOT_TIRE_HEIGHT);

        tire.setLayoutX(x);
        tire.setLayoutY(y);
    }

    public void setXPos(double x) {
        // robotPosition = new Point2D(x, robotPosition.getY());

        setTranslateX(x - ROBOT_GROUP_WIDTH / 2f);
    }

    public void setYPos(double y) {
        // robotPosition = new Point2D(robotPosition.getX(), y);

        setTranslateY(y - ROBOT_GROUP_HEIGHT / 2f);
    }

    public boolean isRunning() {
        return state == State.RUNNING;
    }

    public boolean isPaused() {
        return state == State.PAUSED;
    }

    public boolean isIdle() {
        return state == State.IDLE;
    }

    public void addStateListener(RobotStateListener listener) {
        stateListeners.add(listener);
    }

    public void addCommand(RobotInstruction command) {
        commandQueue.offer(command);
    }

    public void executeCommand(RobotInstruction command) {
        if (command == null) {
            changeState(State.IDLE);
            startPosition = null;
            return;
        }

        switch (command) {
            case FORWARD -> forward();
            case BACKWARDS -> backwards();
            case TURN_LEFT -> turnLeft();
            case TURN_RIGHT -> turnRight();
            case MELODY -> melody();
            case BLINK -> blink();
        }
    }

    private void triggerNextCommand() {
        timelineFL.pause();
        timelineFR.pause();
        timelineRL.pause();
        timelineRR.pause();
        executeCommand(commandQueue.poll());
    }

    private void changeState(State state) {
        this.state = state;
        for (RobotStateListener listener: stateListeners) {
            listener.onStateChange(state);
        }
    }

    public void start() {
        resetPosition();
        changeState(State.RUNNING);
        startPosition = new Point2D(getTranslateX() + ROBOT_GROUP_WIDTH / 2f, getTranslateY() + ROBOT_GROUP_HEIGHT / 2f);
        triggerNextCommand();
    }

    public void stop() {
        timelineFL.pause();
        timelineFR.pause();
        timelineRL.pause();
        timelineRR.pause();
        currentTransition.stop();
        currentTransition = null;
        commandQueue.clear();
        resetPosition();
        startPosition = null;
        changeState(State.IDLE);
    }

    public void pause() {
        if (currentTransition != null) {
            timelineFL.pause();
            timelineFR.pause();
            timelineRL.pause();
            timelineRR.pause();
            currentTransition.pause();
            changeState(State.PAUSED);
        }
    }

    public void resume() {
        if (currentTransition != null) {
            timelineFL.play();
            timelineFR.play();
            timelineRL.play();
            timelineRR.play();
            currentTransition.play();
            changeState(State.RUNNING);
        }
    }

    public void resetPosition() {
        direction = Direction.UP;
        setRotate(0);

        if (startPosition == null) {
            return;
        }

        setXPos(startPosition.getX());
        setYPos(startPosition.getY());
    }

    private void forward() {
        timelineFL.setRate(1);
        timelineFL.play();
        timelineFR.setRate(1);
        timelineFR.play();
        timelineRL.setRate(1);
        timelineRL.play();
        timelineRR.setRate(1);
        timelineRR.play();
        currentTransition = new TranslateTransition(Duration.millis(2000), this);
        currentTransition.setInterpolator(Interpolator.LINEAR);
        currentTransition.setRate(simulationScale);
        switch (direction) {
            case UP -> ((TranslateTransition) currentTransition).setByY(-100);
            case DOWN -> ((TranslateTransition) currentTransition).setByY(100);
            case LEFT -> ((TranslateTransition) currentTransition).setByX(-100);
            case RIGHT -> ((TranslateTransition) currentTransition).setByX(100);
        }
        currentTransition.setOnFinished(event -> triggerNextCommand());
        currentTransition.play();
    }

    private void backwards() {
        timelineFL.setRate(-1);
        timelineFL.play();
        timelineFR.setRate(-1);
        timelineFR.play();
        timelineRL.setRate(-1);
        timelineRL.play();
        timelineRR.setRate(-1);
        timelineRR.play();
        currentTransition = new TranslateTransition(Duration.millis(2000), this);
        currentTransition.setInterpolator(Interpolator.LINEAR);
        currentTransition.setRate(simulationScale);
        switch (direction) {
            case UP -> ((TranslateTransition) currentTransition).setByY(100);
            case DOWN -> ((TranslateTransition) currentTransition).setByY(-100);
            case LEFT -> ((TranslateTransition) currentTransition).setByX(100);
            case RIGHT -> ((TranslateTransition) currentTransition).setByX(-100);
        }
        currentTransition.setOnFinished(event -> triggerNextCommand());
        currentTransition.play();
    }

    private void turnLeft() {
        timelineFL.setRate(-1);
        timelineFL.play();
        timelineFR.setRate(1);
        timelineFR.play();
        timelineRL.setRate(-1);
        timelineRL.play();
        timelineRR.setRate(1);
        timelineRR.play();
        currentTransition = new RotateTransition(Duration.millis(2000), this);
        currentTransition.setInterpolator(Interpolator.LINEAR);
        ((RotateTransition) currentTransition).setByAngle(-90);
        currentTransition.setOnFinished(event -> {
            switch (direction) {
                case UP -> direction = Direction.LEFT;
                case LEFT -> direction = Direction.DOWN;
                case DOWN -> direction = Direction.RIGHT;
                case RIGHT -> direction = Direction.UP;
            }
            triggerNextCommand();
        });
        currentTransition.play();
    }

    private void turnRight() {
        timelineFL.setRate(1);
        timelineFL.play();
        timelineFR.setRate(-1);
        timelineFR.play();
        timelineRL.setRate(1);
        timelineRL.play();
        timelineRR.setRate(-1);
        timelineRR.play();
        currentTransition = new RotateTransition(Duration.millis(2000), this);
        currentTransition.setInterpolator(Interpolator.LINEAR);
        ((RotateTransition) currentTransition).setByAngle(90);
        currentTransition.setOnFinished(event -> {
            switch (direction) {
                case UP -> direction = Direction.RIGHT;
                case RIGHT -> direction = Direction.DOWN;
                case DOWN -> direction = Direction.LEFT;
                case LEFT -> direction = Direction.UP;
            }
            triggerNextCommand();
        });
        currentTransition.play();
    }

    private void melody() {
        simulationMelody.play();
        new Thread(() -> {
            try {
                Thread.sleep(4000);
                Platform.runLater(this::triggerNextCommand);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }).start();
    }

    private void blink() {
        new Thread(() -> {
            try {
                Platform.runLater(() -> background.setImage(robotBlinkingImage));
                Thread.sleep(500);
                Platform.runLater(() -> background.setImage(robotImage));
                Thread.sleep(500);
                Platform.runLater(() -> background.setImage(robotBlinkingImage));
                Thread.sleep(500);
                Platform.runLater(() -> background.setImage(robotImage));
                Thread.sleep(500);
                Platform.runLater(() -> background.setImage(robotBlinkingImage));
                Thread.sleep(500);
                Platform.runLater(() -> background.setImage(robotImage));
                Thread.sleep(500);
                Platform.runLater(this::triggerNextCommand);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }).start();
    }

    public interface RobotStateListener {

        void onStateChange(State newState);

    }

}
