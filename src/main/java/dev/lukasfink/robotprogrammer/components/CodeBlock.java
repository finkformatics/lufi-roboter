package dev.lukasfink.robotprogrammer.components;

import javafx.geometry.Point2D;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;

public class CodeBlock extends StackPane {

    public static final double SIZE_WIDTH = 386;
    public static final double SIZE_HEIGHT = 86.5;
    public static final double SPACING = -15;

    private final String text;

    private final Image image;
    private final ImageView imageView;
    private final Label label;

    private Point2D dragDelta = new Point2D(0, 0);
    private Point2D dragOrigin = new Point2D(0, 0);

    private CodeBlock previous;
    private CodeBlock next;
    private boolean previousAllowed;
    private boolean nextAllowed;

    public CodeBlock(String imgPath, String text, boolean previousAllowed, boolean nextAllowed) {
        this.text = text;
        this.previousAllowed = previousAllowed;
        this.nextAllowed = nextAllowed;

        image = new Image(getClass().getResourceAsStream(imgPath));
        imageView = new ImageView(image);
        imageView.setLayoutX(0);
        imageView.setLayoutY(0);
        imageView.setPreserveRatio(false);
        imageView.setFitWidth(SIZE_WIDTH);
        imageView.setFitHeight(SIZE_HEIGHT);
        imageView.setSmooth(true);

        getChildren().add(imageView);

        label = new Label(text.toUpperCase());
        label.setPrefSize(SIZE_WIDTH, SIZE_HEIGHT);
        label.setStyle("-fx-font-size: 25; -fx-text-fill: GREY");
        label.setAlignment(Pos.CENTER);
        StackPane.setAlignment(label, Pos.CENTER);

        getChildren().add(label);

        setPrefSize(SIZE_WIDTH, SIZE_HEIGHT);
    }

    public String getText() {
        return text;
    }

    public boolean hasPrevious() {
        return previous != null;
    }

    public boolean hasNext() {
        return next != null;
    }

    public boolean isPreviousAllowed() {
        return previousAllowed;
    }

    public boolean isNextAllowed() {
        return nextAllowed;
    }

    public CodeBlock getPrevious() {
        return previous;
    }

    public CodeBlock getNext() {
        return next;
    }

    public void setPrevious(CodeBlock previous) {
        this.previous = previous;
    }

    public void setNext(CodeBlock next) {
        this.next = next;
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
}
