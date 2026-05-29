package com.tower.game.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.tower.game.common.dto.bigmap.BigMapListItemVO;
import com.tower.game.common.dto.bigmap.BigMapListResponse;
import com.tower.game.common.dto.bigmap.BigMapRunState;
import com.tower.game.common.dto.bigmap.BigMapVO;
import com.tower.game.common.dto.bigmap.LayerVO;
import com.tower.game.common.exception.BusinessException;
import com.tower.game.mapper.BigMapLayerMapper;
import com.tower.game.mapper.BigMapMapper;
import com.tower.game.model.entity.BigMap;
import com.tower.game.model.entity.BigMapLayer;
import com.tower.game.util.JsonUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

/**
 * 大章节：开局生成锁定路线并写入 Redis，供移动与出口进层校验。
 */
@Service
@RequiredArgsConstructor
public class BigMapService {

    private final BigMapMapper bigMapMapper;
    private final BigMapLayerMapper bigMapLayerMapper;
    private final BigMapRunRedisService bigMapRunRedisService;

    /**
     * 查询全部大章节元数据（id、name、层数），不写 Redis、不做每层随机。
     */
    public BigMapListResponse listAllChapters() {
        List<BigMap> maps = bigMapMapper.selectList(
                new LambdaQueryWrapper<BigMap>().orderByAsc(BigMap::getId));
        if (maps == null || maps.isEmpty()) {
            return new BigMapListResponse(Collections.emptyList());
        }
        List<BigMapLayer> allLayers = bigMapLayerMapper.selectList(new LambdaQueryWrapper<>());
        Map<Integer, Long> countByBigMapId = allLayers == null ? Map.of() : allLayers.stream()
                .collect(Collectors.groupingBy(BigMapLayer::getBigMapId, Collectors.counting()));
        List<BigMapListItemVO> items = new ArrayList<>(maps.size());
        for (BigMap m : maps) {
            int n = countByBigMapId.getOrDefault(m.getId(), 0L).intValue();
            items.add(new BigMapListItemVO(m.getId(), m.getName(), n));
        }
        return new BigMapListResponse(items);
    }

    /**
     * 开始指定大章节：每层从 options 随机一张小地图，写入 run，返回本章结构与各层 mapId。
     */
    public BigMapVO startChapter(Long userId, int bigMapId) {
        BigMap chapter = bigMapMapper.selectById(bigMapId);
        if (chapter == null) {
            throw new BusinessException(404, "大章节不存在");
        }
        List<BigMapLayer> mapLayers = bigMapLayerMapper.selectList(
                new LambdaQueryWrapper<BigMapLayer>()
                        .eq(BigMapLayer::getBigMapId, bigMapId)
                        .orderByAsc(BigMapLayer::getSortOrder));
        if (mapLayers == null || mapLayers.isEmpty()) {
            throw new BusinessException(400, "该章节没有层配置");
        }

        List<Integer> layerMapIds = new ArrayList<>(mapLayers.size());
        List<LayerVO> layerVOList = new ArrayList<>(mapLayers.size());
        for (BigMapLayer layer : mapLayers) {
            List<Integer> options = parseOptions(layer.getOptions());
            if (options.isEmpty()) {
                throw new BusinessException(500, "层配置 options 为空 bigMapId=" + bigMapId + " sortOrder=" + layer.getSortOrder());
            }
            int chosen = options.get(ThreadLocalRandom.current().nextInt(options.size()));
            layerMapIds.add(chosen);
            layerVOList.add(new LayerVO(Collections.singletonList(chosen)));
        }

        BigMapRunState run = BigMapRunState.builder()
                .bigMapId(bigMapId)
                .layerIndex(0)
                .currentMapId(layerMapIds.get(0))
                .cellX(null)
                .cellY(null)
                .layerMapIds(layerMapIds)
                .build();
        bigMapRunRedisService.startNewRun(userId, run);

        return new BigMapVO(chapter.getId(), chapter.getName(), layerVOList);
    }

    private List<Integer> parseOptions(String optionsJson) {
        if (optionsJson == null || optionsJson.isBlank()) {
            return Collections.emptyList();
        }
        try {
            List<Integer> list = JsonUtil.parseObject(optionsJson, new TypeReference<List<Integer>>() {});
            return list != null ? list : Collections.emptyList();
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }
}
