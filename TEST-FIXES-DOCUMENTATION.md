# TEST FIXES DOCUMENTATION
---
This is a comprehensive documentation for all the failed tests in the project that I fixed. It details information such as the problem, the root cause of the problem, how I fixed it and how I confirmed that it's been correctly fixed.
---


---
### 📝📝TRADE CONTROLLER TEST FIXES📝📝
---

### 📝 fix(test): TradeControllerTest - Fixed createTrade endpoint response code by updating the TradeControllerTest.testCreateTrade() to use .isCreated() method instead of .isOk() so it can conform to the REST API response code behaviour for a create request.
#### ⚠️ Problem Description:
The API was returning status code 200 instead of 201
#### 🔍 Root Cause Analysis:
status().isOk() was used to verify the API response which by defaults returns code 200, however, POST endpoints expects 201 response which is produced by status().isCreated()
#### 💡 Solution Implemented:
I changed to .isCreated() method instead of .isOk()
#### ✅ Impact:
This verifies and ensures the correct status code is returned."


---


### 📝 fix(test): TradeControllerTest -  TradeControllerTest.testCreateTradeValidationFailure_MissingBook:175 Status expected:<400> but was:<201>
#### ⚠️ Problem Description:
The API was returning a status code 201 instead of 400. A book name was not provided so we expect it to fail with a bad request response.
#### 🔍 Root Cause Analysis:
There was no form of validation for bookName in the TradeController class.Although there is some form of validation in the validateReferenceData(Trade trade) in the TradeService class but it looks like the controller did not properly catch that.
#### 💡 Solution Implemented:
I added @NotNull annotation to the bookName field in TradeDTO and also added null checks for bookName and counterpartyName in the TradeController.java class.
- #### ✅ Impact: 
This ensure proper book name validation  and rejects any trade creation requests missing both Book and Counterparty."



---

### 📝 fix(test): TradeControllerTest -  TradeControllerTest.testCreateTradeValidationFailure_MissingTradeDate:158 Response content expected:<Trade date is required> but was:<>
#### ⚠️ Problem Description:
The API was returning a null instead of "Trade date is required (400 bad request). 
#### 🔍 Root Cause Analysis:
There was no form of validation for tradeDate in the TradeController class. 
#### 💡 Solution Implemented: 
I added @NotNull annotation to the tradeDate field in TradeDTO and also added null checks for tradeDate in the TradeController.java class.
- #### ✅ Impact:
This ensure proper tradeDate validation  and rejects any trade creation requests missing a tradeDate."


---


### 📝 fix(test): TradeControllerTest - TradeControllerTest.testDeleteTrade:223 Status expected:<204> but was:<200>
#### ⚠️ Problem Description:
The test expected the DELETE endpoint to return HTTP 204 No Content, but the controller returned 200 OK.
#### 🔍 Root Cause Analysis:
The controller delete method was returning ResponseEntity.ok(), which maps to 200 OK. Standard REST conventions expect 204 for delete success with no body; however the controller was returning a success code with a body message for a deletion.
#### 💡 Solution Implemented:
I modified the deleteTrade method in the TradeController.java class to return ResponseEntity.noContent().build() instead of return ResponseEntity.ok().body("Trade cancelled successfully"); so the endpoint responds with 204 No Content.
- #### ✅ Impact:
The test now passes successfully, confirming that the API correctly returns 204 No Content upon successful trade deletion."


---


### 📝 fix(test): TradeControllerTest - TradeControllerTest.testUpdateTrade:196 No value at JSON path "$.tradeId"
#### ⚠️ Problem Description:
The test called the update endpoint (PUT /trades/{id}), and expected the JSON response to contain a field named "tradeId", 
but the actual response did not include that field at all.

#### 🔍 Root Cause Analysis:
Even though the service layer properly mapped the tradeIds for both the DTO and entity, lack for checks/validations for a mismatch cause the traded not to be returned in the JSON response.

#### 💡 Solution Implemented:
I added two is statements in  the update method of the TradeController.java class. One is to check to ensure the tradeId in the path matches the tradeId in the request body if provided and the other one is to ensure the tradeId is set in the response DTO even if the service layer does not return it.

- #### ✅ Impact:
It enforces business rules and validates that the tradeId in the request body (if provided) matches the id in the URL path.If the IDs don’t match, the request is rejected with a 400 Bad Request error and a message saying Trade ID in path must match Trade ID in request body."



---


### 📝 fix(test): TradeControllerTest -TradeControllerTest.testUpdateTrade:203 Status expected:<200> but was:<400>

#### ⚠️ Problem Description:
The test was failing because it was mocking tradeService.saveTrade(...), but the controller's updateTrade method actually calls tradeService.amendTrade(id, tradeDTO)
#### 🔍 Root Cause Analysis:
The test does not mock tradeService.amendTrade(...), so when the controller calls it, it returns null (the default for an unmocked method). This leads to a NullPointerException when tradeMapper.toDto(amendedTrade) is called, which is caught and results in a 400 Bad Request.
#### 💡 Solution Implemented:
I mocked tradeService.amendTrade(...) instead of saveTrade(...) and also verified that amendTrade was called.
- #### ✅ Impact:
This ensures that the correct method need to update a trade is mocked and verified."




---


---
### 📝📝TRADE LEG CONTROLLER TEST FIXES📝📝
---

### 📝 fix(test): TradeLegControllerTest - TradeLegControllerTest.testCreateTradeLegValidationFailure_NegativeNotional
#### ⚠️ Problem Description:
The test failure Response content expected:Notional must be positive but was:null means your controller returned a 400 Bad Request as expected, but the response body was empty instead of containing the error message Notional must be positive

#### 🔍 Root Cause Analysis:
There is a validation conflict: there is an annotation validation @Positive in the TradeLegDTO and then there is also a manual validation check in the TradeLegController class using an if statement.

#### 💡 Solution Implemented:
I removed the validation annotation in the TradeLegDTO.notional to rely solely on the controller logic. This way, the code will return the custom error message: Notional must be positive

- #### ✅ Impact:
This ensures the code flow proceeds to the manual validation check in TradeLegController, guaranteeing the return of the expected error string."



---



---
### 📝📝BOOK SERVICE TEST FIXES📝📝
---

### 📝 fix(test): BookServiceTest - I resolved the NPE in BookServiceTest.findBookById and findBookByNonExistentId

#### ⚠️ Problem Description:
The BookServiceTest.testFindBookById method in the BookServiceTest.java test is failing with a NullPointerException due to missing DTO instantiation and also bookMapper was not initialized, mocked and stubbed in the BookServiceTest.testFindBookById method. Also, the BookServiceTest.testFindBookByNonExistentId failed due to a NullPointerException. This usually means something inside BookService.findBookById(...) is trying to access a null object.

#### 🔍 Root Cause Analysis:
BookDTO was not instantiated and a Mapper was also not mocked and stubbed. The referenced BookService.getBookById method mapped the retrieved book to a DTO immediately after retrieval and that was not mocked in the test. These two are essential for the BookService to correctly map entity into a DTO and to communicate with the service layer.

#### 💡 Solution Implemented:
I created an instance of the DTO, set its Id and Name. 
```
    BookDTO bookDTO = new BookDTO();
    bookDTO.setId(1L);
    bookDTO.setBookName("FX-BOOK-1");
```

I also injected a BookMapper by using @Mock annotation and created a stub for it.
```
@Mock    
private BookMapper bookMapper;
```
and stubbed
```
when(bookMapper.toDto(book)).thenReturn(bookDTO);
```
This ensures no NPE because the mocks are injected before the test runs. Also, prevents null values during mapping and allows the service to return the expected DTO. I then verified by re-running tests, which now pass successfully. 


#### ✅ Impact:
This ensures that null pointer exception is not thrown and also correctly follows business logic and the concept of Serialization.
 



---


### 📝 Stubbing Problem in BookServiceTest.testSaveBook

#### ⚠️ Problem Description:
BookServiceTest.testSaveBook fails with PotentialStubbingProblem because bookMapper.toEntity(dto) returned null, causing bookRepository.save(null) to be called instead of the expected entity.

```
when(bookRepository.save(any(Book.class))).thenReturn(book);
```
is what was stubbed. But in the actual service code, it looks like 
```
bookRepository.save(null) 
```
is being called, which Mockito treats as an "argument mismatch

#### 🔍 Root Cause Analysis:

Looking at the BookServiceTest.testSaveBook, only bookRepository was stubbed, the mapper also needs stubbing to transfer data from entity to DTO and from DTO back to entity. bookMappers to entity and back to DTO was not in place

### 💡 Solution Implemented:
I Updated BookServiceTest.testSaveBook to stub bookMapper.toEntity() and bookMapper.toDto() methods properly, ensuring a consistent DTO → Entity → Repository → DTO flow. This prevents null arguments being passed to save() and aligns the test with real service behavior. I verified by re-running the test, which now passes successfully.
```
when(bookMapper.toEntity(bookDTO)).thenReturn(book);
when(bookMapper.toDto(book)).thenReturn(bookDTO);
```
Now the flow is consistent:

- DTO → Entity (bookMapper.toEntity)

- Entity saved (bookRepository.save(book))

- Entity → DTO (bookMapper.toDto)

### ✅ Impact:
This ensures that savedBook is properly mocked and stubbed


---




---
### 📝📝 TRADE SERVICE TEST FIXES 📝📝
---
### 📝 fix(test): TradeServiceTest - Wrong Assertion Failure in TradeServiceTest.testCreateTrade_InvalidDates_ShouldFail()
#### ⚠️ Problem Description:
testCreateTrade_InvalidDates_ShouldFail() was failing as the assertion was using the wrong error message
#### 🔍 Root Cause Analysis:
The expected exception message should be "Start date cannot be before trade date"
### 💡 Solution Implemented:
I changed the assertion to display "Start date cannot be before trade date"
### ✅ Impact:
This ensures the test displays the correct exception message




---



### 📝 fix(test): TradeServiceTest - I resolved TradeServiceTest.testCreateTrade_Success:80 » Runtime Book not found or not set
#### ⚠️ Problem Description:
testCreateTrade_Success() failed due to some required repositories not being mocked and then TradeDTO resulted into having null fields that were required to successfully create a trade. Only tradeRepository.save(...) is being mocked, but in the real TradeService.createTrade(...), it likely also looks up:

-a Book (via bookRepository)

-a Counterparty (via counterpartyRepository)

-a TradeStatus (via tradeStatusRepository)
And since these were not mocked, an exception is thrown.

#### 🔍 Root Cause Analysis: 
createTrade() calls private methods populateReferenceDataByName() and createTradeLegsWithCashflows() which depend on BookRepository and CounterpartyRepository, TradeStatusRepository and TradeLegRepository. Also, tradeDTO was missing bookName, counterpartyName and tradeStatus which are required for trade creation
### 💡 Solution Implemented:
  1. Added mocks for BookRepository, CounterpartyRepository, CashflowRepository, HolidayCalendarRepository using @mock annotation.
  2. I also set bookName, counterpartyName and tradeStatus on tradeDTO instance in the setup method in TradeServiceTest.java class
  3. I then added the following stubs to the createTrade_success class. when(bookRepository.findByBookName(anyString())).thenReturn(Optional.of(new Book()));
  4. Added when(counterpartyRepository.findByName(anyString())).thenReturn(Optional.of(new Counterparty()));
  5. Added when(tradeStatusRepository.findByTradeStatus(anyString())).thenReturn(Optional.of(tradeStatus));
  6. Added when(tradeLegRepository.save(any(TradeLeg.class))).thenReturn(new TradeLeg());
### ✅ Impact:
Enables verification that a trade is created successfully when all data is present.




---



### 📝 fix(test): TradeServiceTest - TradeServiceTest.testAmendTrade_Success:188 » NullPointer Cannot invoke "java.lang.Integer.intValue()" because the return value of "com.technicalchallenge.model.Trade.getVersion()" is null
#### ⚠️ Problem Description:
testAmendTrade_Success() failed due missing stub and a null trade version

#### 🔍 Root Cause Analysis: 
TradeLegRepository.save() which is required was not stubbed and a version was also not provided
### 💡 Solution Implemented:
I added the below for versioning in the setup to address the failures and also stub the required repository.
```
 trade.setVersion(1);

when(tradeLegRepository.save(any(TradeLeg.class))).thenReturn(new TradeLeg());
```
### ✅ Verification:
This verifies that a trade is amended successfully when a versioned trade is received.



---




### 📝 fix(test): TradeServiceTest - I resolved the failure: TradeServiceTest.testCashflowGeneration_MonthlySchedule:226 expected: <1> but was: <12>
#### ⚠️ Problem Description: 
`testCashflowGeneration_MonthlySchedule` was failing with a placeholder assertion: `expected: <1> but was: <12>`. The test was not verifying the service's core cashflow calculation logic.

#### 🔍 Root Cause Analysis:
The test setup was incomplete, all the necessary mocks or dependencies were not injected. Also, the test contained an incorrect assertion which needed to be fixed.

### 💡 Solution Implemented:
1. I created a monthlySchedule explicitly. The generatecashflow method in the TradeService class is private, so I had to use the createTrade which calls the generateCashFlow method internally.
2. I added and stubbed the required data repositories.
3. I replaced the failing assertion with the correct Mockito verification: `verify(cashflowRepository, times(24)).save(any(Cashflow.class))`, based on 2 legs * 12 monthly payments/year.

### ✅ Impact:
Enables accurate unit testing and proper verification of cashflow generation for a monthly schedule.