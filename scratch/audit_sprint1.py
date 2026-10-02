import json
import os

path = r"C:\Users\ASUS\.gemini\antigravity-ide\brain\363244ab-5440-419c-ac42-8cfffff48249\scratch\backlog_data.json"
with open(path, "r", encoding="utf-8") as f:
    data = json.load(f)

for s in data["sprints"]:
    header = s.get("header", "")
    if "SPRINT 1" in header:
        print(f"=== {header} ===")
        print(f"Goal: {s.get('goal')}\n")
        for st in s["stories"]:
            print(f"--- [{st['id']}] {st['story']} (Points: {st['point']}) ---")
            print(f"    Acceptance Criteria (AC):\n{st.get('ac')}")
            print(f"    Priority: {st.get('priority')}\n")
