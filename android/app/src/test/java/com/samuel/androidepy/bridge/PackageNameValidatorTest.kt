package com.samuel.androidepy.bridge

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PackageNameValidatorTest {

    @Test
    fun acceptsCommonAndroidPackageNames() {
        assertTrue(PackageNameValidator.isValid("com.example.app"))
        assertTrue(PackageNameValidator.isValid("org.example_2.application"))
    }

    @Test
    fun rejectsMalformedPackageNames() {
        assertFalse(PackageNameValidator.isValid(""))
        assertFalse(PackageNameValidator.isValid("com"))
        assertFalse(PackageNameValidator.isValid("com..example"))
        assertFalse(PackageNameValidator.isValid("1com.example"))
        assertFalse(PackageNameValidator.isValid("com.example."))
        assertFalse(PackageNameValidator.isValid("com.example.app!"))
    }
}
