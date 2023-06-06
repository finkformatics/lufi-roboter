module dev.lukafink.robotprogrammer {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.media;

    requires org.controlsfx.controls;
    requires org.kordamp.ikonli.javafx;
    requires org.kordamp.ikonli.material2;
    requires com.google.gson;
    requires com.fazecast.jSerialComm;

    opens dev.lukasfink.robotprogrammer to javafx.fxml;
    opens dev.lukasfink.robotprogrammer.components to javafx.fxml;
    opens dev.lukasfink.robotprogrammer.io to com.google.gson;
    exports dev.lukasfink.robotprogrammer;
}