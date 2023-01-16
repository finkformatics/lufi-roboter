module dev.lukafink.robotprogrammer {
    requires javafx.controls;
    requires javafx.fxml;

    requires org.controlsfx.controls;
    requires org.kordamp.ikonli.javafx;
    requires org.jfxtras.styles.jmetro;

    opens dev.lukafink.robotprogrammer to javafx.fxml;
    exports dev.lukafink.robotprogrammer;
}