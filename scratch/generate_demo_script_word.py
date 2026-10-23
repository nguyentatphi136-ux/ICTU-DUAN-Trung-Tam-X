import os
import sys

if hasattr(sys.stdout, "reconfigure"):
    sys.stdout.reconfigure(encoding="utf-8")

from docx import Document
from docx.shared import Inches, Pt, RGBColor
from docx.enum.text import WD_ALIGN_PARAGRAPH
from docx.enum.table import WD_TABLE_ALIGNMENT, WD_ALIGN_VERTICAL
from docx.oxml import OxmlElement, parse_xml
from docx.oxml.ns import nsdecls, qn

WORKSPACE = os.path.abspath(os.path.join(os.path.dirname(__file__), ".."))

def set_cell_background(cell, fill_hex):
    tcPr = cell._tc.get_or_add_tcPr()
    tcPr.append(parse_xml(f'<w:shd {nsdecls("w")} w:fill="{fill_hex}"/>'))

def set_cell_margins(cell, top=120, bottom=120, left=150, right=150):
    tcPr = cell._tc.get_or_add_tcPr()
    tcMar = parse_xml(f'<w:tcMar {nsdecls("w")}><w:top w:w="{top}" w:type="dxa"/><w:bottom w:w="{bottom}" w:type="dxa"/><w:left w:w="{left}" w:type="dxa"/><w:right w:w="{right}" w:type="dxa"/></w:tcMar>')
    tcPr.append(tcMar)

def create_demo_script_docx():
    doc = Document()

    # Page Margins
    for section in doc.sections:
        section.top_margin = Inches(0.8)
        section.bottom_margin = Inches(0.8)
        section.left_margin = Inches(0.8)
        section.right_margin = Inches(0.8)

    # Styles
    normal_style = doc.styles['Normal']
    normal_style.font.name = 'Segoe UI'
    normal_style.font.size = Pt(10.5)
    normal_style.font.color.rgb = RGBColor(15, 23, 42) # Slate 900

    # Document Header Title
    title = doc.add_paragraph()
    title.alignment = WD_ALIGN_PARAGRAPH.CENTER
    title_run = title.add_run("KỊCH BẢN DEMO TOÀN DIỆN HỆ THỐNG QUẢN LÝ ĐÀO TẠO (EMS / TMS)")
    title_run.font.size = Pt(18)
    title_run.font.bold = True
    title_run.font.color.rgb = RGBColor(2, 132, 199) # Sky 600

    sub = doc.add_paragraph()
    sub.alignment = WD_ALIGN_PARAGRAPH.CENTER
    sub_run = sub.add_run("Kịch bản thuyết trình đầy đủ 8 Tác nhân (Actors) theo chuỗi vòng đời học viên khép kín")
    sub_run.font.size = Pt(11)
    sub_run.font.italic = True
    sub_run.font.color.rgb = RGBColor(100, 116, 139) # Slate 500

    # Meta Info Box
    p_meta = doc.add_paragraph()
    p_meta.paragraph_format.space_before = Pt(6)
    p_meta.paragraph_format.space_after = Pt(12)
    run_meta = p_meta.add_run(
        "• Đường dẫn ứng dụng web (Local): http://localhost:5173\n"
        "• Mật khẩu mặc định dùng chung cho tất cả tài khoản: 123456\n"
        "• Kiến trúc: React + TypeScript (Frontend) kết hợp Java Servlet RESTful API (Backend) theo mô hình kép Dual-Path\n"
        "• Thời lượng demo dự kiến: 12 – 18 phút (Chia theo 8 Actors liên hoàn + Q&A phản biện)"
    )
    run_meta.font.bold = True
    run_meta.font.color.rgb = RGBColor(30, 58, 138)

    # =========================================================================
    # BẢNG TỔNG HỢP 8 ACTORS
    # =========================================================================
    h1 = doc.add_heading("1. BẢNG DANH MỤC 8 TÁC NHÂN (ACTORS) VÀ TÀI KHOẢN DEMO", level=1)
    h1.paragraph_format.space_before = Pt(12)
    h1.paragraph_format.space_after = Pt(6)
    for run in h1.runs:
        run.font.color.rgb = RGBColor(2, 132, 199)
        run.font.size = Pt(13)

    table = doc.add_table(rows=1, cols=4)
    table.alignment = WD_TABLE_ALIGNMENT.CENTER
    table.autofit = False

    headers = ["Actor / Vai trò", "Tài khoản đăng nhập", "Mật khẩu", "Phạm vi chức năng Demo"]
    col_widths = [Inches(1.6), Inches(1.8), Inches(0.9), Inches(2.6)]

    hdr_cells = table.rows[0].cells
    for i, title_text in enumerate(headers):
        hdr_cells[i].text = title_text
        hdr_cells[i].paragraphs[0].runs[0].font.bold = True
        hdr_cells[i].paragraphs[0].runs[0].font.color.rgb = RGBColor(255, 255, 255)
        set_cell_background(hdr_cells[i], "0284C7")
        set_cell_margins(hdr_cells[i], top=120, bottom=120, left=120, right=120)

    actors_table = [
        ("1. Khách vãng lai (Guest)", "Không cần đăng nhập\n(Form công khai)", "–", "Truy cập biểu mẫu tuyển sinh công khai, gửi đăng ký tư vấn khoá học, tự động sinh Lead."),
        ("2. Tư vấn tuyển sinh (Admissions)", "tuvan@tms.vn", "123456", "Phễu Lead Kanban, lọc nguồn, phân công, ghi nhật ký chăm sóc và chuyển đổi Lead thành Học viên."),
        ("3. Quản lý đào tạo (Training Manager)", "daotao@tms.vn", "123456", "Chương trình đào tạo, môn học, mở lớp, ghi danh, xếp phòng, sinh TKB tự động chống trùng lịch."),
        ("4. Kế toán (Accountant)", "ketoan@tms.vn", "123456", "Biểu phí, công nợ, thu tiền từng đợt, chặn thu vượt nợ (AC3), sinh mã biên lai BL-..., in/tải PDF (AC4), lập bút toán điều chỉnh ĐC-... (AC5)."),
        ("5. Giảng viên (Instructor)", "giangvien@tms.vn", "123456", "Lịch dạy, điểm danh buổi học 1 chạm (S4-07), theo dõi bảng chuyên cần (S5-01), tạo bài tập, chấm điểm rubric, tải học liệu, thông báo lớp."),
        ("6. Trợ giảng (Teaching Assistant)", "trogiang@tms.vn", "123456", "Hỗ trợ điểm danh lớp, theo dõi tiến độ nộp bài (S5-10), duyệt đơn xin phép vắng (S4-08), giải đáp học viên."),
        ("7. Học viên (Student)", "hocvien@tms.vn\n(hoặc kiet.dt@gmail.com)", "123456", "Lịch học cá nhân, xin phép vắng, xem tình trạng học phí cá nhân và tải lại biên lai (S7-07), nộp bài tập, xem điểm & xuất PDF bảng điểm toàn khoá (S8-03)."),
        ("8. Quản trị hệ thống (Admin)", "admin@tms.vn", "123456", "Ma trận phân quyền Least Privilege (S1-05), quản trị tài khoản, khoá/mở khoá có lý do & thu hồi phiên (S1-10), nhập Excel (S2-01), nhật ký kiểm toán (S6-11).")
    ]

    for role, acc, pwd, scope in actors_table:
        row_cells = table.add_row().cells
        row_cells[0].text = role
        row_cells[0].paragraphs[0].runs[0].font.bold = True
        row_cells[1].text = acc
        row_cells[2].text = pwd
        row_cells[3].text = scope
        for i, cell in enumerate(row_cells):
            set_cell_margins(cell, top=90, bottom=90, left=100, right=100)
            if i == 2:
                cell.paragraphs[0].alignment = WD_ALIGN_PARAGRAPH.CENTER

    for row in table.rows:
        for idx, width in enumerate(col_widths):
            row.cells[idx].width = width

    # =========================================================================
    # KỊCH BẢN CHI TIẾT 8 ACTORS THEO CHUỖI VÒNG ĐỜI
    # =========================================================================
    h2 = doc.add_heading("2. KỊCH BẢN CHI TIẾT THEO CHUỖI VÒNG ĐỜI CỦA 8 TÁC NHÂN", level=1)
    h2.paragraph_format.space_before = Pt(18)
    for run in h2.runs:
        run.font.color.rgb = RGBColor(2, 132, 199)
        run.font.size = Pt(13)

    # --- ACTOR 1: GUEST ---
    doc.add_heading("Actor 1: Khách vãng lai (Guest) — Khởi đầu từ phễu tuyển sinh (1 phút)", level=2)
    p = doc.add_paragraph()
    p.add_run("• Tình huống: ").bold = True
    p.add_run("Người dùng quan tâm khoá học truy cập trang web của trung tâm để đăng ký tư vấn.\n")
    p.add_run("• Đường dẫn: ").bold = True
    p.add_run("http://localhost:5173/dang-ky-tu-van (Story S2-08)\n")
    p.add_run("• Thao tác demo:\n").bold = True
    p.add_run("  1. Mở biểu mẫu 'Đăng ký tư vấn khoá học'.\n")
    p.add_run("  2. Điền họ tên: 'DO TUAN KIET', SĐT: '0971 846 320', Email: 'kiet.dt@gmail.com', chọn chương trình 'Lập trình web Full-stack'.\n")
    p.add_run("  3. Bấm 'Gửi đăng ký'. Hệ thống thông báo tiếp nhận thành công và tự động tạo Lead mã #24 vào phễu tuyển sinh nội bộ mà không cần can thiệp thủ công.\n")

    # --- ACTOR 2: ADMISSIONS ---
    doc.add_heading("Actor 2: Tư vấn tuyển sinh (Admissions) — Chăm sóc & Chuyển đổi Lead (2 phút)", level=2)
    p = doc.add_paragraph()
    p.add_run("• Tài khoản đăng nhập: ").bold = True
    p.add_run("tuvan@tms.vn / 123456\n")
    p.add_run("• Đường dẫn trọng tâm: ").bold = True
    p.add_run("http://localhost:5173/tuyen-sinh/pheu (Story S3-01, S3-02, S3-03)\n")
    p.add_run("• Thao tác demo:\n").bold = True
    p.add_run("  1. Vào menu Tuyển sinh -> Phễu Lead: Hiển thị bảng Kanban các cột: Mới -> Đang liên hệ -> Hẹn tư vấn -> Đã tư vấn -> Chốt / Từ chối.\n")
    p.add_run("  2. Bấm vào Lead DO TUAN KIET: Mở ngăn chi tiết, ghi nhật ký chăm sóc: 'Học viên có nguyện vọng học lớp tối, tư vấn lộ trình Full-stack 6 tháng'.\n")
    p.add_run("  3. Thao tác 'Chốt Lead': Bấm nút 'Chuyển thành học viên' (S3-03). Hệ thống tạo hồ sơ học viên chính thức với mã HV260142, chuẩn bị cho khâu xếp lớp và học phí.\n")

    # --- ACTOR 3: TRAINING MANAGER ---
    doc.add_heading("Actor 3: Quản lý đào tạo (Training Manager) — Tổ chức lớp & Lịch học (2 - 3 phút)", level=2)
    p = doc.add_paragraph()
    p.add_run("• Tài khoản đăng nhập: ").bold = True
    p.add_run("daotao@tms.vn / 123456\n")
    p.add_run("• Đường dẫn trọng tâm: ").bold = True
    p.add_run("http://localhost:5173/dao-tao/chuong-trinh và http://localhost:5173/lop-hoc (S2-04, S3-05, S3-06, S4-01)\n")
    p.add_run("• Thao tác demo:\n").bold = True
    p.add_run("  1. Xem Chương trình đào tạo & Môn học (S2-04, S2-05): Giới thiệu khung chương trình chuẩn hoá gồm 6 môn học và cấu trúc buổi.\n")
    p.add_run("  2. Quản lý lớp học (S3-05): Mở lớp mới 'WEB-K16'.\n")
    p.add_run("  3. Ghi danh học viên (S3-06): Chọn học viên DO TUAN KIET (HV260142) vào danh sách lớp WEB-K16.\n")
    p.add_run("  4. Phân công & Xếp phòng (S3-08, S3-09): Phân công Giảng viên Vũ Đức Long, Trợ giảng Phạm Gia Huy, phòng học LAB-02 (cảnh báo nếu quá sức chứa phòng).\n")
    p.add_run("  5. Sinh thời khoá biểu tự động (S4-01, S4-02): Chọn lịch học thứ 2 - 4 - 6 lúc 18h30. Bấm 'Xem trước & Sinh lịch' -> Hệ thống tự động phân bổ toàn bộ các buổi học và kiểm tra chống trùng lịch giáo viên/phòng học.\n")

    # --- ACTOR 4: ACCOUNTANT ---
    doc.add_heading("Actor 4: Kế toán (Accountant) — Công nợ, Thu tiền & Bút toán điều chỉnh (S7-03, S7-05) (3 - 4 phút)", level=2)
    p = doc.add_paragraph()
    p.add_run("• Ý nghĩa: ").bold = True
    p.add_run("Đây là chức năng nghiệp vụ trọng điểm vừa được kiểm thử tự động 100% đạt 5/5 Acceptance Criteria.\n")
    p.add_run("• Tài khoản đăng nhập: ").bold = True
    p.add_run("ketoan@tms.vn / 123456\n")
    p.add_run("• Đường dẫn trọng tâm: ").bold = True
    p.add_run("http://localhost:5173/hoc-phi/cong-no và http://localhost:5173/hoc-phi/cong-no/HV260142 (S7-03, S7-05)\n")
    p.add_run("• Thao tác demo:\n").bold = True
    p.add_run("  1. Mở Bảng công nợ: Trình bày các thẻ thống kê tổng hợp: Tổng phải thu, Đã thu, Còn nợ, Quá hạn.\n")
    p.add_run("  2. Vào chi tiết học viên DO TUAN KIET (HV260142): Kế hoạch chia 3 đợt: Đợt 1 (Đã thu đủ 3,420,000 ₫), Đợt 2 (Phải đóng 2,565,000 ₫, đã đóng 1,000,000 ₫, còn nợ 1,565,000 ₫).\n")
    p.add_run("  3. Demo AC3 (Chặn thu vượt nợ): Bấm 'Ghi nhận thanh toán' cho Đợt 2, cố tình gõ số tiền 2,000,000 ₫ -> Form lập tức cảnh báo vượt số nợ và khoá nút ghi nhận.\n")
    p.add_run("  4. Demo AC1 & AC2 (Thu một phần & Mã GD ngân hàng): Bấm 'Thu đủ 1,565,000 ₫' (hoặc gõ số tiền lẻ), chọn hình thức 'Chuyển khoản'. Thử bấm ghi nhận khi chưa nhập mã GD -> Form bắt buộc nhập mã GD. Nhập mã GD 'VCB.FT20261023' và bấm 'Ghi nhận và in biên lai'.\n")
    p.add_run("  5. Demo AC4 (Sinh biên lai duy nhất & In/Tải PDF): Cửa sổ biên lai mở ra với mã duy nhất BL-2026-XXXXXX, có số tiền viết bằng chữ ('Một triệu năm trăm sáu mươi lăm nghìn đồng chẵn'), tên trung tâm, người thu. Bấm nút 'In hoặc lưu PDF' (chuẩn A5) hoặc 'Mẫu in máy chủ (PDF)'.\n")
    p.add_run("  6. Demo AC5 (Bút toán điều chỉnh - Không xoá): Nhấn mạnh khoản đã thu không được phép xoá bằng HTTP DELETE. Bấm 'Lập bút toán điều chỉnh', chọn loại 'Giảm khoản thu', số tiền 100,000 ₫ kèm lý do bắt buộc 'Áp dụng voucher sau'. Hệ thống sinh mã ĐC-2026-XXXXXX, cập nhật số tiền thực thu (net amount) và ghi vào audit log.\n")

    # --- ACTOR 5: INSTRUCTOR ---
    doc.add_heading("Actor 5: Giảng viên (Instructor) — Lịch dạy, Điểm danh & Chấm bài (2 - 3 phút)", level=2)
    p = doc.add_paragraph()
    p.add_run("• Tài khoản đăng nhập: ").bold = True
    p.add_run("giangvien@tms.vn / 123456\n")
    p.add_run("• Đường dẫn trọng tâm: ").bold = True
    p.add_run("http://localhost:5173/diem-danh và http://localhost:5173/chuyen-can/WEB-K15 (S4-07, S5-01, S6-01)\n")
    p.add_run("• Thao tác demo:\n").bold = True
    p.add_run("  1. Điểm danh buổi học (S4-07): Giao diện tối ưu di động. Bấm 'Tất cả có mặt' (1 chạm hoàn thành), sau đó chuyển nhanh 1 học viên sang 'Đi muộn' hoặc 'Vắng có phép'. Bấm 'Lưu điểm danh'.\n")
    p.add_run("  2. Bảng chuyên cần lớp (S5-01): Xem ma trận hiện diện cả khoá, nhận diện học viên chạm ngưỡng vắng nguy cơ (S5-02).\n")
    p.add_run("  3. Chấm bài tập theo Rubric (S6-01, S6-02): Mở bài tập cuối môn của học viên, chấm điểm theo các tiêu chí Rubric định sẵn, để lại nhận xét và công bố điểm.\n")
    p.add_run("  4. Đăng tải học liệu (S6-05) & Gửi thông báo lớp (S6-07): Gửi tin tức nhắc nhở lớp học chuẩn bị cho buổi bảo vệ đồ án.\n")

    # --- ACTOR 6: TEACHING ASSISTANT ---
    doc.add_heading("Actor 6: Trợ giảng (Teaching Assistant) — Hỗ trợ vận hành lớp học (1 - 2 phút)", level=2)
    p = doc.add_paragraph()
    p.add_run("• Tài khoản đăng nhập: ").bold = True
    p.add_run("trogiang@tms.vn / 123456\n")
    p.add_run("• Đường dẫn trọng tâm: ").bold = True
    p.add_run("http://localhost:5173/duyet-phep và http://localhost:5173/bai-tap/6 (S4-08, S5-10)\n")
    p.add_run("• Thao tác demo:\n").bold = True
    p.add_run("  1. Duyệt đơn xin phép vắng (S4-08): Mở danh sách đơn xin nghỉ của học viên, xem lý do và minh chứng (giấy khám/lý do cá nhân), bấm 'Duyệt đơn' -> Buổi học tự động ghi nhận trạng thái 'Vắng có phép'.\n")
    p.add_run("  2. Kiểm tra tình trạng nộp bài (S5-10): Xem danh sách học viên đã nộp/chưa nộp bài tập số 6, gửi lời nhắc tới những học viên chưa nộp bài.\n")

    # --- ACTOR 7: STUDENT ---
    doc.add_heading("Actor 7: Học viên (Student) — Học tập, Học phí cá nhân & Bảng điểm tốt nghiệp (S7-07, S8-03) (3 phút)", level=2)
    p = doc.add_paragraph()
    p.add_run("• Tài khoản đăng nhập: ").bold = True
    p.add_run("hocvien@tms.vn / 123456\n")
    p.add_run("• Đường dẫn trọng tâm: ").bold = True
    p.add_run("http://localhost:5173/hoc-phi-cua-toi và http://localhost:5173/bang-diem/HV260101 (S7-07, S8-03)\n")
    p.add_run("• Thao tác demo:\n").bold = True
    p.add_run("  1. Học phí của tôi (S7-07 AC1): Hiển thị đầy đủ tiến độ đóng tiền: Đợt 1 (Đã đóng), Đợt 2 (Thanh toán một phần), Đợt 3 (Chưa thanh toán). Cung cấp hướng dẫn chuyển khoản kèm cú pháp tự sinh chính xác.\n")
    p.add_run("  2. Tải lại biên lai các khoản đã đóng (S7-07 AC2): Bấm nút 'Xem' để mở biên lai điện tử; bấm 'Tải PDF' để tải trực tiếp bản in PDF chính thức từ máy chủ.\n")
    p.add_run("  3. Thử nghiệm bảo mật (Least Privilege): Thử truy cập biên lai của học viên khác bằng cách đổi URL -> Máy chủ lập tức chặn với mã lỗi HTTP 403 Forbidden: 'Học viên chỉ có quyền xem và tải lại biên lai của chính mình'.\n")
    p.add_run("  4. Nộp bài tập & Xem điểm (S5-06, S5-07, S6-04): Nộp link GitHub bài tập, xem điểm số và lời nhận xét chi tiết của giảng viên.\n")
    p.add_run("  5. Bảng điểm toàn khoá & Xuất PDF (S8-03): Mở trang bảng điểm cá nhân, xem kết quả 6 môn học, GPA toàn khoá theo trọng số (8.32/10 - Đạt loại Giỏi), bấm 'In / Xuất PDF' để tải bảng điểm hoàn thành khoá học chính thức.\n")

    # --- ACTOR 8: ADMIN ---
    doc.add_heading("Actor 8: Quản trị viên (Admin) — Ma trận phân quyền & An toàn hệ thống (2 phút)", level=2)
    p = doc.add_paragraph()
    p.add_run("• Tài khoản đăng nhập: ").bold = True
    p.add_run("admin@tms.vn / 123456\n")
    p.add_run("• Đường dẫn trọng tâm: ").bold = True
    p.add_run("http://localhost:5173/quan-tri/phan-quyen và http://localhost:5173/quan-tri/tai-khoan (S1-05, S1-08, S1-10, S6-11)\n")
    p.add_run("• Thao tác demo:\n").bold = True
    p.add_run("  1. Ma trận phân quyền (S1-05): Trình diễn bảng phân quyền chi tiết cho 8 vai trò trên toàn bộ API (GET/POST/PUT/DELETE).\n")
    p.add_run("  2. Quản lý tài khoản (S1-08, S1-10): Tìm kiếm người dùng, thử nghiệm tính năng 'Khoá tài khoản' kèm bắt buộc nhập lý do và lập tức thu hồi toàn bộ phiên đăng nhập (Session Revocation).\n")
    p.add_run("  3. Nhập người dùng từ Excel (S2-01): Xem trước dữ liệu, kiểm tra lỗi định dạng và import hàng loạt tài khoản học viên.\n")
    p.add_run("  4. Nhật ký kiểm toán hệ thống (S6-11): Kiểm tra lịch sử thao tác, chứng minh mọi can thiệp vào điểm số, học phí, gán quyền đều được ghi vết thời gian thực.\n")
    p.add_run("  5. Trang Mục lục Backlog (http://localhost:5173/muc-luc): Trình chiếu danh mục hơn 60 User Stories đã được hiện thực hoá hoàn chỉnh.\n")

    # =========================================================================
    # BÍ QUYẾT TRẢ LỜI CÂU HỎI PHẢN BIỆN (Q&A CHEATSHEET)
    # =========================================================================
    h3 = doc.add_heading("3. BÍ QUYẾT TRẢ LỜI CÂU HỎI PHẢN BIỆN (Q&A CHEATSHEET)", level=1)
    h3.paragraph_format.space_before = Pt(18)
    for run in h3.runs:
        run.font.color.rgb = RGBColor(2, 132, 199)
        run.font.size = Pt(13)

    qa_list = [
        ("Q1: Tại sao hệ thống có tới 8 vai trò (Actors)? Phân chia như vậy có bị dư thừa không?",
         "A1: Trong các trung tâm đào tạo chuyên nghiệp, sự phân công trách nhiệm phải tách biệt để chống thất thoát và gian lận (Separation of Concerns). Tuyển sinh chỉ nắm Lead, Kế toán nắm tài chính nhưng không sửa được điểm, Giảng viên chấm điểm nhưng không can thiệp được học phí, Quản lý đào tạo điều phối lịch và phòng học, Trợ giảng hỗ trợ lớp, Học viên tự quản lý tiến độ, và Admin quản trị kỹ thuật. Mô hình 8 vai trò đáp ứng chuẩn vận hành thực tế của các đơn vị giáo dục."),
        ("Q2: Cơ chế bảo mật Least Privilege được kiểm soát ở Frontend hay Backend?",
         "A2: Bảo mật được thực thi đa tầng, trong đó Backend là chốt chặn quyết định (Zero Trust). Dù người dùng có cố tình sửa mã nguồn frontend, đổi URL hay dùng Postman/Curl gọi API trái phép, Backend PermissionPolicy và Servlet Filters sẽ kiểm tra Role và Identity trong phiên để từ chối ngay lập tức với HTTP 403 Forbidden hoặc HTTP 401 Unauthorized."),
        ("Q3: Làm sao đảm bảo không bị thất thoát tiền hoặc ghi lệch số sách trong module Kế toán (S7-03)?",
         "A3: Hệ thống áp dụng 3 quy tắc bất biến kế toán: (1) Overpayment Guard: không cho phép thu vượt số nợ của đợt; (2) Transaction Integrity: thanh toán chuyển khoản bắt buộc phải có mã giao dịch ngân hàng để đối soát; (3) Immutable Ledger: chặn hoàn toàn phương thức xoá (DELETE), nếu có sai sót bắt buộc phải lập Bút toán điều chỉnh (Adjustment Entry) có mã ĐC-..., ghi rõ lý do và vết kiểm toán."),
        ("Q4: Hệ thống xử lý thế nào khi mất kết nối Cơ sở dữ liệu hoặc cần demo độc lập?",
         "A4: Backend được thiết kế theo cơ chế Dual-Layer: Ưu tiên ghi vào MySQL thông qua JDBC/Connection Pool; khi môi trường không có CSDL, hệ thống kích hoạt Memory Cache Fallback an toàn (ConcurrentHashMap) và Frontend hỗ trợ Local Store Persistence, giúp hệ thống luôn hoạt động ổn định và có thể trình diễn ở bất kỳ điều kiện mạng nào.")
    ]

    for q, a in qa_list:
        p_q = doc.add_paragraph()
        p_q.paragraph_format.space_before = Pt(8)
        p_q.paragraph_format.space_after = Pt(2)
        r_q = p_q.add_run(q)
        r_q.font.bold = True
        r_q.font.color.rgb = RGBColor(185, 28, 28) # Red 700

        p_a = doc.add_paragraph()
        p_a.paragraph_format.space_after = Pt(8)
        r_a = p_a.add_run(a)
        r_a.font.color.rgb = RGBColor(30, 41, 59)

    # Footer note
    p_end = doc.add_paragraph()
    p_end.alignment = WD_ALIGN_PARAGRAPH.CENTER
    p_end.paragraph_format.space_before = Pt(20)
    r_end = p_end.add_run("--- HẾT KỊCH BẢN DEMO 8 ACTORS ---")
    r_end.font.bold = True
    r_end.font.color.rgb = RGBColor(148, 163, 184)

    output_path = os.path.join(WORKSPACE, "KICH_BAN_DEMO_EMS_TMS.docx")
    doc.save(output_path)
    print(f"File Word đã được tạo thành công tại: {output_path}")

if __name__ == "__main__":
    create_demo_script_docx()
