import pickle, sys, os

sys.path.insert(0, r'c:\Users\shrik\Desktop\Project\fork repo\development\New\JMD new python script\wks-platform\apps\python\JMD')

from engine.calculator import run_month

res = run_month(
    plant_id='A4AF8441-73AD-4F9F-BCF4-6734E8202F7A',
    month=4,
    year=2026,
    save_to_db=False,
)

pickle.dump(res, open('u4u_dump_apr2026.pkl', 'wb'))
print('DUMP_DONE')
