module dev.lukafink.robotprogrammer {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.media;

    requires org.controlsfx.controls;
    requires org.kordamp.ikonli.javafx;

    opens dev.lukasfink.robotprogrammer to javafx.fxml;
    exports dev.lukasfink.robotprogrammer;
}