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

@Component("mediaDavidia")
public class MediaProcessorDavidia extends MediaProcessor {

    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(3))
        .build();

    @Value("${app.server}")
    private String appServer;

    @Override
    public Event event(Map<String, String> queries) {
        // 提供转化ID

        Event e = new Event();
        e.setMedia("davidia");

        if (!isValid(queries, "davidia_id")) {
            return null;
        }
        e.setMediaCode(queries.get("davidia_id"));

        if (!isValid(queries, "davidia_event")) {
            return null;
        }
        e.setEvent(queries.get("davidia_event"));

        e.setQueries(queries);

        return e;
    }

    @Override
    public Boolean callback(Callback callback, Event event) {
        if (isValid(event.getQueries(), "davidia_callback")) {
            String url = UriComponentsBuilder.fromUriString(event.getQueries().get("davidia_callback"))
                .replaceQueryParam("davidia_event", callback.getEvent())
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
            "davidia_media=davidia&" +
            "davidia_id=__DAVIDIA_ID__&" +
            "davidia_event=__DAVIDIA_EVENT__&" +
            "davidia_callback=__DAVIDIA_CALLBACK__&" +

            "ts=__TS__&" +
            "req_id=__REQ_ID__&" +
            "click_id=__CLICK_ID__&" +

            "imei=__IMEI__&" +
            "imei_md5=__IMEI_MD5__&" +
            "android_id=__ANDROID_ID__&" +
            "android_id_md5=__ANDROID_ID_MD5__&" +
            "oaid=__OAID__&" +
            "oaid_md5=__OAID_MD5__&" +
            "idfa=__IDFA__&" +
            "idfa_md5=__IDFA_MD5__&" +
            "idfv=__IDFV__&" +
            "idfv_md5=__IDFV_MD5__&" +
            "caid1=__CAID1__&" +
            "caid1_md5=__CAID1_MD5__&" +
            "caid1_v=__CAID1_V__&" +
            "caid2=__CAID2__&" +
            "caid2_md5=__CAID2_MD5__&" +
            "caid2_v=__CAID2_V__&" +
            "aaid=__AAID__&" +
            "mac=__MAC__&" +
            "mac_md5=__MAC_MD5__&" +
            "ip=__IP__&" +
            "ipv6=__IPV6__&" +

            "ua=__UA__&" +
            "os=__OS__&" +
            "os_v=__OS_V__";
    }

    private String trimTrailingSlash(String url) {
        if (url.endsWith("/")) {
            return url.substring(0, url.length() - 1);
        }

        return url;
    }

}
