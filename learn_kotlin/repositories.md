# Understanding Repositories in Kotlin Development

## Lesson Overview

In this lesson, we'll explore the Repository pattern in Kotlin development. By the end of this lesson, you'll understand:

- What repositories are and why they're important
- How repositories fit into clean architecture
- How to implement repositories in Kotlin
- Best practices for working with repositories

## What is a Repository?

A repository acts as an abstraction layer between your application's business logic and data sources. It provides a clean API for data access that abstracts the underlying implementation details.

```kotlin
interface UserRepository {
    suspend fun getUser(id: String): User
    suspend fun saveUser(user: User)
    suspend fun deleteUser(id: String)
    suspend fun getAllUsers(): List<User>
}
```

## Why Use Repositories?

1. **Separation of Concerns**: Isolate data access logic from business logic
2. **Testability**: Easily mock repositories for testing
3. **Flexibility**: Switch data sources without changing business logic
4. **Single Source of Truth**: Centralize data access logic

## Repository Pattern in Clean Architecture

```
┌─────────────────┐      ┌─────────────────┐      ┌─────────────────┐
│                 │      │                 │      │                 │
│  Presentation   │─────▶│  Domain Layer   │─────▶│    Data Layer   │
│     Layer       │      │   (Use Cases)   │      │ (Repositories)  │
│                 │      │                 │      │                 │
└─────────────────┘      └─────────────────┘      └─────────────────┘
                                                          │
                                                          ▼
                                                 ┌─────────────────┐
                                                 │  Data Sources   │
                                                 │ (API, Database) │
                                                 │                 │
                                                 └─────────────────┘
```

## Implementing Repositories in Kotlin

### Step 1: Define Repository Interfaces in the Domain Layer

```kotlin
// Domain layer - independent of any framework
interface UserRepository {
    suspend fun getUser(id: String): Result<User>
    suspend fun saveUser(user: User): Result<Unit>
    suspend fun getAllUsers(): Result<List<User>>
}
```

### Step 2: Implement Repository in the Data Layer

```kotlin
// Data layer - contains implementation details
class UserRepositoryImpl(
    private val userApi: UserApi,
    private val userDao: UserDao
) : UserRepository {
    
    override suspend fun getUser(id: String): Result<User> = runCatching {
        val localUser = userDao.getUserById(id)
        if (localUser != null) {
            localUser
        } else {
            val remoteUser = userApi.getUser(id)
            userDao.insertUser(remoteUser)
            remoteUser
        }
    }
    
    override suspend fun saveUser(user: User): Result<Unit> = runCatching {
        userDao.insertUser(user)
        userApi.updateUser(user)
    }
    
    override suspend fun getAllUsers(): Result<List<User>> = runCatching {
        userDao.getAllUsers()
    }
}
```

## Dependency Injection with Repositories

Using Hilt (a Dependency Injection library for Android):

```kotlin
@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {
    
    @Provides
    @Singleton
    fun provideUserRepository(
        userApi: UserApi,
        userDao: UserDao
    ): UserRepository {
        return UserRepositoryImpl(userApi, userDao)
    }
}
```

## Working with Room Database in Repositories

Room is a persistence library that provides an abstraction layer over SQLite:

```kotlin
@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey val id: String,
    val name: String,
    val email: String
)

@Dao
interface UserDao {
    @Query("SELECT * FROM users WHERE id = :id")
    suspend fun getUserById(id: String): UserEntity?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: UserEntity)
    
    @Query("SELECT * FROM users")
    suspend fun getAllUsers(): List<UserEntity>
}
```

## Implementing a Repository with Room and Retrofit

```kotlin
class UserRepositoryImpl(
    private val userApi: UserApi,
    private val userDao: UserDao,
    private val userMapper: UserMapper
) : UserRepository {
    
    override suspend fun getUser(id: String): Result<User> = runCatching {
        try {
            // Try to get from local database first
            val localUser = userDao.getUserById(id)?.let { userMapper.mapToDomain(it) }
            
            if (localUser != null) {
                return@runCatching localUser
            }
            
            // If not in database, fetch from API
            val remoteUser = userApi.getUser(id)
            val userEntity = userMapper.mapToEntity(remoteUser)
            userDao.insertUser(userEntity)
            userMapper.mapToDomain(userEntity)
        } catch (e: Exception) {
            throw e
        }
    }
    
    // Other implementations...
}
```

## Testing Repositories

Using MockK for testing:

```kotlin
class UserRepositoryTest {
    
    @MockK
    private lateinit var userApi: UserApi
    
    @MockK
    private lateinit var userDao: UserDao
    
    @MockK
    private lateinit var userMapper: UserMapper
    
    private lateinit var userRepository: UserRepository
    
    @Before
    fun setup() {
        MockKAnnotations.init(this)
        userRepository = UserRepositoryImpl(userApi, userDao, userMapper)
    }
    
    @Test
    fun `getUser should return user from database when available`() = runTest {
        // Given
        val userId = "123"
        val userEntity = UserEntity(id = userId, name = "John", email = "john@example.com")
        val user = User(id = userId, name = "John", email = "john@example.com")
        
        every { userDao.getUserById(userId) } returns userEntity
        every { userMapper.mapToDomain(userEntity) } returns user
        
        // When
        val result = userRepository.getUser(userId)
        
        // Then
        assert(result.isSuccess)
        assert(result.getOrNull() == user)
        verify { userDao.getUserById(userId) }
        verify(exactly = 0) { userApi.getUser(any()) }
    }
}
```

## Common Repository Patterns

### 1. Single Source of Truth

```kotlin
class ProductRepositoryImpl(
    private val remoteDataSource: ProductRemoteDataSource,
    private val localDataSource: ProductLocalDataSource
) : ProductRepository {
    
    override suspend fun getProducts(): Flow<List<Product>> {
        // Always display data from local database
        return localDataSource.getProducts()
            .onStart {
                // Refresh data from remote on start
                try {
                    val remoteProducts = remoteDataSource.fetchProducts()
                    localDataSource.saveProducts(remoteProducts)
                } catch (e: Exception) {
                    // Network error, will still emit local data
                    Log.e("Repository", "Error fetching remote data", e)
                }
            }
    }
}
```

### 2. Offline-First Architecture

```kotlin
class NoteRepositoryImpl(
    private val noteApi: NoteApi,
    private val noteDao: NoteDao,
    private val syncManager: SyncManager
) : NoteRepository {
    
    override suspend fun saveNote(note: Note) {
        // Save locally first
        noteDao.insertNote(note.toEntity())
        
        // Mark for sync
        syncManager.markForSync(note.id)
        
        // Try to sync immediately if possible
        syncManager.trySyncNow()
    }
    
    // Other implementations...
}
```

## Best Practices for Repositories

1. **Use interfaces**: Define repositories as interfaces in your domain layer
2. **Return Result or Flow**: Use Kotlin's Result type or Flow for asynchronous data
3. **Handle errors gracefully**: Implement proper error handling
4. **Keep repositories focused**: Each repository should handle a single domain entity
5. **Consider caching strategies**: Implement appropriate caching for your use case
6. **Use coroutines for async operations**: Leverage Kotlin's coroutines for asynchronous tasks
7. **Test thoroughly**: Write comprehensive tests for your repositories

## Practical Exercise

Implement a simple `TaskRepository` with the following requirements:

1. Create a `Task` data class in the domain layer
2. Define a `TaskRepository` interface with CRUD operations
3. Implement the repository with Room for local storage
4. Add network operations with Retrofit
5. Implement offline-first functionality

## Conclusion

Repositories are a powerful pattern that helps maintain a clean, testable, and flexible architecture in your Kotlin applications. By separating data access from business logic, you create a more maintainable codebase that can easily adapt to changing requirements.

## Additional Resources

- [Android Architecture Components](https://developer.android.com/topic/libraries/architecture)
- [Room Persistence Library](https://developer.android.com/training/data-storage/room)
- [Kotlin Coroutines](https://kotlinlang.org/docs/coroutines-overview.html)
- [Retrofit for API calls](https://square.github.io/retrofit/)
- [Clean Architecture by Robert C. Martin](https://blog.cleancoder.com/uncle-bob/2012/08/13/the-clean-architecture.html)

## Assignment

Create a complete Weather app that uses repositories to fetch and store weather data:

1. Define a WeatherRepository interface
2. Implement it with a remote data source (weather API) and local storage
3. Handle error cases and network connectivity issues
4. Implement a cache invalidation strategy
5. Write unit tests for your repository implementation

Happy coding!
