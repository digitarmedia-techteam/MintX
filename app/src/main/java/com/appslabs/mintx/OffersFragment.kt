package com.appslabs.mintx

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.appslabs.mintx.databinding.FragmentOffersBinding

class OffersFragment : Fragment() {
    private var _binding: FragmentOffersBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentOffersBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()

        binding.tabLayout.addOnTabSelectedListener(object :
            com.google.android.material.tabs.TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: com.google.android.material.tabs.TabLayout.Tab?) {
                // Filter logic
            }

            override fun onTabUnselected(tab: com.google.android.material.tabs.TabLayout.Tab?) {}
            override fun onTabReselected(tab: com.google.android.material.tabs.TabLayout.Tab?) {}
        })
    }

    private fun setupRecyclerView() {
        val adapter = com.appslabs.mintx.ui.adapter.TaskAdapter()
        binding.rvTasks.layoutManager =
            androidx.recyclerview.widget.LinearLayoutManager(requireContext())
        binding.rvTasks.adapter = adapter

        val sampleTasks = listOf(
            com.appslabs.mintx.data.model.Task(
                "1", "Install Amazon App", "+150 Coins", "Pending Verification",
                R.drawable.amazon_logo,
                listOf(
                    "Download and install the Amazon app.",
                    "Create a new account using a valid email."
                ),
                "Automatic credit after 24 hours."
            ),

            com.appslabs.mintx.data.model.Task(
                "2", "Watch Rewarded Video", "+50 Coins", "Available",
                R.drawable.amazon_logo,
                listOf(
                    "Watch the full video without skipping.",
                    "Click the claim button after completion."
                ),
                "Instant credit."
            ),

            com.appslabs.mintx.data.model.Task(
                "3", "Join Telegram Channel", "+200 Coins", "Available",
                R.drawable.amazon_logo,
                listOf(
                    "Tap on the join button.",
                    "Stay joined for at least 7 days."
                ),
                "Manual review within 48 hours."
            ),

            com.appslabs.mintx.data.model.Task(
                "4", "Install Flipkart App", "+120 Coins", "Available",
                R.drawable.amazon_logo,
                listOf(
                    "Install the Flipkart app from Play Store.",
                    "Login with a new account."
                ),
                "Coins credited within 24 hours."
            ),

            com.appslabs.mintx.data.model.Task(
                "5", "Daily App Login", "+20 Coins", "Available",
                R.drawable.amazon_logo,
                listOf(
                    "Open the app today.",
                    "Stay active for at least 2 minutes."
                ),
                "Instant credit."
            ),

            com.appslabs.mintx.data.model.Task(
                "6", "Play Featured Game", "+100 Coins", "Available",
                R.drawable.amazon_logo,
                listOf(
                    "Launch the featured game.",
                    "Play for at least 5 minutes."
                ),
                "Coins added instantly."
            ),

            com.appslabs.mintx.data.model.Task(
                "7", "Invite a Friend", "+300 Coins", "Available",
                R.drawable.amazon_logo,
                listOf(
                    "Share your referral link.",
                    "Friend must sign up and open the app."
                ),
                "Credited after friend verification."
            ),

            com.appslabs.mintx.data.model.Task(
                "8", "Enable App Notifications", "+30 Coins", "Available",
                R.drawable.amazon_logo,
                listOf(
                    "Allow push notifications.",
                    "Do not disable for 24 hours."
                ),
                "Automatic credit."
            ),

            com.appslabs.mintx.data.model.Task(
                "9", "Complete Profile", "+80 Coins", "Available",
                R.drawable.amazon_logo,
                listOf(
                    "Add name and profile picture.",
                    "Save your profile details."
                ),
                "Instant credit after saving."
            ),

            com.appslabs.mintx.data.model.Task(
                "10", "Rate App on Play Store", "+250 Coins", "Pending Verification",
                R.drawable.amazon_logo,
                listOf(
                    "Give a 5-star rating.",
                    "Write a short review."
                ),
                "Manual verification within 72 hours."
            )
        )
        adapter.submitList(sampleTasks)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
