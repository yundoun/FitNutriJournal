package com.example.fitnutrijournal.data.adapter

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.example.fitnutrijournal.ui.diet.CustomAddTabFragment
import com.example.fitnutrijournal.ui.diet.DietFragment
import com.example.fitnutrijournal.ui.diet.DietTabFragment
import com.example.fitnutrijournal.ui.diet.FavoriteTabFragment
import com.example.fitnutrijournal.ui.diet.MealListFragment
import com.example.fitnutrijournal.ui.diet.OftenFoodFragment

class DietPagerAdapter(fragmentActivity: DietFragment) :
    FragmentStateAdapter(fragmentActivity) {

    companion object {
        private val TAB_TITLES = arrayOf("음식", "직접 추가한 음식", "즐겨찾기", "최근에 먹은 음식")
    }

    override fun getItemCount(): Int {
        return TAB_TITLES.size
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> {
                DietTabFragment.newInstance(TAB_TITLES[position])
            }
            1 -> {
                CustomAddTabFragment()
            }
            2 -> {
                FavoriteTabFragment()
            }
            else -> {
                OftenFoodFragment()
            }
        }
    }

    fun getTabTitle(position: Int): String {
        return TAB_TITLES[position]
    }
}
