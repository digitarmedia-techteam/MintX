# Category Chips - Customization Guide

## 🎨 Current Chip Styling

Each category chip has the following properties:

```kotlin
val chip = Chip(requireContext()).apply {
    // Text
    text = categoryName                    // e.g., "Cryptocurrencies"
    textSize = 12f                         // 12sp
    setTextColor(mint_gold)                // Gold text color
    
    // Background
    chipBackgroundColor = mint_surface     // Light background
    
    // Border
    chipStrokeColor = mint_gold            // Gold border
    chipStrokeWidth = 2f                   // 2px border
    
    // Shape
    chipCornerRadius = 40f                 // Pill shape
    chipMinHeight = 32f                    // Minimum height
    
    // Behavior
    isClickable = false                    // Not clickable
    isCheckable = false                    // Not checkable
}
```

---

## 🔧 Customization Options

### Change Chip Colors

**Text Color:**
```kotlin
setTextColor(ContextCompat.getColor(requireContext(), R.color.YOUR_COLOR))
```

**Background Color:**
```kotlin
chipBackgroundColor = ColorStateList.valueOf(
    ContextCompat.getColor(requireContext(), R.color.YOUR_COLOR)
)
```

**Border Color:**
```kotlin
chipStrokeColor = ColorStateList.valueOf(
    ContextCompat.getColor(requireContext(), R.color.YOUR_COLOR)
)
```

---

### Change Chip Size

**Text Size:**
```kotlin
textSize = 14f  // Larger text (default: 12f)
```

**Minimum Height:**
```kotlin
chipMinHeight = 40f  // Taller chips (default: 32f)
```

**Border Width:**
```kotlin
chipStrokeWidth = 3f  // Thicker border (default: 2f)
```

---

### Change Chip Shape

**More Rounded (Pill):**
```kotlin
chipCornerRadius = 50f  // More rounded (default: 40f)
```

**Less Rounded (Rectangle):**
```kotlin
chipCornerRadius = 16f  // Less rounded
```

**Perfect Circle (for single letter):**
```kotlin
chipCornerRadius = 100f
```

---

### Change Chip Spacing

**In `fragment_quiz.xml`:**

**Horizontal Spacing:**
```xml
app:chipSpacingHorizontal="12dp"  <!-- Default: 8dp -->
```

**Vertical Spacing:**
```xml
app:chipSpacingVertical="8dp"  <!-- Default: 4dp -->
```

---

### Add Icon to Chips

```kotlin
val chip = Chip(requireContext()).apply {
    // ... existing properties ...
    
    // Add icon
    chipIcon = ContextCompat.getDrawable(requireContext(), R.drawable.ic_category)
    chipIconTint = ColorStateList.valueOf(
        ContextCompat.getColor(requireContext(), R.color.mint_gold)
    )
    chipIconSize = 16f
}
```

---

### Make Chips Clickable (Optional)

If you want chips to be removable or clickable:

```kotlin
val chip = Chip(requireContext()).apply {
    // ... existing properties ...
    
    isClickable = true
    isCloseIconVisible = true  // Show X button
    
    setOnCloseIconClickListener {
        // Remove this chip
        binding.chipGroupSelectedCategories.removeView(this)
    }
    
    setOnClickListener {
        // Handle chip click
        Toast.makeText(context, "Clicked: $text", Toast.LENGTH_SHORT).show()
    }
}
```

---

### Change Chip Layout

**Single Line (Horizontal Scroll):**
```xml
<HorizontalScrollView
    android:layout_width="match_parent"
    android:layout_height="wrap_content">
    
    <com.google.android.material.chip.ChipGroup
        android:id="@+id/chip_group_selected_categories"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        app:singleLine="true" />
</HorizontalScrollView>
```

**Multi-line (Current - Wraps):**
```xml
<com.google.android.material.chip.ChipGroup
    android:id="@+id/chip_group_selected_categories"
    android:layout_width="0dp"
    android:layout_height="wrap_content"
    app:singleLine="false" />  <!-- Wraps to next line -->
```

---

## 🎨 Color Schemes

### Option 1: Gold Accent (Current)
- Background: `mint_surface` (white/light)
- Border: `mint_gold` (gold)
- Text: `text_headline` (dark)

### Option 2: Green Accent
```kotlin
chipBackgroundColor = ColorStateList.valueOf(mint_surface)
chipStrokeColor = ColorStateList.valueOf(mint_green)
setTextColor(mint_green)
```

### Option 3: Filled Style
```kotlin
chipBackgroundColor = ColorStateList.valueOf(mint_gold)
chipStrokeWidth = 0f  // No border
setTextColor(Color.WHITE)
```

### Option 4: Gradient (Advanced)
Create `bg_chip_gradient.xml`:
```xml
<shape xmlns:android="http://schemas.android.com/apk/res/android">
    <gradient
        android:angle="135"
        android:startColor="@color/mint_gold"
        android:endColor="@color/mint_green" />
    <corners android:radius="40dp" />
</shape>
```

Then use:
```kotlin
chipBackgroundColor = null
background = ContextCompat.getDrawable(requireContext(), R.drawable.bg_chip_gradient)
```

---

## 📐 Layout Positioning

### Current Position:
- Between header and question card
- Aligned to start (left)
- Constrained to timer on right

### Center Align:
```xml
app:layout_constraintStart_toStartOf="parent"
app:layout_constraintEnd_toEndOf="parent"
android:layout_marginStart="20dp"
android:layout_marginEnd="20dp"
```

### Full Width:
```xml
app:layout_constraintStart_toStartOf="parent"
app:layout_constraintEnd_toEndOf="parent"
android:layout_marginStart="0dp"
android:layout_marginEnd="0dp"
```

---

## 🎭 Animation Customization

### Faster Animation:
```kotlin
duration = 400  // Default: 600
startDelay = 50  // Default: 100
```

### Different Direction:
```kotlin
// From left instead of right
val chipTransX = ObjectAnimator.ofFloat(
    binding.chipGroupSelectedCategories, 
    "translationX", 
    -100f,  // Negative = from left
    0f
)
```

### Scale Animation (Zoom In):
```kotlin
val chipScaleX = ObjectAnimator.ofFloat(chipGroup, "scaleX", 0.5f, 1f)
val chipScaleY = ObjectAnimator.ofFloat(chipGroup, "scaleY", 0.5f, 1f)

chipAnimSet.playTogether(chipAlpha, chipTransY, chipTransX, chipScaleX, chipScaleY)
```

---

## 💡 Pro Tips

1. **Keep text short** - Long category names may wrap awkwardly
2. **Limit categories** - Too many chips can clutter the UI
3. **Test on small screens** - Ensure chips don't overflow
4. **Consistent styling** - Match chip style with app theme
5. **Accessibility** - Ensure sufficient contrast for text

---

## 🐛 Troubleshooting

### Chips not appearing?
- Check `chipGroupSelectedCategories.visibility = View.VISIBLE`
- Verify `removeAllViews()` is called before adding new chips
- Check if categories list is empty

### Chips overlapping?
- Increase `chipSpacingHorizontal` and `chipSpacingVertical`
- Check layout constraints
- Ensure `singleLine="false"` for wrapping

### Animation not smooth?
- Reduce `duration` if too slow
- Check device animation settings
- Ensure no other animations running simultaneously

---

**Happy Customizing!** 🎨
