# AI Recipe Import Setup

## Get Your Gemini API Key

1. Go to [Google AI Studio](https://makersuite.google.com/app/apikey)
2. Sign in with your Google account
3. Click "Create API Key"
4. Copy the generated API key

## Add API Key to the App

1. Open `app/src/main/java/com/example/mealplanner/ai/RecipeImportService.kt`
2. Find line with: `private val apiKey = "YOUR_GEMINI_API_KEY_HERE"`
3. Replace `YOUR_GEMINI_API_KEY_HERE` with your actual API key
4. Save the file
5. Rebuild and reinstall the app

## How to Use

1. Open the Recipes screen
2. Tap the "Import" button (magnifying glass icon)
3. Paste recipe text or URL in the dialog
4. Tap "Import"
5. The AI will parse the recipe and populate the form
6. Review and edit if needed
7. Save the recipe

## Features

- **Text Input**: Paste any recipe text
- **URL Support**: Paste a recipe URL (coming soon)
- **Automatic Parsing**: AI extracts:
  - Recipe name
  - Instructions
  - Servings
  - Ingredients with amounts and units
  - Ingredient categories (Meat, Fish, Dairy, etc.)
- **Unit Conversion**: Automatically converts to metric units
- **Smart Categorization**: Assigns ingredients to proper categories

## Supported Units

The AI will convert to these units:
- Weight: grams (g)
- Volume: milliliters (ml), teaspoons (tsp), tablespoons (tbsp), cups
- Count: pieces (pc)

## Example Recipe Text

```
Chicken Pasta

Ingredients:
- 500g chicken breast
- 400g pasta
- 2 tbsp olive oil
- 3 cloves garlic
- 1 cup heavy cream
- Salt and pepper to taste

Instructions:
1. Cook pasta according to package directions
2. Cut chicken into bite-sized pieces
3. Heat oil in pan, cook chicken until golden
4. Add garlic and cook for 1 minute
5. Add cream and simmer for 5 minutes
6. Toss with cooked pasta
7. Season with salt and pepper

Serves 4
```

## Notes

- The Gemini API has a free tier with generous limits
- Internet connection required for AI parsing
- First import may take a few seconds
- Review parsed data before saving - AI is smart but not perfect!
