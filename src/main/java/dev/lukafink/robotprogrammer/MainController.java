package dev.lukafink.robotprogrammer;

import dev.lukafink.robotprogrammer.components.CodeBlock;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Cursor;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.TextArea;
import javafx.scene.image.Image;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;

import java.net.URL;
import java.util.LinkedList;
import java.util.List;
import java.util.ResourceBundle;

public class MainController implements Initializable {

    @FXML
    private TextArea codeEditor;

    @FXML
    private Pane simulationParent;

    @FXML
    private Canvas simulationCanvas;

    @FXML
    private Pane graphicalStatements;

    private Image robotImage;

    private List<CodeBlock> codeBlocks;

    private CodeBlock startBlock;

    public MainController() {
        codeBlocks = new LinkedList<>();
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        robotImage = new Image(getClass().getResourceAsStream("balloon_robot.png"));

        startBlock = new CodeBlock("code_block_start.png", "Start", false, true);
        CodeBlock endBlock = new CodeBlock("code_block_end.png", "Ende", true, false);
        CodeBlock statementBlock = new CodeBlock("code_block_stmt.png", "Vorwärts", true, true);

        startBlock.setLayoutX(50);
        startBlock.setLayoutY(400);
        addCodeBlock(startBlock);

        endBlock.setLayoutX(50);
        endBlock.setLayoutY(600);
        addCodeBlock(endBlock);

        addCodeBlock(statementBlock);

        graphicalStatements.getChildren().addAll(startBlock, endBlock, statementBlock);

        simulationCanvas.heightProperty().bind(simulationParent.heightProperty());
        simulationCanvas.widthProperty().bind(simulationParent.widthProperty());

        simulationParent.heightProperty().addListener(observable -> {
            redraw();
        });

        simulationParent.widthProperty().addListener(observable -> {
            redraw();
        });

        redraw();
    }

    private void updateSourceCode() {
        StringBuilder sourceCode = new StringBuilder(startBlock.getText() + "\n");
        CodeBlock currentBlock = startBlock;
        while (currentBlock.hasNext()) {
            currentBlock = currentBlock.getNext();
            sourceCode.append(currentBlock.getText()).append("\n");
        }

        codeEditor.setText(sourceCode.toString());
    }

    private void redraw() {
        GraphicsContext gc = simulationCanvas.getGraphicsContext2D();
        gc.setImageSmoothing(true);
        gc.setFill(Color.rgb(0, 0, 50));
        gc.fillRect(0, 0, simulationCanvas.getWidth(), simulationCanvas.getHeight());

        gc.drawImage(robotImage, 50, 50, 150, 150);
    }

    private void addCodeBlock(CodeBlock codeBlock) {
        codeBlocks.add(codeBlock);
        makeDraggable(codeBlock);
    }

    private void makeDraggable(CodeBlock codeBlockNode) {
        codeBlockNode.setOnMousePressed(mouseEvent -> {
            codeBlockNode.setDragDelta(codeBlockNode.getLayoutX() - mouseEvent.getSceneX(), codeBlockNode.getLayoutY() - mouseEvent.getSceneY());
            codeBlockNode.setDragOrigin(codeBlockNode.getLayoutX(), codeBlockNode.getLayoutY());
            codeBlockNode.getScene().setCursor(Cursor.MOVE);
        });
        codeBlockNode.setOnMouseReleased(mouseEvent -> {
            for (CodeBlock codeBlock: codeBlocks) {
                if (codeBlock == codeBlockNode) {
                    continue;
                }

                if (codeBlockNode.getBoundsInParent().intersects(codeBlock.getBoundsInParent())) {
                    if (codeBlockNode.isPreviousAllowed() && codeBlock.isNextAllowed() && !codeBlockNode.hasPrevious() && !codeBlock.hasNext()) {
                        codeBlockNode.setLayoutX(codeBlock.getLayoutX());
                        codeBlockNode.setLayoutY(codeBlock.getLayoutY() + CodeBlock.SIZE_HEIGHT + CodeBlock.SPACING);
                        codeBlock.setNext(codeBlockNode);
                        codeBlockNode.setPrevious(codeBlock);
                    } else {
                        codeBlockNode.setLayoutX(codeBlockNode.getDragOriginX());
                        codeBlockNode.setLayoutY(codeBlockNode.getDragOriginY());
                    }

                    updateSourceCode();
                }
            }

            // TODO: Remove code block from flow?!

            codeBlockNode.getScene().setCursor(Cursor.HAND);
        });
        codeBlockNode.setOnMouseDragged(mouseEvent -> {
            codeBlockNode.setLayoutX(mouseEvent.getSceneX() + codeBlockNode.getDragDeltaX());
            codeBlockNode.setLayoutY(mouseEvent.getSceneY() + codeBlockNode.getDragDeltaY());
        });
        codeBlockNode.setOnMouseEntered(mouseEvent -> {
            if (!mouseEvent.isPrimaryButtonDown()) {
                codeBlockNode.getScene().setCursor(Cursor.HAND);
            }
        });
        codeBlockNode.setOnMouseExited(mouseEvent -> {
            if (!mouseEvent.isPrimaryButtonDown()) {
                codeBlockNode.getScene().setCursor(Cursor.DEFAULT);
            }
        });
    }

}
