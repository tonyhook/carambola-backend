package cc.tonyhook.carambola.backend.service.scheduled;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TimeZone;
import java.util.TreeMap;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import cc.tonyhook.carambola.backend.entity.ad.Bill;
import cc.tonyhook.carambola.backend.entity.ad.Client;
import cc.tonyhook.carambola.backend.entity.ad.ClientMedia;
import cc.tonyhook.carambola.backend.service.ad.BillService;
import cc.tonyhook.carambola.backend.service.ad.ClientMediaService;
import cc.tonyhook.carambola.backend.service.ad.ClientService;
import cc.tonyhook.carambola.backend.service.shared.HashHelperService;
import cc.tonyhook.carambola.backend.service.shared.ParameterStringBuilder;

@Service
public class ClientReportCollectingService {

    private final ClientService clientService;
    private final ClientMediaService clientMediaService;

    private final BillService billService;

    public ClientReportCollectingService(ClientService clientService, ClientMediaService clientMediaService, BillService billService) {
        this.clientService = clientService;
        this.clientMediaService = clientMediaService;
        this.billService = billService;
    }

    @Scheduled(cron = "0 0 8 * * ?")
    public void syncClientReport() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        Date date = new Date();
        date.setTime(date.getTime() - 24 * 60 * 60 * 1000);
        String start = sdf.format(date);
        String end = sdf.format(date);

        syncClientReport("kkmh", start, end);
        syncClientReport("mobrtb", start, end);
    }

    public List<Bill> syncClientReport(String client, String start, String end) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        try {
            Date startDate = sdf.parse(start);
            Date endDate = sdf.parse(end);

            if (startDate.after(endDate)) {
                return null;
            }

            if (client.equals("kkmh")) {
                return syncClientReportKkmh(start, end);
            }

            if (client.equals("mobrtb")) {
                return syncClientReportMobrtb(start, end);
            }

            return null;
        } catch (Exception e) {
            return null;
        }
    }

    private List<Bill> syncClientReportKkmh(String start, String end) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        TimeZone tz = TimeZone.getTimeZone("GMT+08:00");
        sdf.setTimeZone(tz);

        Date startDate = new Date();
        Date endDate = new Date();
        try {
            startDate = sdf.parse(start);
            endDate = sdf.parse(end);
        } catch (Exception e) {
            return null;
        }

        Client client = clientService.getClient(null, "kkmh");
        if (client == null) {
            return null;
        }

        List<ClientMedia> clientMediaList = clientMediaService.getClientMediaList(null, client);

        List<Bill> billList = new ArrayList<Bill>();
        Set<String> clientMediaCodes = new HashSet<String>();
        for (ClientMedia clientMedia : clientMediaList) {
            if (clientMediaCodes.contains(clientMedia.getCode())) {
                continue;
            }
            clientMediaCodes.add(clientMedia.getCode());

            Date date = new Date(startDate.getTime());
            while (!date.after(endDate)) {
                TreeMap<String, String> params = new TreeMap<>();
                params.put("start_date", sdf.format(date));
                params.put("end_date", sdf.format(date));
                params.put("app_id", clientMedia.getCode());
                params.put("timestamp", Long.toString(System.currentTimeMillis() / 1000));

                String raw = ParameterStringBuilder.getCodeString(params) + clientMedia.getSecret();
                params.put("sign", HashHelperService.hash(raw.getBytes(), "MD5"));

                try {
                    ObjectMapper mapper = new ObjectMapper();

                    URL url = new URI("https", "api.kkmh.com", "/ad/open-api/report/union",
                        ParameterStringBuilder.getParamsString(params), null).toURL();

                    HttpURLConnection con = (HttpURLConnection) url.openConnection();
                    con.setRequestMethod("GET");

                    if (con.getResponseCode() == HttpURLConnection.HTTP_OK) {
                        BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
                        String inputLine;
                        StringBuffer content = new StringBuffer();
                        while ((inputLine = in.readLine()) != null) {
                            content.append(inputLine);
                        }
                        in.close();
                        con.disconnect();

                        JsonNode resp = mapper.readTree(content.toString());
                        if (resp.get("code") != null) {
                            String code = resp.get("code").asText();
                            if (code.equals("200") && resp.get("data").isArray()) {
                                for (JsonNode node : resp.get("data")) {
                                    Bill bill = new Bill();
                                    bill.setDate(new Timestamp(date.getTime()));
                                    bill.setTagId(node.get("pos_id").asText());
                                    bill.setImpression(node.get("view").asLong());
                                    bill.setClick(node.get("click").asLong());
                                    bill.setCost(Math.round(node.get("revenue").asDouble() * 100000));
                                    bill.setStatus(Bill.BILL_STATUS_FETCHED);

                                    billList.add(bill);
                                }
                            }
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }

                date = new Date(date.getTime() + 24 * 60 * 60 * 1000);
            }
        }

        billService.addBillList(null, billList, "GMT+08:00");

        return billList;
    }

    private List<Bill> syncClientReportMobrtb(String start, String end) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        TimeZone tz = TimeZone.getTimeZone("GMT+08:00");
        sdf.setTimeZone(tz);

        Date startDate = new Date();
        Date endDate = new Date();
        try {
            startDate = sdf.parse(start);
            endDate = sdf.parse(end);
        } catch (Exception e) {
            return null;
        }

        Client client = clientService.getClient(null, "mobrtb");
        if (client == null) {
            return null;
        }

        List<ClientMedia> clientMediaList = clientMediaService.getClientMediaList(null, client);

        List<Bill> billList = new ArrayList<Bill>();
        Set<String> clientMediaCodes = new HashSet<String>();
        for (ClientMedia clientMedia : clientMediaList) {
            if (clientMediaCodes.contains(clientMedia.getCode())) {
                continue;
            }
            clientMediaCodes.add(clientMedia.getCode());

            Date date = new Date(startDate.getTime());
            while (!date.after(endDate)) {
                TreeMap<String, String> params = new TreeMap<>();
                params.put("from", sdf.format(date));
                params.put("to", sdf.format(date));

                String raw = clientMedia.getSecret() + ParameterStringBuilder.getCodeString(params);

                try {
                    ObjectMapper mapper = new ObjectMapper();

                    URL url = new URI("https", "api.mobrtb.com", "/reports/" + clientMedia.getCode(),
                        ParameterStringBuilder.getParamsString(params), null).toURL();

                    HttpURLConnection con = (HttpURLConnection) url.openConnection();
                    con.setRequestMethod("GET");
                    con.setRequestProperty ("X-SIGNATURE", HashHelperService.hash(raw.getBytes(), "MD5"));

                    if (con.getResponseCode() == HttpURLConnection.HTTP_OK) {
                        BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
                        String inputLine;
                        StringBuffer content = new StringBuffer();
                        while ((inputLine = in.readLine()) != null) {
                            content.append(inputLine);
                        }
                        in.close();
                        con.disconnect();

                        JsonNode resp = mapper.readTree(content.toString());
                        if (resp.get("code") != null) {
                            String code = resp.get("code").asText();
                            if (code.equals("200") && resp.get("data").isArray()) {
                                for (JsonNode node : resp.get("data")) {
                                    Bill bill = new Bill();
                                    bill.setDate(new Timestamp(date.getTime()));
                                    bill.setTagId(node.get("monetizer_app_ad_unit_token").asText());
                                    bill.setRequest(node.get("requests").asLong());
                                    bill.setImpression(node.get("impressions").asLong());
                                    bill.setClick(node.get("clicks").asLong());
                                    bill.setCost(Math.round(node.get("income").asDouble() * 100000));
                                    bill.setStatus(Bill.BILL_STATUS_FETCHED);

                                    billList.add(bill);
                                }
                            }
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }

                date = new Date(date.getTime() + 24 * 60 * 60 * 1000);
            }
        }

        billService.addBillList(null, billList, "GMT+08:00");

        return billList;
    }

}
