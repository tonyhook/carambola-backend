package cc.tonyhook.carambola.backend.entity.perf;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

/**
 * 常量的声明顺序本身不影响任何行为(all() 是 Set.of,下游要么 TreeSet 重排要么只做 contains),
 * 可以按漏斗随意分组。这里守住的是重排时真正会出事的两点:漏进 ALL、和码值撞车。
 */
class EventCodesTest {

    @Test
    void allContainsEveryDeclaredCode() {
        List<String> declared = declaredCodes();

        assertThat(declared).isNotEmpty();
        // 常量还在、ALL 里漏了的话,那个事件码会被 ownProtocolEvent 静默拒收
        assertThat(EventCodes.all()).containsExactlyInAnyOrderElementsOf(declared);
    }

    @Test
    void codesAreUniqueFourDigitNumbers() {
        List<String> declared = declaredCodes();

        assertThat(declared).doesNotHaveDuplicates();
        assertThat(declared).allSatisfy(code -> assertThat(code).matches("\\d{4}"));
        // 自有协议的自定义区间归客户用,预置事件码不能落进去
        assertThat(declared).noneSatisfy(code -> assertThat(EventCodes.isCustom(code)).isTrue());
    }

    @Test
    void userCountedCodesAreRealEvents() {
        assertThat(EventCodes.userCounted()).isSubsetOf(EventCodes.all());
    }

    private static List<String> declaredCodes() {
        List<String> codes = new ArrayList<String>();
        for (Field field : EventCodes.class.getDeclaredFields()) {
            int modifiers = field.getModifiers();
            if (Modifier.isPublic(modifiers) && Modifier.isStatic(modifiers) && (field.getType() == String.class)) {
                try {
                    codes.add((String) field.get(null));
                } catch (IllegalAccessException e) {
                    throw new IllegalStateException(e);
                }
            }
        }
        return codes;
    }

}
