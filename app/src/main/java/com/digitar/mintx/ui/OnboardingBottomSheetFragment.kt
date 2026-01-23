package com.digitar.mintx.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.digitar.mintx.R
import com.digitar.mintx.databinding.FragmentOnboardingBottomSheetBinding
import com.digitar.mintx.databinding.ItemOnboardingCategoryBinding
import com.digitar.mintx.utils.SessionManager
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class OnboardingBottomSheetFragment : BottomSheetDialogFragment() {

    private var _binding: FragmentOnboardingBottomSheetBinding? = null
    private val binding get() = _binding!!
    private lateinit var sessionManager: SessionManager
    private val selectedCategories = mutableSetOf<String>()

    private val categories = listOf(
        Category("action", "Action", R.drawable.action),
        Category("adventure", "Adventure", R.drawable.adventure),
        Category("puzzle", "Puzzle", R.drawable.puzzle),
        Category("strategy", "Strategy", R.drawable.strategy),
        Category("sports", "Sports", R.drawable.sports),
        Category("racing", "Racing", R.drawable.racing),
        Category("rpg", "RPG", R.drawable.rpg),
        Category("simulation", "Simulation", R.drawable.simulation)
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Use a transparent style so our background drawable is visible
        setStyle(STYLE_NORMAL, R.style.BottomSheetDialogTheme)
        sessionManager = SessionManager(requireContext())
        isCancelable = false
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentOnboardingBottomSheetBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupListeners()
        fetchUserCategories()
    }

    private fun setupRecyclerView() {
        val adapter = CategoryAdapter(categories) { category, isSelected ->
            if (isSelected) {
                selectedCategories.add(category.id)
            } else {
                selectedCategories.remove(category.id)
            }
            updateContinueButton()
        }
        binding.rvCategories.layoutManager = GridLayoutManager(context, 2)
        binding.rvCategories.adapter = adapter
    }

    private fun setupListeners() {
        binding.btnContinue.setOnClickListener {
            saveCategoriesAndDismiss()
        }

        binding.tvSkip.setOnClickListener {
            // Dismiss without saving categories, but mark as seen?
            // User requirement: "Show an option in the Profile screen later to select categories manually."
            // So we probably shouldn't mark "categories selected" as true in session, 
            // but we might need a flag "onboardingShown" so we don't show it again immediately.
            // For now, I'll allow dismissal. Session tracking is up to MainActivity to check "isCategoriesSelected".
            // If we don't set it, it will show again next app launch, which is what "Onboarding popup should be shown only once for new users" implies?
            // Actually usually "shown only once" means don't annoy them every time. 
            // So I should set a flag "isOnboardingSkipped" or "hasSeenOnboarding".
            // Re-reading: "If the user skips: Do not block the user. Show an option in the Profile screen later."
            // I will set 'isCategoriesSelected' to true (as in, flow handled) but with empty list? 
            // Or add a new flag 'hasSeenCategoryOnboarding'.
            // I'll stick to 'isCategoriesSelected = true' for now to prevent loop, assuming empty list means skipped.
            
            sessionManager.setCategoriesSelected(true) 
            dismiss()
        }
    }

    private fun updateContinueButton() {
        val isEnabled = selectedCategories.size >= 3
        binding.btnContinue.isEnabled = isEnabled
        binding.btnContinue.alpha = if (isEnabled) 1.0f else 0.5f
    }

    private fun saveCategoriesAndDismiss() {
        binding.btnContinue.text = "Saving..."
        binding.btnContinue.isEnabled = false

        val uid = FirebaseAuth.getInstance().currentUser?.uid
        if (uid != null) {
            val userRef = FirebaseFirestore.getInstance().collection("users").document(uid)
            userRef.update("categories", selectedCategories.toList())
                .addOnSuccessListener {
                    sessionManager.setCategoriesSelected(true)
                    Toast.makeText(context, "Preferences saved!", Toast.LENGTH_SHORT).show()
                    dismiss()
                }
                .addOnFailureListener { e ->
                    // Handle failure (maybe offline)
                    // For now, assume success or offline persistence
                    // But if it fails, we should probably still let them in or retry.
                    Toast.makeText(context, "Failed to save: ${e.message}", Toast.LENGTH_SHORT).show()
                    binding.btnContinue.text = "Continue"
                    binding.btnContinue.isEnabled = true
                }
        } else {
            // Should not happen if logged in
            dismiss()
        }
    }

    private fun fetchUserCategories() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        FirebaseFirestore.getInstance().collection("users").document(uid).get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    val loadedCategories = document.get("categories") as? List<String>
                    if (loadedCategories != null) {
                        selectedCategories.clear()
                        selectedCategories.addAll(loadedCategories)
                        binding.rvCategories.adapter?.notifyDataSetChanged()
                        updateContinueButton()
                    }
                }
            }
            .addOnFailureListener {
                // Silently fail or log
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    inner class CategoryAdapter(
        private val items: List<Category>,
        private val onItemClick: (Category, Boolean) -> Unit
    ) : RecyclerView.Adapter<CategoryAdapter.CategoryViewHolder>() {

        inner class CategoryViewHolder(val binding: ItemOnboardingCategoryBinding) :
            RecyclerView.ViewHolder(binding.root) {
            
            fun bind(category: Category) {
                binding.tvCategoryName.text = category.name
                binding.ivCategoryIcon.setImageResource(category.iconRes)
                
                val isSelected = selectedCategories.contains(category.id)
                binding.ivCheck.visibility = if (isSelected) View.VISIBLE else View.GONE
                binding.cardCategory.strokeColor = if (isSelected) 
                    binding.root.context.getColor(R.color.mint_gold) 
                else 
                    android.graphics.Color.parseColor("#444444")
                
                // Animation logic could go here
                
                binding.root.setOnClickListener {
                    val newState = !selectedCategories.contains(category.id)
                    onItemClick(category, newState)
                    notifyItemChanged(adapterPosition)
                }
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CategoryViewHolder {
            val binding = ItemOnboardingCategoryBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
            return CategoryViewHolder(binding)
        }

        override fun onBindViewHolder(holder: CategoryViewHolder, position: Int) {
            holder.bind(items[position])
        }

        override fun getItemCount() = items.size
    }

    data class Category(val id: String, val name: String, val iconRes: Int)
}
