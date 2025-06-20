package mk.ukim.finki.docappointassistbot

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognizerIntent
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import mk.ukim.finki.docappointassistbot.adapter.ChatRecyclerAdapter
import mk.ukim.finki.docappointassistbot.databinding.FragmentChatbotBinding
import mk.ukim.finki.docappointassistbot.domain.MessagesModel
import mk.ukim.finki.docappointassistbot.domain.enums.ChatRole
import mk.ukim.finki.docappointassistbot.ui.viewModels.MessagesViewModel
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.util.Locale

class ChatbotFragment : Fragment() {

    private var _binding: FragmentChatbotBinding? = null
    private val binding get() = _binding!!
    private var patientId: String? = null
    private var role: String? = null

    private lateinit var chatAdapter: ChatRecyclerAdapter
    private lateinit var viewModel: MessagesViewModel

    companion object {
        private const val ARG_PATIENT_ID = "id"
        private const val ARG_ROLE = "role"

        fun newInstance(patientId: String?, role: String?): ChatbotFragment {
            val fragment = ChatbotFragment()
            val args = Bundle()
            args.putString(ARG_PATIENT_ID, patientId)
            args.putString(ARG_ROLE, role)
            fragment.arguments = args
            return fragment
        }
    }

    private val speechRecognizerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val data = result.data
            val resultList = data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            resultList?.get(0)?.let { spokenText ->
                viewModel.addMessage(MessagesModel(spokenText, ChatRole.USER))
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentChatbotBinding.inflate(inflater, container, false)
        patientId = arguments?.getString(ARG_PATIENT_ID)
        role = arguments?.getString(ARG_ROLE)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel = ViewModelProvider(this).get(MessagesViewModel::class.java)
        chatAdapter = ChatRecyclerAdapter(emptyList())
        binding.chatRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.chatRecyclerView.adapter = chatAdapter


        binding.messageSendBtn.setOnClickListener {
            viewModel.addMessage(MessagesModel(binding.chatMessageInput.text.toString(), ChatRole.USER))
            binding.chatMessageInput.setText("")
        }

        binding.btnMic.setOnClickListener {
            startSpeechToText()
        }

        viewModel.messages.observe(viewLifecycleOwner) { messages ->
            chatAdapter.updateMessages(messages)
            if (messages[messages.size - 1].role == ChatRole.USER){
                val jsonRequestBody = buildJsonRequestBody(messages)
                streamChatResponse(jsonRequestBody)
            }
        }
    }

    private fun startSpeechToText() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
        }

        try {
            speechRecognizerLauncher.launch(intent)
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(requireContext(), "Speech recognition not supported", Toast.LENGTH_SHORT).show()
        }
    }

    fun streamChatResponse(jsonRequestBody: String) {
        val client = OkHttpClient()

        val requestBody = jsonRequestBody.toRequestBody("application/json; charset=utf-8".toMediaType())

        val request = Request.Builder()
            .url("http://10.0.2.2:8000/chat")
            .post(requestBody)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
            }

            override fun onResponse(call: Call, response: Response) {
                val handler = Handler(Looper.getMainLooper())
                val reader = BufferedReader(InputStreamReader(response.body!!.byteStream()))
                var line: String?

                while (reader.readLine().also { line = it } != null) {
                    val textChunk = line ?: ""
                    handler.post {
                        viewModel.addOrUpdateMessage(textChunk)
                    }
                }

                reader.close()
            }
        })
    }

    fun buildJsonRequestBody(messages: List<MessagesModel>): String {
        val json = JSONObject()
        val messagesArray = JSONArray()

        for (msg in messages) {
            val msgObject = JSONObject()
            msgObject.put("content", msg.content)
            msgObject.put("role", msg.role.name)
            messagesArray.put(msgObject)
        }

        json.put("messages", messagesArray)
        json.put("role", role)
        json.put("patient_id", patientId)
        return json.toString()
    }
}