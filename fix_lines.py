with open('app/src/main/java/com/example/ui/ScanHistoryManager.kt', 'r') as f:
    lines = f.readlines()

for i, line in enumerate(lines):
    if "val uid = getUserId() ?: return@launch" in line and i > 150:
        lines[i] = ""

with open('app/src/main/java/com/example/ui/ScanHistoryManager.kt', 'w') as f:
    f.writelines(lines)
