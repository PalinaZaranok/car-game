module org.example.cargame {
    requires javafx.controls;
    requires javafx.fxml;

    requires org.kordamp.bootstrapfx.core;
    requires com.fazecast.jSerialComm;

    opens org.example.cargame to javafx.fxml;
    exports org.example.cargame;
}