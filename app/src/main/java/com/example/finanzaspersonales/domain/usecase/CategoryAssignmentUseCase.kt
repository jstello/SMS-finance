package com.example.finanzaspersonales.domain.usecase

import com.example.finanzaspersonales.data.model.Category
import com.example.finanzaspersonales.data.model.TransactionData
import com.example.finanzaspersonales.data.repository.CategoryRepository

/**
 * Use case for assigning categories to transactions based on patterns in transaction data
 */
class CategoryAssignmentUseCase(
    private val categoryRepository: CategoryRepository
) {
    // Cached list of categories
    private var categories: List<Category> = emptyList()
    
    /**
     * Assign a category to a transaction based on its content
     */
    suspend fun assignCategoryToTransaction(transaction: TransactionData): Category? {
        // Load categories if needed
        if (categories.isEmpty()) {
            categories = categoryRepository.getCategories()
        }
        
        // Extract keywords from the transaction
        val keywords = extractKeywords(transaction)
        
        // Find matching category based on keywords
        return findMatchingCategory(keywords, transaction.isIncome)
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
        
        // Extract keywords from the SMS body
        val body = transaction.originalMessage.body.lowercase()
        
        // Add common category-specific keywords
        addKeywordsFromText(body, keywords)
        
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
    }
} 