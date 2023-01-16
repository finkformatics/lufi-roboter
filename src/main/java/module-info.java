module dev.lukafink.robotprogrammer {
    requires javafx.controls;
    requires javafx.fxml;

    requires org.controlsfx.controls;
    requires org.kordamp.ikonli.javafx;
    requires org.jfxtras.styles.jmetro;

    opens dev.lukasfink.robotprogrammer to javafx.fxml;
    exports dev.lukasfink.robotprogrammer;
}