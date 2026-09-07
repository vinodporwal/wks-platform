import csv, os, re, glob

path = r'c:\Users\shrik\Desktop\Project\fork repo\development\New\JMD new python script\wks-platform\apps\python\files\Norm, Qty, Cost .csv'

with open(path, 'r', encoding='utf-16') as f:
    for _ in range(4):  # skip FY/Q1/month sub-headers
        f.readline()
    reader = csv.DictReader(f, delimiter='\t')
    # strip keys
    reader.fieldnames = [n.strip() if n else '' for n in reader.fieldnames]
    for r in list(reader):
        u = r.get('Utility', '').strip()
        m = r.get('Material', '').strip()
        if 'SEA' in u.upper() or 'SEA' in m.upper() or 'DESAL' in u.upper() or 'DESAL' in m.upper():
            print('---')
            for k in ('Generating Plant','Generating Plant ID','Utility','Utility ID','UOM','Account','Material','Material ID','Issuing Plant','Issuing Plant ID','UOM.1'):
                print(f'{k}: {r.get(k,"")!r}')
            # print April norms/qty/price (first block)
            for k in ('Norms','Quantity','Amount (Rs.)','Price'):
                print(f'April {k}: {r.get(k,"")!r}')
