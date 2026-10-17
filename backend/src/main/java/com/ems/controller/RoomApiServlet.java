package com.ems.controller;

import com.ems.security.ApiResponse;
import com.ems.service.ClassService;
import com.ems.service.ClassService.RoomInput;
import com.google.gson.JsonObject;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * S3-09 Phòng học vật lý và trực tuyến, quyền CLASS_MANAGE. Xếp phòng cho lớp: PUT /api/classes/{code}/room.
 *
 * <pre>
 * GET    /api/rooms?type=PHYSICAL|ONLINE
 * GET    /api/rooms/{id}
 * GET    /api/rooms/{id}/validate-capacity?students=30
 * POST   /api/rooms          { code, name, type, capacity, location, meetingUrl }
 * PUT    /api/rooms/{id}     như POST
 * DELETE /api/rooms/{id}     chặn khi phòng đang xếp cho lớp chưa kết thúc
 * </pre>
 */
@WebServlet(name = "RoomApiServlet", urlPatterns = {"/api/rooms", "/api/rooms/*"})
public class RoomApiServlet extends JsonApiServlet {
    private static final long serialVersionUID = 1L;
    private static final Pattern ITEM = Pattern.compile("^/(\\d+)$");
    private static final Pattern CHECK = Pattern.compile("^/(\\d+)/validate-capacity$");

    private transient ClassService service = new ClassService();

    @Override
    protected void handle(String method, String path, HttpServletRequest req, HttpServletResponse resp) throws Exception {
        Matcher item = ITEM.matcher(path);
        Matcher check = CHECK.matcher(path);
        if ("GET".equals(method) && path.isEmpty()) {
            ApiResponse.success(resp, "ROOM_LIST_SUCCESS", "Danh sách phòng học.", service.rooms(req.getParameter("type")));
        } else if ("GET".equals(method) && check.matches()) {
            ApiResponse.success(resp, "ROOM_CAPACITY_CHECKED", "Đã kiểm tra sức chứa.",
                    service.checkCapacity(Integer.parseInt(check.group(1)), intParam(req, "students", 0)));
        } else if ("GET".equals(method) && item.matches()) {
            ApiResponse.success(resp, "ROOM_DETAIL_SUCCESS", "Chi tiết phòng học.", service.room(Integer.parseInt(item.group(1))));
        } else if ("POST".equals(method) && path.isEmpty()) {
            ApiResponse.created(resp, "ROOM_CREATED", "Đã thêm phòng học.", service.createRoom(input(body(req))));
        } else if ("PUT".equals(method) && item.matches()) {
            ApiResponse.success(resp, "ROOM_UPDATED", "Đã lưu phòng học.", service.updateRoom(Integer.parseInt(item.group(1)), input(body(req))));
        } else if ("DELETE".equals(method) && item.matches()) {
            service.deleteRoom(Integer.parseInt(item.group(1)));
            ApiResponse.success(resp, "ROOM_DELETED", "Đã xoá phòng học.", null);
        } else {
            notFound(resp);
        }
    }

    private static RoomInput input(JsonObject b) {
        return new RoomInput(str(b, "code"), str(b, "name"), str(b, "type"), ClassApiServlet.integer(b, "capacity"),
                str(b, "location"), str(b, "meetingUrl"));
    }
}
