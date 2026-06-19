package com.subway.ticket.config;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.subway.ticket.domain.Line;
import com.subway.ticket.domain.LineStation;
import com.subway.ticket.domain.Station;
import com.subway.ticket.repository.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@Profile("db")
public class DataInitializer implements CommandLineRunner {
    private final LineMapper lineMapper;
    private final StationMapper stationMapper;
    private final LineStationMapper lineStationMapper;
    private final OrderMapper orderMapper;
    private final TicketMapper ticketMapper;
    private final PaymentMapper paymentMapper;
    private final QrcodeTokenMapper qrcodeTokenMapper;
    private final JdbcTemplate jdbcTemplate;

    public DataInitializer(LineMapper lineMapper, StationMapper stationMapper, LineStationMapper lineStationMapper,
                           OrderMapper orderMapper, TicketMapper ticketMapper, PaymentMapper paymentMapper, 
                           QrcodeTokenMapper qrcodeTokenMapper, JdbcTemplate jdbcTemplate) {
        this.lineMapper = lineMapper;
        this.stationMapper = stationMapper;
        this.lineStationMapper = lineStationMapper;
        this.orderMapper = orderMapper;
        this.ticketMapper = ticketMapper;
        this.paymentMapper = paymentMapper;
        this.qrcodeTokenMapper = qrcodeTokenMapper;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    @Transactional
    public void run(String... args) {
        // Load from classpath
        String jsonFile = "hangzhou_subway.json";
        org.springframework.core.io.Resource resource = new org.springframework.core.io.ClassPathResource(jsonFile);
        if (!resource.exists()) {
            log.warn("JSON file not found in classpath: {}. Skipping import.", jsonFile);
            return;
        }

        try {
            log.info("Starting data import from classpath: {}", jsonFile);
            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(resource.getInputStream());
            // Hangzhou JSON structure: root -> "l" (array of lines)
            JsonNode linesNode = root.path("l");

            if (!linesNode.isArray()) {
                log.error("Invalid JSON format: 'l' field is missing or not an array.");
                return;
            }

            // Clear existing data (Order must be cleared first due to FK)
            log.info("Clearing existing data...");
            ticketMapper.delete(null);
            paymentMapper.delete(null);
            qrcodeTokenMapper.delete(null);
            orderMapper.delete(null);
            lineStationMapper.delete(null);
            stationMapper.delete(null);
            lineMapper.delete(null);

            // Process each Line
            for (JsonNode lineNode : linesNode) {
                // Line Info
                String lineName = lineNode.path("ln").asText(); // "1号线"
                String lineDirection = lineNode.path("la").asText(); // "湘湖-萧山国际机场"
                String lineCode = lineNode.path("ls").asText(); // "330100023133"
                String lineColor = lineNode.path("cl").asText(); // "color"
                if (lineColor == null || lineColor.isEmpty()) {
                    lineColor = "#1a4695"; // Default
                } else if (!lineColor.startsWith("#")) {
                    lineColor = "#" + lineColor;
                }

                // If direction exists, append it to name for better clarity
                String displayName = lineName;
                if (lineDirection != null && !lineDirection.isEmpty()) {
                    displayName = lineName + " (" + lineDirection + ")";
                }

                Line line = new Line();
                line.setName(displayName);
                line.setCode(lineCode);
                line.setColor(lineColor);
                line.setIsActive(1);
                
                lineMapper.insert(line);
                log.info("Importing Line: {} (Code: {})", lineName, lineCode);

                // Stations
                JsonNode stationsNode = lineNode.path("st");
                if (stationsNode.isArray()) {
                    int seq = 1;
                    for (JsonNode stNode : stationsNode) {
                        String stName = stNode.path("n").asText();
                        String stId = stNode.path("sid").asText(); // Unique ID
                        
                        // Check if station already exists by name
                        QueryWrapper<Station> stQuery = new QueryWrapper<>();
                        stQuery.eq("name", stName);
                        // Optional: Also check coordinates distance to ensure it's the same physical station
                        Station station = stationMapper.selectOne(stQuery);
                        
                        if (station == null) {
                            station = new Station();
                            station.setName(stName);
                            station.setLineId(line.getId());
                            station.setCode(stId);
                            station.setIsActive(1);
                            
                            // Parse coordinates
                            String sl = stNode.path("sl").asText();
                            if (sl != null && sl.contains(",")) {
                                String[] parts = sl.split(",");
                                try {
                                    station.setLng(Double.parseDouble(parts[0]));
                                    station.setLat(Double.parseDouble(parts[1]));
                                } catch (Exception ignored) {}
                            }
                            stationMapper.insert(station);
                        }
                        
                        // Link Line-Station
                        LineStation ls = new LineStation();
                        ls.setLineId(line.getId());
                        ls.setStationId(station.getId());
                        ls.setSeq(seq++);
                        lineStationMapper.insert(ls);
                    }
                }
            }

            log.info("Data import completed successfully.");
            log.info("Total Lines: {}", lineMapper.selectCount(null));
            log.info("Total Stations: {}", stationMapper.selectCount(null));
            log.info("Total LineStations: {}", lineStationMapper.selectCount(null));
            
        } catch (Exception e) {
            log.error("Data initialization failed", e);
        }
    }
}
