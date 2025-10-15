# Questions for NeuroID Unit Test Coverage Implementation

## 1. Project Structure & Scope Questions

**Q1.1:** I notice the NeuroID module has two build flavors (`androidLib` and `reactNativeLib`). Should the unit tests cover both flavors equally, or should I focus primarily on one?

*** 1.Android lib for now. 

**Q1.2:** The existing test structure shows tests for utilities, callbacks, and services, but I notice there are also `events/`, `models/`, `storage/`, `extensions/`, and `compose/` packages. Should I create tests for **all** packages in `com.neuroid.tracker`, or are there specific packages to prioritize/exclude?

*** 1. Yes all would be good. Please start utilties, then proceed to events, models, storage, callbacks, services, NeuroID, extensions and compose in that order. This should allow us to start at the bottom level classes with the least dependencies to the classes with the most dependencies. 

**Q1.3:** Should I create tests for the `NeuroIDPublic.kt` interface, or focus primarily on the main `NeuroID.kt` class implementation?

*** 1. NeuroID as the NeuroIDPublic is simply and interface. 

## 2. Testing Strategy & Dependencies

**Q2.1:** You mentioned avoiding Robolectric and wrapping System static calls first. I can see direct usage of:
- `System.currentTimeMillis()` (in NIDTime.kt)
- `System.getenv()` (in RootHelper.kt)
- `Runtime.getRuntime().exec()` (in RootHelper.kt)
- `UUID.randomUUID()` (in UtilExt.kt, NIDSingletonIDs.kt)
- `Calendar.getInstance()` (various places)

Should I create wrapper interfaces for **all** of these system dependencies, or start with a specific subset?

### 1. Yes that would be good. Create wrappers for these System calls. Create or modify the Constructor in classes that make these System calls and pass in the System implementation as a default so we can pass in a mock for the tests. 

**Q2.2:** The codebase uses many Android framework classes (Context, ConnectivityManager, LocationManager, etc.). How should I handle mocking these without Robolectric? Should I:
- Create wrapper interfaces for Android system services?
- Use MockK with relaxed mocking?
- Focus only on business logic that doesn't directly use Android APIs?

### 1. Please create wrapper interfaces for the Android System service. Create or modify the Constructor in classes that make these System calls and pass in the System implementation as a default so we can pass in a mock for the tests. 

**Q2.3:** Many classes have singleton patterns and static methods. Should I refactor these to be more testable (dependency injection), or work around them in tests?

### 1. Yes please create or modify the Constructor in classes that make these Static or Singleton calls and pass in the proper class implementation as a default so we can pass in a mock for the tests. 

## 3. Coverage Goals & Prioritization

**Q3.1:** What's the target coverage percentage I should aim for? 
- Overall package coverage target?
- Should certain classes have higher coverage requirements than others?

### 1. At a minimum we are looking for 80% converage on all classes. Idelaly we would like to have 100% coverage however this might not be possible. We already have 60% overal coverage according to the Jacoco coverage docs. 

### 2. Yes, the NeuroID, service and callback classes should have nearly all branches covered with tests. The rest should have at least 80% coverage if possible. 

**Q3.2:** Since I need to break this into small PRs per class, which **specific order** would you prefer for implementation? You mentioned:
1. Utilities first
2. Then callbacks 
3. Then services
4. Finally NeuroID class

But within each category, should I prioritize by:
- Current coverage gaps (lowest coverage first)?
- Complexity (simplest first)?
- Dependencies (classes with fewer dependencies first)?

### 1. Prioritize building out the wrappers first. Ensure the current tests work with these wrappers. Then work on the utilties classes, then work on the others. 

**Q3.3:** The jacoco.xml file appears empty - should I generate a fresh coverage report first, or do you have specific classes you know need the most attention?

### 1. The jacoco.xml file has been filled in. Please take a look. Sorry about that. 

## 4. Technical Implementation Questions

**Q4.1:** I see some tests are using `UnconfinedTestDispatcher` for coroutines. Should I standardize on this approach for all coroutine testing, or are there specific patterns you prefer?

### 1. Yes, this is critial. We need the tests to be operate consistently. Ensure that any Coroutines that you launch have dispatchers that can be replaced at runtime (passed in through constructor so we can replace, generally IO Dispatchers, with unconfined dispatchers during test time)

**Q4.2:** The existing tests use MockK. Should I continue with this library, or do you have a preference for other mocking frameworks?

### 1. unless there are too many difficutlties and you need to change this, please continue with mockk. 

**Q4.3:** For classes that heavily use Android lifecycle (Activities, Fragments), what's the preferred approach for testing without Robolectric?

### 1. Mock out the Activities and Fragments if possible by passing them through the class constructor or method parameters. Most of the methods have Activty/Fragment parameters that are passed in so these can be mocked during test time. 

**Q4.4:** Some utility classes like `NIDMetaData` likely collect device information. How should I test these without access to real device APIs?

### 1. Again any dependencies that these require should be passed (constructor or method parameters) so these can be mocked during test time. 

## 5. Code Quality & Maintenance

**Q5.1:** I notice some deprecation warnings in the build output (NetworkInfo, PhoneStateListener, etc.). Should I:
- Test the current deprecated implementations as-is?
- Update to newer APIs first, then test?
- Create tests that work with both old and new APIs?

### 1. Dont worry about warnings. We can address these later. 

**Q5.2:** Should the tests follow any specific naming conventions beyond the existing pattern I see in the codebase?

### 1. follow the code base and append Test to the name where appropriate. 

**Q5.3:** Are there any specific edge cases or error scenarios you want to ensure are well-tested?

### 1. Just ensure that all methods that have code that are not getter or setters (basically business logic) are tested. Use mocked dependencies with known values and ensure the numbers are apppropriate. Follow the existing unit tests and their sturucture where appriopriate. 

## 6. Phase 0 - System Dependencies Wrapper Question

**Q6.1:** Before starting the actual unit tests, should I implement wrapper classes for system dependencies as a "Phase 0"? This would involve:
- Creating wrapper interfaces for all System.* calls
- Creating wrapper interfaces for Android system services  
- Creating wrapper interfaces for time/calendar operations
- Creating wrapper interfaces for random value generation
- Updating existing classes to use these wrappers

This would make subsequent unit testing much easier and enable testing without Robolectric. Would you like me to tackle this foundation work first?

### 1. Yes please od this however ensure that the current tests do not break in this process. 

## 7. Test Data & Scenarios

**Q7.1:** Are there specific business scenarios or user flows that should be heavily tested?

### 1. No, just ensure that all branches are covered in code that are not getters or setters. 

**Q7.2:** Should I create test data builders/factories for complex model objects, or keep tests simple with inline object creation?

### 1. You do not need to create builders or factories. Just create mocked dependencies and pass them as paramters in methods or into classes through constructors and assert on known values.   

**Q7.3:** For classes that interact with network/storage, should I focus on testing the logic around success/failure scenarios rather than actual network/disk operations?

### 1. You do not need to worry about network/storage. For Network/Storage, ensure that the Storage or Netowrk class that is prioviding service is passed in as a dependency and mock it during runtime. We can work on these together when we get to these classes in the next phases of this development.   

## 8. Network & Storage Service Wrapper Specific Questions

**Q8.1:** Looking at the coverage analysis, I see several network-related classes with low coverage (`NIDHttpService`, `NIDAdvancedDeviceNetworkService`, `LoggerIntercepter`). For HTTP/network operations, should I:
- Create wrapper interfaces for OkHttp client dependencies?
- Wrap ConnectivityManager and NetworkInfo access?
- Focus on wrapping just the high-level network state detection logic?
### 1. These are already injected in constructors or method params so all we need to do is to mock these services out. 

**Q8.2:** For storage services, I notice classes like `NIDSharedPrefsDefaults` and potential database operations. Should I:
- Create wrapper interfaces for SharedPreferences access?
- Wrap file system operations (if any)?
- Create abstractions for any database operations?
### 1. These are already injected in constructors or method params so all we need to do is to mock these services out. 

**Q8.3:** The `NIDJobServiceManager` appears to handle background network operations with significant missed coverage (613 missed instructions). For background/async network operations, should I:
- Mock the underlying HTTP client completely?
- Create wrappers for the job scheduling mechanisms?
- Focus on testing the business logic that processes network responses?
### 1. These are already injected in constructors or method params so all we need to do is to mock these services out. 

**Q8.4:** I see `RetrySender` and error handling logic. For network retry/error scenarios, should I:
- Create deterministic failure simulation through wrapper interfaces?
- Mock specific HTTP status codes and network exceptions?
- Test both success and failure paths with controlled responses?
### 1. These are already injected in constructors or method params so all we need to do is to mock these services out. 

**Q8.5:** For classes that combine network + storage (like caching network responses), should I:
- Create separate wrappers for network and storage, then test their interaction?
- Create a unified service wrapper that handles both concerns?
- Test these integration scenarios with both dependencies mocked?
### 1. These are already injected in constructors or method params so all we need to do is to mock these services out. 

**Q8.6:** Looking at `LocationService` (91 missed instructions), this appears to combine location hardware access with potential network location services. Should I:
- Wrap LocationManager system service access?
- Mock GPS vs network location provider differences? 
- Create test scenarios for location permission states?
### 1. These are already injected in constructors or method params so all we need to do is to mock these services out. 

---

**Q9.1:** I would like to know how you will use the NIDTimeProvider. In NIDEventSender at line 56, there is a usage of the System.currentTimeMillis(). How will this be changed. can you give an example of how this will be changed and how will the NIDTimeProvider be referenced?

### Answer: Here's exactly how the NIDTimeProvider wrapper will be implemented:

#### Step 1: Create the wrapper interface and implementations:
```kotlin
// Create interface
interface NIDTimeProvider {
    fun currentTimeMillis(): Long
    fun calendarInstance(): Calendar
}

// Production implementation
class NIDSystemTimeProvider : NIDTimeProvider {
    override fun currentTimeMillis(): Long = System.currentTimeMillis()
    override fun calendarInstance(): Calendar = Calendar.getInstance()
}

// Test implementation  
class NIDTestTimeProvider(
    private val fixedTime: Long = 1234567890L
) : NIDTimeProvider {
    override fun currentTimeMillis(): Long = fixedTime
    override fun calendarInstance(): Calendar = Calendar.getInstance().apply {
        timeInMillis = fixedTime
    }
}
```

#### Step 2: Modify NIDEventSender constructor to accept the wrapper:
```kotlin
class NIDEventSender(
    private val httpService: HttpService,
    private val context: Context,
    private val timeProvider: NIDTimeProvider = NIDSystemTimeProvider() // Default to production
) {
    // ... existing code
    
    private fun initializeStaticPayload() {
        // BEFORE: System.currentTimeMillis()
        // AFTER: 
        val currentTime = timeProvider.currentTimeMillis()
        // ... use currentTime instead of System.currentTimeMillis()
    }
}
```

#### Step 3: In unit tests, inject the test implementation:
```kotlin
@Test
fun `test initializeStaticPayload with known time`() {
    // Arrange
    val fixedTime = 1234567890L
    val mockHttpService = mockk<HttpService>()
    val mockContext = mockk<Context>()
    val testTimeProvider = NIDTestTimeProvider(fixedTime)
    
    // Act
    val eventSender = NIDEventSender(
        httpService = mockHttpService,
        context = mockContext, 
        timeProvider = testTimeProvider
    )
    
    // Assert - now we can verify the exact time was used
    // ... test assertions with known time value
}
```

#### Step 4: For existing callers, no changes needed:
```kotlin
// Existing code continues to work unchanged
val eventSender = NIDEventSender(httpService, context) 
// Uses NIDSystemTimeProvider() by default - no breaking changes
```

This approach:
- ✅ Maintains backward compatibility (existing code works unchanged)
- ✅ Enables deterministic testing with known time values
- ✅ Follows dependency injection pattern via constructor
- ✅ No changes needed to existing calling code

---
**Q10:** Acutally there is already a utility class called NIDTime in the com.neuroid.tracker.utils.NIDTime that already has a wrapper for System.currentTimeMillis(). Can you update your implementation.md to note that we already have a System.currentTimeMillis() wrapper class and use this wrapper class throughout the NeuroID module? Also please check for any other wrapper classes that are already available. 

### Answer: Excellent discovery! I found several existing wrapper classes that we can leverage:

#### Existing Wrapper Classes Found:
1. **`NIDTime`** - Already wraps `System.currentTimeMillis()`
   ```kotlin
   class NIDTime {
       fun getCurrentTimeMillis(): Long = System.currentTimeMillis()
   }
   ```

2. **`RandomGenerator`** - Already wraps `Math.random()`
   ```kotlin
   class RandomGenerator {
       fun getRandom(multiplier: Int): Double = Math.random() * multiplier
   }
   ```

3. **`NIDBuildConfigWrapper`** - Already wraps `BuildConfig` access
   ```kotlin
   class NIDBuildConfigWrapper {
       fun getBuildVersion(): String = BuildConfig.VERSION_NAME
       fun getGitHash(): String = BuildConfig.GIT_HASH  
       fun getFlavor(): String = BuildConfig.FLAVOR
   }
   ```

4. **`Base64Decoder`** - Already wraps Android Base64 operations
   ```kotlin
   class Base64Decoder {
       fun decodeBase64(data: String): String = String(Base64.decode(data, Base64.DEFAULT))
   }
   ```

#### Phase 0 Strategy Update:
✅ **Use existing wrappers** - `NIDTime`, `RandomGenerator`, `NIDBuildConfigWrapper`, `Base64Decoder`  
⚠️  **Still need to create**:
- `NIDUuidProvider` for `UUID.randomUUID()` calls
- `NIDEnvironmentProvider` for `System.getenv()` calls  
- `NIDRuntimeProvider` for `Runtime.getRuntime().exec()` calls
- `NIDCalendarProvider` for `Calendar.getInstance()` calls

#### Updated Implementation Approach:
1. **Leverage existing wrappers** throughout the codebase via constructor injection
2. **Create missing wrappers** for UUID, environment, runtime, and calendar operations
3. **Update classes** to use these wrappers instead of direct system calls
4. **Ensure backward compatibility** with default implementations

This significantly reduces Phase 0 work since 4 major wrapper classes already exist and are partially integrated!

---

Please answer these questions in this file, and I'll create a comprehensive implementation plan in `docs/implementation.md` once I have your guidance.