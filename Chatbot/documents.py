from langchain.schema import Document
from models import PatientReport

documents = [
    Document(
        page_content=(
            "PAT001 presented with a 2-week history of persistent headaches localized to the frontal region, "
            "accompanied by intermittent nausea and photophobia. No history of trauma or recent illness. "
            "Neurological exam unremarkable. Recommended MRI to rule out secondary causes. Patient advised to "
            "reduce screen time and increase hydration. Prescribed ibuprofen 400mg as needed for pain relief."
        ),
        metadata={"patient_id": "PAT001", "date": "2024-06-01"}
    ),
    Document(
        page_content=(
            "Follow-up visit for PAT001 regarding persistent headaches. MRI results showed no abnormalities. "
            "Symptoms have improved slightly with ibuprofen and lifestyle modifications. Patient reports fewer "
            "episodes and no longer experiences nausea. Recommended continuation of current regimen and scheduled "
            "a follow-up in 3 weeks to monitor progress."
        ),
        metadata={"patient_id": "PAT001", "date": "2024-06-08"}
    ),
    Document(
        page_content=(
            "PAT002 seen for elevated blood pressure, measured at 162/98 mmHg. Patient has a history of obesity "
            "and poor diet. No signs of end-organ damage at present. Initiated lifestyle counseling and prescribed "
            "lisinopril 10mg daily. Referred for nutritionist consultation. Blood work ordered to assess lipid profile, "
            "renal function, and HbA1c levels."
        ),
        metadata={"patient_id": "PAT002", "date": "2024-06-03"}
    ),
    Document(
        page_content=(
            "PAT002 returns with complaints of dizziness and fatigue, especially in the mornings. Recent labs show "
            "slight drop in potassium levels (3.2 mmol/L). Lisinopril dosage reduced to 5mg. Encouraged increased "
            "fluid intake and potassium-rich foods. ECG and Holter monitor scheduled to rule out arrhythmias."
        ),
        metadata={"patient_id": "PAT002", "date": "2024-06-10"}
    ),
    Document(
        page_content=(
            "PAT003 evaluated following a rear-end vehicle collision. Reports neck pain and stiffness but no loss of "
            "consciousness or neurological symptoms. Cervical spine x-ray shows no fracture. Diagnosed with mild "
            "whiplash injury. Advised rest, cold compress, and over-the-counter analgesics. Fitted with soft cervical collar. "
            "Scheduled follow-up to assess improvement."
        ),
        metadata={"patient_id": "PAT003", "date": "2024-06-02"}
    ),
    Document(
        page_content=(
            "Follow-up for PAT003’s whiplash injury. Patient reports reduced pain but persistent neck stiffness with limited "
            "range of motion. Referred to physiotherapy for cervical mobility exercises and manual therapy. No signs of "
            "neurological impairment. Advised to gradually wean off cervical collar use and avoid high-impact activities."
        ),
        metadata={"patient_id": "PAT003", "date": "2024-06-09"}
    ),
    Document(
        page_content=(
            "Initial visit for PAT004 following routine screening revealed elevated fasting glucose levels (145 mg/dL) and "
            "HbA1c of 7.3%. Diagnosed with type 2 diabetes mellitus. Initiated metformin 500mg twice daily. Provided patient "
            "education on dietary changes, physical activity, and self-monitoring of blood glucose. Scheduled diabetes nurse "
            "education session and ophthalmology referral."
        ),
        metadata={"patient_id": "PAT004", "date": "2024-06-05"}
    ),
    Document(
        page_content=(
            "Follow-up consultation for PAT004. Patient tolerating metformin well with no gastrointestinal side effects. "
            "Glucose logs show improvement with average fasting levels between 100-115 mg/dL. Weight reduced by 2kg. "
            "Encouraged continued adherence to diet and exercise. Will recheck HbA1c in 3 months. No diabetic retinopathy "
            "found during recent ophthalmology screening."
        ),
        metadata={"patient_id": "PAT004", "date": "2024-06-12"}
    ),
    Document(
        page_content=(
            "PAT005 reports chest tightness during intense physical exertion, described as a dull ache radiating to the left "
            "shoulder. Symptoms resolve with rest. No prior cardiac history. Resting ECG normal. Ordered stress echocardiogram "
            "and cardiac enzymes to evaluate for exertional angina. Advised patient to avoid strenuous activity until further evaluation."
        ),
        metadata={"patient_id": "PAT005", "date": "2024-06-04"}
    ),
    Document(
        page_content=(
            "PAT005 stress echo results normal with no evidence of ischemia. Cardiac enzymes within normal limits. "
            "Symptoms likely musculoskeletal or deconditioning-related. Cleared for gradual return to physical activity "
            "with low-impact exercise. Recommended stretching and physical therapy for shoulder pain management."
        ),
        metadata={"patient_id": "PAT005", "date": "2024-06-11"}
    ),
]

def report_to_document(report: PatientReport):
    return Document(
        page_content=(report.text),
        metadata={"patient_id": report.patient_id, "date": report.date}
    )
