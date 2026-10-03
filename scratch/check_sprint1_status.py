import json
import sys
if hasattr(sys.stdout, "reconfigure"):
    sys.stdout.reconfigure(encoding="utf-8")

with open(r'C:\Users\ASUS\.gemini\antigravity-ide\brain\363244ab-5440-419c-ac42-8cfffff48249\scratch\backlog_data.json', 'r', encoding='utf-8') as f:
    data = json.load(f)

s1 = data['sprints'][0]
print(s1['header'])
print(s1['goal'])
print(f"Tổng số stories: {len(s1['stories'])}")
print("-" * 70)
for idx, st in enumerate(s1['stories'], 1):
    print(f"{idx}. ID: {st.get('id')} | Priority: {st.get('priority')} | Points: {st.get('point')} | Epic: {st.get('epic')}")
    print(f"   Role: {st.get('role')}")
    print(f"   Story: {st.get('story')}")
    print(f"   Criteria / Tasks: {st.get('acceptance_criteria') or st.get('tasks') or 'N/A'}")
    print()
