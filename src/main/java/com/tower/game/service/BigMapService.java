package com.tower.game.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.tower.game.common.dto.bigmap.BigMapVO;
import com.tower.game.common.dto.bigmap.BigMapsResponse;
import com.tower.game.common.dto.bigmap.LayerVO;
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
import java.util.stream.Collectors;

/**
 * 大章节配置服务，查询 bigMaps 并组装为约定 JSON 出参
 */
@Service
@RequiredArgsConstructor
public class BigMapService {

    private final BigMapMapper bigMapMapper;
    private final BigMapLayerMapper bigMapLayerMapper;

    /**
     * 查询全部大章节，出参结构为 { "bigMaps": [ { "id", "name", "layers": [ { "options": [...] } ] } ] }
     */
    public BigMapsResponse listBigMaps() {
        List<BigMap> maps = bigMapMapper.selectList(
                new LambdaQueryWrapper<BigMap>().orderByAsc(BigMap::getId));
        if (maps == null || maps.isEmpty()) {
            return new BigMapsResponse(Collections.emptyList());
        }

        List<Integer> mapIds = maps.stream().map(BigMap::getId).collect(Collectors.toList());
        List<BigMapLayer> layers = bigMapLayerMapper.selectList(
                new LambdaQueryWrapper<BigMapLayer>()
                        .in(BigMapLayer::getBigMapId, mapIds)
                        .orderByAsc(BigMapLayer::getBigMapId, BigMapLayer::getSortOrder));

        Map<Integer, List<BigMapLayer>> layersByMapId = layers.stream()
                .collect(Collectors.groupingBy(BigMapLayer::getBigMapId));

        List<BigMapVO> bigMaps = new ArrayList<>(maps.size());
        for (BigMap map : maps) {
            List<BigMapLayer> mapLayers = layersByMapId.getOrDefault(map.getId(), Collections.emptyList());
            List<LayerVO> layerVOList = new ArrayList<>(mapLayers.size());
            for (BigMapLayer layer : mapLayers) {
                List<Integer> options = parseOptions(layer.getOptions());
                layerVOList.add(new LayerVO(options));
            }
            bigMaps.add(new BigMapVO(map.getId(), map.getName(), layerVOList));
        }
        return new BigMapsResponse(bigMaps);
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
