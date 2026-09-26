import re

with open('app/src/main/java/com/example/ui/ScanHistoryManager.kt', 'r') as f:
    content = f.read()

old_block = """    private var deviceId: String = ""
    private fun getUserId(): String = auth?.currentUser?.uid ?: deviceId"""
    
new_block = """    private var deviceId: String = ""
    private fun getUserId(): String = try { auth?.currentUser?.uid ?: deviceId } catch(e: Exception) { deviceId }"""

if old_block in content:
    content = content.replace(old_block, new_block)
    with open('app/src/main/java/com/example/ui/ScanHistoryManager.kt', 'w') as f:
        f.write(content)
    print("Patched ScanHistoryManager user")
else:
    print("Pattern not found in ScanHistoryManager user")
