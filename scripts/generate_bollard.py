"""Generate alpha.22 decorative models/recipes and optional rope ingredients; no new artwork."""
from pathlib import Path
import json,copy
ROOT=Path(__file__).resolve().parents[1];R=ROOT/'src/main/resources';A=R/'assets/builders_balloon';D=R/'data/builders_balloon'
COLORS='white orange magenta light_blue yellow lime pink gray light_gray cyan purple blue brown green red black'.split()
def write(path,data):path.parent.mkdir(parents=True,exist_ok=True);path.write_text(json.dumps(data,indent=2)+'\n')
def main():
 state=json.loads((A/'blockstates/balloon_tether.json').read_text())
 for p in state['multipart']:
  if 'connected' in p['when']:p['when']['roped']=p['when'].pop('connected')
 write(A/'blockstates/bollard.json',state)
 base=json.loads((A/'models/item/balloon_tether.json').read_text());base['parent']='builders_balloon:item/balloon_tether_white';write(A/'models/item/balloon_tether.json',base)
 decor=copy.deepcopy(base);decor['parent']='builders_balloon:item/bollard_white'
 for v in decor['overrides']:v['model']=v['model'].replace('item/balloon_tether_','item/bollard_')
 write(A/'models/item/bollard.json',decor)
 for color in COLORS:
  m=json.loads((A/f'models/item/balloon_tether_{color}.json').read_text())
  m['children']['anchor']['visibility']['Rope']=False;m['children'].pop('contact_shadow');m['item_render_order'].remove('contact_shadow')
  write(A/f'models/item/bollard_{color}.json',m)
  write(D/f'recipe/bollard_{color}.json',{'type':'minecraft:crafting_shapeless','category':'building','ingredients':[{'item':'builders_balloon:bollard'},{'item':f'minecraft:{color}_dye'}],'result':{'id':'builders_balloon:bollard','count':1,'components':{'minecraft:block_state':{'color':color}}}})
  p=D/f'recipe/balloon_tether_from_wool_{color}.json';recipe=json.loads(p.read_text());recipe['key']['L']={'tag':'builders_balloon:ropes'};write(p,recipe)
 write(D/'recipe/bollard.json',{'type':'minecraft:crafting_shaped','category':'building','pattern':['G','I'],'key':{'G':{'tag':'minecraft:logs'},'I':{'item':'minecraft:iron_ingot'}},'result':{'id':'builders_balloon:bollard','count':2,'components':{'minecraft:block_state':{'color':'white'}}}})
 for name in ['bollard']+[f'bollard_{c}' for c in COLORS]:
  ingredients={'bollard':'minecraft:logs'} if name=='bollard' else {'dye':f'minecraft:{name[8:]}_dye'}
  criteria={k:{'trigger':'minecraft:inventory_changed','conditions':{'items':[{'items':'#'+v if v=='minecraft:logs' else v}]}} for k,v in ingredients.items()}
  criteria['has_the_recipe']={'trigger':'minecraft:recipe_unlocked','conditions':{'recipe':f'builders_balloon:{name}'}}
  write(D/f'advancement/recipes/{name}.json',{'parent':'minecraft:recipes/root','criteria':criteria,'requirements':[list(criteria)],'rewards':{'recipes':[f'builders_balloon:{name}']}})
 loot=json.loads((D/'loot_table/blocks/balloon_tether.json').read_text())
 entry=loot['pools'][0]['entries'][0];entry['name']='builders_balloon:bollard';entry['functions'][0]['block']='builders_balloon:bollard'
 write(D/'loot_table/blocks/bollard.json',loot)
 write(D/'tags/item/ropes.json',{'replace':False,'values':['minecraft:lead',{'id':'supplementaries:rope','required':False},{'id':'farmersdelight:rope','required':False},{'id':'quark:rope','required':False}]})
 p=R/'data/minecraft/tags/block/mineable/axe.json';tag=json.loads(p.read_text())
 if 'builders_balloon:bollard' not in tag['values']:tag['values'].append('builders_balloon:bollard')
 write(p,tag)
 p=A/'lang/en_us.json';lang=json.loads(p.read_text());lang['block.builders_balloon.bollard']='Bollard'
 for c in COLORS:lang[f'item.builders_balloon.bollard.{c}']=c.replace('_',' ').title()+' Bollard'
 write(p,lang)
if __name__=='__main__':main()
