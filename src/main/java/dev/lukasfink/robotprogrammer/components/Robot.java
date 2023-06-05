package dev.lukasfink.robotprogrammer.components;

import dev.lukasfink.robotprogrammer.flow.RobotInstruction;
import javafx.animation.RotateTransition;
import javafx.animation.Transition;
import javafx.animation.TranslateTransition;
import javafx.geometry.Point2D;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.util.Duration;

import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

public class Robot extends ImageView {

    private static final int ROBOT_IMAGE_WIDTH = 150;
    private static final int ROBOT_IMAGE_HEIGHT = 150;

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

    private Direction direction;

    private Transition currentTransition;

    private Point2D startPosition;

    private State state;

    public Robot(Image robotImage) {
        super(robotImage);

        stateListeners = new LinkedList<>();
        commandQueue = new ConcurrentLinkedQueue<>();
        direction = Direction.UP;
        state = State.IDLE;

        setFitWidth(ROBOT_IMAGE_WIDTH);
        setFitHeight(ROBOT_IMAGE_HEIGHT);
    }

    public void setXPos(double x) {
        setTranslateX(x - ROBOT_IMAGE_WIDTH / 2f);
    }

    public void setYPos(double y) {
        setTranslateY(y - ROBOT_IMAGE_HEIGHT / 2f);
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
            return;
        }

        switch (command) {
            case FORWARD -> forward();
            case BACKWARDS -> backwards();
            case TURN_LEFT -> turnLeft();
            case TURN_RIGHT -> turnRight();
        }
    }

    private void triggerNextCommand() {
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
        startPosition = new Point2D(getTranslateX() + ROBOT_IMAGE_WIDTH / 2f, getTranslateY() + ROBOT_IMAGE_HEIGHT / 2f);
        triggerNextCommand();
    }

    public void stop() {
        currentTransition.stop();
        currentTransition = null;
        commandQueue.clear();
        resetPosition();
        changeState(State.IDLE);
    }

    public void pause() {
        if (currentTransition != null) {
            currentTransition.pause();
            changeState(State.PAUSED);
        }
    }

    public void resume() {
        if (currentTransition != null) {
            currentTransition.play();
            changeState(State.RUNNING);
        }
    }

    public void resetPosition() {
        if (startPosition == null) {
            return;
        }

        setXPos(startPosition.getX());
        setYPos(startPosition.getY());
        direction = Direction.UP;
        setRotate(0);
    }

    private void forward() {
        currentTransition = new TranslateTransition(Duration.millis(2000), this);
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
        currentTransition = new TranslateTransition(Duration.millis(2000), this);
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
        currentTransition = new RotateTransition(Duration.millis(2000), this);
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
        currentTransition = new RotateTransition(Duration.millis(2000), this);
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

    public interface RobotStateListener {

        void onStateChange(State newState);

    }

}
