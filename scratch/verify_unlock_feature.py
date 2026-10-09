import re

with open('frontend/app.js', encoding='utf-8') as f:
    app = f.read()

with open('frontend/admin.html', encoding='utf-8') as f:
    adm = f.read()

print("1. Lock modal in admin.html:", "id=\"lock-account-modal\"" in adm)
print("2. Unlock modal in admin.html:", "id=\"unlock-account-modal\"" in adm)
print("3. Unlock form in admin.html:", "id=\"unlock-account-form\"" in adm)
print("4. Unlock button in admin.html:", "id=\"unlock-confirm-submit-btn\"" in adm)
print("5. data-unlock-account in app.js:", "data-unlock-account" in app)
print("6. Clears temporary 15m lockout in app.js:", "delete lockoutMap[norm]" in app)
print("7. Success toast in app.js:", "Mở khoá tài khoản" in app and "thành công!" in app)
