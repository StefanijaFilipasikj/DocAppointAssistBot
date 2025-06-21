package mk.ukim.finki.docappointassistbot

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import mk.ukim.finki.docappointassistbot.adapter.UserAdapter
import mk.ukim.finki.docappointassistbot.databinding.FragmentPatientsBinding
import mk.ukim.finki.docappointassistbot.domain.User
import mk.ukim.finki.docappointassistbot.ui.viewModels.AppointmentsViewModel


class PatientsFragment : Fragment() {

    private var _binding: FragmentPatientsBinding? = null
    private val binding get() = _binding!!

    private lateinit var users: ArrayList<User>
    private lateinit var firebaseRef: DatabaseReference
    private lateinit var appointmentsViewModel: AppointmentsViewModel

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPatientsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        appointmentsViewModel = activityViewModels<AppointmentsViewModel>().value

        val user = FirebaseAuth.getInstance().currentUser

        if(user != null){
            appointmentsViewModel.fetchAppointmentsForDoctor(user.uid)
        }

        users = arrayListOf()
        appointmentsViewModel.appointments.observe(viewLifecycleOwner)  {appointments ->
            val patientIds = appointments.map { app -> app.userId }.toSet()
            Log.d("PatientsFragment", "Patient ids fetched: $patientIds")
            fetchData(patientIds)
        }

        binding.patients.layoutManager = LinearLayoutManager(context)
    }

    private fun fetchData(patientIds: Set<String>) {
        firebaseRef = FirebaseDatabase
            .getInstance("https://docappointassistbot-default-rtdb.europe-west1.firebasedatabase.app")
            .getReference("users")
        users.clear();

        firebaseRef.addValueEventListener(object: ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot){
                users.clear()
                if (snapshot.exists()){
                    for (userSnap in snapshot.children){
                        val user = userSnap.getValue(User::class.java)
                        if (user != null && patientIds.contains(user.email)) {
                            users.add(user)
                            Log.d("PatientsFragment", "Patient fetched: $user")
                        }
                    }
                }
                val adapter = UserAdapter(users){ selectedUser ->
                    val fragment = ChatbotFragment.newInstance(selectedUser.email, "doctor")

                    parentFragmentManager.beginTransaction()
                        .replace(R.id.frameLayout, fragment)
                        .addToBackStack(null)
                        .commit()
                }
                binding.patients.adapter = adapter
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(requireContext(), "error: $error", Toast.LENGTH_SHORT).show()
            }
        })
    }
}