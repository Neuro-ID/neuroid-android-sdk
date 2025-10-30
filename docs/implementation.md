# NeuroID Android SDK Unit Test Coverage Implementation Plan

## Current Coverage Baseline
- **Overall Coverage**: 61.3% (17,612 covered / 11,110 missed instructions)
- **Target Coverage**: 80% minimum
- **Coverage Gap**: 18.7% improvement needed
- **Total Instructions**: 28,722

## Phase 0: System Dependency Wrapper Implementation

### Existing Wrapper Classes (Already Available!)
**Good news!** Several wrapper classes already exist and can be leveraged:

1. **`NIDTime`** - Time operations wrapper
   ```kotlin
   class NIDTime {
       fun getCurrentTimeMillis(): Long = System.currentTimeMillis()
   }
   ```

2. **`RandomGenerator`** - Random number generation wrapper  
   ```kotlin
   class RandomGenerator {
       fun getRandom(multiplier: Int): Double = Math.random() * multiplier
   }
   ```

3. **`NIDBuildConfigWrapper`** - BuildConfig access wrapper
   ```kotlin
   class NIDBuildConfigWrapper {
       fun getBuildVersion(): String = BuildConfig.VERSION_NAME
       fun getGitHash(): String = BuildConfig.GIT_HASH
       fun getFlavor(): String = BuildConfig.FLAVOR
   }
   ```

4. **`Base64Decoder`** - Base64 operations wrapper
   ```kotlin
   class Base64Decoder {
       fun decodeBase64(data: String): String = String(Base64.decode(data, Base64.DEFAULT))
   }
   ```

### System Dependencies Still Needing Wrappers
Based on coverage analysis, these system dependencies still need wrapper interfaces:

1. **UUID Generation**
   - `UUID.randomUUID()` (in UtilExt.kt, NIDSingletonIDs.kt, NIDSharedPrefsDefaults.kt)
   - Impact: Used in identifier services and session management

2. **Environment & Runtime Services**
   - `System.getenv()` (in RootHelper.kt)
   - `Runtime.getRuntime().exec()` (in RootHelper.kt) 
   - Impact: Device fingerprinting and system info collection

3. **Calendar Services**
   - `Calendar.getInstance()` (various places in event models)
   - Impact: Event timestamping and date operations

4. **Android System Services**
   - Context.getSystemService() calls
   - TelephonyManager, LocationManager, SensorManager
   - Impact: Hardware and network service integration

### Wrapper Interface Design Pattern

```kotlin
// EXISTING: NIDTime wrapper (already available)
class NIDTime {
    fun getCurrentTimeMillis(): Long = System.currentTimeMillis()
}

// EXISTING: RandomGenerator wrapper (already available) 
class RandomGenerator {
    fun getRandom(multiplier: Int): Double = Math.random() * multiplier
}

// NEW: UUID wrapper needed
interface NIDUuidProvider {
    fun randomUUID(): String
    fun nameUUIDFromBytes(name: ByteArray): String
}

class NIDSystemUuidProvider : NIDUuidProvider {
    override fun randomUUID(): String = UUID.randomUUID().toString()
    override fun nameUUIDFromBytes(name: ByteArray): String = UUID.nameUUIDFromBytes(name).toString()
}

// NEW: Environment wrapper needed
interface NIDEnvironmentProvider {
    fun getenv(name: String): String?
    fun getProperty(key: String): String?
}

class NIDSystemEnvironmentProvider : NIDEnvironmentProvider {
    override fun getenv(name: String): String? = System.getenv(name)
    override fun getProperty(key: String): String? = System.getProperty(key)
}

// NEW: Calendar wrapper needed
interface NIDCalendarProvider {
    fun getInstance(): Calendar
}

class NIDSystemCalendarProvider : NIDCalendarProvider {
    override fun getInstance(): Calendar = Calendar.getInstance()
}

// EXISTING: Base64Decoder wrapper (already available)
class Base64Decoder {
    fun decodeBase64(data: String): String = String(Base64.decode(data, Base64.DEFAULT))
}
```

## Package-Level Coverage Analysis & Implementation Order

### 1. com.neuroid.tracker.service (Current: ~40% coverage)
**Priority: HIGH** - Core business logic with significant gaps

#### Low Coverage Classes Requiring Phase 0 Wrappers:
- `NIDSessionService` - Session lifecycle management
- `NIDJobServiceManager` - Background task coordination  
- `NIDCallActivityListener` - Phone state monitoring
- `LocationService` - GPS/location services
- `NIDAdvancedDeviceNetworkService` - Network state tracking

#### Implementation Strategy:
1. Create system service wrapper interfaces
2. Add constructor injection for wrappers
3. Build comprehensive unit tests with MockK
4. Target: 80%+ coverage for service classes

### 2. com.neuroid.tracker.utils (Current: ~50% coverage)
**Priority: HIGH** - Utility functions, easiest wins

#### Focus Areas:
- String manipulation utilities
- Date/time formatting helpers
- Validation functions
- Device information helpers

#### Implementation Strategy:
1. Wrap system dependencies (time, environment)
2. Create focused unit tests for pure functions
3. Target: 90%+ coverage (utilities should be highly testable)

### 3. com.neuroid.tracker.events (Current: ~45% coverage)
**Priority: MEDIUM** - Event handling and processing

#### Low Coverage Classes:
- Event creation and validation
- Touch event processing
- Registration helpers

#### Implementation Strategy:
1. Mock event dependencies
2. Test event creation/validation logic
3. Target: 85%+ coverage

### 4. com.neuroid.tracker.models (Current: ~55% coverage)
**Priority: MEDIUM** - Data models and DTOs

#### Focus Areas:
- Model validation
- Serialization/deserialization
- Data transformation

#### Implementation Strategy:
1. Test model constructors and validation
2. Test JSON serialization patterns
3. Target: 90%+ coverage (models are typically highly testable)

### 5. com.neuroid.tracker.storage (Current: ~60% coverage)
**Priority: MEDIUM** - Data persistence layer

#### Implementation Strategy:
1. Mock SharedPreferences and database dependencies
2. Test data CRUD operations
3. Target: 85%+ coverage

### 6. com.neuroid.tracker.callbacks (Current: ~70% coverage)
**Priority: LOW** - Callback interfaces, already good coverage

#### Implementation Strategy:
1. Fill remaining gaps in sensor callbacks
2. Test callback registration/unregistration
3. Target: 85%+ coverage

### 7. Core NeuroID Class (Current: ~65% coverage)
**Priority: HIGH** - Main SDK interface

#### Implementation Strategy:
1. Comprehensive wrapper injection
2. Test public API methods
3. Mock all service dependencies
4. Target: 85%+ coverage

### 8. com.neuroid.tracker.extensions (Current: ~75% coverage)
**Priority: LOW** - Extension functions, good coverage

#### Implementation Strategy:
1. Fill minor gaps in extension utilities
2. Target: 90%+ coverage

### 9. com.neuroid.tracker.compose (Current: ~80% coverage)
**Priority: LOW** - Compose integration, already good coverage

#### Implementation Strategy:
1. Maintain current high coverage
2. Target: 85%+ coverage

## Implementation Phases

### Phase 0: Wrapper Infrastructure (Week 1)
1. ✅ **Leverage existing wrappers**: `NIDTime`, `RandomGenerator`, `NIDBuildConfigWrapper`, `Base64Decoder`
2. **Create missing wrappers**: `NIDUuidProvider`, `NIDEnvironmentProvider`, `NIDCalendarProvider` 
3. **Update class constructors** to inject existing and new wrappers with default implementations
4. **Ensure current tests continue working** with wrapper integration
5. **Update calls to use wrappers** instead of direct system dependencies

### Phase 1: High-Impact Services (Week 2-3)
1. `NIDSessionService` - Core session management
2. `NIDJobServiceManager` - Background processing
3. `LocationService` - GPS functionality
4. `NIDCallActivityListener` - Phone state monitoring

### Phase 2: Utilities & Models (Week 4)
1. Complete utils package testing
2. Comprehensive model validation tests
3. Storage layer testing with mocked dependencies

### Phase 3: Events & Core (Week 5)
1. Event processing and validation
2. Core NeuroID class comprehensive testing
3. Integration test scenarios

### Phase 4: Polish & Extensions (Week 6)
1. Complete callback testing
2. Extension function coverage
3. Compose integration testing
4. Final coverage validation

## Testing Strategy

### Unit Test Patterns
1. **Constructor Injection Pattern**
   ```kotlin
   class ServiceClass(
       private val timeProvider: NIDTimeProvider = NIDSystemTimeProvider(),
       private val uuidProvider: NIDUuidProvider = NIDSystemUuidProvider()
   )
   ```

2. **MockK Integration**
   ```kotlin
   @Test
   fun `test service behavior with mocked dependencies`() {
       val mockTimeProvider = mockk<NIDTimeProvider>()
       every { mockTimeProvider.currentTimeMillis() } returns 1234567890L
       
       val service = ServiceClass(timeProvider = mockTimeProvider)
       // Test implementation
   }
   ```

3. **UnconfinedTestDispatcher for Coroutines**
   ```kotlin
   @Test
   fun `test coroutine behavior`() = runTest {
       val service = ServiceClass(testDispatcher = UnconfinedTestDispatcher(testScheduler))
       // Test coroutine-based functionality
   }
   ```

## Success Metrics

### Coverage Targets by Package
- **services**: 40% → 80% (+40%)
- **utils**: 50% → 90% (+40%) 
- **events**: 45% → 85% (+40%)
- **models**: 55% → 90% (+35%)
- **storage**: 60% → 85% (+25%)
- **callbacks**: 70% → 85% (+15%)
- **core**: 65% → 85% (+20%)
- **extensions**: 75% → 90% (+15%)
- **compose**: 80% → 85% (+5%)

### Overall Target
- **Current**: 61.3% (17,612 / 28,722 instructions)
- **Target**: 80%+ (22,978+ / 28,722 instructions)
- **Additional Instructions**: ~5,366 instructions need coverage

## Risk Mitigation

### Potential Challenges
1. **Complex Android System Dependencies**: Mitigated by comprehensive wrapper strategy
2. **Coroutine Testing Complexity**: Addressed with UnconfinedTestDispatcher pattern
3. **Hardware-Dependent Code**: Isolated through dependency injection
4. **Network-Dependent Operations**: Mocked through service wrappers

### Quality Assurance
1. **Small PR Strategy**: Each class/service implemented separately
2. **Code Review Process**: Ensure wrapper patterns are consistently applied
3. **Coverage Validation**: Verify coverage increases with each PR
4. **Integration Testing**: Ensure mocked components work with real implementations

## Next Steps

1. **Immediate**: Start Phase 0 wrapper implementation
2. **Week 1**: Complete system dependency wrapper infrastructure
3. **Week 2-6**: Execute phases 1-4 according to priority order
4. **Ongoing**: Monitor coverage metrics and adjust strategy as needed

This plan provides a systematic approach to achieving 80%+ unit test coverage while maintaining code quality and ensuring testability through proper dependency injection patterns.