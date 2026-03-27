##########################
# Input
notes_dir = r'./output/local' # Directory where you have all you DeidNote-xxxxx-of-xxxxxx files
output_filename = r'./notes_deid.json' # Output merged jsonl file path
###########################

import os
import json
if os.path.exists(output_filename):
    os.remove(output_filename)
for root, _, files in os.walk(notes_dir): # walk through notes_dir and all subfolders
    for file in files:
        file_path = os.path.join(root, file)
        if 'DeidNote' in os.path.basename(file_path): # if filename contains 'DeidNote'
            print(f"Processing: {file_path}")
            with open(output_filename, 'a', encoding='utf-8') as output_f:
                with  open(file_path, "r", encoding='utf-8') as json_file:
                    data = json_file.read()
                data = data.replace('}\n{', '}\n\n{') # to avoid special char \n contaminate result
                data = data.split('\n\n')
                for i in data:
                    if i:
                        current_record = json.loads(i)
                        result = {
                            "note_id": current_record['note_id'] , # need to be align with field in Tide output file
                            "note": current_record['TEXT_DEID_note_text'] # need to be align with field in Tide output file
                        }
                        output_f.write(json.dumps(result)+'\n')
