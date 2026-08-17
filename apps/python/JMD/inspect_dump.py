import pickle, json, sys

res = pickle.load(open('u4u_dump_apr2026.pkl','rb'))
u4u = res.get('u4u_iteration') or {}

# Save a JSON version for easier diff
with open('u4u_dump_apr2026.json','w') as f:
    json.dump({
        'power_result': u4u.get('final_power_result'),
        'steam_result': u4u.get('final_steam_result'),
        'final_total_demands': u4u.get('final_total_demands'),
        'final_u4u_demands': u4u.get('final_u4u_demands'),
        'final_dynamic_table': u4u.get('final_dynamic_table'),
        'final_detail_records': u4u.get('final_detail_records'),
        'all_consumption_norms': u4u.get('all_consumption_norms'),
    }, f, indent=2, default=str)

print('JSON done')

# Print distribution utility rows from dynamic table
dt = u4u.get('final_dynamic_table',[])
print('\n--- Dynamic table _Dis rows ---')
for r in dt:
    if r['producer'].endswith('_Dis') or r['producer_utility'].endswith('_Dis'):
        print(r['producer'], r['producer_utility'], r['generation'], r['material'], r['quantity'], r['bpc_quantity'] if 'bpc_quantity' in r else '-')

# Print power assets
pr = u4u.get('final_power_result',{})
print('\n--- Power assets ---')
for a in pr.get('assets',[]):
    print(a.get('asset_name'), a.get('dispatched_mwh'))

# Print steam assets
sr = u4u.get('final_steam_result',{})
print('\n--- Steam assets ---')
for a in sr.get('assets',[]):
    print(a.get('asset_name'), a.get('asset_type'), a.get('total_output_mt'))

# Print PRDS detail records
print('\n--- PRDS detail records ---')
for r in u4u.get('final_detail_records',[]):
    if 'PRDS' in r.get('producer','').upper():
        print(r['producer'], r['generation'], r['material'], r['quantity'])
