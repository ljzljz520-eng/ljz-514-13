import { create } from "zustand";
import { notification } from "antd";

export type TravelNode = {
  id: string;
  name: string;
  lat: number;
  lng: number;
  type?: string;
  desc?: string;
};

export type RouteStrategyKey = "shortest" | "fastest" | "less-climb" | "less-transfer" | "photo";

export const STRATEGIES: Array<{ key: RouteStrategyKey; name: string; hint: string }> = [
  { key: "shortest", name: "最短距离", hint: "按地理距离规划（默认）" },
  { key: "fastest", name: "省时间", hint: "优先总耗时最短的路线" },
  { key: "less-climb", name: "少爬坡", hint: "尽量避开上坡路段" },
  { key: "less-transfer", name: "少换乘", hint: "尽量减少换乘次数" },
  { key: "photo", name: "适合拍照", hint: "优先经过高颜值景点" },
];

export type PathResult = {
  startId: string;
  endId: string;
  totalDistanceMeters: number;
  pathNodeIds: string[];
  pathNodes: TravelNode[];
  segmentDistanceMeters: number[];
  strategy?: string;
  strategyName?: string;
  totalTimeMinutes?: number;
  totalClimbMeters?: number;
  totalTransfers?: number;
};

type State = {
  nodes: TravelNode[];
  nodesLoading: boolean;
  startId?: string;
  endId?: string;
  strategy: RouteStrategyKey;
  route?: PathResult;
  routeLoading: boolean;
  selectedNodeId?: string;
};

type Actions = {
  loadNodes: () => Promise<void>;
  setStartId: (id?: string) => void;
  setEndId: (id?: string) => void;
  setStrategy: (s: RouteStrategyKey) => void;
  swap: () => void;
  clear: () => void;
  setSelectedNodeId: (id?: string) => void;
  fetchRoute: () => Promise<void>;
};

const apiBase = import.meta.env.VITE_API_BASE || "/api";

export const useTravelStore = create<State & Actions>((set, get) => ({
  nodes: [],
  nodesLoading: false,
  strategy: "shortest",
  routeLoading: false,

  loadNodes: async () => {
    if (get().nodesLoading) return;
    set({ nodesLoading: true });
    try {
      const res = await fetch(`${apiBase}/nodes`);
      if (!res.ok) throw new Error("nodes_fetch_failed");
      const data = await res.json();
      if (!Array.isArray(data)) throw new Error("nodes_payload_invalid");
      const nodes = (data as any[])
        .map((raw) => {
          const id = String(raw?.id ?? "").trim();
          const name = String(raw?.name ?? "").trim();
          const lat = Number(raw?.lat);
          const lng = Number(raw?.lng);
          const type = typeof raw?.type === "string" ? raw.type : undefined;
          const desc = typeof raw?.desc === "string" ? raw.desc : undefined;
          return { id, name, lat, lng, type, desc } satisfies TravelNode;
        })
        .filter((n) => n.id && Number.isFinite(n.lat) && Number.isFinite(n.lng));
      set({ nodes });
    } catch {
      notification.error({ message: "加载节点失败", description: "请检查后端服务是否已启动" });
    } finally {
      set({ nodesLoading: false });
    }
  },

  setStartId: (id) => set({ startId: id, route: undefined }),
  setEndId: (id) => set({ endId: id, route: undefined }),
  setSelectedNodeId: (id) => set({ selectedNodeId: id }),

  setStrategy: (strategy) => {
    const { strategy: prev, route, startId, endId } = get();
    if (prev === strategy) return;
    set({ strategy, route: undefined });
    // 已有规划结果时切换策略立即重算，保证展示与策略一致
    if (route && startId && endId) {
      void get().fetchRoute();
    }
  },

  swap: () => {
    const { startId, endId } = get();
    set({ startId: endId, endId: startId, route: undefined });
  },

  clear: () => set({ startId: undefined, endId: undefined, route: undefined, selectedNodeId: undefined }),

  fetchRoute: async () => {
    const { startId, endId, strategy } = get();
    if (!startId || !endId) {
      notification.warning({ message: "请选择起点与终点" });
      return;
    }
    set({ routeLoading: true });
    try {
      const qs = new URLSearchParams({ from: startId, to: endId, strategy });
      const res = await fetch(`${apiBase}/path?${qs.toString()}`);
      const data = await res.json();
      if (!res.ok) {
        notification.error({ message: "规划失败", description: data?.error || "后端错误" });
        return;
      }
      set({ route: data as PathResult });
    } catch {
      notification.error({ message: "规划失败", description: "网络异常或后端不可用" });
    } finally {
      set({ routeLoading: false });
    }
  },
}));
