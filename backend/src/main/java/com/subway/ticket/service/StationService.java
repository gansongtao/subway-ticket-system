package com.subway.ticket.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.subway.ticket.domain.Line;
import com.subway.ticket.domain.Station;
import com.subway.ticket.repository.LineMapper;
import com.subway.ticket.repository.StationMapper;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@Service
public class StationService {

    private final StationMapper stationMapper;
    private final LineMapper lineMapper;

    public StationService(StationMapper stationMapper, LineMapper lineMapper) {
        this.stationMapper = stationMapper;
        this.lineMapper = lineMapper;
    }

    public List<Station> getStationsByLine(Long lineId) {
        List<Station> stations = stationMapper.selectList(new QueryWrapper<Station>()
                .eq("line_id", lineId)
                .eq("is_active", 1)
                .orderByAsc("id"));
        populateLineNames(stations);
        return stations;
    }

    public List<Station> getAllStations() {
        List<Station> stations = stationMapper.selectList(new QueryWrapper<Station>()
                .eq("is_active", 1)
                .orderByAsc("id"));
        populateLineNames(stations);
        return stations;
    }

    public List<Station> searchStations(String keyword) {
        QueryWrapper<Station> query = new QueryWrapper<>();
        query.eq("is_active", 1);

        if (keyword != null && !keyword.trim().isEmpty()) {
            query.like("name", keyword);
            // Limit only when searching to avoid huge result if user spams search
            query.last("LIMIT 50");
        }

        List<Station> list = stationMapper.selectList(query);
        populateLineNames(list);

        // Deduplicate by name (since transfer stations have multiple records)
        return list.stream()
                .filter(distinctByKey(Station::getName))
                .collect(Collectors.toList());
    }

    private void populateLineNames(List<Station> stations) {
        if (stations == null || stations.isEmpty()) return;

        // Get all unique line IDs
        List<Long> lineIds = stations.stream()
                .map(Station::getLineId)
                .distinct()
                .collect(Collectors.toList());

        // Fetch lines and create maps
        List<Line> lines = lineMapper.selectByIds(lineIds);
        Map<Long, String> lineNameMap = lines.stream()
                .collect(Collectors.toMap(Line::getId, Line::getName));
        Map<Long, String> lineColorMap = lines.stream()
                .collect(Collectors.toMap(Line::getId, line -> line.getColor() != null ? line.getColor() : "", (existing, replacement) -> existing));

        // Populate line names and colors
        stations.forEach(s -> {
            s.setLineName(lineNameMap.get(s.getLineId()));
            String color = lineColorMap.get(s.getLineId());
            s.setLineColor(ensureHashPrefix(color));
        });
    }

    private String ensureHashPrefix(String color) {
        if (color == null || color.isEmpty()) return "#999";
        if (color.startsWith("#")) return color;
        if (color.matches("^[0-9a-fA-F]{3,6}$")) {
            return "#" + color;
        }
        return color;
    }

    public String getStationNameById(Long id) {
        Station s = stationMapper.selectById(id);
        return s != null ? s.getName() : "Unknown";
    }

    private static <T> Predicate<T> distinctByKey(Function<? super T, ?> keyExtractor) {
        Map<Object, Boolean> seen = new ConcurrentHashMap<>();
        return t -> seen.putIfAbsent(keyExtractor.apply(t), Boolean.TRUE) == null;
    }
}
