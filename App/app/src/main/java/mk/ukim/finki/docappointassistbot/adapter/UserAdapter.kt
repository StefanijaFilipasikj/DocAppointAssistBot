package mk.ukim.finki.docappointassistbot.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import mk.ukim.finki.docappointassistbot.R
import mk.ukim.finki.docappointassistbot.databinding.ItemUserBinding
import mk.ukim.finki.docappointassistbot.domain.User

class UserAdapter(private var users : List<User>,
                  private val onUserClick: (User) -> Unit
) : RecyclerView.Adapter<UserAdapter.ViewHolder>() {

    class ViewHolder(val binding : ItemUserBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserAdapter.ViewHolder {
        return ViewHolder(ItemUserBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    }

    override fun getItemCount(): Int {
        return users.size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val currentItem = users[position]
        holder.apply {
            binding.apply {
                userFullName.text = currentItem.fullName

                Glide.with(holder.itemView.context)
                    .load(currentItem.photoUrl)
                    .centerCrop()
                    .placeholder(R.drawable.ic_launcher_background)
                    .into(userImage)

                btnChat.setOnClickListener{
                    onUserClick(currentItem)
                }
            }
        }
    }
}