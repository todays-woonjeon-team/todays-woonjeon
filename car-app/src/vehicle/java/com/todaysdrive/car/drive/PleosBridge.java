package com.todaysdrive.car.drive;

import android.util.Log;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Vehicle SDK 리스너 연결부(임시).
 *
 * <p>Vehicle SDK는 Kotlin으로 만들어져 있고, 공개 문서에서 리스너 콜백의 정확한 시그니처를 아직 확인하지
 * 못했다. 그래서 등록 메서드를 이름으로 찾고, 리스너·콜백 인터페이스는 {@link Proxy}로 구현해 값을 받는다.
 * 연결할 때 실제 메서드 시그니처를 로그로 남기므로, 에뮬레이터에서 한 번 실행해 보고 그 시그니처로
 * 타입이 있는 코드를 쓰면 된다(docs/vehicle-signals.md).
 *
 * <p>규칙
 * <ul>
 *   <li>리스너 콜백 인자 중 Throwable이 있으면 오류, 아니면 첫 번째 인자를 값으로 본다.</li>
 *   <li>Kotlin 함수 타입(Function1, Function2)은 Unit.INSTANCE를 돌려준다.</li>
 *   <li>인터페이스가 아닌 인자(좌석·영역 같은 값)가 필요한 메서드는 연결하지 않는다.</li>
 * </ul>
 */
final class PleosBridge {

    private static final String TAG = "PleosBridge";

    interface ValueSink {
        void onValue(Object value);

        void onError(Throwable error);
    }

    interface CapabilitySink {
        void onResult(boolean readable);
    }

    /** 등록한 리스너. 해제할 때 같은 객체를 넘겨야 한다. */
    static final class Registration {
        final Object target;
        final String registerName;
        final Object[] args;

        Registration(Object target, String registerName, Object[] args) {
            this.target = target;
            this.registerName = registerName;
            this.args = args;
        }
    }

    private PleosBridge() {
    }

    /**
     * target.registerXxx(listener)를 부른다. 정확한 이름이 없으면 listenerSimpleName 타입 인자를 받는
     * register 메서드를 찾는다.
     */
    static Registration register(Object target, String methodName, String listenerSimpleName, ValueSink sink) {
        if (target == null) {
            return null;
        }
        Method method = find(target, methodName, listenerSimpleName);
        if (method == null) {
            Log.w(TAG, methodName + "를 찾지 못했습니다. 사용 가능한 메서드: " + listMethods(target, "register"));
            return null;
        }
        Object[] args = proxiesFor(method, sink);
        if (args == null) {
            Log.w(TAG, "인자를 만들 수 없어 건너뜀: " + method.toGenericString());
            return null;
        }
        try {
            method.invoke(target, args);
            Log.i(TAG, "연결됨: " + method.toGenericString());
            return new Registration(target, method.getName(), args);
        } catch (ReflectiveOperationException | RuntimeException e) {
            Log.w(TAG, "등록 실패: " + method.getName(), e);
            return null;
        }
    }

    /** register를 unregister로 바꾼 메서드에 같은 리스너를 넘긴다. */
    static void unregister(Registration reg) {
        if (reg == null) {
            return;
        }
        String name = "un" + reg.registerName;
        for (Method m : reg.target.getClass().getMethods()) {
            if (m.getName().equals(name) && m.getParameterTypes().length == reg.args.length) {
                try {
                    m.invoke(reg.target, reg.args);
                } catch (ReflectiveOperationException | RuntimeException e) {
                    Log.w(TAG, "해제 실패: " + name, e);
                }
                return;
            }
        }
    }

    /**
     * checkXxxCapabilities(onSuccess, onFailure)를 부른다. 성공 콜백의 설정값 accessType이 READ 또는
     * READ_WRITE면 읽을 수 있는 신호로 본다.
     */
    static void checkCapability(Object target, String methodName, CapabilitySink result) {
        if (target == null) {
            result.onResult(false);
            return;
        }
        Method method = find(target, methodName, null);
        if (method == null) {
            Log.w(TAG, methodName + "를 찾지 못했습니다. 등록 성공 여부로 판단합니다");
            result.onResult(true);
            return;
        }
        Object[] args = proxiesFor(method, new ValueSink() {
            @Override
            public void onValue(Object config) {
                result.onResult(isReadable(config));
            }

            @Override
            public void onError(Throwable error) {
                Log.w(TAG, methodName + " 실패: " + error);
                result.onResult(false);
            }
        });
        if (args == null) {
            result.onResult(true);
            return;
        }
        try {
            method.invoke(target, args);
        } catch (ReflectiveOperationException | RuntimeException e) {
            Log.w(TAG, methodName + " 호출 실패", e);
            result.onResult(false);
        }
    }

    private static boolean isReadable(Object config) {
        if (config == null) {
            return false;
        }
        try {
            Method getter = config.getClass().getMethod("getAccessType");
            String access = String.valueOf(getter.invoke(config));
            return access.startsWith("READ");
        } catch (ReflectiveOperationException e) {
            return true; // accessType이 없는 설정이면 성공 콜백 자체를 지원으로 본다
        }
    }

    private static Method find(Object target, String exactName, String listenerSimpleName) {
        Method fallback = null;
        for (Method m : target.getClass().getMethods()) {
            if (m.getName().equals(exactName) && allInterfaces(m)) {
                return m;
            }
            if (fallback == null && listenerSimpleName != null && m.getName().startsWith("register")
                    && allInterfaces(m) && hasParam(m, listenerSimpleName)) {
                fallback = m;
            }
        }
        return fallback;
    }

    private static boolean allInterfaces(Method m) {
        for (Class<?> c : m.getParameterTypes()) {
            if (!c.isInterface()) {
                return false;
            }
        }
        return true;
    }

    private static boolean hasParam(Method m, String simpleName) {
        for (Class<?> c : m.getParameterTypes()) {
            if (c.getSimpleName().equals(simpleName)) {
                return true;
            }
        }
        return false;
    }

    private static Object[] proxiesFor(Method method, ValueSink sink) {
        Class<?>[] types = method.getParameterTypes();
        Object[] args = new Object[types.length];
        for (int i = 0; i < types.length; i++) {
            if (!types[i].isInterface()) {
                return null;
            }
            args[i] = Proxy.newProxyInstance(types[i].getClassLoader(), new Class<?>[]{types[i]},
                    new Forwarder(types[i].getSimpleName(), sink));
        }
        return args;
    }

    private static String listMethods(Object target, String prefix) {
        List<String> names = new ArrayList<>();
        for (Method m : target.getClass().getMethods()) {
            if (m.getName().startsWith(prefix)) {
                names.add(m.toGenericString());
            }
        }
        return names.toString();
    }

    /** 리스너 호출을 ValueSink로 넘긴다. */
    private static final class Forwarder implements InvocationHandler {
        private final String typeName;
        private final ValueSink sink;
        private boolean logged;

        Forwarder(String typeName, ValueSink sink) {
            this.typeName = typeName;
            this.sink = sink;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) {
            switch (method.getName()) {
                case "hashCode":
                    return System.identityHashCode(proxy);
                case "equals":
                    return args != null && args.length == 1 && proxy == args[0];
                case "toString":
                    return "PleosBridge(" + typeName + ")";
                default:
                    break;
            }
            if (!logged) {
                logged = true;
                Log.i(TAG, "콜백 시그니처: " + method.toGenericString());
            }
            Throwable error = null;
            Object value = null;
            if (args != null) {
                for (Object a : args) {
                    if (a instanceof Throwable) {
                        error = (Throwable) a;
                    }
                }
                if (args.length > 0) {
                    value = args[0];
                }
            }
            if (error != null) {
                sink.onError(error);
            } else {
                sink.onValue(value);
            }
            return defaultReturn(method.getReturnType());
        }
    }

    private static Object defaultReturn(Class<?> type) {
        if (type == Void.TYPE) {
            return null;
        }
        if (type == Boolean.TYPE) {
            return false;
        }
        if (type.isPrimitive()) {
            return 0;
        }
        return kotlinUnit(); // Kotlin 함수 타입의 invoke는 Unit을 돌려줘야 한다
    }

    private static Object kotlinUnit() {
        try {
            return Class.forName("kotlin.Unit").getField("INSTANCE").get(null);
        } catch (ReflectiveOperationException e) {
            return null;
        }
    }

    // ── 값 변환 ──

    static Double toDouble(Object value) {
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        if (value != null) {
            try {
                return Double.parseDouble(String.valueOf(value));
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }

    /** 숫자는 0보다 크면 켜짐, 이름은 OFF/RELEASED/UNBUCKLED/NONE/FALSE가 아니면 켜짐 */
    static boolean toBoolean(Object value) {
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        if (value instanceof Number) {
            return ((Number) value).doubleValue() > 0;
        }
        String name = String.valueOf(value).toUpperCase(Locale.ROOT);
        return !(name.contains("OFF") || name.contains("RELEASE") || name.contains("UNBUCKLE")
                || name.contains("NONE") || name.contains("FALSE") || name.equals("NULL"));
    }
}
