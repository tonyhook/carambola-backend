package cc.tonyhook.carambola.backend.controller.managed.ad;

import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.TimeZone;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import tools.jackson.databind.ObjectMapper;

import cc.tonyhook.carambola.backend.entity.ad.PerformanceClient;
import cc.tonyhook.carambola.backend.entity.ad.PerformanceClientBundle;
import cc.tonyhook.carambola.backend.entity.ad.PerformanceVendor;
import cc.tonyhook.carambola.backend.entity.ad.PerformanceVendorBundle;
import cc.tonyhook.carambola.backend.service.ad.AuthenticationService;
import cc.tonyhook.carambola.backend.service.ad.PerformanceService;
import cc.tonyhook.carambola.backend.service.scheduled.PerformanceCollectingService;
import cc.tonyhook.carambola.backend.service.shared.Query;

@RestController
public class PerformanceController {

    private final AuthenticationService authenticationService;
    private final PerformanceService performanceService;
    private final PerformanceCollectingService performanceCollectingService;

    public PerformanceController(
            AuthenticationService authenticationService,
            PerformanceService performanceService,
            PerformanceCollectingService performanceCollectingService
    ) {
        this.authenticationService = authenticationService;
        this.performanceService = performanceService;
        this.performanceCollectingService = performanceCollectingService;
    }

    @GetMapping(value = "/api/managed/performance/aggregate", produces = "application/json; charset=UTF-8")
    public ResponseEntity<?> aggregatePerformance(
            @RequestParam(defaultValue = "2024-01-31") String start,
            @RequestParam(defaultValue = "2024-01-31") String end,
            @RequestParam(defaultValue = "GMT+08:00") String timezone,
            Authentication authentication) {
        String username = authenticationService.getUsername(authentication);

        if (username == null) {
            try {
                SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd");
                TimeZone tz = TimeZone.getTimeZone(timezone);
                df.setTimeZone(tz);
                ZonedDateTime startTime = df.parse(start).toInstant().atZone(ZoneId.of(timezone));
                ZonedDateTime endTime = df.parse(end).toInstant().atZone(ZoneId.of(timezone));
                Long s = Timestamp.from(startTime.toInstant()).getTime();
                Long e = Timestamp.from(endTime.toInstant()).getTime();

                while (s <= e) {
                    performanceCollectingService.aggregatePerformance(
                        new Timestamp(s),
                        new Timestamp(s + 86399999),
                        timezone,
                        false);
                    performanceCollectingService.aggregatePerformanceBundle(
                        new Timestamp(s),
                        new Timestamp(s + 86399999),
                        timezone,
                        false);

                    s += 86400000;
                    System.gc();
                }

                return ResponseEntity.ok().build();
            } catch (Exception e) {
                return ResponseEntity.badRequest().build();
            }
        } else {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
    }

    @GetMapping(value = "/api/managed/performance/client", produces = "application/json; charset=UTF-8")
    public ResponseEntity<List<PerformanceClient>> getPerformanceClientList(
            @RequestParam(required = false) String query,
            @RequestParam(defaultValue = "quarter") String interval,
            @RequestParam(defaultValue = "false") Boolean expand,
            @RequestParam(defaultValue = "2024-01-31T00:00:00+08:00") String start,
            @RequestParam(defaultValue = "2024-01-31T00:00:00+08:00") String end,
            @RequestParam(defaultValue = "GMT+08:00") String timezone,
            Authentication authentication) {
        Query queryObject = null;
        Timestamp startTimestamp = null;
        Timestamp endTimestamp = null;
        if (query != null) {
            try {
                ObjectMapper objectMapper = new ObjectMapper();
                queryObject = objectMapper.readValue(query, Query.class);

                SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX");
                startTimestamp = new Timestamp(df.parse(start).getTime());
                endTimestamp = new Timestamp(df.parse(end).getTime());
            } catch (Exception e) {
                return ResponseEntity.badRequest().build();
            }
        }
        List<PerformanceClient> performanceClientList = performanceService.queryPerformanceClientList(
            authentication,
            queryObject,
            interval,
            expand,
            startTimestamp,
            endTimestamp,
            timezone);

        return ResponseEntity.ok().body(performanceClientList);
    }

    @GetMapping(value = "/api/managed/performance/vendor", produces = "application/json; charset=UTF-8")
    public ResponseEntity<List<PerformanceVendor>> getPerformanceVendorList(
            @RequestParam(required = false) String query,
            @RequestParam(defaultValue = "quarter") String interval,
            @RequestParam(defaultValue = "false") Boolean expand,
            @RequestParam(defaultValue = "2024-01-31T00:00:00+08:00") String start,
            @RequestParam(defaultValue = "2024-01-31T00:00:00+08:00") String end,
            @RequestParam(defaultValue = "GMT+08:00") String timezone,
            Authentication authentication) {
        Query queryObject = null;
        Timestamp startTimestamp = null;
        Timestamp endTimestamp = null;
        if (query != null) {
            try {
                ObjectMapper objectMapper = new ObjectMapper();
                queryObject = objectMapper.readValue(query, Query.class);

                SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX");
                startTimestamp = new Timestamp(df.parse(start).getTime());
                endTimestamp = new Timestamp(df.parse(end).getTime());
            } catch (Exception e) {
                return ResponseEntity.badRequest().build();
            }
        }
        List<PerformanceVendor> performanceVendorList = performanceService.queryPerformanceVendorList(
            authentication,
            queryObject,
            interval,
            expand,
            startTimestamp,
            endTimestamp,
            timezone);

        return ResponseEntity.ok().body(performanceVendorList);
    }

    @GetMapping(value = "/api/managed/performance/client/bundle", produces = "application/json; charset=UTF-8")
    public ResponseEntity<List<PerformanceClientBundle>> getPerformanceClientBundleList(
            @RequestParam(required = false) String query,
            @RequestParam(defaultValue = "quarter") String interval,
            @RequestParam(defaultValue = "false") Boolean expand,
            @RequestParam(defaultValue = "2024-01-31T00:00:00+08:00") String start,
            @RequestParam(defaultValue = "2024-01-31T00:00:00+08:00") String end,
            @RequestParam(defaultValue = "GMT+08:00") String timezone,
            Authentication authentication) {
        Query queryObject = null;
        Timestamp startTimestamp = null;
        Timestamp endTimestamp = null;
        if (query != null) {
            try {
                ObjectMapper objectMapper = new ObjectMapper();
                queryObject = objectMapper.readValue(query, Query.class);

                SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX");
                startTimestamp = new Timestamp(df.parse(start).getTime());
                endTimestamp = new Timestamp(df.parse(end).getTime());
            } catch (Exception e) {
                return ResponseEntity.badRequest().build();
            }
        }
        List<PerformanceClientBundle> performanceClientBundleList = performanceService.queryPerformanceClientBundleList(
            authentication,
            queryObject,
            interval,
            expand,
            startTimestamp,
            endTimestamp,
            timezone);

        return ResponseEntity.ok().body(performanceClientBundleList);
    }

    @GetMapping(value = "/api/managed/performance/vendor/bundle", produces = "application/json; charset=UTF-8")
    public ResponseEntity<List<PerformanceVendorBundle>> getPerformanceVendorBundleList(
            @RequestParam(required = false) String query,
            @RequestParam(defaultValue = "quarter") String interval,
            @RequestParam(defaultValue = "false") Boolean expand,
            @RequestParam(defaultValue = "2024-01-31T00:00:00+08:00") String start,
            @RequestParam(defaultValue = "2024-01-31T00:00:00+08:00") String end,
            @RequestParam(defaultValue = "GMT+08:00") String timezone,
            Authentication authentication) {
        Query queryObject = null;
        Timestamp startTimestamp = null;
        Timestamp endTimestamp = null;
        if (query != null) {
            try {
                ObjectMapper objectMapper = new ObjectMapper();
                queryObject = objectMapper.readValue(query, Query.class);

                SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX");
                startTimestamp = new Timestamp(df.parse(start).getTime());
                endTimestamp = new Timestamp(df.parse(end).getTime());
            } catch (Exception e) {
                return ResponseEntity.badRequest().build();
            }
        }
        List<PerformanceVendorBundle> performanceVendorBundleList = performanceService.queryPerformanceVendorBundleList(
            authentication,
            queryObject,
            interval,
            expand,
            startTimestamp,
            endTimestamp,
            timezone);

        return ResponseEntity.ok().body(performanceVendorBundleList);
    }

    @GetMapping(value = "/api/managed/performance/client/download", produces = "application/json; charset=UTF-8")
    public ResponseEntity<byte[]> downloadPerformanceClientList(
            @RequestParam(required = false) String query,
            @RequestParam(defaultValue = "quarter") String interval,
            @RequestParam(defaultValue = "2024-01-31T00:00:00+08:00") String start,
            @RequestParam(defaultValue = "2024-01-31T00:00:00+08:00") String end,
            @RequestParam(defaultValue = "GMT+08:00") String timezone,
            Authentication authentication) {
        Query queryObject = null;
        Timestamp startTimestamp = null;
        Timestamp endTimestamp = null;
        if (query != null) {
            try {
                ObjectMapper objectMapper = new ObjectMapper();
                queryObject = objectMapper.readValue(query, Query.class);

                SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX");
                startTimestamp = new Timestamp(df.parse(start).getTime());
                endTimestamp = new Timestamp(df.parse(end).getTime());
            } catch (Exception e) {
                return ResponseEntity.badRequest().build();
            }
        }
        byte[] performanceList = performanceService.downloadPerformanceClientList(
            authentication,
            queryObject,
            interval,
            startTimestamp,
            endTimestamp,
            timezone);

        return ResponseEntity.ok()
            .contentType(MediaType.parseMediaType("application/vnd.ms-excel"))
            .header("Content-Disposition", "attachment; filename=performance-client.xlsx")
            .body(performanceList);
    }

    @GetMapping(value = "/api/managed/performance/vendor/download", produces = "application/json; charset=UTF-8")
    public ResponseEntity<byte[]> downloadPerformanceVendorList(
            @RequestParam(required = false) String query,
            @RequestParam(defaultValue = "quarter") String interval,
            @RequestParam(defaultValue = "2024-01-31T00:00:00+08:00") String start,
            @RequestParam(defaultValue = "2024-01-31T00:00:00+08:00") String end,
            @RequestParam(defaultValue = "GMT+08:00") String timezone,
            Authentication authentication) {
        Query queryObject = null;
        Timestamp startTimestamp = null;
        Timestamp endTimestamp = null;
        if (query != null) {
            try {
                ObjectMapper objectMapper = new ObjectMapper();
                queryObject = objectMapper.readValue(query, Query.class);

                SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX");
                startTimestamp = new Timestamp(df.parse(start).getTime());
                endTimestamp = new Timestamp(df.parse(end).getTime());
            } catch (Exception e) {
                return ResponseEntity.badRequest().build();
            }
        }
        byte[] performanceList = performanceService.downloadPerformanceVendorList(
            authentication,
            queryObject,
            interval,
            startTimestamp,
            endTimestamp,
            timezone);

        return ResponseEntity.ok()
            .contentType(MediaType.parseMediaType("application/vnd.ms-excel"))
            .header("Content-Disposition", "attachment; filename=performance-vendor.xlsx")
            .body(performanceList);
    }

    @GetMapping(value = "/api/managed/performance/client/bundle/download", produces = "application/json; charset=UTF-8")
    public ResponseEntity<byte[]> downloadPerformanceClientBundleList(
            @RequestParam(required = false) String query,
            @RequestParam(defaultValue = "quarter") String interval,
            @RequestParam(defaultValue = "2024-01-31T00:00:00+08:00") String start,
            @RequestParam(defaultValue = "2024-01-31T00:00:00+08:00") String end,
            @RequestParam(defaultValue = "GMT+08:00") String timezone,
            Authentication authentication) {
        Query queryObject = null;
        Timestamp startTimestamp = null;
        Timestamp endTimestamp = null;
        if (query != null) {
            try {
                ObjectMapper objectMapper = new ObjectMapper();
                queryObject = objectMapper.readValue(query, Query.class);

                SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX");
                startTimestamp = new Timestamp(df.parse(start).getTime());
                endTimestamp = new Timestamp(df.parse(end).getTime());
            } catch (Exception e) {
                return ResponseEntity.badRequest().build();
            }
        }
        byte[] performanceList = performanceService.downloadPerformanceClientBundleList(
            authentication,
            queryObject,
            interval,
            startTimestamp,
            endTimestamp,
            timezone);

        return ResponseEntity.ok()
            .contentType(MediaType.parseMediaType("application/vnd.ms-excel"))
            .header("Content-Disposition", "attachment; filename=bundle-client.xlsx")
            .body(performanceList);
    }

    @GetMapping(value = "/api/managed/performance/vendor/bundle/download", produces = "application/json; charset=UTF-8")
    public ResponseEntity<byte[]> downloadPerformanceVendorBundleList(
            @RequestParam(required = false) String query,
            @RequestParam(defaultValue = "quarter") String interval,
            @RequestParam(defaultValue = "2024-01-31T00:00:00+08:00") String start,
            @RequestParam(defaultValue = "2024-01-31T00:00:00+08:00") String end,
            @RequestParam(defaultValue = "GMT+08:00") String timezone,
            Authentication authentication) {
        Query queryObject = null;
        Timestamp startTimestamp = null;
        Timestamp endTimestamp = null;
        if (query != null) {
            try {
                ObjectMapper objectMapper = new ObjectMapper();
                queryObject = objectMapper.readValue(query, Query.class);

                SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX");
                startTimestamp = new Timestamp(df.parse(start).getTime());
                endTimestamp = new Timestamp(df.parse(end).getTime());
            } catch (Exception e) {
                return ResponseEntity.badRequest().build();
            }
        }
        byte[] performanceList = performanceService.downloadPerformanceVendorBundleList(
            authentication,
            queryObject,
            interval,
            startTimestamp,
            endTimestamp,
            timezone);

        return ResponseEntity.ok()
            .contentType(MediaType.parseMediaType("application/vnd.ms-excel"))
            .header("Content-Disposition", "attachment; filename=bundle-vendor.xlsx")
            .body(performanceList);
    }
}
