package com.example.morsecodekeyboard.handlers

import android.os.Handler
import android.os.Looper
import android.widget.EditText
import android.widget.TextView
import android.widget.ImageButton
import android.widget.HorizontalScrollView
import com.example.morsecodekeyboard.R
import com.example.morsecodekeyboard.data.ShiftState

class MorseInputHandler(
    private val morseMap: Map<String, String>,
    private val writtenTextView: EditText,
    private val progressDisplayView: TextView,
    private val shiftButton: ImageButton,
    private val writtenTextScrollView: HorizontalScrollView,
    private val onCommit: (String) -> Unit,
    private val onHideKeyboard: () -> Unit,
    private val onShiftStateChanged: (ShiftState) -> Unit
) {
    var progress = ""
    var writtenText = ""
    var shiftState: ShiftState = ShiftState.LOWER

    private val autoPickDelay = 1000L
    private val handler = Handler(Looper.getMainLooper())
    private val autoPickRunnable = Runnable {
        if (progress.isNotEmpty()) {
            handleInput()
        }
    }

    fun getCharacter(): String {
        var character = morseMap[progress] ?: ""
        if (shiftState == ShiftState.LOWER) {
            character = character.lowercase()
        }
        return character
    }

    fun getCurrentProgress(): String {
        return getCharacter().plus(" ").plus(progress)
    }

    fun updateWrittenTextView() {
        writtenTextScrollView.post {
            writtenTextView.requestFocus()
        }
    }

    fun appendText(text: String) {
        val start = writtenTextView.selectionStart
        val end = writtenTextView.selectionEnd
        writtenTextView.text.replace(
            start.coerceAtMost(end),
            start.coerceAtLeast(end),
            text,
            0,
            text.length
        )
        val newCursor = start.coerceAtMost(end) + text.length
        writtenTextView.setSelection(newCursor)
        writtenText = writtenTextView.text.toString()
        updateWrittenTextView()
    }

    fun shiftText() {
        val start = writtenTextView.selectionStart
        val end = writtenTextView.selectionEnd
        if (start != end) {
            writtenTextView.text.delete(start, end)
            writtenTextView.setSelection(start)
        } else if (start > 0) {
            writtenTextView.text.delete(start - 1, start)
            writtenTextView.setSelection(start - 1)
        }
        writtenText = writtenTextView.text.toString()
        updateWrittenTextView()
    }

    fun toggleShift() {
        shiftState = when (shiftState) {
            ShiftState.LOWER -> {
                shiftButton.setImageResource(R.drawable.shift_single)
                ShiftState.SINGLE
            }
            ShiftState.SINGLE -> {
                shiftButton.setImageResource(R.drawable.shift_toggled)
                ShiftState.TOGGLED
            }
            ShiftState.TOGGLED -> {
                shiftButton.setImageResource(R.drawable.shift_lower)
                ShiftState.LOWER
            }
        }
        progressDisplayView.text = getCurrentProgress()
        onShiftStateChanged(shiftState)
    }

    fun commitWrittenText() {
        if (writtenText.isNotEmpty()) {
            onCommit(writtenText)
            writtenTextView.setText("")
            writtenText = ""
            updateWrittenTextView()
        }
    }

    fun resetAutoPickTimer() {
        handler.removeCallbacks(autoPickRunnable)
        handler.postDelayed(autoPickRunnable, autoPickDelay)
    }

    fun onInput() {
        progressDisplayView.text = getCurrentProgress()
        resetAutoPickTimer()
    }

    fun handleInput() {
        when (val character = getCharacter()) {
            "[space]" -> appendText(" ")
            "[enter]" -> {
                if (writtenText.isEmpty()) {
                    progress = ""
                    progressDisplayView.text = ""
                    return
                }
                commitWrittenText()
                onHideKeyboard()
                progress = ""
                progressDisplayView.text = ""
                shiftState = ShiftState.LOWER
                shiftButton.setImageResource(R.drawable.shift_lower)
            }
            "[shift]" -> toggleShift()
            "[backspace]" -> shiftText()
            else -> appendText(character)
        }
        progress = ""
        progressDisplayView.text = ""
        if (shiftState == ShiftState.SINGLE) {
            shiftState = ShiftState.LOWER
            shiftButton.setImageResource(R.drawable.shift_lower)
            onShiftStateChanged(shiftState)
        }
    }

    fun clear() {
        handler.removeCallbacks(autoPickRunnable)
        progress = ""
        writtenText = ""
        progressDisplayView.text = ""
        writtenTextView.setText("")
        shiftState = ShiftState.LOWER
        shiftButton.setImageResource(R.drawable.shift_lower)
    }
}
