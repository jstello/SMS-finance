# Finanzas Personales

A Colombian SMS-based finance tracker for Bancolombia & Nequi transactions

## ✅ Implemented Features

- **SMS Processing**
  - Reads Bancolombia/Nequi messages
  - Extracts COP amounts with regex
  - Parses dates from message text
  - Detects income via "recibiste" keyword

- **Transaction UI**
  - Material3-styled lists
  - Sort by date/amount (asc/desc)
  - Filter by year/month
  - Basic total calculations

- **Technical Base**
  - Jetpack Compose (Material3 1.2.0)
  - Kotlin 2.0 + JDK 17
  - Gradle Version Catalog
  - SMS permission runtime request

## 📥 Installation

```bash
git clone https://github.com/yourusername/finanzas-personales.git
```
**Requirements:**
- Android Studio 2022.1+
- API 24+ device/emulator

## ⚙️ Code Verified

- **Dependencies**
  ```kotlin
  implementation(libs.androidx.material3) // 1.2.0-alpha11
  implementation(libs.androidx.material.icons.extended)
  ```

- **Architecture**
  - Single Activity
  - Composable-based UI
  - State hoisting
  - Remember/MutableState

## 🚧 Current Limitations

- **Currency**
  - COP only
  - No USD support

- **Analysis**
  - No charts/graphs
  - Basic totals only

- **Localization**
  - Spanish SMS only
  - App UI in Spanish

## 🌟 Future Enhancements

- **Visual Analytics & Insights**
  - Integrate charts and graphs for spending habits and trends.
  - Implement budgeting tools to track spending against set budgets.

- **Notifications & Reminders**
  - Notify users of large transactions or when spending exceeds thresholds.
  - Send monthly summaries highlighting key financial insights.

- **Cloud Sync & Backup**
  - Store transactions securely in the cloud for cross-device synchronization.
  - Allow users to backup and restore their transaction history easily.

- **User Authentication & Security**
  - Implement secure authentication methods (email/password, OAuth).
  - Ensure sensitive financial data is encrypted.

- **Receipt Management**
  - Allow users to attach photos of receipts to transactions.
  - Use OCR to automatically extract transaction details from receipt images.

- **Multi-Currency Support**
  - Automatically convert transactions to different currencies based on real-time exchange rates.

- **Transaction Categorization**
  - Use machine learning to automatically categorize transactions.
  - Allow users to create and manage their own categories.

- **Scheduled Transactions**
  - Track and remind users of upcoming recurring payments.

- **Advanced Search & Filtering**
  - Enhance search capabilities to filter transactions by various criteria.

- **Export & Reporting**
  - Allow users to export transaction data for external analysis.
  - Generate tax reports formatted for easy submission.

- **Widgets & Quick Actions**
  - Provide home screen widgets for quick access to recent transactions.

- **Integration with Financial APIs**
  - Directly integrate with banking APIs for real-time data.

- **Collaboration Features**
  - Allow multiple users to manage shared finances collaboratively.

## 🌱 User Acquisition & Growth Strategy

To transform "Finanzas Personales" into a sustainable source of income, we have outlined a comprehensive user acquisition and growth strategy:

### 1. Product Refinement & Launch
- Enhance the app by addressing current limitations and integrating future features.
- Conduct a closed beta to gather user feedback and testimonials.

### 2. Brand Positioning & Messaging
- Develop a clear value proposition emphasizing the app's unique features for Colombian users.
- Create a professional website and optimize app store listings.

### 3. Marketing & User Acquisition Strategy
- **Content Marketing**: Start a blog or YouTube channel focused on personal finance.
- **Social Media**: Build a presence on platforms like Facebook and Instagram.
- **App Store Optimization**: Use targeted keywords and encourage user reviews.
- **Paid Advertising**: Test localized ads and partner with influencers.

### 4. Monetization Strategy
- Implement a freemium model with premium features available via subscription.
- Offer value-added services such as data export and API access.

### 5. Retention & Growth Tactics
- Create a seamless onboarding process and provide excellent customer support.
- Regularly update the app with new features based on user feedback.
- Launch a referral program to incentivize user growth.

### 6. Scaling & Long-Term Vision
- Explore geographical expansion into other Latin American markets.
- Build partnerships with local banks and fintech companies.
- Focus on creating a recurring revenue model that supports ongoing development.

### Long-Term Goals
- Achieve steady monthly active user growth (targeting 10,000 MAUs within 12–18 months).
- Convert 5–10% of free users to premium subscribers.
- Expand the app's reach and integrate with financial institutions.
- Build a sustainable recurring revenue model to cover operational costs and fund future enhancements.

By following this strategy, we aim to create a robust user base and establish "Finanzas Personales" as a leading personal finance management tool in Colombia and beyond.

## 📜 Version History

1.1.0 - Material3 UI, Filter Chips, Sorting  
1.0.1 - Stability fixes, Dependency cleanup  
1.0.0 - Core SMS parsing & list UI 