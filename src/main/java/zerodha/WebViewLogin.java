package zerodha;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import lombok.SneakyThrows;

import java.util.concurrent.Executors;

public class WebViewLogin extends Application {
    public static Stage primaryStage = null;

    public static String getKiteTokenHeader() {
        return "enctoken " + kiteToken;
    }

    public static String getKiteToken() {
        return kiteToken;
    }

    private static String kiteToken = "";

    @SneakyThrows
    public static void start(Stage stage, Object LOCK) {
        kiteToken = null;
        if (primaryStage != null) {
            primaryStage.close();
            primaryStage = null;

        }
        primaryStage = new Stage();
        primaryStage.setTitle("ALGO ZERODHA LOGIN PAGE - DON'T CLOSE, THE APP USES THIS");
        WebView webView = new WebView();
        webView.getEngine().load("https://kite.zerodha.com/");
        Executors.newSingleThreadExecutor().execute(new Runnable() {
            @Override
            @SneakyThrows
            public void run() {
                Thread.currentThread().setName("WebViewLogin.start");
                while (true) {
                    Thread.sleep(2000);
                    if (kiteToken != null) {
                        synchronized (LOCK) {
                            LOCK.notify();
                        }
                        Platform.runLater(new Runnable() {
                            @Override
                            public void run() {
                                primaryStage.toBack();
                                stage.toFront();
                            }
                        });
                        break;
                    }
                    Platform.runLater(new Runnable() {
                        @Override
                        @SneakyThrows
                        public void run() {
                            String cookies = (String) webView.getEngine().executeScript("document.cookie;");
                            String[] data = cookies.split(";");
                            for (String s : data) {
                                if (s.startsWith("enctoken")) {
                                    String newToken = s.replace("enctoken=", "");
                                    kiteToken = newToken;
                                    placeOrder();
                                } else if (s.startsWith(" enctoken")) {
                                    String newToken = s.replace(" enctoken=", "");
                                    kiteToken = newToken;
                                    placeOrder();
                                }
                            }
                        }
                    });
                }
            }
        });

        Button backButton = new Button();
        backButton.setText("<<== BROWSE BACK");
        backButton.setStyle("-fx-text-fill: green");

        Button forwardButton = new Button();
        forwardButton.setText("BROWSE FORWARD ==>>");
        forwardButton.setStyle("-fx-text-fill: green");

        Label button = new Label();
        button.setText("DO NOT LOGIN FROM ANY NEW BROWSER OR COMPUTER, OTHERWISE SHINING STAR WILL LOG OUT");
        button.setStyle("-fx-text-fill: red; -fx-text-alignment: center");

        Region region1 = new Region();
        HBox.setHgrow(region1, Priority.ALWAYS);

        Region region2 = new Region();
        HBox.setHgrow(region2, Priority.ALWAYS);

        Button zoomInButton = new Button();
        zoomInButton.setText("ZOOM IN");
        zoomInButton.setStyle("-fx-text-fill: blue");

        Button zoomOutButton = new Button();
        zoomOutButton.setText("ZOOM OUT");
        zoomOutButton.setStyle("-fx-text-fill: blue");

        zoomInButton.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                double zoom = webView.getZoom();
                zoom = zoom + 0.1;
                webView.setZoom(zoom);
            }
        });

        zoomOutButton.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                double zoom = webView.getZoom();
                zoom = zoom - 0.1;
                webView.setZoom(zoom);
            }
        });


        HBox hBox = new HBox(backButton, zoomInButton, region1, button, region2, zoomOutButton, forwardButton);

        backButton.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                goBack(webView.getEngine());
            }
        });

        forwardButton.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                goForward(webView.getEngine());
            }
        });

        VBox vBox = new VBox();
        vBox.getChildren().add(hBox);
        StackPane stackPane = new StackPane(webView);
        VBox.setVgrow(stackPane, Priority.ALWAYS);
        vBox.getChildren().add(stackPane);
        Scene scene = new Scene(vBox, 1200, 800);

        primaryStage.setOnCloseRequest(new EventHandler<WindowEvent>() {
            @Override
            public void handle(WindowEvent event) {
                event.consume();
            }
        });

        primaryStage.setScene(scene);
        primaryStage.show();
    }


    public static void goBack(WebEngine webEngine) {
        Platform.runLater(() -> {
            webEngine.executeScript("history.back()");
        });
    }

    public static void goForward(WebEngine webEngine) {
        Platform.runLater(() -> {
            webEngine.executeScript("history.forward()");
        });
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        start(primaryStage, new Object());
    }

    public static void placeOrder() {
        Executors.newSingleThreadExecutor().execute(new Runnable() {
            @Override
            public void run() {
                while (true) {
                    HttpAgent.getHoldings();
                    try {
                        Thread.sleep(2000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        });
    }
}