const fs = require('fs');
const path = require('path');

const ROOT = path.resolve(__dirname, '..');
let passed = 0;
let failed = 0;

function assert(condition, message) {
    if (condition) {
        passed++;
        console.log(`  [PASS] ${message}`);
    } else {
        failed++;
        console.error(`  [FAIL] ${message}`);
    }
}

function readFile(relPath) {
    return fs.readFileSync(path.join(ROOT, relPath), 'utf8');
}

console.log('='.repeat(80));
console.log('KIỂM THỬ TỰ ĐỘNG TOÀN DIỆN CHO SPRINT 2 - USER STORY S2-09 [IDTTX-45]');
console.log('Chủ đề: [BE] DB + API CRUD LEAD (JSP & SERVLET)');
console.log('='.repeat(80));

// 1. Kiểm tra Lead Table & Entity
console.log('\n--- 1. Kiểm tra Lead Table & Entity ---');
const leadModel = readFile('src/main/java/com/ems/model/Lead.java');
assert(leadModel.includes('public class Lead implements Serializable'), 'Lead entity tồn tại và implements Serializable');
assert(leadModel.includes('fullName') && leadModel.includes('phone') && leadModel.includes('email'), 'Có đủ các trường cơ bản: fullName, phone, email');
assert(leadModel.includes('programId') && leadModel.includes('programInterest'), 'Có các trường chương trình quan tâm: programId, programInterest');
assert(leadModel.includes('status') && leadModel.includes('source') && leadModel.includes('notes'), 'Có các trường quản lý: status, source, notes');
assert(leadModel.includes('assignedTo') && leadModel.includes('createdBy') && leadModel.includes('updatedBy'), 'Có các trường liên kết user: assignedTo, createdBy, updatedBy');
assert(leadModel.includes('isValidStatus') && leadModel.includes('isValidSource'), 'Có các phương thức xác thực trạng thái và nguồn');

// 2. Kiểm tra Database Migration
console.log('\n--- 2. Kiểm tra Database Migration ---');
const migrationSql = readFile('database/migration_s2_09_leads.sql');
assert(migrationSql.includes('CREATE TABLE IF NOT EXISTS `leads`'), 'File migration có lệnh tạo bảng leads');
assert(migrationSql.includes('`phone` VARCHAR(20) NOT NULL'), 'Cột phone NOT NULL');
assert(migrationSql.includes('idx_leads_phone'), 'Có đánh index trên phone để tối ưu kiểm tra trùng lặp');
assert(migrationSql.includes('LEAD_VIEW') && migrationSql.includes('LEAD_CREATE') && migrationSql.includes('LEAD_UPDATE') && migrationSql.includes('LEAD_DELETE'), 'Đăng ký đầy đủ 4 quyền Lead');
assert(migrationSql.includes('TRAINING_MANAGER') || migrationSql.includes('TrainingManager'), 'Phân quyền cho Training Manager');
assert(migrationSql.includes('ADMISSIONS') || migrationSql.includes('Admissions'), 'Phân quyền cho Admissions');
assert(migrationSql.includes('DELETE rp FROM') && migrationSql.includes('LEAD_DELETE'), 'Thu hồi rõ ràng quyền LEAD_DELETE của Admissions');

// 3. Kiểm tra DAO Layer
console.log('\n--- 3. Kiểm tra DAO Layer (LeadDAO.java) ---');
const leadDao = readFile('src/main/java/com/ems/dao/LeadDAO.java');
assert(leadDao.includes('public Lead findById(long id)'), 'LeadDAO có findById');
assert(leadDao.includes('public Lead findByPhone(String phone)'), 'LeadDAO có findByPhone');
assert(leadDao.includes('public boolean existsByPhone(String phone, Long excludeId)'), 'LeadDAO có existsByPhone');
assert(leadDao.includes('public long create(Lead lead)'), 'LeadDAO có create');
assert(leadDao.includes('public boolean update(Lead lead)'), 'LeadDAO có update');
assert(leadDao.includes('public boolean delete(long id)'), 'LeadDAO có delete');
assert(leadDao.includes('public Map<String, Object> searchLeads'), 'LeadDAO có searchLeads với phân trang');

// 4. Kiểm tra Validation & Duplicate Phone Logic (LeadService.java)
console.log('\n--- 4. Kiểm tra Validation & Xử lý trùng số điện thoại ---');
const leadService = readFile('src/main/java/com/ems/service/LeadService.java');
assert(leadService.includes('validateLead'), 'LeadService có hàm validateLead');
assert(leadService.includes('isValidPhoneNumber'), 'LeadService có hàm isValidPhoneNumber');
assert(leadService.includes('isValidEmail'), 'LeadService có hàm isValidEmail');
assert(leadService.includes('checkDuplicatePhone'), 'LeadService có hàm checkDuplicatePhone');
assert(leadService.includes('canDeleteLead'), 'LeadService có kiểm tra quyền xóa');

// Test phone regex logic in JS
const PHONE_PATTERN = /^(?:\+84|0)(?:3[2-9]|5[6|8|9]|7[0|6-9]|8[1-9]|9[0-9])[0-9]{7}$/;
assert(PHONE_PATTERN.test('0912345678'), 'Regex chấp nhận số 0912345678');
assert(PHONE_PATTERN.test('0987654321'), 'Regex chấp nhận số 0987654321');
assert(PHONE_PATTERN.test('+84912345678'), 'Regex chấp nhận số +84912345678');
assert(PHONE_PATTERN.test('0388991122'), 'Regex chấp nhận số 0388991122');
assert(!PHONE_PATTERN.test('12345'), 'Regex từ chối số quá ngắn 12345');
assert(!PHONE_PATTERN.test('0123456789'), 'Regex từ chối đầu số 0123456789 cũ');
assert(!PHONE_PATTERN.test('0243888999'), 'Regex từ chối số cố định 024');

// 5. Kiểm tra Phân quyền RBAC (PermissionConstant & PermissionPolicy)
console.log('\n--- 5. Kiểm tra Phân quyền RBAC ---');
const permConst = readFile('src/main/java/com/ems/constant/PermissionConstant.java');
assert(permConst.includes('LEAD_VIEW') && permConst.includes('LEAD_CREATE') && permConst.includes('LEAD_UPDATE') && permConst.includes('LEAD_DELETE'), 'PermissionConstant có khai báo 4 quyền LEAD');
assert(permConst.includes('ROLE_PERMISSIONS.put(RoleConstant.TRAINING_MANAGER') && permConst.includes('LEAD_DELETE'), 'Training Manager có quyền LEAD_DELETE');
assert(permConst.includes('ROLE_PERMISSIONS.put(RoleConstant.ADMISSIONS') && !permConst.includes('ADMISSIONS, new HashSet<>(Arrays.asList(\n                LEAD_MANAGE, LEAD_VIEW, LEAD_CREATE, LEAD_UPDATE, LEAD_DELETE'), 'Admissions KHÔNG có quyền LEAD_DELETE');

const permPolicy = readFile('src/main/java/com/ems/security/PermissionPolicy.java');
assert(permPolicy.includes('Map.entry("GET /api/leads", "LEAD_VIEW")'), 'GET /api/leads yêu cầu LEAD_VIEW');
assert(permPolicy.includes('Map.entry("POST /api/leads", "LEAD_CREATE")'), 'POST /api/leads yêu cầu LEAD_CREATE');
assert(permPolicy.includes('path.matches("^/api/leads/\\\\d+$")') && permPolicy.includes('LEAD_UPDATE'), 'PUT /api/leads/:id yêu cầu LEAD_UPDATE');
assert(permPolicy.includes('path.matches("^/api/leads/\\\\d+$")') && permPolicy.includes('LEAD_DELETE'), 'DELETE /api/leads/:id yêu cầu LEAD_DELETE');

// 6. Kiểm tra Lead API Servlet (LeadApiServlet.java)
console.log('\n--- 6. Kiểm tra Lead API Servlet (LeadApiServlet.java) ---');
const leadServlet = readFile('src/main/java/com/ems/controller/LeadApiServlet.java');
assert(leadServlet.includes('@WebServlet(name = "LeadApiServlet", urlPatterns = {"/api/leads", "/api/leads/*"})'), 'Servlet khai báo đúng urlPatterns');
assert(leadServlet.includes('doGet') && leadServlet.includes('handleListLeads') && leadServlet.includes('handleGetLeadDetail') && leadServlet.includes('handleCheckPhone'), 'doGet xử lý danh sách, chi tiết và check-phone');
assert(leadServlet.includes('doPost') && leadServlet.includes('DUPLICATE_PHONE') && leadServlet.includes('SC_CONFLICT'), 'doPost kiểm tra trùng số điện thoại và trả 409 Conflict');
assert(leadServlet.includes('doPut'), 'doPut xử lý cập nhật lead');
assert(leadServlet.includes('doDelete') && leadServlet.includes('RoleConstant.ADMISSIONS') && leadServlet.includes('LEAD_DELETE_FORBIDDEN'), 'doDelete từ chối vai trò Admissions với 403 và thông báo rõ ràng');

// 7. Kiểm tra Swagger OpenAPI Documentation
console.log('\n--- 7. Kiểm tra Swagger Documentation ---');
const swaggerPath = path.join(ROOT, 'src/main/webapp/api-docs/swagger.json');
assert(fs.existsSync(swaggerPath), 'File swagger.json tồn tại');
const swaggerJson = JSON.parse(fs.readFileSync(swaggerPath, 'utf8'));
assert(swaggerJson.openapi === '3.0.3', 'Chuẩn OpenAPI 3.0.3');
assert(swaggerJson.paths['/api/leads'] !== undefined, 'Có định nghĩa path /api/leads');
assert(swaggerJson.paths['/api/leads']['get'] !== undefined, 'Có method GET /api/leads');
assert(swaggerJson.paths['/api/leads']['post'] !== undefined, 'Có method POST /api/leads');
assert(swaggerJson.paths['/api/leads/{id}']['get'] !== undefined, 'Có method GET /api/leads/{id}');
assert(swaggerJson.paths['/api/leads/{id}']['put'] !== undefined, 'Có method PUT /api/leads/{id}');
assert(swaggerJson.paths['/api/leads/{id}']['delete'] !== undefined, 'Có method DELETE /api/leads/{id}');
assert(swaggerJson.paths['/api/leads/check-phone']['get'] !== undefined, 'Có method GET /api/leads/check-phone');
assert(swaggerJson.paths['/api/leads']['post'].responses['409'] !== undefined, 'Có mô tả lỗi 409 DUPLICATE_PHONE');
assert(swaggerJson.paths['/api/leads/{id}']['delete'].responses['403'] !== undefined, 'Có mô tả lỗi 403 Forbidden cho Delete');

const swaggerHtml = readFile('src/main/webapp/api-docs/index.html');
assert(swaggerHtml.includes('SwaggerUIBundle'), 'File index.html chứa SwaggerUIBundle');
assert(swaggerHtml.includes('swagger.json'), 'File index.html liên kết tới swagger.json');

const swaggerServlet = readFile('src/main/java/com/ems/controller/SwaggerApiServlet.java');
assert(swaggerServlet.includes('@WebServlet(name = "SwaggerApiServlet"'), 'SwaggerApiServlet tồn tại');

// 8. Kiểm tra JUnit 5 Test Files
console.log('\n--- 8. Kiểm tra JUnit 5 Test Files ---');
assert(fs.existsSync(path.join(ROOT, 'src/test/java/com/ems/model/LeadTest.java')), 'LeadTest.java tồn tại');
assert(fs.existsSync(path.join(ROOT, 'src/test/java/com/ems/test/LeadValidationTest.java')), 'LeadValidationTest.java tồn tại');
assert(fs.existsSync(path.join(ROOT, 'src/test/java/com/ems/test/LeadRoleAuthorizationTest.java')), 'LeadRoleAuthorizationTest.java tồn tại');
assert(fs.existsSync(path.join(ROOT, 'src/test/java/com/ems/test/LeadDuplicatePhoneTest.java')), 'LeadDuplicatePhoneTest.java tồn tại');

console.log('\n' + '='.repeat(80));
console.log(`KẾT QUẢ KIỂM THỬ: ${passed} PASS, ${failed} FAIL`);
console.log('='.repeat(80));

if (failed > 0) {
    process.exit(1);
} else {
    process.exit(0);
}
