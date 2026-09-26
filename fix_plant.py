import re

with open('app/src/main/java/com/example/data/Plant.kt', 'r') as f:
    content = f.read()

content = content.replace("fun String.isStatusHealthy(): Boolean {", "fun String?.isStatusHealthy(): Boolean {\n    if (this == null) return false")

with open('app/src/main/java/com/example/data/Plant.kt', 'w') as f:
    f.write(content)
print("Patched Plant.kt")
