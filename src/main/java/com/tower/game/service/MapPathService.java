package com.tower.game.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Set;

/**
 * 服务端寻路：A* 算法，基于 MapWalkableService 可通行性。
 * 目标格为互动格（怪物/宝箱）时，路径终点仅为相邻可走格，不包含事件格本身（方案 A）。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MapPathService {

    private static final int EVENT_TYPE_MONSTER = 5;
    private static final int EVENT_TYPE_CHEST = 6;
    private static final int EVENT_TYPE_KEY = 7;
    private static final int EVENT_TYPE_DOOR = 8;
    private static final int EVENT_TYPE_BLOOD_POTION = 9;
    private static final int[][] NEIGHBORS = {{0, 1}, {1, 0}, {0, -1}, {-1, 0}};

    private final MapWalkableService mapWalkableService;

    /**
     * 从 (fromX, fromY) 寻路到 (toX, toY)。
     * 若目标格为怪物/宝箱，则终点改为该格相邻的一格可通行格。
     *
     * @param mapData 缓存的地图 JSON，不可为空
     * @return 路径格子列表（不含起点，含终点），无法到达则返回空列表
     */
    public List<int[]> findPath(Integer mapId, int fromX, int fromY, int toX, int toY, String mapData) {
        if (mapId == null) return List.of();
        int[] size = mapWalkableService.getMapSize(mapId, mapData);
        int width = size[0], height = size[1];
        if (fromX < 0 || fromX >= width || fromY < 0 || fromY >= height) return List.of();
        if (toX < 0 || toX >= width || toY < 0 || toY >= height) return List.of();
        if (!mapWalkableService.isWalkableForPathfinding(mapId, fromX, fromY, mapData)) return List.of();

        int endX = toX, endY = toY;
        boolean endIsEvent = false;
        int[] cellEvent = mapWalkableService.getCellEvent(mapId, toX, toY, mapData);
        if (cellEvent != null) {
            int eventType = cellEvent[0];
            if (eventType == EVENT_TYPE_MONSTER || eventType == EVENT_TYPE_CHEST
                    || eventType == EVENT_TYPE_KEY || eventType == EVENT_TYPE_DOOR
                    || eventType == EVENT_TYPE_BLOOD_POTION) {
                endIsEvent = true;
                // 方案 A：路径终点仅为相邻可走格，不包含事件格本身
            } else if (!mapWalkableService.isWalkableForPathfinding(mapId, toX, toY, mapData)) {
                return List.of();
            }
        } else {
            if (!mapWalkableService.isWalkableForPathfinding(mapId, toX, toY, mapData)) return List.of();
        }

        if (fromX == endX && fromY == endY) return List.of();

        return aStar(mapId, mapData, width, height, fromX, fromY, endX, endY, endIsEvent);
    }

    private int[] getAdjacentWalkableForPathfinding(Integer mapId, String mapData, int x, int y) {
        for (int[] d : NEIGHBORS) {
            int nx = x + d[0], ny = y + d[1];
            if (mapWalkableService.isWalkableForPathfinding(mapId, nx, ny, mapData))
                return new int[]{nx, ny};
        }
        return null;
    }

    private List<int[]> aStar(Integer mapId, String mapData, int width, int height, int fromX, int fromY, int endX, int endY, boolean endIsEvent) {
        PriorityQueue<Node> open = new PriorityQueue<>(Comparator.comparingDouble(n -> n.f));
        Set<Long> closed = new HashSet<>();
        open.add(new Node(fromX, fromY, 0, manhattan(fromX, fromY, endX, endY), null));

        while (!open.isEmpty()) {
            Node cur = open.poll();
            long key = key(cur.x, cur.y);
            if (closed.contains(key)) continue;
            closed.add(key);

            if (cur.x == endX && cur.y == endY) {
                return buildPathFromNode(cur, fromX, fromY);
            }

            for (int[] d : NEIGHBORS) {
                int nx = cur.x + d[0], ny = cur.y + d[1];
                if (nx < 0 || nx >= width || ny < 0 || ny >= height) continue;
                if (nx == endX && ny == endY && endIsEvent) {
                    // 方案 A：路径只到相邻格，不包含事件格；终点为 cur（怪物/宝箱面前的格）
                    return buildPathFromNode(cur, fromX, fromY);
                }
                if (!mapWalkableService.isWalkableForPathfinding(mapId, nx, ny, mapData)) continue;
                if (closed.contains(key(nx, ny))) continue;

                double g = cur.g + 1;
                double h = manhattan(nx, ny, endX, endY);
                open.add(new Node(nx, ny, g, h, cur));
            }
        }
        return List.of();
    }

    private List<int[]> buildPathFromNode(Node cur, int fromX, int fromY) {
        List<int[]> path = new ArrayList<>();
        for (Node n = cur; n != null; n = n.parent) path.add(new int[]{n.x, n.y});
        path.remove(path.size() - 1); // 去掉起点
        Collections.reverse(path);
        return path;
    }

    private static double manhattan(int x1, int y1, int x2, int y2) {
        return Math.abs(x1 - x2) + Math.abs(y1 - y2);
    }

    private static long key(int x, int y) {
        return ((long) x << 32) | (y & 0xFFFFFFFFL);
    }

    private static class Node {
        final int x, y;
        final double g, h, f;
        final Node parent;

        Node(int x, int y, double g, double h, Node parent) {
            this.x = x;
            this.y = y;
            this.g = g;
            this.h = h;
            this.f = g + h;
            this.parent = parent;
        }
    }
}
