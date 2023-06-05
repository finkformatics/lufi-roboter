package dev.lukasfink.robotprogrammer.components;

import dev.lukasfink.robotprogrammer.flow.FlowCommand;
import dev.lukasfink.robotprogrammer.util.Translator;
import javafx.geometry.Point2D;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.effect.ColorAdjust;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;

import java.util.Objects;

public class CodeBlock extends StackPane {

    public static final double SIZE_WIDTH = 386;
    public static final double SIZE_HEIGHT = 86.5;
    public static final double SPACING = -15;

    private static final Color COLOR_COMPLETE = Color.rgb(0, 192, 15);
    private static final Color COLOR_INCOMPLETE = Color.rgb(200, 150, 0);
    private static final Color COLOR_WITHOUT_CONNECTIONS = Color.rgb(200, 80, 30);

    private final FlowCommand flowCommand;

    private final ImageView imageView;

    private Point2D dragDelta = new Point2D(0, 0);
    private Point2D dragOrigin = new Point2D(0, 0);

    private boolean hovered;

    private final String imgPath;

    public CodeBlock(String imgPath, FlowCommand flowCommand) {
        this.imgPath = imgPath;
        this.flowCommand = flowCommand;

        Image image = new Image(Objects.requireNonNull(getClass().getResourceAsStream(imgPath)));
        imageView = new ImageView(image);
        imageView.setLayoutX(0);
        imageView.setLayoutY(0);
        imageView.setPreserveRatio(false);
        imageView.setFitWidth(SIZE_WIDTH);
        imageView.setFitHeight(SIZE_HEIGHT);
        imageView.setSmooth(true);

        getChildren().add(imageView);

        Label label = new Label(Translator.translate(flowCommand.getInstructionText()).toUpperCase());
        label.setPrefSize(SIZE_WIDTH, SIZE_HEIGHT);
        label.setStyle("-fx-font-size: 25; -fx-text-fill: GREY");
        label.setAlignment(Pos.CENTER);
        StackPane.setAlignment(label, Pos.CENTER);

        getChildren().add(label);

        setPrefSize(SIZE_WIDTH, SIZE_HEIGHT);
        updateLook();
    }

    public String getImgPath() {
        return imgPath;
    }

    public void updateLook() {
        Color newColor = switch (flowCommand.getState()) {
            case COMPLETE -> COLOR_COMPLETE;
            case INCOMPLETE -> COLOR_INCOMPLETE;
            default -> COLOR_WITHOUT_CONNECTIONS;
        };

        if (hovered) {
            newColor = newColor.darker();
        }

        ColorAdjust colorAdjust = new ColorAdjust();
        colorAdjust.setHue(map((newColor.getHue() + 180) % 360, 0, 360, -1, 1));
        colorAdjust.setBrightness(map(newColor.getBrightness(), 0, 1, -1, 1));
        colorAdjust.setSaturation(newColor.getSaturation());
        imageView.setEffect(colorAdjust);
    }

    public FlowCommand getFlowCommand() {
        return flowCommand;
    }

    public void setHovered(boolean hovered) {
        this.hovered = hovered;

        updateLook();
    }

    public void setDragDelta(double x, double y) {
        dragDelta = new Point2D(x, y);
    }

    public void setDragOrigin(double x, double y) {
        dragOrigin = new Point2D(x, y);
    }

    public double getDragOriginX() {
        return dragOrigin.getX();
    }

    public double getDragOriginY() {
        return dragOrigin.getY();
    }

    public double getDragDeltaX() {
        return dragDelta.getX();
    }

    public double getDragDeltaY() {
        return dragDelta.getY();
    }

    private static double map(double value, double start, double stop, double targetStart, double targetStop) {
        return targetStart + (targetStop - targetStart) * ((value - start) / (stop - start));
    }

}
