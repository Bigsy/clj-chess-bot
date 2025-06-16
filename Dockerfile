# Build stage
FROM clojure:temurin-21-tools-deps AS builder

WORKDIR /build

# Copy deps.edn first for better layer caching
COPY deps.edn .
RUN clojure -P

# Copy source code
COPY . .

# Build uberjar
RUN clojure -T:build uber

# Runtime stage
FROM eclipse-temurin:21-jre-alpine

RUN apk add --no-cache dumb-init

WORKDIR /app

# Copy the built jar from builder stage
COPY --from=builder /build/target/*.jar app.jar

# Create non-root user
RUN addgroup -g 1001 -S appgroup && \
    adduser -u 1001 -S appuser -G appgroup

USER appuser

# Use dumb-init to handle signals properly
ENTRYPOINT ["dumb-init", "--"]
CMD ["java", "-jar", "app.jar"]