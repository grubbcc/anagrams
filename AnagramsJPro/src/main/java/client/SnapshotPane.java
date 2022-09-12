package client;

import javafx.animation.*;
import javafx.application.Platform;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.Scene;

import com.madgag.gif.fmsware.AnimatedGifEncoder;
import javafx.scene.image.WritableImage;

import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * A user interface for playing, watching, or analyzing a game.
 */
class SnapshotPane extends GameWindowBase {

    final SimpleDoubleProperty progress = new SimpleDoubleProperty(0);
    final SimpleBooleanProperty finished = new SimpleBooleanProperty(false);

    AnimatedGifEncoder encoder = new AnimatedGifEncoder();

    /**
     *
     */
    SnapshotPane(AnagramsClient client, String gameID, String username, String minLength, int blankPenalty, String numSets, String speed, String lexicon, ArrayList<String[]> gameLog) {
        super(client, gameID, username, minLength, blankPenalty, numSets, speed, lexicon, gameLog);

        int WIDTH = numSets.equals("3") ? 1100 : 1000;
        int HEIGHT = numSets.equals("3") ? 650 : 630;

        setPrefSize(WIDTH, HEIGHT);
        setMinSize(WIDTH, HEIGHT);
        setMaxSize(WIDTH, HEIGHT);

        Scene scene = new Scene(this, WIDTH, HEIGHT);
        Stage stage = new Stage(StageStyle.TRANSPARENT);
        stage.setScene(scene);

        encoder.setDelay(1000);
        encoder.setQuality(20);
        encoder.setRepeat(0 /* infinite */);

        encoder.setSize(WIDTH, HEIGHT);
        applyCss();
        layout();
        Platform.runLater(() -> {
            try {
                animate();
            } catch (IOException e) {
                e.printStackTrace();
            }
        });

    }

    /**
     * Create an animation (offscreen) and play it. Screen capture each frame,
     * save the resulting images in gif format, and download it.
     */
    void animate() throws IOException {

        final Duration frameDuration = Duration.millis(1000);
        Timeline animation = new Timeline(20);

        File tempFile = new File("tmp/" + gameID + ".gif");

        encoder.start(tempFile.getAbsolutePath());

        for (int i = 0; i < gameLog.size(); i++) {
            final int finalI = i;

            animation.getKeyFrames().add(new KeyFrame(frameDuration.multiply(i + 1), e -> {
                showPosition(gameLog.get(finalI));

                progress.set((double) finalI / gameLog.size());
                try {
                    capture();
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }));
        }

        //display final frame first
        animation.getKeyFrames().set(0, new KeyFrame(Duration.ZERO, e -> {

            showPosition(gameLog.get(gameLog.size() - 1));
            try {
                capture();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
            System.out.println();
        }));

        animation.playFromStart();

        animation.setOnFinished(event -> {
            try {
                finished.set(true);
                System.out.println("FINISHED!");
                encoder.finish();
                client.getWebAPI().downloadURL(tempFile.toURI().toURL());
            } catch (IOException e) {
                e.printStackTrace();
            }
        });

    }

    /**
     *
     */
    void capture() throws IOException {

        WritableImage fxImage = new WritableImage((int)getPrefWidth(), (int)getPrefHeight());

        snapshot(null, fxImage);
        BufferedImage image = SwingFXUtils.fromFXImage(fxImage, null);

        Graphics2D graphics = image.createGraphics();
        graphics.drawImage(image, 0, 0, null);

        encoder.addFrame(image);

    }
}
