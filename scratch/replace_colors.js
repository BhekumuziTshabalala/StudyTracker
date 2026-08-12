const fs = require('fs');
const path = require('path');

const replacements = {
    "DarkBackground": "MaterialTheme.colorScheme.background",
    "DarkSurfaceVariant": "MaterialTheme.colorScheme.surfaceVariant",
    "DarkSurface": "MaterialTheme.colorScheme.surface",
    "TextPrimary": "MaterialTheme.colorScheme.onBackground",
    "TextSecondary": "MaterialTheme.colorScheme.onSurfaceVariant",
    "TextTertiary": "MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)"
};

function processDirectory(directory) {
    const files = fs.readdirSync(directory);
    for (const file of files) {
        const fullPath = path.join(directory, file);
        if (fs.statSync(fullPath).isDirectory()) {
            processDirectory(fullPath);
        } else if (fullPath.endsWith('.kt') && !file.includes('Theme.kt') && !file.includes('Color.kt')) {
            let content = fs.readFileSync(fullPath, 'utf-8');
            let newContent = content;
            for (const [old, new_val] of Object.entries(replacements)) {
                newContent = newContent.split(old).join(new_val);
            }
            if (content !== newContent) {
                fs.writeFileSync(fullPath, newContent);
                console.log("Updated", fullPath);
            }
        }
    }
}

processDirectory('c:/Code/StudyTracker/app/src/main/java/com/iu/studytracker/ui');
