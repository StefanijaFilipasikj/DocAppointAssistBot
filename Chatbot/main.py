from fastapi import FastAPI
from fastapi.responses import StreamingResponse
from dotenv import load_dotenv
from langchain_community.chat_models import ChatOpenAI
from langchain.schema import HumanMessage
import uvicorn
from fastapi.middleware.cors import CORSMiddleware
from langchain_community.vectorstores import Chroma
from langchain.tools import tool
from langchain.prompts import ChatPromptTemplate, MessagesPlaceholder
from langchain.agents import create_openai_functions_agent, AgentExecutor

from documents import documents, report_to_document
from embedding_model import embedding_model
from models import Message, MessagesList, PatientReport
from utils import convert_to_langchain_messages, generate_sentences, get_snomed_classes, generate_triplets, extract_triplets, create_triplet, get_patient_info, get_patient_info_sentence
from neo4j import GraphDatabase
import os

load_dotenv()

# Vectorstore
CHROMA_DB_DIR = "chroma_medembed_db"

vectorstore = Chroma(
    persist_directory=CHROMA_DB_DIR,
    embedding_function=embedding_model,
)
driver = GraphDatabase.driver("bolt://localhost:7687", auth=("neo4j", os.getenv("NEO4J_PASSWORD")))

# Tools
def get_patient_context(query: str, patient_id: str) -> str:
    retriever = vectorstore.as_retriever(
        search_kwargs={"k": 4, "filter": {"patient_id": patient_id}}
    )
    docs = retriever.get_relevant_documents(query)
    return f"\n\n".join([f"Metadata={doc.metadata} \n{doc.page_content}" for doc in docs])

@tool
def patient_medical_search(query: str, patient_id: str) -> str:
    """Search a patient's medical history for information relevant to the query."""
    return get_patient_context(query, patient_id)

@tool
def patient_graph_medical_search(patient_id: str) -> str:
    """Search all patient's medical history."""
    with driver.session() as session:
        facts = session.execute_read(get_patient_info, patient_id)
    return facts

@tool
def patient_graph_medical_search_query(query: str, patient_id: str) -> str:
    """Search a patient's medical history for information relevant to the query."""
    with driver.session() as session:
        facts = session.execute_read(get_patient_info_sentence, patient_id, query)
    return facts

# FastAPI app
app = FastAPI()

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)


@app.post("/chat")
async def chat_endpoint(messages: MessagesList):
    llm = ChatOpenAI(streaming=True, temperature=0)
    system_message = "Use tools when needed. Dont make up information that is not from the medical history. If you cant find the information just say that there is not information. Dont generate bulletpoints or other symbols. Don't ask the user for the patient_id, this is it: " + messages.patient_id
    if messages.role == "patient":
        system_message = "You are a helpful medical assistant that is talking to a patient. " + system_message
    else:
        system_message = "You are a helpful medical assistant that is talking to a doctor. " + system_message

    prompt = ChatPromptTemplate.from_messages([
        ("system", system_message),
        MessagesPlaceholder(variable_name="chat_history"),
        ("human", "{input}"),
        MessagesPlaceholder(variable_name="agent_scratchpad"),
    ])

    agent = create_openai_functions_agent(
        llm=llm,
        tools=[patient_graph_medical_search, patient_graph_medical_search_query],
        prompt=prompt
    )

    agent_executor = AgentExecutor.from_agent_and_tools(
        agent=agent,
        tools=[patient_graph_medical_search, patient_graph_medical_search_query],
        verbose=True
    )
    
    messages = convert_to_langchain_messages(messages.messages)

    chat_history = messages[:-1]
    input_message = messages[-1].content

    agent_input = {"input": input_message,"chat_history": chat_history}

    agent_response = await agent_executor.ainvoke(agent_input)

    async def token_stream():
        stream_llm = ChatOpenAI(streaming=True, temperature=0)
        async for chunk in stream_llm.astream([HumanMessage(content="Repeat this sentence: " + agent_response["output"])]):
            if chunk.content:
                yield f"{chunk.content}\n"

    return StreamingResponse(token_stream(), media_type="text/plain")

@app.post("/embed")
async def insert_report(report: PatientReport):
    """Insert a report to the knowledge graph"""
    try:
        text = report.text
        patient_id = report.patient_id + "/" + report.date
        sentences = generate_sentences(text)
        response = generate_triplets(sentences, text, patient_id)
        triplets = extract_triplets(response, patient_id)

        with driver.session() as session:
            for subj, rel, obj in triplets:
                session.execute_write(create_triplet, subj, rel, obj)
    except Exception as e:
        return False
    print(f"Document embedded: {report}")
    return True

@app.get("/health")
async def health():
    """Check the api is running"""
    return {"status": "🤙"}

if __name__ == "__main__":
    if(len(vectorstore) == 0):
        vectorstore.add_documents(documents)

    uvicorn.run(
        "main:app",
        host="localhost",
        port=8000,
        reload=True
    )