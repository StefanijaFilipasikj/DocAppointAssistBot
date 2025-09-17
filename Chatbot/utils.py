from typing import List
from langchain.schema import HumanMessage, AIMessage, SystemMessage, BaseMessage
from models import Message
from ollama import chat
from prompts import example_report, example_sentences
from sentence_transformers import SentenceTransformer
import numpy as np
import json

EMBEDDINGS_FILE_PATH = "data/embeddings.npy"
LABELS_FILE_PATH = "data/labels.json"

def convert_to_langchain_messages(messages: List[Message]) -> List[BaseMessage]:
    role_map = {
        "USER": HumanMessage,
        "CHATBOT": AIMessage,
        "SYSTEM": SystemMessage
    }
    return [role_map[msg.role](content=msg.content) for msg in messages]

def generate(system_message: str, user_message: str, model: str):
    return chat(
        model=model, 
        messages =[
        {"role": "system", "content": system_message},
        {"role": "user", "content": user_message}],
        options={
            "temperature": 0
        }
    )

def generate_sentences(report):
  print("Generating sentences")
  system_message = f"""You are assistant for extracting information from medical reports."""

  human_message = f"""Your job is to extract information in simple and short sentences from this report.
The information needs to be describing something about the patient.
Only respond with the short and simple object descriptions seperated only by a new line.
Give at least 10 sentences. Respond only with the sentences.

Example:
Report:
{example_report}

Sentences:
{example_sentences}

Begin!
Report:
{report}
"""

  result = generate(system_message, human_message, model="llama3.2:3b-instruct-q4_K_S")
  return result.message.content.split("\n")

def get_snomed_classes(sen: str, top_k=20):
    print("Fetching relevant snomed classes")
    with open(LABELS_FILE_PATH, 'r') as f:
        labels = json.load(f)

    embeddings = np.load(EMBEDDINGS_FILE_PATH)
    model = SentenceTransformer("abhinand/MedEmbed-large-v0.1")
    
    sen_emb = model.encode(sen)

    sen_emb_norm = sen_emb / np.linalg.norm(sen_emb)
    embeddings_norm = embeddings / np.linalg.norm(embeddings, axis=1, keepdims=True)

    similarities = np.dot(embeddings_norm, sen_emb_norm)

    top_k_indices = np.argsort(similarities)[::-1][:top_k]

    classes = [labels[i] for i in top_k_indices]
    return classes

def generate_triplets(sentences, report, patient_name, print_results=False):
  print("Generating triplets")
  system_message = f"""You are a powerfull model for creating triplets and giving a simple and short working out."""
  results = []

  for sen in sentences:
    classes = get_snomed_classes(sen)

    human_message = f"""You need to create a triplet based on a sentence and some relevant classes.
Pick the most relevant class and create a triplet. Never make up information, use only from the context and the given sentence.
Never make up new classes, use only the given ones. If no class is relevant return <SOLUTION>None</SOLUTION>

Think about the problem and provide a short and simple working out.
Place it between <start_working_out> and <end_working_out>.
Then, provide your triplet between <SOLUTION></SOLUTION>

Example response 1:
Sentence:
male patient
Classes:
['Female', 'Age more than 50 years, male', 'Male', 'Well male adult', 'Age 60 to 64 years']
<start_working_out>Based on the text the patient appears to be male<end_working_out>
<SOLUTION>'Patient 2' 'is' 'Male'</SOLUTION>

Example respone 2:
Sentence:
wrist pain from motor vehicle accident
Classes:
['Pain in wrist', 'Injury of wrist', 'Other wrist injuries', 'Wrist joint tender', 'Wrist joint pain']
<start_working_out>The patient had some pain in the wrist from the motor accident.<end_working_out>
<SOLUTION>'Patient 2' 'has' 'Pain in wrist'</SOLUTION>

Example response 3:
Sentence:
positive COVID-19 result
Classes:
['Schick test positive', 'Pregnancy test positive', 'HIV positive', 'Hepatitis A test positive']
<start_working_out>The relevant class is not listed. None of the classes are relevant.<end_working_out>
<SOLUTION>None</SOLUTION>

Begin!
Given the context for patient '{patient_name}':
{report}
Sentence:
{sen}

Classes:
{classes}"""

    result = generate(system_message, human_message, "lastmass/Qwen3_Medical_GRPO:latest")
    if print_results:
      print(sen)
      print(result.message.content)
      print(classes)
      print()
    results.append(result.message.content)
  return results

def extract_triplets(results, patient_name):
  print("Extracting triplets")
  triplets = []
  for result in results:
    triplet = []
    for word in result.split("<SOLUTION>")[1].split("</SOLUTION>")[0].split("'"):
      if word.strip() != '':
        triplet.append(word)
    if len(tuple(triplet)) == 3:
      triplet[0] = patient_name
      triplets.append(tuple(triplet))
  return triplets

def create_triplet(tx, subj, rel, obj):
  print("Saving triplets")
  query = f"""
  MERGE (s:Entity {{name: $subj}})
  MERGE (o:Entity {{name: $obj}})
  MERGE (s)-[r:{rel.upper().replace(" ", "_")}]->(o)
  """
  tx.run(query, subj=subj, obj=obj)

def get_patient_info(tx, patient_id):
  query = """
  MATCH (p:Entity)-[r]->(o:Entity)
  WHERE toLower(p.name) CONTAINS toLower($patient_id)
  RETURN p.name AS subject, type(r) AS relation, o.name AS object
  """
  result = tx.run(query, patient_id=patient_id)
  return [(record["subject"], record["relation"], record["object"]) for record in result]

def get_patient_info_sentence(tx, patient_id, sentence):
  relevant_calsses = get_snomed_classes(sentence)

  query = """
MATCH (p:Entity)-[r]->(o:Entity)
WHERE toLower(p.name) CONTAINS toLower($patient_id)
  AND o.name IN $objects_list
RETURN p.name AS subject, type(r) AS relation, o.name AS object
"""
  result = tx.run(query, patient_id=patient_id, objects_list=relevant_calsses)
  return [(record["subject"], record["relation"], record["object"]) for record in result]