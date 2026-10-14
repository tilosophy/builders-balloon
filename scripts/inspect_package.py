"""Inspect baseline geometry, audio, bytecode targets and release package metadata."""
import sys, zipfile, json, tomllib, struct, hashlib
from pathlib import Path
p=Path(sys.argv[1]);checks=0
with zipfile.ZipFile(p) as z:
 names=z.namelist()
 assert not any('/test/' in n or '/sabletest/' in n or 'builders_balloon_tests' in n or 'builders_balloon_sable_tests' in n for n in names);checks+=1
 mod=tomllib.loads(z.read('META-INF/neoforge.mods.toml').decode());deps={d['modId']:d for d in mod['dependencies']['builders_balloon']}
 assert set(deps)=={'minecraft','neoforge','sable'} and deps['sable']['type']=='optional' and deps['minecraft']['versionRange']=='[1.21.1]' and deps['neoforge']['versionRange']=='[21.1.252,21.2)';checks+=1
 assert p.stem.endswith('-'+mod['mods'][0]['version']);checks+=1
 assert mod['mixins'][0]['config'] in names;checks+=1
 if 'logoFile' in mod['mods'][0]:
  logo=mod['mods'][0]['logoFile'];data=z.read(logo)
  assert '/' not in logo and data.startswith(b'\x89PNG\r\n\x1a\n');checks+=1
  assert struct.unpack('>II',data[16:24])==(256,256);checks+=1
  assert data==(Path(__file__).resolve().parents[1]/'src/main/resources'/logo).read_bytes();checks+=1
 if mod['license']=='MIT':
  assert z.read('META-INF/LICENSE')==(Path(__file__).resolve().parents[1]/'LICENSE').read_bytes();checks+=1
 assert not any(n.startswith('dev/ryanhcode/') or n.endswith('.jar') for n in names);checks+=1
 assert not any(n.endswith('.wav') or n.startswith('audio/masters/') for n in names);checks+=1
 report=json.loads((Path(__file__).resolve().parents[1]/'docs/runtime-audio-hashes.json').read_text())
 for sound in ('balloon_inflate','balloon_pop'):
  data=z.read('assets/builders_balloon/sounds/'+sound+'.ogg');idx=data.index(b'\x01vorbis')
  assert data.startswith(b'OggS') and data[idx+11]==1 and struct.unpack('<I',data[idx+12:idx+16])[0]==48000;checks+=1
  assert hashlib.sha256(data).hexdigest()==report[sound]['ogg_sha256'];checks+=1
  assert 'Pixabay' in z.read('META-INF/NOTICE.md').decode();checks+=1
 assert deps['sable']['versionRange']=='[2.0.6,3.0)' and deps['sable']['ordering']=='AFTER';checks+=1
 for n in names:
  if n.endswith('.json'):json.loads(z.read(n));checks+=1
  if n.endswith('.class'):
   d=z.read(n);assert struct.unpack('>H',d[6:8])[0]==65;checks+=1
   if '/client/' not in n:assert b'net/minecraft/client/' not in d;checks+=1
 assert json.loads(z.read('data/minecraft/tags/block/mineable/axe.json'))=={'replace':False,'values':['builders_balloon:balloon_tether','builders_balloon:bollard']};checks+=1
 if len(sys.argv)>2:
  # Private development comparison; public package inspection uses one JAR.
  if not (Path(__file__).resolve().parent/'audit_bollard.py').is_file():
   raise SystemExit('Historical comparison requires private development helpers; pass one JAR for public package inspection.')
  from audit_bollard import compare_runtime
  checks+=compare_runtime(z,zipfile.ZipFile(sys.argv[2]))
 colors='white orange magenta light_blue yellow lime pink gray light_gray cyan purple blue brown green red black'.split()
 lang=json.loads(z.read('assets/builders_balloon/lang/en_us.json'))
 for color in colors:
  suffix='' if color=='blue' else '_'+color
  model=json.loads(z.read('assets/builders_balloon/models/block/balloon_tether/balloon'+suffix+'.json'))
  assert model['model']=='builders_balloon:models/block/balloon_tether/balloon.obj';checks+=1
  texture=model['textures']['balloon'].split(':',1)[1]
  png=z.read('assets/builders_balloon/textures/'+texture+'.png')
  assert png[:8]==b'\x89PNG\r\n\x1a\n' and struct.unpack('>II',png[16:24])==(64,32);checks+=1
  assert model['textures']['tie']=='builders_balloon:block/balloon_tie' and not model['automatic_culling'];checks+=1
  # Authored PNGs use RGBA8/unfiltered rows. Check opacity and seam contrast.
  import zlib
  offset=8;idat=[]
  while offset<len(png):
   size=struct.unpack('>I',png[offset:offset+4])[0];kind=png[offset+4:offset+8]
   if kind==b'IDAT':idat.append(png[offset+8:offset+8+size])
   offset+=size+12
  raw=zlib.decompress(b''.join(idat));stride=64*4+1
  assert len(raw)==32*stride and all(raw[y*stride]==0 for y in range(32));checks+=1
  assert all(raw[y*stride+1+x*4+3]==255 for y in range(32) for x in range(64));checks+=1
  edge=raw[8*stride+1:8*stride+4];cloth=raw[8*stride+9:8*stride+12]
  assert sum(cloth)-sum(edge)>30,(color,edge,cloth);checks+=1
  assert model['textures']['crown']=='builders_balloon:block/balloon_crown_'+color;checks+=1
  crown_png=z.read('assets/builders_balloon/textures/block/balloon_crown_'+color+'.png')
  assert struct.unpack('>II',crown_png[16:24])==(32,32);checks+=1
  chunks=[];off=8
  while off<len(crown_png):
   length=struct.unpack('>I',crown_png[off:off+4])[0]
   if crown_png[off+4:off+8]==b'IDAT':chunks.append(crown_png[off+8:off+8+length])
   off+=length+12
  crown_raw=zlib.decompress(b''.join(chunks));cs=129
  assert len(crown_raw)==32*cs and all(crown_raw[y*cs]==0 for y in range(32));checks+=1
  def crown_pixel(x,y):return crown_raw[y*cs+1+x*4:y*cs+5+x*4]
  plain=crown_pixel(16,16);rim=crown_pixel(29,16)
  assert plain[3]==rim[3]==255 and rim[:3]==edge and sum(plain[:3])>sum(rim[:3])+30;checks+=1
  for y in range(32):
   for x in range(32):
    expected=rim if ((x+.5-16)**2+(y+.5-16)**2)**.5>=12.3 else plain
    assert crown_pixel(x,y)==expected,(color,x,y);checks+=1
  recipe=json.loads(z.read('data/builders_balloon/recipe/balloon_tether_'+color+'.json'))
  assert recipe['ingredients']==[{'item':'builders_balloon:balloon_tether'},{'item':'minecraft:'+color+'_dye'}] and recipe['result']=={'id':'builders_balloon:balloon_tether','count':1,'components':{'minecraft:block_state':{'color':color}}};checks+=1
  assert lang['item.builders_balloon.balloon_tether.'+color]==color.replace('_',' ').title()+' Builder\'s Balloon';checks+=1
 for color in colors:
  path='data/builders_balloon/recipe/balloon_tether_from_wool_'+color+'.json'
  recipe=json.loads(z.read(path))
  assert recipe['type']=='minecraft:crafting_shaped' and recipe['pattern']==[' W ','LGL',' I '];checks+=1
  assert recipe['key']=={'W':{'item':'minecraft:'+color+'_wool'},'L':{'tag':'builders_balloon:ropes'},'G':{'tag':'minecraft:logs'},'I':{'item':'minecraft:iron_ingot'}};checks+=1
  assert recipe['result']=={'id':'builders_balloon:balloon_tether','count':1,'components':{'minecraft:block_state':{'color':color}}};checks+=1
  for recipe_id in ['balloon_tether_'+color,'balloon_tether_from_wool_'+color]:
   advance=json.loads(z.read('data/builders_balloon/advancement/recipes/'+recipe_id+'.json'))
   assert advance['rewards']['recipes']==['builders_balloon:'+recipe_id] and advance['criteria']['has_the_recipe']['conditions']['recipe']=='builders_balloon:'+recipe_id;checks+=1
 assert 'assets/builders_balloon/blockstates/balloon_tether.json' in names and 'assets/builders_balloon/models/item/balloon_tether.json' in names;checks+=1
 assert not any('/stake' in n for n in names);checks+=1
 for n in names:
  if n.endswith('.json'): assert b'builders_balloon:stake' not in z.read(n)
 checks+=1
 assert json.loads(z.read('data/builders_balloon/loot_table/blocks/balloon_tether.json'))['pools'][0]['entries'][0]['functions']==[{'function':'minecraft:copy_state','block':'builders_balloon:balloon_tether','properties':['color']}];checks+=1

 prefix='assets/builders_balloon/'
 def model(path): return json.loads(z.read(prefix+'models/'+path+'.json'))
 base=model('block/balloon_tether/model')
 assert base['model']=='builders_balloon:models/block/balloon_tether/anchor.obj' and not base['automatic_culling'];checks+=1
 assert base['textures']['stripped']=='minecraft:block/stripped_oak_log';checks+=1
 assert base['visibility']=={'Timber':True,'Mount':True,'Rope':False};checks+=1
 assert base['textures']['wrap']=='builders_balloon:block/anchor/rope' and base['textures']['wood']=='minecraft:block/oak_log' and base['textures']['particle']=='minecraft:block/oak_log';checks+=1
 assert 'assets/minecraft/atlases/blocks.json' not in names;checks+=1
 assert not any(n.endswith(('/stripped_oak_log.png','/lead_knot.png','/oak_log.png','/anchor/wood.png','/anchor/wrap.png','/anchor/endgrain.png')) for n in names);checks+=1
 mtl=z.read(prefix+'models/block/balloon_tether/anchor.mtl').decode()
 assert 'newmtl Rope' in mtl and 'map_Kd #wrap' in mtl;checks+=1
 rope=model('block/balloon_tether/rope')
 assert rope['visibility']=={'Timber':False,'Mount':False,'Rope':True};checks+=1
 item=model('item/balloon_tether')
 assert item['parent']=='builders_balloon:item/balloon_tether_white';checks+=1
 overrides=item['overrides']
 assert len(overrides)==16;checks+=1
 for index,color in enumerate(colors):
  assert overrides[index]=={'predicate':{'builders_balloon:color':index},'model':'builders_balloon:item/balloon_tether_'+color};checks+=1
  variant=model('item/balloon_tether_'+color)
  assert variant['loader']=='neoforge:composite' and variant['parent']=='block/block';checks+=1
  assert variant['children']=={'anchor':{'parent':'builders_balloon:block/balloon_tether/anchor_'+color,'visibility':{'Rope':True}},'weathering':{'parent':'builders_balloon:block/balloon_tether/weathering'},'contact_shadow':{'parent':'builders_balloon:block/balloon_tether/contact_shadow'}};checks+=1
  assert variant['item_render_order']==['anchor','weathering','contact_shadow'] and variant['textures']['particle']=='minecraft:block/oak_log';checks+=1
  assert set(variant['display'])=={'gui','ground','fixed','thirdperson_righthand','thirdperson_lefthand','firstperson_righthand','firstperson_lefthand'};checks+=1
  anchor=model('block/balloon_tether/anchor_'+color)
  assert anchor['textures']=={'paint':'builders_balloon:block/anchor/paint_'+color,'paint_end':'builders_balloon:block/anchor/cap_'+color};checks+=1
  for tex in anchor['textures'].values():
   assert z.read(prefix+'textures/'+tex.split(':')[1]+'.png')[:8]==b'\x89PNG\r\n\x1a\n';checks+=1
  # Vanilla chooses overrides in reverse order using >= thresholds.
  selected=next(v for v in reversed(overrides) if index>=v['predicate']['builders_balloon:color'])
  assert selected['model'].endswith('_'+color);checks+=1
 multipart=json.loads(z.read(prefix+'blockstates/balloon_tether.json'))['multipart']
 rotations={'down':{},'up':{'x':180},'north':{'x':90,'y':180},'south':{'x':90},'west':{'x':90,'y':90},'east':{'x':90,'y':270}}
 assert len(multipart)==456;checks+=1
 for direction,rot in rotations.items():
  for spin in range(4):
   suffix='' if spin==0 else '_spin'+str(spin)
   for color in colors:
    for connected in (False,True):
     for waterlogged in (False,True):
      state={'direction':direction,'spin':spin,'color':color,'connected':connected,'waterlogged':waterlogged}
      parts=[part['apply'] for part in multipart if all(state[k]==v for k,v in part['when'].items())]
      expected=[{'model':'builders_balloon:block/balloon_tether/anchor_'+color+suffix,**rot},{'model':'builders_balloon:block/balloon_tether/weathering'+suffix,**rot}]
      if connected:expected += [{'model':'builders_balloon:block/balloon_tether/'+stem+suffix,**rot} for stem in ('rope','contact_shadow')]
      assert parts==expected;checks+=1
 # Independently validate OBJ topology, finite vertices, bounds and materials.
 import math
 vertices=[];uvs=[];groups=set();materials=set();faces=0;group=None;material=None;material_faces=[]
 for line in z.read(prefix+'models/block/balloon_tether/anchor.obj').decode().splitlines():
  tokens=line.split()
  if not tokens:continue
  if tokens[0]=='v':
   v=tuple(map(float,tokens[1:]));assert len(v)==3 and all(math.isfinite(n) for n in v);assert .25-1e-6<=v[0]<=.75+1e-6 and 0<=v[1]<=.75 and .25-1e-6<=v[2]<=(12.15/16+1e-6 if group=='Rope' else .75+1e-6);vertices.append(v);checks+=1
  elif tokens[0]=='vt':
   uv=tuple(map(float,tokens[1:]));assert len(uv)==2 and all(0<=n<=1 for n in uv);uvs.append(uv);checks+=1
  elif tokens[0]=='o':groups.add(tokens[1]);group=tokens[1]
  elif tokens[0]=='usemtl':materials.add(tokens[1]);material=tokens[1]
  elif tokens[0]=='f':
   ids=[tuple(map(int,v.split('/'))) for v in tokens[1:]]
   assert len(ids) in (3,4) and all(1<=v<=len(vertices) and 1<=t<=len(uvs) for v,t in ids)
   a,b,c=[vertices[v-1] for v,t in ids[:3]]
   ab=[b[i]-a[i] for i in range(3)];ac=[c[i]-a[i] for i in range(3)]
   cross=[ab[1]*ac[2]-ab[2]*ac[1],ab[2]*ac[0]-ab[0]*ac[2],ab[0]*ac[1]-ab[1]*ac[0]]
   assert sum(n*n for n in cross)>1e-12;faces+=1;checks+=1
   material_faces.append((material,[vertices[v-1] for v,t in ids]))
 assert groups=={'Timber','Mount','Rope'} and materials=={'Wood','Stripped','Metal','Rivet','Rope','Paint','PaintEnd'} and faces==360;checks+=1
 assert len(vertices)==1392;checks+=1
 stripped=[pts for mat,pts in material_faces if mat=='Stripped']
 assert len(stripped)==16;checks+=1
 for pts in stripped:
  assert {round(p[1]*16,4) for p in pts}=={6.25,7.85}
  assert all(abs(math.hypot(p[0]*16-8,p[2]*16-8)-2.95)<1e-5 for p in pts);checks+=1
 shoulders=[(mat,pts) for mat,pts in material_faces if {round(p[1]*16,4) for p in pts} in ({5.9,6.25},{7.85,8.2})]
 assert len(shoulders)==32 and all(mat=='Wood' for mat,pts in shoulders);checks+=1
 paint=[p[1]*16 for mat,pts in material_faces if mat in ('Paint','PaintEnd') for p in pts]
 assert abs(min(paint)-8.2)<1e-5 and max(paint)==12 and abs(max(paint)-min(paint)-3.8)<1e-5;checks+=1
 # Verify separate wear pass covers the 48 bark quads, excluding the cut groove.
 weather=model('block/balloon_tether/weathering')
 assert weather['loader']=='neoforge:obj' and weather['render_type']=='translucent' and not weather['automatic_culling'];checks+=1
 assert weather['textures']=={'particle':'minecraft:block/oak_log','weathering':'builders_balloon:block/anchor/weathering'};checks+=1
 wear_text=z.read(prefix+'models/block/balloon_tether/weathering.obj').decode()
 assert wear_text.count('\nf ')==48 and 'o Weathering' in wear_text and 'mtllib weathering.mtl' in wear_text;checks+=1
 wear_vertices=[tuple(map(float,l.split()[1:])) for l in wear_text.splitlines() if l.startswith('v ')]
 assert len(wear_vertices)==192 and all(.25<x<.75 and 0<y<.75 and .25<zz<.75 for x,y,zz in wear_vertices);checks+=1
 assert b'map_Kd #weathering' in z.read(prefix+'models/block/balloon_tether/weathering.mtl') and b'Kd 1 1 1 0.333333333' in z.read(prefix+'models/block/balloon_tether/weathering.mtl');checks+=1
 png=z.read(prefix+'textures/block/anchor/weathering.png')
 assert struct.unpack('>II',png[16:24])==(32,16) and png[25]==6;checks+=1
 if len(sys.argv)>2:
  with zipfile.ZipFile(sys.argv[2]) as old:
   for color in colors:
    for stem in ('paint_','cap_'):
     path=prefix+'textures/block/anchor/'+stem+color+'.png'
     assert z.read(path)==old.read(path),path;checks+=1
 assert not any(n.endswith('/balloon_tether.obj') or n.endswith('/balloon_tether.mtl') or n.endswith('/balloon_tether/balloon_tether.png') for n in names);checks+=1

 balloon=z.read(prefix+'models/block/balloon_tether/balloon.obj').decode()
 if len(sys.argv)>2:
  with zipfile.ZipFile(sys.argv[2]) as old:
   before=old.read('assets/builders_balloon/models/block/balloon_tether/balloon.obj').decode()
   assert [l for l in balloon.splitlines() if not l.startswith(('vt ','usemtl '))]==[l for l in before.splitlines() if not l.startswith(('vt ','usemtl '))];checks+=1
   old_uv=[l for l in before.splitlines() if l.startswith('vt ')]
   new_uv=[l for l in balloon.splitlines() if l.startswith('vt ')]
   assert new_uv[:384]==old_uv[:384] and new_uv[432:]==old_uv[432:];checks+=1
 verts=[];uv=[];groups=set();mats=set();count=0;crown_edges={};crown=0;current_mat=None
 for line in balloon.splitlines():
  t=line.split()
  if not t:continue
  if t[0]=='v':
   v=tuple(map(float,t[1:]));assert len(v)==3 and all(math.isfinite(n) for n in v) and .1875<=v[0]<=.8125 and 0<=v[1]<=11.8/16+1e-7 and .1875<=v[2]<=.8125;verts.append(v);checks+=1
  elif t[0]=='vt':
   a=tuple(map(float,t[1:]));assert len(a)==2 and all(0<=n<=1 for n in a);uv.append(a);checks+=1
  elif t[0]=='o':groups.add(t[1])
  elif t[0]=='usemtl':mats.add(t[1]);current_mat=t[1]
  elif t[0]=='f':
   ids=[tuple(map(int,v.split('/'))) for v in t[1:]]
   assert len(ids) in (3,4) and all(1<=v<=len(verts) and 1<=u<=len(uv) for v,u in ids)
   a,b,c=[verts[v-1] for v,u in ids[:3]];ab=[b[i]-a[i] for i in range(3)];ac=[c[i]-a[i] for i in range(3)]
   cross=[ab[1]*ac[2]-ab[2]*ac[1],ab[2]*ac[0]-ab[0]*ac[2],ab[0]*ac[1]-ab[1]*ac[0]]
   assert sum(n*n for n in cross)>1e-12;count+=1;checks+=1
   if len(ids)==3:
    assert current_mat=='Crown';checks+=1
    # Top triangles must cover an actual interior texture area and face upward.
    A,B,C=[uv[u-1] for v,u in ids]
    uv_area=abs((B[0]-A[0])*(C[1]-A[1])-(B[1]-A[1])*(C[0]-A[0]))/2
    assert cross[1]>0 and uv_area>.001 and all(.01<x<.99 and .01<y<.99 for x,y in (A,B,C));checks+=1
    points=[verts[v-1] for v,u in ids]
    for i in range(3):
     key=tuple(sorted((points[i],points[(i+1)%3])))
     crown_edges[key]=crown_edges.get(key,0)+1
    crown+=1
 assert crown==16 and sum(v==2 for v in crown_edges.values())==16 and sum(v==1 for v in crown_edges.values())==16;checks+=1
 assert groups=={'Envelope','Tie'} and mats=={'Fabric','Crown','Tie'} and count==124 and len(verts)==480;checks+=1
 mtl=z.read(prefix+'models/block/balloon_tether/balloon.mtl')
 assert b'map_Kd #balloon' in mtl and b'map_Kd #crown' in mtl and b'map_Kd #tie' in mtl and b'Blender' not in mtl;checks+=1
 tie=z.read(prefix+'textures/block/balloon_tie.png')
 assert struct.unpack('>II',tie[16:24])==(16,16);checks+=1
 assert prefix+'textures/block/balloon_tether/rope.png' not in names;checks+=1
 sources=p.with_name(p.stem+'-sources.jar')
 with zipfile.ZipFile(sources) as s:
  assert not any('/test/' in n or '/sabletest/' in n or 'builders_balloon_tests' in n or 'builders_balloon_sable_tests' in n for n in s.namelist());checks+=1
  renderer=s.read('dev/tilosophy/buildersballoon/client/StakeRenderer.java')
  assert b'BALLOON_SCALE = 1.2f' in renderer;checks+=1
  assert renderer.count(b'128 / 255f * shade, 112 / 255f * shade, 98 / 255f * shade')==2;checks+=1
  assert renderer.count(b'StakeRenderer.rope(poses, buffers, origin,')==2;checks+=1
  assert b'defineInRange("range", 16, 8, 32)' in s.read('dev/tilosophy/buildersballoon/BalloonConfig.java');checks+=1
  for filename in ('TetherBoundary.java','BalloonNetwork.java'):
   source=s.read('dev/tilosophy/buildersballoon/'+filename)
   assert b'range >= 8' in source and b'range <= 64' in source;checks+=1
 rope_png=z.read(prefix+'textures/block/anchor/rope.png')
 assert struct.unpack('>II',rope_png[16:24])==(32,16);checks+=1
 assert rope_png==(Path(__file__).resolve().parents[1]/'src/main/resources/assets/builders_balloon/textures/block/anchor/rope.png').read_bytes();checks+=1
 assert z.read(prefix+'models/block/balloon_tether/contact_shadow.obj').count(b'\nf ')==32;checks+=1
 assert struct.unpack('>II',z.read(prefix+'textures/block/anchor/contact_shadow.png')[16:24])==(16,16);checks+=1
 assert b'Kd 1 1 1 0.333333333' in z.read(prefix+'models/block/balloon_tether/contact_shadow.mtl');checks+=1
 def obj_vertices(path):return [tuple(map(float,l.split()[1:])) for l in z.read(prefix+'models/block/balloon_tether/'+path+'.obj').decode().splitlines() if l.startswith('v ')]
 for spin in range(1,4):
  for stem in ('anchor','weathering','contact_shadow'):
   before=obj_vertices(stem);after=obj_vertices(stem+'_spin'+str(spin));assert len(before)==len(after)
   for (x,y,zz),point in zip(before,after):
    x-=.5;zz-=.5;rx,rz=[(x,zz),(-zz,x),(-x,-zz),(zz,-x)][spin]
    assert all(abs(a-b)<1e-7 for a,b in zip((rx+.5,y,rz+.5),point));checks+=1
  for color in colors:
   m=model('block/balloon_tether/anchor_'+color+'_spin'+str(spin))
   assert m['loader']=='neoforge:obj' and m['model'].endswith('anchor_spin'+str(spin)+'.obj') and not m['automatic_culling'];checks+=1
 with zipfile.ZipFile(sources) as source:
  assert b'.setValue(SPIN, 0)' in source.read('dev/tilosophy/buildersballoon/StakeBlock.java');checks+=1
  assert b'AnchorOrientation.spinLocal' in source.read('dev/tilosophy/buildersballoon/client/StakeRenderer.java');checks+=1
 print(json.dumps({'checks_passed':checks,'version':mod['mods'][0]['version'],'bytes':p.stat().st_size,'sha256':hashlib.sha256(p.read_bytes()).hexdigest()},indent=2))
