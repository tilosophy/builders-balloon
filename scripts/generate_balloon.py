"""Generate the original alpha.16 fabric balloon in all 16 dye colours.
Run from project root: python3 scripts/generate_balloon.py (standard library).
No XyCraft, Education Edition or Aeronautics artwork is sampled or traced.
The model centre and bottom attachment preserve the existing renderer alignment.
"""
from generate_anchor import Mesh, MODELS, ASSETS, COLORS, png, rgb, blend, noise, shade, write_json


def fabric(color,x,y):
    base=blend(rgb(COLORS[color]),(167,159,143),.14)
    panel=x%8
    delta=noise(x//3,y//4,45)//20-2
    # Black needs darker edging; a neutral grey seam disappears against its cloth.
    thread=(12,12,14) if sum(base)/3<65 else (47,46,43)
    if panel==0:return blend(base,thread,.48)
    if panel==1:return shade(base,7 if y%4==0 else -12)
    if y in (17,18):return blend(base,thread,.36 if y==17 else .22)
    return shade(base,delta+(2 if (x+y)%5==0 else 0))


def main():
    mesh=Mesh()
    mesh.lines[:2]=['# Original fabric-envelope balloon; scripts/generate_balloon.py','mtllib balloon.mtl']
    mesh.group('Envelope')
    rings=[(.9,.65),(1.65,1.25),(2.7,3.1),(5.4,4.9),(8.25,5),(10.35,3.9),(11.5,1.65)]
    import math
    for (y,r),(Y,R) in zip(rings,rings[1:]):
        for i in range(16):
            a=i*math.pi/8+math.pi/16;b=(i+1)*math.pi/8+math.pi/16
            mesh.face([(8+r*math.cos(a),y,8+r*math.sin(a)),(8+R*math.cos(a),Y,8+R*math.sin(a)),
                (8+R*math.cos(b),Y,8+R*math.sin(b)),(8+r*math.cos(b),y,8+r*math.sin(b))],
                'Fabric',[(i/16,y/11.8),(i/16,Y/11.8),((i+1)/16,Y/11.8),((i+1)/16,y/11.8)])
    for i in range(16):
        a=i*math.pi/8+math.pi/16;b=(i+1)*math.pi/8+math.pi/16
        mesh.face([(8+1.65*math.cos(b),11.5,8+1.65*math.sin(b)),(8+1.65*math.cos(a),11.5,8+1.65*math.sin(a)),(8,11.8,8)],
            'Crown',[(.5+.44*math.cos(b),.5+.44*math.sin(b)),(.5+.44*math.cos(a),.5+.44*math.sin(a)),(.5,.5)])
    mesh.group('Tie');mesh.box((7.3,0,7.3),(8.7,.95,8.7),'Tie');mesh.box((8.2,.2,7.5),(8.8,.75,8.3),'Tie')
    (MODELS/'balloon.obj').write_text('\n'.join(mesh.lines)+'\n')
    (MODELS/'balloon.mtl').write_text(''.join(f'newmtl {name}\nKa 0 0 0\nKd 1 1 1\nmap_Kd #{texture}\nforge_TintIndex -1\n' for name,texture in [('Fabric','balloon'),('Crown','crown'),('Tie','tie')]))
    textures=ASSETS/'textures/block'
    png(textures/'balloon_tie.png',16,16,lambda x,y: (115,88,55))
    for color in COLORS:
        texture='balloon' if color=='blue' else 'balloon_'+color
        png(textures/(texture+'.png'),64,32,lambda x,y,color=color:fabric(color,x,y))
        crown_texture='balloon_crown_'+color
        # UV radius .44 keeps the entire fan inside the sprite. The outer
        # seam extends beyond the sampled edge to avoid gaps at polygon corners.
        base=blend(rgb(COLORS[color]),(167,159,143),.14)
        thread=(12,12,14) if sum(base)/3<65 else (47,46,43)
        seam=blend(base,thread,.48)
        png(textures/(crown_texture+'.png'),32,32,
            lambda x,y,base=base,seam=seam: seam if math.hypot(x+.5-16,y+.5-16)>=12.3 else base)
        suffix='' if color=='blue' else '_'+color
        write_json(MODELS/('balloon'+suffix+'.json'),{'loader':'neoforge:obj','model':'builders_balloon:models/block/balloon_tether/balloon.obj',
            'textures':{'balloon':'builders_balloon:block/'+texture,'crown':'builders_balloon:block/'+crown_texture,'tie':'builders_balloon:block/balloon_tie','particle':'#balloon'},
            'parent':'block/block','automatic_culling':False,'flip_v':True,'render_type':'cutout'})
    print(f'Generated original balloon: {mesh.count} vertices, 16 fabric colours; no imported assets.')

if __name__=='__main__':main()
