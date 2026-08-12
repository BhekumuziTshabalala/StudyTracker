import os
import glob

replacements = {
    "DarkBackground": "MaterialTheme.colorScheme.background",
    "DarkSurface": "MaterialTheme.colorScheme.surface",
    "DarkSurfaceVariant": "MaterialTheme.colorScheme.surfaceVariant",
    "TextPrimary": "MaterialTheme.colorScheme.onBackground",
    "TextSecondary": "MaterialTheme.colorScheme.onSurfaceVariant",
    "TextTertiary": "MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)"
}

def process_file(filepath):
    with open(filepath, 'r', encoding='utf-8') as f:
        content = f.read()
    
    new_content = content
    for old, new in replacements.items():
        # Only replace if old is not in Theme.kt definition (we'll skip Theme.kt)
        new_content = new_content.replace(old, new)
        
    if new_content != content:
        with open(filepath, 'w', encoding='utf-8') as f:
            f.write(new_content)
        print(f"Updated {filepath}")

for root, dirs, files in os.walk('c:\\Code\\StudyTracker\\app\\src\\main\\java\\com\\iu\\studytracker\\ui'):
    for file in files:
        if file.endswith('.kt') and file != 'Theme.kt' and file != 'Color.kt':
            process_file(os.path.join(root, file))
