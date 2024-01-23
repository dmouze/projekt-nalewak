package com.kierman.projektnalewak.util

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.kierman.projektnalewak.databinding.UserListItemBinding

class UserListAdapter(
    private val userList: List<UserModel>,
    private val itemClickListener: ItemClickListener,
    private val longClickListener: ItemLongClickListener
) : RecyclerView.Adapter<UserListAdapter.UserViewHolder>() {

    // Funkcja do znalezienia indeksu użytkownika z najlepszym wynikiem
    private fun findIndexOfBestUser(): Int {
        var bestIndex = -1
        var bestTime = Double.MAX_VALUE // Domyślnie ustawiamy na największą możliwą wartość

        for ((index, user) in userList.withIndex()) {
            val results = user.time
            if (results!!.isNotEmpty()) {
                val bestResult = results
                    .filter { it > 0.0 } // Usuń niepoprawne wyniki
                    .minOrNull()

                bestResult?.let {
                    if (it < bestTime) {
                        bestTime = it
                        bestIndex = index
                    }
                }
            }
        }

        return bestIndex
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = UserListItemBinding.inflate(inflater, parent, false)
        return UserViewHolder(binding)
    }

    override fun onBindViewHolder(holder: UserViewHolder, position: Int) {
        val currentUser = userList[position]
        holder.bind(currentUser, position)
        holder.itemView.setOnClickListener {
            itemClickListener.onItemClick(currentUser)
        }
        holder.itemView.setOnLongClickListener {
            longClickListener.onItemLongClick(currentUser)
            true
        }
    }

    override fun getItemCount(): Int {
        return userList.size
    }

    inner class UserViewHolder(private val binding: UserListItemBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(user: UserModel, position: Int) {
            val isBestUser = position == findIndexOfBestUser()

            val userNameText = if (isBestUser) {
                "🥇${user.name}" // Dodać emotikonę tylko przed imieniem najlepszego użytkownika
            } else {
                user.name ?: "" // Pozostali użytkownicy bez emotikony
            }

            binding.userNameTextView.text = userNameText

            binding.root.setOnClickListener {
                itemClickListener.onItemClick(user) // Przekazanie użytkownika do listenera
            }
        }
    }

    interface ItemClickListener {
        fun onItemClick(user: UserModel)
    }

    interface ItemLongClickListener {
        fun onItemLongClick(user: UserModel)
    }
}