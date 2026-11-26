package com.samuel.androidepy.accessibility

import android.text.InputType

internal enum class TextInputClassification {
    COMMON,
    SENSITIVE,
    AMBIGUOUS,
    UNSUPPORTED
}

/**
 * Conservative allow-list for text fields. It intentionally avoids reading
 * the node's current text, hint, label, or content description.
 */
internal object AccessibilityTextInputPolicy {

    fun classify(
        editable: Boolean,
        focused: Boolean,
        enabled: Boolean,
        password: Boolean,
        accessibilitySensitive: Boolean,
        inputType: Int
    ): TextInputClassification {
        if (!editable || !focused || !enabled) {
            return TextInputClassification.UNSUPPORTED
        }

        val variation = inputType and InputType.TYPE_MASK_VARIATION
        val passwordVariation = variation == InputType.TYPE_TEXT_VARIATION_PASSWORD ||
            variation == InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD ||
            variation == InputType.TYPE_TEXT_VARIATION_WEB_PASSWORD

        if (password || accessibilitySensitive || passwordVariation) {
            return TextInputClassification.SENSITIVE
        }

        val inputClass = inputType and InputType.TYPE_MASK_CLASS
        if (inputClass != InputType.TYPE_CLASS_TEXT) {
            return TextInputClassification.AMBIGUOUS
        }

        val commonVariations = setOf(
            InputType.TYPE_TEXT_VARIATION_NORMAL,
            InputType.TYPE_TEXT_VARIATION_URI,
            InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS,
            InputType.TYPE_TEXT_VARIATION_WEB_EDIT_TEXT,
            InputType.TYPE_TEXT_VARIATION_PERSON_NAME,
            InputType.TYPE_TEXT_VARIATION_POSTAL_ADDRESS,
            InputType.TYPE_TEXT_VARIATION_SHORT_MESSAGE,
            InputType.TYPE_TEXT_VARIATION_LONG_MESSAGE,
            InputType.TYPE_TEXT_VARIATION_FILTER,
            InputType.TYPE_TEXT_VARIATION_PHONETIC
        )

        return if (variation in commonVariations) {
            TextInputClassification.COMMON
        } else {
            TextInputClassification.AMBIGUOUS
        }
    }
}
