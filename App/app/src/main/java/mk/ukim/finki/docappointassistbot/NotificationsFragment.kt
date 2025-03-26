package mk.ukim.finki.docappointassistbot

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import mk.ukim.finki.docappointassistbot.adapter.NotificationsAdapter
import mk.ukim.finki.docappointassistbot.ui.viewModels.NotificationsViewModel

class NotificationsFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: NotificationsAdapter
    private lateinit var viewModel: NotificationsViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_notifications, container, false)
        recyclerView = view.findViewById(R.id.notificationsRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        viewModel = ViewModelProvider(this).get(NotificationsViewModel::class.java)
        viewModel.notifications.observe(viewLifecycleOwner) { notifications ->
            adapter = NotificationsAdapter(notifications)
            recyclerView.adapter = adapter
        }

        return view
    }
}
