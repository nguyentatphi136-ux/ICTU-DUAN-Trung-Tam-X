package com.ems.test;

import com.ems.dao.RoomDAO;
import com.ems.model.Room;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

/**
 * In-memory Mock RoomDAO phục vụ kiểm thử nhanh và độc lập
 */
public class MockRoomDAO extends RoomDAO {
    private final Map<Long, Room> db = new LinkedHashMap<>();
    private final AtomicLong seq = new AtomicLong(1);
    private final Map<Long, Boolean> referencedRooms = new LinkedHashMap<>();
    private final Map<Long, List<Map<String, Object>>> violationsByRoom = new LinkedHashMap<>();

    public void markReferenced(long roomId, boolean referenced) {
        referencedRooms.put(roomId, referenced);
    }

    public void addAssignedClassViolation(long roomId, int studentCount, String className, String classCode) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("classId", 101L);
        map.put("classCode", classCode);
        map.put("className", className);
        map.put("studentCount", studentCount);
        violationsByRoom.computeIfAbsent(roomId, k -> new ArrayList<>()).add(map);
    }

    @Override
    public Room findById(long id) {
        Room r = db.get(id);
        return r != null ? cloneRoom(r) : null;
    }

    @Override
    public Room findByCode(String roomCode) {
        return db.values().stream()
                .filter(r -> r.getRoomCode().equalsIgnoreCase(roomCode))
                .findFirst()
                .map(this::cloneRoom)
                .orElse(null);
    }

    @Override
    public boolean existsByCode(String roomCode, Long excludeId) {
        return db.values().stream()
                .anyMatch(r -> r.getRoomCode().equalsIgnoreCase(roomCode)
                        && (excludeId == null || !r.getId().equals(excludeId)));
    }

    @Override
    public Map<String, Object> searchRooms(String keyword, String roomType, Boolean isActive, int page, int pageSize) {
        List<Room> filtered = db.values().stream().filter(r -> {
            if (keyword != null && !keyword.isBlank()) {
                boolean match = r.getRoomCode().toLowerCase().contains(keyword.toLowerCase())
                        || r.getRoomName().toLowerCase().contains(keyword.toLowerCase());
                if (!match) return false;
            }
            if (roomType != null && !roomType.isBlank() && !"ALL".equalsIgnoreCase(roomType)) {
                if (!r.getRoomType().equalsIgnoreCase(roomType)) return false;
            }
            if (isActive != null) {
                if (!r.getIsActive().equals(isActive)) return false;
            }
            return true;
        }).map(this::cloneRoom).collect(Collectors.toList());

        Map<String, Object> res = new LinkedHashMap<>();
        res.put("items", filtered);
        res.put("totalCount", filtered.size());
        res.put("page", page);
        res.put("pageSize", pageSize);
        res.put("totalPages", 1);
        return res;
    }

    @Override
    public Room createRoom(Room room) {
        long id = seq.getAndIncrement();
        room.setId(id);
        db.put(id, cloneRoom(room));
        return cloneRoom(room);
    }

    @Override
    public Room updateRoom(Room room) {
        if (!db.containsKey(room.getId())) return null;
        db.put(room.getId(), cloneRoom(room));
        return cloneRoom(room);
    }

    @Override
    public boolean deleteRoom(long id) {
        return db.remove(id) != null;
    }

    @Override
    public boolean softDeleteRoom(long id) {
        Room r = db.get(id);
        if (r != null) {
            r.setIsActive(false);
            r.setStatus(Room.STATUS_MAINTENANCE);
            return true;
        }
        return false;
    }

    @Override
    public boolean isRoomReferenced(long id) {
        return referencedRooms.getOrDefault(id, false);
    }

    @Override
    public List<Map<String, Object>> getAssignedClassesExceedingCapacity(long roomId, int newCapacity) {
        List<Map<String, Object>> violations = violationsByRoom.getOrDefault(roomId, List.of());
        return violations.stream()
                .filter(v -> (int) v.get("studentCount") > newCapacity)
                .collect(Collectors.toList());
    }

    private Room cloneRoom(Room original) {
        Room c = new Room();
        c.setId(original.getId());
        c.setRoomCode(original.getRoomCode());
        c.setRoomName(original.getRoomName());
        c.setRoomType(original.getRoomType());
        c.setCapacity(original.getCapacity());
        c.setLocation(original.getLocation());
        c.setOnlineMeetingUrl(original.getOnlineMeetingUrl());
        c.setDescription(original.getDescription());
        c.setIsActive(original.getIsActive());
        c.setStatus(original.getStatus());
        c.setCreatedAt(original.getCreatedAt());
        c.setUpdatedAt(original.getUpdatedAt());
        return c;
    }
}
