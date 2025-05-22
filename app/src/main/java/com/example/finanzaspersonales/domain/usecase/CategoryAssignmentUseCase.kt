package com.example.finanzaspersonales.domain.usecase

import com.example.finanzaspersonales.data.model.Category
import com.example.finanzaspersonales.data.model.TransactionData
import com.example.finanzaspersonales.data.repository.CategoryRepository
import android.util.Log
// import com.example.finanzaspersonales.data.auth.AuthRepository // Temporarily removed
// import kotlinx.coroutines.flow.firstOrNull // Temporarily removed
import javax.inject.Inject

/**
 * Use case for assigning categories to transactions based on patterns in transaction data
 */
class CategoryAssignmentUseCase @Inject constructor(
    private val categoryRepository: CategoryRepository
    // private val authRepository: AuthRepository // Temporarily removed
) {
    // Cached list of categories
    private var categories: List<Category> = emptyList()
    
    /**
     * Assign a category to a transaction based on its content
     */
    suspend fun assignCategoryToTransaction(transaction: TransactionData): Category? {
        // TODO: Restore AuthRepository and userId usage for provider-specific category mapping
        val userId: String? = null // Temporarily null
        // val userId = authRepository.currentUserState.firstOrNull()?.uid // Temporarily removed

        // 1. Check for provider-specific category mapping if provider and user ID exist
        if (userId != null && transaction.provider != null && transaction.provider!!.isNotBlank()) { // Ensure provider is not blank
            Log.d("CategoryAssign", "Attempting to find mapping for provider: '${transaction.provider}' for user: $userId")
            val providerCategoryMappingResult = categoryRepository.getCategoryForProvider(userId, transaction.provider!!)
            providerCategoryMappingResult.fold(
                onSuccess = { categoryId ->
                    if (categoryId != null) {
                        if (categories.isEmpty()) { // Ensure categories are loaded
                            Log.d("CategoryAssign", "Categories list empty, fetching from repository.")
                            categories = categoryRepository.getCategories()
                            Log.d("CategoryAssign", "Loaded ${categories.size} categories.")
                        }
                        val mappedCategory = categories.find { it.id == categoryId }
                        if (mappedCategory != null) {
                            Log.i("CategoryAssign", "SUCCESS: Assigned category '${mappedCategory.name}' (ID: $categoryId) based on provider '${transaction.provider}' mapping.")
                            return mappedCategory
                        } else {
                            Log.w("CategoryAssign", "Provider mapping found for '${transaction.provider}' to categoryId '$categoryId', but category not in local list. Categories count: ${categories.size}")
                            // Optional: Log available categories for debugging
                            // categories.forEach { cat -> Log.d("CategoryAssign", "Available cat: ${cat.name} (ID: ${cat.id})") }
                        }
                    } else {
                        Log.d("CategoryAssign", "No category mapping found for provider: '${transaction.provider}'. Proceeding to keyword matching.")
                    }
                },
                onFailure = { exception ->
                    Log.e("CategoryAssign", "Error fetching provider category mapping for '${transaction.provider}'. Proceeding to keyword matching.", exception)
                    // Proceed to keyword-based assignment
                }
            )
        } else {
            // if (userId == null) Log.d("CategoryAssign", "User not logged in, skipping provider mapping check.") // Temporarily adjusted
            if (userId == null) Log.d("CategoryAssign", "User ID not available (AuthRepository removed), skipping provider mapping check.")
            if (transaction.provider == null || transaction.provider!!.isBlank()) Log.d("CategoryAssign", "Transaction provider is null or blank, skipping provider mapping check.")
        }

        // Load categories if needed (might have been loaded above, but check again for safety if not found via provider mapping)
        if (categories.isEmpty()) {
            Log.d("CategoryAssign", "Categories list still empty before keyword assignment, fetching.")
            categories = categoryRepository.getCategories()
            Log.d("CategoryAssign", "Loaded ${categories.size} categories for keyword assignment.")
        }

        // 2. Fallback to keyword-based assignment (existing logic)
        Log.d("CategoryAssign", "Attempting keyword-based assignment for provider: '${transaction.provider}'")
        val keywords = extractKeywords(transaction)
        val keywordBasedCategory = findMatchingCategory(keywords, transaction.isIncome)
        if (keywordBasedCategory != null) {
            Log.i("CategoryAssign", "SUCCESS: Assigned category '${keywordBasedCategory.name}' based on keywords for provider '${transaction.provider}'.")
        } else {
            Log.w("CategoryAssign", "No category could be assigned via keywords for provider '${transaction.provider}'.")
        }
        return keywordBasedCategory
    }
    
    /**
     * Extract keywords from a transaction
     */
    private fun extractKeywords(transaction: TransactionData): Set<String> {
        val keywords = mutableSetOf<String>()
        
        // Add provider name to keywords if available
        transaction.provider?.let { provider ->
            keywords.add(provider.lowercase())
            
            // Add individual words from provider name
            provider.split(" ").forEach { word ->
                if (word.length > 3) {
                    keywords.add(word.lowercase())
                }
            }
        }
        
        // Add contact name to keywords if available
        transaction.contactName?.let { contact ->
            keywords.add(contact.lowercase())
        }
        
        // Extract keywords from the transaction description
        val descriptionText = transaction.description?.lowercase()
        
        if (descriptionText != null) {
            // Add common category-specific keywords
            addKeywordsFromText(descriptionText, keywords)
        } else {
            Log.w("CategoryAssign", "Transaction description is null, cannot extract keywords from text for tx date ${transaction.date}")
        }
        
        return keywords
    }
    
    /**
     * Add keywords from text based on common category-related terms
     */
    private fun addKeywordsFromText(text: String, keywords: MutableSet<String>) {
        // Food & Restaurants
        if (text.contains(Regex("restaurante|comida|menu|almuerzo|cena|desayuno|café|pizza|hamburguesa|comidas|"
                + "rappi|ifood|domicilio|domicilios|uber eats|delivery"))) {
            keywords.add("food")
            keywords.add("restaurant")
        }
        
        // Transportation
        if (text.contains(Regex("taxi|uber|didi|cabify|transporte|gasolina|combustible|"
                + "parqueadero|estacionamiento|peaje|metro|transmilenio|bus|"
                + "boleto|pasaje|vuelo|avión"))) {
            keywords.add("transportation")
            keywords.add("travel")
        }
        
        // Shopping
        if (text.contains(Regex("tienda|compra|mercado|super|supermercado|mall|centro comercial|"
                + "ropa|calzado|zapatos|vestido|pantalon|falda|blusa|camisa|amazon|"
                + "éxito|carulla|jumbo|alkosto|falabella|zara"))) {
            keywords.add("shopping")
        }
        
        // Entertainment
        if (text.contains(Regex("película|cine|teatro|concierto|festival|entretenimiento|"
                + "netflix|disney|spotify|apple music|evento|boleta|juego|videojuego"))) {
            keywords.add("entertainment")
        }
        
        // Health
        if (text.contains(Regex("médico|doctor|clínica|hospital|eps|medicina|farmacia|"
                + "droguería|medicamento|salud|consulta|examen|laboratorio|terapia"))) {
            keywords.add("health")
        }
        
        // Home
        if (text.contains(Regex("arriendo|alquiler|hipoteca|casa|apartamento|servicios|agua|luz|"
                + "energía|electricidad|gas|internet|telefonía|mantenimiento|reparación"))) {
            keywords.add("home")
            keywords.add("housing")
        }
        
        // Education
        if (text.contains(Regex("educación|universidad|colegio|escuela|curso|matrícula|"
                + "clase|capacitación|libro|librería|academia|taller|seminario"))) {
            keywords.add("education")
        }
        
        // Personal care
        if (text.contains(Regex("salón|belleza|barbería|corte|peinado|spa|gimnasio|gym|"
                + "entrenamiento|estética|maquillaje|manicure|pedicure"))) {
            keywords.add("personal")
            keywords.add("beauty")
        }
        
        // Income
        if (text.contains(Regex("nómina|sueldo|salario|pago|honorario|ingreso|abono|"
                + "depósito|transferencia recibida|consignación|recaudo"))) {
            keywords.add("income")
            keywords.add("salary")
        }

        // Travel
        if (text.contains(Regex("hotel|hospedaje|alojamiento|vuelo|aerolínea|avión|viaje|pasaje|"
                + "aeropuerto|turismo|agencia de viajes|reserva|booking|airbnb|expedia|"
                + "vacaciones|excursión|tour|crucero"))) {
            keywords.add("travel")
        }

        // Subscriptions
        if (text.contains(Regex("suscripción|membresía|mensual|anual|netflix|spotify|disney|"
                + "amazon prime|hbo|apple tv|youtube premium|microsoft|office|adobe|"
                + "gym|gimnasio|revista|periódico|newspaper|patreon|twitch"))) {
            keywords.add("subscription")
        }
    }
    
    /**
     * Find a matching category based on keywords and transaction type
     */
    private fun findMatchingCategory(keywords: Set<String>, isIncome: Boolean): Category? {
        // Map of category names to patterns
        val categoryPatterns = mapOf(
            "Groceries" to setOf("supermercado", "mercado", "éxito", "carulla", "jumbo", "d1", "ara", 
                "justo y bueno", "food", "grocery", "groceries", "super"),
                
            "Restaurants" to setOf("restaurant", "restaurante", "comida", "almuerzo", "cena", "comidas",
                "rappi", "ifood", "domicilio", "domicilios", "uber eats", "delivery", "food"),
                
            "Transportation" to setOf("transportation", "transporte", "taxi", "uber", "didi", "cabify",
                "gasolina", "combustible", "parqueadero", "peaje", "metro", "transmilenio", "bus"),
                
            "Shopping" to setOf("shopping", "tienda", "compra", "ropa", "calzado", "zapatos", "mall",
                "centro comercial", "amazon", "falabella", "zara"),
                
            "Bills" to setOf("bill", "servicio", "agua", "luz", "energía", "gas", "internet", "telefonía",
                "celular", "factura", "recibo", "pago"),
                
            "Entertainment" to setOf("entertainment", "entretenimiento", "película", "cine", "teatro",
                "concierto", "festival", "netflix", "disney", "spotify", "evento"),
                
            "Health" to setOf("health", "médico", "doctor", "clínica", "hospital", "medicina", "farmacia",
                "droguería", "medicamento", "salud", "consulta"),
                
            "Housing" to setOf("home", "housing", "arriendo", "alquiler", "hipoteca", "casa", "apartamento",
                "mantenimiento", "reparación"),
                
            "Income" to setOf("income", "nómina", "sueldo", "salario", "pago", "honorario", "ingreso",
                "abono", "depósito", "transferencia recibida", "consignación"),
                
            "Education" to setOf("education", "educación", "universidad", "colegio", "escuela", "curso",
                "matrícula", "clase", "capacitación", "libro"),
                
            "Personal Care" to setOf("personal", "beauty", "salón", "belleza", "barbería", "corte",
                "spa", "gimnasio", "gym", "entrenamiento", "estética"),

            "Travel" to setOf("travel", "viaje", "hotel", "vuelo", "aerolínea", "avión", "pasaje", 
                "aeropuerto", "turismo", "hospedaje", "alojamiento", "booking", "airbnb", "vacaciones"),
                
            "Subscriptions" to setOf("subscription", "suscripción", "membresía", "mensual", "anual",
                "netflix", "spotify", "disney", "amazon prime", "hbo", "office", "gym"),
                
            "Other" to setOf("other", "otro")
        )
        
        // If it's income, prioritize the income category
        if (isIncome) {
            val incomeCategory = categories.find { it.name == "Income" }
            if (incomeCategory != null) {
                return incomeCategory
            }
        }
        
        // Find the best matching category
        var bestMatch: Category? = null
        var maxMatches = 0
        
        for (category in categories) {
            val patterns = categoryPatterns[category.name]
            
            if (patterns != null) {
                // Count matching keywords
                val matches = keywords.count { keyword ->
                    patterns.any { pattern -> keyword.contains(pattern) || pattern.contains(keyword) }
                }
                
                if (matches > maxMatches) {
                    maxMatches = matches
                    bestMatch = category
                }
            }
        }
        
        // If no matches found, return the "Other" category
        if (bestMatch == null || maxMatches == 0) {
            return categories.find { it.name == "Other" }
        }
        
        return bestMatch
    }} 
