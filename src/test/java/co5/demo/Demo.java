package co5.demo;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.FileInputStream;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.ArrayList;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.apache.commons.codec.digest.DigestUtils;
import org.junit.jupiter.api.Test;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

class Demo {

    @Test
    void run() {
        try {
            String auth = "changeme";
            HttpRequest request;
            HttpResponse<String> response;
            JsonObject responseBody;
            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, trustAllCerts, new SecureRandom());

            HttpClient client = HttpClient.newBuilder()
                    .sslContext(sslContext)
                    .build();

            //Domain knowledge
            byte[] ba = new FileInputStream("build/libs/demo.jar").readAllBytes();
            byte[] resultByte = DigestUtils.md5(ba);
            String streamMD5 = new String(java.util.Base64.getEncoder().encode(resultByte));
            //Request upload url
            request = HttpRequest.newBuilder()
                    .GET()
                    .header("Authorization", auth)
                    .uri(new URI("https://deployment.co5.be/v1?md5=" + streamMD5))
                    .build();
            response = client.send(request, BodyHandlers.ofString());
            responseBody = (JsonObject) JsonParser.parseString(response.body());
            String id = responseBody.get("id").getAsString().trim();
            //Upload domain knowledge as library
            request = HttpRequest.newBuilder()
                    .PUT(BodyPublishers.ofByteArray(ba))
                    .timeout(Duration.ofMinutes(60))
                    .uri(new URI(responseBody.get("url").getAsString().trim()))
                    .header("content-type", "application/octet-stream")
                    .build();
            response = client.send(request, BodyHandlers.ofString());
            assertTrue(response.statusCode() == 200);
            //Request cloud sandbox. This will take 5-10 minutes to create the resources  
            request = HttpRequest.newBuilder()
                    .POST(BodyPublishers.ofString("{ \"sourceIp\": \"71.56.205.231\", \"builderClass\": \"co5.demo.DemoBuilder\"}"))
                    .timeout(Duration.ofMinutes(60))
                    .header("Authorization", auth)
                    .uri(new URI("https://deployment.co5.be/v1?id=" + responseBody.get("id").getAsString().trim()))
                    .build();
            response = client.send(request, BodyHandlers.ofString());
            responseBody = (JsonObject) JsonParser.parseString(response.body());
            assertTrue(response.statusCode() == 200);
            //Wait 3 seconds for Load Balancer to be ready
            Thread.sleep(3000);
            runJob(responseBody.get("url").getAsString().trim(), sslContext);
            request = HttpRequest.newBuilder()
                    .DELETE()
                    .timeout(Duration.ofMinutes(60))
                    .header("Authorization", auth)
                    .uri(new URI("https://deployment.co5.be/v1?id=" + id))
                    .build();
            response = client.send(request, BodyHandlers.ofString());
            assertTrue(response.statusCode() == 200);
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    private void runJob(String url, SSLContext ssl) throws IOException {
        int count = 20;
        byte[] bais = Files.readAllBytes(Path.of("8grade.158k.xlsx"));
        ArrayList<CompletableFuture<Void>> results = new ArrayList<CompletableFuture<Void>>(count);
        HttpClient client = HttpClient.newBuilder().version(HttpClient.Version.HTTP_2)
                .sslContext(ssl)
                .build();
        HttpRequest request = HttpRequest.newBuilder(URI.create(String.format("%s?id=%s",
                 String.format("%s/co5/submit", url),
                 UUID.randomUUID())))
                .header("authorization", "valid")
                .POST(HttpRequest.BodyPublishers.ofByteArray(bais))
                .build();
        for (int i = 0; i < count;) {
            int ii = i++;
            results.add(client.sendAsync(request, HttpResponse.BodyHandlers.ofByteArray())
                    .thenAccept(response -> {
                        if (response.statusCode() == 200) {
                            try {

                                Files.write(Path.of("output", String.format("doc-%d.xlsx", ii)), response.body(), StandardOpenOption.CREATE);
                            } catch (IOException e) {
                                // TODO Auto-generated catch block
                                e.printStackTrace();
                            }
                        } else {
                            System.out.println("error:" + response.statusCode());
                            System.out.println(new String(response.body()));
                        }
                    }));
        }
        for (int i = 0; i < count;) {
            results.get(i).join();
            i++;
        }
    }

    private static TrustManager[] trustAllCerts = new TrustManager[]{
        new X509TrustManager() {
            public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                return null;
            }

            public void checkClientTrusted(
                    java.security.cert.X509Certificate[] certs, String authType) {
            }

            public void checkServerTrusted(
                    java.security.cert.X509Certificate[] certs, String authType) {
            }
        }
    };
}
