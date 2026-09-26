import re

with open('app/src/main/java/com/example/ui/PointsManager.kt', 'r') as f:
    content = f.read()

content = content.replace("fun syncFromFirestore() {\n        val uid = getUserId() ?: return@launch", "fun syncFromFirestore() {\n        val uid = getUserId() ?: return")

with open('app/src/main/java/com/example/ui/PointsManager.kt', 'w') as f:
    f.write(content)
print("Fixed syncFromFirestore")
