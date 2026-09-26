import { Button, Divider, Select, Skeleton, Typography } from "antd";
import { ArrowLeftRight, Camera, Clock3, Mountain, Route, Shuffle, X } from "lucide-react";
import { useMemo, useState } from "react";
import { STRATEGIES, useTravelStore } from "@/stores/useTravelStore";

const { Text } = Typography;

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

  const activeStrategy = useMemo(() => STRATEGIES.find((s) => s.key === strategy), [strategy]);

  const distanceText = useMemo(() => {
    if (!route) return "";
    const m = route.totalDistanceMeters;
    if (m < 1000) return `${Math.round(m)} m`;
    return `${(m / 1000).toFixed(2)} km`;
  }, [route]);

  const durationText = useMemo(() => {
    if (!route || typeof route.totalTimeMinutes !== "number") return "";
    const min = Math.round(route.totalTimeMinutes);
    if (min < 60) return `约 ${min} 分钟`;
    const h = Math.floor(min / 60);
    const m = min % 60;
    return m === 0 ? `约 ${h} 小时` : `约 ${h} 小时 ${m} 分钟`;
  }, [route]);

  return (
    <div className="h-full flex flex-col p-4">
      <div className="flex items-start justify-between gap-3">
        <div>
          <div className="text-base font-semibold text-slate-900">重庆旅游线路规划</div>
          <div className="mt-1 flex items-center gap-2">
            <span className="inline-flex items-center rounded-full bg-slate-100 px-2 py-0.5 text-xs text-slate-700">数据源：nodes.csv</span>
            <span className="inline-flex items-center rounded-full bg-blue-50 px-2 py-0.5 text-xs text-blue-700">
              策略：{activeStrategy?.name ?? "最短距离"}
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
                const active = s.key === strategy;
                return (
                  <button
                    key={s.key}
                    type="button"
                    title={s.hint}
                    onClick={() => setStrategy(s.key)}
                    className={
                      "rounded-lg border px-2 py-1.5 text-xs transition-colors " +
                      (active
                        ? "border-blue-600 bg-blue-600 text-white"
                        : "border-slate-200 bg-white text-slate-700 hover:border-blue-300 hover:text-blue-600")
                    }
                  >
                    {s.name}
                  </button>
                );
              })}
            </div>
            <div className="text-xs text-slate-400">{activeStrategy?.hint}</div>
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
                  {route.strategyName || activeStrategy?.name || "最短距离"}
                </span>
              ) : null}
            </div>

            {route ? (
              <>
                <div className="mt-2 flex flex-wrap gap-1.5">
                  {durationText ? (
                    <span className="inline-flex items-center gap-1 rounded-full bg-emerald-50 px-2 py-0.5 text-xs text-emerald-700">
                      <Clock3 className="h-3 w-3" />
                      总耗时 {durationText}
                    </span>
                  ) : null}
                  <span className="inline-flex items-center gap-1 rounded-full bg-slate-100 px-2 py-0.5 text-xs text-slate-700">
                    <Route className="h-3 w-3" />
                    总距离 {distanceText}
                  </span>
                  {typeof route.totalClimbMeters === "number" && route.totalClimbMeters > 0 ? (
                    <span className="inline-flex items-center gap-1 rounded-full bg-amber-50 px-2 py-0.5 text-xs text-amber-700">
                      <Mountain className="h-3 w-3" />
                      爬升 {Math.round(route.totalClimbMeters)} m
                    </span>
                  ) : null}
                  {typeof route.totalTransfers === "number" && route.totalTransfers > 0 ? (
                    <span className="inline-flex items-center gap-1 rounded-full bg-violet-50 px-2 py-0.5 text-xs text-violet-700">
                      <Shuffle className="h-3 w-3" />
                      换乘 {route.totalTransfers} 次
                    </span>
                  ) : null}
                  {route.strategy === "photo" ? (
                    <span className="inline-flex items-center gap-1 rounded-full bg-pink-50 px-2 py-0.5 text-xs text-pink-700">
                      <Camera className="h-3 w-3" />
                      沿途适合拍照
                    </span>
                  ) : null}
                </div>

                <div className="mt-3 text-xs text-slate-500">经过景点（{route.pathNodes.length} 个）：</div>
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
              <div className="mt-3 text-sm text-slate-600">选择起点与终点后开始规划。</div>
            )}
          </div>
        </>
      )}
    </div>
  );
}
