package cc.tonyhook.carambola.backend.entity.perf;

import java.util.Set;

public final class EventCodes {

    public static final String IMPRESSION = "0001";
    public static final String CLICK = "0002";

    public static final String PAGE_SUBMIT = "1001";
    public static final String PAGE_CLEAR = "1002";
    public static final String PAGE_CANCEL = "1003";
    public static final String PAGE_DIAL = "1004";
    public static final String PAGE_WECHAT_COPY = "1005";
    public static final String PAGE_DOWNLOAD = "1006";
    public static final String PAGE_REGISTER = "1007";
    public static final String PAGE_ADD_TO_CART = "1008";
    public static final String PAGE_PURCHASE = "1009";
    public static final String PAGE_REFUND = "1010";
    // 页面上的贷款漏斗,与应用内的 2009/2010/2011 一一对应
    public static final String PAGE_SUBMIT_CREDIT = "1011";
    public static final String PAGE_PRE_CREDIT = "1012";
    public static final String PAGE_CREDIT = "1013";
    public static final String PAGE_START_PAYMENT = "1014";
    public static final String PAGE_PAYMENT_SUCCESS = "1015";
    public static final String PAGE_PAYMENT_FAILED = "1016";
    public static final String PAGE_VISIT_KEY_PAGE = "1017";
    public static final String PAGE_INTERACTION = "1018";

    public static final String APP_DOWNLOAD_COMPLETED = "2001";
    public static final String APP_ACTIVATE = "2002";
    public static final String APP_REGISTER = "2003";
    // 留存窗口自激活次日起算,次日/三日/七日/十四日留存分别为 2004/2018/2005/2025
    public static final String APP_RETENTION_1 = "2004";
    public static final String APP_RETENTION_7 = "2005";
    // 应用内电商漏斗:商品浏览(2019) → 加入购物车(2006) → 下单(2016) → 付费(2007)。
    // APP_CHECK_OUT 是漏斗末端的“付费完成”,不是“发起结算”;与它平级的付费类事件还有
    // 应用付费 APP_PAY(2015,非电商漏斗,如充值)、页面购买 PAGE_PURCHASE(1009)
    public static final String APP_ADD_TO_CART = "2006";
    public static final String APP_CHECK_OUT = "2007";
    public static final String APP_REFUND = "2008";
    // 应用内贷款漏斗:完件(2009)、预授信(2010)、授信(2011)。
    // APP_CREDIT 是漏斗末端的“贷款申请通过”,前两段多数媒体没有对应的转化类型
    public static final String APP_SUBMIT_CREDIT = "2009";
    public static final String APP_PRE_CREDIT = "2010";
    public static final String APP_CREDIT = "2011";
    public static final String APP_KEY_ACTION = "2012";
    public static final String APP_FIRST_WAKE_UP = "2013";
    public static final String APP_ACTIVE = "2014";
    public static final String APP_PAY = "2015";
    public static final String APP_PLACE_ORDER = "2016";
    public static final String APP_LATER_WAKE_UP = "2017";
    public static final String APP_RETENTION_3 = "2018";
    public static final String APP_PRODUCT_VIEW = "2019";
    public static final String APP_RECALL = "2020";
    // 时间窗口内的付费。窗口自激活当天起算,首日即激活当天(与留存的“次日”起算点不同),
    // 一笔付费只产生一个事件,落在它所属的那个窗口里,窗口之间不重叠
    public static final String APP_PAY_1 = "2021";
    public static final String APP_PAY_3 = "2022";
    public static final String APP_PAY_7 = "2023";
    public static final String APP_PAY_14 = "2024";
    public static final String APP_RETENTION_14 = "2025";

    public static final String WECHAT_ACTIVATE = "3001";
    public static final String WECHAT_REGISTER = "3002";
    public static final String WECHAT_PAY = "3003";
    public static final String WECHAT_RETENTION_1 = "3004";
    public static final String WECHAT_RETENTION_7 = "3005";
    public static final String WECHAT_RETENTION_3 = "3006";
    public static final String WECHAT_JOIN_GROUP = "3007";
    public static final String WECHAT_VIEW_DETAILS = "3008";
    public static final String WECHAT_FOLLOW = "3009";
    public static final String WECHAT_ACTIVE = "3010";

    public static final int CUSTOM_MIN = 9901;
    public static final int CUSTOM_MAX = 9999;

    private static final Set<String> ALL = Set.of(
        IMPRESSION,
        CLICK,
        PAGE_SUBMIT,
        PAGE_CLEAR,
        PAGE_CANCEL,
        PAGE_DIAL,
        PAGE_WECHAT_COPY,
        PAGE_DOWNLOAD,
        PAGE_REGISTER,
        PAGE_ADD_TO_CART,
        PAGE_PURCHASE,
        PAGE_REFUND,
        PAGE_SUBMIT_CREDIT,
        PAGE_PRE_CREDIT,
        PAGE_CREDIT,
        PAGE_START_PAYMENT,
        PAGE_PAYMENT_SUCCESS,
        PAGE_PAYMENT_FAILED,
        PAGE_VISIT_KEY_PAGE,
        PAGE_INTERACTION,
        APP_DOWNLOAD_COMPLETED,
        APP_ACTIVATE,
        APP_REGISTER,
        APP_RETENTION_1,
        APP_RETENTION_7,
        APP_ADD_TO_CART,
        APP_CHECK_OUT,
        APP_REFUND,
        APP_SUBMIT_CREDIT,
        APP_PRE_CREDIT,
        APP_CREDIT,
        APP_KEY_ACTION,
        APP_FIRST_WAKE_UP,
        APP_ACTIVE,
        APP_PAY,
        APP_PLACE_ORDER,
        APP_LATER_WAKE_UP,
        APP_RETENTION_3,
        APP_PRODUCT_VIEW,
        APP_RECALL,
        APP_PAY_1,
        APP_PAY_3,
        APP_PAY_7,
        APP_PAY_14,
        APP_RETENTION_14,
        WECHAT_ACTIVATE,
        WECHAT_REGISTER,
        WECHAT_PAY,
        WECHAT_RETENTION_1,
        WECHAT_RETENTION_7,
        WECHAT_RETENTION_3,
        WECHAT_JOIN_GROUP,
        WECHAT_VIEW_DETAILS,
        WECHAT_FOLLOW,
        WECHAT_ACTIVE
    );

    // 报表里需要按去重设备数(COUNT DISTINCT device_id)统计的事件。
    // 去重开销随行数线性上涨,而点击占了绝大多数行、它的去重结果前端并不展示,
    // 所以聚合时只对这里列出的事件计算,其余事件的 userCount 恒为 0。
    private static final Set<String> USER_COUNTED = Set.of(
        APP_PAY_1,
        APP_PAY_3,
        APP_PAY_7,
        APP_PAY_14,
        APP_PAY
    );

    private EventCodes() {
    }

    public static Set<String> all() {
        return ALL;
    }

    public static Set<String> userCounted() {
        return USER_COUNTED;
    }

    public static boolean isCustom(String event) {
        if (event == null || !event.matches("\\d{4}")) {
            return false;
        }

        int code = Integer.parseInt(event);
        return code >= CUSTOM_MIN && code <= CUSTOM_MAX;
    }

}
