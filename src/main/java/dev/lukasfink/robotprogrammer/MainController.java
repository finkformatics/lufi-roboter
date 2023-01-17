package dev.lukasfink.robotprogrammer;

import dev.lukasfink.robotprogrammer.components.CodeBlock;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Cursor;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.TextArea;
import javafx.scene.image.Image;
import javafx.scene.layout.Pane;
import javafx.scene.media.AudioClip;
import javafx.scene.paint.Color;

import java.net.URL;
import java.util.HashSet;
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

    private final List<CodeBlock> codeBlocks;

    private CodeBlock startBlock;

    private final AudioClip selectClip;
    private final AudioClip dropClip;

    public MainController() {
        codeBlocks = new LinkedList<>();

        selectClip = new AudioClip(getClass().getResource("select.wav").toString());
        dropClip = new AudioClip(getClass().getResource("drop.wav").toString());
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        robotImage = new Image(getClass().getResourceAsStream("balloon_robot.png"));

        startBlock = new CodeBlock("code_block_start.png", "Start", false, true);
        CodeBlock endBlock = new CodeBlock("code_block_end.png", "Ende", true, false);
        CodeBlock statementBlock = new CodeBlock("code_block_stmt.png", "Vorwärts", true, true);

        startBlock.setLayoutX(50);
        startBlock.setLayoutY(400);
        startBlock.setState(CodeBlock.State.INCOMPLETE);
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

    private void updateStates() {
        for (CodeBlock codeBlock: codeBlocks) {
            CodeBlock currentBlock = codeBlock;
            boolean connectsToStart = currentBlock == startBlock;
            if (!connectsToStart) {
                while (currentBlock.hasPrevious()) {
                    currentBlock = currentBlock.getPrevious();
                }

                if (currentBlock == startBlock) {
                    connectsToStart = true;
                }
            }

            currentBlock = codeBlock;
            while (currentBlock.hasNext()) {
                currentBlock = currentBlock.getNext();
            }

            boolean connectsToEnd = currentBlock.getText().equals("Ende");

            if (connectsToStart && connectsToEnd) {
                codeBlock.setState(CodeBlock.State.COMPLETE);
            } else if (connectsToStart) {
                codeBlock.setState(CodeBlock.State.INCOMPLETE);
            } else {
                codeBlock.setState(CodeBlock.State.WITHOUT_CONNECTIONS);
            }
        }
    }

    private void makeDraggable(CodeBlock codeBlockNode) {
        codeBlockNode.setOnMousePressed(mouseEvent -> {
            selectClip.play();
            codeBlockNode.setDragDelta(codeBlockNode.getLayoutX() - mouseEvent.getSceneX(), codeBlockNode.getLayoutY() - mouseEvent.getSceneY());
            codeBlockNode.setDragOrigin(codeBlockNode.getLayoutX(), codeBlockNode.getLayoutY());
            codeBlockNode.getScene().setCursor(Cursor.MOVE);
        });
        codeBlockNode.setOnMouseReleased(mouseEvent -> {
            dropClip.play();
            boolean intersects = false;

            HashSet<CodeBlock> blockedBlocks = new HashSet<>();
            blockedBlocks.add(codeBlockNode);
            CodeBlock referenceBlock = codeBlockNode;
            if (codeBlockNode == startBlock) {
                while (referenceBlock.hasNext()) {
                    referenceBlock = referenceBlock.getNext();
                    blockedBlocks.add(referenceBlock);
                }
            }

            for (CodeBlock codeBlock: codeBlocks) {
                if (blockedBlocks.contains(codeBlock)) {
                    continue;
                }

                if (referenceBlock.getBoundsInParent().intersects(codeBlock.getBoundsInParent())) {
                    intersects = true;
                    if (codeBlockNode != startBlock && referenceBlock.getLayoutY() > codeBlock.getLayoutY() && referenceBlock.isPreviousAllowed() && codeBlock.isNextAllowed() && !referenceBlock.hasPrevious() && !codeBlock.hasNext()) {
                        referenceBlock.setLayoutX(codeBlock.getLayoutX());
                        referenceBlock.setLayoutY(codeBlock.getLayoutY() + CodeBlock.SIZE_HEIGHT + CodeBlock.SPACING);
                        codeBlock.setNext(referenceBlock);
                        referenceBlock.setPrevious(codeBlock);
                    } else if (referenceBlock.isNextAllowed() && codeBlock.isPreviousAllowed() && !referenceBlock.hasNext() && !codeBlock.hasPrevious()) {
                        referenceBlock.setLayoutX(codeBlock.getLayoutX());
                        referenceBlock.setLayoutY(codeBlock.getLayoutY() - CodeBlock.SIZE_HEIGHT - CodeBlock.SPACING);
                        codeBlock.setPrevious(referenceBlock);
                        referenceBlock.setNext(codeBlock);

                        if (codeBlockNode == startBlock) {
                            int distanceCounter = 0;
                            CodeBlock currentBlock = referenceBlock;
                            while (currentBlock.hasPrevious()) {
                                currentBlock = currentBlock.getPrevious();
                                distanceCounter++;
                                currentBlock.setLayoutX(referenceBlock.getLayoutX());
                                currentBlock.setLayoutY(referenceBlock.getLayoutY() - distanceCounter * (CodeBlock.SIZE_HEIGHT + CodeBlock.SPACING));
                            }
                        }
                    } else {
                        codeBlockNode.setLayoutX(codeBlockNode.getDragOriginX());
                        codeBlockNode.setLayoutY(codeBlockNode.getDragOriginY());
                    }

                    updateSourceCode();
                }
            }

            if (!intersects && codeBlockNode != startBlock) {
                if (codeBlockNode.hasNext()) {
                    CodeBlock next = codeBlockNode.getNext();
                    next.setPrevious(null);
                    codeBlockNode.setNext(null);
                }

                if (codeBlockNode.hasPrevious()) {
                    CodeBlock previous = codeBlockNode.getPrevious();
                    previous.setNext(null);
                    codeBlockNode.setPrevious(null);
                }
            } else if (codeBlockNode != startBlock) {
                if (codeBlockNode.hasNext() && !codeBlockNode.getBoundsInParent().intersects(codeBlockNode.getNext().getBoundsInParent())) {
                    CodeBlock next = codeBlockNode.getNext();
                    next.setPrevious(null);
                    codeBlockNode.setNext(null);
                }

                if (codeBlockNode.hasPrevious() && !codeBlockNode.getBoundsInParent().intersects(codeBlockNode.getPrevious().getBoundsInParent())) {
                    CodeBlock previous = codeBlockNode.getPrevious();
                    previous.setNext(null);
                    codeBlockNode.setPrevious(null);
                }
            }

            updateStates();

            codeBlockNode.getScene().setCursor(Cursor.HAND);
        });
        codeBlockNode.setOnMouseDragged(mouseEvent -> {
            codeBlockNode.setLayoutX(mouseEvent.getSceneX() + codeBlockNode.getDragDeltaX());
            codeBlockNode.setLayoutY(mouseEvent.getSceneY() + codeBlockNode.getDragDeltaY());

            if (codeBlockNode == startBlock) {
                CodeBlock currentBlock = codeBlockNode;
                int distanceCounter = 0;
                while (currentBlock.hasNext()) {
                    currentBlock = currentBlock.getNext();
                    distanceCounter++;
                    currentBlock.setLayoutY(codeBlockNode.getLayoutY() + distanceCounter * (CodeBlock.SIZE_HEIGHT + CodeBlock.SPACING));
                    currentBlock.setLayoutX(codeBlockNode.getLayoutX());
                }
            }
        });
        codeBlockNode.setOnMouseEntered(mouseEvent -> {
            if (!mouseEvent.isPrimaryButtonDown()) {
                codeBlockNode.getScene().setCursor(Cursor.HAND);
                codeBlockNode.setHovered(true);
            }
        });
        codeBlockNode.setOnMouseExited(mouseEvent -> {
            if (!mouseEvent.isPrimaryButtonDown()) {
                codeBlockNode.getScene().setCursor(Cursor.DEFAULT);
                codeBlockNode.setHovered(false);
            }
        });
    }

}
