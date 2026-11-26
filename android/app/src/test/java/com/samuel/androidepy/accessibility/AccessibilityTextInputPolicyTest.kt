package com.samuel.androidepy.accessibility

import android.text.InputType
import org.junit.Assert.assertEquals
import org.junit.Test

class AccessibilityTextInputPolicyTest {

    @Test
    fun acceptsFocusedEditableEnabledCommonTextFields() {
        assertEquals(
            TextInputClassification.COMMON,
            AccessibilityTextInputPolicy.classify(
                editable = true,
                focused = true,
                enabled = true,
                password = false,
                accessibilitySensitive = false,
                inputType = InputType.TYPE_CLASS_TEXT
            )
        )
    }

    @Test
    fun acceptsCommonEmailVariation() {
        assertEquals(
            TextInputClassification.COMMON,
            AccessibilityTextInputPolicy.classify(
                editable = true,
                focused = true,
                enabled = true,
                password = false,
                accessibilitySensitive = false,
                inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS
            )
        )
    }

    @Test
    fun blocksPasswordAndAccessibilitySensitiveFields() {
        val password = AccessibilityTextInputPolicy.classify(
            editable = true,
            focused = true,
            enabled = true,
            password = true,
            accessibilitySensitive = false,
            inputType = InputType.TYPE_CLASS_TEXT
        )
        val sensitive = AccessibilityTextInputPolicy.classify(
            editable = true,
            focused = true,
            enabled = true,
            password = false,
            accessibilitySensitive = true,
            inputType = InputType.TYPE_CLASS_TEXT
        )
        assertEquals(TextInputClassification.SENSITIVE, password)
        assertEquals(TextInputClassification.SENSITIVE, sensitive)
    }

    @Test
    fun blocksPasswordInputVariationEvenWithoutPasswordFlag() {
        assertEquals(
            TextInputClassification.SENSITIVE,
            AccessibilityTextInputPolicy.classify(
                editable = true,
                focused = true,
                enabled = true,
                password = false,
                accessibilitySensitive = false,
                inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            )
        )
    }

    @Test
    fun treatsNumericAndUnknownInputTypesAsAmbiguous() {
        val numeric = AccessibilityTextInputPolicy.classify(
            editable = true,
            focused = true,
            enabled = true,
            password = false,
            accessibilitySensitive = false,
            inputType = InputType.TYPE_CLASS_NUMBER
        )
        val unknown = AccessibilityTextInputPolicy.classify(
            editable = true,
            focused = true,
            enabled = true,
            password = false,
            accessibilitySensitive = false,
            inputType = 0
        )
        assertEquals(TextInputClassification.AMBIGUOUS, numeric)
        assertEquals(TextInputClassification.AMBIGUOUS, unknown)
    }

    @Test
    fun rejectsUnfocusedReadonlyOrDisabledFields() {
        val unfocused = AccessibilityTextInputPolicy.classify(
            editable = true,
            focused = false,
            enabled = true,
            password = false,
            accessibilitySensitive = false,
            inputType = InputType.TYPE_CLASS_TEXT
        )
        val readonly = AccessibilityTextInputPolicy.classify(
            editable = false,
            focused = true,
            enabled = true,
            password = false,
            accessibilitySensitive = false,
            inputType = InputType.TYPE_CLASS_TEXT
        )
        val disabled = AccessibilityTextInputPolicy.classify(
            editable = true,
            focused = true,
            enabled = false,
            password = false,
            accessibilitySensitive = false,
            inputType = InputType.TYPE_CLASS_TEXT
        )
        assertEquals(TextInputClassification.UNSUPPORTED, unfocused)
        assertEquals(TextInputClassification.UNSUPPORTED, readonly)
        assertEquals(TextInputClassification.UNSUPPORTED, disabled)
    }
}
