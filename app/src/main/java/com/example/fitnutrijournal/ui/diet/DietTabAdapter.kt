import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import androidx.lifecycle.LiveData
import androidx.recyclerview.widget.RecyclerView
import com.example.fitnutrijournal.R
import com.example.fitnutrijournal.data.model.Food
import com.example.fitnutrijournal.databinding.ItemDietBinding
import com.example.fitnutrijournal.viewmodel.DietViewModel

class DietTabAdapter(
    private var diets: List<Food>,
    private val toggleFavorite: (Food) -> Unit,
    private val favorites: LiveData<Set<String>>,
    private val onItemClick: (Food) -> Unit,
    private val viewModel: DietViewModel
) : RecyclerView.Adapter<DietTabAdapter.DietViewHolder>() {

    @SuppressLint("NotifyDataSetChanged")
    fun updateDiets(newDiets: List<Food>) {
        diets = newDiets
        notifyDataSetChanged() // This will notify the RecyclerView to refresh its items
    }

    inner class DietViewHolder(private val binding: ItemDietBinding) :
        RecyclerView.ViewHolder(binding.root) {
        @SuppressLint("SetTextI18n")
        fun bind(item: Food) {
            binding.foodName.text = item.foodName
            binding.foodTotalContent.text = "${item.servingSize} g"
            binding.foodCalories.text = "${item.calories} kcal"
            updateFavoriteButton(binding.favoriteBtn, item.foodCd)

            binding.favoriteBtn.setOnClickListener {
                toggleFavorite(item)
            }

            binding.root.setOnClickListener {
                onItemClick(item)
            }

            when (viewModel.isCheckboxVisible.value) {
                true -> {
                    binding.favoriteBtn.visibility = View.GONE
                    binding.checkbox.visibility = View.VISIBLE
                }
                false -> {
                    binding.favoriteBtn.visibility = View.VISIBLE
                    binding.checkbox.visibility = View.GONE
                }
                else -> {
                    binding.favoriteBtn.visibility = View.GONE
                    binding.checkbox.visibility = View.GONE
                }
            }

            // 체크박스 상태 설정
            binding.checkbox.setOnCheckedChangeListener(null) // Prevents infinite loop
            binding.checkbox.isChecked = viewModel.checkedItems.value?.contains(item) == true
            binding.checkbox.setOnCheckedChangeListener { _, isChecked ->
                viewModel.toggleCheckedItem(item)
            }

        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DietViewHolder {
        val binding = ItemDietBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return DietViewHolder(binding)
    }

    override fun onBindViewHolder(holder: DietViewHolder, position: Int) {
        val item = diets[position]
        holder.bind(item)

        // 즐겨찾기 상태 관찰
        favorites.observeForever {
            holder.bind(item)
        }


    }

    private fun updateFavoriteButton(button: ImageButton, foodCode: String) {
        val isFavorite = favorites.value?.contains(foodCode) ?: false
        button.setImageResource(
            if (isFavorite) R.drawable.ic_star
            else R.drawable.ic_star_border
        )
    }


    override fun getItemCount(): Int = diets.size
}
