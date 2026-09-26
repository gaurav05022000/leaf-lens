import re

with open('app/build.gradle.kts', 'r') as f:
    content = f.read()

# Make sure we're bumping the version to avoid version code conflicts if one exists
if "versionCode = 23" in content:
    content = content.replace("versionCode = 23", "versionCode = 24")
    content = content.replace('versionName = "23.0"', 'versionName = "24.0"')

with open('app/build.gradle.kts', 'w') as f:
    f.write(content)
print("Bumpted version code to 24")

