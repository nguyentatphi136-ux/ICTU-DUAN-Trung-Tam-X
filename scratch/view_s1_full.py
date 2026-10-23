import json
import sys
if hasattr(sys.stdout, "reconfigure"):
    sys.stdout.reconfigure(encoding="utf-8")

with open(r'C:\Users\ASUS\.gemini\antigravity-ide\brain\363244ab-5440-419c-ac42-8cfffff48249\scratch\backlog_data.json', 'r', encoding='utf-8') as f:
    data = json.load(f)

for st in data['sprints'][0]['stories']:
    print(f"=== {st['id']}: {st['story']} ===")
    print(f"Priority: {st['priority']} | Points: {st['point']} | Status: {st.get('status')} | Notes: {st.get('notes')}")
    print(f"Acceptance Criteria (ac):\n{st.get('ac')}")
    print("-" * 50)
