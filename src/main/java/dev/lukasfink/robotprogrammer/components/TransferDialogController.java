package dev.lukasfink.robotprogrammer.components;

import com.fazecast.jSerialComm.SerialPort;
import dev.lukasfink.robotprogrammer.flow.Flow;
import dev.lukasfink.robotprogrammer.flow.FlowCommand;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SplitMenuButton;

import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.HashMap;
import java.util.ResourceBundle;

public class TransferDialogController implements Initializable {

    @FXML
    private SplitMenuButton comPortMenuButton;

    @FXML
    private Button cancelButton;

    @FXML
    private Button transferButton;

    private HashMap<String, MenuItem> portMenuItems;
    private SerialPort[] serialPorts;
    private Flow flow;

    public TransferDialogController() {
        portMenuItems = new HashMap<>();
    }

    public void setFlow(Flow flow) {
        this.flow = flow;
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        cancelButton.setOnAction(event -> comPortMenuButton.getScene().getWindow().hide());

        transferButton.setOnAction(event -> {
            for (SerialPort serialPort: serialPorts) {
                if (serialPort.getPortDescription().equals(comPortMenuButton.getText())) {
                    cancelButton.setDisable(true);
                    transferButton.setDisable(true);
                    new Thread(() -> {
                        serialPort.openPort();
                        try {
                            byte[] bytes = "--TRANSFER-PROGRAM--\n".getBytes(StandardCharsets.US_ASCII);
                            serialPort.writeBytes(bytes, bytes.length);
                            Thread.sleep(100);
                            FlowCommand currentCommand = flow.getStartCommand();
                            while (true) {
                                byte[] commandBytes = (currentCommand.getInstruction().getValue() + "\n").getBytes(StandardCharsets.US_ASCII);
                                serialPort.writeBytes(commandBytes, commandBytes.length);
                                Thread.sleep(100);

                                if (!currentCommand.hasNext()) {
                                    break;
                                } else {
                                    currentCommand = currentCommand.getNext();
                                }
                            }
                            bytes = "--TRANSFER-END--\n".getBytes(StandardCharsets.US_ASCII);
                            serialPort.writeBytes(bytes, bytes.length);
                            Thread.sleep(100);
                            for (int i = 0; i < 100; i++) {
                                if (serialPort.bytesAvailable() == 0) {
                                    Thread.sleep(100);
                                }

                                byte[] readBuffer = new byte[serialPort.bytesAvailable()];
                                int numRead = serialPort.readBytes(readBuffer, readBuffer.length);
                                System.out.println("Read " + numRead + " bytes.");
                                String readString = new String(readBuffer);
                                System.out.println("Read: " + readString);
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
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
