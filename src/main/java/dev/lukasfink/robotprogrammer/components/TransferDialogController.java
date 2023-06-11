package dev.lukasfink.robotprogrammer.components;

import com.fazecast.jSerialComm.SerialPort;
import dev.lukasfink.robotprogrammer.flow.Flow;
import dev.lukasfink.robotprogrammer.flow.FlowCommand;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.util.HashMap;
import java.util.ResourceBundle;

public class TransferDialogController implements Initializable {

    @FXML
    private SplitMenuButton comPortMenuButton;

    @FXML
    private Button cancelButton;

    @FXML
    private Button transferButton;

    @FXML
    private ProgressBar transferProgress;

    private HashMap<String, MenuItem> portMenuItems;
    private SerialPort[] serialPorts;
    private Flow flow;
    private boolean blocked = false;
    private StringBuilder dataReceived;
    private OutputStream outputStream;

    public TransferDialogController() {
        portMenuItems = new HashMap<>();
        dataReceived = new StringBuilder();
    }

    public void setFlow(Flow flow) {
        this.flow = flow;
    }

    private synchronized void writeToRobot(String message) throws InterruptedException, IOException {
        long waitedSince = System.currentTimeMillis();
        while (blocked) {
            Thread.sleep(20);
            if (System.currentTimeMillis() - waitedSince > 3000) {
                System.out.println("ERROR! Waited 3 seconds for the lock being released...");
                blocked = false;
            }
        }
        System.out.println("Closing lock, writing message: " + message);
        blocked = true;
        outputStream.write(message.getBytes());
        Thread.sleep(1000);
    }

    private void appendReceivedData(String receivedData) {
        String msg1 = receivedData;
        String msg2 = null;
        boolean foundNewline = false;
        if (receivedData.contains("\n") || receivedData.contains("\r")) {
            foundNewline = true;
            String[] msgParts = receivedData.split("(\\n|\\r)");
            if (msgParts.length > 0) {
                msg1 = msgParts[0];
                if (msgParts.length > 1) {
                    msg2 = msgParts[1];
                }
            }
        }

        dataReceived.append(msg1);
        if (foundNewline) {
            System.out.println("Data combination successful, received: " + dataReceived);
            if ("ACK".equals(dataReceived.toString().replaceAll("\\s", ""))) {
                System.out.println("Releasing lock");
                blocked = false;
            }

            dataReceived = new StringBuilder();
        }

        if (msg2 != null) {
            dataReceived.append(msg2);
        }
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        cancelButton.setOnAction(event -> comPortMenuButton.getScene().getWindow().hide());

        transferButton.setOnAction(event -> {
            for (SerialPort serialPort: serialPorts) {
                if (serialPort.getPortDescription().equals(comPortMenuButton.getText())) {
                    cancelButton.setDisable(true);
                    transferButton.setDisable(true);
                    transferProgress.setVisible(true);
                    transferProgress.setProgress(ProgressIndicator.INDETERMINATE_PROGRESS);
                    new Thread(() -> {
                        boolean successfullyOpened = serialPort.openPort(20);
                        serialPort.setComPortParameters(9600, 8, 1, SerialPort.NO_PARITY);
                        if (!successfullyOpened) {
                            throw new RuntimeException("Error opening serial port");
                        }

                        try {
                            // Create input and output streams for the port
                            InputStream inputStream = serialPort.getInputStream();
                            outputStream = serialPort.getOutputStream();

                            // Create a separate thread to read from the serial port
                            Thread readThread = new Thread(() -> {
                                try {
                                    while (serialPort.isOpen()) {
                                        // Read data from the input stream
                                        if (inputStream.available() > 0) {
                                            byte[] buffer = new byte[inputStream.available()];
                                            int bytesRead = inputStream.read(buffer);
                                            String response = new String(buffer, 0, bytesRead);
                                            appendReceivedData(response);
                                        }

                                        // Add some delay between readings
                                        Thread.sleep(100);
                                    }
                                } catch (IOException | InterruptedException e) {
                                    e.printStackTrace();
                                }
                            });

                            // Start the read thread
                            readThread.start();

                            Thread.sleep(2500);
                            FlowCommand currentCommand = flow.getStartCommand();
                            int counter = 0;
                            final int total = flow.count();
                            while (true) {
                                writeToRobot(currentCommand.getInstruction().getValue());
                                int finalCounter = ++counter;
                                Platform.runLater(() -> transferProgress.setProgress(Math.min(1, Math.max(0, finalCounter / (double) total))));

                                if (!currentCommand.hasNext()) {
                                    break;
                                } else {
                                    currentCommand = currentCommand.getNext();
                                }
                            }
                            Thread.sleep(1000);
                            Platform.runLater(() -> transferProgress.setProgress(ProgressIndicator.INDETERMINATE_PROGRESS));
                            Thread.sleep(2500);
                            serialPort.closePort();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        Platform.runLater(() -> {
                            comPortMenuButton.getScene().getWindow().hide();
                        });
                    }).start();
                    break;
                }
            }
        });

        new Thread(() -> {
            serialPorts = SerialPort.getCommPorts();
            Platform.runLater(() -> {
                comPortMenuButton.getItems().clear();
                boolean first = true;
                for (SerialPort serialPort: serialPorts) {
                    if (!serialPort.getPortDescription().contains("Arduino")) {
                        continue;
                    }

                    if (first) {
                        comPortMenuButton.setText(serialPort.getPortDescription());
                        comPortMenuButton.setDisable(false);
                        transferButton.setDisable(false);
                    }

                    MenuItem menuItem = new MenuItem(serialPort.getPortDescription());
                    menuItem.setOnAction(event -> comPortMenuButton.setText(serialPort.getPortDescription()));

                    portMenuItems.put(serialPort.getPortDescription(), menuItem);
                    comPortMenuButton.getItems().add(menuItem);
                    first = false;
                }
            });
        }).start();
    }

}
