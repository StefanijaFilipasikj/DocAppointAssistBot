from sentence_transformers import SentenceTransformer
from langchain.embeddings.base import Embeddings
from typing import List

class SentenceTransformerEmbeddings(Embeddings):
    def __init__(self, model_name: str):
        self.model = SentenceTransformer(model_name)

    def embed_documents(self, texts: List[str]) -> List[List[float]]:
        return self.model.encode(texts, show_progress_bar=False, convert_to_numpy=True).tolist()

    def embed_query(self, text: str) -> List[float]:
        return self.model.encode(text, convert_to_numpy=True).tolist()
    

embedding_model = SentenceTransformerEmbeddings("abhinand/MedEmbed-large-v0.1")