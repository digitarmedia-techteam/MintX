package com.digitar.mintx.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
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
    
    // Dynamic List
    private val categories = mutableListOf<Category>()
    private lateinit var adapter: CategoryAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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
        loadData()
    }

    private fun setupRecyclerView() {
        adapter = CategoryAdapter(categories) { category, isSelected ->
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
            if (selectedCategories.isNotEmpty()) {
                sessionManager.setCategoriesSelected(true)
                dismiss()
            } else {
                Toast.makeText(context, "Please select at least 1 category to continue", Toast.LENGTH_SHORT).show()
            }
        }
        
        // Select All checkbox
        binding.cbSelectAll.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                selectAllCategories()
            } else {
                deselectAllCategories()
            }
        }
    }

    private fun updateContinueButton() {
        val isEnabled = selectedCategories.isNotEmpty()
        binding.btnContinue.isEnabled = isEnabled
        binding.btnContinue.alpha = if (isEnabled) 1.0f else 0.5f
        
        // Update Select All checkbox state
        binding.cbSelectAll.setOnCheckedChangeListener(null) // Temporarily remove listener
        binding.cbSelectAll.isChecked = selectedCategories.size == categories.size && categories.isNotEmpty()
        binding.cbSelectAll.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                selectAllCategories()
            } else {
                deselectAllCategories()
            }
        }
        
        updateStepIndicator()
    }
    
    private fun selectAllCategories() {
        selectedCategories.clear()
        selectedCategories.addAll(categories.map { it.id })
        adapter.notifyDataSetChanged()
        updateContinueButton()
    }
    
    private fun deselectAllCategories() {
        selectedCategories.clear()
        adapter.notifyDataSetChanged()
        updateContinueButton()
    }
    
    private fun updateStepIndicator() {
        val count = selectedCategories.size
        val total = categories.size
        binding.tvStepIndicator.text = "Selected $count / Total $total"
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
                    Toast.makeText(context, "Failed to save: ${e.message}", Toast.LENGTH_SHORT).show()
                    binding.btnContinue.text = "Continue"
                    binding.btnContinue.isEnabled = true
                }
        } else {
            dismiss()
        }
    }

    private fun loadData() {
        // 1. Fetch Categories from Firestore
        FirebaseFirestore.getInstance().collection("quiz_categories")
            .get()
            .addOnSuccessListener { result ->
                categories.clear()
                for (document in result) {
                    val name = document.getString("name") ?: continue
                    // We use name as ID for compatibility with current User model
                    // Check for image url field (standard "imageUrl" or fallback)
                    val imageUrl = document.getString("imageUrl") ?: document.getString("image") // Support both naming
                    
                    categories.add(Category(name, name, imageUrl))
                }
                
                // Sort optionally?
                categories.sortBy { it.name }

                adapter.notifyDataSetChanged()
                binding.tvSubtitle.text = "Select at least 1 category to continue"
                
                // 2. Fetch User Selection after categories are loaded
                fetchUserSelection()
            }
            .addOnFailureListener { e ->
                Toast.makeText(context, "Error loading categories: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun fetchUserSelection() {
        // Initial update
        updateStepIndicator()
        
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        FirebaseFirestore.getInstance().collection("users").document(uid).get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    val loadedCategories = document.get("categories") as? List<String>
                    if (loadedCategories != null) {
                        selectedCategories.clear()
                        selectedCategories.addAll(loadedCategories)
                        adapter.notifyDataSetChanged()
                        updateContinueButton()
                    }
                }
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
            
            fun bind(category: Category, animate: Boolean = false) {
                binding.tvCategoryName.text = category.name
                
                // Load Image if available
                if (!category.imageUrl.isNullOrEmpty()) {
                    binding.ivCategoryIcon.visibility = View.VISIBLE
                    Glide.with(binding.root.context)
                        .load(category.imageUrl)
                        .error(R.drawable.ic_category_generic)
                        .into(binding.ivCategoryIcon)
                } else {
                    binding.ivCategoryIcon.visibility = View.GONE
                }
                
                val isSelected = selectedCategories.contains(category.id)
                binding.ivCheck.visibility = if (isSelected) View.VISIBLE else View.GONE
                
                val context = binding.root.context
                if (isSelected) {
                    binding.cardCategory.setCardBackgroundColor(context.getColor(R.color.mint_primary))
                    binding.cardCategory.strokeColor = context.getColor(R.color.mint_primary)
                    binding.tvCategoryName.setTextColor(context.getColor(R.color.text_inverse))
                } else {
                    binding.cardCategory.setCardBackgroundColor(context.getColor(R.color.mint_surface))
                    binding.cardCategory.strokeColor = context.getColor(R.color.accent_glass_border)
                    binding.tvCategoryName.setTextColor(context.getColor(R.color.text_headline))
                }
                
                if (animate) {
                    if (isSelected) {
                        // Pop effect for Card
                        binding.root.scaleX = 0.9f
                        binding.root.scaleY = 0.9f
                        binding.root.animate()
                            .scaleX(1.0f).scaleY(1.0f)
                            .setDuration(300)
                            .setInterpolator(android.view.animation.OvershootInterpolator(1.5f))
                            .start()
                            
                        // Pop effect for Check Icon
                        binding.ivCheck.scaleX = 0f
                        binding.ivCheck.scaleY = 0f
                        binding.ivCheck.animate()
                            .scaleX(1f).scaleY(1f)
                            .setDuration(300)
                            .setStartDelay(50) // Slight delay for satisfaction
                            .setInterpolator(android.view.animation.OvershootInterpolator(2.0f))
                            .start()
                    } else {
                         // Reset scale smoothly if needed
                        binding.root.animate()
                            .scaleX(1.0f).scaleY(1.0f)
                            .setDuration(200)
                            .start()
                    }
                }
                
                binding.root.setOnClickListener {
                    val newState = !selectedCategories.contains(category.id)
                    onItemClick(category, newState)
                    notifyItemChanged(adapterPosition, "SELECTION_ANIM")
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
        
        override fun onBindViewHolder(holder: CategoryViewHolder, position: Int, payloads: MutableList<Any>) {
            if (payloads.isNotEmpty() && payloads.contains("SELECTION_ANIM")) {
                holder.bind(items[position], animate = true)
            } else {
                super.onBindViewHolder(holder, position, payloads)
            }
        }

        override fun onBindViewHolder(holder: CategoryViewHolder, position: Int) {
            holder.bind(items[position])
        }

        override fun getItemCount() = items.size
    }

    data class Category(val id: String, val name: String, val imageUrl: String?)
}
