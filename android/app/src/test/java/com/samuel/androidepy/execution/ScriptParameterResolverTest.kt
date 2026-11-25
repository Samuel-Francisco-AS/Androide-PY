package com.samuel.androidepy.execution

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import org.json.JSONObject

class ScriptParameterResolverTest {
    @Test
    fun resolvesRequiredOptionalAndDefaultValues() {
        val definitions = """[
            {"name":"query","type":"string","required":true},
            {"name":"limit","type":"integer","default":10},
            {"name":"verbose","type":"boolean"}
        ]"""
        val result = JSONObject(ScriptParameterResolver.resolve(definitions, """{"query":"python","verbose":true}"""))
        assertEquals("python", result.getString("query"))
        assertEquals(10, result.getInt("limit"))
        assertEquals(true, result.getBoolean("verbose"))
    }

    @Test
    fun rejectsMissingRequiredValues() {
        assertThrows(IllegalArgumentException::class.java) {
            ScriptParameterResolver.resolve("""[{"name":"token","type":"string","required":true}]""", "{}")
        }
    }

    @Test
    fun rejectsValuesWithWrongTypes() {
        assertThrows(IllegalArgumentException::class.java) {
            ScriptParameterResolver.resolve("""[{"name":"count","type":"integer"}]""", """{"count":"many"}""")
        }
    }

    @Test
    fun rejectsUndeclaredValues() {
        assertThrows(IllegalArgumentException::class.java) {
            ScriptParameterResolver.resolve("[]", """{"surprise":true}""")
        }
    }
}
