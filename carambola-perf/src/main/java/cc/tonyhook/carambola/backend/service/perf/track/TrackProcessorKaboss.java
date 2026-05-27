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

@Component("trackKaboss")
public class TrackProcessorKaboss extends TrackProcessor {

    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(3))
        .build();
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Value("${app.server}")
    private String appServer;

    @Override
    public Callback callback(Map<String, String> queries) {
        Callback c = new Callback();
        c.setTrack("kaboss");

        if (!isValid(queries, "davidia_id")) {
            return null;
        }
        c.setSerialno(queries.get("davidia_id"));

        if (!isValid(queries, "conv_type")) {
            return null;
        } else {
            switch (queries.get("conv_type")) {
                case "active":
                    c.setEvent("2002");
                    break;
                case "register":
                    c.setEvent("2003");
                    break;
                case "wake_up":
                    c.setEvent("2013");
                    break;
                case "retain":
                    c.setEvent("2004");
                    break;
                case "pay":
                    c.setEvent("2015");
                    break;
                case "key_behavior":
                    c.setEvent("2012");
                    break;
                case "eft_login":
                    c.setEvent("2014");
                    break;
                default:
                    return null;
            }
        }

        c.setQueries(queries);

        return c;
    }

    public Boolean event(Event event, ClientChannel cc) {
        String url = "https://ad.kaboss.cn/media/monitor/commonClick/distributor?imei=__IMEI__&imei_md5=__IMEI_MD5__&oaid=__OAID__&oaid_md5=__OAID_MD5__&android_id=__ANDROID_ID__&android_id_md5=__ANDROID_ID_MD5__&callback_url=__CALLBACK_URL__&project_id=__PROJECT_ID__&plan_id=__PLAN_ID__&plan_name=__PLAN_NAME__&unit_id=__UNIT_ID__&unit_name=__UNIT_NAME__&creative_id=__CID__&creative_name=__CID_NAME__&idfa=__IDFA__&idfa_md5=__IDFA_MD5__&caid=__CAID__&reqid=__REQ_ID__&trace_id=__TRACE_ID__&click_time=__TS__&client_ip=__IP__&ua=__UA__&resource_id=__RID__&os=__OS__&os_version=__OS_VERSION__&model=__MODEL__&advertiser_id=__USER_ID__&bchannel=__BCHANNEL__&behavior=__BEHAVIOR__";

        String callbackUrl = trimTrailingSlash(appServer) + "/api/open/callback?davidia_track=kaboss&davidia_id=" + event.getSerialno();
        url = url.replace("__CALLBACK_URL__", URLEncoder.encode(callbackUrl, StandardCharsets.UTF_8));

        url = url.replace("__BCHANNEL__", URLEncoder.encode(cc.getTrackCode(), StandardCharsets.UTF_8));

        if (event.getEvent().equals("0002")) {
            url = url.replace("__BEHAVIOR__", "CLICK");
        } else {
            return false;
        }

        // 替换事件字段
        url = url.replace("__REQ_ID__", event.getSerialno());
        if (isValid(event.getQueries(), "ts")) {
            url = url.replace("__TS__", event.getQueries().get("ts"));
        }

        // 替换设备字段
        if (isValid(event.getQueries(), "imei")) {
            url = url.replace("__IMEI__", event.getQueries().get("imei"));
        }
        if (isValid(event.getQueries(), "imei_md5")) {
            url = url.replace("__IMEI_MD5__", event.getQueries().get("imei_md5"));
        }
        if (isValid(event.getQueries(), "oaid")) {
            url = url.replace("__OAID__", event.getQueries().get("oaid"));
        }
        if (isValid(event.getQueries(), "oaid_md5")) {
            url = url.replace("__OAID_MD5__", event.getQueries().get("oaid_md5"));
        }
        if (isValid(event.getQueries(), "android_id")) {
            url = url.replace("__ANDROID_ID__", event.getQueries().get("android_id"));
        }
        if (isValid(event.getQueries(), "android_id_md5")) {
            url = url.replace("__ANDROID_ID_MD5__", event.getQueries().get("android_id_md5"));
        }
        if (isValid(event.getQueries(), "idfa")) {
            url = url.replace("__IDFA__", event.getQueries().get("idfa"));
        }
        if (isValid(event.getQueries(), "idfa_md5")) {
            url = url.replace("__IDFA_MD5__", event.getQueries().get("idfa_md5"));
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
        if (isValid(event.getQueries(), "ipv6")) {
            url = url.replace("__IPV6__", URLEncoder.encode(event.getQueries().get("ipv6"), StandardCharsets.UTF_8));
        }
        if (isValid(event.getQueries(), "ua")) {
            url = url.replace("__UA__", URLEncoder.encode(event.getQueries().get("ua"), StandardCharsets.UTF_8));
        }
        if (isValid(event.getQueries(), "os")) {
            url = url.replace("__OS__", event.getQueries().get("os").toLowerCase());
        } else {
            url = url.replace("__OS__", "android");
        }
        if (isValid(event.getQueries(), "os_v")) {
            url = url.replace("__OS_VERSION__", event.getQueries().get("os_v"));
        }

        // 替换其它必填字段
        if (isValid(event.getQueries(), "click_id")) {
            url = url.replace("__TRACE_ID__", event.getQueries().get("click_id"));
        } else {
            url = url.replace("__TRACE_ID__", "trace_id");
        }

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
