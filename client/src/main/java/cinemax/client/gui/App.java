package cinemax.client.gui;

import cinemax.client.gui.navigation.ClientApplication;
import javafx.application.Application;

// Entry point dell'app, corrisponde alla mainClass dichiarata nel pom.xml.
public class App {

    public static void main(String[] args) {
        Application.launch(ClientApplication.class, args);
    }
}
