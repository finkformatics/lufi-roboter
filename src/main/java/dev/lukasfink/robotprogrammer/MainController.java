package dev.lukasfink.robotprogrammer;

import dev.lukasfink.robotprogrammer.components.CodeBlock;
import dev.lukasfink.robotprogrammer.components.Robot;
import dev.lukasfink.robotprogrammer.components.TransferDialogController;
import dev.lukasfink.robotprogrammer.flow.Flow;
import dev.lukasfink.robotprogrammer.flow.FlowCommand;
import dev.lukasfink.robotprogrammer.flow.RobotInstruction;
import dev.lukasfink.robotprogrammer.io.ExportedCodeBlock;
import dev.lukasfink.robotprogrammer.io.IOHelper;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Point2D;
import javafx.scene.Cursor;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.media.AudioClip;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.kordamp.ikonli.javafx.FontIcon;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.*;

public class MainController implements Initializable {

    private static final int GRID_GAP = 50;

    @FXML
    private TextArea codeEditor;

    @FXML
    private Pane simulationParent;

    @FXML
    private Canvas simulationCanvas;

    @FXML
    private StackPane statementsStack;

    @FXML
    private Canvas gridCanvas;

    @FXML
    private AnchorPane graphicalStatements;

    @FXML
    private Button zoomOutButton;

    @FXML
    private Button zoomInButton;

    @FXML
    private ToggleButton playButton;

    @FXML
    private Button stopButton;

    @FXML
    private MenuItem newMenuItem;

    @FXML
    private MenuItem openMenuItem;

    @FXML
    private MenuItem saveMenuItem;

    @FXML
    private MenuItem exitMenuItem;

    @FXML
    private MenuItem noMazeMenuItem;

    @FXML
    private MenuItem maze1MenuItem;

    @FXML
    private MenuItem maze2MenuItem;

    @FXML
    private MenuItem maze3MenuItem;

    @FXML
    private MenuItem transferMenuItem;

    @FXML
    private MenuItem aboutMenuItem;

    private static final HashMap<String, String> instructionImageMap = new HashMap<>();
    static {
        instructionImageMap.put("forward", "code_block_stmt.png");
        instructionImageMap.put("backwards", "code_block_stmt.png");
        instructionImageMap.put("turn_left", "code_block_stmt.png");
        instructionImageMap.put("turn_right", "code_block_stmt.png");
        instructionImageMap.put("terminate", "code_block_end.png");
        instructionImageMap.put("init", "code_block_start.png");
        instructionImageMap.put("melody", "code_block_stmt.png");
        instructionImageMap.put("blink", "code_block_stmt.png");
    }

    private final HashMap<FlowCommand, CodeBlock> codeBlockMap;

    private final Flow flow;

    private final AudioClip selectClip;
    private final AudioClip dropClip;

    private CodeBlock newBlock;

    private FontIcon trashArea;

    private FontIcon zoomOutIcon;
    private FontIcon zoomInIcon;
    private FontIcon playIcon;
    private FontIcon pauseIcon;

    private final Robot robot;

    private Point2D gridOffset;
    private Point2D gridDragStart;

    private int templateBlocksMaxX;
    private int templateBlocksMaxY;

    private int mazeNumber;

    private double simulationScale;

    public MainController() {
        flow = new Flow();
        codeBlockMap = new HashMap<>();
        robot = new Robot(new Image(Objects.requireNonNull(getClass().getResourceAsStream("balloon_robot.png"))));

        selectClip = new AudioClip(Objects.requireNonNull(getClass().getResource("select.wav")).toString());
        dropClip = new AudioClip(Objects.requireNonNull(getClass().getResource("drop.wav")).toString());

        gridOffset = new Point2D(0, 0);

        simulationScale = 1.0;
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        CodeBlock[] templateBlocks = new CodeBlock[]{
                new CodeBlock(instructionImageMap.get(RobotInstruction.FORWARD.getValue()), new FlowCommand(RobotInstruction.FORWARD)),
                new CodeBlock(instructionImageMap.get(RobotInstruction.BACKWARDS.getValue()), new FlowCommand(RobotInstruction.BACKWARDS)),
                new CodeBlock(instructionImageMap.get(RobotInstruction.TURN_RIGHT.getValue()), new FlowCommand(RobotInstruction.TURN_RIGHT)),
                new CodeBlock(instructionImageMap.get(RobotInstruction.TURN_LEFT.getValue()), new FlowCommand(RobotInstruction.TURN_LEFT)),
                new CodeBlock(instructionImageMap.get(RobotInstruction.MELODY.getValue()), new FlowCommand(RobotInstruction.MELODY)),
                new CodeBlock(instructionImageMap.get(RobotInstruction.BLINK.getValue()), new FlowCommand(RobotInstruction.BLINK)),
                new CodeBlock(instructionImageMap.get(RobotInstruction.TERMINATE.getValue()), new FlowCommand(RobotInstruction.TERMINATE)),
        };

        for (int i = 0; i < templateBlocks.length; i++) {
            templateBlocks[i].setLayoutX(10);
            templateBlocks[i].setLayoutY(10 + i * (CodeBlock.SIZE_HEIGHT - CodeBlock.SPACING));
            configureTemplateBlock(templateBlocks[i]);
        }

        templateBlocksMaxX = 10 + (int) CodeBlock.SIZE_WIDTH + 10;
        templateBlocksMaxY = 10 + templateBlocks.length * (int) (CodeBlock.SIZE_HEIGHT - CodeBlock.SPACING);

        CodeBlock startBlock = addCodeBlock(new CodeBlock(instructionImageMap.get(RobotInstruction.INIT.getValue()), flow.getStartCommand()));
        startBlock.setLayoutX(templateBlocksMaxX + 80);
        startBlock.setLayoutY(50);

        graphicalStatements.getChildren().addAll(templateBlocks);

        trashArea = new FontIcon("mdral-delete:256:RED");
        graphicalStatements.getChildren().add(trashArea);
        AnchorPane.setTopAnchor(trashArea, templateBlocksMaxY + 20.0);
        AnchorPane.setLeftAnchor(trashArea, 0.0);

        graphicalStatements.setOnMousePressed(event -> {
            if (event.isMiddleButtonDown()) {
                gridDragStart = new Point2D(event.getX(), event.getY());
            }
        });

        graphicalStatements.setOnMouseDragged(event -> {
            if (event.isMiddleButtonDown()) {
                Point2D diff = new Point2D(event.getX() - gridDragStart.getX(), event.getY() - gridDragStart.getY());
                gridOffset = new Point2D(gridOffset.getX() + diff.getX(), gridOffset.getY() + diff.getY());
                for (CodeBlock codeBlock: codeBlockMap.values()) {
                    codeBlock.setLayoutX(codeBlock.getLayoutX() + diff.getX());
                    codeBlock.setLayoutY(codeBlock.getLayoutY() + diff.getY());
                }
                redraw();
                gridDragStart = new Point2D(event.getX(), event.getY());
            }
        });

        simulationCanvas.heightProperty().bind(simulationParent.heightProperty());
        simulationCanvas.widthProperty().bind(simulationParent.widthProperty());

        simulationParent.heightProperty().addListener(observable -> {
            redraw();
        });

        simulationParent.widthProperty().addListener(observable -> {
            redraw();
        });

        simulationParent.getChildren().add(robot);

        gridCanvas.heightProperty().bind(statementsStack.heightProperty());
        gridCanvas.widthProperty().bind(statementsStack.widthProperty());

        redraw();
        flow.updateStates();
        updateLooks();

        robot.setVisible(false);

        zoomOutIcon = new FontIcon("mdmz-zoom_out:56:BLACK");
        zoomInIcon = new FontIcon("mdmz-zoom_in:56:BLACK");
        playIcon = new FontIcon("mdmz-play_arrow:56:GREEN");
        pauseIcon = new FontIcon("mdomz-pause:56:GREY");
        FontIcon stopIcon = new FontIcon("mdrmz-stop:56:RED");
        playButton.setGraphic(playIcon);
        playButton.setDisable(true);
        playButton.setOnAction(event -> {
            if (playButton.isSelected()) {
                if (robot.isIdle()) {
                    simulateCommands();
                    robot.setXPos(simulationCanvas.getWidth() / 2);
                    robot.setYPos(simulationCanvas.getHeight() / 2);
                    robot.start();
                } else if (robot.isPaused()) {
                    robot.resume();
                }
            } else {
                if (robot.isRunning()) {
                    robot.pause();
                }
            }
        });

        stopButton.setGraphic(stopIcon);
        stopButton.setOnAction(event -> {
            robot.stop();
        });
        stopButton.setDisable(true);

        zoomOutButton.setGraphic(zoomOutIcon);
        zoomOutButton.setOnAction(event -> {
            simulationScale *= 0.9;
            robot.setSimulationScale(simulationScale, simulationCanvas.getWidth() / 2, simulationCanvas.getHeight() / 2);
            redraw();
        });

        zoomInButton.setGraphic(zoomInIcon);
        zoomInButton.setOnAction(event -> {
            simulationScale *= 1.1;
            robot.setSimulationScale(simulationScale, simulationCanvas.getWidth() / 2, simulationCanvas.getHeight() / 2);
            redraw();
        });

        robot.addStateListener(newState -> {
            switch (newState) {
                case IDLE -> {
                    playButton.setSelected(false);
                    playButton.setGraphic(playIcon);
                    stopButton.setDisable(true);
                }
                case PAUSED -> {
                    playButton.setSelected(false);
                    playButton.setGraphic(playIcon);
                    stopButton.setDisable(false);
                }
                case RUNNING -> {
                    playButton.setSelected(true);
                    playButton.setGraphic(pauseIcon);
                    stopButton.setDisable(false);
                }
            }
        });

        exitMenuItem.setOnAction(event -> Platform.exit());

        newMenuItem.setOnAction(event -> {
            reset(true);
        });

        openMenuItem.setOnAction(event -> {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setInitialDirectory(new File(System.getProperty("user.home")));
            fileChooser.setTitle("Roboteranweisungen laden");
            fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Roboteranweisungen", "*.rbt"));
            File chosenFile = fileChooser.showOpenDialog(graphicalStatements.getScene().getWindow());
            if (chosenFile != null) {
                try {
                    String json = Files.readString(chosenFile.toPath(), StandardCharsets.UTF_8);
                    List<ExportedCodeBlock> exportedCodeBlocks = IOHelper.translateToStatements(json);
                    reset(false);
                    for (ExportedCodeBlock exportedCodeBlock: exportedCodeBlocks) {
                        newBlock = addCodeBlock(
                                new CodeBlock(
                                        instructionImageMap.get(exportedCodeBlock.getInstruction()),
                                        RobotInstruction.INIT.getValue().equals(exportedCodeBlock.getInstruction()) ? flow.getStartCommand() : new FlowCommand(RobotInstruction.byValue(exportedCodeBlock.getInstruction()))
                                )
                        );
                        newBlock.setLayoutX(exportedCodeBlock.getPosX());
                        newBlock.setLayoutY(exportedCodeBlock.getPosY());
                        checkIntersection(newBlock);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });

        saveMenuItem.setOnAction(event -> {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setInitialDirectory(new File(System.getProperty("user.home")));
            fileChooser.setInitialFileName("anweisungen.rbt");
            fileChooser.setTitle("Roboteranweisungen speichern");
            fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Roboteranweisungen", "*.rbt"));
            File chosenFile = fileChooser.showSaveDialog(graphicalStatements.getScene().getWindow());
            if (chosenFile != null) {
                String json = IOHelper.translateToJson(codeBlockMap);
                try {
                    Files.writeString(chosenFile.toPath(), json, StandardCharsets.UTF_8, StandardOpenOption.WRITE, StandardOpenOption.CREATE);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });

        noMazeMenuItem.setOnAction(event -> {
            mazeNumber = 0;
            redraw();
        });

        maze1MenuItem.setOnAction(event -> {
            mazeNumber = 1;
            redraw();
        });

        maze2MenuItem.setOnAction(event -> {
            mazeNumber = 2;
            redraw();
        });

        maze3MenuItem.setOnAction(event -> {
            mazeNumber = 3;
            redraw();
        });

        transferMenuItem.setOnAction(event -> openTransferDialog());

        aboutMenuItem.setOnAction(event -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Über");
            alert.setHeaderText("Robot Programmer");
            alert.setContentText("Version " + App.VERSION + ", erstellt von Lukas Fink");
            alert.showAndWait();
        });

        new Thread(() -> {
            try {
                Thread.sleep(500);
                robot.setXPos(simulationCanvas.getWidth() / 2);
                robot.setYPos(simulationCanvas.getHeight() / 2);
                robot.setVisible(true);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }).start();
    }

    void openTransferDialog() {
        FXMLLoader fxmlLoader = new FXMLLoader(TransferDialogController.class.getResource("transfer-dialog.fxml"));
        try {
            Parent parent = fxmlLoader.load();
            TransferDialogController dialogController = fxmlLoader.getController();
            dialogController.setFlow(flow);

            Scene scene = new Scene(parent, 300, 200);
            Stage stage = new Stage();
            stage.setTitle("Programm transferieren");
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setScene(scene);
            stage.showAndWait();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void reset(boolean createStartBlock) {
        for (CodeBlock codeBlock: codeBlockMap.values()) {
            graphicalStatements.getChildren().remove(codeBlock);
        }

        codeBlockMap.clear();
        flow.reset();
        gridOffset = new Point2D(0, 0);
        redraw();

        if (createStartBlock) {
            CodeBlock startBlock = addCodeBlock(new CodeBlock(instructionImageMap.get(RobotInstruction.INIT.getValue()), flow.getStartCommand()));
            startBlock.setLayoutX(templateBlocksMaxX + 150);
            startBlock.setLayoutY(50);
        }

        flow.updateStates();
        updateLooks();
    }

    private void updateSourceCode() {
        codeEditor.setText(flow.generateSourceCode());
    }

    private void simulateCommands() {
        FlowCommand currentCommand = flow.getStartCommand();
        while (currentCommand.hasNext()) {
            currentCommand = currentCommand.getNext();
            switch (currentCommand.getInstruction()) {
                case FORWARD, BACKWARDS, TURN_LEFT, TURN_RIGHT, MELODY, BLINK -> robot.addCommand(currentCommand.getInstruction());
            }
        }
    }

    private void drawMaze1(GraphicsContext gc) {
        gc.setFill(Color.YELLOW);
        gc.fillRect(simulationCanvas.getWidth() / 2 - 10, simulationCanvas.getHeight() / 2 - 210, 20, 220);
        gc.fillRect(simulationCanvas.getWidth() / 2, simulationCanvas.getHeight() / 2 - 210, 210, 20);
        gc.fillRect(simulationCanvas.getWidth() / 2 - 10 + 200, simulationCanvas.getHeight() / 2 - 200, 20, 210);
        gc.fillRect(simulationCanvas.getWidth() / 2 + 100, simulationCanvas.getHeight() / 2 - 10, 110, 20);
    }

    private void drawMaze2(GraphicsContext gc) {
        drawMaze1(gc);
        gc.fillRect(simulationCanvas.getWidth() / 2 + 100 - 10, simulationCanvas.getHeight() / 2 - 10, 20, 210);
        gc.fillRect(simulationCanvas.getWidth() / 2 - 100, simulationCanvas.getHeight() / 2 + 200 - 10, 210, 20);
        gc.fillRect(simulationCanvas.getWidth() / 2 - 100 - 10, simulationCanvas.getHeight() / 2 + 100, 20, 110);
        gc.fillRect(simulationCanvas.getWidth() / 2 - 300, simulationCanvas.getHeight() / 2 + 100 - 10, 210, 20);
    }

    private void drawMaze3(GraphicsContext gc) {
        drawMaze2(gc);
        gc.fillRect(simulationCanvas.getWidth() / 2 - 300 - 10, simulationCanvas.getHeight() / 2 - 200, 20, 310);
        gc.fillRect(simulationCanvas.getWidth() / 2 - 300 - 10, simulationCanvas.getHeight() / 2 - 200 - 10, 110, 20);
        gc.fillRect(simulationCanvas.getWidth() / 2 - 200 - 10, simulationCanvas.getHeight() / 2 - 200 - 10, 20, 110);
        gc.fillRect(simulationCanvas.getWidth() / 2 - 200 - 10, simulationCanvas.getHeight() / 2 - 100 - 10, 110, 20);
        gc.fillRect(simulationCanvas.getWidth() / 2 - 100 - 10, simulationCanvas.getHeight() / 2 - 200 - 10, 20, 120);
    }

    private void redraw() {
        GraphicsContext gc = simulationCanvas.getGraphicsContext2D();
        gc.setImageSmoothing(true);
        gc.setFill(Color.rgb(0, 0, 50));
        gc.fillRect(0, 0, simulationCanvas.getWidth(), simulationCanvas.getHeight());
        gc.setStroke(Color.gray(0.3));
        gc.strokeLine(simulationCanvas.getWidth() / 2, 0, simulationCanvas.getWidth() / 2, simulationCanvas.getHeight());
        for (double x = GRID_GAP * simulationScale; x < simulationCanvas.getWidth() / 2; x += GRID_GAP * simulationScale) {
            gc.strokeLine(simulationCanvas.getWidth() / 2 - x, 0, simulationCanvas.getWidth() / 2 - x, simulationCanvas.getHeight());
            gc.strokeLine(simulationCanvas.getWidth() / 2 + x, 0, simulationCanvas.getWidth() / 2 + x, simulationCanvas.getHeight());
        }
        gc.strokeLine(0, simulationCanvas.getHeight() / 2, simulationCanvas.getWidth(), simulationCanvas.getHeight() / 2);
        for (double y = GRID_GAP * simulationScale; y < simulationCanvas.getHeight() / 2; y += GRID_GAP * simulationScale) {
            gc.strokeLine(0, simulationCanvas.getHeight() / 2 - y, simulationCanvas.getWidth(), simulationCanvas.getHeight() / 2 - y);
            gc.strokeLine(0, simulationCanvas.getHeight() / 2 + y, simulationCanvas.getWidth(), simulationCanvas.getHeight() / 2 + y);
        }

        switch (mazeNumber) {
            case 1 -> drawMaze1(gc);
            case 2 -> drawMaze2(gc);
            case 3 -> drawMaze3(gc);
        }

        GraphicsContext grid = gridCanvas.getGraphicsContext2D();
        grid.clearRect(0, 0, gridCanvas.getWidth(), gridCanvas.getHeight());
        grid.setStroke(Color.gray(0.9));
        for (int x = -1 + ((int) gridOffset.getX() % GRID_GAP); x < gridCanvas.getWidth(); x += GRID_GAP) {
            grid.strokeLine(x, 0, x, gridCanvas.getHeight());
        }
        for (int y = -1 + ((int) gridOffset.getY() % GRID_GAP); y < gridCanvas.getHeight(); y += GRID_GAP) {
            grid.strokeLine(0, y, gridCanvas.getWidth(), y);
        }

        grid.setFill(Color.WHITE);
        grid.fillRect(0, 0, templateBlocksMaxX, templateBlocksMaxY);

        if (robot.isIdle()) {
            robot.setXPos(simulationCanvas.getWidth() / 2);
            robot.setYPos(simulationCanvas.getHeight() / 2);
        }
    }

    private CodeBlock addCodeBlock(CodeBlock codeBlock) {
        codeBlockMap.put(codeBlock.getFlowCommand(), codeBlock);
        if (codeBlock.getFlowCommand().getInstruction() != RobotInstruction.INIT) {
            flow.addCommand(codeBlock.getFlowCommand());
        }
        makeDraggable(codeBlock);
        graphicalStatements.getChildren().add(codeBlock);

        return codeBlock;
    }

    private void removeCodeBlock(CodeBlock codeBlock) {
        codeBlockMap.remove(codeBlock.getFlowCommand());
        flow.removeCommand(codeBlock.getFlowCommand());
        graphicalStatements.getChildren().remove(codeBlock);
        if (codeBlock.getFlowCommand().hasPrevious()) {
            codeBlock.getFlowCommand().getPrevious().setNext(null);
            codeBlock.getFlowCommand().setPrevious(null);
        }

        if (codeBlock.getFlowCommand().hasNext()) {
            codeBlock.getFlowCommand().getNext().setPrevious(null);
            codeBlock.getFlowCommand().setNext(null);
        }
    }

    private void configureTemplateBlock(CodeBlock templateBlock) {
        templateBlock.setOnMouseEntered(event -> {
            templateBlock.getScene().setCursor(Cursor.HAND);
        });

        templateBlock.setOnMouseReleased(event -> {
            if (newBlock != null) {
                newBlock.getOnMouseReleased().handle(event);
            }

            newBlock = null;
        });

        templateBlock.setOnMousePressed(event -> {
            if (event.isMiddleButtonDown()) {
                return;
            }

            newBlock = addCodeBlock(new CodeBlock(templateBlock.getImgPath(), new FlowCommand(templateBlock.getFlowCommand().getInstruction())));
            newBlock.setLayoutX(templateBlock.getLayoutX());
            newBlock.setLayoutY(templateBlock.getLayoutY());
            newBlock.setHovered(true);
            newBlock.getOnMousePressed().handle(event);
        });

        templateBlock.setOnMouseDragged(event -> {
            if (event.isMiddleButtonDown()) {
                return;
            }

            if (newBlock != null) {
                newBlock.getOnMouseDragged().handle(event);
            }
        });

        templateBlock.setOnMouseExited(event -> {
            templateBlock.getScene().setCursor(Cursor.DEFAULT);
        });
    }

    private void checkIntersection(CodeBlock codeBlockNode) {
        boolean intersects = false;
        HashSet<CodeBlock> blockedBlocks = new HashSet<>();
        blockedBlocks.add(codeBlockNode);
        CodeBlock referenceBlock = codeBlockNode;
        if (codeBlockNode.getFlowCommand() == flow.getStartCommand()) {
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

        playButton.setDisable(!flow.isComplete());
    }

    private void makeDraggable(CodeBlock codeBlockNode) {
        codeBlockNode.setOnMousePressed(mouseEvent -> {
            if (mouseEvent.isMiddleButtonDown()) {
                return;
            }

            selectClip.play();
            codeBlockNode.setDragDelta(codeBlockNode.getLayoutX() - mouseEvent.getSceneX(), codeBlockNode.getLayoutY() - mouseEvent.getSceneY());
            codeBlockNode.setDragOrigin(codeBlockNode.getLayoutX(), codeBlockNode.getLayoutY());
            codeBlockNode.getScene().setCursor(Cursor.MOVE);
        });
        codeBlockNode.setOnMouseReleased(mouseEvent -> {
            if (mouseEvent.isMiddleButtonDown()) {
                return;
            }

            dropClip.play();

            checkIntersection(codeBlockNode);

            trashArea.setIconColor(Color.RED);

            codeBlockNode.getScene().setCursor(Cursor.HAND);

            if (codeBlockNode.getFlowCommand().getInstruction() != RobotInstruction.INIT && codeBlockNode.getBoundsInParent().intersects(trashArea.getBoundsInParent())) {
                removeCodeBlock(codeBlockNode);
            }
        });
        codeBlockNode.setOnMouseDragged(mouseEvent -> {
            if (mouseEvent.isMiddleButtonDown()) {
                return;
            }

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
            } else if (codeBlockNode.getBoundsInParent().intersects(trashArea.getBoundsInParent())) {
                trashArea.setIconColor(Color.DARKRED);
            } else {
                trashArea.setIconColor(Color.RED);
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

        transferMenuItem.setDisable(!flow.isComplete());
    }

}
