package com.example.bugs


import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter

class ViewPagerAdapter(fragmentActivity: FragmentActivity) : FragmentStateAdapter(fragmentActivity) {

    override fun getItemCount(): Int = 5

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> RegistrationTab()
            1 -> RuleTab()
            2 -> AuthorsTab()
            3 -> SettingsTab()
            4 -> PlayerListTab()
            else -> throw IllegalArgumentException("Invalid position: $position")
        }
    }
}