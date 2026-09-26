import re

with open('app/src/main/java/com/example/data/PlantRepository.kt', 'r') as f:
    content = f.read()

old_block = """                        val plants = snapshot.documents.mapNotNull { doc ->
                            val p = doc.toObject(Plant::class.java)
                            p?.copy(firestoreId = doc.id)
                        }"""
new_block = """                        val plants = snapshot.documents.mapNotNull { doc ->
                            try {
                                val p = doc.toObject(Plant::class.java)
                                p?.copy(firestoreId = doc.id)
                            } catch (e: Exception) {
                                android.util.Log.e("PlantRepository", "Failed to parse plant doc", e)
                                null
                            }
                        }"""

if old_block in content:
    content = content.replace(old_block, new_block)
    with open('app/src/main/java/com/example/data/PlantRepository.kt', 'w') as f:
        f.write(content)
    print("Patched PlantRepository")
else:
    print("Pattern not found in PlantRepository")
