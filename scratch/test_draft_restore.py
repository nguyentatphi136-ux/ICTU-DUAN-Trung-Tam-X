import os

app_js = open("frontend/app.js", "r", encoding="utf-8").read()

assert "FORM_DRAFTS_KEY = \"edumanager-form-drafts\"" in app_js, "FORM_DRAFTS_KEY missing"
assert "saveFormDrafts" in app_js, "saveFormDrafts missing"
assert "restoreFormDrafts" in app_js, "restoreFormDrafts missing"
assert "RETURN_URL_KEY" in app_js, "RETURN_URL_KEY missing"
assert "saveFormDrafts()" in app_js, "saveFormDrafts not called in expireSession"
assert "restoreFormDrafts()" in app_js, "restoreFormDrafts not called"
assert "clearContainerDrafts" in app_js, "clearContainerDrafts missing"

print("All draft preservation code checks PASS!")
