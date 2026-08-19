import xml.etree.ElementTree as ET

tree = ET.parse('c:/Code/StudyTracker/icon/two-dolphins-svgrepo-com.svg')
root = tree.getroot()

paths = []
for path in root.iter('{http://www.w3.org/2000/svg}path'):
    d = path.get('d')
    d = ' '.join(d.split())
    paths.append(d)

vector_xml = f'''<?xml version="1.0" encoding="utf-8"?>
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="108dp"
    android:height="108dp"
    android:viewportWidth="545"
    android:viewportHeight="545">
    <group
        android:translateX="90.84"
        android:translateY="90.84">
        <path android:fillColor="#FFFFFF" android:pathData="{paths[0]}" />
        <path android:fillColor="#FFFFFF" android:pathData="{paths[1]}" />
    </group>
</vector>
'''

with open('c:/Code/StudyTracker/app/src/main/res/drawable/ic_launcher_foreground.xml', 'w') as f:
    f.write(vector_xml)
print('Done!')
