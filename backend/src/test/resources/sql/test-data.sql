-- Test data: 2 lines with 6 station records (5 distinct stations, 1 transfer station)
-- Line 1: 武林广场 → 凤起路 → 龙翔桥
-- Line 2: 凤起路(transfer) → 中河北路 → 建国北路

INSERT INTO line (id, name, color, code, is_active, created_at, updated_at) VALUES
(1, '1号线', '#FF0000', 'L1', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(2, '2号线', '#0000FF', 'L2', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO station (id, name, line_id, code, lng, lat, is_active, created_at, updated_at) VALUES
(1, '武林广场', 1, 'S001', 120.164, 30.275,  1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(2, '凤起路',   1, 'S002', 120.168, 30.268, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(3, '龙翔桥',   1, 'S003', 120.169, 30.260, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(4, '凤起路',   2, 'S002', 120.168, 30.268, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(5, '中河北路', 2, 'S004', 120.172, 30.270, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(6, '建国北路', 2, 'S005', 120.178, 30.273, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO line_station (id, line_id, station_id, seq) VALUES
(1, 1, 1, 1),   -- L1: 武林广场
(2, 1, 2, 2),   -- L1: 凤起路
(3, 1, 3, 3),   -- L1: 龙翔桥
(4, 2, 4, 1),   -- L2: 凤起路 (transfer)
(5, 2, 5, 2),   -- L2: 中河北路
(6, 2, 6, 3);   -- L2: 建国北路
