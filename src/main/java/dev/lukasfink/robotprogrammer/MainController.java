package dev.lukasfink.robotprogrammer;

import dev.lukasfink.robotprogrammer.components.CodeBlock;
import dev.lukasfink.robotprogrammer.flow.Flow;
import dev.lukasfink.robotprogrammer.flow.FlowCommand;
import dev.lukasfink.robotprogrammer.flow.RobotInstruction;
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
import java.util.*;

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

    private final HashMap<FlowCommand, CodeBlock> codeBlockMap;

    private Flow flow;

    private final AudioClip selectClip;
    private final AudioClip dropClip;

    public MainController() {
        flow = new Flow();
        codeBlockMap = new HashMap<>();

        selectClip = new AudioClip(getClass().getResource("select.wav").toString());
        dropClip = new AudioClip(getClass().getResource("drop.wav").toString());
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        robotImage = new Image(getClass().getResourceAsStream("balloon_robot.png"));

        addCodeBlock(new CodeBlock("code_block_start.png", flow.getStartCommand()));
        addCodeBlock(new CodeBlock("code_block_stmt.png", new FlowCommand(RobotInstruction.FORWARD)));
        addCodeBlock(new CodeBlock("code_block_stmt.png", new FlowCommand(RobotInstruction.BACKWARDS)));
        addCodeBlock(new CodeBlock("code_block_stmt.png", new FlowCommand(RobotInstruction.TURN_RIGHT)));
        addCodeBlock(new CodeBlock("code_block_stmt.png", new FlowCommand(RobotInstruction.TURN_LEFT)));
        addCodeBlock(new CodeBlock("code_block_end.png", new FlowCommand(RobotInstruction.TERMINATE)));

        simulationCanvas.heightProperty().bind(simulationParent.heightProperty());
        simulationCanvas.widthProperty().bind(simulationParent.widthProperty());

        simulationParent.heightProperty().addListener(observable -> {
            redraw();
        });

        simulationParent.widthProperty().addListener(observable -> {
            redraw();
        });

        redraw();
        flow.updateStates();
        updateLooks();
    }

    private void updateSourceCode() {
        codeEditor.setText(flow.generateSourceCode());
    }

    private void redraw() {
        GraphicsContext gc = simulationCanvas.getGraphicsContext2D();
        gc.setImageSmoothing(true);
        gc.setFill(Color.rgb(0, 0, 50));
        gc.fillRect(0, 0, simulationCanvas.getWidth(), simulationCanvas.getHeight());

        gc.drawImage(robotImage, 50, 50, 150, 150);
    }

    private void addCodeBlock(CodeBlock codeBlock) {
        codeBlockMap.put(codeBlock.getFlowCommand(), codeBlock);
        flow.addCommand(codeBlock.getFlowCommand());
        makeDraggable(codeBlock);
        graphicalStatements.getChildren().add(codeBlock);
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
            boolean isStartBlock = false;
            if (codeBlockNode.getFlowCommand() == flow.getStartCommand()) {
                isStartBlock = true;
                FlowCommand currentCommand = flow.getStartCommand();
                while (currentCommand.hasNext()) {
                    currentCommand = currentCommand.getNext();
                    referenceBlock = codeBlockMap.get(currentCommand);
                    blockedBlocks.add(referenceBlock);
                }
            }

            for (CodeBlock codeBlock: codeBlockMap.values()) {
                if (blockedBlocks.contains(codeBlock)) {
                    continue;
                }

                if (referenceBlock.getBoundsInParent().intersects(codeBlock.getBoundsInParent())) {
                    intersects = true;
                    if (codeBlockNode.getFlowCommand() != flow.getStartCommand() && referenceBlock.getLayoutY() > codeBlock.getLayoutY() && referenceBlock.getFlowCommand().getInstruction().isPreviousAllowed() && codeBlock.getFlowCommand().getInstruction().isNextAllowed() && !referenceBlock.getFlowCommand().hasPrevious() && !codeBlock.getFlowCommand().hasNext()) {
                        referenceBlock.setLayoutX(codeBlock.getLayoutX());
                        referenceBlock.setLayoutY(codeBlock.getLayoutY() + CodeBlock.SIZE_HEIGHT + CodeBlock.SPACING);
                        codeBlock.getFlowCommand().setNext(referenceBlock.getFlowCommand());
                        referenceBlock.getFlowCommand().setPrevious(codeBlock.getFlowCommand());
                    } else if (referenceBlock.getFlowCommand().getInstruction().isNextAllowed() && codeBlock.getFlowCommand().getInstruction().isPreviousAllowed() && !referenceBlock.getFlowCommand().hasNext() && !codeBlock.getFlowCommand().hasPrevious()) {
                        referenceBlock.setLayoutX(codeBlock.getLayoutX());
                        referenceBlock.setLayoutY(codeBlock.getLayoutY() - CodeBlock.SIZE_HEIGHT - CodeBlock.SPACING);
                        codeBlock.getFlowCommand().setPrevious(referenceBlock.getFlowCommand());
                        referenceBlock.getFlowCommand().setNext(codeBlock.getFlowCommand());

                        if (codeBlockNode.getFlowCommand() == flow.getStartCommand()) {
                            int distanceCounter = 0;
                            CodeBlock currentBlock = referenceBlock;
                            FlowCommand currentCommand = referenceBlock.getFlowCommand();
                            while (currentCommand.hasPrevious()) {
                                currentCommand = currentCommand.getPrevious();
                                currentBlock = codeBlockMap.get(currentCommand);
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

            if (!intersects && codeBlockNode.getFlowCommand() != flow.getStartCommand()) {
                if (codeBlockNode.getFlowCommand().hasNext()) {
                    FlowCommand next = codeBlockNode.getFlowCommand().getNext();
                    next.setPrevious(null);
                    codeBlockNode.getFlowCommand().setNext(null);
                }

                if (codeBlockNode.getFlowCommand().hasPrevious()) {
                    FlowCommand previous = codeBlockNode.getFlowCommand().getPrevious();
                    previous.setNext(null);
                    codeBlockNode.getFlowCommand().setPrevious(null);
                }
            } else if (codeBlockNode.getFlowCommand() != flow.getStartCommand()) {
                if (codeBlockNode.getFlowCommand().hasNext() && !codeBlockNode.getBoundsInParent().intersects(codeBlockMap.get(codeBlockNode.getFlowCommand().getNext()).getBoundsInParent())) {
                    FlowCommand next = codeBlockNode.getFlowCommand().getNext();
                    next.setPrevious(null);
                    codeBlockNode.getFlowCommand().setNext(null);
                }

                if (codeBlockNode.getFlowCommand().hasPrevious() && !codeBlockNode.getBoundsInParent().intersects(codeBlockMap.get(codeBlockNode.getFlowCommand().getPrevious()).getBoundsInParent())) {
                    FlowCommand previous = codeBlockNode.getFlowCommand().getPrevious();
                    previous.setNext(null);
                    codeBlockNode.getFlowCommand().setPrevious(null);
                }
            }

            flow.updateStates();
            updateLooks();

            codeBlockNode.getScene().setCursor(Cursor.HAND);
        });
        codeBlockNode.setOnMouseDragged(mouseEvent -> {
            codeBlockNode.setLayoutX(mouseEvent.getSceneX() + codeBlockNode.getDragDeltaX());
            codeBlockNode.setLayoutY(mouseEvent.getSceneY() + codeBlockNode.getDragDeltaY());

            if (codeBlockNode.getFlowCommand() == flow.getStartCommand()) {
                CodeBlock currentBlock = codeBlockNode;
                int distanceCounter = 0;
                while (currentBlock.getFlowCommand().hasNext()) {
                    currentBlock = codeBlockMap.get(currentBlock.getFlowCommand().getNext());
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

    private void updateLooks() {
        for (CodeBlock codeBlock: codeBlockMap.values()) {
            codeBlock.updateLook();
        }
    }

}
