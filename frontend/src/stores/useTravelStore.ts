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

export type RouteStrategy = "shortest" | "fastest" | "least_climb" | "fewest_transfers" | "photo";

export const DEFAULT_STRATEGY: RouteStrategy = "shortest";

export type PathResult = {
  startId: string;
  endId: string;
  totalDistanceMeters: number;
  pathNodeIds: string[];
  pathNodes: TravelNode[];
  segmentDistanceMeters: number[];
  strategy?: string;
  strategyLabel?: string;
  totalTimeMinutes?: number;
  totalClimbMeters?: number;
  totalTransfers?: number;
};

type State = {
  nodes: TravelNode[];
  nodesLoading: boolean;
  startId?: string;
  endId?: string;
  strategy: RouteStrategy;
  route?: PathResult;
  routeLoading: boolean;
  selectedNodeId?: string;
};

type Actions = {
  loadNodes: () => Promise<void>;
  setStartId: (id?: string) => void;
  setEndId: (id?: string) => void;
  setStrategy: (strategy: RouteStrategy) => void;
  swap: () => void;
  clear: () => void;
  setSelectedNodeId: (id?: string) => void;
  fetchRoute: () => Promise<void>;
};

const apiBase = import.meta.env.VITE_API_BASE || "/api";

export const useTravelStore = create<State & Actions>((set, get) => ({
  nodes: [],
  nodesLoading: false,
  strategy: DEFAULT_STRATEGY,
  routeLoading: false,

  loadNodes: async () => {
    if (get().nodesLoading) return;
    set({ nodesLoading: true });
    try {
      const res = await fetch(`${apiBase}/nodes`);
      if (!res.ok) throw new Error("nodes_fetch_failed");
      const data = await res.json();
      if (!Array.isArray(data)) throw new Error("nodes_payload_invalid");
      const nodes = (data as Array<Record<string, unknown>>)
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

  // 切换策略时若起终点已齐全，直接按新策略重新规划
  setStrategy: (strategy) => {
    set({ strategy, route: undefined });
    const { startId, endId } = get();
    if (startId && endId) {
      void get().fetchRoute();
    }
  },

  swap: () => {
    const { startId, endId } = get();
    set({ startId: endId, endId: startId, route: undefined });
  },

  clear: () =>
    set({ startId: undefined, endId: undefined, route: undefined, selectedNodeId: undefined, strategy: DEFAULT_STRATEGY }),

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
