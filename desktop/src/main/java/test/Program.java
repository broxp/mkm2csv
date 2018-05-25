package test;

import javafx.application.Application;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.EventHandler;
import javafx.scene.Scene;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.image.Image;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;

public class Program extends Application implements EventHandler<MouseEvent>, ChangeListener<Object>, Runnable {
    private Thread thread = new Thread(this);
    private TextArea area;
    private TextArea res;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void changed(ObservableValue<?> observable, Object oldValue, Object newValue) {
        thread.interrupt();
    }

    @Override
    public void handle(MouseEvent event) {
        try {
            if (Desktop.isDesktopSupported()) {
                Desktop desktop = Desktop.getDesktop();
                if (desktop.isSupported(Desktop.Action.BROWSE)) {
                    String txt = ((Hyperlink) event.getSource()).getText();
                    desktop.browse(new URI(txt));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        Object oldData = null;
        while (true) {
            try {
                Thread.sleep(1000);
                Object data = Toolkit.getDefaultToolkit().getSystemClipboard().getData(DataFlavor.stringFlavor);
                if (!Objects.equals(data, oldData)) {
                    oldData = data;
                    if (changedString(data)) {
                        return;
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private boolean changedString(Object data) throws ScriptException, NoSuchMethodException, IOException {
        String str = data == null ? null : data.toString();
        area.setText(str);
        res.setText("analyzing...");

        if (str != null) {
            Pattern p = Pattern.compile(".*App token(.*)\\s+App secret(.*)\\s+Access token(.*)\\s+Access token secret(.*)\\s*.*?");
            Matcher matcher = p.matcher(str);

            if (!matcher.find()) {
                res.setText("No data found. Copy from 'App token' to the value of 'Access token secret'.");
            } else {
                res.setText("Data found!");
                String mkmAppToken = matcher.group(1);
                String mkmAppSecret = matcher.group(2);
                String mkmAccessToken = matcher.group(3);
                String mkmAccessTokenSecret = matcher.group(4);

                M11DedicatedApp app = new M11DedicatedApp(mkmAppToken, mkmAppSecret, mkmAccessToken, mkmAccessTokenSecret);
                if (app.request("https://www.mkmapi.eu/ws/v2.0/output.json/stock/file")) {
                    // https://stackoverflow.com/a/42106801/773842
                    ScriptEngine engine = new ScriptEngineManager().getEngineByName("nashorn");
                    engine.eval("var fun = function(raw) {return JSON.parse(raw).stock}");
                    Invocable invocable = (Invocable) engine;
                    Object result = invocable.invokeFunction("fun", app.responseContent());

                    // get string content from base64'd gzip
                    byte[] arr = Base64.getDecoder().decode(result.toString());
                    ByteArrayInputStream asd = new ByteArrayInputStream(arr);
                    GZIPInputStream gz = new GZIPInputStream(asd);

                    BufferedReader rd = new BufferedReader(new InputStreamReader(gz));
                    String line;
                    List<String> content = new ArrayList<>();
                    while ((line = rd.readLine()) != null) {
                        content.add(line);
                    }
                    String time = new SimpleDateFormat("yyyy-MM-dd").format(new Date());
                    String file = "stock" + time + ".csv";
                    res.setText("Written: " + file + "\n\n" + String.join("\n", content.toArray(new String[0])));
                    Files.write(Paths.get(file), content);
                    return true;
                } else {
                    String text = "Server response: " + app.responseCode() + " ";
                    if (app.lastError() != null) {
                        text += app.lastError().toString();
                    }
                    res.setText(text);
                }
            }
        }
        return false;
    }

    @Override
    public void start(Stage primaryStage) {
        Hyperlink link = new Hyperlink("https://www.cardmarket.com/de/Magic/MainPage/showMyAccount");
        link.onMouseClickedProperty().setValue(this);
        Hyperlink link2 = new Hyperlink("https://github.com/broxp/mkm2csv");
        link2.onMouseClickedProperty().setValue(this);

        area = new TextArea("\uD83D\uDCCB");
        area.setEditable(false);

        res = new TextArea("");
        res.setEditable(false);
        primaryStage.setTitle("mkm2csv");
        String b64 = "iVBORw0KGgoAAAANSUhEUgAAABAAAAAQCAYAAAAf8/9hAAAA5klEQVQ4T6XTPyvFURgH8M9ll8WiyJ83IEYDk8VrEAbJYnAXgw0xySADg10xMcjMYpE3YCAsyshA+tbvVzfLvSenzvZ8P+fpeToN/zwNTGChxXnDdqdugB3stgS6sIz3NsglXgKsYe9P8QCG2gDTOAiwhQ0sorfT1jGDzRq4qsJPBcAgJmvgByd4LAC6cVwD/VjCdwEwjPkA+/jEekE4pVMYDZDW73BYCMzhNcBZhVwUAtncaYBrNPFQCBxhNcANZvFRAPRVM2sGuMdYQTil41jJ5gLk43wVAj04x22A3JFCIA8+J/MLEH0o16x9yUEAAAAASUVORK5CYII=";
        primaryStage.getIcons().add(new Image(new ByteArrayInputStream(Base64.getDecoder().decode(b64))));
        primaryStage.setScene(new Scene(
                new VBox(
                        new Label("This program can download your MKM stock as csv. You have to provide an app key to do so."),
                        link,
                        new Label("We will only make one get stock call using that api, this program is open source, check for yourself:"),
                        link2,
                        new Label("In MKM , create a dedicated app key (if you haven't one), copy all APP DETAILS to your clipboard, it will be pasted below:"),
                        area,
                        new Label("Output:"),
                        res
                ))
        );
        primaryStage.onCloseRequestProperty().addListener(this);
        primaryStage.show();
        thread.start();
    }
}