import sqlite3
import json
import os
from datetime import datetime

db_path = r'c:\Users\devon\Projects\Phill\db_dump\phill.db'
output_path = r'c:\Users\devon\Projects\Phill\db_dump\full_dump.json'

print(f'DB size: {os.path.getsize(db_path)} bytes')

conn = sqlite3.connect(db_path)
conn.row_factory = sqlite3.Row
cur = conn.cursor()

# Get all tables
cur.execute("SELECT name FROM sqlite_master WHERE type='table' AND name NOT LIKE 'sqlite_%' AND name NOT LIKE 'room_%' ORDER BY name")
tables = [r[0] for r in cur.fetchall()]
print(f'Tables found: {len(tables)}')
print(f'Table names: {tables}')
print()

all_data = {}

for table in tables:
    cur.execute(f'SELECT COUNT(*) FROM [{table}]')
    count = cur.fetchone()[0]
    print(f'=== {table} ({count} rows) ===')
    
    # Get column info
    cur.execute(f'PRAGMA table_info([{table}])')
    columns = [(r[1], r[2]) for r in cur.fetchall()]
    col_names = [c[0] for c in columns]
    print(f'  Columns: {col_names}')
    
    # Get all rows
    cur.execute(f'SELECT * FROM [{table}]')
    rows = cur.fetchall()
    
    table_data = []
    for row in rows:
        row_dict = {}
        for i, col in enumerate(col_names):
            val = row[i]
            # Try to convert epoch timestamps to readable dates for display
            if col.endswith('_epoch') or col.endswith('Epoch') or col in ('clockInEpoch', 'clockOutEpoch', 'scheduledStartEpoch', 'scheduledEndEpoch', 'createdAtEpoch', 'completedAtEpoch', 'paidAtEpoch', 'dateEpoch', 'lastMessageEpoch', 'timestampEpoch', 'sharedAtEpoch'):
                if val and isinstance(val, (int, float)) and val > 1000000000:
                    try:
                        # Try milliseconds first
                        if val > 10000000000:
                            dt = datetime.fromtimestamp(val / 1000)
                        else:
                            dt = datetime.fromtimestamp(val)
                        row_dict[col] = val
                        row_dict[col + '_readable'] = dt.strftime('%Y-%m-%d %H:%M:%S')
                    except:
                        row_dict[col] = val
                else:
                    row_dict[col] = val
            else:
                row_dict[col] = val
        table_data.append(row_dict)
        
        # Print each row
        print(f'  Row: {json.dumps(row_dict, indent=2, default=str)}')
    
    all_data[table] = {
        'count': count,
        'columns': columns,
        'rows': table_data
    }
    print()

# Save full dump to JSON
with open(output_path, 'w', encoding='utf-8') as f:
    json.dump(all_data, f, indent=2, default=str)

print(f'\nFull dump saved to: {output_path}')

# Summary
print('\n=== SUMMARY ===')
for table, data in all_data.items():
    print(f'  {table}: {data["count"]} rows')

conn.close()
