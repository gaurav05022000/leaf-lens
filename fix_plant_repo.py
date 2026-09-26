import re

with open('app/src/main/java/com/example/data/PlantRepository.kt', 'r') as f:
    content = f.read()

old_block = """                                val p = doc.toObject(Plant::class.java)
                                p?.copy(firestoreId = doc.id)"""
                                
new_block = """                                val p = doc.toObject(Plant::class.java)
                                p?.copy(
                                    firestoreId = doc.id,
                                    name = p.name ?: "Unknown",
                                    species = p.species ?: "",
                                    healthStatus = p.healthStatus ?: "Unknown"
                                )"""

content = content.replace(old_block, new_block)

with open('app/src/main/java/com/example/data/PlantRepository.kt', 'w') as f:
    f.write(content)
print("Patched PlantRepository")
