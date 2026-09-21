package cc.tonyhook.carambola.backend.service.perf;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

@Service
public class EventIdValidationService {

    private static final List<String> ID_KEYS = List.of(
        "imei",
        "imei_md5",
        "android_id",
        "android_id_md5",
        "oaid",
        "oaid_md5",
        "idfa",
        "idfa_md5",
        "idfv",
        "idfv_md5",
        "caid",
        "caid1",
        "caid1_md5",
        "caid2",
        "caid2_md5",
        "aaid",
        "mac",
        "mac_md5"
    );

    private static final Set<String> PLACEHOLDER_VALUES = Set.of(
        "0",
        "unknown",
        "null",
        "none",
        "nil",
        "na",
        "n/a",
        "undefined",
        "__none__"
    );

    private static final Pattern MD5_PATTERN = Pattern.compile("^[a-fA-F0-9]{32}$");
    private static final Pattern ALL_ZERO_PATTERN = Pattern.compile("^[0\\-:]+$");

    private static final int DEVICE_ID_MAX_LENGTH = 128;

    public boolean hasValidId(Map<String, String> queries) {
        if (queries == null) {
            return false;
        }

        for (String key : ID_KEYS) {
            if (isValidId(key, queries.get(key))) {
                return true;
            }
        }
        return false;
    }

    public String resolveDeviceId(Map<String, String> queries) {
        if (queries == null) {
            return null;
        }

        for (String key : ID_KEYS) {
            String value = queries.get(key);
            if (isValidId(key, value)) {
                String deviceId = key + ":" + value.trim();
                return deviceId.length() > DEVICE_ID_MAX_LENGTH
                    ? deviceId.substring(0, DEVICE_ID_MAX_LENGTH)
                    : deviceId;
            }
        }

        return null;
    }

    private boolean isValidId(String key, String value) {
        if (StringUtils.isBlank(value)) {
            return false;
        }

        String trimmed = value.trim();
        String normalized = trimmed.toLowerCase();
        if (PLACEHOLDER_VALUES.contains(normalized) || ALL_ZERO_PATTERN.matcher(trimmed).matches()) {
            return false;
        }
        if (normalized.equals("__" + key.toLowerCase() + "__")) {
            return false;
        }
        if (key.endsWith("_md5")) {
            return MD5_PATTERN.matcher(trimmed).matches();
        }

        return true;
    }

}
