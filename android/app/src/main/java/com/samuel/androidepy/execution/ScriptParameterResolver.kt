package com.samuel.androidepy.execution

import org.json.JSONArray
import org.json.JSONObject

/** Validates UI-provided values against a script's declarative parameter schema. */
object ScriptParameterResolver {
    fun resolve(definitionsJson: String, valuesJson: String): String {
        val definitions = JSONArray(definitionsJson)
        val supplied = JSONObject(valuesJson)
        val resolved = JSONObject()
        val knownNames = mutableSetOf<String>()

        for (index in 0 until definitions.length()) {
            val definition = definitions.optJSONObject(index)
                ?: throw IllegalArgumentException("Cada parâmetro precisa ser um objeto JSON.")
            val name = definition.optString("name", "").trim()
            val type = definition.optString("type", "string").lowercase()
            require(name.matches(Regex("[A-Za-z_][A-Za-z0-9_]*"))) {
                "Nome de parâmetro inválido: '$name'."
            }
            require(knownNames.add(name)) { "Parâmetro duplicado: '$name'." }

            val value = when {
                supplied.has(name) -> supplied.get(name)
                definition.has("default") -> definition.get("default")
                definition.optBoolean("required", false) ->
                    throw IllegalArgumentException("Informe o parâmetro obrigatório '$name'.")
                else -> continue
            }
            validateType(name, type, value)
            resolved.put(name, value)
        }

        val unknown = supplied.keys().asSequence().filterNot { it in knownNames }.toList()
        require(unknown.isEmpty()) { "Parâmetros não declarados: ${unknown.joinToString()}." }
        return resolved.toString()
    }

    private fun validateType(name: String, type: String, value: Any) {
        val valid = when (type) {
            "string" -> value is String
            "integer" -> value is Number && value.toDouble() % 1.0 == 0.0
            "number" -> value is Number
            "boolean" -> value is Boolean
            "json" -> true
            else -> throw IllegalArgumentException("Tipo de parâmetro não suportado: '$type'.")
        }
        require(valid) { "Valor incompatível com o tipo '$type' do parâmetro '$name'." }
    }
}
