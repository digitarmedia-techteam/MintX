package com.appslabs.mintx.ui.components

import android.animation.ArgbEvaluator
import android.animation.ValueAnimator
import android.content.Context
import android.os.Bundle
import android.os.Parcelable
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.animation.OvershootInterpolator
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.view.doOnLayout
import com.appslabs.mintx.R
import com.appslabs.mintx.databinding.ViewCustomBottomNavBinding

class SmoothBottomBar @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    private var binding: ViewCustomBottomNavBinding =
        ViewCustomBottomNavBinding.inflate(LayoutInflater.from(context), this, true)

    private var onItemSelectedListener: ((Int) -> Unit)? = null
    var selectedIndex = 0
        private set

    val selectedItemId: Int
        get() = getMenuIdForIndex(selectedIndex)

    // Item Views
    private val itemViews: List<View>
    private val icons: List<ImageView>
    private val labels: List<TextView>
    
    // IDs map to index (View IDs for internal use if needed, but not strictly required for public API)
    private val idMap = mapOf(
        R.id.nav_item_home to 0,
        R.id.nav_item_quiz to 1,
        R.id.nav_item_predict to 2,
        R.id.nav_item_earn to 3,
        R.id.nav_item_wallet to 4
    )

    init {
        itemViews = listOf(
            binding.navItemHome,
            binding.navItemQuiz,
            binding.navItemPredict,
            binding.navItemEarn,
            binding.navItemWallet
        )
        
        icons = listOf(
            binding.iconHome,
            binding.iconQuiz,
            binding.iconPredict,
            binding.iconEarn,
            binding.iconWallet
        )
        
        labels = listOf(
            binding.labelHome,
            binding.labelQuiz,
            binding.labelPredict,
            binding.labelEarn,
            binding.labelWallet
        )

        setupInteractions()
        
        // Ensure initial state is set once layout is ready to calculate pill position
        binding.root.doOnLayout {
            updateUI(selectedIndex, animate = false)
        }
        
        // Handle layout changes (e.g. rotation, resizing) to correct indicator position
        binding.root.addOnLayoutChangeListener { _, left, _, right, _, oldLeft, _, oldRight, _ ->
            if (left != oldLeft || right != oldRight) {
                 updateUI(selectedIndex, animate = false)
            }
        }
    }

    private fun setupInteractions() {
        itemViews.forEachIndexed { index, view ->
            view.setOnClickListener {
                if (selectedIndex != index) {
                    val prevIndex = selectedIndex
                    selectedIndex = index
                    updateUI(selectedIndex, animate = true, prevIndex = prevIndex)
                    onItemSelectedListener?.invoke(index)
                }
            }
        }
    }

    fun setOnItemSelectedListener(listener: (Int) -> Unit) {
        this.onItemSelectedListener = listener
    }

    fun selectItem(index: Int, animate: Boolean = true) {
        if (index !in itemViews.indices) return
        
        // Even if same index, we might want to force update (e.g. onResume or layout change)
        // But preventing loop is good. We allow if indicator is hidden (first load)
        if (selectedIndex == index && binding.viewNavIndicator.visibility == View.VISIBLE && binding.viewNavIndicator.translationX != 0f) {
           // check if position is correct? Hard to exact check float.
           // Proceed to updateUI to be safe if animate is false (force sync)
           if (animate) return 
        }
        
        val prevIndex = selectedIndex
        selectedIndex = index
        
        // If layout is not ready, post it
        if (width == 0 || height == 0) {
            post { updateUI(index, animate, prevIndex) }
        } else {
            updateUI(index, animate, prevIndex)
        }
    }

    private fun updateUI(index: Int, animate: Boolean, prevIndex: Int = -1) {
        val targetView = itemViews[index]
        val indicator = binding.viewNavIndicator
        
        // Safety check: if layout is not ready, post the update
        if (targetView.width == 0 || binding.root.width == 0) {
            binding.root.post { updateUI(index, animate, prevIndex) }
            return
        }

        // 1. Move Indicator
        // Calculate X position
        val centerTarget = targetView.x + (targetView.width / 2f)
        val targetX = centerTarget - (indicator.width / 2f)
        
        if (indicator.visibility != View.VISIBLE) {
            indicator.visibility = View.VISIBLE
            indicator.translationX = targetX
        } else {
            if (animate) {
                 indicator.animate()
                     .translationX(targetX)
                     .setDuration(300)
                     .setInterpolator(OvershootInterpolator(1.1f))
                     .start()
            } else {
                indicator.animate().cancel()
                indicator.translationX = targetX
            }
        }

        // 2. Update Icons & Text
        val activeColor = ContextCompat.getColor(context, R.color.mint_gold)
        val inactiveColor = ContextCompat.getColor(context, R.color.text_subtext)
        
        icons.forEachIndexed { i, icon ->
            val label = labels[i]
            val isActive = i == index
            
            // Determine separate animation needs
            val isChanging = (i == index) || (i == prevIndex)
            val shouldAnimate = animate && isChanging
            
            val targetColor = if (isActive) activeColor else inactiveColor
            val targetScale = if (isActive) 1.2f else 1.0f
            val targetTranslationY = if (isActive) -6f else 0f
            
            // Reset state if we are doing a hard set (animate=false) or if logic demands
            if (shouldAnimate) {
                 val startColor = if (isActive) inactiveColor else activeColor
                 
                 // Color Animation
                 val colorAnim = ValueAnimator.ofObject(ArgbEvaluator(), startColor, targetColor)
                 colorAnim.addUpdateListener { animator ->
                     val color = animator.animatedValue as Int
                     icon.setColorFilter(color)
                     label.setTextColor(color)
                 }
                 colorAnim.duration = 300
                 colorAnim.start()
                 
                 // Transform Animation
                 icon.animate()
                     .scaleX(targetScale)
                     .scaleY(targetScale)
                     .translationY(targetTranslationY)
                     .setDuration(350)
                     .setInterpolator(OvershootInterpolator())
                     .start()
            } else {
                // Instant Update
                icon.animate().cancel() // Cancel any running animation
                icon.setColorFilter(targetColor)
                label.setTextColor(targetColor)
                icon.scaleX = targetScale
                icon.scaleY = targetScale
                icon.translationY = targetTranslationY
            }
        }
    }
    
    // State Persistence
    override fun onSaveInstanceState(): Parcelable {
        val bundle = Bundle()
        bundle.putParcelable("superState", super.onSaveInstanceState())
        bundle.putInt("selectedIndex", selectedIndex)
        return bundle
    }

    override fun onRestoreInstanceState(state: Parcelable?) {
        if (state is Bundle) {
            selectedIndex = state.getInt("selectedIndex", 0)
            super.onRestoreInstanceState(state.getParcelable("superState"))
            // Update UI after restore
            post { updateUI(selectedIndex, animate = false) }
        } else {
            super.onRestoreInstanceState(state)
        }
    }
    
    // Helper to map menu ID to index
    fun getMenuIdForIndex(index: Int): Int {
        return when(index) {
            0 -> R.id.navigation_home
            1 -> R.id.navigation_quiz
            2 -> R.id.navigation_prediction
            3 -> R.id.navigation_earn
            4 -> R.id.navigation_wallet
            else -> R.id.navigation_home
        }
    }
    
    // Correctly map Navigation IDs to index
    fun getIndexForMenuId(id: Int): Int {
        return when(id) {
            R.id.navigation_home -> 0
            R.id.navigation_quiz -> 1
            R.id.navigation_prediction -> 2
            R.id.navigation_earn -> 3
            R.id.navigation_wallet -> 4
            else -> 0
        }
    }
}

