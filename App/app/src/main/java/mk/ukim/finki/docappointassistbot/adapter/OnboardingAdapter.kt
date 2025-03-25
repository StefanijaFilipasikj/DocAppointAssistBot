package mk.ukim.finki.docappointassistbot.adapter

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import mk.ukim.finki.docappointassistbot.LoadingFragment
import mk.ukim.finki.docappointassistbot.EnableNotificationsFragment
import mk.ukim.finki.docappointassistbot.EnableLocationFragment

class OnboardingAdapter(activity: FragmentActivity) : FragmentStateAdapter(activity) {

    override fun getItemCount(): Int = 3

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> LoadingFragment()
            1 -> EnableNotificationsFragment()
            2 -> EnableLocationFragment()
            else -> throw IllegalStateException("Invalid position")
        }
    }
}