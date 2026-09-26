import { Button, Divider, Select, Skeleton, Tooltip, Typography } from "antd";
import {
  ArrowLeftRight,
  Camera,
  Clock3,
  Mountain,
  Repeat,
  Route,
  Ruler,
  X,
} from "lucide-react";
import type { ReactNode } from "react";
import { useMemo, useState } from "react";
import type { RouteStrategy } from "@/stores/useTravelStore";
import { useTravelStore } from "@/stores/useTravelStore";

const { Text } = Typography;

const STRATEGIES: Array<{
  value: RouteStrategy;
  label: string;
  hint: string;
  icon: ReactNode;
}> = [
  { value: "shortest", label: "距离最短", hint: "总里程最省", icon: <Ruler className="h-3.5 w-3.5" /> },
  { value: "fastest", label: "省时间", hint: "总耗时最少", icon: <Clock3 className="h-3.5 w-3.5" /> },
  { value: "least_climb", label: "少爬坡", hint: "爬坡路段更少", icon: <Mountain className="h-3.5 w-3.5" /> },
  { value: "fewest_transfers", label: "少换乘", hint: "换乘次数更少", icon: <Repeat className="h-3.5 w-3.5" /> },
  { value: "photo", label: "适合拍照", hint: "优先高颜值景点", icon: <Camera className="h-3.5 w-3.5" /> },
];

function formatDuration(totalMinutes?: number): string {
  if (totalMinutes == null || !Number.isFinite(totalMinutes)) return "";
  const m = Math.round(totalMinutes);
  if (m <= 0) return "约 0 分钟";
  if (m < 60) return `约 ${m} 分钟`;
  const h = Math.floor(m / 60);
  const rest = m % 60;
  return rest === 0 ? `约 ${h} 小时` : `约 ${h} 小时 ${rest} 分钟`;
}

export default function ControlPanel() {
  const nodes = useTravelStore((s) => s.nodes);
  const nodesLoading = useTravelStore((s) => s.nodesLoading);
  const startId = useTravelStore((s) => s.startId);
  const endId = useTravelStore((s) => s.endId);
  const strategy = useTravelStore((s) => s.strategy);
  const route = useTravelStore((s) => s.route);
  const routeLoading = useTravelStore((s) => s.routeLoading);
  const setStartId = useTravelStore((s) => s.setStartId);
  const setEndId = useTravelStore((s) => s.setEndId);
  const setStrategy = useTravelStore((s) => s.setStrategy);
  const swap = useTravelStore((s) => s.swap);
  const clear = useTravelStore((s) => s.clear);
  const fetchRoute = useTravelStore((s) => s.fetchRoute);

  const [keyword, setKeyword] = useState<string>("");

  const options = useMemo(() => {
    const k = keyword.trim().toLowerCase();
    const list = k ? nodes.filter((n) => (n.name || "").toLowerCase().includes(k)) : nodes;
    return list.map((n) => ({ label: n.name || n.id, value: n.id }));
  }, [keyword, nodes]);

  const distanceText = useMemo(() => {
    if (!route) return "";
    const m = route.totalDistanceMeters;
    if (m < 1000) return `${Math.round(m)} m`;
    return `${(m / 1000).toFixed(2)} km`;
  }, [route]);

  const durationText = formatDuration(route?.totalTimeMinutes);
  const strategyLabel =
    STRATEGIES.find((s) => s.value === (route?.strategy ?? strategy))?.label ?? "距离最短";

  return (
    <div className="h-full flex flex-col p-4">
      <div className="flex items-start justify-between gap-3">
        <div>
          <div className="text-base font-semibold text-slate-900">重庆旅游线路规划</div>
          <div className="mt-1 flex items-center gap-2">
            <span className="inline-flex items-center rounded-full bg-slate-100 px-2 py-0.5 text-xs text-slate-700">数据源：nodes.csv</span>
            <span className="inline-flex items-center rounded-full bg-blue-50 px-2 py-0.5 text-xs text-blue-700">
              当前策略：{strategyLabel}
            </span>
          </div>
        </div>
        <Button type="text" onClick={() => clear()} icon={<X className="h-4 w-4" />} />
      </div>

      <Divider className="my-3" />

      {nodesLoading ? (
        <Skeleton active paragraph={{ rows: 6 }} />
      ) : (
        <>
          <div className="space-y-2">
            <Text type="secondary">起点</Text>
            <Select
              showSearch
              value={startId}
              placeholder="选择起点"
              options={options}
              className="w-full"
              filterOption={false}
              onSearch={setKeyword}
              onChange={(v) => setStartId(v)}
              allowClear
            />
          </div>

          <div className="mt-3 space-y-2">
            <Text type="secondary">终点</Text>
            <Select
              showSearch
              value={endId}
              placeholder="选择终点"
              options={options}
              className="w-full"
              filterOption={false}
              onSearch={setKeyword}
              onChange={(v) => setEndId(v)}
              allowClear
            />
          </div>

          <div className="mt-3 space-y-2">
            <Text type="secondary">路线策略</Text>
            <div className="grid grid-cols-3 gap-1.5">
              {STRATEGIES.map((s) => {
                const active = strategy === s.value;
                return (
                  <Tooltip key={s.value} title={s.hint} placement="top">
                    <button
                      type="button"
                      onClick={() => setStrategy(s.value)}
                      className={
                        "flex items-center justify-center gap-1 rounded-lg border px-1 py-1.5 text-xs transition-colors " +
                        (active
                          ? "border-slate-900 bg-slate-900 text-white"
                          : "border-slate-200 bg-white text-slate-600 hover:border-slate-400 hover:text-slate-900")
                      }
                    >
                      {s.icon}
                      <span>{s.label}</span>
                    </button>
                  </Tooltip>
                );
              })}
            </div>
          </div>

          <div className="mt-4 grid grid-cols-2 gap-2">
            <Button onClick={() => swap()} icon={<ArrowLeftRight className="h-4 w-4" />}>
              交换
            </Button>
            <Button type="primary" loading={routeLoading} onClick={() => fetchRoute()} icon={<Route className="h-4 w-4" />}>
              开始规划
            </Button>
          </div>

          <Divider className="my-4" />

          <div className="flex-1 overflow-auto rounded-xl border border-slate-200 bg-white p-3">
            <div className="flex items-center justify-between">
              <div className="text-sm font-semibold text-slate-900">路径结果</div>
              {route ? (
                <span className="inline-flex items-center rounded-full bg-blue-50 px-2 py-0.5 text-xs text-blue-700">
                  {route.strategyLabel || strategyLabel}
                </span>
              ) : null}
            </div>

            {route ? (
              <>
                <div className="mt-2 flex flex-wrap gap-1.5 text-xs">
                  <span className="inline-flex items-center gap-1 rounded-full bg-slate-100 px-2 py-0.5 text-slate-700">
                    <Clock3 className="h-3 w-3" />
                    {durationText}
                  </span>
                  <span className="inline-flex items-center gap-1 rounded-full bg-slate-100 px-2 py-0.5 text-slate-700">
                    <Ruler className="h-3 w-3" />
                    {distanceText}
                  </span>
                  <span className="inline-flex items-center gap-1 rounded-full bg-slate-100 px-2 py-0.5 text-slate-700">
                    <Mountain className="h-3 w-3" />
                    爬升 {Math.round(route.totalClimbMeters ?? 0)} m
                  </span>
                  <span className="inline-flex items-center gap-1 rounded-full bg-slate-100 px-2 py-0.5 text-slate-700">
                    <Repeat className="h-3 w-3" />
                    换乘 {route.totalTransfers ?? 0} 次
                  </span>
                </div>
                <div className="mt-3 text-xs text-slate-500">经过景点（{route.pathNodes.length} 个）</div>
                <div className="mt-2 space-y-2">
                  {route.pathNodes.map((n, idx) => (
                    <div key={n.id} className="rounded-lg border border-slate-200 p-2 hover:bg-slate-50 transition-colors">
                      <div className="flex items-center justify-between">
                        <div className="text-sm text-slate-900">
                          <span className="mr-2 inline-flex h-6 w-6 items-center justify-center rounded-full bg-slate-900 text-white text-xs">
                            {idx + 1}
                          </span>
                          {n.name}
                        </div>
                        <div className="text-xs text-slate-500">{n.type || ""}</div>
                      </div>
                      {n.desc ? <div className="mt-1 text-xs text-slate-600">{n.desc}</div> : null}
                      {idx > 0 ? (
                        <div className="mt-1 text-xs text-slate-500">
                          与上一点约 {Math.round(route.segmentDistanceMeters[idx - 1])} m
                        </div>
                      ) : null}
                    </div>
                  ))}
                </div>
              </>
            ) : (
              <div className="mt-3 text-sm text-slate-600">选择起点与终点后开始规划，可随时切换路线策略。</div>
            )}
          </div>
        </>
      )}
    </div>
  );
}
