package cc.tonyhook.carambola.backend.service.perf;

/**
 * 一次对外投递(入口事件上报给监测方、转化回传给媒体)的结果。
 *
 * UNSUPPORTED 表示对方协议里没有这个事件,请求根本没有发出;它与 FAILED 分开,
 * 否则“不支持”会被记成“转发失败”,拉低回传成功率。
 */
public enum DeliveryResult {

    SUCCEEDED,
    FAILED,
    UNSUPPORTED;

    public static DeliveryResult of(boolean succeeded) {
        return succeeded ? SUCCEEDED : FAILED;
    }

}
