package com.tower.game.service;

import com.tower.game.common.dto.battle.DropItemDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 怪物掉落随机：解析 Monster.item 字符串，按万分比与数量范围 roll（与 Unity DropParser 一致）
 */
@Slf4j
@Service
public class DropRollService {

    /**
     * 解析并 roll，返回实际掉落列表。格式：道具id_数量范围_万分比，多条用分号分隔，如 "3_1-1_10000;4_2-5_5000"
     */
    public List<DropItemDto> parseAndRoll(String itemConfig) {
        List<DropEntry> entries = parseDropString(itemConfig);
        return roll(entries);
    }

    private static class DropEntry {
        int itemId;
        int countMin;
        int countMax;
        int ratePerTenThousand;
    }

    private List<DropEntry> parseDropString(String dropStr) {
        List<DropEntry> list = new ArrayList<>();
        if (dropStr == null || dropStr.isBlank()) return list;
        String[] parts = dropStr.split(";");
        for (String part : parts) {
            String p = part.trim();
            if (p.isEmpty()) continue;
            String[] seg = p.split("_");
            if (seg.length < 3) continue;
            try {
                int itemId = Integer.parseInt(seg[0].trim());
                String range = seg[1].trim();
                int countMin = 1, countMax = 1;
                int dash = range.indexOf('-');
                if (dash >= 0) {
                    countMin = Integer.parseInt(range.substring(0, dash).trim());
                    countMax = Integer.parseInt(range.substring(dash + 1).trim());
                } else {
                    countMin = Integer.parseInt(range);
                }
                if (countMax < countMin) countMax = countMin;
                int rate = Integer.parseInt(seg[2].trim());
                rate = Math.max(0, Math.min(10000, rate));
                DropEntry e = new DropEntry();
                e.itemId = itemId;
                e.countMin = countMin;
                e.countMax = countMax;
                e.ratePerTenThousand = rate;
                list.add(e);
            } catch (NumberFormatException ignored) {
                // skip invalid segment
            }
        }
        return list;
    }

    private List<DropItemDto> roll(List<DropEntry> entries) {
        List<DropItemDto> result = new ArrayList<>();
        for (DropEntry e : entries) {
            int roll = ThreadLocalRandom.current().nextInt(1, 10001);
            if (roll > e.ratePerTenThousand) continue;
            int count = ThreadLocalRandom.current().nextInt(e.countMin, e.countMax + 1);
            if (count > 0) result.add(new DropItemDto(e.itemId, count));
        }
        return result;
    }
}
