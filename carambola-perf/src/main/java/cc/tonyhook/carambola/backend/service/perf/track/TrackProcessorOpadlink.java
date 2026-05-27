package cc.tonyhook.carambola.backend.service.perf.track;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import cc.tonyhook.carambola.backend.entity.perf.Callback;
import cc.tonyhook.carambola.backend.entity.perf.ClientChannel;
import cc.tonyhook.carambola.backend.entity.perf.Event;

@Component("trackOpadlink")
public class TrackProcessorOpadlink extends TrackProcessor {

    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(3))
        .build();
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Value("${app.server}")
    private String appServer;

    @Override
    public Callback callback(Map<String, String> queries) {
        Callback c = new Callback();
        c.setTrack("opadlink");

        if (!isValid(queries, "davidia_id")) {
            return null;
        }
        c.setSerialno(queries.get("davidia_id"));

        if (!isValid(queries, "conv_type")) {
            c.setEvent("2003");
        } else {
            switch (queries.get("conv_type")) {
                case "activation":
                    c.setEvent("2002");
                    break;
                case "registered":
                    c.setEvent("2003");
                    break;
                case "retain_1":
                    c.setEvent("2004");
                    break;
                default:
                    c.setEvent("2003");
            }
        }

        c.setQueries(queries);

        return c;
    }

    public Boolean event(Event event, ClientChannel cc) {
        String url = "https://opadlink.com/ad/" +
            "__ACTION__/__OP_AK__" +
            "?cid=__CID__&op_cc=__OP_CC__&os=__OS__&idfa=__IDFA__&oaid=__OAID__&caid=__CAID__&imei=__IMEI__&mac=__MAC__&ip=__IP__&ua=__UA__&ad_clickid=__AD_CLICKID__";

        String callbackUrl = trimTrailingSlash(appServer) + "/api/open/callback?davidia_track=opadlink&davidia_id=" + event.getSerialno();
        url = url.replace("__AD_CLICKID__", URLEncoder.encode(callbackUrl, StandardCharsets.UTF_8));

        if (cc.getTrackCode().split("\\|").length >= 3) {
            url = url.replace("__OP_AK__", URLEncoder.encode(cc.getTrackCode().split("\\|")[0], StandardCharsets.UTF_8));
            url = url.replace("__CID__", URLEncoder.encode(cc.getTrackCode().split("\\|")[1], StandardCharsets.UTF_8));
            url = url.replace("__OP_CC__", URLEncoder.encode(cc.getTrackCode().split("\\|")[2], StandardCharsets.UTF_8));
        } else {
            return false;
        }

        if (event.getEvent().equals("0002")) {
            url = url.replace("__ACTION__", "click");
        } else {
            return false;
        }

        // 替换事件字段

        // 替换设备字段
        if (isValid(event.getQueries(), "imei")) {
            url = url.replace("__IMEI__", event.getQueries().get("imei"));
        }
        if (isValid(event.getQueries(), "oaid")) {
            url = url.replace("__OAID__", event.getQueries().get("oaid"));
        }
        if (isValid(event.getQueries(), "idfa")) {
            url = url.replace("__IDFA__", event.getQueries().get("idfa"));
        }
        if (isValid(event.getQueries(), "caid")) {
            url = url.replace("__CAID__", event.getQueries().get("caid"));
        }
        if (isValid(event.getQueries(), "caid1")) {
            url = url.replace("__CAID__", event.getQueries().get("caid1"));
        }
        if (isValid(event.getQueries(), "ip")) {
            url = url.replace("__IP__", event.getQueries().get("ip"));
        }
        if (isValid(event.getQueries(), "ua")) {
            url = url.replace("__UA__", URLEncoder.encode(event.getQueries().get("ua"), StandardCharsets.UTF_8));
        }
        if (isValid(event.getQueries(), "os")) {
            String os;
            switch (event.getQueries().get("os").toLowerCase()) {
                case "ios":
                    os = "1";
                    break;
                case "android":
                    os = "0";
                    break;
                default:
                    os = "0";
                    break;
            }
            url = url.replace("__OS__", os);
        } else {
            url = url.replace("__OS__", "0");
        }

        // 替换其它必填字段

        try {
            System.out.println("   eventB:" + url);

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

            Map<String, Object> result = OBJECT_MAPPER.readValue(body, new TypeReference<Map<String, Object>>() {});
            return Integer.valueOf(0).equals(result.get("code"));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        } catch (Exception e) {
            return false;
        }
    }

    private String trimTrailingSlash(String url) {
        if (url.endsWith("/")) {
            return url.substring(0, url.length() - 1);
        }

        return url;
    }

}
