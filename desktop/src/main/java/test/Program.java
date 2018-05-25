package test;

import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;

/**
 * https://www.mkmapi.eu/ws/documentation/API:Auth_java
 */
public class Program {
    public static void main(String[] args) throws Exception {
        Object data = Toolkit.getDefaultToolkit().getSystemClipboard().getData(DataFlavor.stringFlavor);
        String str = data == null ? null : data.toString();

        String mkmAppToken = null;
        String mkmAppSecret = null;
        String mkmAccessToken = null;
        String mkmAccessTokenSecret = null;
        if (str != null) {
            Pattern p = Pattern.compile(".*App token(.*)\\s+App secret(.*)\\s+Access token(.*)\\s+Access token secret(.*)\\s*.*?");
            Matcher matcher = p.matcher(str);

            if (matcher.find()) {
                mkmAppToken = matcher.group(1);
                mkmAppSecret = matcher.group(2);
                mkmAccessToken = matcher.group(3);
                mkmAccessTokenSecret = matcher.group(4);
            }
        }
        if (mkmAppToken == null) {
            System.out.println("Goto\nhttps://www.cardmarket.com/de/Magic/MainPage/showMyAccount\ncreate a dedicated app key, copy all APP DETAILS to your clipboard, then run the program again.");
            return;
        }

        M11DedicatedApp app = new M11DedicatedApp(mkmAppToken, mkmAppSecret, mkmAccessToken, mkmAccessTokenSecret);

        if (app.request("https://www.mkmapi.eu/ws/v2.0/output.json/stock/file")) {
            // https://stackoverflow.com/a/42106801/773842
            ScriptEngine engine = new ScriptEngineManager().getEngineByName("nashorn");
            engine.eval("var fun = function(raw) {return JSON.parse(raw).stock}");
            Invocable invocable = (Invocable) engine;
            Object result = invocable.invokeFunction("fun", new Object[]{app.responseContent()});

            // get string content from base64'd gzip
            byte[] arr = Base64.getDecoder().decode(result.toString());
            ByteArrayInputStream asd = new ByteArrayInputStream(arr);
            GZIPInputStream gz = new GZIPInputStream(asd);

            BufferedReader rd = new BufferedReader(new InputStreamReader(gz));
            String line = null;
            List<String> content = new ArrayList<String>();
            while ((line = rd.readLine()) != null) {
                System.out.println(line);
                content.add(line);
            }
            String time = new SimpleDateFormat("yyyy-MM-dd").format(new Date());
            Files.write(Paths.get("stock" + time + ".csv"), content);
        } else {
            System.out.println("Server response: " + app.responseCode());
            if (app.lastError() != null) {
                app.lastError().printStackTrace();
            }
        }
    }
}