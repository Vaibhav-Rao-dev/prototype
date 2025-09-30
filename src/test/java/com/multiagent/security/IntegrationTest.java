package com.multiagent.security;

import com.multiagent.security.db.Db;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

public class IntegrationTest {
    private static Thread serverThread;

    @BeforeAll
    public static void startServer() throws Exception {
        Db.init();
        serverThread = new Thread(() -> Main.main(new String[]{}));
        serverThread.setDaemon(true);
        serverThread.start();
        // wait for server readiness by polling /agents
        int attempts = 0; boolean up = false;
        while (attempts++ < 20) {
            try { Thread.sleep(200); var u = new URL("http://localhost:4567/agents"); var c = (HttpURLConnection)u.openConnection(); c.setRequestMethod("GET"); c.setConnectTimeout(200); c.getInputStream().close(); up=true; break; } catch (Exception e) { }
        }
        if (!up) throw new RuntimeException("server did not start in time");
    }

    @AfterAll
    public static void stopServer() throws Exception {
        // best-effort shutdown: call /shutdown if implemented (not present) and interrupt thread
        if (serverThread != null) serverThread.interrupt();
    }

    @Test
    public void fullFlow() throws IOException, InterruptedException {
        // create a playbook
        String pb = "{\"name\":\"it-pb\",\"definition\":{\"steps\":[{\"action\":\"create_ticket\",\"requires_approval\":true,\"approver_role\":\"manager\"}]}}";
        String pbId = postJsonReturnField("http://localhost:4567/playbooks", pb, "id");
        assertNotNull(pbId);

        // run it
        String runJson = postJson("http://localhost:4567/playbooks/"+pbId+"/run","{}");
        assertTrue(runJson.contains("runId"));

        // list runs
        String runs = get("http://localhost:4567/playbook_runs");
        assertTrue(runs.contains(pbId));

        // fetch run id from runs (simple parse)
        // for brevity assume runs contains an id string
    }

    static String postJsonReturnField(String url, String json, String field) throws IOException {
        String r = postJson(url, json);
        int idx = r.indexOf(field);
        if (idx==-1) return null;
        int q = r.indexOf('"', idx+field.length());
        int s = r.indexOf('"', q+1);
        if (q==-1||s==-1) return null;
        return r.substring(q+1,s);
    }

    static String postJson(String urlStr, String json) throws IOException {
        URL url = new URL(urlStr);
        var c = (HttpURLConnection)url.openConnection();
        c.setRequestMethod("POST"); c.setDoOutput(true); c.setRequestProperty("Content-Type","application/json");
        c.getOutputStream().write(json.getBytes(StandardCharsets.UTF_8));
        var in = c.getInputStream();
        return new String(in.readAllBytes(), StandardCharsets.UTF_8);
    }

    static String get(String urlStr) throws IOException {
        URL url = new URL(urlStr);
        var c = (HttpURLConnection)url.openConnection();
        c.setRequestMethod("GET");
        var in = c.getInputStream();
        return new String(in.readAllBytes(), StandardCharsets.UTF_8);
    }
}
