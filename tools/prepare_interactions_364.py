"""Convert the approved green-screen MP4s into silent transparent one-shot WebP assets.

Usage: python tools/prepare_interactions_364.py /path/to/upload
The source videos stay outside the repository; approved keyed assets are shipped.
"""
from pathlib import Path
import subprocess, sys, json
import numpy as np
from PIL import Image

ROOT=Path(__file__).resolve().parents[1]; SRC=Path(sys.argv[1]); RES=ROOT/'app/src/main/res/drawable-nodpi'
REVIEW=ROOT.parent/'work-364/video-review';REVIEW.mkdir(parents=True,exist_ok=True)
def key(rgb):
    f=rgb.astype(np.float32);r,g,b=f[...,0],f[...,1],f[...,2]
    ratio=(g-np.maximum(r,b))/np.maximum(g,1)
    alpha=np.clip((.58-ratio)/.38,0,1)
    alpha=alpha*alpha*(3-2*alpha)
    # Remove green reflected at the matte boundary, preserving white hair.
    f[...,1]=np.where(g>np.maximum(r,b),np.minimum(g,np.maximum(r,b)+2),g)
    out=np.dstack((np.clip(f,0,255).astype(np.uint8),(alpha*255).astype(np.uint8)))
    out[alpha<.025]=0
    return Image.fromarray(out,'RGBA')

manifest=[]
for token,name in [('2054','mascot_06_confident'),('2659','mascot_08_stretch')]:
    src=next(SRC.glob('jimeng-2026-09-10-'+token+'-*.mp4'))
    proc=subprocess.Popen(['ffmpeg','-v','error','-i',str(src),'-an','-vf','fps=24,scale=640:640:flags=lanczos','-f','rawvideo','-pix_fmt','rgb24','-'],stdout=subprocess.PIPE)
    frames=[]
    while data:=proc.stdout.read(640*640*3):
        if len(data)!=640*640*3:raise RuntimeError('Truncated decoded frame')
        frames.append(key(np.frombuffer(data,np.uint8).reshape(640,640,3)))
    if proc.wait():raise RuntimeError('Decode failed')
    bounds=frames[0].getchannel('A').point(lambda a:255 if a>100 else 0).getbbox()
    x0,y0,x1,y1=bounds
    # Match the idle character's 597px height and foot baseline. Preserve the
    # entire union of the raised arms instead of re-centring moving frames.
    scale=597/(y1-y0)
    tx=310-(x0+x1)/2*scale;ty=625-y1*scale
    matrix=(1/scale,0,-tx/scale,0,1/scale,-ty/scale)
    frames=[f.transform((640,640),Image.Transform.AFFINE,matrix,Image.Resampling.BICUBIC) for f in frames]
    # Integer frame durations sum to precisely 5 seconds for 120 frames.
    durations=[round((i+1)*1000/24)-round(i*1000/24) for i in range(len(frames))]
    frames[0].save(RES/(name+'_poster.png'),optimize=True)
    frames[0].save(RES/(name+'.webp'),save_all=True,append_images=frames[1:],duration=durations,loop=1,quality=86,method=5,exact=True)
    image=Image.new('RGB',(960,640),'#f4f5fa')
    for row,bg in enumerate(['#f4f5fa','#152031']):
        for col,t in enumerate([0,2,4]):
            cell=Image.new('RGB',(320,320),bg);f=frames[t*24].resize((320,320),Image.Resampling.LANCZOS);cell.paste(f,(0,0),f);image.paste(cell,(col*320,row*320))
    image.save(REVIEW/(name+'-keyed.png'))
    union=Image.new('L',(640,640)); from PIL import ImageChops
    for f in frames: union=ImageChops.lighter(union,f.getchannel('A'))
    report={'name':name,'frames':len(frames),'duration_ms':sum(durations),'first_source_bounds':bounds,'union':union.getbbox(),'bytes':(RES/(name+'.webp')).stat().st_size}
    manifest.append(report);print(report,flush=True)
(ROOT/'design/brand-3.6.4/animation-manifest.json').write_text(json.dumps(manifest,indent=2))
