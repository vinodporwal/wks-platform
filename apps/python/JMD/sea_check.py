from pprint import pprint
from engine.ods_norms_reader import ODSNormsReader

r = ODSNormsReader.get_reader('DTA', 4, 2026)

d = r.get_consumption_norms(include_all_accounts=False)
keys = [k for k in d if 'SEA' in k.upper() or 'WATER' in k.upper()]
print('=== consumption keys matching SEA/WATER ===')
print('\n'.join(keys))

print('\n=== consumption norms for each key ===')
for k in keys:
    print('\n---', k, '---')
    pprint(d[k])

mx = r.get_u4u_norms_matrix()
print('\n=== u4u matrix all keys ===')
for k, v in mx.items():
    if 'SEA' in str(k).upper() or 'WATER' in str(k).upper():
        print(k, '->', v)

gen = r.get_bpc_generation_quantities()
print('\n=== bpc generation keys matching SEA/WATER ===')
print('\n'.join(str(k) for k in gen if 'SEA' in str(k).upper() or 'WATER' in str(k).upper()))

try:
    print('\n=== bpc quantities for Sea Water ===')
    pprint(r.get_bpc_quantities('Sea Water'))
except Exception as e:
    print('get_bpc_quantities("Sea Water") error:', e)
