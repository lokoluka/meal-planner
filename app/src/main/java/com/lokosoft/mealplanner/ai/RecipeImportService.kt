package com.lokosoft.mealplanner.ai

import com.google.ai.client.generativeai.GenerativeModel
import com.lokosoft.mealplanner.BuildConfig
import com.lokosoft.mealplanner.data.Ingredient
import com.lokosoft.mealplanner.data.MeasurementUnit
import com.lokosoft.mealplanner.data.Recipe
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.util.Locale


// Data class to hold ingredient information for the caller
data class IngredientInfo(val ingredient: Ingredient, val amount: Double, val unit: MeasurementUnit)

// 1. Data classes for parsing the structured recipe data from the Gemini API

/**
 * Represents a recipe parsed from the Gemini API response. This class is designed to be
 * easily serializable from JSON.
 */
@Serializable
data class ParsedRecipe(
    val name: String,
    val instructions: List<String>,
    val ingredients: List<ParsedIngredient>,
    val servings: Int = 1
) {
    /**
     * Converts this parsed recipe into a [Recipe] domain object.
     */
    fun toRecipe(): Recipe = Recipe(
        name = name,
        instructions = instructions.joinToString(separator = "\n"),
        servings = servings
    )
}

/**
 * Represents a single ingredient parsed from the Gemini API response.
 */
@Serializable
data class ParsedIngredient(
    val name: String,
    val amount: Double,
    val unit: String,
    val category: String
) {
    /**
     * Converts this parsed ingredient into an [Ingredient] domain object.
     */
    fun toIngredient(): Ingredient = Ingredient(
        name = name.lowercase(Locale.getDefault()),
        // The category is not used in the current implementation but is parsed for future use.
    )

    /**
     * Converts the string unit from the API response to a [MeasurementUnit] enum.
     * Defaults to [MeasurementUnit.PIECE] if the unit is unknown.
     */
    fun toMeasurementUnit(): MeasurementUnit {
        return MeasurementUnit.entries.firstOrNull { it.name.equals(unit, ignoreCase = true) } 
            ?: MeasurementUnit.PIECE
    }
    
    /**
     * Converts the string category from the API response to an [IngredientCategory] enum.
     * Defaults to [IngredientCategory.OTHER] if the category is unknown.
     */
    fun toIngredientCategory(): com.lokosoft.mealplanner.data.IngredientCategory {
        return com.lokosoft.mealplanner.data.IngredientCategory.entries.firstOrNull { 
            it.name.equals(category, ignoreCase = true) 
        } ?: com.lokosoft.mealplanner.data.IngredientCategory.OTHER
    }
}

/**
 * Service class for importing recipes using the Gemini API.
 *
 * @property lastError Stores the last error message that occurred during recipe parsing.
 */
class RecipeImportService {
    var lastError: String? = null
        private set

    /**
     * Parses a recipe from a given URL or plain text.
     *
     * @param text The recipe content (either a URL or plain text).
     * @return A [Result] containing the [ParsedRecipe], or an error if parsing fails.
     */
    suspend fun parseRecipe(text: String): Result<ParsedRecipe> {
        // 1. Prepare the prompt for the Gemini API
        val prompt = "You are a master chef with a deep understanding of recipes. " +
                "Your task is to extract the recipe title, instructions, and a list of ingredients " +
                "from the provided text. The text could be a just a recipe name or a full recipe. " +
                "Ensure that each ingredient includes its name, quantity, and measurement unit. " +
                "Detect the language of the recipe and respond in the same language. " +
                "Please format the output as a JSON object with the following structure: " +
                "name (String), instructions (List<String>), ingredients (List<Object> with name, amount, unit, and category), and servings (Int).\n\n" +
                "For the category, classify each ingredient into one of the following: " +
                "'DAIRY', 'MEAT', 'VEGETABLE', 'FRUIT', 'SPICE', 'GRAIN', 'LIQUID', 'SWEETENER', or 'OTHER'.\n\n" +
                "Here is the recipe text:\n$text"

        // 2. Define a list of potential Gemini models to try
        val model =  "gemini-3-flash-preview"

        // 3. Loop through the models and attempt to generate the recipe

            try {
                val generativeModel = GenerativeModel(
                    modelName = model,
                    apiKey = BuildConfig.GEMINI_API_KEY
                )

                val response = generativeModel.generateContent(prompt)

                response.text?.let { jsonString ->
                    // 4. Find and parse the JSON part of the response
                    val startIndex = jsonString.indexOf('{')
                    val endIndex = jsonString.lastIndexOf('}')
                    if (startIndex == -1 || endIndex == -1 || startIndex > endIndex) {
                        throw IllegalStateException("No valid JSON found in the response")
                    }
                    val jsonSubstring = jsonString.substring(startIndex, endIndex + 1)
                    val parsedRecipe = Json.decodeFromString<ParsedRecipe>(jsonSubstring)

                    return Result.success(parsedRecipe)
                }
            } catch (e: Exception) {
                // 6. Store the last error and try the next model
                lastError = "Error with model $model: ${e.message}"
                e.printStackTrace() // Log the full stack trace for debugging
            }
        

        // 7. Return failure if all models fail
        return Result.failure(Exception(lastError ?: "Failed to parse recipe"))
    }
}