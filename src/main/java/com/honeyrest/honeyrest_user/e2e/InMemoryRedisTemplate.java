package com.honeyrest.honeyrest_user.e2e;

import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.DefaultTypedTuple;
import org.springframework.data.redis.core.ListOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.ZSetOperations;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.regex.Pattern;

/**
 * e2e 프로필 전용: Redis 서버 없이 동작하는 인메모리 {@link RedisTemplate}.
 * <p>
 * 앱 코드는 캐시·인기 지역 집계·좋아요 집합 등에 RedisTemplate 을 직접 쓰고, 대부분 Redis 장애 폴백이 없다.
 * e2e 는 외부 인프라 없이 사용자 여정을 검증하는 것이 목적이므로, 앱이 실제로 쓰는 연산만
 * 프로세스 메모리의 {@link Store} 로 흉내 낸다 (TTL 포함, 두 템플릿이 같은 키 공간을 공유).
 * 여기 없는 연산을 호출하면 {@link UnsupportedOperationException} 으로 바로 드러나게 한다.
 * 운영/로컬/test 프로필에서는 절대 등록되지 않는다 ({@link E2eRedisConfig} 참고).
 *
 * @param <V> 값 타입. String 템플릿은 읽을 때 문자열로 변환한다 (실제 Redis 의 StringRedisSerializer 와 동일한 관찰 결과).
 */
public class InMemoryRedisTemplate<V> extends RedisTemplate<String, V> {

    /** 두 템플릿(String/Object)이 공유하는 키 공간. 실제 Redis 의 단일 DB 에 해당한다. */
    public static final class Store {
        private final Map<String, Object> data = new ConcurrentHashMap<>();
        private final Map<String, Long> expiresAt = new ConcurrentHashMap<>();

        synchronized Object get(String key) {
            Long exp = expiresAt.get(key);
            if (exp != null && System.currentTimeMillis() >= exp) {
                data.remove(key);
                expiresAt.remove(key);
                return null;
            }
            return data.get(key);
        }

        synchronized void put(String key, Object value, Duration ttl) {
            data.put(key, value);
            if (ttl != null) {
                expiresAt.put(key, System.currentTimeMillis() + ttl.toMillis());
            } else {
                expiresAt.remove(key);
            }
        }

        synchronized boolean remove(String key) {
            expiresAt.remove(key);
            return data.remove(key) != null;
        }

        synchronized boolean expire(String key, Duration ttl) {
            if (get(key) == null) return false;
            expiresAt.put(key, System.currentTimeMillis() + ttl.toMillis());
            return true;
        }

        synchronized Set<String> keys(String globPattern) {
            Pattern regex = globToRegex(globPattern);
            Set<String> result = new LinkedHashSet<>();
            for (String key : new ArrayList<>(data.keySet())) {
                if (get(key) != null && regex.matcher(key).matches()) result.add(key);
            }
            return result;
        }

        @SuppressWarnings("unchecked")
        synchronized <T> T compute(String key, Function<Object, T> fn) {
            Object current = get(key);
            T next = fn.apply(current);
            // TTL 은 유지한다 (INCR/SADD/ZINCRBY 가 기존 만료 시간을 바꾸지 않는 것과 동일)
            data.put(key, next);
            return next;
        }

        private static Pattern globToRegex(String glob) {
            StringBuilder sb = new StringBuilder();
            for (char c : (glob == null ? "*" : glob).toCharArray()) {
                switch (c) {
                    case '*' -> sb.append(".*");
                    case '?' -> sb.append('.');
                    default -> sb.append(Pattern.quote(String.valueOf(c)));
                }
            }
            return Pattern.compile(sb.toString());
        }
    }

    private final Store store;
    private final Function<Object, V> reader;

    /**
     * @param reader 저장된 값을 템플릿의 값 타입으로 바꾸는 함수 (String 템플릿은 {@code String::valueOf})
     */
    public InMemoryRedisTemplate(Store store, Function<Object, V> reader) {
        this.store = store;
        this.reader = reader;
    }

    /** 연결 팩토리가 없어도 되도록 초기화 검증을 생략한다. */
    @Override
    public void afterPropertiesSet() {
        // no-op
    }

    // ---- 키 연산 -------------------------------------------------------------------------

    @Override
    public Boolean hasKey(String key) {
        return store.get(key) != null;
    }

    @Override
    public Boolean delete(String key) {
        return store.remove(key);
    }

    @Override
    public Long delete(Collection<String> keys) {
        return keys.stream().filter(store::remove).count();
    }

    @Override
    public Boolean unlink(String key) {
        return delete(key);
    }

    @Override
    public Long unlink(Collection<String> keys) {
        return delete(keys);
    }

    @Override
    public Set<String> keys(String pattern) {
        return store.keys(pattern);
    }

    @Override
    public Boolean expire(String key, long timeout, TimeUnit unit) {
        return store.expire(key, Duration.ofMillis(unit.toMillis(timeout)));
    }

    @Override
    public Cursor<String> scan(ScanOptions options) {
        String pattern = options != null && options.getPattern() != null ? options.getPattern() : "*";
        Iterator<String> it = new ArrayList<>(store.keys(pattern)).iterator();
        return proxy(Cursor.class, (p, method, args) -> switch (method.getName()) {
            case "hasNext" -> it.hasNext();
            case "next" -> it.next();
            case "forEachRemaining" -> {
                @SuppressWarnings("unchecked")
                java.util.function.Consumer<Object> action = (java.util.function.Consumer<Object>) args[0];
                it.forEachRemaining(action);
                yield null;
            }
            case "close" -> null;
            case "isClosed" -> !it.hasNext();
            case "getCursorId", "getPosition" -> 0L;
            case "hashCode" -> System.identityHashCode(p);
            case "equals" -> p == args[0];
            case "toString" -> "InMemoryCursor";
            default -> throw unsupported("Cursor", method.getName());
        });
    }

    // ---- 값(String) 연산 -----------------------------------------------------------------

    @Override
    public ValueOperations<String, V> opsForValue() {
        return proxy(ValueOperations.class, (p, method, args) -> {
            String name = method.getName();
            int n = args == null ? 0 : args.length;
            String key = n > 0 ? (String) args[0] : null;
            switch (name) {
                case "get":
                    if (n == 1) return read(store.get(key));
                    break;
                case "set":
                    if (n == 2) { store.put(key, args[1], null); return null; }
                    if (n == 3 && args[2] instanceof Duration d) { store.put(key, args[1], d); return null; }
                    if (n == 4 && args[3] instanceof TimeUnit u) {
                        store.put(key, args[1], Duration.ofMillis(u.toMillis((Long) args[2])));
                        return null;
                    }
                    break;
                case "setIfAbsent":
                    if (n == 2) {
                        synchronized (store) {
                            if (store.get(key) != null) return false;
                            store.put(key, args[1], null);
                            return true;
                        }
                    }
                    break;
                case "increment":
                    return store.compute(key, cur -> toLong(cur) + (n == 2 ? ((Number) args[1]).longValue() : 1L));
                case "decrement":
                    return store.compute(key, cur -> toLong(cur) - (n == 2 ? ((Number) args[1]).longValue() : 1L));
                default:
                    break;
            }
            return objectMethod(p, method.getName(), args, "ValueOperations");
        });
    }

    // ---- 리스트 연산 ---------------------------------------------------------------------

    @Override
    public ListOperations<String, V> opsForList() {
        return proxy(ListOperations.class, (p, method, args) -> {
            String name = method.getName();
            int n = args == null ? 0 : args.length;
            String key = n > 0 ? (String) args[0] : null;
            if ("rightPush".equals(name) && n == 2) {
                return (long) store.<List<Object>>compute(key, cur -> {
                    List<Object> list = cur instanceof List<?> l ? new ArrayList<>(l) : new ArrayList<>();
                    list.add(args[1]);
                    return list;
                }).size();
            }
            if ("range".equals(name) && n == 3) {
                Object cur = store.get(key);
                List<Object> list = cur instanceof List<?> l ? new ArrayList<>(l) : new ArrayList<>();
                int size = list.size();
                int start = normalizeIndex(((Number) args[1]).longValue(), size);
                int end = Math.min(normalizeIndex(((Number) args[2]).longValue(), size), size - 1);
                List<V> result = new ArrayList<>();
                for (int i = Math.max(start, 0); i <= end; i++) result.add(read(list.get(i)));
                return result;
            }
            if ("size".equals(name) && n == 1) {
                Object cur = store.get(key);
                return cur instanceof List<?> l ? (long) l.size() : 0L;
            }
            return objectMethod(p, name, args, "ListOperations");
        });
    }

    // ---- 집합 연산 -----------------------------------------------------------------------

    @Override
    public SetOperations<String, V> opsForSet() {
        return proxy(SetOperations.class, (p, method, args) -> {
            String name = method.getName();
            int n = args == null ? 0 : args.length;
            String key = n > 0 ? (String) args[0] : null;
            if ("add".equals(name) || "remove".equals(name)) {
                Object[] members = n == 2 && args[1] instanceof Object[] arr ? arr : new Object[0];
                boolean add = "add".equals(name);
                long[] changed = {0};
                store.compute(key, cur -> {
                    Set<Object> set = cur instanceof Set<?> s ? new LinkedHashSet<>(s) : new LinkedHashSet<>();
                    for (Object m : members) {
                        if (add ? set.add(m) : set.remove(m)) changed[0]++;
                    }
                    return set;
                });
                return changed[0];
            }
            if ("isMember".equals(name) && n == 2) {
                Object cur = store.get(key);
                return cur instanceof Set<?> s && s.contains(args[1]);
            }
            if ("members".equals(name) && n == 1) {
                Object cur = store.get(key);
                Set<V> result = new LinkedHashSet<>();
                if (cur instanceof Set<?> s) s.forEach(m -> result.add(read(m)));
                return result;
            }
            return objectMethod(p, name, args, "SetOperations");
        });
    }

    // ---- 정렬 집합 연산 ------------------------------------------------------------------

    @Override
    public ZSetOperations<String, V> opsForZSet() {
        return proxy(ZSetOperations.class, (p, method, args) -> {
            String name = method.getName();
            int n = args == null ? 0 : args.length;
            String key = n > 0 ? (String) args[0] : null;
            if ("incrementScore".equals(name) && n == 3) {
                double delta = ((Number) args[2]).doubleValue();
                double[] updated = {0};
                store.compute(key, cur -> {
                    Map<Object, Double> zset = cur instanceof Map<?, ?> m ? copyScores(m) : new LinkedHashMap<>();
                    updated[0] = zset.merge(args[1], delta, Double::sum);
                    return zset;
                });
                return updated[0];
            }
            if (("reverseRange".equals(name) || "reverseRangeWithScores".equals(name)) && n == 3) {
                Object cur = store.get(key);
                Map<Object, Double> zset = cur instanceof Map<?, ?> m ? copyScores(m) : new LinkedHashMap<>();
                List<Map.Entry<Object, Double>> sorted = new ArrayList<>(zset.entrySet());
                sorted.sort(Map.Entry.<Object, Double>comparingByValue(Comparator.reverseOrder()));
                int size = sorted.size();
                int start = Math.max(normalizeIndex(((Number) args[1]).longValue(), size), 0);
                int end = Math.min(normalizeIndex(((Number) args[2]).longValue(), size), size - 1);
                boolean withScores = "reverseRangeWithScores".equals(name);
                Set<Object> result = new LinkedHashSet<>();
                for (int i = start; i <= end; i++) {
                    Map.Entry<Object, Double> e = sorted.get(i);
                    result.add(withScores ? new DefaultTypedTuple<>(read(e.getKey()), e.getValue()) : read(e.getKey()));
                }
                return result;
            }
            return objectMethod(p, name, args, "ZSetOperations");
        });
    }

    // ---- 내부 도우미 ---------------------------------------------------------------------

    private V read(Object raw) {
        return raw == null ? null : reader.apply(raw);
    }

    private static long toLong(Object cur) {
        if (cur == null) return 0L;
        if (cur instanceof Number num) return num.longValue();
        try {
            return Long.parseLong(cur.toString());
        } catch (NumberFormatException e) {
            throw new IllegalStateException("ERR value is not an integer or out of range");
        }
    }

    private static int normalizeIndex(long index, int size) {
        long i = index < 0 ? size + index : index;
        return (int) Math.min(i, Integer.MAX_VALUE);
    }

    private static Map<Object, Double> copyScores(Map<?, ?> source) {
        Map<Object, Double> copy = new LinkedHashMap<>();
        source.forEach((k, v) -> copy.put(k, ((Number) v).doubleValue()));
        return copy;
    }

    private static Object objectMethod(Object proxy, String name, Object[] args, String type) {
        return switch (name) {
            case "hashCode" -> System.identityHashCode(proxy);
            case "equals" -> proxy == args[0];
            case "toString" -> "InMemory" + type;
            default -> throw unsupported(type, name);
        };
    }

    private static UnsupportedOperationException unsupported(String type, String method) {
        return new UnsupportedOperationException(
                "e2e 인메모리 Redis 가 지원하지 않는 연산입니다: " + type + "." + method
                        + " (e2e/InMemoryRedisTemplate 에 추가하세요)");
    }

    @SuppressWarnings("unchecked")
    private static <T> T proxy(Class<?> type, InvocationHandler handler) {
        return (T) Proxy.newProxyInstance(InMemoryRedisTemplate.class.getClassLoader(), new Class<?>[]{type}, handler);
    }
}
