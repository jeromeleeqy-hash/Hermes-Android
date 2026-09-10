"""Export the two approved originals; launcher masks are applied by Android."""
from pathlib import Path
from PIL import Image
ROOT=Path(__file__).resolve().parents[1]
SRC=ROOT/'design/brand-3.6.4';RES=ROOT/'app/src/main/res'
for key,bg,launcher in [('partner','#F6F8F3','ic_launcher'),('sprite','#2D78F0','ic_launcher_sprite')]:
    im=Image.open(SRC/(key+'.png')).convert('RGB')
    im.resize((640,640),Image.Resampling.LANCZOS).save(RES/'drawable-nodpi'/('launcher_'+key+'_art.png'),optimize=True)
    im.resize((256,256),Image.Resampling.LANCZOS).save(RES/'drawable-nodpi'/('launcher_'+key+'_preview.png'),optimize=True)
    (RES/'drawable'/('launcher_'+key+'_background.xml')).write_text(f'<shape xmlns:android="http://schemas.android.com/apk/res/android"><solid android:color="{bg}" /></shape>\n')
    (RES/'drawable'/('launcher_'+key+'_foreground.xml')).write_text(f'''<inset xmlns:android="http://schemas.android.com/apk/res/android" android:insetLeft="13%" android:insetTop="13%" android:insetRight="13%" android:insetBottom="13%">
    <bitmap android:src="@drawable/launcher_{key}_art" android:antialias="true" android:filter="true" android:gravity="fill" />
</inset>\n''')
    xml=f'''<adaptive-icon xmlns:android="http://schemas.android.com/apk/res/android">
    <background android:drawable="@drawable/launcher_{key}_background" />
    <foreground android:drawable="@drawable/launcher_{key}_foreground" />
    <monochrome android:drawable="@drawable/launcher_headphones_mono" />
</adaptive-icon>\n'''
    (RES/'mipmap-anydpi'/(launcher+'.xml')).write_text(xml)
    if key=='partner':(RES/'mipmap-anydpi/ic_launcher_round.xml').write_text(xml)
