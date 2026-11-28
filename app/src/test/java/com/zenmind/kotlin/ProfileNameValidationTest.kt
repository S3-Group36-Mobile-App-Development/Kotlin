package com.zenmind.kotlin

import com.zenmind.kotlin.features.profile.viewmodel.ProfileViewModel
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class ProfileNameValidationTest {
    @Test fun acceptsAccentsHyphensApostrophesAndTrimsSpaces() {
        assertNull(ProfileViewModel.validateName("  María-José O'Neill  "))
        assertNull(ProfileViewModel.validateName("Ana María"))
    }

    @Test fun rejectsBlankNumbersSymbolsAndWrongLength() {
        listOf("  ", "a", "--", "Ana 2", "Ana!", "a".repeat(51)).forEach {
            assertNotNull("Se aceptó $it", ProfileViewModel.validateName(it))
        }
    }
}
