package com.example.morsecodekeyboard

import android.inputmethodservice.InputMethodService
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.TextView

class MorseCodeKeyboardService : InputMethodService() {

    private var progress = ""

    override fun onCreateInputView(): View {
        val keyboardView = LayoutInflater.from(this).inflate(R.layout.keyboard_layout, null)

        val dotButton: Button = keyboardView.findViewById(R.id.dotButton)
        val lineButton: Button = keyboardView.findViewById(R.id.lineButton)
        val spaceButton: Button = keyboardView.findViewById(R.id.spaceButton)
        val backspaceButton: Button = keyboardView.findViewById(R.id.backspaceButton)
        val progressDisplay: TextView = keyboardView.findViewById(R.id.progressDisplay)

        dotButton.setOnClickListener {
            progress += "."
            progressDisplay.text = progress
        }

        lineButton.setOnClickListener {
            progress += "-"
            progressDisplay.text = progress
        }

        spaceButton.setOnClickListener {
            if (progress.isNotEmpty()) {
                val character = morseCodeMap[progress] ?: ""
                currentInputConnection.commitText(character, 1)
                progress = ""
                progressDisplay.text = ""
            } else {
                currentInputConnection.commitText(" ", 1)
            }
        }

        backspaceButton.setOnClickListener {
            if (progress.isNotEmpty()) {
                progress = ""
                progressDisplay.text = ""
            } else {
                currentInputConnection.deleteSurroundingText(1, 0)
            }
        }

        return keyboardView
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
