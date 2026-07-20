package com.bm.bmrpc.loadbalancer;

import com.bm.bmrpc.model.ServiceMetaInfo;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

/**
 * 一致性哈希负载均衡器
 * <p>
 * 线程安全策略：
 * <ul>
 *     <li>将 hash 环和对应的节点地址集合封装在不可变的 {@link RingSnapshot} 中</li>
 *     <li>使用 {@link AtomicReference} + CAS 保证「检测变更 → 重建 → 写入」的原子性</li>
 *     <li>重建过程在局部变量中完成后才通过 CAS 发布（copy-on-write），读操作全程无锁</li>
 *     <li>CAS 失败说明其他线程已更新环，当前线程放弃写入并使用最新环，避免旧列表覆盖新环</li>
 * </ul>
 *
 * @author <a href="https://github.com/liyupi">程序员鱼皮</a>
 * @learn <a href="https://codefather.cn">鱼皮的编程宝典</a>
 * @from <a href="https://yupi.icu">编程导航知识星球</a>
 */
public class ConsistentHashLoadBalancer implements LoadBalancer {

    /**
     * 虚拟节点数
     */
    private static final int VIRTUAL_NODE_NUM = 100;

    /**
     * 当前 Hash 环快照（AtomicReference + CAS 保证原子替换）
     * <p>
     * 将 hash 环和对应的节点地址集合封装在不可变对象中，通过 CAS 原子替换，
     * 避免以下问题：
     * <ul>
     *     <li>多字段可见性交错（环已更新但地址集合未更新）</li>
     *     <li>旧列表的线程覆盖新列表的线程已写入的环（CAS 失败时放弃写入）</li>
     * </ul>
     */
    private final AtomicReference<RingSnapshot> ringSnapshot = new AtomicReference<>(RingSnapshot.EMPTY);

    /**
     * Hash 环快照（不可变对象，发布后不再修改）
     */
    private static final class RingSnapshot {
        /** 空快照，作为初始值 */
        static final RingSnapshot EMPTY = new RingSnapshot(new TreeMap<>(), Collections.emptySet());

        /** 一致性 Hash 环，存放虚拟节点 */
        final TreeMap<Integer, ServiceMetaInfo> virtualNodes;

        /** 构建此环时使用的服务节点地址集合（用于检测节点变动） */
        final Set<String> nodeAddresses;

        RingSnapshot(TreeMap<Integer, ServiceMetaInfo> virtualNodes, Set<String> nodeAddresses) {
            this.virtualNodes = virtualNodes;
            this.nodeAddresses = nodeAddresses;
        }
    }

    @Override
    public ServiceMetaInfo select(Map<String, Object> requestParams, List<ServiceMetaInfo> serviceMetaInfoList) {
        if (serviceMetaInfoList.isEmpty()) {
            return null;
        }

        // 1. 计算当前服务节点地址集合
        Set<String> currentNodeAddresses = serviceMetaInfoList.stream()
                .map(ServiceMetaInfo::getServiceAddress)
                .collect(Collectors.toSet());

        // 2. 读取当前快照（局部变量，保证本次调用内看到的是同一个一致的快照）
        RingSnapshot snapshot = ringSnapshot.get();

        // 3. 对比节点是否有变动，有变动则重新构建 hash 环
        if (!currentNodeAddresses.equals(snapshot.nodeAddresses)) {
            // 在局部变量中构建全新的环，构建完成后通过 CAS 发布（copy-on-write）
            TreeMap<Integer, ServiceMetaInfo> newVirtualNodes = new TreeMap<>();
            for (ServiceMetaInfo serviceMetaInfo : serviceMetaInfoList) {
                for (int i = 0; i < VIRTUAL_NODE_NUM; i++) {
                    int hash = getHash(serviceMetaInfo.getServiceAddress() + "#" + i);
                    newVirtualNodes.put(hash, serviceMetaInfo);
                }
            }
            RingSnapshot newSnapshot = new RingSnapshot(newVirtualNodes, currentNodeAddresses);

            // CAS：仅当快照未被其他线程修改时才写入
            //   成功 → 本次重建的环生效
            //   失败 → 其他线程已写入更新的环，放弃本次写入，使用最新的环
            //         这样避免了「旧列表线程重建慢，完成后覆盖新列表线程已写入的环」
            if (ringSnapshot.compareAndSet(snapshot, newSnapshot)) {
                snapshot = newSnapshot;
            } else {
                // CAS 失败：其他线程已经更新了环，重新读取最新快照
                snapshot = ringSnapshot.get();
            }
        }

        // 4. 获取调用请求的 hash 值
        int hash = getHash(requestParams);

        // 5. 选择最接近且大于等于调用请求 hash 值的虚拟节点
        TreeMap<Integer, ServiceMetaInfo> nodes = snapshot.virtualNodes;
        Map.Entry<Integer, ServiceMetaInfo> entry = nodes.ceilingEntry(hash);
        if (entry == null) {
            // 如果没有大于等于调用请求 hash 值的虚拟节点，则返回环首部的节点
            entry = nodes.firstEntry();
        }
        return entry.getValue();
    }


    /**
     * Hash 算法（FNV-1a 32-bit）
     * <p>
     * 相比 {@code String.hashCode()}（多项式哈希 h=31*h+c），FNV-1a 具有良好的雪崩效应：
     * 输入的微小变化（如 {@code "#0"} vs {@code "#1"}）会导致哈希值剧烈变化，
     * 确保同一服务的 100 个虚拟节点均匀分散在整个哈希环上。
     * <p>
     * 若使用 {@code String.hashCode()}，仅末尾字符不同的字符串哈希值差异仅为 1，
     * 会导致虚拟节点全部聚集在环上一个极小区间，负载均衡完全失效。
     *
     * @param key 支持任意类型，Map 类型会先做规范化排序再哈希
     * @return 32-bit 哈希值
     */
    private int getHash(Object key) {
        // 对 Map 做规范化处理：按 key 排序后拼接，保证相同内容产生相同哈希
        String str;
        if (key instanceof Map) {
            str = ((Map<?, ?>) key).entrySet().stream()
                    .sorted(Comparator.comparing(e -> String.valueOf(e.getKey())))
                    .map(e -> e.getKey() + "=" + e.getValue())
                    .collect(Collectors.joining("&"));
        } else {
            str = key.toString();
        }

        // FNV-1a 32-bit
        int hash = 0x811c9dc5;
        for (byte b : str.getBytes(StandardCharsets.UTF_8)) {
            hash ^= (b & 0xff);
            hash *= 0x01000193;
        }
        return hash;
    }
}
