package com.cqu.model;

/**
 * 路线规划策略。不同策略只是给 Dijkstra 提供不同的边权重函数，
 * 最短路径算法本身保持不变。
 */
public enum RouteStrategy {
    /** 距离最短（默认，保持原有行为） */
    SHORTEST("shortest", "距离最短"),
    /** 省时间：按通行时间加权 */
    FASTEST("fastest", "省时间"),
    /** 少爬坡：按爬升高度加权 */
    LEAST_CLIMB("least_climb", "少爬坡"),
    /** 少换乘：按换乘次数加权 */
    FEWEST_TRANSFERS("fewest_transfers", "少换乘"),
    /** 适合拍照：优先经过拍照指数高的景点 */
    PHOTO("photo", "适合拍照");

    private final String code;
    private final String label;

    RouteStrategy(String code, String label) {
        this.code = code;
        this.label = label;
    }

    public String getCode() {
        return code;
    }

    public String getLabel() {
        return label;
    }

    /**
     * 解析前端传入的策略参数。null / 空白时回退到默认的距离最短策略，
     * 无法识别时抛出 IllegalArgumentException 由上层转成 400。
     */
    public static RouteStrategy fromCode(String raw) {
        if (raw == null || raw.isBlank()) {
            return SHORTEST;
        }
        String norm = raw.trim().toLowerCase();
        for (RouteStrategy s : values()) {
            if (s.code.equals(norm) || s.name().equalsIgnoreCase(norm)) {
                return s;
            }
        }
        throw new IllegalArgumentException("未知的路线策略：" + raw);
    }
}
