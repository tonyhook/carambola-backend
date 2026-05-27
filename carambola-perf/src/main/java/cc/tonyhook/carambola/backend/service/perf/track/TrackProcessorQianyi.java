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

@Component("trackQianyi")
public class TrackProcessorQianyi extends TrackProcessor {

    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(3))
        .build();
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Value("${app.server}")
    private String appServer;

    @Override
    public Callback callback(Map<String, String> queries) {
        Callback c = new Callback();
        c.setTrack("qianyi");

        if (!isValid(queries, "davidia_id")) {
            return null;
        }
        c.setSerialno(queries.get("davidia_id"));

        if (!isValid(queries, "transformType")) {
            return null;
        }
        switch (queries.get("transformType")) {
            case "1":
                c.setEvent("2002");
                break;
            case "2":
                c.setEvent("2003");
                break;
            case "3":
                c.setEvent("2004");
                break;
            case "4":
                c.setEvent("2015");
                break;
            case "5":
                c.setEvent("2013");
                break;
            default:
                return null;
        }

        c.setQueries(queries);

        return c;
    }

    public Boolean event(Event event, ClientChannel cc) {
        String url = "https://ad.qianyichuanmei.com.cn/cpa/ocpx/" +
            "__action__" +
            "?fid=__fid__&rid=__request_id__&cb=__callback__&ua=__user_agent__&tms=__tms__&ip=__ip__&ipv6=__ipv6__&imei=__imei__&imeimd5=__imei_md5__&oid=__oaid__&oidmd5=__oaid_md5__&aid=__android_id__&aidmd5=__android_id_md5__&idfa=__idfa__&idfamd5=__idfa_md5__&caid1=__caid1__&caidVer1=__caid_ver1__&caid2=__caid2__&caidVer2=__caid_ver2__&mac=__mac__&macmd5=__mac_md5__&ost=__os_type__&aaid=__aaid__";

        String callbackUrl = trimTrailingSlash(appServer) + "/api/open/callback?davidia_track=qianyi&davidia_id=" + event.getSerialno();
        url = url.replace("__callback__", URLEncoder.encode(callbackUrl, StandardCharsets.UTF_8));

        url = url.replace("__fid__", URLEncoder.encode(cc.getTrackCode(), StandardCharsets.UTF_8));

        if (event.getEvent().equals("0002")) {
            url = url.replace("__action__", "click");
        } else {
            return false;
        }

        // 替换事件字段
        url = url.replace("__REQ_ID__", event.getSerialno());
        if (isValid(event.getQueries(), "ts")) {
            url = url.replace("__tms__", event.getQueries().get("ts"));
        }

        // 替换设备字段
        if (isValid(event.getQueries(), "imei")) {
            url = url.replace("__imei__", event.getQueries().get("imei"));
        }
        if (isValid(event.getQueries(), "imei_md5")) {
            url = url.replace("__imei_md5__", event.getQueries().get("imei_md5"));
        }
        if (isValid(event.getQueries(), "oaid")) {
            url = url.replace("__oaid__", event.getQueries().get("oaid"));
        }
        if (isValid(event.getQueries(), "oaid_md5")) {
            url = url.replace("__oaid_md5__", event.getQueries().get("oaid_md5"));
        }
        if (isValid(event.getQueries(), "android_id")) {
            url = url.replace("__android_id__", event.getQueries().get("android_id"));
        }
        if (isValid(event.getQueries(), "android_id_md5")) {
            url = url.replace("__android_id_md5__", event.getQueries().get("android_id_md5"));
        }
        if (isValid(event.getQueries(), "idfa")) {
            url = url.replace("__idfa__", event.getQueries().get("idfa"));
        }
        if (isValid(event.getQueries(), "idfa_md5")) {
            url = url.replace("__idfa_md5__", event.getQueries().get("idfa_md5"));
        }
        if (isValid(event.getQueries(), "caid")) {
            url = url.replace("__caid1__", event.getQueries().get("caid"));
        }
        if (isValid(event.getQueries(), "caid1")) {
            url = url.replace("__caid1__", event.getQueries().get("caid1"));
        }
        if (isValid(event.getQueries(), "caid1_v")) {
            url = url.replace("__caid_ver1__", event.getQueries().get("caid1_v"));
        }
        if (isValid(event.getQueries(), "caid2")) {
            url = url.replace("__caid2__", event.getQueries().get("caid2"));
        }
        if (isValid(event.getQueries(), "caid2_v")) {
            url = url.replace("__caid_ver2__", event.getQueries().get("caid2_v"));
        }
        if (isValid(event.getQueries(), "aaid")) {
            url = url.replace("__aaid__", event.getQueries().get("aaid"));
        }
        if (isValid(event.getQueries(), "mac")) {
            url = url.replace("__mac__", event.getQueries().get("mac"));
        }
        if (isValid(event.getQueries(), "mac_md5")) {
            url = url.replace("__mac_md5__", event.getQueries().get("mac_md5"));
        }
        if (isValid(event.getQueries(), "ip")) {
            url = url.replace("__ip__", event.getQueries().get("ip"));
        }
        if (isValid(event.getQueries(), "ipv6")) {
            url = url.replace("__ipv6__", URLEncoder.encode(event.getQueries().get("ipv6"), StandardCharsets.UTF_8));
        }
        if (isValid(event.getQueries(), "ua")) {
            url = url.replace("__user_agent__", event.getQueries().get("ua"));
        }
        if (isValid(event.getQueries(), "os")) {
            String osType;
            switch (event.getQueries().get("os").toLowerCase()) {
                case "ios":
                    osType = "1";
                    break;
                case "android":
                    osType = "2";
                    break;
                default:
                    osType = "0";
                    break;
            }
            url = url.replace("__os_type__", osType);
        } else {
            url = url.replace("__os_type__", "0");
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
