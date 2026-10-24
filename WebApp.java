import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// A tiny web server. It serves index.html and answers two requests:
//   /api/url   -> checks a URL
//   /api/email -> checks an email
// All the real checking is done by PhishGuard.java.
public class WebApp {

    static void send(HttpExchange ex, String type, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().set("Content-Type", type + "; charset=utf-8");
        ex.sendResponseHeaders(200, bytes.length);
        try (OutputStream out = ex.getResponseBody()) {
            out.write(bytes);
        }
    }

    // Reads the data the web page sends (name=value&name2=value2)
    static Map<String, String> readForm(HttpExchange ex) throws IOException {
        String body = new String(ex.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        Map<String, String> form = new HashMap<>();
        for (String pair : body.split("&")) {
            int eq = pair.indexOf("=");
            if (eq > 0) {
                String key = URLDecoder.decode(pair.substring(0, eq), StandardCharsets.UTF_8);
                String value = URLDecoder.decode(pair.substring(eq + 1), StandardCharsets.UTF_8);
                form.put(key, value);
            }
        }
        return form;
    }

    static String escape(String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", " ").replace("\r", "");
    }

    // Packs the answer so the web page can read it
    static String toJson(int score, List<String> reasons) {
        if (score > 100) {
            score = 100;
        }
        StringBuilder sb = new StringBuilder();
        sb.append("{\"score\":").append(score);
        sb.append(",\"verdict\":\"").append(PhishGuard.verdictFor(score)).append("\"");
        sb.append(",\"reasons\":[");
        for (int i = 0; i < reasons.size(); i++) {
            if (i > 0) {
                sb.append(",");
            }
            sb.append("\"").append(escape(reasons.get(i))).append("\"");
        }
        sb.append("]}");
        return sb.toString();
    }

    public static void main(String[] args) throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);

        server.createContext("/", ex -> {
            String page = new String(Files.readAllBytes(Path.of("index.html")), StandardCharsets.UTF_8);
            send(ex, "text/html", page);
        });

        server.createContext("/api/url", ex -> {
            Map<String, String> form = readForm(ex);
            List<String> reasons = new ArrayList<>();
            int score = PhishGuard.analyzeUrl(form.getOrDefault("url", "").trim(), reasons);
            if (reasons.isEmpty()) {
                reasons.add("No suspicious signs found");
            }
            send(ex, "application/json", toJson(score, reasons));
        });

        server.createContext("/api/email", ex -> {
            Map<String, String> form = readForm(ex);
            List<String> reasons = new ArrayList<>();
            int score = PhishGuard.analyzeEmail(form.getOrDefault("sender", "").trim(),
                    form.getOrDefault("body", ""), reasons);
            if (reasons.isEmpty()) {
                reasons.add("No suspicious signs found");
            }
            send(ex, "application/json", toJson(score, reasons));
        });

        server.start();
        System.out.println("PhishGuard is running. Open http://localhost:8080 in your browser.");
        System.out.println("Press Ctrl+C here to stop it.");
    }
}