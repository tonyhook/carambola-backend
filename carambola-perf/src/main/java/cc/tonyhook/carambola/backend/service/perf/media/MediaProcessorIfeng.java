package cc.tonyhook.carambola.backend.service.perf.media;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import cc.tonyhook.carambola.backend.entity.perf.Callback;
import cc.tonyhook.carambola.backend.entity.perf.Event;

@Component("mediaIfeng")
public class MediaProcessorIfeng extends MediaProcessor {

    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(3))
        .build();

    @Value("${app.server}")
    private String appServer;

    @Override
    public Event event(Map<String, String> queries) {
        // 不提供转化ID

        Event e = new Event();
        e.setMedia("ifeng");

        if (!isValid(queries, "davidia_id")) {
            return null;
        }
        e.setMediaCode(queries.get("davidia_id"));

        if (!isValid(queries, "davidia_event")) {
            return null;
        }
        e.setEvent(queries.get("davidia_event"));

        if (isValid(queries, "os")) {
            String os = queries.get("os");
            switch (os) {
                case "0":
                    queries.put("os", "Android");
                    break;
                case "1":
                    queries.put("os", "iOS");
                    break;
                case "2":
                    queries.put("os", "Windows");
                    break;
                case "3":
                    queries.put("os", "MacOS");
                    break;
                default:
                    break;
            }
        }

        e.setQueries(queries);

        return e;
    }

    @Override
    public Boolean callback(Callback callback, Event event) {
        if (isValid(event.getQueries(), "davidia_callback")) {
            String url = UriComponentsBuilder.fromUriString(event.getQueries().get("davidia_callback"))
                .build()
                .encode(StandardCharsets.UTF_8)
                .toUriString();

            try {
                System.out.println("callbackB:" + url);

                HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(5))
                    .GET()
                    .build();
                HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
                String body = response.body();
                if (response.statusCode() < 200 || response.statusCode() >= 300 || body == null) {
                    return false;
                }

                return true;
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            } catch (Exception e) {
                return false;
            }
        }

        return true;
    }

    @Override
    public String getEventUrl() {
        return trimTrailingSlash(appServer) + "/api/open/event?" +
            "davidia_media=ifeng&" +
            "davidia_id=__DAVIDIA_ID__&" +
            "davidia_event=0002&" +

            "ts=MNT_10_TS&" +
            "req_id=MNT_09_REQ&" +

            "imei=MNT_03_IMEI&" +
            "imei_md5=MNT_03_MD5_IMEI&" +
            "oaid=MNT_14_OAID&" +
            "idfa=MNT_04_IDFA&" +
            "idfa_md5=MNT_04_MD5_IDFA&" +
            "mac_md5=MNT_02_MAC&" +
            "ip=MNT_01_IP&" +

            "ua=MNT_18_UA&" +
            "os=MNT_00_OS&" +

            "davidia_callback=MNT_08_CALLBACK";
    }

    private String trimTrailingSlash(String url) {
        if (url.endsWith("/")) {
            return url.substring(0, url.length() - 1);
        }

        return url;
    }

}
