import re
import sys

def add_nolock_hints(sql_content):
    """Add WITH(NOLOCK) hints to all SELECT queries in SQL stored procedures."""
    
    # Pattern 1: FROM/JOIN dbo.TableName alias
    # Match: FROM dbo.TableName alias -> FROM dbo.TableName WITH(NOLOCK) alias
    sql_content = re.sub(
        r'(?i)\b(FROM|(?:INNER|LEFT|RIGHT|OUTER)\s+JOIN)\s+(dbo\.[a-zA-Z0-9_]+)\s+(?!WITH\s*\()([a-zA-Z_][a-zA-Z0-9_]*)\b',
        r'\1 \2 WITH(NOLOCK) \3',
        sql_content
    )
    
    # Pattern 2: FROM/JOIN TableName alias (without dbo prefix)
    # Match: FROM TableName alias -> FROM TableName WITH(NOLOCK) alias
    sql_content = re.sub(
        r'(?i)\b(FROM|(?:INNER|LEFT|RIGHT|OUTER)\s+JOIN)\s+([A-Z][a-zA-Z0-9_]+)\s+(?!WITH\s*\()([a-z][a-zA-Z0-9_]*)\b',
        r'\1 \2 WITH(NOLOCK) \3',
        sql_content
    )
    
    # Pattern 3: FROM/JOIN dbo.TableName (no alias, followed by WHERE/ORDER/GROUP/etc)
    sql_content = re.sub(
        r'(?i)\b(FROM|(?:INNER|LEFT|RIGHT|OUTER)\s+JOIN)\s+(dbo\.[a-zA-Z0-9_]+)\s+(?!WITH\s*\()(?=WHERE|ORDER|GROUP|INNER|LEFT|RIGHT|CROSS|\))',
        r'\1 \2 WITH(NOLOCK) ',
        sql_content
    )
    
    # Pattern 4: FROM/JOIN TableName (no dbo, no alias)
    sql_content = re.sub(
        r'(?i)\b(FROM|(?:INNER|LEFT|RIGHT|OUTER)\s+JOIN)\s+([A-Z][a-zA-Z0-9_]+)\s+(?!WITH\s*\()(?=WHERE|ORDER|GROUP|INNER|LEFT|RIGHT|CROSS|\))',
        r'\1 \2 WITH(NOLOCK) ',
        sql_content
    )
    
    # Pattern 5: CROSS APPLY (dbo.TableName) or CROSS APPLY TableName
    sql_content = re.sub(
        r'(?i)\bCROSS\s+APPLY\s+\(\s*SELECT.*?FROM\s+(dbo\.[a-zA-Z0-9_]+)\s+(?!WITH\s*\()',
        lambda m: m.group(0).replace(m.group(1), m.group(1) + ' WITH(NOLOCK)'),
        sql_content
    )
    
    return sql_content

# Read from stdin or file
if len(sys.argv) > 1:
    with open(sys.argv[1], 'r', encoding='utf-8') as f:
        content = f.read()
else:
    content = sys.stdin.read()

# Process
result = add_nolock_hints(content)

# Write to stdout or file
if len(sys.argv) > 2:
    with open(sys.argv[2], 'w', encoding='utf-8') as f:
        f.write(result)
else:
    print(result)
