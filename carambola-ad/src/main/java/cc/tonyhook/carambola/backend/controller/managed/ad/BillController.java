package cc.tonyhook.carambola.backend.controller.managed.ad;

import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.TimeZone;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import cc.tonyhook.carambola.backend.entity.ad.Bill;
import cc.tonyhook.carambola.backend.entity.ad.ClientPort;
import cc.tonyhook.carambola.backend.entity.ad.Medium;
import cc.tonyhook.carambola.backend.entity.ad.Sign;
import cc.tonyhook.carambola.backend.service.ad.AuthenticationService;
import cc.tonyhook.carambola.backend.service.ad.BillService;
import cc.tonyhook.carambola.backend.service.ad.ClientPortService;
import cc.tonyhook.carambola.backend.service.scheduled.ClientReportCollectingService;
import cc.tonyhook.carambola.backend.service.shared.Query;

@RestController
public class BillController {

    private final AuthenticationService authenticationService;
    private final ClientPortService clientPortService;
    private final BillService billService;
    private final ClientReportCollectingService clientReportService;

    public BillController(
            AuthenticationService authenticationService,
            ClientPortService clientPortService,
            BillService billService,
            ClientReportCollectingService clientReportService
    ) {
        this.authenticationService = authenticationService;
        this.clientPortService = clientPortService;
        this.billService = billService;
        this.clientReportService = clientReportService;
    }

    @GetMapping(value = "/api/managed/bill/split", produces = "application/json; charset=UTF-8")
    public ResponseEntity<?> splitBillList(
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

                List<ClientPort> clientPortList = clientPortService.getClientPortList(authentication);
                List<Integer> clientPortIdList = clientPortList.stream().map(ClientPort::getId).collect(Collectors.toList());

                List<Bill> billList = billService.getBillList(clientPortIdList, Timestamp.from(startTime.toInstant()), Timestamp.from(endTime.toInstant()));
                billService.splitBillList(authentication, billList, timezone);

                return ResponseEntity.ok().build();
            } catch (Exception e) {
                e.printStackTrace();
                return ResponseEntity.badRequest().build();
            }
        } else {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
    }

    @GetMapping(value = "/api/managed/bill", produces = "application/json; charset=UTF-8")
    public ResponseEntity<List<Bill>> getBillList(
            @RequestParam(required = false) String query,
            @RequestParam(defaultValue = "day") String interval,
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
        List<Bill> billList = billService.queryBillList(
            authentication,
            queryObject,
            interval,
            startTimestamp,
            endTimestamp,
            timezone);

        return ResponseEntity.ok().body(billList);
    }

    @PostMapping(value = "/api/managed/bill", produces = "application/json; charset=UTF-8")
    public ResponseEntity<List<Bill>> addBillList(
            @RequestBody List<Bill> billList,
            @RequestParam(defaultValue = "GMT+08:00") String timezone,
            Authentication authentication) {
        for (Bill bill : billList) {
            bill.setStatus(Bill.BILL_STATUS_MANUAL);
        }
        List<Bill> processedBillList = billService.addBillList(authentication, billList, timezone);

        return ResponseEntity.ok().body(processedBillList);
    }

    @PostMapping(value = "/api/managed/bill/remove", produces = "application/json; charset=UTF-8")
    public ResponseEntity<List<Bill>> removeBillList(
            @RequestBody List<Bill> billList,
            @RequestParam(defaultValue = "GMT+08:00") String timezone,
            Authentication authentication) {
        List<Bill> processedBillList = billService.removeBillList(authentication, billList, timezone);

        return ResponseEntity.ok().body(processedBillList);
    }

    @PostMapping(value = "/api/managed/bill/upload", consumes = "multipart/form-data")
    public ResponseEntity<String> uploadBill(
            @RequestParam(defaultValue = "GMT+08:00") String timezone,
            @RequestPart(name = "upload") MultipartFile upload,
            Authentication authentication) {
        ObjectNode node = billService.uploadBill(authentication, upload, timezone);

        return ResponseEntity.ok(node.toString());
    }

    @GetMapping(value = "/api/managed/bill/download", produces = "application/vnd.ms-excel")
    public ResponseEntity<byte[]> downloadBill(
            @RequestParam(required = false) String query,
            @RequestParam(defaultValue = "day") String interval,
            @RequestParam(defaultValue = "all") String aggregateUpstream,
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
        byte[] billList = billService.downloadBill(
            authentication,
            queryObject,
            interval,
            aggregateUpstream,
            startTimestamp,
            endTimestamp,
            timezone);

        return ResponseEntity.ok()
            .contentType(MediaType.parseMediaType("application/vnd.ms-excel"))
            .header("Content-Disposition", "attachment; filename=bill-client.xlsx")
            .body(billList);
    }

    @GetMapping(value = "/api/managed/bill/sync", produces = "application/json; charset=UTF-8")
    public ResponseEntity<List<Bill>> syncBill(
            @RequestParam(required = true) String client,
            @RequestParam(required = true) String start,
            @RequestParam(required = true) String end) {
        List<Bill> billList = clientReportService.syncClientReport(client, start, end);

        return ResponseEntity.ok(billList);
    }

    @GetMapping(value = "/api/managed/medium/client", produces = "application/json; charset=UTF-8")
    public ResponseEntity<List<Medium>> getMediumListClient(
            @RequestParam(required = false) String query,
            @RequestParam(defaultValue = "day") String interval,
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
        List<Medium> mediumList = billService.queryMediumListClient(
            authentication,
            queryObject,
            interval,
            startTimestamp,
            endTimestamp,
            timezone);

        return ResponseEntity.ok().body(mediumList);
    }

    @GetMapping(value = "/api/managed/medium/vendor", produces = "application/json; charset=UTF-8")
    public ResponseEntity<List<Medium>> getMediumListVendor(
            @RequestParam(required = false) String query,
            @RequestParam(defaultValue = "day") String interval,
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
        List<Medium> mediumList = billService.queryMediumListVendor(
            authentication,
            queryObject,
            interval,
            startTimestamp,
            endTimestamp,
            timezone);

        return ResponseEntity.ok().body(mediumList);
    }

    @GetMapping(value = "/api/managed/sign", produces = "application/json; charset=UTF-8")
    public ResponseEntity<List<Sign>> getSignList(
            @RequestParam(required = false) String query,
            @RequestParam(defaultValue = "day") String interval,
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
        List<Sign> signList = billService.querySignList(
            authentication,
            queryObject,
            interval,
            startTimestamp,
            endTimestamp,
            timezone);

        return ResponseEntity.ok().body(signList);
    }

    @PostMapping(value = "/api/managed/sign", produces = "application/json; charset=UTF-8")
    public ResponseEntity<List<Sign>> addSignList(
            @RequestBody List<Sign> signList,
            @RequestParam(defaultValue = "GMT+08:00") String timezone,
            Authentication authentication) {
        for (Sign sign : signList) {
            sign.setStatus(Sign.SIGN_STATUS_CREATED);
        }
        List<Sign> processedSignList = billService.addSignList(authentication, signList, timezone);

        return ResponseEntity.ok().body(processedSignList);
    }

    @PostMapping(value = "/api/managed/sign/remove", produces = "application/json; charset=UTF-8")
    public ResponseEntity<List<Sign>> removeSignList(
            @RequestBody List<Sign> signList,
            @RequestParam(defaultValue = "GMT+08:00") String timezone,
            Authentication authentication) {
        List<Sign> processedSignList = billService.removeSignList(authentication, signList, timezone);

        return ResponseEntity.ok().body(processedSignList);
    }

    @PostMapping(value = "/api/managed/sign/sign", produces = "application/json; charset=UTF-8")
    public ResponseEntity<List<Sign>> signSignList(
            @RequestBody List<Sign> signList,
            @RequestParam(defaultValue = "GMT+08:00") String timezone,
            Authentication authentication) {
        List<Sign> processedSignList = billService.addSignList(authentication, signList, timezone);

        return ResponseEntity.ok().body(processedSignList);
    }

    @PostMapping(value = "/api/managed/sign/revoke", produces = "application/json; charset=UTF-8")
    public ResponseEntity<List<Sign>> revokeSignList(
            @RequestBody List<Sign> signList,
            @RequestParam(defaultValue = "GMT+08:00") String timezone,
            Authentication authentication) {
        List<Sign> processedSignList = billService.addSignList(authentication, signList, timezone);

        return ResponseEntity.ok().body(processedSignList);
    }

    @PostMapping(value = "/api/managed/sign/upload", consumes = "multipart/form-data")
    public ResponseEntity<String> uploadSign(
            @RequestParam(defaultValue = "GMT+08:00") String timezone,
            @RequestPart(name = "upload") MultipartFile upload,
            Authentication authentication) {
        ObjectNode node = billService.uploadSign(authentication, upload, timezone);

        return ResponseEntity.ok(node.toString());
    }

    @GetMapping(value = "/api/managed/sign/download", produces = "application/vnd.ms-excel")
    public ResponseEntity<byte[]> downloadSign(
            @RequestParam(required = false) String query,
            @RequestParam(defaultValue = "day") String interval,
            @RequestParam(defaultValue = "all") String aggregateDownstream,
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
        byte[] signList = billService.downloadSign(
            authentication,
            queryObject,
            interval,
            aggregateDownstream,
            startTimestamp,
            endTimestamp,
            timezone);

        return ResponseEntity.ok()
            .contentType(MediaType.parseMediaType("application/vnd.ms-excel"))
            .header("Content-Disposition", "attachment; filename=sign-vendor.xlsx")
            .body(signList);
    }

}
