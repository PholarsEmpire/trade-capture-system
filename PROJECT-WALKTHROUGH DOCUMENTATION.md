## ✍ STEP 1: PROJECT SETUP
The very first thing I did was to clone the upstream repo from CBF and then cloned to my local machine. Installed Maven via environment variable, ensured both the frontend and backend ran as expected.



## ✍ STEP 2: TEST FIXES
The project had some unit tests pre-written which had so many errors and failures that we had to first debug before proceeding to actual feature implementations. My test fixes have been documented in a separated markdown file called "Test-Fixes-Documnetation.md". This details what the error or failure was and the steps I took to correct them.



## ✍ STEP 3: ADDITIONAL FUNCTIONALITY IMPLEMENTATIONS

### 💎**Enhancement 1: Advanced Trade Search System**
🥅 **Business Goal:** Traders need the ability to be able to quickly find trades using multiple search criteria so that they can efficiently manage their trading portfolio.

😭 **Current Problem:** The application only supports basic trade retrieval (getAll, getByID) but lacks the advanced search capabilities such as multi-criteria search or filtering that traders need.

🎯 **Solutions/Implementation:**
1. The first thing I did was to modify the TradeRepository to also extend JpaSpecificationExecutor interface which provides the ability to execute dynamic criteria-based queries/filtering using Specifications
2. I created a helper class called TradeSpecifications.java to build dynamic queries for filtering trades based on various optional criteria. This is necessary because the JPASpecificationExecutor does not actually create those criteria for us, it only executes them via its execution mechanism. In a real-world analogy, the TradeSpecifications class is like a "recipe book" which defines how to filter and what fields to filter by, it does not actually execute them, the JPASpecificationsExecutor executes the combined recipe (query) (it's like the chef that does the cooking based on the given recipe).

    Each method in the class returns a Specification (a re-usable filter) that can be combined to form complex queries with having to write any SQL queries. If a filter parameter is null or empty or not provided, the corresponding Specification returns a no-op (conjunction) meaning it does not affect the query. This approach allows for flexible and reusable query construction in the service layer. In real life scenarios, this approach is seen when searching for laptop on e-commerce sites where multiple optional filters (the laptop specifications) can be applied.

    NOTE: I have implemented the tradeStartDate in my criteria search because its most likely going to be the most date the operations team are interested in especially for when we want to get Settlement operations (cash moves on this date), Risk management (position starts counting from here), Cashflow projections (when money actually flows) etc.


3. Moving on, I added a searchTrades() method to the TradeService.java for dynamic search using the TradeSpecifications class;

```

public List<Trade> searchTrades(String counterparty,

                             String book,

                             String trader,

                             String status,

                             LocalDate from,

                             LocalDate to) {



     Specification<Trade> spec = Specification

             .where(TradeSpecifications.hasCounterparty(counterparty))

             .and(TradeSpecifications.hasBook(book))

             .and(TradeSpecifications.hasTrader(trader))

             .and(TradeSpecifications.hasStatus(status))

             .and(TradeSpecifications.dateBetween(from, to));



     return tradeRepository.findAll(spec);

 }

```

4. Next, an overloaded method, also called searchTrades() which handles paginated search and sorting; and a searchByRsql(String query, Pageable pageable) for complex searches were also added to TradeService.java class. The advantage of the RSQL search over the other implemented multi-criteria search is that the user is not confined or limited to some particular search criteria, it gives the flexibility of defining and searching based on your own intended criteria which I find really intriguing and the RSQL statements are also easy to write and understand.

5. I then added additional endpoints to the TradeController.java class (@GetMapping("/search"), @GetMapping("/filter") and @GetMapping("/rsql")) for searching and filtering trades based on the above methods I have written in the TradeService class.

6. For @GetMapping("/filter") and @GetMapping("/rsql"), I created 2 utility classes called RsqlSpecificationBuilder.java and RsqlVisitor.java. The RsqlSpecificationBuilder uses the inbuilt RSQLParser library to parse (or transform) the query string into an AST (Abstract Syntax Tree, which is a tree representation of the syntax structure) and the RsqlVisitor to convert RSQL AST (Abstract Syntax Tree) nodes which is received from an rsql-parser into Spring Boots JPA Specifications which is then passed onto the repository for generating the SQL syntax automatically. 

RSQL's architecture is like understanding a foreign language.

```

User's RSQL Query (English) 
    ↓
RSQLParser (Dictionary) 
    ↓
Abstract Syntax Tree - AST (Grammar Structure)
    ↓
RsqlVisitor (Translator)
    ↓
JPA Specification (SQL that database understands)

```

The RSQL process workflow:

i. Takes a raw RSQL string (e.g., "counterparty.name==MegaFund;tradeDate=ge=2025-01-01")
ii. Uses RSQLParser (from the library) to parse it into an Abstract Syntax Tree (AST)
iii. Passes the AST to RsqlVisitor for conversion
iv. Returns the final JPA Specification

🎯 **Business Impact:** With the implementation of these advanced searches, traders and managers will be able to search and filter for trades and make informed decisions without having to scan through hundreds or thousands of trades while also saving time.




### 💎**Enhancement 2: Comprehensive Trade Validation Engine**
---

This is where I turned my TradeService class into a risk-proof validation engine. It acts like a "**bouncer**", so no invalid or risky trades slips through the cracks.

🥅 **Business Goal:** As a risk manager, I need comprehensive validation of all trade data to prevent invalid trades from entering our systems and causing operational issues.

😭 **Current Problem:** Only basic field validations exist, but comprehensive business rule validation and user privilege enforcement are missing. This means that ANY application_user (even those without the TRADER or SALES privilege can create trades, and also there is no proper business rules validation checks (such as start date, maturity date, legs consistency etc validation checks in place).

🎯 **Solutions/Implementation:**
1. I started by creating a simple DTO object class called ValidationResult.java to store validation results and error messages. It helps in aggregating multiple validation errors and returning them together to the caller. That can greatly improve user experience.

2. I then added date validation, leg consistency and entity existence checks validation methods to TradeService.java. These method are: validateTradeBusinessRules(), validateTradeLegConsistency().

3. Next, I added a method for user validations (validateUserPrivileges()) in the TradeService class which checks every user's roles and the privileges given to them. This ensures that only users with some certain privileges can perform some trade actions. Only users with the TRADER, SALES or SUPERUSER roles for example should be able to create and terminate a trade. 

4.  I included spring-boot-starter-security and spring-security-config in my pom.xml file for user authentications. And created other security configuration files in the security package/folder of the project for password encoding and user authentications.

 	```
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-security</artifactId>
</dependency>

<dependency>
    <groupId>org.springframework.security</groupId>
    <artifactId>spring-security-config</artifactId>
</dependency>
```

5. Next, I integrated the above validation methods (validateBusinessRules() and validateUserPrivilege()) into the createTrade method and other methods that needs authentication (before populating reference data and before a trade is created, saved, amended, terminated or deleted). This ensures no trade can be saved/modified if it violates business rules or if the user does not have the required privilege.

6. For production grade advanced security practice, I created a utility class or runner called PasswordMigrationRunner.java to encrpyt all existing users passwords which were saved as plain text in the database and its not safe. I then included a password encoder method in the ApplicationUserService class to encrypt the passwords of newly created users before saving them into the application_user database table.

7. I then added basic unit tests to TradeServiceTest.java to test and validate the set business rules/validations and user privileges.

8. Also, I integrated these validations into my controller layer (TradeController.java) so invalid trades return a clean, clear and friendly 400 error messages instead of exceptions.

9. All of these ensures:

 	        - Every trade is validated before saving

 	        - All legs follow cross-validation rules

 	        - Role-based privilege control is in place

 	        - Traders can't create trades missing essential details or information.

10. I then tested some of the methods in the TraderControllerTest.java




### 💎**Enhancement 3: Trader Dashboard and Blotter System**
---

🥅 **Business Goal:** As a trader, I need personalized dashboard views and summary statistics so that I can monitor my positions and make informed trading decisions.

😭 **Current Problem:** No personalized views or summary information available for traders to monitor their activity.

🎯 **Solutions/Implementation:**

1. I created 2 DTO classes (TradeSummaryDTO and DailySummaryDTO) that will be used to return dashboard summaries; both for the total overall trade summaries and analytics for the current date/day.
2. I then extended my TradeService.java by adding new methods for summary and dashboard logics. The getTradesByTrader() method gets all traders booked by the currently logged in user. getTradesByBook() gets all trade analytics by book ID, getTradeSummary() gets the overall trade summary analytics for all existing trades, calculateNetExposureByCounterparty() calculates net exposure by counterparty and so many more useful methods needed for trade analytics.
3. I then extended my controller with the new dashboard endpoints as well for all these newly added service methods.




## ✍ STEP 4: BUG INVESTIGATION AND FIX REPORT (TRD-2025-001)

😭 **Current Problem:** Critical cashflow calculation bug producing values approximately 100x larger than expected, affecting all fixed-leg interest rate calculations in production. These can have huge negative business impacts such as **incorrect risk exposure calculations, potential regulatory reporting issues, trading desk P&L miscalculations and client settlement discrepancies**.

😭 **Root Cause:** The rate value used in calculateCashflowValue method was not being converted to decimal (it was treated as 3.5 instead of 0.035), as a result all calculations are 100x larger than the expected value. There were also some floating-point precision issues using double for monetary calculations.

🎯 **Solutions/Implementation:** 
1.	I changed all the variable data types in the calculateCashflowValue() method of the TradeService.java class and the calculation data types to BigDecimal for exact precision.




## ✍ STEP 5: FULL-STACK IMPLEMENTATION

🥅 **Business Goal:** Traders need to be able to capture settlement information when other trade information are being captured on the system.

😭 **Current Problem:** The Trade Capture System lets traders create and manage trades — but it doesn’t capture settlement instructions (i.e., how and where a trade should be settled after it’s booked). Right now, traders send those settlement details separately through emails, excel sheets and maybe chats which is very risky and could impact the business greatly because people forget, mistype and may lose those details.

😭 **Root Cause:** Settlement information are not being captured when a trade is booked or executed.

🎯 **Solutions/Implementation:** My goal is to give every trade the ability to store and update its settlement instructions by adding settlement instructions to every trade during booking - so it's stored in the system, visible to operations and searchable later in needed. I am following the Option B approach suggested in the README file (using the AdditionalInfo entity table) for ease of expansion later e.g to store other dynamic trade information like Clearing Info, Client Notes etc). The AdditionalInfo table is like a notes section where I can store any custom key/value pairs linked to a trade.

1.	I started the implementation by creating a DTO settlement class called SettlementInstructionDTO.java which simply defines what data comes from the user when updating settlement instructions. I used the @NotBlank annotation to ensure that its not always left blank/null. I decided to create a new DTO class and not add this to the TradeDTO because when updating or searching for settlement instructions only, I don’t want the entire TradeDTO information to be populated — I only want to work with the settlement instructions field that is required; this is to avoid any accidental errors with other trade information (e.g accidental delete or change); also for expandability in the future and for separation of concerns.

2.	I then added a few queries to the AdditionalInfoRepository.java for additional search capabilities. findByEntityTypeAndEntityIdAndFieldName: finds if a trade already has instructions and this is useful when we need to update an instruction while findEntityIdsByFieldNameAndValueContainingIgnoreCase lets us find trades containing a specified keyword or phrase in its settlement instruction.

3.	Next, I added a few methods called saveSettlementInstructions(), getSettlementInstructions(), and findTradesBySettlementInstructions() to my AdditionalInfoService class. The first method handles both creating new settlement instructions for new trades and updating existing ones if they already exist. The third method will be useful to the operations team to quickly find trades mentioning certain phrase in their settlement instruction (e.g "Euroclear or JPM").

4.	Moving on, to implement the proper business logic flow, I also created 2 new methods in TradeService class called updateSettlementInstructions() which calls the AdditionalInfoService class for versioned storage and also a searchBySettlementInstructions() method which gets a list of all trades that meets the search criteria from the TradeRepository.

5.	I then added 2 new endpoints to my TradeController.java for updating an existing settlement instruction, creating an instruction for a trade if not already exists and also search for settlement instructions containing some words or phrases.

6.	To ensure the settlement instruction text meets business and security requirements, I created a method called validateSettlementInstructions() in my TradeService class and then used it in both createTrade and amendTrade methods to prevent invalid settlement instructions when booking a new trade or during an update/amendment.

7. Also, to ensure that settlement instructions are returned with other trade information when we search getAllTrades() or getTradeById() , I included the below code in those methods in my controller layer.

    ```
    additionalInfoService.getSettlementInstructions(trade.getTradeId())
                .ifPresent(dto::setSettlementInstructions);
    
    ```

7.	For the frontend implementation part of the task, I added a new form field of input type to the tradeFormFields.ts under frontend/src/utils package.




## ✍ STEP 6: CONTAINERIZATION

Step 6 is about containerization — turning my backend (Spring Boot app) and frontend (React app) into Docker containers that can run anywhere.

Containerization means packaging apps (and all its dependencies, JDKs, NPM packages, etc.) into a self-contained image that can run anywhere — locally on the laptop, a cloud server, or Kubernetes — with no environment differences.

🎯 **Solutions/Implementation:**
1. I created `a dockerfile for my backend spring-boot application`. The file was created in /backend/Dockerfile. Below is the content of the dockerfile.

```
# =========================
#  Build stage
# =========================
FROM maven:3.9.6-eclipse-temurin-21-alpine AS build
WORKDIR /app

# Copy dependency files first for better layer caching
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy source code and build
COPY src ./src
RUN mvn clean package -DskipTests

# =========================
# Runtime stage
# =========================
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Create non-root user for security
RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

# Copy the jar from build stage
COPY --from=build /app/target/*.jar app.jar

# Expose the application port
EXPOSE 8080

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
  CMD wget --quiet --tries=1 --spider http://localhost:8080/actuator/health || exit 1

# Run the app
ENTRYPOINT ["java", "-jar", "app.jar"]

```

| Line                                     | Meaning                                    |
| ---------------------------------------- | ------------------------------------------ |
| `maven:3.8.6-openjdk-17`                 | Uses Maven image to build your project     |
| `COPY src ./src`                         | Copies all code into the container         |
| `RUN mvn clean package`                  | Builds the JAR                             |
| `openjdk:17-jdk-slim`                    | Lightweight runtime image for running Java |
| `COPY --from=build`                      | Copies built JAR from previous stage       |
| `ENTRYPOINT ["java", "-jar", "app.jar"]` | Runs the application when container starts |


2. I also creater `a dockerfile for my frontend application`. Below is the content of the frontend dockerfile.

```
# =========================
# Build stage
# =========================
FROM node:18-alpine AS build
WORKDIR /app

# Copy dependency files first for better layer caching
COPY package*.json ./
COPY pnpm-lock.yaml ./

# Install dependencies
RUN npm install -g pnpm && pnpm install

# Copy source code and build
COPY . .
RUN pnpm run build

# =========================
# Serve stage
# =========================
FROM nginx:1.25-alpine

# Remove default nginx config
RUN rm /etc/nginx/nginx.conf

# Copy custom nginx config
COPY nginx.conf /etc/nginx/nginx.conf

# Copy built assets from build stage
COPY --from=build /app/dist /usr/share/nginx/html

# Create non-root user for security
RUN chown -R nginx:nginx /usr/share/nginx/html && \
    chown -R nginx:nginx /var/cache/nginx && \
    chown -R nginx:nginx /var/log/nginx && \
    chmod -R 755 /usr/share/nginx/html

# Switch to non-root user
USER nginx

# Expose port
EXPOSE 80

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=5s --retries=3 \
  CMD wget --quiet --tries=1 --spider http://localhost/health || exit 1

# Start nginx
CMD ["nginx", "-g", "daemon off;"]

```

| Stage            | Purpose                                    |
| ---------------- | ------------------------------------------ |
| `node:18-alpine` | Builds the frontend assets (HTML, JS, CSS) |
| `npm run build`  | Generates the production build             |
| `nginx:alpine`   | Serves the static files efficiently        |

3. Next, I created `a docker compose file to run both together`. This is created in the project's root folder as docker-compose.yml and below is its content.

```
version: '3.8'
services:
  backend:
    build: ./backend
    container_name: trade_backend
    ports:
      - "8080:8080"
    networks:
      - app-network
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:8080/actuator/health"]
      interval: 30s
      timeout: 10s
      retries: 3

  frontend:
    build: ./frontend
    container_name: trade_frontend
    ports:
      - "3000:80"
    depends_on:
      - backend
    networks:
      - app-network

networks:
  app-network:
    driver: bridge


```

| Section             | Meaning                                          |
| ------------------- | ------------------------------------------------ |
| `services.backend`  | Builds and runs the backend image                |
| `services.frontend` | Builds and runs frontend image                   |
| `healthcheck`       | Ensures the backend container is healthy before frontend connects to it                    |
| `depends_on`        | Ensures backend starts first                     |
| `networks`          | Enables the two containers to talk to each other |
| `ports`             | Maps container → local machine ports             |

4. I then used the below command to run the docker compose file. This is ran from the project's root folder where the docker-compose.yml lives.

```
docker compose up --build

```

⚙️ **Some Useful Commands on How to Use and Run Docker**
```
# Build and start all services
docker-compose up --build

# Start in detached mode (background)
docker-compose up -d

# View logs
docker-compose logs -f

# Stop all services
docker-compose down

# Rebuild specific service
docker-compose build backend
docker-compose build frontend

```

And these are very useful for testing if everything is running as expected.

```
# 1. Check if containers are running
docker ps

# 2. Check backend health
curl http://localhost:8080/actuator/health

# 3. Check frontend (should serve React app)
curl http://localhost:3000

# 4. Check API proxy through frontend
curl http://localhost:3000/api/trades

# 5. Check container logs
docker-compose logs backend
docker-compose logs frontend

```