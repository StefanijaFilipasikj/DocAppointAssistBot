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
from utils import convert_to_langchain_messages

load_dotenv()

# Vectorstore
CHROMA_DB_DIR = "chroma_medembed_db"

vectorstore = Chroma(
    persist_directory=CHROMA_DB_DIR,
    embedding_function=embedding_model,
)

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
    system_message = "Use tools when needed. Dont make up information that is not from the medical history. If you cant find the information just say that there is not information. Dont generate bulletpoints or other symbols. Use this patient_id in the tools: " + messages.patient_id
    if messages.role == "patient":
        system_message = "You are a helpful medical assistant for a patient. " + system_message
    else:
        system_message = "You are a helpful medical assistant for a doctor. " + system_message

    prompt = ChatPromptTemplate.from_messages([
        ("system", system_message),
        MessagesPlaceholder(variable_name="chat_history"),
        ("human", "{input}"),
        MessagesPlaceholder(variable_name="agent_scratchpad"),
    ])

    agent = create_openai_functions_agent(
        llm=llm,
        tools=[patient_medical_search],
        prompt=prompt
    )

    agent_executor = AgentExecutor.from_agent_and_tools(
        agent=agent,
        tools=[patient_medical_search],
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
async def health(report: PatientReport):
    """Embed a document"""
    try:
        document = report_to_document(report)
        vectorstore.add_documents([document])
    except Exception as e:
        return False
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