import json
import os
import glob
from tqdm import tqdm
import dask.dataframe as dd
import pandas as pd

#def tide2jsonl(tide_output_dir: str, jsonl_output_dir: str):
#    """
#    Method to transform TiDE JSON output into JSONL format required by DOCCANO
#    """
#tide_output_dir = "/opt/deid/original"
#jsonl_output_dir = "opt/deid/transform"

base_tide_output_dir = "/opt/deid/final_output"
latestValue = ""
with open(base_tide_output_dir+"/latest.txt", "r") as infile:
    latestValue = infile.readline()
tide_output_dir = base_tide_output_dir + "/" + latestValue
jsonl_output_dir = base_tide_output_dir + latestValue + "/transform/"

#tide_output_dir = "C:\\Official\\Dev\\SHC\\TiDE\\2021-07-09\\main\\tide\\local_deid\\original"
#jsonl_output_dir = "C:\\Official\\Dev\\SHC\\TiDE\\2021-07-09\\main\\tide\\local_deid\\transform"
if not os.path.exists(jsonl_output_dir):
    os.makedirs(jsonl_output_dir)

file_list = glob.glob(f"{tide_output_dir}/DeidNote-*")
print(file_list)
for file in tqdm(file_list):
    tide_output_df = dd.read_json(file,
                                  orient='records',
                                  typ='frame',
                                  lines=True).compute()

    # finding_set = set()
    # This mapping is tied to deid_config_omop_genrep.yaml
    findings_mapping = {'general-accession': 'ACCESSION',
                        'accession_num': 'ACCESSION',
                        'patient_mrn': 'MRN',
                        'mrn': 'MRN',
                        'general_name': 'NAME',
                        'patient_name': 'NAME',
                        'other_name': 'NAME',
                        'care_provider_name': 'NAME',
                        'mergency_contact': 'NAME',
                        'ner-PERSON': 'NAME',
                        'ner-LOCATION': 'LOCATION',
                        'location': 'LOCATION',
                        'other_address': 'LOCATION',
                        'patient_address': 'LOCATION',
                        'mergency_contact_address': 'LOCATION',
                        'patient_phone': 'PHONE',
                        'mergency_contact_phone': 'PHONE',
                        'general-phone': 'PHONE',
                        'phi_date': 'DATE',
                        'general-account': 'OTHER_INDIRECT',
                        'other_id': 'OTHER_INDIRECT',
                        'general': 'OTHER_INDIRECT',
                        'general_number': 'OTHER_INDIRECT',
                        'patient_ssn': 'SSN',
                        'patient_email': 'EMAIL/URL',
                        'other_email': 'EMAIL/URL',
                        'public_id': 'OTHER_DIRECT',
                        'family_id': 'OTHER_DIRECT',
                        'general-email': 'EMAIL/URL',
                        }
    text = []
    label = []
    note_source_value = []
    id = []
    for i, note_df in tide_output_df.iterrows():
        tide_findings = pd.DataFrame(
            json.loads(note_df['FINDING_note']))

        tide_findings.drop_duplicates(subset=['start', 'end'],
                                      inplace=True,
                                      ignore_index=True)

        # finding_set.update(tide_findings['type'].unique().tolist())

        findings_list = [[finding['start'],
                          finding['end'],
                          findings_mapping[finding['type']]] \
                         for j, finding in tide_findings.iterrows()]

        text.append(note_df['note'])
        label.append(findings_list)
        note_source_value.append(note_df['note'])
        id.append(note_df['id'])

    df = pd.DataFrame({'text': text,
                       'label': label,
                       'note_source_value': note_source_value,
                       'id': id})

    filename = os.path.basename(file)
    df.to_json(path_or_buf=f'{jsonl_output_dir}/{filename}.json',
               orient='records',
               lines=True)