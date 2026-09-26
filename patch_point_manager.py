import re

with open('app/src/main/java/com/example/ui/PointsManager.kt', 'r') as f:
    content = f.read()

old_block = """                val cloudPoints = snapshot.getLong("availablePoints")?.toInt() ?: 0
                val cloudScans = snapshot.getLong("totalScans")?.toInt() ?: 0"""

new_block = """                val cloudPoints = try { snapshot.getLong("availablePoints")?.toInt() ?: 0 } catch (e: Exception) { 0 }
                val cloudScans = try { snapshot.getLong("totalScans")?.toInt() ?: 0 } catch (e: Exception) { 0 }"""

if old_block in content:
    content = content.replace(old_block, new_block)
    with open('app/src/main/java/com/example/ui/PointsManager.kt', 'w') as f:
        f.write(content)
    print("Patched PointsManager")
else:
    print("Pattern not found in PointsManager")
