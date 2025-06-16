package mk.ukim.finki.docappointassistbot

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.database.*
import mk.ukim.finki.docappointassistbot.adapter.AdminRequestAdapter
import mk.ukim.finki.docappointassistbot.databinding.FragmentAdminRequestsBinding
import mk.ukim.finki.docappointassistbot.domain.DoctorRequest

class AdminRequestsFragment : Fragment() {

    private var _binding: FragmentAdminRequestsBinding? = null
    private val binding get() = _binding!!

    private lateinit var dbRef: DatabaseReference
    private val requests = mutableListOf<DoctorRequest>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAdminRequestsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.rvDoctorRequests.layoutManager = LinearLayoutManager(requireContext())
        dbRef = FirebaseDatabase.getInstance().getReference("doctorRequests")
        loadRequests()
    }

    private fun loadRequests() {
        dbRef.orderByChild("status").equalTo("Submitted")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    requests.clear()
                    for (child in snapshot.children) {
                        val request = child.getValue(DoctorRequest::class.java)
                        request?.let { requests.add(it) }
                    }
                    binding.rvDoctorRequests.adapter = AdminRequestAdapter(requests) {
                        loadRequests()
                    }
                }

                override fun onCancelled(error: DatabaseError) {}
            })
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
