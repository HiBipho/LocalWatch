import glob
import shutil
from PIL import Image
import os

img_path = glob.glob('/home/ubuntu/.gemini/antigravity-cli/brain/4551ce3b-e6f8-4e11-aeca-92e50bff24a2/localwatch_icon_*.jpg')[0]
img = Image.open(img_path)
img.save('/home/ubuntu/LocalWatch/assets/icon.png')
img.save('/home/ubuntu/LocalWatch/assets/adaptive-icon.png')
img.save('/home/ubuntu/LocalWatch/assets/android-icon-foreground.png')
print("Icons updated successfully!")
