"""Generate original weathered metal finishes; reuse accepted geometry and rope.
No external artwork is sampled. Vanilla/Create recipes remain separate.
"""
from pathlib import Path
import json,copy,math,argparse
from generate_anchor import COLORS,noise,png,blend,rgb,shade
ROOT=Path(__file__).resolve().parents[1];R=ROOT/'src/main/resources';A=R/'assets/builders_balloon';D=R/'data/builders_balloon'
def write(p,data):p.parent.mkdir(parents=True,exist_ok=True);p.write_text(json.dumps(data,indent=2)+'\n')
def cap_substrate(x,y):
 # Cool worn iron, connected patches and occasional small oxidation marks.
 coarse=noise(x//4,y//3,61)
 c=(119,125,127) if coarse<35 else (99,106,109) if coarse<78 else (140,145,145)
 if noise(x//2,y//2,65)<13:c=blend(c,(105,82,65),.32)
 if (x in (5,6) and 3<=y<=10) or (x==23 and 8<=y<=14):c=shade(c,12)
 return shade(c,noise(x,y,63)//17-3)
def iron(x,y):
 # Continuous worn steel, short connected scratches and sparse oxidation.
 # Authored marks span body/groove UVs and the sleeve's actual side rows 13-15.
 delta=noise(x,y,63)//17-3
 # Broad connected tonal fields give the steel itself structure between wear
 # marks. Rounded levels remain pixel-art patches, without per-pixel grit.
 material_tone=3*round((11*math.sin(x*.49+y*.25)+6*math.cos(x*.23-y*.61))/3)
 c=shade((120,126,129),delta+material_tone)
 if ((x-7)/7)**2+((y-8)/5)**2<1:c=blend(c,(105,112,116),.48)
 if ((x-25)/6)**2+((y-5)/4)**2<1:c=blend(c,(132,138,139),.38)
 # Narrow abrasions have a dark cut and a restrained polished edge.
 scratch=6+(x//4)%2
 if 3<=x<=10 and y==scratch:c=(94,102,108)
 if 4<=x<=9 and y==scratch-1:c=(145,149,148)
 if 20<=x<=26 and y==9-(x//3)%2:c=(101,107,111)
 if 21<=x<=25 and y==8-(x//3)%2:c=(140,145,146)
 # Irregular small oxide clusters, not a grid of independent grey patches.
 if (x,y) in {(4,10),(5,10),(3,11),(4,11),(25,4),(26,4),(25,5),(24,5),(19,14),(20,14),(20,15)}:c=blend(c,(91,72,56),.48)
 if y==13 and x in (5,6,7,8,9,23,24,25):c=shade(c,17)
 if y==14 and x in (8,9,24):c=shade(c,10)
 if y==15 and x in (12,13,14,15,16):c=shade(c,-15)
 return tuple(round(v*.75) for v in c)
def main(base_style='darker'):
 tex=A/'textures/block/metal_anchor';tex.mkdir(parents=True,exist_ok=True)
 png(tex/'iron.png',32,16,iron)
 png(tex/'iron_base.png',32,16,lambda x,y:tuple(round(v*.86) for v in iron(x,y)))
 for name,factor in [('rivet',1.0),('rivet_base',.86)]:
  png(tex/(name+'.png'),4,4,lambda x,y,f=factor:tuple(round(v*.75) for v in shade(tuple(round(v*f) for v in (124,130,133)),7 if y==0 else -3 if y==3 or x in (0,3) else 2)))
 for c,value in COLORS.items():
  pigment=blend(rgb(value),(145,148,144),.16)
  def paint(x,y,pigment=pigment):
   # Worn paint edges reveal metal, never wood grain.
   edge=[6,6,7,7,6,5,5,6,7,7,6,6,5,5,6,6,7,7,6,5,5,6,7,7,6,5,5,6,6,7,6,6][x]
   if y>=edge:return cap_substrate(x,y//2)
   wear=.07+noise(x//3,y//2,70)/650
   if y>=edge-1:wear+=.35
   return blend(pigment,cap_substrate(x,y//2),wear)
  png(tex/f'paint_{c}.png',32,32,paint)
  png(tex/f'cap_{c}.png',16,16,lambda x,y,p=pigment:blend(p,cap_substrate(x*2,y),.10+noise(x//3,y//3,71)/650))
 old=A/'models/block/balloon_tether';new=A/'models/block/metal_balloon_tether'
 for p in old.glob('*.json'):
  if p.name.startswith('balloon'):continue
  m=json.loads(p.read_text())
  # Keep OBJ geometry shared. Redirect model parents only; no copied meshes.
  if 'parent' in m:m['parent']=m['parent'].replace('block/balloon_tether/','block/metal_balloon_tether/')
  textures=m.setdefault('textures',{})
  for key in ['wood','stripped','metal','particle']:textures[key]='builders_balloon:block/metal_anchor/iron'
  textures['metal']='builders_balloon:block/metal_anchor/'+('iron_base' if base_style=='darker' else 'iron')
  textures['rivet']='builders_balloon:block/metal_anchor/'+('rivet_base' if base_style=='darker' else 'rivet')
  for key in ['paint','paint_end']:
   if key in textures:textures[key]=textures[key].replace('block/anchor/','block/metal_anchor/')
  if p.name.startswith('weathering'):
   # No wood-specific wear overlay on metal; blockstates/items omit this pass.
   continue
  write(new/p.name,m)
 for name,oldname in [('metal_balloon_tether','balloon_tether'),('metal_bollard','bollard')]:
  state=json.loads((A/f'blockstates/{oldname}.json').read_text())
  state['multipart']=[p for p in state['multipart'] if '/weathering' not in p['apply']['model']]
  for part in state['multipart']:part['apply']['model']=part['apply']['model'].replace('block/balloon_tether/','block/metal_balloon_tether/')
  write(A/f'blockstates/{name}.json',state)
  for c in COLORS:
   m=json.loads((A/f'models/item/{oldname}_{c}.json').read_text());m['children'].pop('weathering',None)
   m['item_render_order']=[v for v in m['item_render_order'] if v!='weathering']
   m['textures']['particle']='builders_balloon:block/metal_anchor/iron'
   for child in m['children'].values():child['parent']=child['parent'].replace('block/balloon_tether/','block/metal_balloon_tether/')
   write(A/f'models/item/{name}_{c}.json',m)
   write(D/f'recipe/{name}_{c}.json',{'type':'minecraft:crafting_shapeless','category':'building','ingredients':[{'item':f'builders_balloon:{name}'},{'item':f'minecraft:{c}_dye'}],'result':result(name,c,1)})
  base=json.loads((A/f'models/item/{oldname}.json').read_text());base['parent']=f'builders_balloon:item/{name}_white'
  for v in base['overrides']:v['model']=v['model'].replace('item/'+oldname+'_','item/'+name+'_')
  write(A/f'models/item/{name}.json',base)
  loot=json.loads((D/f'loot_table/blocks/{oldname}.json').read_text());e=loot['pools'][0]['entries'][0];e['name']=f'builders_balloon:{name}';e['functions'][0]['block']=f'builders_balloon:{name}';write(D/f'loot_table/blocks/{name}.json',loot)
 for create in [False,True]:
  block='create:industrial_iron_block' if create else 'minecraft:iron_block';suffix='_from_create' if create else ''
  conditions=[{'type':'neoforge:mod_loaded','modid':'create'},{'type':'neoforge:item_exists','item':block}] if create else None
  for c in COLORS:
   recipe={'type':'minecraft:crafting_shaped','category':'building','pattern':[' W ','LGL',' I '],'key':{'W':{'item':f'minecraft:{c}_wool'},'L':{'tag':'builders_balloon:ropes'},'G':{'item':block},'I':{'item':'minecraft:iron_ingot'}},'result':result('metal_balloon_tether',c,1)}
   if conditions:recipe['neoforge:conditions']=conditions
   name=f'metal_balloon_tether_from_wool_{c}{suffix}';write(D/f'recipe/{name}.json',recipe);advancement(name,block,conditions)
  name='metal_bollard'+suffix
  recipe={'type':'minecraft:crafting_shaped','category':'building','pattern':['G','I'],'key':{'G':{'item':block},'I':{'item':'minecraft:iron_ingot'}},'result':result('metal_bollard','white',2 if create else 4)}
  if conditions:recipe['neoforge:conditions']=conditions
  write(D/f'recipe/{name}.json',recipe);advancement(name,block,conditions)
 for name in ['metal_balloon_tether','metal_bollard']:
  for c in COLORS:advancement(f'{name}_{c}',f'minecraft:{c}_dye',None)
 p=D/'tags/item/ropes.json';tag=json.loads(p.read_text());entry={'id':'quark:rope','required':False}
 if entry not in tag['values']:tag['values'].append(entry)
 write(p,tag)
 write(R/'data/create/tags/block/wrench_pickup.json',{'replace':False,'values':[f'builders_balloon:{n}' for n in ['balloon_tether','bollard','metal_balloon_tether','metal_bollard']]})
 write(R/'data/minecraft/tags/block/mineable/pickaxe.json',{'replace':False,'values':['builders_balloon:metal_balloon_tether','builders_balloon:metal_bollard']})
 p=A/'lang/en_us.json';lang=json.loads(p.read_text())
 for name,title in [('metal_balloon_tether','Metal Balloon Tether'),('metal_bollard','Metal Bollard')]:
  lang[f'block.builders_balloon.{name}']=title
  for c in COLORS:lang[f'item.builders_balloon.{name}.{c}']=c.replace('_',' ').title()+' '+title
 write(p,lang)
 print('Generated 16-colour metal models, separate recipes, wrench tag and optional Quark rope')
def result(name,color,count):return {'id':f'builders_balloon:{name}','count':count,'components':{'minecraft:block_state':{'color':color}}}
def advancement(name,item,conditions):
 data={'parent':'minecraft:recipes/root','criteria':{'ingredient':{'trigger':'minecraft:inventory_changed','conditions':{'items':[{'items':item}]}},'has_the_recipe':{'trigger':'minecraft:recipe_unlocked','conditions':{'recipe':f'builders_balloon:{name}'}}},'requirements':[['ingredient','has_the_recipe']],'rewards':{'recipes':[f'builders_balloon:{name}']}}
 if conditions:data['neoforge:conditions']=conditions
 write(D/f'advancement/recipes/{name}.json',data)
if __name__=='__main__':
 parser=argparse.ArgumentParser(description=__doc__)
 parser.add_argument('--base-style',choices=['matching','darker'],default='darker')
 main(parser.parse_args().base_style)
