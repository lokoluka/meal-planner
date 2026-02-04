package com.example.mealplanner.ai

import com.example.mealplanner.data.MeasurementUnit
import com.example.mealplanner.data.IngredientCategory
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.generationConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

data class ParsedRecipe(
    val name: String,
    val instructions: String,
    val servings: Int,
    val ingredients: List<ParsedIngredient>
)

data class ParsedIngredient(
    val name: String,
    val amount: Double,
    val unit: String,
    val category: String
) {
    fun toMeasurementUnit(): MeasurementUnit {
        return when (unit.uppercase()) {
            "GRAM" -> MeasurementUnit.GRAM
            "MILLILITER" -> MeasurementUnit.MILLILITER
            "TEASPOON" -> MeasurementUnit.TEASPOON
            "TABLESPOON" -> MeasurementUnit.TABLESPOON
            "CUP" -> MeasurementUnit.CUP
            "PIECE" -> MeasurementUnit.PIECE
            else -> MeasurementUnit.GRAM
        }
    }
    
    fun toIngredientCategory(): IngredientCategory {
        return when (category.uppercase()) {
            "MEAT" -> IngredientCategory.MEAT
            "FISH" -> IngredientCategory.FISH
            "DAIRY" -> IngredientCategory.DAIRY
            "VEGETABLES" -> IngredientCategory.VEGETABLES
            "FRUITS" -> IngredientCategory.FRUITS
            "PANTRY" -> IngredientCategory.PANTRY
            "SPICES" -> IngredientCategory.SPICES
            "BEVERAGES" -> IngredientCategory.BEVERAGES
            else -> IngredientCategory.OTHER
        }
    }
}

class RecipeImportService {
    
    // Replace with your Gemini API key
    // Get one free at: https://makersuite.google.com/app/apikey
    private val apiKey = "AIzaSyActiSfIx2nYIPsxaPnjjKX7Ucqm5D1Eto"
    
    private val modelNames = listOf(
        "gemini-3-flash-preview"
    )

    private fun createModel(modelName: String): GenerativeModel {
        return GenerativeModel(
            modelName = modelName,
            apiKey = apiKey,
            generationConfig = generationConfig {
                temperature = 0.2f
                topK = 1
                topP = 0.8f
                maxOutputTokens = 2048
            }
        )
    }
    
    suspend fun parseRecipe(input: String): Result<ParsedRecipe> = withContext(Dispatchers.IO) {
        var lastError: Exception? = null
        val prompt = buildPrompt(input)

        for (modelName in modelNames) {
            try {
                val response = createModel(modelName).generateContent(prompt)
                val jsonText = response.text?.trim()
                    ?: return@withContext Result.failure(Exception("Empty response"))

                // Extract JSON from markdown code blocks if present
                val cleanJson = jsonText
                    .removePrefix("```json")
                    .removePrefix("```")
                    .removeSuffix("```")
                    .trim()

                val recipe = parseJsonResponse(cleanJson)
                return@withContext Result.success(recipe)
            } catch (e: Exception) {
                lastError = e
            }
        }

        Result.failure(lastError ?: Exception("Failed to parse recipe"))
    }
    
    private fun buildPrompt(input: String): String {
        return """
You are a recipe parser. Extract the recipe information from the following text or URL content and return it as JSON.

Input:
$input

Return ONLY a JSON object with this exact structure (no markdown, no extra text):
{
  "name": "Recipe name",
  "instructions": "Cooking instructions",
  "servings": 4,
  "ingredients": [
    {
      "name": "Ingredient name",
      "amount": 250.0,
      "unit": "GRAM",
      "category": "PANTRY"
    }
  ]
}

Rules:
- Use these units ONLY: GRAM, MILLILITER, TEASPOON, TABLESPOON, CUP, PIECE
- Use these categories ONLY: MEAT, FISH, DAIRY, VEGETABLES, FRUITS, PANTRY, SPICES, BEVERAGES, OTHER
- Convert all measurements to numbers (1/2 = 0.5, 1 1/2 = 1.5)
- For items like "1 onion", use unit: "PIECE"
- Normalize ingredient names (e.g., "tomatoes" -> "Tomato")
- Be precise with amounts and units
        """.trimIndent()
    }
    
    private fun parseJsonResponse(json: String): ParsedRecipe {
        val obj = JSONObject(json)
        
        val name = obj.getString("name")
        val instructions = obj.getString("instructions")
        val servings = obj.getInt("servings")
        
        val ingredientsArray = obj.getJSONArray("ingredients")
        val ingredients = mutableListOf<ParsedIngredient>()
        
        for (i in 0 until ingredientsArray.length()) {
            val ing = ingredientsArray.getJSONObject(i)
            ingredients.add(
                ParsedIngredient(
                    name = ing.getString("name"),
                    amount = ing.getDouble("amount"),
                    unit = ing.getString("unit"),
                    category = ing.getString("category")
                )
            )
        }
        
        return ParsedRecipe(name, instructions, servings, ingredients)
    }
}
