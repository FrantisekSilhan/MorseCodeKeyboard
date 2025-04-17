package com.example.morsecodekeyboard

import android.annotation.SuppressLint
import android.inputmethodservice.InputMethodService
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.ExtractedTextRequest
import android.widget.Button
import android.widget.EditText
import android.widget.HorizontalScrollView
import android.widget.ImageButton
import android.widget.TextView

class MorseCodeKeyboardService : InputMethodService() {

    private var progress = ""
    private var writtenText = ""
    private lateinit var writtenTextView: EditText
    private lateinit var progressDisplayView: TextView
    private lateinit var shiftButton: ImageButton
    private lateinit var writtenTextScrollView: HorizontalScrollView

    private val autoPickDelay = 1000L
    private val handler = Handler(Looper.getMainLooper())
    private val autoPickRunnable = Runnable {
        if (progress.isNotEmpty()) {
            handleInput()
        }
    }

    private val backspaceRepeatDelay = 50L
    private val backspaceInitialDelay = 400L
    private val backspaceHandler = Handler(Looper.getMainLooper())
    private var backspaceHeld = false

    private val backspaceRunnable = object : Runnable {
        override fun run() {
            resetAutoPickTimer()
            if (progress.isNotEmpty()) {
                progress = progress.dropLast(1)
                progressDisplayView.text = getProgress()
            } else {
                shiftText()
            }
            if (backspaceHeld) {
                backspaceHandler.postDelayed(this, backspaceRepeatDelay)
            }
        }
    }


    private enum class ShiftType {
        LOWER,
        SINGLE,
        TOGGLED,
    }
    private var shiftState: ShiftType = ShiftType.LOWER

    private fun getCharacter(): String {
        var character = morseCodeMap[progress] ?: ""
        if (shiftState == ShiftType.LOWER) {
            character = character.lowercase()
        }
        return character
    }

    private fun getProgress(): String {
        return getCharacter().plus(" ").plus(progress)
    }

    private fun updateWrittenTextView() {
        writtenTextScrollView.post {
            writtenTextView.requestFocus()
        }
    }

    private fun appendText(text: String) {
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

    private fun shiftText() {
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

    private fun toggleShift(shiftButton: ImageButton) {
        shiftState = when (shiftState) {
            ShiftType.LOWER -> {
                shiftButton.setImageResource(R.drawable.shift_single)
                ShiftType.SINGLE
            }
            ShiftType.SINGLE -> {
                shiftButton.setImageResource(R.drawable.shift_toggled)
                ShiftType.TOGGLED
            }
            ShiftType.TOGGLED -> {
                shiftButton.setImageResource(R.drawable.shift_lower)
                ShiftType.LOWER
            }
        }

        progressDisplayView.text = getProgress()
    }

    private fun commitWrittenText() {
        if (writtenText.isNotEmpty()) {
            val inputConnection = currentInputConnection
            inputConnection?.let {
                val text = it.getExtractedText(ExtractedTextRequest(), 0)?.text
                val length = text?.length ?: 0
                if (length > 0) {
                    it.deleteSurroundingText(length, 0)
                    it.deleteSurroundingText(0, length)
                }
            }

            currentInputConnection.commitText(writtenText, 1)
            writtenTextView.setText("")
            writtenText = ""
            updateWrittenTextView()
        }
    }

    private fun resetAutoPickTimer() {
        handler.removeCallbacks(autoPickRunnable)
        handler.postDelayed(autoPickRunnable, autoPickDelay)
    }

    fun onInput() {
        progressDisplayView.text = getProgress()
        resetAutoPickTimer()
    }

    fun handleInput() {
        when (val character = getCharacter()) {
            "[space]" -> {
                appendText(" ")
            }
            "[enter]" -> {
                if (writtenText.isEmpty()) {
                    progress = ""
                    progressDisplayView.text = ""
                    return
                }
                commitWrittenText()
                val handled = currentInputConnection.performEditorAction(EditorInfo.IME_ACTION_SEARCH)
                if (!handled) {
                    currentInputConnection.performEditorAction(EditorInfo.IME_ACTION_DONE)
                }

                progress = ""
                progressDisplayView.text = ""
                shiftState = ShiftType.LOWER
                shiftButton.setImageResource(R.drawable.shift_lower)
                requestHideSelf(0)
            }
            "[shift]" -> {
                toggleShift(shiftButton)
            }
            "[backspace]" -> {
                shiftText()
            }
            else -> {
                appendText(character)
            }
        }

        progress = ""
        progressDisplayView.text = ""

        if (shiftState === ShiftType.SINGLE) {
            shiftState = ShiftType.LOWER
            shiftButton.setImageResource(R.drawable.shift_lower)
        }
    }

    @SuppressLint("ClickableViewAccessibility", "InflateParams")
    override fun onCreateInputView(): View {
        val keyboardView = LayoutInflater.from(this).inflate(R.layout.keyboard_layout, null)

        val dotButton: Button = keyboardView.findViewById(R.id.dotButton)
        val lineButton: Button = keyboardView.findViewById(R.id.lineButton)
        val spaceButton: ImageButton = keyboardView.findViewById(R.id.spaceButton)
        val backspaceButton: ImageButton = keyboardView.findViewById(R.id.backspaceButton)
        shiftButton = keyboardView.findViewById(R.id.shiftButton)
        progressDisplayView = keyboardView.findViewById(R.id.progressDisplay)
        writtenTextView = keyboardView.findViewById(R.id.writtenText)
        writtenTextScrollView = keyboardView.findViewById(R.id.writtenTextScrollView)
        writtenTextView.requestFocus()

        dotButton.setOnClickListener {
            if (progress.length >= 6) {
                progress = ""
            } else {
                progress += "."
            }
            onInput()
        }

        lineButton.setOnClickListener {
            if (progress.length >= 6) {
                progress = ""
            } else {
                progress += "-"
            }
            onInput()
        }

        spaceButton.setOnClickListener {
            resetAutoPickTimer()
            if (progress.isNotEmpty()) {
                handleInput()
            } else {
                appendText(" ")
            }
        }

        backspaceButton.setOnClickListener {
            resetAutoPickTimer()
            if (progress.isNotEmpty()) {
                progress = progress.dropLast(1)
                progressDisplayView.text = getProgress()
            } else {
                shiftText()
            }
        }

        backspaceButton.setOnTouchListener { v, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    v.isPressed = true
                    backspaceHeld = true

                    resetAutoPickTimer()
                    if (progress.isNotEmpty()) {
                        progress = progress.dropLast(1)
                        progressDisplayView.text = getProgress()
                    } else {
                        shiftText()
                    }

                    backspaceHandler.postDelayed(backspaceRunnable, backspaceInitialDelay)
                    true
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    v.isPressed = false
                    backspaceHeld = false
                    backspaceHandler.removeCallbacks(backspaceRunnable)
                    true
                }
                else -> false
            }
        }

        shiftButton.setOnClickListener {
            toggleShift(shiftButton)
            resetAutoPickTimer()
        }

        updateWrittenTextView()
        return keyboardView
    }

    override fun onFinishInputView(finishingInput: Boolean) {
        super.onFinishInputView(finishingInput)

        handler.removeCallbacks(autoPickRunnable)
        backspaceHandler.removeCallbacks(backspaceRunnable)

        progress = ""
        writtenText = ""


        progressDisplayView.text = ""
        writtenTextView.setText("")

        shiftState = ShiftType.LOWER
        shiftButton.setImageResource(R.drawable.shift_lower)
    }

    private val morseCodeMap = mapOf(
        ".-" to "A",
        "-..." to "B",
        "-.-." to "C",
        "-.." to "D",
        "." to "E",
        "..-." to "F",
        "--." to "G",
        "...." to "H",
        ".." to "I",
        ".---" to "J",
        "-.-" to "K",
        ".-.." to "L",
        "--" to "M",
        "-." to "N",
        "---" to "O",
        ".--." to "P",
        "--.-" to "Q",
        ".-." to "R",
        "..." to "S",
        "-" to "T",
        "..-" to "U",
        "...-" to "V",
        ".--" to "W",
        "-..-" to "X",
        "-.--" to "Y",
        "--.." to "Z",
        ".----" to "1",
        "..---" to "2",
        "...--" to "3",
        "....-" to "4",
        "....." to "5",
        "-...." to "6",
        "--..." to "7",
        "---.." to "8",
        "----." to "9",
        "-----" to "0",
        ".-.-.-" to ".",
        "--..--" to ",",
        "..--.." to "?",
        ".----." to "'",
        "-.-.--" to "!",
        "-..-." to "/",
        "-.--." to "(",
        "-.--.-" to ")",
        ".-..." to "&",
        "---..." to ":",
        "-.-.-." to ";",
        "-...-" to "=",
        ".-.-." to "+",
        "-....-" to "-",
        "..--.-" to "_",
        ".--.-." to "@",
        ".-..-." to "\"",
        "...-." to "*",
        "-.-.-" to "\\",
        "---.-" to "%",
        "--.-." to "#",
        "--.-.-" to "|",
        "......" to "^",
        ".---.." to "~",
        "-..-.-" to "`",
        "...-.." to "$",
        ".--.." to "[",
        ".--..-" to "]",
        ".--.-" to "{",
        ".--.--" to "}",
        "-.---" to "<",
        "-.----" to ">",
        "..--" to "[space]",
        ".-.-" to "[enter]",
        "....-." to "[shift]",
        "----" to "[backspace]",
    )
}
