module com.agrisystem {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.graphics;

    opens com.agrisystem to javafx.graphics, javafx.fxml;
    opens com.agrisystem.controller to javafx.fxml;
    opens com.agrisystem.controller.farmer to javafx.fxml;
    opens com.agrisystem.controller.admin to javafx.fxml;
    opens com.agrisystem.controller.officer to javafx.fxml;
    opens com.agrisystem.model to javafx.fxml;
}
