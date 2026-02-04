package com.example.mealplanner.data

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.Junction
import androidx.room.PrimaryKey
import androidx.room.Relation

enum class IngredientCategory {
    MEAT,
    FISH,
    DAIRY,
    VEGETABLES,
    FRUITS,
    PANTRY,
    SPICES,
    BEVERAGES,
    OTHER
}

enum class PackageType {
    UNIT,       // Individual items (1 tomato, 1 onion)
    PACKAGE,    // Pre-packaged (500g package, 250g box)
    BULK,       // Sold by weight (per kg, per 100g)
    BOTTLE,     // Liquids in bottles
    CARTON,     // Milk, juice cartons
    BAG,        // Bags of rice, flour, etc.
    CAN,        // Canned goods
    JAR,        // Jars of sauces, pickles, etc.
    OTHER
}

@Entity(tableName = "ingredients")
data class Ingredient(
    @PrimaryKey(autoGenerate = true)
    val ingredientId: Long = 0,
    val name: String,
    val defaultUnit: MeasurementUnit = MeasurementUnit.GRAM,
    val category: IngredientCategory = IngredientCategory.OTHER
)

@Entity(
    tableName = "ingredient_packages",
    foreignKeys = [
        ForeignKey(
            entity = Ingredient::class,
            parentColumns = ["ingredientId"],
            childColumns = ["ingredientId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["ingredientId"])]
)
data class IngredientPackage(
    @PrimaryKey(autoGenerate = true)
    val packageId: Long = 0,
    val ingredientId: Long,
    val packageType: PackageType,
    val packageSize: Double,
    val packageUnit: MeasurementUnit
)

data class IngredientWithPackages(
    @Embedded val ingredient: Ingredient,
    @Relation(
        parentColumn = "ingredientId",
        entityColumn = "ingredientId"
    )
    val packages: List<IngredientPackage>
)

enum class MeasurementUnit {
    GRAM,
    KILOGRAM,
    MILLILITER,
    LITER,
    TEASPOON,
    TABLESPOON,
    CUP,
    PIECE
}

@Entity(
    tableName = "recipe_ingredient_cross_ref",
    primaryKeys = ["recipeId", "ingredientId"],
    foreignKeys = [
        ForeignKey(
            entity = Recipe::class,
            parentColumns = ["recipeId"],
            childColumns = ["recipeId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Ingredient::class,
            parentColumns = ["ingredientId"],
            childColumns = ["ingredientId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["recipeId"]), Index(value = ["ingredientId"])]
)
data class RecipeIngredientCrossRef(
    val recipeId: Long,
    val ingredientId: Long,
    val amount: Double,
    val unit: MeasurementUnit
)

@Entity(tableName = "recipes")
data class Recipe(
    @PrimaryKey(autoGenerate = true)
    val recipeId: Long = 0,
    val name: String,
    val instructions: String,
    val servings: Int = 1
)

// This class is used to fetch a recipe with its ingredients
data class RecipeWithIngredients(
    @Embedded val recipe: Recipe,
    @Relation(
        parentColumn = "recipeId",
        entityColumn = "ingredientId",
        associateBy = Junction(RecipeIngredientCrossRef::class)
    )
    val ingredients: List<Ingredient>
)

data class IngredientAmount(
    val ingredientId: Long,
    val name: String,
    val amount: Double,
    val unit: MeasurementUnit
)

@Entity(tableName = "weekly_plans")
data class WeeklyPlan(
    @PrimaryKey(autoGenerate = true)
    val weeklyPlanId: Long = 0,
    val name: String,
    val startDate: Long, // Timestamp
    val commensals: Int = 2, // Number of people
    val userId: String, // Owner of the plan
    val createdDate: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "weekly_plan_family_cross_ref",
    primaryKeys = ["weeklyPlanId", "familyId"],
    foreignKeys = [
        ForeignKey(
            entity = WeeklyPlan::class,
            parentColumns = ["weeklyPlanId"],
            childColumns = ["weeklyPlanId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Family::class,
            parentColumns = ["familyId"],
            childColumns = ["familyId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["weeklyPlanId"]), Index(value = ["familyId"])]
)
data class WeeklyPlanFamilyCrossRef(
    val weeklyPlanId: Long,
    val familyId: Long
)

data class WeeklyPlanWithFamilies(
    @Embedded val weeklyPlan: WeeklyPlan,
    @Relation(
        parentColumn = "weeklyPlanId",
        entityColumn = "familyId",
        associateBy = Junction(WeeklyPlanFamilyCrossRef::class)
    )
    val families: List<Family>
)

@Entity(
    tableName = "meal_plans",
    foreignKeys = [
        ForeignKey(
            entity = Recipe::class,
            parentColumns = ["recipeId"],
            childColumns = ["recipeId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = WeeklyPlan::class,
            parentColumns = ["weeklyPlanId"],
            childColumns = ["weeklyPlanId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["recipeId"]), Index(value = ["weeklyPlanId"])]
)
data class MealPlan(
    @PrimaryKey(autoGenerate = true)
    val mealPlanId: Long = 0,
    val weeklyPlanId: Long,
    val recipeId: Long,
    val dayOfWeek: DayOfWeek,
    val mealType: MealType,
    val servings: Int = 1,
    val commensals: Int = 2
)

data class MealPlanWithRecipe(
    @Embedded val mealPlan: MealPlan,
    @Relation(
        parentColumn = "recipeId",
        entityColumn = "recipeId"
    )
    val recipe: Recipe
)

data class WeeklyPlanWithMeals(
    @Embedded val weeklyPlan: WeeklyPlan,
    @Relation(
        parentColumn = "weeklyPlanId",
        entityColumn = "weeklyPlanId",
        entity = MealPlan::class
    )
    val mealPlans: List<MealPlanWithRecipe>
)

enum class DayOfWeek {
    MONDAY,
    TUESDAY,
    WEDNESDAY,
    THURSDAY,
    FRIDAY,
    SATURDAY,
    SUNDAY
}

enum class MealType {
    LUNCH,
    DINNER
}

@Entity(tableName = "families")
data class Family(
    @PrimaryKey(autoGenerate = true)
    val familyId: Long = 0,
    val name: String,
    val ownerId: String, // Firebase user ID of the creator
    val createdDate: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "family_members",
    foreignKeys = [
        ForeignKey(
            entity = Family::class,
            parentColumns = ["familyId"],
            childColumns = ["familyId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["familyId"])]
)
data class FamilyMember(
    @PrimaryKey(autoGenerate = true)
    val memberId: Long = 0,
    val familyId: Long,
    val userId: String, // Firebase user ID
    val email: String,
    val displayName: String?,
    val joinedDate: Long = System.currentTimeMillis()
)

data class FamilyWithMembers(
    @Embedded val family: Family,
    @Relation(
        parentColumn = "familyId",
        entityColumn = "familyId"
    )
    val members: List<FamilyMember>
)
