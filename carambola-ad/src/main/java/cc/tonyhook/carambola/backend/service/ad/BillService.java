package cc.tonyhook.carambola.backend.service.ad;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DataFormat;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

import cc.tonyhook.carambola.backend.dao.ad.BillRepository;
import cc.tonyhook.carambola.backend.dao.ad.MediumRepository;
import cc.tonyhook.carambola.backend.dao.ad.PerformanceClientDayRepository;
import cc.tonyhook.carambola.backend.dao.ad.PerformanceVendorDayRepository;
import cc.tonyhook.carambola.backend.dao.ad.SignRepository;
import cc.tonyhook.carambola.backend.entity.ad.Bill;
import cc.tonyhook.carambola.backend.entity.ad.Client;
import cc.tonyhook.carambola.backend.entity.ad.ClientPort;
import cc.tonyhook.carambola.backend.entity.ad.Connection;
import cc.tonyhook.carambola.backend.entity.ad.Medium;
import cc.tonyhook.carambola.backend.entity.ad.PerformanceClientDay;
import cc.tonyhook.carambola.backend.entity.ad.PerformanceVendor;
import cc.tonyhook.carambola.backend.entity.ad.PerformanceVendorDay;
import cc.tonyhook.carambola.backend.entity.ad.PerformanceView;
import cc.tonyhook.carambola.backend.entity.ad.Sign;
import cc.tonyhook.carambola.backend.entity.ad.TenantUser;
import cc.tonyhook.carambola.backend.entity.ad.Vendor;
import cc.tonyhook.carambola.backend.entity.ad.VendorPort;
import cc.tonyhook.carambola.backend.service.shared.CellService;
import cc.tonyhook.carambola.backend.service.shared.Query;
import jakarta.transaction.Transactional;

@Service
public class BillService {

    private final BillRepository billRepository;
    private final MediumRepository mediumRepository;
    private final SignRepository signRepository;
    private final PerformanceClientDayRepository performanceClientDayRepository;
    private final PerformanceVendorDayRepository performanceVendorDayRepository;

    private final CellService cellService;
    private final ClientPortService clientPortService;
    private final VendorPortService vendorPortService;
    private final PartnerService partnerService;
    private final ConnectionService connectionService;
    private final PerformanceService performanceService;
    private final AuthenticationService authenticationService;

    public BillService(
            BillRepository billRepository,
            MediumRepository mediumRepository,
            SignRepository signRepository,
            PerformanceClientDayRepository performanceClientDayRepository,
            PerformanceVendorDayRepository performanceVendorDayRepository,
            CellService cellService,
            ClientPortService clientPortService,
            VendorPortService vendorPortService,
            PartnerService partnerService,
            ConnectionService connectionService,
            PerformanceService performanceService,
            AuthenticationService authenticationService
    ) {
        this.billRepository = billRepository;
        this.mediumRepository = mediumRepository;
        this.signRepository = signRepository;
        this.performanceClientDayRepository = performanceClientDayRepository;
        this.performanceVendorDayRepository = performanceVendorDayRepository;
        this.cellService = cellService;
        this.clientPortService = clientPortService;
        this.vendorPortService = vendorPortService;
        this.partnerService = partnerService;
        this.connectionService = connectionService;
        this.performanceService = performanceService;
        this.authenticationService = authenticationService;
    }

    public List<Bill> queryBillList(
            Authentication authentication,
            Query query,
            String interval,
            Timestamp start,
            Timestamp end,
            String timezone) {
        List<Client> qualifiedClientList = partnerService.getQualifiedClientListWithoutFilterAndSearch(authentication, query);
        List<ClientPort> clientPortList = partnerService.getQualifiedClientPortList(qualifiedClientList, query);
        List<Integer> clientPortIdList = clientPortList.stream().map(ClientPort::getId).distinct().collect(Collectors.toList());

        List<Bill> billList = getBillList(clientPortIdList, start, end);
        return billList;
    }

    public List<Bill> getBillList(List<Integer> clientPortIdList, Timestamp start, Timestamp end) {
        List<Bill> billList = billRepository.findByClientPortInAndDateBetween(clientPortIdList, start, end);
        return billList;
    }

    public ObjectNode uploadBill(
            Authentication authentication,
            MultipartFile upload,
            String timezone) {
        ObjectNode node = JsonNodeFactory.instance.objectNode();
        List<Bill> billList = new ArrayList<Bill>();

        try {
            String filename = upload.getOriginalFilename();
            if (filename == null) {
                node.put("uploaded", -1);
                return node;
            }
            String fileType = filename.substring(filename.lastIndexOf(".") + 1, filename.length());
            InputStream inputStream = upload.getInputStream();
            Workbook workbook = null;

            if (fileType.equalsIgnoreCase("XLS")) {
                workbook = new HSSFWorkbook(inputStream);
            } else if (fileType.equalsIgnoreCase("XLSX")) {
                workbook = new XSSFWorkbook(inputStream);
            } else {
                node.put("uploaded", -1);
                return node;
            }

            for (int si = 0; si < workbook.getNumberOfSheets(); si++) {
                Sheet sheet = workbook.getSheetAt(si);

                List<String> keyFields = Arrays.asList("时间", "代码位", "收益", "展现量", "点击量");
                List<String> keyFieldsCandidate = Arrays.asList("时间", "代码位候选", "收益", "展现量", "点击量");

                boolean headerRowFound = false;
                Map<String, Integer> header = new HashMap<String, Integer>();

                for (int ri = sheet.getFirstRowNum(); ri <= sheet.getLastRowNum(); ri++) {
                    Row row = sheet.getRow(ri);
                    if (row == null) {
                        continue;
                    }

                    if (!headerRowFound) {
                        header.clear();
                        Set<String> keyFieldFound = new HashSet<String>();
                        for (int ci = row.getFirstCellNum(); ci < row.getLastCellNum(); ci++) {
                            if (row.getCell(ci) == null) {
                                continue;
                            }

                            String caption = cellService.getStringValue(row.getCell(ci));

                            if (caption.equals("时间") || caption.equals("日期") || caption.equals("日期时间") || caption.equals("收益日期")) {
                                keyFieldFound.add("时间");
                                header.put("时间", ci);
                            }
                            if (caption.equals("代码位") || caption.equals("广告位ID") || caption.equals("广告位 Token")) {
                                keyFieldFound.add("代码位");
                                header.put("代码位", ci);
                            }
                            if (caption.equals("广告位名称")) {
                                keyFieldFound.add("代码位候选");
                                header.put("代码位候选", ci);
                            }
                            if (caption.equals("收益") || caption.equals("预估收益") || caption.equals("预估收入") || caption.equals("收入") || caption.equals("预估收入(¥)") || caption.equals("收入(元)") || caption.equals("预估支出")) {
                                keyFieldFound.add("收益");
                                header.put("收益", ci);
                            }
                            if (caption.equals("展现量") || caption.equals("曝光") || caption.equals("展示") || caption.equals("展示数") || caption.equals("曝光量") || caption.equals("曝光数") || caption.equals("最终展现") || caption.equals("有效曝光数")) {
                                keyFieldFound.add("展现量");
                                header.put("展现量", ci);
                            }
                            if (caption.equals("点击量") || caption.equals("点击") || caption.equals("点击数") || caption.equals("有效点击数")) {
                                keyFieldFound.add("点击量");
                                header.put("点击量", ci);
                            }
                            if (caption.equals("请求量") || caption.equals("广告请求量") || caption.equals("请求") || caption.equals("请求数")) {
                                header.put("请求量", ci);
                            }
                            if (caption.equals("响应量") || caption.equals("广告返回量") || caption.equals("填充数") || caption.equals("返回量")) {
                                header.put("响应量", ci);
                            }
                        }

                        if (keyFieldFound.containsAll(keyFields)) {
                            headerRowFound = true;
                        }
                        if (keyFieldFound.containsAll(keyFieldsCandidate) && !keyFieldFound.contains("代码位")) {
                            header.put("代码位", header.get("代码位候选"));
                            headerRowFound = true;
                        }
                    } else {
                        Calendar calendar = null;
                        Cell cellDate = row.getCell(header.get("时间"));
                        if (cellDate != null) {
                            SimpleDateFormat df0 = new SimpleDateFormat("yyyy-MM-dd");
                            SimpleDateFormat df1 = new SimpleDateFormat("yyyy/M/d");
                            SimpleDateFormat df2 = new SimpleDateFormat("yyyy-MM-dd");
                            SimpleDateFormat df3 = new SimpleDateFormat("yyyyMMdd");
                            TimeZone tz = TimeZone.getTimeZone(timezone);
                            df1.setTimeZone(tz);
                            df2.setTimeZone(tz);
                            df3.setTimeZone(tz);

                            if (cellDate.getCellType() == CellType.STRING) {
                                try {
                                    long time = df1.parse(cellService.getStringValue(cellDate)).getTime();
                                    calendar = Calendar.getInstance(tz);
                                    calendar.setTimeInMillis(time);
                                } catch (Exception e1) {
                                    try {
                                        long time = df2.parse(cellService.getStringValue(cellDate)).getTime();
                                        calendar = Calendar.getInstance(tz);
                                        calendar.setTimeInMillis(time);
                                    } catch (Exception e2) {
                                        try {
                                            long time = df3.parse(cellService.getStringValue(cellDate)).getTime();
                                            calendar = Calendar.getInstance(tz);
                                            calendar.setTimeInMillis(time);
                                        } finally {
                                        }
                                    }
                                }
                            } else if (cellDate.getCellType() == CellType.NUMERIC) {
                                if (DateUtil.isCellDateFormatted(cellDate)) {
                                    String date = df0.format(cellDate.getDateCellValue());
                                    long time = df2.parse(date).getTime();
                                    calendar = Calendar.getInstance(tz);
                                    calendar.setTimeInMillis(time);
                                } else {
                                    long n = Math.round(cellDate.getNumericCellValue());
                                    String date = n / 10000 + "-" + n % 10000 / 100 + "-" + n % 100;
                                    long time = df2.parse(date).getTime();
                                    calendar = Calendar.getInstance(tz);
                                    calendar.setTimeInMillis(time);
                                }
                            }
                        }
                        if (calendar == null) {
                            continue;
                        }

                        String tagId = null;
                        Cell cellTagId = row.getCell(header.get("代码位"));
                        if (cellTagId != null) {
                            tagId = cellService.getStringValue(cellTagId);
                            if (tagId.split("\\(").length > 1) {
                                tagId = tagId.split("\\(")[1];
                                tagId = tagId.split("\\)")[0];
                            }
                        }
                        if (tagId == null || tagId.isBlank()) {
                            continue;
                        }

                        Long impression = null;
                        Cell cellImpression = row.getCell(header.get("展现量"));
                        if (cellImpression != null) {
                            try {
                                impression = Math.round(Double.parseDouble(cellService.getStringValue(cellImpression).replace(",", "")));
                            } catch (Exception e) {
                            }
                        }
                        if (impression == null) {
                            continue;
                        }

                        Long click = null;
                        Cell cellClick = row.getCell(header.get("点击量"));
                        if (cellClick != null) {
                            try {
                                click = Math.round(Double.parseDouble(cellService.getStringValue(cellClick).replace(",", "")));
                            } catch (Exception e) {
                            }
                        }
                        if (click == null) {
                            continue;
                        }

                        Long cost = null;
                        Cell cellCost = row.getCell(header.get("收益"));
                        if (cellCost != null) {
                            try {
                                cost = Math.round(Double.parseDouble(cellService.getStringValue(cellCost).replace(",", "")) * 100000);
                            } catch (Exception e) {
                            }
                        }
                        if (cost == null) {
                            continue;
                        }

                        Long request = null;
                        if (header.containsKey("请求量")) {
                            Cell cellRequest = row.getCell(header.get("请求量"));
                            if (cellRequest != null) {
                                try {
                                    request = Math.round(Double.parseDouble(cellService.getStringValue(cellRequest).replace(",", "")));
                                } catch (Exception e) {
                                }
                            }
                        }

                        Long response = null;
                        if (header.containsKey("响应量")) {
                            Cell cellResponse = row.getCell(header.get("响应量"));
                            if (cellResponse != null) {
                                try {
                                    response = Math.round(Double.parseDouble(cellService.getStringValue(cellResponse).replace(",", "")));
                                } catch (Exception e) {
                                }
                            }
                        }

                        Bill bill = new Bill();
                        bill.setDate(new Timestamp(calendar.getTime().getTime()));
                        bill.setTagId(tagId);
                        bill.setRequest(request);
                        bill.setResponse(response);
                        bill.setImpression(impression);
                        bill.setClick(click);
                        bill.setCost(cost);
                        bill.setStatus(Bill.BILL_STATUS_UPLOADED);

                        billList.add(bill);
                    }
                }
            }

            workbook.close();

            if (addBillList(authentication, billList, timezone).size() != 0) {
                node.put("uploaded", billList.size());
            } else {
                node.put("uploaded", -1);
            }
        } catch (Exception e) {
            e.printStackTrace();
            node.put("uploaded", -1);
        }

        return node;
    }

    public byte[] downloadBill(
            Authentication authentication,
            Query query,
            String interval,
            String aggregateUpstream,
            Timestamp start,
            Timestamp end,
            String timezone) {
        List<Client> qualifiedClientList = partnerService.getQualifiedClientListWithoutFilterAndSearch(authentication, query);
        Map<Integer, Client> clientMap = qualifiedClientList.stream().collect(Collectors.toMap(Client::getId, Function.identity(), (first, second) -> first));
        List<ClientPort> clientPortList = partnerService.getQualifiedClientPortList(qualifiedClientList, query);
        List<Integer> clientPortIdList = clientPortList.stream().map(ClientPort::getId).distinct().collect(Collectors.toList());
        Map<Integer, ClientPort> clientPortMap = clientPortList.stream().collect(Collectors.toMap(ClientPort::getId, Function.identity(), (first, second) -> first));

        List<Bill> billList = getBillList(clientPortIdList, start, end);

        return generateBillReport(
            interval,
            aggregateUpstream,
            billList,
            clientMap,
            clientPortMap,
            timezone);
    }

    @Transactional
    public List<Bill> addBillList(
            Authentication authentication,
            List<Bill> billList,
            String timezone) {
        removeBillList(authentication, billList, timezone);

        billRepository.saveAll(billList);

        splitBillList(authentication, billList, timezone);

        return billList;
    }

    @Transactional
    public List<Bill> removeBillList(
            Authentication authentication,
            List<Bill> billList,
            String timezone) {
        verifyBillList(authentication, billList, timezone);

        Iterator<Bill> it = billList.iterator();
        Timestamp minDate = null;
        Timestamp maxDate = null;
        Set<Integer> clientPortIdSet = new HashSet<Integer>();
        Map<String, Bill> billMap = new HashMap<String, Bill>();
        while (it.hasNext()) {
            Bill bill = it.next();
            billMap.put(bill.getDate().getTime() + "|" + bill.getClientPort() , bill);
            clientPortIdSet.add(bill.getClientPort());

            if (minDate == null || minDate.after(bill.getDate())) {
                minDate = bill.getDate();
            }
            if (maxDate == null || maxDate.before(bill.getDate())) {
                maxDate = bill.getDate();
            }
        }
        List<Integer> clientPortIdList = clientPortIdSet.stream().collect(Collectors.toList());

        if (minDate == null || maxDate == null) {
            return null;
        }

        List<Medium> existedMediumList = mediumRepository.findByClientPortInAndDateBetween(clientPortIdList, minDate, maxDate);
        List<Medium> existedMediumListToBeRemoved = new ArrayList<Medium>();
        for (Medium medium : existedMediumList) {
            String key = medium.getDate().getTime() + "|" + medium.getClientPort();
            if (billMap.containsKey(key)) {
                existedMediumListToBeRemoved.add(medium);
            }
        }
        mediumRepository.deleteAll(existedMediumListToBeRemoved);

        List<Bill> existedBillList = billRepository.findByClientPortInAndDateBetween(clientPortIdList, minDate, maxDate);
        List<Bill> existedBillListToBeRemoved = new ArrayList<Bill>();
        for (Bill bill : existedBillList) {
             String key = bill.getDate().getTime() + "|" + bill.getClientPort();
            if (billMap.containsKey(key)) {
                existedBillListToBeRemoved.add(bill);
            }
        }
        billRepository.deleteAll(existedBillListToBeRemoved);

        return existedBillListToBeRemoved;
    }

    @Transactional
    public void splitBillList(
            Authentication authentication,
            List<Bill> billList,
            String timezone) {
        if (billList == null || billList.isEmpty()) {
            return;
        }

        Timestamp minDate = null;
        Timestamp maxDate = null;
        Set<Integer> clientPortIdSet = new HashSet<Integer>();
        Map<String, Bill> billMap = new HashMap<String, Bill>();
        for (Bill bill : billList) {
            billMap.put(bill.getDate().getTime() + "|" + bill.getClientPort() , bill);

            clientPortIdSet.add(bill.getClientPort());

            if (minDate == null || minDate.after(bill.getDate())) {
                minDate = bill.getDate();
            }
            if (maxDate == null || maxDate.before(bill.getDate())) {
                maxDate = bill.getDate();
            }
        }
        List<Integer> clientPortIdList = clientPortIdSet.stream().collect(Collectors.toList());
        List<ClientPort> clientPortList = clientPortService.getClientPortList(authentication);
        Map<Integer, ClientPort> clientPortMap = clientPortList.stream().collect(Collectors.toMap(ClientPort::getId, Function.identity(), (first, second) -> first));

        Map<Long, Map<Integer, List<Integer>>> timedPairedVendorPortMap = connectionService.getPairedVendorPortMap(clientPortIdList, minDate, maxDate);
        Map<String, Double> downstreamRatioMap = getDownstreamRatioMap(minDate, maxDate);

        List<Medium> existedMediumList = mediumRepository.findByClientPortInAndDateBetween(clientPortIdList, minDate, maxDate);
        List<Medium> existedMediumListToBeRemoved = new ArrayList<Medium>();
        for (Medium medium : existedMediumList) {
            String key = medium.getDate().getTime() + "|" + medium.getClientPort();
            if (billMap.containsKey(key)) {
                existedMediumListToBeRemoved.add(medium);
            }
        }
        mediumRepository.deleteAll(existedMediumListToBeRemoved);

        List<PerformanceClientDay> performanceClientDayList = performanceClientDayRepository.findDetailByClientPortInAndTimeBetween(clientPortIdList, minDate, maxDate);
        Map<String, List<PerformanceClientDay>> performanceClientDayMap = new HashMap<String, List<PerformanceClientDay>>();
        for (PerformanceClientDay performanceClientDay : performanceClientDayList) {
            String key = performanceClientDay.getTime().getTime() + "|" + performanceClientDay.getClientPort();
            if (!performanceClientDayMap.containsKey(key)) {
                performanceClientDayMap.put(key, new ArrayList<PerformanceClientDay>());
            }
            performanceClientDayMap.get(key).add(performanceClientDay);
        }

        List<Medium> mediumList = new ArrayList<Medium>();
        for (Bill bill : billList) {
            Integer clientPortId = bill.getClientPort();
            ClientPort clientPort = clientPortMap.get(clientPortId);
            if (clientPort == null) {
                continue;
            }

            Map<Integer, List<Integer>> pairedVendorPortMap = timedPairedVendorPortMap.get(bill.getDate().getTime());
            if (pairedVendorPortMap == null) {
                continue;
            }

            List<Integer> vendorPortIdList = pairedVendorPortMap.get(clientPortId);
            if (vendorPortIdList == null || vendorPortIdList.isEmpty()) {
                continue;
            }

            if (clientPort.getMode() == ClientPort.PORT_TYPE_DIRECT) {
                Integer vendorPortId = vendorPortIdList.get(0);

                Medium medium = new Medium(clientPortId, vendorPortId, bill.getDate());

                medium.setRequest(bill.getRequest());
                medium.setResponse(bill.getResponse());
                medium.setImpression(bill.getImpression());
                medium.setClick(bill.getClick());
                medium.setIncome(bill.getCost());
                medium.setOutcomeUpstream(0L);
                medium.setOutcomeRebate(0L);
                medium.setOutcomeDownstream(bill.getCost());

                mediumList.add(medium);
            } else {
                List<PerformanceClientDay> performanceClientDayListPerClient = performanceClientDayMap.get(bill.getDate().getTime() + "|" + clientPortId);
                if (performanceClientDayListPerClient == null || performanceClientDayListPerClient.isEmpty()) {
                    continue;
                }

                PerformanceView performanceViewTotal = new PerformanceView();
                Map<Integer, PerformanceView> performanceViewMap = new HashMap<Integer, PerformanceView>();
                for (PerformanceClientDay performanceClientDay : performanceClientDayListPerClient) {
                    PerformanceView performanceViewPerVendor = performanceService.convertClientToView(performanceClientDay, "day", timezone);
                    performanceViewTotal.setRequest(performanceViewTotal.getRequest() + performanceViewPerVendor.getRequest());
                    performanceViewTotal.setResponse(performanceViewTotal.getResponse() + performanceViewPerVendor.getResponse());
                    performanceViewTotal.setRequestv(performanceViewTotal.getRequestv() + performanceViewPerVendor.getRequestv());
                    performanceViewTotal.setResponsev(performanceViewTotal.getResponsev() + performanceViewPerVendor.getResponsev());
                    performanceViewTotal.setImpression(performanceViewTotal.getImpression() + performanceViewPerVendor.getImpression());
                    performanceViewTotal.setClick(performanceViewTotal.getClick() + performanceViewPerVendor.getClick());
                    performanceViewTotal.setIncome(performanceViewTotal.getIncome() + performanceViewPerVendor.getIncome());
                    performanceViewTotal.setOutcomeUpstream(performanceViewTotal.getOutcomeUpstream() + performanceViewPerVendor.getOutcomeUpstream());
                    performanceViewTotal.setOutcomeRebate(performanceViewTotal.getOutcomeRebate() + performanceViewPerVendor.getOutcomeRebate());
                    performanceViewTotal.setOutcomeDownstream(performanceViewTotal.getOutcomeDownstream() + performanceViewPerVendor.getOutcomeDownstream());

                    performanceViewMap.put(performanceClientDay.getVendorPort(), performanceViewPerVendor);
                }

                for (Integer vendorPortId : vendorPortIdList) {
                    Medium medium = new Medium(bill.getClientPort(), vendorPortId, bill.getDate());

                    if (performanceViewMap.containsKey(vendorPortId)) {
                        if (bill.getRequest() != null) {
                            medium.setRequest(performanceViewTotal.getRequestv() == 0 ? 0 : Math.round(1.0 * bill.getRequest() * performanceViewMap.get(vendorPortId).getRequestv() / performanceViewTotal.getRequestv()));
                        }
                        if (bill.getResponse() != null) {
                            medium.setResponse(performanceViewTotal.getResponse() == 0 ? 0 : Math.round(1.0 * bill.getResponse() * performanceViewMap.get(vendorPortId).getResponse() / performanceViewTotal.getResponse()));
                        }
                        if (bill.getImpression() != null) {
                            medium.setImpression(performanceViewTotal.getImpression() == 0 ? 0 : Math.round(1.0 * bill.getImpression() * performanceViewMap.get(vendorPortId).getImpression() / performanceViewTotal.getImpression()));
                        }
                        if (bill.getClick() != null) {
                            medium.setClick(performanceViewTotal.getClick() == 0 ? 0 : Math.round(1.0 * bill.getClick() * performanceViewMap.get(vendorPortId).getClick() / performanceViewTotal.getClick()));
                        }
                        if (bill.getCost() != null) {
                            if (clientPort.getMode() == ClientPort.PORT_TYPE_SHARE) {
                                medium.setIncome(performanceViewTotal.getImpression() == 0 ? 0 : Math.round(1.0 * bill.getCost() * performanceViewMap.get(vendorPortId).getImpression() / performanceViewTotal.getImpression()));
                            } else {
                                medium.setIncome(performanceViewTotal.getIncome() == 0 ? 0 : Math.round(1.0 * bill.getCost() * performanceViewMap.get(vendorPortId).getIncome() / performanceViewTotal.getIncome()));
                            }
                        }
                        if (bill.getCost() != null) {
                            medium.setOutcomeUpstream(performanceViewTotal.getOutcomeUpstream() == 0 ? 0 : Math.round(1.0 * bill.getCost() * performanceViewMap.get(vendorPortId).getOutcomeUpstream() / performanceViewTotal.getIncome()));
                        }
                        if (bill.getCost() != null) {
                            medium.setOutcomeRebate(performanceViewTotal.getOutcomeRebate() == 0 ? 0 : Math.round(1.0 * bill.getCost() * performanceViewMap.get(vendorPortId).getOutcomeRebate() / performanceViewTotal.getIncome()));
                        }
                        if (bill.getCost() != null) {
                            if (clientPort.getMode() == ClientPort.PORT_TYPE_SHARE) {
                                Double downstreamRatio = downstreamRatioMap.get(bill.getDate().getTime() + "|" + clientPortId + "|" + vendorPortId);
                                medium.setOutcomeDownstream(downstreamRatio == null ? 0 : Math.round(medium.getIncome() * downstreamRatio));
                            } else {
                                medium.setOutcomeDownstream(performanceViewTotal.getIncome() == 0 ? 0 : Math.round(1.0 * bill.getCost() * performanceViewMap.get(vendorPortId).getOutcomeDownstream() / performanceViewTotal.getIncome()));
                            }
                        }
                    }

                    mediumList.add(medium);
                }
            }
        }
        mediumRepository.saveAll(mediumList);

        patchSignList(authentication, mediumList, timezone);
    }

    private Map<String, Double> getDownstreamRatioMap(Timestamp startDate, Timestamp endDate) {
        Map<String, Double> downstreamRatioMap = new HashMap<String, Double>();
        List<Connection> connectionList = connectionService.getConnectionList();

        Timestamp date = new Timestamp(startDate.getTime());
        while (date.before(endDate) || date.equals(endDate)) {
            Timestamp dayEnd = new Timestamp(date.getTime() + 86400000);
            for (Connection connection : connectionList) {
                if (connection.getClientPort() == null || connection.getVendorPort() == null || connection.getDownstreamRatio() == null) {
                    continue;
                }
                if ((connection.getValidFrom().equals(dayEnd) || connection.getValidFrom().before(dayEnd)) && connection.getValidTo().after(date)) {
                    String key = date.getTime() + "|" + connection.getClientPort().getId() + "|" + connection.getVendorPort().getId();
                    downstreamRatioMap.put(key, connection.getDownstreamRatio());
                }
            }

            date = dayEnd;
        }

        return downstreamRatioMap;
    }

    public List<Medium> queryMediumListClient(
            Authentication authentication,
            Query query,
            String interval,
            Timestamp start,
            Timestamp end,
            String timezone) {
        List<Client> qualifiedClientList = partnerService.getQualifiedClientListWithoutFilterAndSearch(authentication, query);
        List<ClientPort> clientPortList = partnerService.getQualifiedClientPortList(qualifiedClientList, query);
        List<Integer> clientPortIdList = clientPortList.stream().map(ClientPort::getId).distinct().collect(Collectors.toList());

        List<Medium> mediumList = getMediumListClient(clientPortIdList, start, end);
        return mediumList;
    }

    public List<Medium> queryMediumListVendor(
            Authentication authentication,
            Query query,
            String interval,
            Timestamp start,
            Timestamp end,
            String timezone) {
        List<Vendor> qualifiedVendorList = partnerService.getQualifiedVendorListWithoutFilterAndSearch(authentication, query);
        List<VendorPort> vendorPortList = partnerService.getQualifiedVendorPortList(qualifiedVendorList, query);
        List<Integer> vendorPortIdList = vendorPortList.stream().map(VendorPort::getId).distinct().collect(Collectors.toList());

        List<Medium> mediumList = getMediumListVendor(vendorPortIdList, start, end);
        return mediumList;
    }

    public List<Medium> getMediumListClient(List<Integer> clientPortIdList, Timestamp start, Timestamp end) {
        List<Medium> mediumList = mediumRepository.findByClientPortInAndDateBetween(clientPortIdList, start, end);
        return mediumList;
    }

    public List<Medium> getMediumListVendor(List<Integer> vendorPortIdList, Timestamp start, Timestamp end) {
        List<Medium> mediumList = mediumRepository.findByVendorPortInAndDateBetween(vendorPortIdList, start, end);
        return mediumList;
    }

    public List<Sign> querySignList(
            Authentication authentication,
            Query query,
            String interval,
            Timestamp start,
            Timestamp end,
            String timezone) {
        List<Vendor> qualifiedVendorList = partnerService.getQualifiedVendorListWithoutFilterAndSearch(authentication, query);
        List<VendorPort> vendorPortList = partnerService.getQualifiedVendorPortList(qualifiedVendorList, query);
        List<Integer> vendorPortIdList = vendorPortList.stream().map(VendorPort::getId).distinct().collect(Collectors.toList());
        Map<Integer, VendorPort> vendorPortMap = vendorPortList.stream().collect(Collectors.toMap(VendorPort::getId, Function.identity(), (first, second) -> first));

        List<Sign> signList = getSignList(vendorPortIdList, start, end);
        List<Sign> qualifiedSignList = new ArrayList<Sign>();
        for (Sign sign : signList) {
            VendorPort vendorPort = vendorPortMap.get(sign.getVendorPort());
            if (vendorPort == null) {
                continue;
            }

            Boolean isManager = authenticationService.hasAccess(authentication, vendorPort.getVendor().getTenant(), TenantUser.ROLE_TENANT_MANAGER, null)
                || authenticationService.hasAccess(authentication, vendorPort.getVendor().getTenant(), TenantUser.ROLE_TENANT_OPERATOR, null)
                || authenticationService.hasAccess(authentication, vendorPort.getVendor().getTenant(), TenantUser.ROLE_TENANT_OBSERVER, null);

            if (!isManager && sign.getStatus() != Sign.SIGN_STATUS_SIGNED) {
                continue;
            }

            qualifiedSignList.add(sign);
        }

        return qualifiedSignList;
    }

    public List<Sign> getSignList(List<Integer> vendorPortIdList, Timestamp start, Timestamp end) {
        List<Sign> signList = signRepository.findByVendorPortInAndDateBetween(vendorPortIdList, start, end);
        return signList;
    }

    public ObjectNode uploadSign(
            Authentication authentication,
            MultipartFile upload,
            String timezone) {
        List<ClientPort> clientPortList = clientPortService.getClientPortList(authentication);
        Map<String, ClientPort> clientPortTagIdMap = clientPortList.stream().collect(Collectors.toMap(ClientPort::getTagId, Function.identity(), (first, second) -> first));
        List<VendorPort> vendorPortList = vendorPortService.getVendorPortList(authentication);
        Map<Integer, VendorPort> vendorPortIdMap = vendorPortList.stream().collect(Collectors.toMap(VendorPort::getId, Function.identity(), (first, second) -> first));
        Map<String, VendorPort> vendorPortTagIdMap = vendorPortList.stream().collect(Collectors.toMap(VendorPort::getTagId, Function.identity(), (first, second) -> first));

        ObjectNode node = JsonNodeFactory.instance.objectNode();
        List<Sign> signList = new ArrayList<Sign>();

        try {
            String filename = upload.getOriginalFilename();
            if (filename == null) {
                node.put("uploaded", -1);
                return node;
            }
            String fileType = filename.substring(filename.lastIndexOf(".") + 1, filename.length());
            InputStream inputStream = upload.getInputStream();
            Workbook workbook = null;

            if (fileType.equalsIgnoreCase("XLS")) {
                workbook = new HSSFWorkbook(inputStream);
            } else if (fileType.equalsIgnoreCase("XLSX")) {
                workbook = new XSSFWorkbook(inputStream);
            } else {
                node.put("uploaded", -1);
                return node;
            }

            for (int si = 0; si < workbook.getNumberOfSheets(); si++) {
                Sheet sheet = workbook.getSheetAt(si);

                String[] keyFields = {"时间", "代码位", "收益", "展现量", "点击量"};

                boolean headerRowFound = false;
                Map<String, Integer> header = new HashMap<String, Integer>();

                for (int ri = sheet.getFirstRowNum(); ri <= sheet.getLastRowNum(); ri++) {
                    Row row = sheet.getRow(ri);
                    if (row == null) {
                        continue;
                    }

                    if (!headerRowFound) {
                        header.clear();
                        Set<String> keyFieldFound = new HashSet<String>();
                        for (int ci = row.getFirstCellNum(); ci < row.getLastCellNum(); ci++) {
                            if (row.getCell(ci) == null) {
                                continue;
                            }

                            String caption = cellService.getStringValue(row.getCell(ci));

                            if (caption.equals("时间")) {
                                keyFieldFound.add("时间");
                                header.put("时间", ci);
                            }
                            if (caption.equals("代码位") || caption.equals("代码位（下游）")) {
                                keyFieldFound.add("代码位");
                                header.put("代码位", ci);
                            }
                            if (caption.equals("收益") || caption.equals("预估收益") || caption.equals("支出（下游）")) {
                                keyFieldFound.add("收益");
                                header.put("收益", ci);
                            }
                            if (caption.equals("展现量") || caption.equals("展现量（下游）")) {
                                keyFieldFound.add("展现量");
                                header.put("展现量", ci);
                            }
                            if (caption.equals("点击量") || caption.equals("点击量（下游）")) {
                                keyFieldFound.add("点击量");
                                header.put("点击量", ci);
                            }
                            if (caption.equals("请求量") || caption.equals("广告请求量")) {
                                header.put("请求量", ci);
                            }
                            if (caption.equals("响应量") || caption.equals("广告响应量")) {
                                header.put("响应量", ci);
                            }
                        }

                        if (keyFieldFound.size() == keyFields.length) {
                            headerRowFound = true;
                        }
                    } else {
                        Calendar calendar = null;
                        Cell cellDate = row.getCell(header.get("时间"));
                        if (cellDate != null) {
                            SimpleDateFormat df0 = new SimpleDateFormat("yyyy-MM-dd");
                            SimpleDateFormat df1 = new SimpleDateFormat("yyyy/M/d");
                            SimpleDateFormat df2 = new SimpleDateFormat("yyyy-MM-dd");
                            TimeZone tz = TimeZone.getTimeZone(timezone);
                            df1.setTimeZone(tz);
                            df2.setTimeZone(tz);

                            if (cellDate.getCellType() == CellType.STRING) {
                                try {
                                    long time = df1.parse(cellService.getStringValue(cellDate)).getTime();
                                    calendar = Calendar.getInstance(tz);
                                    calendar.setTimeInMillis(time);
                                } catch (Exception e) {
                                    try {
                                        long time = df2.parse(cellService.getStringValue(cellDate)).getTime();
                                        calendar = Calendar.getInstance(tz);
                                        calendar.setTimeInMillis(time);
                                    } finally {
                                    }
                                }
                            } else if (cellDate.getCellType() == CellType.NUMERIC) {
                                if (DateUtil.isCellDateFormatted(cellDate)) {
                                    String date = df0.format(cellDate.getDateCellValue());
                                    long time = df2.parse(date).getTime();
                                    calendar = Calendar.getInstance(tz);
                                    calendar.setTimeInMillis(time);
                                }
                            }
                        }
                        if (calendar == null) {
                            continue;
                        }

                        String tagId = null;
                        Cell cellTagId = row.getCell(header.get("代码位"));
                        if (cellTagId != null) {
                            tagId = cellService.getStringValue(cellTagId);
                        }
                        if (tagId == null || tagId.isBlank()) {
                            continue;
                        }

                        Long impression = null;
                        Cell cellImpression = row.getCell(header.get("展现量"));
                        if (cellImpression != null) {
                            try {
                                impression = Math.round(Double.parseDouble(cellService.getStringValue(cellImpression)));
                            } catch (Exception e) {
                            }
                        }
                        if (impression == null) {
                            continue;
                        }

                        Long click = null;
                        Cell cellClick = row.getCell(header.get("点击量"));
                        if (cellClick != null) {
                            try {
                                click = Math.round(Double.parseDouble(cellService.getStringValue(cellClick)));
                            } catch (Exception e) {
                            }
                        }
                        if (click == null) {
                            continue;
                        }

                        Long cost = null;
                        Cell cellCost = row.getCell(header.get("收益"));
                        if (cellCost != null) {
                            try {
                                cost = Math.round(Double.parseDouble(cellService.getStringValue(cellCost)) * 100000);
                            } catch (Exception e) {
                            }
                        }
                        if (cost == null) {
                            continue;
                        }

                        Long request = null;
                        if (header.containsKey("请求量")) {
                            Cell cellRequest = row.getCell(header.get("请求量"));
                            if (cellRequest != null) {
                                try {
                                    request = Math.round(Double.parseDouble(cellService.getStringValue(cellRequest)));
                                } catch (Exception e) {
                                }
                            }
                        }

                        Long response = null;
                        if (header.containsKey("响应量")) {
                            Cell cellResponse = row.getCell(header.get("响应量"));
                            if (cellResponse != null) {
                                try {
                                    response = Math.round(Double.parseDouble(cellService.getStringValue(cellResponse)));
                                } catch (Exception e) {
                                }
                            }
                        }

                        Sign sign = new Sign();
                        sign.setDate(new Timestamp(calendar.getTime().getTime()));
                        sign.setTagId(tagId);
                        if (clientPortTagIdMap.get(tagId) != null && clientPortTagIdMap.get(tagId).getMode() == VendorPort.PORT_TYPE_DIRECT) {
                            sign.setVendorPort(clientPortTagIdMap.get(tagId).getId());
                        } else {
                            sign.setVendorPort(vendorPortTagIdMap.get(tagId).getId());
                        }
                        sign.setRequest(request);
                        sign.setResponse(response);
                        sign.setImpression(impression);
                        sign.setClick(click);
                        sign.setCost(cost);
                        sign.setStatus(Sign.SIGN_STATUS_CREATED);

                        signList.add(sign);
                    }
                }
            }

            workbook.close();

            List<Timestamp> timestamps = signList.stream().map(sign -> sign.getDate()).distinct().toList();
            Map<Timestamp, Map<Integer, List<Integer>>> pairedVendorPortMap = new HashMap<Timestamp, Map<Integer, List<Integer>>>();
            for (Timestamp timestamp : timestamps) {
                List<Sign> signsOneday = signList.stream().filter(sign -> sign.getDate().equals(timestamp)).toList();
                Map<Integer, List<Integer>> pairedVendorPortMapOneday = connectionService.getPairedVendorPortMap(signsOneday.stream().map(sign -> sign.getVendorPort()).distinct().toList(), timestamp);
                pairedVendorPortMap.put(timestamp, pairedVendorPortMapOneday);
            }

            for (Sign sign : signList) {
                ClientPort clientPort = clientPortTagIdMap.get(sign.getTagId());
                VendorPort vendorPort = vendorPortTagIdMap.get(sign.getTagId());

                if (clientPort != null && clientPort.getMode() == ClientPort.PORT_TYPE_DIRECT || vendorPort != null && vendorPort.getMode() == VendorPort.PORT_TYPE_DIRECT) {
                    Map<Integer, List<Integer>> pairedVendorPortMapOneday = pairedVendorPortMap.get(sign.getDate());
                    if (pairedVendorPortMapOneday == null) {
                        continue;
                    }
                    List<Integer> vendorPorts = pairedVendorPortMapOneday.get(sign.getVendorPort());
                    if (vendorPorts == null) {
                        continue;
                    }
                    sign.setVendorPort(vendorPortIdMap.get(vendorPorts.get(0)).getId());
                    sign.setTagId(vendorPortIdMap.get(vendorPorts.get(0)).getTagId());
                }
            }

            if (addSignList(authentication, signList, timezone).size() != 0) {
                node.put("uploaded", signList.size());
            } else {
                node.put("uploaded", -1);
            }
        } catch (Exception e) {
            e.printStackTrace();
            node.put("uploaded", -1);
        }

        return node;
    }

    public byte[] downloadSign(
            Authentication authentication,
            Query query,
            String interval,
            String aggregateDownstream,
            Timestamp start,
            Timestamp end,
            String timezone) {
        List<Client> qualifiedClientList = partnerService.getQualifiedClientListWithoutFilterAndSearch(authentication, query);
        List<ClientPort> clientPortList = partnerService.getQualifiedClientPortListWithoutFilterAndSearch(qualifiedClientList, query);
        Map<Integer, ClientPort> clientPortMap = clientPortList.stream().collect(Collectors.toMap(ClientPort::getId, Function.identity(), (first, second) -> first));
        List<Vendor> qualifiedVendorList = partnerService.getQualifiedVendorListWithoutFilterAndSearch(authentication, query);
        Map<Integer, Vendor> vendorMap = qualifiedVendorList.stream().collect(Collectors.toMap(Vendor::getId, Function.identity(), (first, second) -> first));
        List<VendorPort> vendorPortList = partnerService.getQualifiedVendorPortList(qualifiedVendorList, query);
        List<Integer> vendorPortIdList = vendorPortList.stream().map(VendorPort::getId).distinct().collect(Collectors.toList());
        Map<Integer, VendorPort> vendorPortMap = vendorPortList.stream().collect(Collectors.toMap(VendorPort::getId, Function.identity(), (first, second) -> first));

        List<Sign> signList = getSignList(vendorPortIdList, start, end);
        List<Sign> qualifiedSignList = new ArrayList<Sign>();
        for (Sign sign : signList) {
            VendorPort vendorPort = vendorPortMap.get(sign.getVendorPort());
            if (vendorPort == null) {
                continue;
            }

            Boolean isManager = authenticationService.hasAccess(authentication, vendorPort.getVendor().getTenant(), TenantUser.ROLE_TENANT_MANAGER, null)
                || authenticationService.hasAccess(authentication, vendorPort.getVendor().getTenant(), TenantUser.ROLE_TENANT_OPERATOR, null)
                || authenticationService.hasAccess(authentication, vendorPort.getVendor().getTenant(), TenantUser.ROLE_TENANT_OBSERVER, null);

            if (!isManager && sign.getStatus() != Sign.SIGN_STATUS_SIGNED) {
                continue;
            }

            qualifiedSignList.add(sign);
        }

        return generateSignReport(
            interval,
            aggregateDownstream,
            qualifiedSignList,
            clientPortMap,
            vendorMap,
            vendorPortMap,
            timezone);
    }

    @Transactional
    public List<Sign> addSignList(
            Authentication authentication,
            List<Sign> signList,
            String timezone) {
        removeSignList(authentication, signList, timezone);

        signRepository.saveAll(signList);

        return signList;
    }

    @Transactional
    public List<Sign> removeSignList(
            Authentication authentication,
            List<Sign> signList,
            String timezone) {
        verifySignList(authentication, signList, timezone);

        Iterator<Sign> it = signList.iterator();
        Timestamp minDate = null;
        Timestamp maxDate = null;
        Set<Integer> vendorPortIdSet = new HashSet<Integer>();
        Map<String, Sign> signMap = new HashMap<String, Sign>();
        while (it.hasNext()) {
            Sign sign = it.next();
            signMap.put(sign.getDate().getTime() + "|" + sign.getVendorPort() , sign);
            vendorPortIdSet.add(sign.getVendorPort());

            if (minDate == null || minDate.after(sign.getDate())) {
                minDate = sign.getDate();
            }
            if (maxDate == null || maxDate.before(sign.getDate())) {
                maxDate = sign.getDate();
            }
        }
        List<Integer> vendorPortIdList = vendorPortIdSet.stream().collect(Collectors.toList());

        if (minDate == null || maxDate == null) {
            return null;
        }

        List<Sign> existedSignList = signRepository.findByVendorPortInAndDateBetween(vendorPortIdList, minDate, maxDate);
        List<Sign> existedSignListToBeRemoved = new ArrayList<Sign>();
        for (Sign sign : existedSignList) {
             String key = sign.getDate().getTime() + "|" + sign.getVendorPort();
            if (signMap.containsKey(key)) {
                existedSignListToBeRemoved.add(sign);
            }
        }
        signRepository.deleteAll(existedSignListToBeRemoved);

        return existedSignListToBeRemoved;
    }

    public void patchSignList(
            Authentication authentication,
            List<Medium> mediumList,
            String timezone) {
        if (mediumList == null || mediumList.isEmpty()) {
            return;
        }

        Timestamp minDate = null;
        Timestamp maxDate = null;
        Set<Integer> vendorPortIdSet = new HashSet<Integer>();
        for (Medium medium : mediumList) {
            vendorPortIdSet.add(medium.getVendorPort());

            if (minDate == null || minDate.after(medium.getDate())) {
                minDate = medium.getDate();
            }
            if (maxDate == null || maxDate.before(medium.getDate())) {
                maxDate = medium.getDate();
            }
        }
        List<Integer> vendorPortIdList = vendorPortIdSet.stream().collect(Collectors.toList());
        List<VendorPort> vendorPortList = vendorPortService.getVendorPortList(authentication);
        Map<Integer, VendorPort> vendorPortMap = vendorPortList.stream().collect(Collectors.toMap(VendorPort::getId, Function.identity(), (first, second) -> first));

        Map<Long, Map<Integer, List<Integer>>> timedPairedClientPortMap = connectionService.getPairedClientPortMap(vendorPortIdList, minDate, maxDate);

        List<Medium> existedMediumList = mediumRepository.findByVendorPortInAndDateBetween(vendorPortIdList, minDate, maxDate);
        Map<String, Map<Integer, Medium>> existedMediumMap = new HashMap<String, Map<Integer, Medium>>();
        for (Medium medium : existedMediumList) {
            String key = medium.getDate().getTime() + "|" + medium.getVendorPort();
            if (!existedMediumMap.containsKey(key)) {
                existedMediumMap.put(key, new HashMap<Integer, Medium>());
            }
            existedMediumMap.get(key).put(medium.getClientPort(), medium);
        }

        List<PerformanceVendorDay> performanceVendorDayList = performanceVendorDayRepository.findSummaryByVendorPortInAndTimeBetween(vendorPortIdList, minDate, maxDate);
        Map<String, PerformanceVendorDay> performanceVendorDayMap = new HashMap<String, PerformanceVendorDay>();
        for (PerformanceVendorDay performanceVendorDay : performanceVendorDayList) {
            String key = performanceVendorDay.getTime().getTime() + "|" + performanceVendorDay.getVendorPort();
            performanceVendorDayMap.put(key, performanceVendorDay);
        }

        List<Sign> existedSignList = signRepository.findByVendorPortInAndDateBetween(vendorPortIdList, minDate, maxDate);
        List<Sign> existedSignListToBeremoved = new ArrayList<Sign>();
        Map<String, Sign> existedSignMap = new HashMap<String, Sign>();
        for (Sign sign : existedSignList) {
            if (sign.getStatus() == Sign.SIGN_STATUS_READY) {
                // automatically generated sign, could be overwritten
                existedSignListToBeremoved.add(sign);
                continue;
            } else {
                String key = sign.getDate().getTime() + "|" + sign.getVendorPort();
                existedSignMap.put(key, sign);
            }
        }
        signRepository.deleteAll(existedSignListToBeremoved);

        List<Sign> signList = new ArrayList<Sign>();
        for (String key : existedMediumMap.keySet()) {
            Long time = Long.parseLong(key.split("\\|")[0]);
            Integer vendorPortId = Integer.parseInt(key.split("\\|")[1]);
            Map<Integer, Medium> existedMediumMapOneday = existedMediumMap.get(key);

            Sign existedSign = existedSignMap.get(key);
            if (existedSign != null) {
                continue;
            }

            Map<Integer, List<Integer>> pairedClientPortMap = timedPairedClientPortMap.get(time);
            if (pairedClientPortMap == null) {
                continue;
            }
            List<Integer> clientPortIdList = pairedClientPortMap.get(vendorPortId);
            if (clientPortIdList == null || clientPortIdList.isEmpty()) {
                continue;
            }

            VendorPort vendorPort = vendorPortMap.get(vendorPortId);
            if (vendorPort.getMode() == VendorPort.PORT_TYPE_DIRECT) {
                clientPortIdList = clientPortIdList.subList(0, 1);
            }

            Sign sign = new Sign();
            sign.setDate(new Timestamp(time));
            sign.setTagId(vendorPort.getTagId());
            sign.setVendorPort(vendorPortId);
            sign.setStatus(Sign.SIGN_STATUS_READY);

            Boolean fullyBilled = true;

            for (Integer clientPortId : clientPortIdList) {
                Medium medium = existedMediumMapOneday.get(clientPortId);
                if (medium == null) {
                    fullyBilled = false;
                    break;
                }

                if (medium.getRequest() != null) {
                    sign.setRequest(sign.getRequest() == null ? medium.getRequest() : sign.getRequest() + medium.getRequest());
                }
                if (medium.getResponse() != null) {
                    sign.setResponse(sign.getResponse() == null ? medium.getResponse() : sign.getResponse() + medium.getResponse());
                }
                if (medium.getImpression() != null) {
                    sign.setImpression(sign.getImpression() == null ? medium.getImpression() : sign.getImpression() + medium.getImpression());
                }
                if (medium.getClick() != null) {
                    sign.setClick(sign.getClick() == null ? medium.getClick() : sign.getClick() + medium.getClick());
                }
                if (medium.getOutcomeDownstream() != null) {
                    sign.setCost(sign.getCost() == null ? medium.getOutcomeDownstream() : sign.getCost() + medium.getOutcomeDownstream());
                }
            }

            if (fullyBilled) {
                if (performanceVendorDayMap.containsKey(key)) {
                    PerformanceVendor performanceVendor = performanceVendorDayMap.get(key);
                    sign.setRequest(
                        performanceVendor.getEventA() + performanceVendor.getEventB() + performanceVendor.getEventC() + performanceVendor.getEventD() + performanceVendor.getEventE() +
                        performanceVendor.getEventF() + performanceVendor.getEventG() + performanceVendor.getEventH() + performanceVendor.getEventI() + performanceVendor.getEventJ());
                    sign.setResponse(
                        performanceVendor.getEventI() + performanceVendor.getEventJ());
                }

                signList.add(sign);
            }
        }

        signRepository.saveAll(signList);
    }

    private byte[] generateBillReport(
            String interval,
            String aggregateUpstream,
            List<Bill> billList,
            Map<Integer, Client> clientMap,
            Map<Integer, ClientPort> clientPortMap,
            String timezone) {
        TimeZone tz = TimeZone.getTimeZone(timezone);
        SimpleDateFormat dfDate = new SimpleDateFormat("yyyy-MM-dd");
        if (interval.equals("day")) {
            dfDate = new SimpleDateFormat("yyyy-MM-dd");
        }
        if (interval.equals("month")) {
            dfDate = new SimpleDateFormat("yyyy-MM");
        }
        if (interval.equals("year")) {
            dfDate = new SimpleDateFormat("yyyy");
        }
        dfDate.setTimeZone(tz);

        Map<String, Bill> billMap = new HashMap<String, Bill>();
        for (Bill bill : billList) {
            String key = dfDate.format(bill.getDate()) + "|";
            if (aggregateUpstream.equals("client") || aggregateUpstream.equals("clientport")) {
                key += "C" + clientPortMap.get(bill.getClientPort()).getClient().getId() + "|";
            }
            if (aggregateUpstream.equals("clientport")) {
                key += "CP" + bill.getClientPort() + "|";
            }

            if (!billMap.containsKey(key)) {
                Bill billNew = new Bill();
                billNew.setDate(bill.getDate());
                billNew.setTagId(bill.getTagId());
                billNew.setClientPort(bill.getClientPort());
                billNew.setRequest(0L);
                billNew.setResponse(0L);
                billNew.setImpression(0L);
                billNew.setClick(0L);
                billNew.setCost(0L);

                billMap.put(key, billNew);
            }

            Bill billAggregated = billMap.get(key);
            billAggregated.setRequest(billAggregated.getRequest() + (bill.getRequest() == null ? 0 : bill.getRequest()));
            billAggregated.setResponse(billAggregated.getResponse() + (bill.getResponse() == null ? 0 : bill.getResponse()));
            billAggregated.setImpression(billAggregated.getImpression() + (bill.getImpression() == null ? 0 : bill.getImpression()));
            billAggregated.setClick(billAggregated.getClick() + (bill.getClick() == null ? 0 : bill.getClick()));
            billAggregated.setCost(billAggregated.getCost() + (bill.getCost() == null ? 0 : bill.getCost()));
        }

        Workbook workbook = new XSSFWorkbook();
        DataFormat format = workbook.createDataFormat();
        CellStyle cellStyleAmount = workbook.createCellStyle();
        cellStyleAmount.setDataFormat(format.getFormat("##0"));
        CellStyle cellStyleCost = workbook.createCellStyle();
        cellStyleCost.setDataFormat(format.getFormat("0.00"));
        Sheet sheet = workbook.createSheet("上游账单");

        Row header = sheet.createRow(0);
        int column = -1;
        Cell headerCell = header.createCell(++column);
        headerCell.setCellValue("时间");
        if (aggregateUpstream.equals("client") || aggregateUpstream.equals("clientport")) {
            headerCell = header.createCell(++column);
            headerCell.setCellValue("上游名称");
        }
        if (aggregateUpstream.equals("clientport")) {
            headerCell = header.createCell(++column);
            headerCell.setCellValue("代码位");
            headerCell = header.createCell(++column);
            headerCell.setCellValue("代码位名称");
        }
        headerCell = header.createCell(++column);
        headerCell.setCellValue("收益");
        headerCell = header.createCell(++column);
        headerCell.setCellValue("请求量");
        headerCell = header.createCell(++column);
        headerCell.setCellValue("响应量");
        headerCell = header.createCell(++column);
        headerCell.setCellValue("展现量");
        headerCell = header.createCell(++column);
        headerCell.setCellValue("点击量");

        Integer line = 0;

        List<String> keys = new ArrayList<String>();
        keys.addAll(billMap.keySet());
        keys.sort((a, b) -> b.compareTo(a));

        for (String key : keys) {
            String date = key.split("\\|")[0];
            int index = 0;
            Integer clientId = -1;
            Integer clientPortId = -1;

            column = -1;

            if (aggregateUpstream.equals("client") || aggregateUpstream.equals("clientport")) {
                clientId = Integer.parseInt(key.split("\\|")[++index].substring(1));
                if (!clientMap.containsKey(clientId)) {
                    continue;
                }
            }
            if (aggregateUpstream.equals("clientport")) {
                clientPortId = Integer.parseInt(key.split("\\|")[++index].substring(2));
                if (!clientPortMap.containsKey(clientPortId)) {
                    continue;
                }
            }

            Row row = sheet.createRow(++line);
            Cell cell = row.createCell(++column);
            cell.setCellValue(date);
            if (aggregateUpstream.equals("client") || aggregateUpstream.equals("clientport")) {
                cell = row.createCell(++column);
                cell.setCellValue(clientMap.get(clientId).getName());
            }
            if (aggregateUpstream.equals("clientport")) {
                cell = row.createCell(++column);
                cell.setCellValue(clientPortMap.get(clientPortId).getTagId());
                cell = row.createCell(++column);
                cell.setCellValue(clientPortMap.get(clientPortId).getName());
            }
            cell = row.createCell(++column);
            cell.setCellValue(Math.round(billMap.get(key).getCost() / 1000.0) / 100.0);
            cell.setCellStyle(cellStyleCost);
            cell = row.createCell(++column);
            if (billMap.get(key).getRequest() != null) {
                cell.setCellValue(billMap.get(key).getRequest());
                cell.setCellStyle(cellStyleAmount);
            } else {
                cell.setCellValue("--");
            }
            cell = row.createCell(++column);
            if (billMap.get(key).getResponse() != null) {
                cell.setCellValue(billMap.get(key).getResponse());
                cell.setCellStyle(cellStyleAmount);
            } else {
                cell.setCellValue("--");
            }
            cell = row.createCell(++column);
            if (billMap.get(key).getImpression() != null) {
                cell.setCellValue(billMap.get(key).getImpression());
                cell.setCellStyle(cellStyleAmount);
            } else {
                cell.setCellValue("--");
            }
            cell = row.createCell(++column);
            if (billMap.get(key).getClick() != null) {
                cell.setCellValue(billMap.get(key).getClick());
                cell.setCellStyle(cellStyleAmount);
            } else {
                cell.setCellValue("--");
            }
        }

        cellService.adjustColumnWeight(sheet, 0, column + 1);

        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            workbook.write(outputStream);
            workbook.close();

            return outputStream.toByteArray();
        } catch (IOException e) {
            return null;
        }
    }

    private byte[] generateSignReport(
            String interval,
            String aggregateDownstream,
            List<Sign> signList,
            Map<Integer, ClientPort> clientPortMap,
            Map<Integer, Vendor> vendorMap,
            Map<Integer, VendorPort> vendorPortMap,
            String timezone) {
        TimeZone tz = TimeZone.getTimeZone(timezone);
        SimpleDateFormat dfDate = new SimpleDateFormat("yyyy-MM-dd");
        if (interval.equals("day")) {
            dfDate = new SimpleDateFormat("yyyy-MM-dd");
        }
        if (interval.equals("month")) {
            dfDate = new SimpleDateFormat("yyyy-MM");
        }
        if (interval.equals("year")) {
            dfDate = new SimpleDateFormat("yyyy");
        }
        dfDate.setTimeZone(tz);

        Map<String, Sign> signMap = new HashMap<String, Sign>();
        for (Sign sign : signList) {
            String key = dfDate.format(sign.getDate()) + "|";
            if (aggregateDownstream.equals("vendor") || aggregateDownstream.equals("vendorport")) {
                key += "V" + vendorPortMap.get(sign.getVendorPort()).getVendor().getId() + "|";
            }
            if (aggregateDownstream.equals("vendorport")) {
                key += "VP" + sign.getVendorPort() + "|";
            }

            if (!signMap.containsKey(key)) {
                Sign signNew = new Sign();
                signNew.setDate(sign.getDate());
                signNew.setTagId(sign.getTagId());
                signNew.setVendorPort(sign.getVendorPort());
                signNew.setRequest(0L);
                signNew.setResponse(0L);
                signNew.setImpression(0L);
                signNew.setClick(0L);
                signNew.setCost(0L);
                signNew.setStatus(sign.getStatus());

                signMap.put(key, signNew);
            }

            Sign signAggregated = signMap.get(key);
            signAggregated.setRequest(signAggregated.getRequest() + (sign.getRequest() == null ? 0 : sign.getRequest()));
            signAggregated.setResponse(signAggregated.getResponse() + (sign.getResponse() == null ? 0 : sign.getResponse()));
            signAggregated.setImpression(signAggregated.getImpression() + (sign.getImpression() == null ? 0 : sign.getImpression()));
            signAggregated.setClick(signAggregated.getClick() + (sign.getClick() == null ? 0 : sign.getClick()));
            signAggregated.setCost(signAggregated.getCost() + (sign.getCost() == null ? 0 : sign.getCost()));
            if (!sign.getStatus().equals(signAggregated.getStatus())) {
                signAggregated.setStatus(0);
            }
        }

        List<Timestamp> timestamps = signList.stream().map(sign -> sign.getDate()).distinct().toList();
        Map<Timestamp, Map<Integer, List<Integer>>> pairedClientPortMap = new HashMap<Timestamp, Map<Integer, List<Integer>>>();
        for (Timestamp timestamp : timestamps) {
            List<Sign> signsOneday = signList.stream().filter(sign -> sign.getDate().equals(timestamp)).toList();
            Map<Integer, List<Integer>> pairedClientPortMapOneday = connectionService.getPairedClientPortMap(signsOneday.stream().map(sign -> sign.getVendorPort()).distinct().toList(), timestamp);
            pairedClientPortMap.put(timestamp, pairedClientPortMapOneday);
        }

        Workbook workbook = new XSSFWorkbook();
        DataFormat format = workbook.createDataFormat();
        CellStyle cellStyleAmount = workbook.createCellStyle();
        cellStyleAmount.setDataFormat(format.getFormat("##0"));
        CellStyle cellStyleCost = workbook.createCellStyle();
        cellStyleCost.setDataFormat(format.getFormat("0.00"));
        CellStyle cellStylePercentage = workbook.createCellStyle();
        cellStylePercentage.setDataFormat(format.getFormat("0.00%"));
        Sheet sheet = workbook.createSheet("下游账单");

        Row header = sheet.createRow(0);
        int column = -1;
        Cell headerCell = header.createCell(++column);
        headerCell.setCellValue("时间");
        if (aggregateDownstream.equals("vendor") || aggregateDownstream.equals("vendorport")) {
            headerCell = header.createCell(++column);
            headerCell.setCellValue("下游名称");
        }
        if (aggregateDownstream.equals("vendorport")) {
            headerCell = header.createCell(++column);
            headerCell.setCellValue("代码位");
            headerCell = header.createCell(++column);
            headerCell.setCellValue("代码位名称");
        }
        headerCell = header.createCell(++column);
        headerCell.setCellValue("预估收益");
        headerCell = header.createCell(++column);
        headerCell.setCellValue("展现量");
        headerCell = header.createCell(++column);
        headerCell.setCellValue("点击量");
        headerCell = header.createCell(++column);
        headerCell.setCellValue("点击率");
        headerCell = header.createCell(++column);
        headerCell.setCellValue("eCPM");
        headerCell = header.createCell(++column);
        headerCell.setCellValue("广告请求量");
        headerCell = header.createCell(++column);
        headerCell.setCellValue("广告响应量");
        headerCell = header.createCell(++column);
        headerCell.setCellValue("广告填充率");

        Integer line = 0;

        List<String> keys = new ArrayList<String>();
        keys.addAll(signMap.keySet());
        keys.sort((a, b) -> b.compareTo(a));

        for (String key : keys) {
            String date = key.split("\\|")[0];
            int index = 0;
            Integer vendorId = -1;
            Integer vendorPortId = -1;

            column = -1;

            if (aggregateDownstream.equals("vendor") || aggregateDownstream.equals("vendorport")) {
                vendorId = Integer.parseInt(key.split("\\|")[++index].substring(1));
                if (!vendorMap.containsKey(vendorId)) {
                    continue;
                }
            }
            if (aggregateDownstream.equals("vendorport")) {
                vendorPortId = Integer.parseInt(key.split("\\|")[++index].substring(2));
                if (!vendorPortMap.containsKey(vendorPortId)) {
                    continue;
                }
            }

            Row row = sheet.createRow(++line);
            Cell cell = row.createCell(++column);
            cell.setCellValue(date);
            if (aggregateDownstream.equals("vendor") || aggregateDownstream.equals("vendorport")) {
                cell = row.createCell(++column);
                cell.setCellValue(vendorMap.get(vendorId).getName());
            }
            if (aggregateDownstream.equals("vendorport")) {
                cell = row.createCell(++column);
                if (vendorPortMap.get(vendorPortId).getMode() != VendorPort.PORT_TYPE_DIRECT) {
                    cell.setCellValue(vendorPortMap.get(vendorPortId).getTagId());
                } else {
                    Integer clientPortId = pairedClientPortMap.get(new Timestamp(signMap.get(key).getDate().getTime())).get(vendorPortId).get(0);
                    cell.setCellValue(clientPortMap.get(clientPortId).getTagId());
                }
                cell = row.createCell(++column);
                cell.setCellValue(vendorPortMap.get(vendorPortId).getName());
            }
            cell = row.createCell(++column);
            cell.setCellValue(Math.round(signMap.get(key).getCost() / 1000.0) / 100.0);
            cell.setCellStyle(cellStyleCost);
            cell = row.createCell(++column);
            if (signMap.get(key).getImpression() != null) {
                cell.setCellValue(signMap.get(key).getImpression());
                cell.setCellStyle(cellStyleAmount);
            } else {
                cell.setCellValue("--");
            }
            cell = row.createCell(++column);
            if (signMap.get(key).getClick() != null) {
                cell.setCellValue(signMap.get(key).getClick());
                cell.setCellStyle(cellStyleAmount);
            } else {
                cell.setCellValue("--");
            }
            cell = row.createCell(++column);
            if (signMap.get(key).getImpression() != null && signMap.get(key).getClick() != null && signMap.get(key).getImpression() != 0) {
                cell.setCellValue(1.0 * signMap.get(key).getClick() / signMap.get(key).getImpression());
                cell.setCellStyle(cellStylePercentage);
            } else {
                cell.setCellValue("--");
            }
            cell = row.createCell(++column);
            if (signMap.get(key).getImpression() != null && signMap.get(key).getCost() != null && signMap.get(key).getImpression() != 0) {
                cell.setCellValue(Math.round(1.0 * signMap.get(key).getCost() / signMap.get(key).getImpression()) / 100.0);
                cell.setCellStyle(cellStyleCost);
            } else {
                cell.setCellValue("--");
            }
            cell = row.createCell(++column);
            if (signMap.get(key).getRequest() != null) {
                cell.setCellValue(signMap.get(key).getRequest());
                cell.setCellStyle(cellStyleAmount);
            } else {
                cell.setCellValue("--");
            }
            cell = row.createCell(++column);
            if (signMap.get(key).getResponse() != null) {
                cell.setCellValue(signMap.get(key).getResponse());
                cell.setCellStyle(cellStyleAmount);
            } else {
                cell.setCellValue("--");
            }
            cell = row.createCell(++column);
            if (signMap.get(key).getRequest() != null && signMap.get(key).getClick() != null && signMap.get(key).getRequest() != 0) {
                cell.setCellValue(1.0 * signMap.get(key).getResponse() / signMap.get(key).getRequest());
                cell.setCellStyle(cellStylePercentage);
            } else {
                cell.setCellValue("--");
            }
        }

        cellService.adjustColumnWeight(sheet, 0, column + 1);

        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            workbook.write(outputStream);
            workbook.close();

            return outputStream.toByteArray();
        } catch (IOException e) {
            return null;
        }
    }

    private void verifyBillList(
            Authentication authentication,
            List<Bill> billList,
            String timezone) {
        List<ClientPort> clientPortList = clientPortService.getClientPortList(authentication);

        Set<Integer> clientPortIdSet = new HashSet<Integer>();
        Timestamp start = new Timestamp(System.currentTimeMillis());
        Timestamp end = new Timestamp(0);

        for (Bill bill : billList) {
            final String clientPortTagIdFragment = bill.getTagId();
            List<Integer> matchedClientPortIdList = clientPortList.stream().filter(clientPort -> clientPort.getTagId().split("\\|")[0].equals(clientPortTagIdFragment)).map(ClientPort::getId).collect(Collectors.toList());
            clientPortIdSet.addAll(matchedClientPortIdList);
            if (start.after(bill.getDate())) {
                start = bill.getDate();
            }
            if (end.before(bill.getDate())) {
                end = bill.getDate();
            }
        }
        List<Integer> clientPortIdList = clientPortIdSet.stream().sorted().collect(Collectors.toList());
        Map<Long, Map<Integer, List<Integer>>> pairedVendorPortIdMap = connectionService.getPairedVendorPortMap(clientPortIdList, start, end);

        Iterator<Bill> it = billList.iterator();
        while (it.hasNext()) {
            Bill bill = it.next();
            final String clientPortTagIdFragment = bill.getTagId();
            List<Integer> matchedClientPortIdList = clientPortList.stream().filter(clientPort -> clientPort.getTagId().split("\\|")[0].equals(clientPortTagIdFragment)).map(ClientPort::getId).collect(Collectors.toList());

            Map<Integer, Integer> candidateClientPortIdMap = new HashMap<Integer, Integer>();
            for (Integer clientPortId : matchedClientPortIdList) {
                Map<Integer, List<Integer>> matchedClientPortTagIdList = pairedVendorPortIdMap.get(bill.getDate().getTime());
                if (matchedClientPortTagIdList == null || !matchedClientPortTagIdList.containsKey(clientPortId)) {
                    continue;
                }
                List<Integer> pairedVendorPortIdList = matchedClientPortTagIdList.get(clientPortId);
                candidateClientPortIdMap.put(clientPortId, pairedVendorPortIdList.size());
            }
            Integer maxSize = 0;
            List<Integer> maxSizeClientPortIdList = new ArrayList<Integer>();
            for (Integer clientPortId : candidateClientPortIdMap.keySet()) {
                if (candidateClientPortIdMap.get(clientPortId) == maxSize) {
                    maxSizeClientPortIdList.add(clientPortId);
                }
                if (candidateClientPortIdMap.get(clientPortId) > maxSize) {
                    maxSize = candidateClientPortIdMap.get(clientPortId);
                    maxSizeClientPortIdList.clear();
                    maxSizeClientPortIdList.add(clientPortId);
                }
            }

            if (maxSize == 0) {
                it.remove();
                continue;
            }
            if (maxSizeClientPortIdList.size() > 1) {
                System.out.println("WARNING: client port tag id matches multiple client ports.");
            }

            bill.setClientPort(maxSizeClientPortIdList.get(0));
        }
    }

    private void verifySignList(
            Authentication authentication,
            List<Sign> signList,
            String timezone) {
        List<VendorPort> vendorPorts = vendorPortService.getVendorPortList(authentication);
        List<String> vendorPortTagIdList = vendorPorts.stream().map(vendorPort -> vendorPort.getTagId()).collect(Collectors.toList());

        Iterator<Sign> it = signList.iterator();
        while (it.hasNext()) {
            Sign sign = it.next();
            final String vendorPortTagId = sign.getTagId();
            List<String> matchedVendorPortTagIdList = vendorPortTagIdList.stream().filter(tagId -> tagId.equals(vendorPortTagId)).collect(Collectors.toList());
            if (matchedVendorPortTagIdList.isEmpty()) {
                it.remove();
                continue;
            }
            if (matchedVendorPortTagIdList.size() > 1) {
                System.out.println("WARNING: vendor port tag id matches multiple vendor ports.");
            }
        }
    }

}
