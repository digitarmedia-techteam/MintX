package com.digitar.mintx.utils

import android.content.Context
import android.util.AttributeSet
import android.view.KeyEvent
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import android.view.inputmethod.InputConnectionWrapper
import androidx.appcompat.widget.AppCompatEditText

class OtpEditText : AppCompatEditText {

    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    private var onBackspacePressedListener: (() -> Unit)? = null

    fun setOnBackspacePressedListener(listener: () -> Unit) {
        this.onBackspacePressedListener = listener
    }

    override fun onCreateInputConnection(outAttrs: EditorInfo): InputConnection? {
        val conn = super.onCreateInputConnection(outAttrs)
        return conn?.let { OtpInputConnection(it, true) }
    }

    private inner class OtpInputConnection(target: InputConnection, mutable: Boolean) :
        InputConnectionWrapper(target, mutable) {

        override fun sendKeyEvent(event: KeyEvent): Boolean {
            if (event.action == KeyEvent.ACTION_DOWN && event.keyCode == KeyEvent.KEYCODE_DEL) {
                if (text.isNullOrEmpty()) {
                    onBackspacePressedListener?.invoke()
                }
            }
            return super.sendKeyEvent(event)
        }
        
        override fun deleteSurroundingText(beforeLength: Int, afterLength: Int): Boolean {
            // Some keyboards use this instead of key events
            if (beforeLength == 1 && afterLength == 0 && text.isNullOrEmpty()) {
                onBackspacePressedListener?.invoke()
                return true
            }
            return super.deleteSurroundingText(beforeLength, afterLength)
        }
    }
}
