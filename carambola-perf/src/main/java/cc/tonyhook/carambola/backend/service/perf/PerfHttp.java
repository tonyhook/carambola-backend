package cc.tonyhook.carambola.backend.service.perf;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * 媒体与监测方处理器共用的出站 HTTP。网络异常一律视为没有响应(返回 null),
 * 由调用方记为投递失败。
 */
public final class PerfHttp {

    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(3))
        .build();

    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(5);

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private PerfHttp() {
    }

    public static Response get(String url) {
        return send(HttpRequest.newBuilder()
            .uri(URI.create(url))
            .timeout(REQUEST_TIMEOUT)
            .GET()
            .build());
    }

    public static Response postJson(String url, String json) {
        return send(HttpRequest.newBuilder()
            .uri(URI.create(url))
            .timeout(REQUEST_TIMEOUT)
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(json, StandardCharsets.UTF_8))
            .build());
    }

    private static Response send(HttpRequest request) {
        try {
            HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
            return new Response(response.statusCode(), response.body());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return null;
        } catch (Exception e) {
            return null;
        }
    }

    public record Response(int status, String body) {

        public boolean isOk() {
            return status >= 200 && status < 300;
        }

        // 2xx 且响应体是 JSON 对象、field 字段的值为 expected(数字与字符串同等看待)
        public boolean isOkWith(String field, String expected) {
            Map<String, Object> result = json();
            return (result != null) && expected.equals(String.valueOf(result.get(field)));
        }

        // 2xx 时把响应体解析成 JSON 对象;非 2xx 或解析不了返回 null
        public Map<String, Object> json() {
            if (!isOk() || body == null) {
                return null;
            }
            try {
                return OBJECT_MAPPER.readValue(body, new TypeReference<Map<String, Object>>() {});
            } catch (Exception e) {
                return null;
            }
        }

        @Override
        public String toString() {
            return status + ":" + body;
        }

    }

}
