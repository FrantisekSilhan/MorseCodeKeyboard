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
import com.example.morsecodekeyboard.data.MorseCodeMap
import com.example.morsecodekeyboard.handlers.MorseInputHandler

class MorseCodeKeyboardService : InputMethodService() {

    private lateinit var inputHandler: MorseInputHandler
    private lateinit var writtenTextView: EditText
    private lateinit var progressDisplayView: TextView
    private lateinit var shiftButton: ImageButton
    private lateinit var writtenTextScrollView: HorizontalScrollView

    private val backspaceRepeatDelay = 50L
    private val backspaceInitialDelay = 400L
    private val backspaceHandler = Handler(Looper.getMainLooper())
    private var backspaceHeld = false

    private val backspaceRunnable = object : Runnable {
        override fun run() {
            inputHandler.resetAutoPickTimer()
            if (inputHandler.progress.isNotEmpty()) {
                inputHandler.progress = inputHandler.progress.dropLast(1)
                progressDisplayView.text = inputHandler.getCurrentProgress()
            } else {
                inputHandler.shiftText()
            }
            if (backspaceHeld) {
                backspaceHandler.postDelayed(this, backspaceRepeatDelay)
            }
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

        inputHandler = MorseInputHandler(
            MorseCodeMap.map,
            writtenTextView,
            progressDisplayView,
            shiftButton,
            writtenTextScrollView,
            onCommit = { text ->
                val inputConnection = currentInputConnection
                inputConnection?.let {
                    val extracted = it.getExtractedText(ExtractedTextRequest(), 0)?.text
                    val length = extracted?.length ?: 0
                    if (length > 0) {
                        it.deleteSurroundingText(length, 0)
                        it.deleteSurroundingText(0, length)
                    }
                }
                currentInputConnection.commitText(text, 1)
            },
            onHideKeyboard = {
                val handled = currentInputConnection.performEditorAction(EditorInfo.IME_ACTION_SEARCH)
                if (!handled) {
                    currentInputConnection.performEditorAction(EditorInfo.IME_ACTION_DONE)
                }
                requestHideSelf(0)
            },
            onShiftStateChanged = { }
        )

        dotButton.setOnClickListener {
            if (inputHandler.progress.length >= 6) {
                inputHandler.progress = ""
            } else {
                inputHandler.progress += "."
            }
            inputHandler.onInput()
        }

        lineButton.setOnClickListener {
            if (inputHandler.progress.length >= 6) {
                inputHandler.progress = ""
            } else {
                inputHandler.progress += "-"
            }
            inputHandler.onInput()
        }

        spaceButton.setOnClickListener {
            inputHandler.resetAutoPickTimer()
            if (inputHandler.progress.isNotEmpty()) {
                inputHandler.handleInput()
            } else {
                inputHandler.appendText(" ")
            }
        }

        backspaceButton.setOnClickListener {
            inputHandler.resetAutoPickTimer()
            if (inputHandler.progress.isNotEmpty()) {
                inputHandler.progress = inputHandler.progress.dropLast(1)
                progressDisplayView.text = inputHandler.getCurrentProgress()
            } else {
                inputHandler.shiftText()
            }
        }

        backspaceButton.setOnTouchListener { v, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    v.isPressed = true
                    backspaceHeld = true

                    inputHandler.resetAutoPickTimer()
                    if (inputHandler.progress.isNotEmpty()) {
                        inputHandler.progress = inputHandler.progress.dropLast(1)
                        progressDisplayView.text = inputHandler.getCurrentProgress()
                    } else {
                        inputHandler.shiftText()
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
            inputHandler.toggleShift()
            inputHandler.resetAutoPickTimer()
        }

        inputHandler.updateWrittenTextView()
        return keyboardView
    }

    override fun onFinishInputView(finishingInput: Boolean) {
        super.onFinishInputView(finishingInput)
        backspaceHandler.removeCallbacks(backspaceRunnable)
        inputHandler.clear()
    }
}
