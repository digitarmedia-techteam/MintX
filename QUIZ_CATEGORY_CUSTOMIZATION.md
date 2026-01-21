# Quiz Category Badge - Customization Guide

## 🎨 Badge Appearance Customization

### Option 1: Current Badge (Gradient)
**File:** `bg_category_badge.xml`
- Gradient background
- Gold border
- Clean and modern

### Option 2: Badge with Shadow (Premium)
**File:** `bg_category_badge_shadow.xml`
- Same as Option 1 + drop shadow
- More depth and premium feel
- To use: Change in `fragment_quiz.xml` line 131:
  ```xml
  android:background="@drawable/bg_category_badge_shadow"
  ```

---

## 🎭 Animation Customization

### Speed Adjustments
**File:** `QuizFragment.kt` - `animateQuizStart()` function

**Make animations faster:**
```kotlin
// Header animation
duration = 300  // Default: 500

// Footer animation
duration = 400  // Default: 600
startDelay = 50  // Default: 100
```

**Make animations slower (more dramatic):**
```kotlin
// Header animation
duration = 800  // Default: 500

// Footer animation
duration = 900  // Default: 600
startDelay = 200  // Default: 100
```

### Bounce Effect
**Current:** `OvershootInterpolator(1.2f)`

**More bounce:**
```kotlin
interpolator = android.view.animation.OvershootInterpolator(1.5f)
```

**Less bounce (smoother):**
```kotlin
interpolator = android.view.animation.OvershootInterpolator(0.8f)
```

**No bounce (linear):**
```kotlin
interpolator = android.view.animation.DecelerateInterpolator()
```

---

## 🎯 Badge Icon Customization

### Change Icon
**File:** `fragment_quiz.xml` line 145

**Current:**
```xml
android:src="@android:drawable/ic_menu_compass"
```

**Alternatives:**
```xml
<!-- Star icon -->
android:src="@android:drawable/btn_star_big_on"

<!-- Info icon -->
android:src="@android:drawable/ic_dialog_info"

<!-- Custom icon (recommended) -->
android:src="@drawable/ic_category_custom"
```

### Icon Size
**Current:** 16dp x 16dp

**Larger:**
```xml
android:layout_width="20dp"
android:layout_height="20dp"
```

**Smaller:**
```xml
android:layout_width="12dp"
android:layout_height="12dp"
```

---

## 🌈 Color Customization

### Badge Colors
**File:** `bg_category_badge.xml`

**Change gradient:**
```xml
<gradient
    android:angle="135"
    android:startColor="#YOUR_START_COLOR"
    android:endColor="#YOUR_END_COLOR"
    android:type="linear" />
```

**Change border color:**
```xml
<stroke
    android:width="1.5dp"
    android:color="#YOUR_BORDER_COLOR" />
```

### Text & Icon Colors
**File:** `fragment_quiz.xml`

**Icon tint (line 146):**
```xml
app:tint="@color/YOUR_COLOR"
```

**Text color (line 154):**
```xml
android:textColor="@color/YOUR_COLOR"
```

---

## 📐 Size & Position Customization

### Badge Size
**File:** `fragment_quiz.xml`

**Larger text:**
```xml
android:textSize="14sp"  <!-- Default: 12sp -->
```

**More padding (in drawable):**
```xml
<padding
    android:bottom="10dp"
    android:left="16dp"
    android:right="16dp"
    android:top="10dp" />
```

### Badge Position
**File:** `fragment_quiz.xml` lines 137-140

**Move left/right:**
```xml
android:layout_marginStart="40dp"  <!-- Default: 20dp -->
```

**Vertical position:**
```xml
app:layout_constraintVertical_bias="0.3"  <!-- Default: 0.5 (center) -->
<!-- 0.0 = top, 0.5 = center, 1.0 = bottom -->
```

---

## 🔄 Animation Direction Customization

### Header Animation Direction
**File:** `QuizFragment.kt`

**Current:** Right to Left
```kotlin
"translationX", 100f, 0f
```

**Left to Right:**
```kotlin
"translationX", -100f, 0f
```

**Top to Bottom:**
```kotlin
"translationY", -100f, 0f
```

### Footer Animation Direction
**Current:** Bottom-Right to Position

**Bottom-Left:**
```kotlin
val footerTransX = ObjectAnimator.ofFloat(binding.llFooter, "translationX", -100f, 0f)
```

**Straight Up (no horizontal):**
```kotlin
// Remove or comment out:
// val footerTransX = ...
// And in AnimatorSet, only use:
playTogether(footerAlpha, footerTransY)
```

---

## 🎪 Advanced Customizations

### Add Scale Animation
```kotlin
val footerScaleX = ObjectAnimator.ofFloat(binding.llFooter, "scaleX", 0.8f, 1f)
val footerScaleY = ObjectAnimator.ofFloat(binding.llFooter, "scaleY", 0.8f, 1f)

// Add to AnimatorSet:
playTogether(footerAlpha, footerTransY, footerTransX, footerScaleX, footerScaleY)
```

### Add Rotation Animation
```kotlin
val footerRotation = ObjectAnimator.ofFloat(binding.llFooter, "rotation", -10f, 0f)

// Add to AnimatorSet:
playTogether(footerAlpha, footerTransY, footerTransX, footerRotation)
```

### Sequential Instead of Together
```kotlin
AnimatorSet().apply {
    playSequentially(headerAnim, footerAnimSet)  // One after another
    start()
}
```

---

## 💡 Tips

1. **Test on Device:** Animations may feel different on real devices vs emulator
2. **Keep it Subtle:** Over-animation can feel cheap, not premium
3. **Consistency:** Match animation style with rest of your app
4. **Performance:** Avoid too many simultaneous animations
5. **Accessibility:** Ensure animations don't cause motion sickness (keep durations < 1s)

---

## 🐛 Troubleshooting

### Badge not appearing?
- Check `android:visibility` is not set to "gone"
- Verify `android:alpha` starts at 0 in XML
- Ensure `animateQuizStart()` is called

### Animation too fast/slow?
- Adjust `duration` values in `animateQuizStart()`
- Check device animation settings (Developer Options)

### Badge overlapping other elements?
- Adjust `android:elevation` in XML
- Modify constraints in layout
- Change `layout_marginStart` value

### Bottom sheet still dismissible?
- Verify `isCancelable = false` in both places
- Check `setCanceledOnTouchOutside(false)` is called
- Ensure you're using the latest code

---

## 📞 Quick Reference

**Badge Background:** `bg_category_badge.xml`  
**Badge Layout:** `fragment_quiz.xml` (lines 125-157)  
**Animations:** `QuizFragment.kt` - `animateQuizStart()`  
**Bottom Sheet:** `QuizCategoryBottomSheet.kt`  
**Navigation:** `QuizFragment.kt` - `showQuizCategorySelector()`  

---

Enjoy your premium quiz category selection UI! 🎉
