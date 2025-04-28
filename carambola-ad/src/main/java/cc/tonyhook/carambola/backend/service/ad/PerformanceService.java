package cc.tonyhook.carambola.backend.service.ad;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.DataFormat;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.BeanUtils;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import cc.tonyhook.carambola.backend.dao.ad.FinanceBundleRepository;
import cc.tonyhook.carambola.backend.dao.ad.FinanceRepository;
import cc.tonyhook.carambola.backend.dao.ad.PerformanceBundleRepository;
import cc.tonyhook.carambola.backend.dao.ad.PerformanceClientBundleDayRepository;
import cc.tonyhook.carambola.backend.dao.ad.PerformanceClientBundleHourRepository;
import cc.tonyhook.carambola.backend.dao.ad.PerformanceClientBundleQuarterRepository;
import cc.tonyhook.carambola.backend.dao.ad.PerformanceClientDayRepository;
import cc.tonyhook.carambola.backend.dao.ad.PerformanceClientHourRepository;
import cc.tonyhook.carambola.backend.dao.ad.PerformanceClientQuarterRepository;
import cc.tonyhook.carambola.backend.dao.ad.PerformanceRepository;
import cc.tonyhook.carambola.backend.dao.ad.PerformanceVendorBundleDayRepository;
import cc.tonyhook.carambola.backend.dao.ad.PerformanceVendorBundleHourRepository;
import cc.tonyhook.carambola.backend.dao.ad.PerformanceVendorBundleQuarterRepository;
import cc.tonyhook.carambola.backend.dao.ad.PerformanceVendorDayRepository;
import cc.tonyhook.carambola.backend.dao.ad.PerformanceVendorHourRepository;
import cc.tonyhook.carambola.backend.dao.ad.PerformanceVendorQuarterRepository;
import cc.tonyhook.carambola.backend.entity.ad.Client;
import cc.tonyhook.carambola.backend.entity.ad.ClientMedia;
import cc.tonyhook.carambola.backend.entity.ad.ClientPort;
import cc.tonyhook.carambola.backend.entity.ad.Finance;
import cc.tonyhook.carambola.backend.entity.ad.FinanceBundle;
import cc.tonyhook.carambola.backend.entity.ad.Performance;
import cc.tonyhook.carambola.backend.entity.ad.PerformanceBundle;
import cc.tonyhook.carambola.backend.entity.ad.PerformanceBundleView;
import cc.tonyhook.carambola.backend.entity.ad.PerformanceClient;
import cc.tonyhook.carambola.backend.entity.ad.PerformanceClientBundle;
import cc.tonyhook.carambola.backend.entity.ad.PerformanceClientBundleDay;
import cc.tonyhook.carambola.backend.entity.ad.PerformanceClientBundleHour;
import cc.tonyhook.carambola.backend.entity.ad.PerformanceClientBundleQuarter;
import cc.tonyhook.carambola.backend.entity.ad.PerformanceClientDay;
import cc.tonyhook.carambola.backend.entity.ad.PerformanceClientHour;
import cc.tonyhook.carambola.backend.entity.ad.PerformanceClientQuarter;
import cc.tonyhook.carambola.backend.entity.ad.PerformanceVendor;
import cc.tonyhook.carambola.backend.entity.ad.PerformanceVendorBundle;
import cc.tonyhook.carambola.backend.entity.ad.PerformanceVendorBundleDay;
import cc.tonyhook.carambola.backend.entity.ad.PerformanceVendorBundleHour;
import cc.tonyhook.carambola.backend.entity.ad.PerformanceVendorBundleQuarter;
import cc.tonyhook.carambola.backend.entity.ad.PerformanceVendorDay;
import cc.tonyhook.carambola.backend.entity.ad.PerformanceVendorHour;
import cc.tonyhook.carambola.backend.entity.ad.PerformanceVendorQuarter;
import cc.tonyhook.carambola.backend.entity.ad.PerformanceView;
import cc.tonyhook.carambola.backend.entity.ad.TrafficControl;
import cc.tonyhook.carambola.backend.entity.ad.Vendor;
import cc.tonyhook.carambola.backend.entity.ad.VendorMedia;
import cc.tonyhook.carambola.backend.entity.ad.VendorPort;
import cc.tonyhook.carambola.backend.service.shared.CellService;
import cc.tonyhook.carambola.backend.service.shared.Query;

@Service
public class PerformanceService {

    private final PerformanceRepository performanceRepository;
    private final PerformanceBundleRepository performanceBundleRepository;
    private final PerformanceClientQuarterRepository performanceClientQuarterRepository;
    private final PerformanceClientHourRepository performanceClientHourRepository;
    private final PerformanceClientDayRepository performanceClientDayRepository;
    private final PerformanceVendorQuarterRepository performanceVendorQuarterRepository;
    private final PerformanceVendorHourRepository performanceVendorHourRepository;
    private final PerformanceVendorDayRepository performanceVendorDayRepository;
    private final PerformanceClientBundleQuarterRepository performanceClientBundleQuarterRepository;
    private final PerformanceClientBundleHourRepository performanceClientBundleHourRepository;
    private final PerformanceClientBundleDayRepository performanceClientBundleDayRepository;
    private final PerformanceVendorBundleQuarterRepository performanceVendorBundleQuarterRepository;
    private final PerformanceVendorBundleHourRepository performanceVendorBundleHourRepository;
    private final PerformanceVendorBundleDayRepository performanceVendorBundleDayRepository;
    private final FinanceRepository financeRepository;
    private final FinanceBundleRepository financeBundleRepository;
    private final TrafficControlService trafficControlService;

    private final CellService cellService;
    private final ClientMediaService clientMediaService;
    private final ClientPortService clientPortService;
    private final VendorMediaService vendorMediaService;
    private final VendorPortService vendorPortService;
    private final PartnerService partnerService;

    public PerformanceService(
            PerformanceRepository performanceRepository,
            PerformanceBundleRepository performanceBundleRepository,
            PerformanceClientQuarterRepository performanceClientQuarterRepository,
            PerformanceClientHourRepository performanceClientHourRepository,
            PerformanceClientDayRepository performanceClientDayRepository,
            PerformanceVendorQuarterRepository performanceVendorQuarterRepository,
            PerformanceVendorHourRepository performanceVendorHourRepository,
            PerformanceVendorDayRepository performanceVendorDayRepository,
            PerformanceClientBundleQuarterRepository performanceClientBundleQuarterRepository,
            PerformanceClientBundleHourRepository performanceClientBundleHourRepository,
            PerformanceClientBundleDayRepository performanceClientBundleDayRepository,
            PerformanceVendorBundleQuarterRepository performanceVendorBundleQuarterRepository,
            PerformanceVendorBundleHourRepository performanceVendorBundleHourRepository,
            PerformanceVendorBundleDayRepository performanceVendorBundleDayRepository,
            FinanceRepository financeRepository,
            FinanceBundleRepository financeBundleRepository,
            TrafficControlService trafficControlService,
            CellService cellService,
            ClientMediaService clientMediaService,
            ClientPortService clientPortService,
            VendorMediaService vendorMediaService,
            VendorPortService vendorPortService,
            PartnerService partnerService
    ) {
        this.performanceRepository = performanceRepository;
        this.performanceBundleRepository = performanceBundleRepository;
        this.performanceClientQuarterRepository = performanceClientQuarterRepository;
        this.performanceClientHourRepository = performanceClientHourRepository;
        this.performanceClientDayRepository = performanceClientDayRepository;
        this.performanceVendorQuarterRepository = performanceVendorQuarterRepository;
        this.performanceVendorHourRepository = performanceVendorHourRepository;
        this.performanceVendorDayRepository = performanceVendorDayRepository;
        this.performanceClientBundleQuarterRepository = performanceClientBundleQuarterRepository;
        this.performanceClientBundleHourRepository = performanceClientBundleHourRepository;
        this.performanceClientBundleDayRepository = performanceClientBundleDayRepository;
        this.performanceVendorBundleQuarterRepository = performanceVendorBundleQuarterRepository;
        this.performanceVendorBundleHourRepository = performanceVendorBundleHourRepository;
        this.performanceVendorBundleDayRepository = performanceVendorBundleDayRepository;
        this.financeRepository = financeRepository;
        this.financeBundleRepository = financeBundleRepository;
        this.trafficControlService = trafficControlService;
        this.cellService = cellService;
        this.clientMediaService = clientMediaService;
        this.clientPortService = clientPortService;
        this.vendorMediaService = vendorMediaService;
        this.vendorPortService = vendorPortService;
        this.partnerService = partnerService;
    }

    public List<Performance> addAllPerformance(List<Performance> newPerformanceList) {
        List<Performance> updatedPerformanceList = performanceRepository.saveAll(newPerformanceList);

        return updatedPerformanceList;
    }

    public List<PerformanceBundle> addAllPerformanceBundle(List<PerformanceBundle> newPerformanceBundleList) {
        List<PerformanceBundle> updatedPerformanceBundleList = performanceBundleRepository.saveAll(newPerformanceBundleList);

        return updatedPerformanceBundleList;
    }

    public List<Performance> getPerformanceList(Timestamp start, Timestamp end) {
        List<Performance> performanceList = performanceRepository.findByTimeBetween(start, end);
        return performanceList;
    }

    public List<PerformanceBundle> getPerformanceBundleList(Timestamp start, Timestamp end) {
        List<PerformanceBundle> performanceBundleList = performanceBundleRepository.findByTimeBetween(start, end);
        return performanceBundleList;
    }

    public List<Finance> addAllFinance(List<Finance> newFinanceList) {
        List<Finance> updatedFinanceList = financeRepository.saveAll(newFinanceList);

        return updatedFinanceList;
    }

    public List<FinanceBundle> addAllFinanceBundle(List<FinanceBundle> newFinanceBundleList) {
        List<FinanceBundle> updatedFinanceBundleList = financeBundleRepository.saveAll(newFinanceBundleList);

        return updatedFinanceBundleList;
    }

    public List<Finance> getFinanceList(Timestamp start, Timestamp end) {
        List<Finance> financeList = financeRepository.findByTimeBetween(start, end);
        return financeList;
    }

    public List<FinanceBundle> getFinanceBundleList(Timestamp start, Timestamp end) {
        List<FinanceBundle> financeBundleList = financeBundleRepository.findByTimeBetween(start, end);
        return financeBundleList;
    }

    public List<PerformanceClient> queryPerformanceClientList(
            Authentication authentication,
            Query query,
            String interval,
            Boolean expand,
            Timestamp start,
            Timestamp end,
            String timezone) {
        List<Client> qualifiedClientList = partnerService.getQualifiedClientListWithoutFilterAndSearch(authentication, query);
        List<ClientPort> clientPortList = partnerService.getQualifiedClientPortList(qualifiedClientList, query);
        List<Integer> clientPortIdList = clientPortList.stream().map(ClientPort::getId).distinct().collect(Collectors.toList());
        Query queryVendor = new Query();
        if (query.filter.containsKey("vendor")) {
            queryVendor.filter.put("vendor", query.filter.get("vendor"));
        }
        if (query.filter.containsKey("vendorMedia")) {
            queryVendor.filter.put("vendorMedia", query.filter.get("vendorMedia"));
        }
        if (query.filter.containsKey("vendorPort")) {
            queryVendor.filter.put("vendorPort", query.filter.get("vendorPort"));
        }
        List<Vendor> qualifiedVendorList = partnerService.getQualifiedVendorListWithoutFilterAndSearch(authentication, queryVendor);
        List<VendorPort> vendorPortList = partnerService.getQualifiedVendorPortList(qualifiedVendorList, queryVendor);
        List<Integer> vendorPortIdList = vendorPortList.stream().map(VendorPort::getId).distinct().collect(Collectors.toList());
        if (!query.filter.containsKey("vendorPort") || queryVendor.filter.get("vendorPort").isEmpty() || queryVendor.filter.get("vendorPort").contains("-1")) {
            vendorPortIdList.add(-1);
        }

        if (interval.equals("quarter")) {
            if (expand) {
                List<PerformanceClientQuarter> performanceClientQuarterList = new ArrayList<PerformanceClientQuarter>();
                if (clientPortIdList.size() > 0 && vendorPortIdList.size() == 0) {
                    performanceClientQuarterList = performanceClientQuarterRepository.findDetailByClientPortInAndTimeBetween(clientPortIdList, start, end);
                }
                if (clientPortIdList.size() == 0 && vendorPortIdList.size() > 0) {
                    performanceClientQuarterList = performanceClientQuarterRepository.findDetailByVendorPortInAndTimeBetween(vendorPortIdList, start, end);
                }
                if (clientPortIdList.size() > 0 && vendorPortIdList.size() > 0) {
                    performanceClientQuarterList = performanceClientQuarterRepository.findDetailByClientPortInAndVendorPortInAndTimeBetween(clientPortIdList, vendorPortIdList, start, end);
                }
                return new ArrayList<PerformanceClient>(performanceClientQuarterList);
            } else {
                List<PerformanceClientQuarter> performanceClientQuarterList = performanceClientQuarterRepository.findSummaryByClientPortInAndTimeBetween(clientPortIdList, start, end);
                return new ArrayList<PerformanceClient>(performanceClientQuarterList);
            }
        } else if (interval.equals("hour")) {
            if (expand) {
                List<PerformanceClientHour> performanceClientHourList = new ArrayList<PerformanceClientHour>();
                if (clientPortIdList.size() > 0 && vendorPortIdList.size() == 0) {
                    performanceClientHourList = performanceClientHourRepository.findDetailByClientPortInAndTimeBetween(clientPortIdList, start, end);
                }
                if (clientPortIdList.size() == 0 && vendorPortIdList.size() > 0) {
                    performanceClientHourList = performanceClientHourRepository.findDetailByVendorPortInAndTimeBetween(vendorPortIdList, start, end);
                }
                if (clientPortIdList.size() > 0 && vendorPortIdList.size() > 0) {
                    performanceClientHourList = performanceClientHourRepository.findDetailByClientPortInAndVendorPortInAndTimeBetween(clientPortIdList, vendorPortIdList, start, end);
                }
                return new ArrayList<PerformanceClient>(performanceClientHourList);
            } else {
                List<PerformanceClientHour> performanceClientHourList = performanceClientHourRepository.findSummaryByClientPortInAndTimeBetween(clientPortIdList, start, end);
                return new ArrayList<PerformanceClient>(performanceClientHourList);
            }
        } else {
            if (expand) {
                List<PerformanceClientDay> performanceClientDayList = new ArrayList<PerformanceClientDay>();
                if (clientPortIdList.size() > 0 && vendorPortIdList.size() == 0) {
                    performanceClientDayList = performanceClientDayRepository.findDetailByClientPortInAndTimeBetween(clientPortIdList, start, end);
                }
                if (clientPortIdList.size() == 0 && vendorPortIdList.size() > 0) {
                    performanceClientDayList = performanceClientDayRepository.findDetailByVendorPortInAndTimeBetween(vendorPortIdList, start, end);
                }
                if (clientPortIdList.size() > 0 && vendorPortIdList.size() > 0) {
                    performanceClientDayList = performanceClientDayRepository.findDetailByClientPortInAndVendorPortInAndTimeBetween(clientPortIdList, vendorPortIdList, start, end);
                }
                return new ArrayList<PerformanceClient>(performanceClientDayList);
            } else {
                List<PerformanceClientDay> performanceClientDayList = performanceClientDayRepository.findSummaryByClientPortInAndTimeBetween(clientPortIdList, start, end);
                return new ArrayList<PerformanceClient>(performanceClientDayList);
            }
        }
    }

    public List<PerformanceVendor> queryPerformanceVendorList(
            Authentication authentication,
            Query query,
            String interval,
            Boolean expand,
            Timestamp start,
            Timestamp end,
            String timezone) {
        List<Vendor> qualifiedVendorList = partnerService.getQualifiedVendorListWithoutFilterAndSearch(authentication, query);
        List<VendorPort> vendorPortList = partnerService.getQualifiedVendorPortList(qualifiedVendorList, query);
        List<Integer> vendorPortIdList = vendorPortList.stream().map(VendorPort::getId).distinct().collect(Collectors.toList());
        Query queryClient = new Query();
        if (query.filter.containsKey("client")) {
            queryClient.filter.put("client", query.filter.get("client"));
        }
        if (query.filter.containsKey("clientMedia")) {
            queryClient.filter.put("clientMedia", query.filter.get("clientMedia"));
        }
        if (query.filter.containsKey("clientPort")) {
            queryClient.filter.put("clientPort", query.filter.get("clientPort"));
        }
        List<Client> qualifiedClientList = partnerService.getQualifiedClientListWithoutFilterAndSearch(authentication, queryClient);
        List<ClientPort> clientPortList = partnerService.getQualifiedClientPortList(qualifiedClientList, queryClient);
        List<Integer> clientPortIdList = clientPortList.stream().map(ClientPort::getId).distinct().collect(Collectors.toList());
        if (!queryClient.filter.containsKey("clientPort") || queryClient.filter.get("clientPort").isEmpty() || queryClient.filter.get("clientPort").contains("-1")) {
            clientPortIdList.add(-1);
        }

        if (interval.equals("quarter")) {
            if (expand) {
                List<PerformanceVendorQuarter> performanceVendorQuarterList = new ArrayList<PerformanceVendorQuarter>();
                if (clientPortIdList.size() > 0 && vendorPortIdList.size() == 0) {
                    performanceVendorQuarterList = performanceVendorQuarterRepository.findDetailByClientPortInAndTimeBetween(clientPortIdList, start, end);
                }
                if (clientPortIdList.size() == 0 && vendorPortIdList.size() > 0) {
                    performanceVendorQuarterList = performanceVendorQuarterRepository.findDetailByVendorPortInAndTimeBetween(vendorPortIdList, start, end);
                }
                if (clientPortIdList.size() > 0 && vendorPortIdList.size() > 0) {
                    performanceVendorQuarterList = performanceVendorQuarterRepository.findDetailByClientPortInAndVendorPortInAndTimeBetween(clientPortIdList, vendorPortIdList, start, end);
                }
                return new ArrayList<PerformanceVendor>(performanceVendorQuarterList);
            } else {
                List<PerformanceVendorQuarter> performanceVendorQuarterList = performanceVendorQuarterRepository.findSummaryByVendorPortInAndTimeBetween(vendorPortIdList, start, end);
                return new ArrayList<PerformanceVendor>(performanceVendorQuarterList);
            }
        } else if (interval.equals("hour")) {
            if (expand) {
                List<PerformanceVendorHour> performanceVendorHourList = new ArrayList<PerformanceVendorHour>();
                if (clientPortIdList.size() > 0 && vendorPortIdList.size() == 0) {
                    performanceVendorHourList = performanceVendorHourRepository.findDetailByClientPortInAndTimeBetween(clientPortIdList, start, end);
                }
                if (clientPortIdList.size() == 0 && vendorPortIdList.size() > 0) {
                    performanceVendorHourList = performanceVendorHourRepository.findDetailByVendorPortInAndTimeBetween(vendorPortIdList, start, end);
                }
                if (clientPortIdList.size() > 0 && vendorPortIdList.size() > 0) {
                    performanceVendorHourList = performanceVendorHourRepository.findDetailByClientPortInAndVendorPortInAndTimeBetween(clientPortIdList, vendorPortIdList, start, end);
                }
                return new ArrayList<PerformanceVendor>(performanceVendorHourList);
            } else {
                List<PerformanceVendorHour> performanceVendorHourList = performanceVendorHourRepository.findSummaryByVendorPortInAndTimeBetween(vendorPortIdList, start, end);
                return new ArrayList<PerformanceVendor>(performanceVendorHourList);
            }
        } else {
            if (expand) {
                List<PerformanceVendorDay> performanceVendorDayList = new ArrayList<PerformanceVendorDay>();
                if (clientPortIdList.size() > 0 && vendorPortIdList.size() == 0) {
                    performanceVendorDayList = performanceVendorDayRepository.findDetailByClientPortInAndTimeBetween(clientPortIdList, start, end);
                }
                if (clientPortIdList.size() == 0 && vendorPortIdList.size() > 0) {
                    performanceVendorDayList = performanceVendorDayRepository.findDetailByVendorPortInAndTimeBetween(vendorPortIdList, start, end);
                }
                if (clientPortIdList.size() > 0 && vendorPortIdList.size() > 0) {
                    performanceVendorDayList = performanceVendorDayRepository.findDetailByClientPortInAndVendorPortInAndTimeBetween(clientPortIdList, vendorPortIdList, start, end);
                }
                return new ArrayList<PerformanceVendor>(performanceVendorDayList);
            } else {
                List<PerformanceVendorDay> performanceVendorDayList = performanceVendorDayRepository.findSummaryByVendorPortInAndTimeBetween(vendorPortIdList, start, end);
                return new ArrayList<PerformanceVendor>(performanceVendorDayList);
            }
        }
    }

    public List<PerformanceClientBundle> queryPerformanceClientBundleList(
            Authentication authentication,
            Query query,
            String interval,
            Boolean expand,
            Timestamp start,
            Timestamp end,
            String timezone) {
        List<Client> qualifiedClientList = partnerService.getQualifiedClientListWithoutFilterAndSearch(authentication, query);
        List<ClientPort> clientPortList = partnerService.getQualifiedClientPortList(qualifiedClientList, query);
        List<Integer> clientPortIdList = clientPortList.stream().map(ClientPort::getId).distinct().collect(Collectors.toList());
        Query queryVendor = new Query();
        if (query.filter.containsKey("vendor")) {
            queryVendor.filter.put("vendor", query.filter.get("vendor"));
        }
        if (query.filter.containsKey("vendorMedia")) {
            queryVendor.filter.put("vendorMedia", query.filter.get("vendorMedia"));
        }
        if (query.filter.containsKey("vendorPort")) {
            queryVendor.filter.put("vendorPort", query.filter.get("vendorPort"));
        }
        List<Vendor> qualifiedVendorList = partnerService.getQualifiedVendorListWithoutFilterAndSearch(authentication, queryVendor);
        List<VendorPort> vendorPortList = partnerService.getQualifiedVendorPortList(qualifiedVendorList, queryVendor);
        List<Integer> vendorPortIdList = vendorPortList.stream().map(VendorPort::getId).distinct().collect(Collectors.toList());
        if (!queryVendor.filter.containsKey("vendorPort") || queryVendor.filter.get("vendorPort").isEmpty() || queryVendor.filter.get("vendorPort").contains("-1")) {
            vendorPortIdList.add(-1);
        }

        if (interval.equals("quarter")) {
            if (expand) {
                List<PerformanceClientBundleQuarter> performanceClientQuarterBundleList = performanceClientBundleQuarterRepository.findDetailByClientPortInAndVendorPortInAndTimeBetween(clientPortIdList, vendorPortIdList, start, end);
                return new ArrayList<PerformanceClientBundle>(performanceClientQuarterBundleList);
            } else {
                List<PerformanceClientBundleQuarter> performanceClientQuarterBundleList = performanceClientBundleQuarterRepository.findSummaryByClientPortInAndTimeBetween(clientPortIdList, start, end);
                return new ArrayList<PerformanceClientBundle>(performanceClientQuarterBundleList);
            }
        } else if (interval.equals("hour")) {
            if (expand) {
                List<PerformanceClientBundleHour> performanceClientBundleHourList = performanceClientBundleHourRepository.findDetailByClientPortInAndVendorPortInAndTimeBetween(clientPortIdList, vendorPortIdList, start, end);
                return new ArrayList<PerformanceClientBundle>(performanceClientBundleHourList);
            } else {
                List<PerformanceClientBundleHour> performanceClientBundleHourList = performanceClientBundleHourRepository.findSummaryByClientPortInAndTimeBetween(clientPortIdList, start, end);
                return new ArrayList<PerformanceClientBundle>(performanceClientBundleHourList);
            }
        } else {
            if (expand) {
                List<PerformanceClientBundleDay> performanceClientBundleDayList = performanceClientBundleDayRepository.findDetailByClientPortInAndVendorPortInAndTimeBetween(clientPortIdList, vendorPortIdList, start, end);
                return new ArrayList<PerformanceClientBundle>(performanceClientBundleDayList);
            } else {
                List<PerformanceClientBundleDay> performanceClientBundleDayList = performanceClientBundleDayRepository.findSummaryByClientPortInAndTimeBetween(clientPortIdList, start, end);
                return new ArrayList<PerformanceClientBundle>(performanceClientBundleDayList);
            }
        }
    }

    public List<PerformanceVendorBundle> queryPerformanceVendorBundleList(
            Authentication authentication,
            Query query,
            String interval,
            Boolean expand,
            Timestamp start,
            Timestamp end,
            String timezone) {
        List<Vendor> qualifiedVendorList = partnerService.getQualifiedVendorListWithoutFilterAndSearch(authentication, query);
        List<VendorPort> vendorPortList = partnerService.getQualifiedVendorPortList(qualifiedVendorList, query);
        List<Integer> vendorPortIdList = vendorPortList.stream().map(VendorPort::getId).distinct().collect(Collectors.toList());
        Query queryClient = new Query();
        if (query.filter.containsKey("client")) {
            queryClient.filter.put("client", query.filter.get("client"));
        }
        if (query.filter.containsKey("clientMedia")) {
            queryClient.filter.put("clientMedia", query.filter.get("clientMedia"));
        }
        if (query.filter.containsKey("clientPort")) {
            queryClient.filter.put("clientPort", query.filter.get("clientPort"));
        }
        List<Client> qualifiedClientList = partnerService.getQualifiedClientListWithoutFilterAndSearch(authentication, queryClient);
        List<ClientPort> clientPortList = partnerService.getQualifiedClientPortList(qualifiedClientList, queryClient);
        List<Integer> clientPortIdList = clientPortList.stream().map(ClientPort::getId).distinct().collect(Collectors.toList());
        if (!queryClient.filter.containsKey("clientPort") || queryClient.filter.get("clientPort").isEmpty() || queryClient.filter.get("clientPort").contains("-1")) {
            clientPortIdList.add(-1);
        }

        if (interval.equals("quarter")) {
            if (expand) {
                List<PerformanceVendorBundleQuarter> performanceVendorBundleQuarterList = performanceVendorBundleQuarterRepository.findDetailByClientPortInAndVendorPortInAndTimeBetween(clientPortIdList, vendorPortIdList, start, end);
                return new ArrayList<PerformanceVendorBundle>(performanceVendorBundleQuarterList);
            } else {
                List<PerformanceVendorBundleQuarter> performanceVendorBundleQuarterList = performanceVendorBundleQuarterRepository.findSummaryByVendorPortInAndTimeBetween(vendorPortIdList, start, end);
                return new ArrayList<PerformanceVendorBundle>(performanceVendorBundleQuarterList);
            }
        } else if (interval.equals("hour")) {
            if (expand) {
                List<PerformanceVendorBundleHour> performanceVendorBundleHourList = performanceVendorBundleHourRepository.findDetailByClientPortInAndVendorPortInAndTimeBetween(clientPortIdList, vendorPortIdList, start, end);
                return new ArrayList<PerformanceVendorBundle>(performanceVendorBundleHourList);
            } else {
                List<PerformanceVendorBundleHour> performanceVendorBundleHourList = performanceVendorBundleHourRepository.findSummaryByVendorPortInAndTimeBetween(vendorPortIdList, start, end);
                return new ArrayList<PerformanceVendorBundle>(performanceVendorBundleHourList);
            }
        } else {
            if (expand) {
                List<PerformanceVendorBundleDay> performanceVendorBundleDayList = performanceVendorBundleDayRepository.findDetailByClientPortInAndVendorPortInAndTimeBetween(clientPortIdList, vendorPortIdList, start, end);
                return new ArrayList<PerformanceVendorBundle>(performanceVendorBundleDayList);
            } else {
                List<PerformanceVendorBundleDay> performanceVendorBundleDayList = performanceVendorBundleDayRepository.findSummaryByVendorPortInAndTimeBetween(vendorPortIdList, start, end);
                return new ArrayList<PerformanceVendorBundle>(performanceVendorBundleDayList);
            }
        }
    }

    public byte[] downloadPerformanceClientList(
            Authentication authentication,
            Query query,
            String interval,
            Timestamp start,
            Timestamp end,
            String timezone) {
        List<Client> qualifiedClientList = partnerService.getQualifiedClientListWithoutFilterAndSearch(authentication, query);
        List<ClientMedia> clientMediaList = partnerService.getQualifiedClientMediaListWithoutFilterAndSearch(qualifiedClientList, query);
        List<ClientPort> clientPortList = partnerService.getQualifiedClientPortList(qualifiedClientList, query);
        Map<Integer, Client> clientMap = qualifiedClientList.stream().collect(Collectors.toMap(Client::getId, Function.identity(), (first, second) -> first));
        Map<Integer, ClientMedia> clientMediaMap = clientMediaList.stream().collect(Collectors.toMap(ClientMedia::getId, Function.identity(), (first, second) -> first));
        Map<Integer, ClientPort> clientPortMap = clientPortList.stream().collect(Collectors.toMap(ClientPort::getId, Function.identity(), (first, second) -> first));

        List<Vendor> qualifiedVendorList = partnerService.getQualifiedVendorListWithoutFilterAndSearch(authentication, query);
        // full media list
        List<VendorMedia> vendorMediaList = vendorMediaService.getVendorMediaList(authentication, qualifiedVendorList);
        // full port list
        List<VendorPort> vendorPortList = vendorPortService.getVendorPortList(authentication, qualifiedVendorList);
        Map<Integer, Vendor> vendorMap = qualifiedVendorList.stream().collect(Collectors.toMap(Vendor::getId, Function.identity(), (first, second) -> first));
        Map<Integer, VendorMedia> vendorMediaMap = vendorMediaList.stream().collect(Collectors.toMap(VendorMedia::getId, Function.identity(), (first, second) -> first));
        Map<Integer, VendorPort> vendorPortMap = vendorPortList.stream().collect(Collectors.toMap(VendorPort::getId, Function.identity(), (first, second) -> first));

        List<PerformanceClient> performanceClientList = queryPerformanceClientList(authentication, query, interval, true, start, end, timezone);

        List<PerformanceView> performanceClientViewList = new ArrayList<PerformanceView>();
        Map<String, PerformanceView> performanceClientViewMap = new HashMap<String, PerformanceView>();

        for (PerformanceClient performanceClient : performanceClientList) {
            PerformanceView performanceClientView = convertClientToView(performanceClient, interval, timezone);

            String key = performanceClient.getTime().getTime() + "|" + performanceClient.getClientPort() + "|" + performanceClient.getVendorPort();
            if (!performanceClientViewMap.containsKey(key)) {
                performanceClientViewList.add(performanceClientView);
                performanceClientViewMap.put(key, performanceClientView);
            } else {
                PerformanceView existedPerformanceClientView = performanceClientViewMap.get(key);
                if (existedPerformanceClientView.getStart().after(performanceClient.getTime())) {
                    existedPerformanceClientView.setStart(performanceClient.getTime());
                }
                if (existedPerformanceClientView.getEnd().before(performanceClient.getTime())) {
                    existedPerformanceClientView.setEnd(performanceClient.getTime());
                }
                existedPerformanceClientView.setRequest(existedPerformanceClientView.getRequest() + performanceClientView.getRequest());
                existedPerformanceClientView.setResponse(existedPerformanceClientView.getResponse() + performanceClientView.getResponse());
                existedPerformanceClientView.setRequestv(existedPerformanceClientView.getRequestv() + performanceClientView.getRequestv());
                existedPerformanceClientView.setResponsev(existedPerformanceClientView.getResponsev() + performanceClientView.getResponsev());
                existedPerformanceClientView.setImpression(existedPerformanceClientView.getImpression() + performanceClientView.getImpression());
                existedPerformanceClientView.setClick(existedPerformanceClientView.getClick() + performanceClientView.getClick());
                existedPerformanceClientView.setIncome(existedPerformanceClientView.getIncome() + performanceClientView.getIncome());
                existedPerformanceClientView.setOutcomeUpstream(existedPerformanceClientView.getOutcomeUpstream() + performanceClientView.getOutcomeUpstream());
                existedPerformanceClientView.setOutcomeRebate(existedPerformanceClientView.getOutcomeRebate() + performanceClientView.getOutcomeRebate());
                existedPerformanceClientView.setOutcomeDownstream(existedPerformanceClientView.getOutcomeDownstream() + performanceClientView.getOutcomeDownstream());
            }
        }

        return generatePerformanceReport(
            "client",
            query,
            performanceClientViewMap,
            start,
            end,
            clientMap,
            clientMediaMap,
            clientPortMap,
            vendorMap,
            vendorMediaMap,
            vendorPortMap,
            interval,
            timezone);
    }

    public byte[] downloadPerformanceVendorList(
            Authentication authentication,
            Query query,
            String interval,
            Timestamp start,
            Timestamp end,
            String timezone) {
        List<Client> qualifiedClientList = partnerService.getQualifiedClientListWithoutFilterAndSearch(authentication, query);
        // full media list
        List<ClientMedia> clientMediaList = clientMediaService.getClientMediaList(authentication, qualifiedClientList);
        // full port list
        List<ClientPort> clientPortList = clientPortService.getClientPortList(authentication, qualifiedClientList);
        Map<Integer, Client> clientMap = qualifiedClientList.stream().collect(Collectors.toMap(Client::getId, Function.identity(), (first, second) -> first));
        Map<Integer, ClientMedia> clientMediaMap = clientMediaList.stream().collect(Collectors.toMap(ClientMedia::getId, Function.identity(), (first, second) -> first));
        Map<Integer, ClientPort> clientPortMap = clientPortList.stream().collect(Collectors.toMap(ClientPort::getId, Function.identity(), (first, second) -> first));

        List<Vendor> qualifiedVendorList = partnerService.getQualifiedVendorListWithoutFilterAndSearch(authentication, query);
        List<VendorMedia> vendorMediaList = partnerService.getQualifiedVendorMediaListWithoutFilterAndSearch(qualifiedVendorList, query);
        List<VendorPort> vendorPortList = partnerService.getQualifiedVendorPortList(qualifiedVendorList, query);
        Map<Integer, Vendor> vendorMap = qualifiedVendorList.stream().collect(Collectors.toMap(Vendor::getId, Function.identity(), (first, second) -> first));
        Map<Integer, VendorMedia> vendorMediaMap = vendorMediaList.stream().collect(Collectors.toMap(VendorMedia::getId, Function.identity(), (first, second) -> first));
        Map<Integer, VendorPort> vendorPortMap = vendorPortList.stream().collect(Collectors.toMap(VendorPort::getId, Function.identity(), (first, second) -> first));

        List<PerformanceVendor> performanceVendorList = queryPerformanceVendorList(authentication, query, interval, true, start, end, timezone);

        List<PerformanceView> performanceVendorViewList = new ArrayList<PerformanceView>();
        Map<String, PerformanceView> performanceVendorViewMap = new HashMap<String, PerformanceView>();

        for (PerformanceVendor performanceVendor : performanceVendorList) {
            PerformanceView performanceVendorView = convertVendorToView(performanceVendor, interval, timezone);

            String key = performanceVendor.getTime().getTime() + "|" + performanceVendor.getClientPort() + "|" + performanceVendor.getVendorPort();
            if (!performanceVendorViewMap.containsKey(key)) {
                performanceVendorViewList.add(performanceVendorView);
                performanceVendorViewMap.put(key, performanceVendorView);
            } else {
                PerformanceView existedPerformanceVendorView = performanceVendorViewMap.get(key);
                if (existedPerformanceVendorView.getStart().after(performanceVendor.getTime())) {
                    existedPerformanceVendorView.setStart(performanceVendor.getTime());
                }
                if (existedPerformanceVendorView.getEnd().before(performanceVendor.getTime())) {
                    existedPerformanceVendorView.setEnd(performanceVendor.getTime());
                }
                existedPerformanceVendorView.setRequest(existedPerformanceVendorView.getRequest() + performanceVendorView.getRequest());
                existedPerformanceVendorView.setResponse(existedPerformanceVendorView.getResponse() + performanceVendorView.getResponse());
                existedPerformanceVendorView.setRequestv(existedPerformanceVendorView.getRequestv() + performanceVendorView.getRequestv());
                existedPerformanceVendorView.setResponsev(existedPerformanceVendorView.getResponsev() + performanceVendorView.getResponsev());
                existedPerformanceVendorView.setImpression(existedPerformanceVendorView.getImpression() + performanceVendorView.getImpression());
                existedPerformanceVendorView.setClick(existedPerformanceVendorView.getClick() + performanceVendorView.getClick());
                existedPerformanceVendorView.setIncome(existedPerformanceVendorView.getIncome() + performanceVendorView.getIncome());
                existedPerformanceVendorView.setOutcomeUpstream(existedPerformanceVendorView.getOutcomeUpstream() + performanceVendorView.getOutcomeUpstream());
                existedPerformanceVendorView.setOutcomeRebate(existedPerformanceVendorView.getOutcomeRebate() + performanceVendorView.getOutcomeRebate());
                existedPerformanceVendorView.setOutcomeDownstream(existedPerformanceVendorView.getOutcomeDownstream() + performanceVendorView.getOutcomeDownstream());
            }
        }

        return generatePerformanceReport(
            "vendor",
            query,
            performanceVendorViewMap,
            start,
            end,
            clientMap,
            clientMediaMap,
            clientPortMap,
            vendorMap,
            vendorMediaMap,
            vendorPortMap,
            interval,
            timezone);
    }

    public byte[] downloadPerformanceClientBundleList(
            Authentication authentication,
            Query query,
            String interval,
            Timestamp start,
            Timestamp end,
            String timezone) {
        List<Client> qualifiedClientList = partnerService.getQualifiedClientListWithoutFilterAndSearch(authentication, query);
        List<ClientMedia> clientMediaList = partnerService.getQualifiedClientMediaListWithoutFilterAndSearch(qualifiedClientList, query);
        List<ClientPort> clientPortList = partnerService.getQualifiedClientPortList(qualifiedClientList, query);
        List<Integer> clientPortIdList = clientPortList.stream().map(ClientPort::getId).distinct().collect(Collectors.toList());
        Map<Integer, Client> clientMap = qualifiedClientList.stream().collect(Collectors.toMap(Client::getId, Function.identity(), (first, second) -> first));
        Map<Integer, ClientMedia> clientMediaMap = clientMediaList.stream().collect(Collectors.toMap(ClientMedia::getId, Function.identity(), (first, second) -> first));
        Map<Integer, ClientPort> clientPortMap = clientPortList.stream().collect(Collectors.toMap(ClientPort::getId, Function.identity(), (first, second) -> first));

        List<Vendor> qualifiedVendorList = partnerService.getQualifiedVendorListWithoutFilterAndSearch(authentication, query);
        // full media list
        List<VendorMedia> vendorMediaList = vendorMediaService.getVendorMediaList(authentication, qualifiedVendorList);
        // full port list
        List<VendorPort> vendorPortList = vendorPortService.getVendorPortList(authentication, qualifiedVendorList);
        List<Integer> vendorPortIdList = vendorPortList.stream().map(VendorPort::getId).distinct().collect(Collectors.toList());
        Map<Integer, Vendor> vendorMap = qualifiedVendorList.stream().collect(Collectors.toMap(Vendor::getId, Function.identity(), (first, second) -> first));
        Map<Integer, VendorMedia> vendorMediaMap = vendorMediaList.stream().collect(Collectors.toMap(VendorMedia::getId, Function.identity(), (first, second) -> first));
        Map<Integer, VendorPort> vendorPortMap = vendorPortList.stream().collect(Collectors.toMap(VendorPort::getId, Function.identity(), (first, second) -> first));

        List<PerformanceClientBundle> performanceClientBundleList = new ArrayList<PerformanceClientBundle>();
        if (interval.equals("quarter")) {
            List<PerformanceClientBundleQuarter> performanceClientBundleQuarterList = performanceClientBundleQuarterRepository.findDetailByClientPortInAndVendorPortInAndTimeBetween(clientPortIdList, vendorPortIdList, start, end);
            performanceClientBundleList.addAll(performanceClientBundleQuarterList);
        } else if (interval.equals("hour")) {
            List<PerformanceClientBundleHour> performanceClientBundleHourList = performanceClientBundleHourRepository.findDetailByClientPortInAndVendorPortInAndTimeBetween(clientPortIdList, vendorPortIdList, start, end);
            performanceClientBundleList.addAll(performanceClientBundleHourList);
        } else {
            List<PerformanceClientBundleDay> performanceClientBundleDayList = performanceClientBundleDayRepository.findDetailByClientPortInAndVendorPortInAndTimeBetween(clientPortIdList, vendorPortIdList, start, end);
            performanceClientBundleList.addAll(performanceClientBundleDayList);
        }

        List<PerformanceBundleView> performanceClientBundleViewList = new ArrayList<PerformanceBundleView>();
        Map<String, PerformanceBundleView> performanceClientBundleViewMap = new HashMap<String, PerformanceBundleView>();
        List<TrafficControl> trafficControlList = trafficControlService.getTrafficControlList(authentication, query);
        Map<String, TrafficControl> trafficControlMap = new HashMap<String, TrafficControl>();

        for (PerformanceClientBundle performanceClientBundle : performanceClientBundleList) {
            PerformanceBundleView performanceClientBundleView = convertClientBundleToView(performanceClientBundle, interval, timezone);

            String key = performanceClientBundle.getTime().getTime() + "|" + performanceClientBundle.getClientPort() + "|" + performanceClientBundle.getVendorPort() + "|" + performanceClientBundle.getBundle();
            if (!performanceClientBundleViewMap.containsKey(key)) {
                performanceClientBundleViewList.add(performanceClientBundleView);
                performanceClientBundleViewMap.put(key, performanceClientBundleView);
            } else {
                PerformanceView existedPerformanceClientBundleView = performanceClientBundleViewMap.get(key);
                if (existedPerformanceClientBundleView.getStart().after(performanceClientBundle.getTime())) {
                    existedPerformanceClientBundleView.setStart(performanceClientBundle.getTime());
                }
                if (existedPerformanceClientBundleView.getEnd().before(performanceClientBundle.getTime())) {
                    existedPerformanceClientBundleView.setEnd(performanceClientBundle.getTime());
                }
                existedPerformanceClientBundleView.setRequest(existedPerformanceClientBundleView.getRequest() + performanceClientBundleView.getRequest());
                existedPerformanceClientBundleView.setResponse(existedPerformanceClientBundleView.getResponse() + performanceClientBundleView.getResponse());
                existedPerformanceClientBundleView.setRequestv(existedPerformanceClientBundleView.getRequestv() + performanceClientBundleView.getRequestv());
                existedPerformanceClientBundleView.setResponsev(existedPerformanceClientBundleView.getResponsev() + performanceClientBundleView.getResponsev());
                existedPerformanceClientBundleView.setImpression(existedPerformanceClientBundleView.getImpression() + performanceClientBundleView.getImpression());
                existedPerformanceClientBundleView.setClick(existedPerformanceClientBundleView.getClick() + performanceClientBundleView.getClick());
                existedPerformanceClientBundleView.setIncome(existedPerformanceClientBundleView.getIncome() + performanceClientBundleView.getIncome());
                existedPerformanceClientBundleView.setOutcomeUpstream(existedPerformanceClientBundleView.getOutcomeUpstream() + performanceClientBundleView.getOutcomeUpstream());
                existedPerformanceClientBundleView.setOutcomeRebate(existedPerformanceClientBundleView.getOutcomeRebate() + performanceClientBundleView.getOutcomeRebate());
                existedPerformanceClientBundleView.setOutcomeDownstream(existedPerformanceClientBundleView.getOutcomeDownstream() + performanceClientBundleView.getOutcomeDownstream());
            }
        }

        for (TrafficControl trafficControl : trafficControlList) {
            String key = trafficControl.getClientPort() + "|" + trafficControl.getVendorPort() + "|" + trafficControl.getBundle();
            trafficControlMap.put(key, trafficControl);
        }

        return generatePerformanceBundleReport(
            "client",
            query,
            performanceClientBundleViewMap,
            trafficControlMap,
            start,
            end,
            clientMap,
            clientMediaMap,
            clientPortMap,
            vendorMap,
            vendorMediaMap,
            vendorPortMap,
            interval,
            timezone);
    }

    public byte[] downloadPerformanceVendorBundleList(
            Authentication authentication,
            Query query,
            String interval,
            Timestamp start,
            Timestamp end,
            String timezone) {
        List<Client> qualifiedClientList = partnerService.getQualifiedClientListWithoutFilterAndSearch(authentication, query);
        // full media list
        List<ClientMedia> clientMediaList = clientMediaService.getClientMediaList(authentication, qualifiedClientList);
        // full port list
        List<ClientPort> clientPortList = clientPortService.getClientPortList(authentication, qualifiedClientList);
        List<Integer> clientPortIdList = clientPortList.stream().map(ClientPort::getId).distinct().collect(Collectors.toList());
        Map<Integer, Client> clientMap = qualifiedClientList.stream().collect(Collectors.toMap(Client::getId, Function.identity(), (first, second) -> first));
        Map<Integer, ClientMedia> clientMediaMap = clientMediaList.stream().collect(Collectors.toMap(ClientMedia::getId, Function.identity(), (first, second) -> first));
        Map<Integer, ClientPort> clientPortMap = clientPortList.stream().collect(Collectors.toMap(ClientPort::getId, Function.identity(), (first, second) -> first));

        List<Vendor> qualifiedVendorList = partnerService.getQualifiedVendorListWithoutFilterAndSearch(authentication, query);
        List<VendorMedia> vendorMediaList = partnerService.getQualifiedVendorMediaListWithoutFilterAndSearch(qualifiedVendorList, query);
        List<VendorPort> vendorPortList = partnerService.getQualifiedVendorPortList(qualifiedVendorList, query);
        List<Integer> vendorPortIdList = vendorPortList.stream().map(VendorPort::getId).distinct().collect(Collectors.toList());
        Map<Integer, Vendor> vendorMap = qualifiedVendorList.stream().collect(Collectors.toMap(Vendor::getId, Function.identity(), (first, second) -> first));
        Map<Integer, VendorMedia> vendorMediaMap = vendorMediaList.stream().collect(Collectors.toMap(VendorMedia::getId, Function.identity(), (first, second) -> first));
        Map<Integer, VendorPort> vendorPortMap = vendorPortList.stream().collect(Collectors.toMap(VendorPort::getId, Function.identity(), (first, second) -> first));

        List<PerformanceVendorBundle> performanceVendorBundleList = new ArrayList<PerformanceVendorBundle>();
        if (interval.equals("quarter")) {
            List<PerformanceVendorBundleQuarter> performanceVendorBundleQuarterList = performanceVendorBundleQuarterRepository.findDetailByClientPortInAndVendorPortInAndTimeBetween(clientPortIdList, vendorPortIdList, start, end);
            performanceVendorBundleList.addAll(performanceVendorBundleQuarterList);
        } else if (interval.equals("hour")) {
            List<PerformanceVendorBundleHour> performanceVendorBundleHourList = performanceVendorBundleHourRepository.findDetailByClientPortInAndVendorPortInAndTimeBetween(clientPortIdList, vendorPortIdList, start, end);
            performanceVendorBundleList.addAll(performanceVendorBundleHourList);
        } else {
            List<PerformanceVendorBundleDay> performanceVendorBundleDayList = performanceVendorBundleDayRepository.findDetailByClientPortInAndVendorPortInAndTimeBetween(clientPortIdList, vendorPortIdList, start, end);
            performanceVendorBundleList.addAll(performanceVendorBundleDayList);
        }

        List<PerformanceBundleView> performanceVendorBundleViewList = new ArrayList<PerformanceBundleView>();
        Map<String, PerformanceBundleView> performanceVendorBundleViewMap = new HashMap<String, PerformanceBundleView>();
        List<TrafficControl> trafficControlList = trafficControlService.getTrafficControlList(authentication, query);
        Map<String, TrafficControl> trafficControlMap = new HashMap<String, TrafficControl>();

        for (PerformanceVendorBundle performanceVendorBundle : performanceVendorBundleList) {
            PerformanceBundleView performanceVendorBundleView = convertVendorBundleToView(performanceVendorBundle, interval, timezone);

            String key = performanceVendorBundle.getTime().getTime() + "|" + performanceVendorBundle.getClientPort() + "|" + performanceVendorBundle.getVendorPort() + "|" + performanceVendorBundle.getBundle();
            if (!performanceVendorBundleViewMap.containsKey(key)) {
                performanceVendorBundleViewList.add(performanceVendorBundleView);
                performanceVendorBundleViewMap.put(key, performanceVendorBundleView);
            } else {
                PerformanceView existedPerformanceVendorBundleView = performanceVendorBundleViewMap.get(key);
                if (existedPerformanceVendorBundleView.getStart().after(performanceVendorBundle.getTime())) {
                    existedPerformanceVendorBundleView.setStart(performanceVendorBundle.getTime());
                }
                if (existedPerformanceVendorBundleView.getEnd().before(performanceVendorBundle.getTime())) {
                    existedPerformanceVendorBundleView.setEnd(performanceVendorBundle.getTime());
                }
                existedPerformanceVendorBundleView.setRequest(existedPerformanceVendorBundleView.getRequest() + performanceVendorBundleView.getRequest());
                existedPerformanceVendorBundleView.setResponse(existedPerformanceVendorBundleView.getResponse() + performanceVendorBundleView.getResponse());
                existedPerformanceVendorBundleView.setRequestv(existedPerformanceVendorBundleView.getRequestv() + performanceVendorBundleView.getRequestv());
                existedPerformanceVendorBundleView.setResponsev(existedPerformanceVendorBundleView.getResponsev() + performanceVendorBundleView.getResponsev());
                existedPerformanceVendorBundleView.setImpression(existedPerformanceVendorBundleView.getImpression() + performanceVendorBundleView.getImpression());
                existedPerformanceVendorBundleView.setClick(existedPerformanceVendorBundleView.getClick() + performanceVendorBundleView.getClick());
                existedPerformanceVendorBundleView.setIncome(existedPerformanceVendorBundleView.getIncome() + performanceVendorBundleView.getIncome());
                existedPerformanceVendorBundleView.setOutcomeUpstream(existedPerformanceVendorBundleView.getOutcomeUpstream() + performanceVendorBundleView.getOutcomeUpstream());
                existedPerformanceVendorBundleView.setOutcomeRebate(existedPerformanceVendorBundleView.getOutcomeRebate() + performanceVendorBundleView.getOutcomeRebate());
                existedPerformanceVendorBundleView.setOutcomeDownstream(existedPerformanceVendorBundleView.getOutcomeDownstream() + performanceVendorBundleView.getOutcomeDownstream());
            }
        }

        for (TrafficControl trafficControl : trafficControlList) {
            String key = trafficControl.getClientPort() + "|" + trafficControl.getVendorPort() + "|" + trafficControl.getBundle();
            trafficControlMap.put(key, trafficControl);
        }

        return generatePerformanceBundleReport(
            "vendor",
            query,
            performanceVendorBundleViewMap,
            trafficControlMap,
            start,
            end,
            clientMap,
            clientMediaMap,
            clientPortMap,
            vendorMap,
            vendorMediaMap,
            vendorPortMap,
            interval,
            timezone);
    }

    private byte[] generatePerformanceReport(
            String direction,
            Query query,
            Map<String, PerformanceView> performanceViewMap,
            Timestamp start,
            Timestamp end,
            Map<Integer, Client> clientMap,
            Map<Integer, ClientMedia> clientMediaMap,
            Map<Integer, ClientPort> clientPortMap,
            Map<Integer, Vendor> vendorMap,
            Map<Integer, VendorMedia> vendorMediaMap,
            Map<Integer, VendorPort> vendorPortMap,
            String interval,
            String timezone) {
        TimeZone tz = TimeZone.getTimeZone(timezone);
        Calendar calendar = Calendar.getInstance(tz);
        SimpleDateFormat dfDatTag = new SimpleDateFormat("yyyy-MM-dd");
        SimpleDateFormat dfDate = new SimpleDateFormat("yyyy-MM-dd");
        if (interval.equals("quarter")) {
            dfDatTag = new SimpleDateFormat("yyyy-MM-dd HH:mm");
            dfDate = new SimpleDateFormat("yyyy-MM-dd HH:mm");
        }
        if (interval.equals("hour")) {
            dfDatTag = new SimpleDateFormat("yyyy-MM-dd HH:mm");
            dfDate = new SimpleDateFormat("yyyy-MM-dd HH:mm");
        }
        if (interval.equals("day") || interval.equals("month") || interval.equals("year")) {
            dfDatTag = new SimpleDateFormat("yyyy-MM-dd");
            dfDate = new SimpleDateFormat("yyyy-MM-dd");
        }
        if (interval.equals("month")) {
            dfDate = new SimpleDateFormat("yyyy-MM");
        }
        if (interval.equals("year")) {
            dfDate = new SimpleDateFormat("yyyy");
        }
        dfDatTag.setTimeZone(tz);
        dfDate.setTimeZone(tz);

        Workbook workbook = new XSSFWorkbook();
        DataFormat format = workbook.createDataFormat();
        Font boldFont = workbook.createFont();
        boldFont.setBold(true);
        CellStyle cellStyleHeader = workbook.createCellStyle();
        cellStyleHeader.setFont(boldFont);
        CellStyle cellStyleAmount = workbook.createCellStyle();
        cellStyleAmount.setDataFormat(format.getFormat("##0"));
        CellStyle cellStylePercentage = workbook.createCellStyle();
        cellStylePercentage.setDataFormat(format.getFormat("0.00%"));
        CellStyle cellStyleCost = workbook.createCellStyle();
        cellStyleCost.setDataFormat(format.getFormat("0.00"));
        Sheet sheet = workbook.createSheet("流量数据");
        Row header = sheet.createRow(0);

        Row tag = sheet.createRow(0);
        Cell tagCell = tag.createCell(0);
        tagCell.setCellValue("广告主");
        tagCell.setCellStyle(cellStyleHeader);
        tagCell = tag.createCell(1);
        if (query.filter.get("client").size() > 0) {
            String clientNameList = "";
            for (String clientId : query.filter.get("client")) {
                clientNameList += clientMap.get(Integer.parseInt(clientId)).getName() + ",";
            }
            tagCell.setCellValue(clientNameList.substring(0, clientNameList.length() - 1));
        } else {
            tagCell.setCellValue("全部");
        }
        tag = sheet.createRow(1);
        tagCell = tag.createCell(0);
        tagCell.setCellValue("上游媒体");
        tagCell.setCellStyle(cellStyleHeader);
        tagCell = tag.createCell(1);
        if (query.filter.get("clientMedia").size() > 0) {
            String clientMediaNameList = "";
            for (String clientId : query.filter.get("clientMedia")) {
                clientMediaNameList += clientMediaMap.get(Integer.parseInt(clientId)).getName() + ",";
            }
            tagCell.setCellValue(clientMediaNameList.substring(0, clientMediaNameList.length() - 1));
        } else {
            tagCell.setCellValue("全部");
        }
        tag = sheet.createRow(2);
        tagCell = tag.createCell(0);
        tagCell.setCellValue("上游广告位");
        tagCell.setCellStyle(cellStyleHeader);
        tagCell = tag.createCell(1);
        if (query.filter.get("clientMedia").size() > 0) {
            String clientMediaNameList = "";
            for (String clientId : query.filter.get("clientMedia")) {
                clientMediaNameList += clientMediaMap.get(Integer.parseInt(clientId)).getName() + ",";
            }
            tagCell.setCellValue(clientMediaNameList.substring(0, clientMediaNameList.length() - 1));
        } else {
            tagCell.setCellValue("全部");
        }
        tag = sheet.createRow(3);
        tagCell = tag.createCell(0);
        tagCell.setCellValue("流量主");
        tagCell.setCellStyle(cellStyleHeader);
        tagCell = tag.createCell(1);
        if (query.filter.get("clientMedia").size() > 0) {
            String clientMediaNameList = "";
            for (String clientId : query.filter.get("clientMedia")) {
                clientMediaNameList += clientMediaMap.get(Integer.parseInt(clientId)).getName() + ",";
            }
            tagCell.setCellValue(clientMediaNameList.substring(0, clientMediaNameList.length() - 1));
        } else {
            tagCell.setCellValue("全部");
        }
        tag = sheet.createRow(4);
        tagCell = tag.createCell(0);
        tagCell.setCellValue("下游媒体");
        tagCell.setCellStyle(cellStyleHeader);
        tagCell = tag.createCell(1);
        if (query.filter.get("clientMedia").size() > 0) {
            String clientMediaNameList = "";
            for (String clientId : query.filter.get("clientMedia")) {
                clientMediaNameList += clientMediaMap.get(Integer.parseInt(clientId)).getName() + ",";
            }
            tagCell.setCellValue(clientMediaNameList.substring(0, clientMediaNameList.length() - 1));
        } else {
            tagCell.setCellValue("全部");
        }
        tag = sheet.createRow(5);
        tagCell = tag.createCell(0);
        tagCell.setCellValue("下游广告位");
        tagCell.setCellStyle(cellStyleHeader);
        tagCell = tag.createCell(1);
        if (query.filter.get("clientMedia").size() > 0) {
            String clientMediaNameList = "";
            for (String clientId : query.filter.get("clientMedia")) {
                clientMediaNameList += clientMediaMap.get(Integer.parseInt(clientId)).getName() + ",";
            }
            tagCell.setCellValue(clientMediaNameList.substring(0, clientMediaNameList.length() - 1));
        } else {
            tagCell.setCellValue("全部");
        }
        tag = sheet.createRow(6);
        tagCell = tag.createCell(0);
        tagCell.setCellValue("起止时间");
        tagCell.setCellStyle(cellStyleHeader);
        tagCell = tag.createCell(1);
        tagCell.setCellValue(dfDatTag.format(start.getTime()) + " - " + dfDatTag.format(end.getTime()));
        tag = sheet.createRow(7);

        header = sheet.createRow(8);

        Cell headerCell = header.createCell(0);
        headerCell.setCellValue("时间");
        headerCell.setCellStyle(cellStyleHeader);
        headerCell = header.createCell(1);
        headerCell.setCellValue("广告主");
        headerCell.setCellStyle(cellStyleHeader);
        headerCell = header.createCell(2);
        headerCell.setCellValue("上游广告位");
        headerCell.setCellStyle(cellStyleHeader);
        headerCell = header.createCell(3);
        headerCell.setCellValue("流量主");
        headerCell.setCellStyle(cellStyleHeader);
        headerCell = header.createCell(4);
        headerCell.setCellValue("下游广告位");
        headerCell.setCellStyle(cellStyleHeader);
        headerCell = header.createCell(5);
        headerCell.setCellValue("请求量");
        headerCell.setCellStyle(cellStyleHeader);
        headerCell = header.createCell(6);
        headerCell.setCellValue("响应量");
        headerCell.setCellStyle(cellStyleHeader);
        headerCell = header.createCell(7);
        headerCell.setCellValue("填充率");
        headerCell.setCellStyle(cellStyleHeader);
        headerCell = header.createCell(8);
        headerCell.setCellValue("展现量");
        headerCell.setCellStyle(cellStyleHeader);
        headerCell = header.createCell(9);
        headerCell.setCellValue("点击量");
        headerCell.setCellStyle(cellStyleHeader);
        headerCell = header.createCell(10);
        headerCell.setCellValue("展现率");
        headerCell.setCellStyle(cellStyleHeader);
        headerCell = header.createCell(11);
        headerCell.setCellValue("点击率");
        headerCell.setCellStyle(cellStyleHeader);
        headerCell = header.createCell(12);
        headerCell.setCellValue("收入");
        headerCell.setCellStyle(cellStyleHeader);
        headerCell = header.createCell(13);
        headerCell.setCellValue("成本");
        headerCell.setCellStyle(cellStyleHeader);
        headerCell = header.createCell(14);
        headerCell.setCellValue("请求价值");
        headerCell.setCellStyle(cellStyleHeader);
        headerCell = header.createCell(15);
        headerCell.setCellValue("上游CPM");
        headerCell.setCellStyle(cellStyleHeader);
        headerCell = header.createCell(16);
        headerCell.setCellValue("下游CPM");
        headerCell.setCellStyle(cellStyleHeader);

        List<String> keys = new ArrayList<String>();
        keys.addAll(performanceViewMap.keySet());
        keys.sort((a, b) -> {
            String[] aArray = a.split("\\|");
            String[] bArray = b.split("\\|");

            Long aTime = Long.parseLong(aArray[0]);
            Long bTime = Long.parseLong(bArray[0]);
            Integer aClientPortId = Integer.parseInt(aArray[1]);
            Integer bClientPortId = Integer.parseInt(bArray[1]);
            Integer aVendorPortId = Integer.parseInt(aArray[2]);
            Integer bVendorPortId = Integer.parseInt(bArray[2]);

            String aClientName = aClientPortId == -1 ? "无填充" : clientPortMap.get(aClientPortId).getClient().getName();
            String bClientName = bClientPortId == -1 ? "无填充" : clientPortMap.get(bClientPortId).getClient().getName();
            String aVendorName = aVendorPortId == -1 ? "无填充" : vendorPortMap.get(aVendorPortId).getVendor().getName();
            String bVendorName = bVendorPortId == -1 ? "无填充" : vendorPortMap.get(bVendorPortId).getVendor().getName();

            if (aTime.equals(bTime)) {
                if (direction.equals("client")) {
                    if (aClientName.equals(bClientName)) {
                        if (aClientPortId.equals(bClientPortId)) {
                            return aVendorPortId.compareTo(bVendorPortId);
                        } else {
                            return aClientPortId.compareTo(bClientPortId);
                        }
                    } else {
                        return aClientName.compareTo(bClientName);
                    }
                 } else {
                     if (aVendorName.equals(bVendorName)) {
                    if (aVendorPortId.equals(bVendorPortId)) {
                            return aClientPortId.compareTo(bClientPortId);
                        } else {
                            return aVendorPortId.compareTo(bVendorPortId);
                        }
                    } else {
                        return aVendorName.compareTo(bVendorName);
                    }
                }
            } else {
                return aTime.compareTo(bTime);
            }
        });

        Integer line = 8;
        for (String key : keys) {
            Integer clientPortId = Integer.parseInt(key.split("\\|")[1]);
            Integer vendorPortId = Integer.parseInt(key.split("\\|")[2]);

            if (query.filter.containsKey("client") && !query.filter.get("client").isEmpty()
                || query.filter.containsKey("clientMedia") && !query.filter.get("clientMedia").isEmpty()
                || query.filter.containsKey("clientPort") && !query.filter.get("clientPort").isEmpty()) {
                if (!clientPortMap.containsKey(clientPortId)) {
                    continue;
                }
            }
            if (query.filter.containsKey("vendor") && !query.filter.get("vendor").isEmpty()
                || query.filter.containsKey("vendorMedia") && !query.filter.get("vendorMedia").isEmpty()
                || query.filter.containsKey("vendorPort") && !query.filter.get("vendorPort").isEmpty()) {
                if (!vendorPortMap.containsKey(vendorPortId)) {
                    continue;
                }
            }

            PerformanceView performanceVendorView = performanceViewMap.get(key);
            calendar.setTimeInMillis(performanceVendorView.getStart().getTime());

            Row row = sheet.createRow(++line);
            Cell cell = row.createCell(0);
            cell.setCellValue(dfDate.format(calendar.getTime()));
            cell = row.createCell(1);
            cell.setCellValue(clientPortId == -1 ? "无填充" : clientPortMap.get(clientPortId).getClient().getName());
            cell = row.createCell(2);
            cell.setCellValue(clientPortId == -1 ? "无填充" : clientPortMap.get(clientPortId).getTagId().split("\\|")[0] + "|" + clientPortMap.get(clientPortId).getName());
            cell = row.createCell(3);
            cell.setCellValue(vendorPortMap.get(vendorPortId).getVendor().getName());
            cell = row.createCell(4);
            cell.setCellValue(vendorPortMap.get(vendorPortId).getTagId() + "|" + vendorPortMap.get(vendorPortId).getName());
            cell = row.createCell(5);
            cell.setCellValue(performanceVendorView.getRequest());
            cell.setCellStyle(cellStyleAmount);
            cell = row.createCell(6);
            cell.setCellValue(performanceVendorView.getResponsev());
            cell.setCellStyle(cellStyleAmount);
            cell = row.createCell(7);
            cell.setCellFormula("IFERROR(G" + (line + 1) + "/F" + (line + 1) + ", \"\")");
            cell.setCellStyle(cellStylePercentage);
            cell = row.createCell(8);
            cell.setCellValue(performanceVendorView.getImpression());
            cell.setCellStyle(cellStyleAmount);
            cell = row.createCell(9);
            cell.setCellValue(performanceVendorView.getClick());
            cell.setCellStyle(cellStyleAmount);
            cell = row.createCell(10);
            cell.setCellFormula("IFERROR(I" + (line + 1) + "/G" + (line + 1) + ", \"\")");
            cell.setCellStyle(cellStylePercentage);
            cell = row.createCell(11);
            cell.setCellFormula("IFERROR(J" + (line + 1) + "/I" + (line + 1) + ", \"\")");
            cell.setCellStyle(cellStylePercentage);
            cell = row.createCell(12);
            cell.setCellValue(Math.round(performanceVendorView.getIncome() / 1000.0) / 100.0);
            cell.setCellStyle(cellStyleCost);
            cell = row.createCell(13);
            cell.setCellValue(Math.round(performanceVendorView.getOutcomeDownstream() / 1000.0) / 100.0);
            cell.setCellStyle(cellStyleCost);
            cell = row.createCell(14);
            cell.setCellFormula("IFERROR(M" + (line + 1) + "/F" + (line + 1) + "*10000, \"\")");
            cell.setCellStyle(cellStyleCost);
            cell = row.createCell(15);
            cell.setCellFormula("IFERROR(M" + (line + 1) + "/I" + (line + 1) + "*1000, \"\")");
            cell.setCellStyle(cellStyleCost);
            cell = row.createCell(16);
            cell.setCellFormula("IFERROR(N" + (line + 1) + "/I" + (line + 1) + "*1000, \"\")");
            cell.setCellStyle(cellStyleCost);
        }

        cellService.adjustColumnWeight(sheet, 0, 17);

        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            workbook.write(outputStream);
            workbook.close();

            return outputStream.toByteArray();
        } catch (IOException e) {
            return null;
        }
    }

    private byte[] generatePerformanceBundleReport(
            String direction,
            Query query,
            Map<String, PerformanceBundleView> performanceBundleViewMap,
            Map<String, TrafficControl> trafficControlMap,
            Timestamp start,
            Timestamp end,
            Map<Integer, Client> clientMap,
            Map<Integer, ClientMedia> clientMediaMap,
            Map<Integer, ClientPort> clientPortMap,
            Map<Integer, Vendor> vendorMap,
            Map<Integer, VendorMedia> vendorMediaMap,
            Map<Integer, VendorPort> vendorPortMap,
            String interval,
            String timezone) {
        TimeZone tz = TimeZone.getTimeZone(timezone);
        SimpleDateFormat dfDateTag = new SimpleDateFormat("yyyy-MM-dd");
        SimpleDateFormat dfDate = new SimpleDateFormat("yyyy-MM-dd");
        if (interval.equals("quarter")) {
            dfDateTag = new SimpleDateFormat("yyyy-MM-dd HH:mm");
            dfDate = new SimpleDateFormat("yyyy-MM-dd HH:mm");
        }
        if (interval.equals("hour")) {
            dfDateTag = new SimpleDateFormat("yyyy-MM-dd HH:mm");
            dfDate = new SimpleDateFormat("yyyy-MM-dd HH:mm");
        }
        if (interval.equals("day") || interval.equals("month") || interval.equals("year")) {
            dfDateTag = new SimpleDateFormat("yyyy-MM-dd");
            dfDate = new SimpleDateFormat("yyyy-MM-dd");
        }
        if (interval.equals("month")) {
            dfDate = new SimpleDateFormat("yyyy-MM");
        }
        if (interval.equals("year")) {
            dfDate = new SimpleDateFormat("yyyy");
        }
        dfDateTag.setTimeZone(tz);
        dfDate.setTimeZone(tz);

        Workbook workbook = new XSSFWorkbook();
        DataFormat format = workbook.createDataFormat();
        Font boldFont = workbook.createFont();
        boldFont.setBold(true);
        Font fontBlocked = workbook.createFont();
        fontBlocked.setBold(true);
        fontBlocked.setColor(IndexedColors.RED.getIndex());
        CellStyle cellStyleHeader = workbook.createCellStyle();
        cellStyleHeader.setFont(boldFont);
        CellStyle cellStyleAmount = workbook.createCellStyle();
        cellStyleAmount.setDataFormat(format.getFormat("##0"));
        CellStyle cellStylePercentage = workbook.createCellStyle();
        cellStylePercentage.setDataFormat(format.getFormat("0.00%"));
        CellStyle cellStyleCost = workbook.createCellStyle();
        cellStyleCost.setDataFormat(format.getFormat("0.00"));
        CellStyle cellStyleBlocked = workbook.createCellStyle();
        cellStyleBlocked.setFont(fontBlocked);
        Sheet sheet = workbook.createSheet("包拆解分析");

        Row tag = sheet.createRow(0);
        Cell tagCell = tag.createCell(0);
        tagCell.setCellValue("广告主");
        tagCell.setCellStyle(cellStyleHeader);
        tagCell = tag.createCell(1);
        if (query.filter.get("client").size() > 0) {
            String clientNameList = "";
            for (String clientId : query.filter.get("client")) {
                clientNameList += clientMap.get(Integer.parseInt(clientId)).getName() + ",";
            }
            tagCell.setCellValue(clientNameList.substring(0, clientNameList.length() - 1));
        } else {
            tagCell.setCellValue("全部");
        }
        tag = sheet.createRow(1);
        tagCell = tag.createCell(0);
        tagCell.setCellValue("上游媒体");
        tagCell.setCellStyle(cellStyleHeader);
        tagCell = tag.createCell(1);
        if (query.filter.get("clientMedia").size() > 0) {
            String clientMediaNameList = "";
            for (String clientId : query.filter.get("clientMedia")) {
                clientMediaNameList += clientMediaMap.get(Integer.parseInt(clientId)).getName() + ",";
            }
            tagCell.setCellValue(clientMediaNameList.substring(0, clientMediaNameList.length() - 1));
        } else {
            tagCell.setCellValue("全部");
        }
        tag = sheet.createRow(2);
        tagCell = tag.createCell(0);
        tagCell.setCellValue("上游广告位");
        tagCell.setCellStyle(cellStyleHeader);
        tagCell = tag.createCell(1);
        if (query.filter.get("clientMedia").size() > 0) {
            String clientMediaNameList = "";
            for (String clientId : query.filter.get("clientMedia")) {
                clientMediaNameList += clientMediaMap.get(Integer.parseInt(clientId)).getName() + ",";
            }
            tagCell.setCellValue(clientMediaNameList.substring(0, clientMediaNameList.length() - 1));
        } else {
            tagCell.setCellValue("全部");
        }
        tag = sheet.createRow(3);
        tagCell = tag.createCell(0);
        tagCell.setCellValue("流量主");
        tagCell.setCellStyle(cellStyleHeader);
        tagCell = tag.createCell(1);
        if (query.filter.get("clientMedia").size() > 0) {
            String clientMediaNameList = "";
            for (String clientId : query.filter.get("clientMedia")) {
                clientMediaNameList += clientMediaMap.get(Integer.parseInt(clientId)).getName() + ",";
            }
            tagCell.setCellValue(clientMediaNameList.substring(0, clientMediaNameList.length() - 1));
        } else {
            tagCell.setCellValue("全部");
        }
        tag = sheet.createRow(4);
        tagCell = tag.createCell(0);
        tagCell.setCellValue("下游媒体");
        tagCell.setCellStyle(cellStyleHeader);
        tagCell = tag.createCell(1);
        if (query.filter.get("clientMedia").size() > 0) {
            String clientMediaNameList = "";
            for (String clientId : query.filter.get("clientMedia")) {
                clientMediaNameList += clientMediaMap.get(Integer.parseInt(clientId)).getName() + ",";
            }
            tagCell.setCellValue(clientMediaNameList.substring(0, clientMediaNameList.length() - 1));
        } else {
            tagCell.setCellValue("全部");
        }
        tag = sheet.createRow(5);
        tagCell = tag.createCell(0);
        tagCell.setCellValue("下游广告位");
        tagCell.setCellStyle(cellStyleHeader);
        tagCell = tag.createCell(1);
        if (query.filter.get("clientMedia").size() > 0) {
            String clientMediaNameList = "";
            for (String clientId : query.filter.get("clientMedia")) {
                clientMediaNameList += clientMediaMap.get(Integer.parseInt(clientId)).getName() + ",";
            }
            tagCell.setCellValue(clientMediaNameList.substring(0, clientMediaNameList.length() - 1));
        } else {
            tagCell.setCellValue("全部");
        }
        tag = sheet.createRow(6);
        tagCell = tag.createCell(0);
        tagCell.setCellValue("起止时间");
        tagCell.setCellStyle(cellStyleHeader);
        tagCell = tag.createCell(1);
        tagCell.setCellValue(dfDateTag.format(start.getTime()) + " - " + dfDateTag.format(end.getTime()));
        tag = sheet.createRow(7);

        Row header = sheet.createRow(8);

        Cell headerCell = header.createCell(0);
        headerCell.setCellValue("广告主");
        headerCell.setCellStyle(cellStyleHeader);
        headerCell = header.createCell(1);
        headerCell.setCellValue("上游广告位");
        headerCell.setCellStyle(cellStyleHeader);
        headerCell = header.createCell(2);
        headerCell.setCellValue("流量主");
        headerCell.setCellStyle(cellStyleHeader);
        headerCell = header.createCell(3);
        headerCell.setCellValue("下游广告位");
        headerCell.setCellStyle(cellStyleHeader);
        headerCell = header.createCell(4);
        headerCell.setCellValue("包名");
        headerCell.setCellStyle(cellStyleHeader);
        headerCell = header.createCell(5);
        headerCell.setCellValue("请求量");
        headerCell.setCellStyle(cellStyleHeader);
        headerCell = header.createCell(6);
        headerCell.setCellValue("响应量");
        headerCell.setCellStyle(cellStyleHeader);
        headerCell = header.createCell(7);
        headerCell.setCellValue("填充率");
        headerCell.setCellStyle(cellStyleHeader);
        headerCell = header.createCell(8);
        headerCell.setCellValue("展现量");
        headerCell.setCellStyle(cellStyleHeader);
        headerCell = header.createCell(9);
        headerCell.setCellValue("点击量");
        headerCell.setCellStyle(cellStyleHeader);
        headerCell = header.createCell(10);
        headerCell.setCellValue("展现率");
        headerCell.setCellStyle(cellStyleHeader);
        headerCell = header.createCell(11);
        headerCell.setCellValue("点击率");
        headerCell.setCellStyle(cellStyleHeader);
        headerCell = header.createCell(12);
        headerCell.setCellValue("请求价值");
        headerCell.setCellStyle(cellStyleHeader);
        headerCell = header.createCell(13);
        headerCell.setCellValue("QPS设定");
        headerCell.setCellStyle(cellStyleHeader);

        List<String> keys = new ArrayList<String>();
        keys.addAll(performanceBundleViewMap.keySet());
        keys.sort((a, b) -> {
            String[] aArray = a.split("\\|");
            String[] bArray = b.split("\\|");

            Long aTime = Long.parseLong(aArray[0]);
            Long bTime = Long.parseLong(bArray[0]);
            Integer aClientPortId = Integer.parseInt(aArray[1]);
            Integer bClientPortId = Integer.parseInt(bArray[1]);
            Integer aVendorPortId = Integer.parseInt(aArray[2]);
            Integer bVendorPortId = Integer.parseInt(bArray[2]);

            String aClientName = aClientPortId == -1 ? "无填充" : clientPortMap.get(aClientPortId).getClient().getName();
            String bClientName = bClientPortId == -1 ? "无填充" : clientPortMap.get(bClientPortId).getClient().getName();
            String aVendorName = aVendorPortId == -1 ? "无填充" : vendorPortMap.get(aVendorPortId).getVendor().getName();
            String bVendorName = bVendorPortId == -1 ? "无填充" : vendorPortMap.get(bVendorPortId).getVendor().getName();

            if (aTime.equals(bTime)) {
                if (direction.equals("client")) {
                    if (aClientName.equals(bClientName)) {
                        if (aClientPortId.equals(bClientPortId)) {
                            return aVendorPortId.compareTo(bVendorPortId);
                        } else {
                            return aClientPortId.compareTo(bClientPortId);
                        }
                    } else {
                        return aClientName.compareTo(bClientName);
                    }
                 } else {
                     if (aVendorName.equals(bVendorName)) {
                    if (aVendorPortId.equals(bVendorPortId)) {
                            return aClientPortId.compareTo(bClientPortId);
                        } else {
                            return aVendorPortId.compareTo(bVendorPortId);
                        }
                    } else {
                        return aVendorName.compareTo(bVendorName);
                    }
                }
            } else {
                return aTime.compareTo(bTime);
            }
        });

        Integer line = 8;
        for (String key : keys) {
            Integer clientPortId = Integer.parseInt(key.split("\\|")[1]);
            Integer vendorPortId = Integer.parseInt(key.split("\\|")[2]);
            String bundle = "UNKNOWN";
            if (key.split("\\|").length > 3) {
                bundle = key.split("\\|")[3];
            }

            if (query.filter.containsKey("client") && !query.filter.get("client").isEmpty()
                || query.filter.containsKey("clientMedia") && !query.filter.get("clientMedia").isEmpty()
                || query.filter.containsKey("clientPort") && !query.filter.get("clientPort").isEmpty()) {
                if (!clientPortMap.containsKey(clientPortId)) {
                    continue;
                }
            }
            if (query.filter.containsKey("vendor") && !query.filter.get("vendor").isEmpty()
                || query.filter.containsKey("vendorMedia") && !query.filter.get("vendorMedia").isEmpty()
                || query.filter.containsKey("vendorPort") && !query.filter.get("vendorPort").isEmpty()) {
                if (!vendorPortMap.containsKey(vendorPortId)) {
                    continue;
                }
            }

            PerformanceBundleView performanceBundleView = performanceBundleViewMap.get(key);

            Row row = sheet.createRow(++line);
            Cell cell = row.createCell(0);
            cell.setCellValue(clientPortId == -1 ? "无填充" : clientPortMap.get(clientPortId).getClient().getName());
            cell = row.createCell(1);
            cell.setCellValue(clientPortId == -1 ? "无填充" : clientPortMap.get(clientPortId).getTagId().split("\\|")[0] + "|" + clientPortMap.get(clientPortId).getName());
            cell = row.createCell(2);
            cell.setCellValue(vendorPortMap.get(vendorPortId).getVendor().getName());
            cell = row.createCell(3);
            cell.setCellValue(vendorPortMap.get(vendorPortId).getTagId() + "|" + vendorPortMap.get(vendorPortId).getName());
            cell = row.createCell(4);
            cell.setCellValue(bundle);
            cell = row.createCell(5);
            cell.setCellValue(performanceBundleView.getRequest());
            cell.setCellStyle(cellStyleAmount);
            cell = row.createCell(6);
            cell.setCellValue(performanceBundleView.getResponsev());
            cell = row.createCell(7);
            cell.setCellStyle(cellStyleAmount);
            cell.setCellFormula("IFERROR(I" + (line + 1) + "/H" + (line + 1) + ", \"\")");
            cell.setCellStyle(cellStylePercentage);
            cell = row.createCell(8);
            cell.setCellValue(performanceBundleView.getImpression());
            cell.setCellStyle(cellStyleAmount);
            cell = row.createCell(9);
            cell.setCellValue(performanceBundleView.getClick());
            cell.setCellStyle(cellStyleAmount);
            cell = row.createCell(10);
            cell.setCellFormula("IFERROR(K" + (line + 1) + "/I" + (line + 1) + ", \"\")");
            cell.setCellStyle(cellStylePercentage);
            cell = row.createCell(11);
            cell.setCellFormula("IFERROR(L" + (line + 1) + "/K" + (line + 1) + ", \"\")");
            cell.setCellStyle(cellStylePercentage);
            cell = row.createCell(12);
            cell.setCellValue(performanceBundleView.getRequest() == 0.0 ? 0.0 : 1.0 * performanceBundleView.getIncome() / performanceBundleView.getRequest() / 10);
            cell.setCellStyle(cellStyleCost);
            cell = row.createCell(13);
            String keyTrafficControl = clientPortId + "|" + vendorPortId + "|" + bundle;
            if (trafficControlMap.containsKey(keyTrafficControl)) {
                TrafficControl trafficControl = trafficControlMap.get(keyTrafficControl);
                if (trafficControl.getIndicator() != TrafficControl.TC_INDICATOR_REQUEST || trafficControl.getPeriod() != TrafficControl.TC_PERIOD_SECOND) {
                    cell.setCellValue("");
                } else {
                    if (trafficControl.getLimitation() == 0) {
                        cell.setCellValue("禁止");
                        cell.setCellStyle(cellStyleBlocked);
                    }
                    if (trafficControl.getLimitation() > 0) {
                        cell.setCellValue(trafficControl.getLimitation());
                        cell.setCellStyle(cellStyleAmount);
                    }
                }
            }
        }

        cellService.adjustColumnWeight(sheet, 0, 14);

        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            workbook.write(outputStream);
            workbook.close();

            return outputStream.toByteArray();
        } catch (IOException e) {
            return null;
        }
    }

    public PerformanceView convertClientToView(PerformanceClient performanceClient, String interval, String timezone) {
        TimeZone tz = TimeZone.getTimeZone(timezone);
        SimpleDateFormat dfDate = new SimpleDateFormat("yyyy-MM-dd");
        if (interval.equals("quarter")) {
            dfDate = new SimpleDateFormat("yyyy-MM-dd HH:mm");
        }
        if (interval.equals("hour")) {
            dfDate = new SimpleDateFormat("yyyy-MM-dd HH:mm");
        }
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

        PerformanceView performanceView = new PerformanceView();
        performanceView.setTime(dfDate.format(performanceClient.getTime()));
        performanceView.setStart(performanceClient.getTime());
        performanceView.setEnd(performanceClient.getTime());
        performanceView.setClientPort(performanceClient.getClientPort());
        performanceView.setVendorPort(performanceClient.getVendorPort());
        performanceView.setRequest(
            performanceClient.getEventA() + performanceClient.getEventB() + performanceClient.getEventC() + performanceClient.getEventD() + performanceClient.getEventE() +
            performanceClient.getEventF() + performanceClient.getEventG() + performanceClient.getEventH() + performanceClient.getEventK() + performanceClient.getEventL() +
            performanceClient.getEventM());
        performanceView.setRequestv(
            performanceClient.getEventC() + performanceClient.getEventD() + performanceClient.getEventE() + performanceClient.getEventF() + performanceClient.getEventK());
        performanceView.setResponse(
            performanceClient.getEventD() + performanceClient.getEventE() + performanceClient.getEventK());
        performanceView.setResponsev(
            performanceClient.getEventD() + performanceClient.getEventE());
        performanceView.setImpression(performanceClient.getImpression());
        performanceView.setClick(performanceClient.getClick());
        performanceView.setIncome(performanceClient.getIncome());
        performanceView.setOutcomeUpstream(performanceClient.getOutcomeUpstream());
        performanceView.setOutcomeRebate(performanceClient.getOutcomeRebate());
        performanceView.setOutcomeDownstream(performanceClient.getOutcomeDownstream());

        return performanceView;
    }

    public PerformanceView convertVendorToView(PerformanceVendor performanceVendor, String interval, String timezone) {
        TimeZone tz = TimeZone.getTimeZone(timezone);
        SimpleDateFormat dfDate = new SimpleDateFormat("yyyy-MM-dd");
        if (interval.equals("quarter")) {
            dfDate = new SimpleDateFormat("yyyy-MM-dd HH:mm");
        }
        if (interval.equals("hour")) {
            dfDate = new SimpleDateFormat("yyyy-MM-dd HH:mm");
        }
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

        PerformanceView performanceView = new PerformanceView();
        performanceView.setTime(dfDate.format(performanceVendor.getTime().getTime()));
        performanceView.setStart(performanceVendor.getTime());
        performanceView.setEnd(performanceVendor.getTime());
        performanceView.setClientPort(performanceVendor.getClientPort());
        performanceView.setVendorPort(performanceVendor.getVendorPort());
        performanceView.setRequest(
            performanceVendor.getEventA() + performanceVendor.getEventB() + performanceVendor.getEventC() + performanceVendor.getEventD() + performanceVendor.getEventE() +
            performanceVendor.getEventF() + performanceVendor.getEventG() + performanceVendor.getEventH() + performanceVendor.getEventI() + performanceVendor.getEventJ());
        performanceView.setRequestv(
            performanceVendor.getEventH() + performanceVendor.getEventI() + performanceVendor.getEventJ());
        performanceView.setResponse(
            performanceVendor.getEventI() + performanceVendor.getEventJ());
        performanceView.setResponsev(
            performanceVendor.getEventI() + performanceVendor.getEventJ());
        performanceView.setImpression(performanceVendor.getImpression());
        performanceView.setClick(performanceVendor.getClick());
        performanceView.setIncome(performanceVendor.getIncome());
        performanceView.setOutcomeUpstream(performanceVendor.getOutcomeUpstream());
        performanceView.setOutcomeRebate(performanceVendor.getOutcomeRebate());
        performanceView.setOutcomeDownstream(performanceVendor.getOutcomeDownstream());

        return performanceView;
    }

    public PerformanceBundleView convertClientBundleToView(PerformanceClientBundle performanceClientBundle, String interval, String timezone) {
        PerformanceView performanceView = convertClientToView(performanceClientBundle, interval, timezone);

        PerformanceBundleView performanceBundleView = new PerformanceBundleView();
        BeanUtils.copyProperties(performanceView, performanceBundleView);
        performanceBundleView.setBundle(performanceClientBundle.getBundle());

        return performanceBundleView;
    }

    public PerformanceBundleView convertVendorBundleToView(PerformanceVendorBundle performanceVendorBundle, String interval, String timezone) {
        PerformanceView performanceView = convertVendorToView(performanceVendorBundle, interval, timezone);

        PerformanceBundleView performanceBundleView = new PerformanceBundleView();
        BeanUtils.copyProperties(performanceView, performanceBundleView);
        performanceBundleView.setBundle(performanceVendorBundle.getBundle());

        return performanceBundleView;
    }

}
