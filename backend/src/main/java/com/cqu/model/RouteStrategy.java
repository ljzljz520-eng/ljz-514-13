package com.cqu.model;

/**
 * 路线规划策略。Dijkstra 主流程不变，仅边权计算方式不同。
 */
public enum RouteStrategy {
    /** 原始逻辑：按地理距离最短 */
    SHORTEST("shortest", "最短距离"),
    /** 省时间：最小化预计总耗时（距离/速度 + 爬升与换乘惩罚） */
    FASTEST("fastest", "省时间"),
    /** 少爬坡：距离 + 爬升惩罚 */
    LESS_CLIMB("less-climb", "少爬坡"),
    /** 少换乘：距离 + 换乘惩罚 */
    LESS_TRANSFER("less-transfer", "少换乘"),
    /** 适合拍照：距离 - 景色奖励（权重保证非负） */
    PHOTO("photo", "适合拍照");

    private final String key;
    private final String displayName;

    RouteStrategy(String key, String displayName) {
        this.key = key;
        this.displayName = displayName;
    }

    public String getKey() {
        return key;
    }

    public String getDisplayName() {
        return displayName;
    }

    /**
     * 按查询参数解析策略；空值回退到 SHORTEST（保持原有行为），未知值抛出异常。
     */
    public static RouteStrategy fromKey(String key) {
        if (key == null || key.isBlank()) {
            return SHORTEST;
        }
        String normalized = key.trim().toLowerCase().replace('_', '-');
        for (RouteStrategy s : values()) {
            if (s.key.equals(normalized) || s.name().equalsIgnoreCase(key.trim())) {
                return s;
            }
        }
        throw new IllegalArgumentException("未知的路线策略：" + key);
    }
}
