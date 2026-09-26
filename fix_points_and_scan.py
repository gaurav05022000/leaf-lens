import re

def fix_file(filename):
    with open(filename, 'r') as f:
        content = f.read()

    # We want to replace getUserId usages or check if it's null before proceeding
    content = content.replace("private fun getUserId(): String = auth?.currentUser?.uid ?: deviceId", 
                              "private fun getUserId(): String? = auth?.currentUser?.uid")
    
    with open(filename, 'w') as f:
        f.write(content)
        
fix_file('app/src/main/java/com/example/ui/PointsManager.kt')
fix_file('app/src/main/java/com/example/ui/ScanHistoryManager.kt')
print("Patched models")
