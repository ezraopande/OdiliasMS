module com.odilias.odms {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.sql;
    requires java.naming;
    requires mysql.connector.j;
    requires jbcrypt;
    requires com.zaxxer.hikari;

    opens com.odilias.odms to javafx.fxml;
    opens com.odilias.odms.controller to javafx.fxml;
    opens com.odilias.odms.model to javafx.base;

    exports com.odilias.odms;
    exports com.odilias.odms.controller;
    exports com.odilias.odms.model;
    exports com.odilias.odms.util;
}
