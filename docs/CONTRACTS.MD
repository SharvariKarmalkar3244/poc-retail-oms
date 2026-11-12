# POC 1 Retail Order & Inventory System

## Shared Service Contracts

**Author:** Sharvari Karmalkar

---

# 1. Purpose

This document defines the contracts used by the Order Service to communicate with the Product Service and Inventory Service.

The goal of these contracts is to separate business logic from implementation details.

The Order Service should not know:

- how products are stored
- where product data comes from
- how inventory is maintained
- how stock reservations are tracked internally

Instead, the Order Service communicates through well-defined interfaces.

This allows all team members to develop their modules independently while following a common agreement.

These contracts represent the communication agreements between services and will continue to be used as the project evolves.

By defining the contracts first, each service can be implemented independently without impacting other modules.

---

# 2. Business Context

The system models a retail ordering process.

### Example

Customer places an order for:

| SKU | Quantity |
|------|----------|
| BEV-001 | 2 |
| BEV-002 | 1 |

Before confirming the order, the system must:

1. Verify all products exist.
2. Retrieve product information and prices.
3. Verify enough stock is available.
4. Reserve stock.
5. Confirm the order or reject it.

The Order Service owns the ordering workflow.

To complete its work, it depends on:

- Product Service
- Inventory Service

The contracts in this package define how that interaction occurs.

---

# 3. ProductLookup

## Purpose

Provides access to product catalogue information.

Order Service uses `ProductLookup` whenever it needs to retrieve product details before creating an order.

The customer provides only:

- SKU
- Quantity

The product price is never supplied by the customer.

The price must always come from the product catalogue.

This guarantees pricing integrity and prevents customers from manipulating prices.

---

## Interface

```java
public interface ProductLookup {

    ProductInfo findBySku(String sku);

}
```

### Input

| Field | Description |
|---------|-------------|
| sku | Unique Product SKU |

### Output

Returns:

```java
ProductInfo
```

containing:

- sku
- name
- price

### Example

```java
ProductInfo product =
        productLookup.findBySku("BEV-001");
```

### Why ProductInfo Is Returned Directly

The method returns `ProductInfo` instead of `Optional<ProductInfo>`.

**Reason:**

A missing product is considered a business error rather than an expected result.

The caller expects a valid product.

If the SKU does not exist, `ProductNotFoundException` is thrown.

This leads to a cleaner and more explicit workflow.

### Preferred

```java
ProductInfo product =
        productLookup.findBySku("BEV-001");
```

### Instead Of

```java
Optional<ProductInfo> product =
        productLookup.findBySku("BEV-001");
```

which would require the caller to handle the absence of a product even though a product is mandatory for order creation.

---

# 4. ProductInfo

## Purpose

Represents the product information required by the Order Service.

The Order Service does not require full Product entities.

It only needs:

- SKU
- Product name
- Selling price

Therefore `ProductInfo` acts as a lightweight Data Transfer Object (DTO).

---

## Structure

```java
public record ProductInfo(
        String sku,
        String name,
        BigDecimal price
) {}
```

### Why Records Are Used

Records are ideal for DTOs because:

- immutable by default
- less boilerplate code
- automatically generate:
    - constructor
    - accessors
    - equals()
    - hashCode()
    - toString()
- easier to maintain and read

### Why BigDecimal Is Used

Money values should never use `double`.

Example:

```java
0.1 + 0.2
```

may produce:

```java
0.30000000000000004
```

Using `BigDecimal` ensures monetary calculations remain accurate.

This is the standard approach used in enterprise retail, finance and banking systems.

---

# 5. ProductNotFoundException

## Purpose

Raised when a requested SKU does not exist in the product catalogue.

### Example

```java
productLookup.findBySku("INVALID-SKU");
```

Result:

```java
ProductNotFoundException
```

### Reason

A missing product prevents order creation.

The Order Service cannot continue without valid product information.

Failing immediately keeps the workflow simple and prevents invalid orders.

---

# 6. StockReserver

## Purpose

Provides inventory reservation and inventory release capabilities.

The Order Service does not directly modify inventory.

All inventory operations must occur through this contract.

This ensures inventory rules remain controlled by the Inventory Service.

---

## Interface

```java
public interface StockReserver {

    StockReservationResponse reserve(
            StockReservationRequest request
    );

    StockReleaseResponse release(
            StockReleaseRequest request
    );

}
```

---

# 7. Inventory Business Rules

The Inventory Service must enforce:

```text
Available Stock = On Hand - Reserved
```

Inventory must never:

- become negative
- reserve more stock than available
- exceed physical stock levels

---

## All-Or-Nothing Reservation Rule

Orders must be reserved atomically.

If even one SKU in the order cannot be reserved:

```text
No inventory is reserved.
```

### Example

| SKU | Requested | Available |
|------|-----------|-----------|
| BEV-001 | 2 | 10 |
| BEV-002 | 5 | 3 |

Result:

```text
Reservation Failed
```

Reserved Inventory:

```text
0 units
```

Partial reservations are not allowed.

This ensures order consistency.

---

# 8. Reservation Ownership Rule

Every successful reservation belongs to exactly one order.

Reservations are tracked using:

```java
orderId
```

Inventory Service must associate all reserved stock with the order that created the reservation.

### Example

Order:

```text
ORD-1001
```

Reserves:

```text
BEV-001 → 2
BEV-002 → 1
```

Inventory Service stores ownership of those reservations.

### Why This Matters

Without ownership information there would be no way to determine:

- who reserved stock
- which order is being cancelled
- which inventory must be released

Using `orderId` prevents one order from releasing another order's reservations.

---

# 9. StockReservationLine

## Purpose

Represents a single product entry inside a reservation request.

---

## Structure

```java
public record StockReservationLine(
        String sku,
        int quantity
) {}
```

---

## Example

```java
new StockReservationLine(
        "BEV-001",
        2
);
```

---

# 10. StockReservationRequest

## Purpose

Represents a request to reserve inventory for an entire order.

An order may contain one or many products.

---

## Structure

```java
public record StockReservationRequest(
        String orderId,
        List<StockReservationLine> items
) {}
```

---

## Example

```java
new StockReservationRequest(
        "ORD-1001",
        List.of(
                new StockReservationLine("BEV-001", 2),
                new StockReservationLine("BEV-002", 1)
        )
);
```

---

## Validation Rules

The Inventory Service should reject requests when:

- orderId is missing
- item list is empty
- quantity is zero
- quantity is negative
- duplicate SKUs exist in the request

---

## Invalid Example

```java
new StockReservationRequest(
        "ORD-1001",
        List.of(
                new StockReservationLine("BEV-001", 2),
                new StockReservationLine("BEV-001", 3)
        )
);
```

Duplicate SKUs are not allowed.

Clients must aggregate quantities before sending the request.

---

# 11. StockReservationResponse

## Purpose

Represents the result of a reservation attempt.

---

## Structure

```java
public record StockReservationResponse(
        ReservationStatus status,
        String message,
        String orderId,
        List<InventoryLineResult> items
) {}
```

---

## Why Not Use boolean

Using:

```java
boolean reserve(...)
```

provides only:

```java
true
```

or

```java
false
```

and does not explain:

- why reservation failed
- whether stock was insufficient
- whether the SKU exists
- whether quantity was invalid

A response object provides meaningful business information.

---

## Example Success

```java
{
    status=SUCCESS,
    message="Inventory reserved successfully",
    orderId="ORD-1001"
}
```

---

## Example Failure

```java
{
    status=INSUFFICIENT_STOCK,
    message="Insufficient inventory",
    orderId="ORD-1001"
}
```

---

# 12. ReservationStatus

## Purpose

Represents possible reservation outcomes.

---

## Values

```java
SUCCESS
INSUFFICIENT_STOCK
SKU_NOT_FOUND
INVALID_QUANTITY
```

### SUCCESS

Reservation completed successfully.

### INSUFFICIENT_STOCK

Requested quantity exceeds available stock.

### SKU_NOT_FOUND

Inventory record does not exist for the SKU.

### INVALID_QUANTITY

Quantity is zero or negative.

---

# 13. InventoryLineResult

## Purpose

Represents inventory details for an individual SKU within a response.

---

## Structure

```java
public record InventoryLineResult(
        String sku,
        int requestedQuantity,
        int availableQuantity
) {}
```

---

## Example

```java
{
    sku="BEV-001",
    requestedQuantity=5,
    availableQuantity=3
}
```

This enables SKU-level visibility for reservation and release operations.

---

# 14. StockReleaseRequest

## Purpose

Represents a request to release inventory associated with an order.

Releasing inventory should always happen through an order identifier rather than by SKU and quantity.

---

## Structure

```java
public record StockReleaseRequest(
        String orderId
) {}
```

---

## Example

```java
new StockReleaseRequest(
        "ORD-1001"
);
```

---

# 15. StockReleaseResponse

## Purpose

Represents the outcome of releasing previously reserved inventory.

---

## Structure

```java
public record StockReleaseResponse(
        ReleaseStatus status,
        String message,
        String orderId,
        List<InventoryLineResult> items
) {}
```

---

# 16. ReleaseStatus

## Purpose

Represents possible inventory release outcomes.

---

## Values

```java
SUCCESS
ORDER_NOT_FOUND
ALREADY_CANCELLED
RESERVATION_NOT_FOUND
```

### SUCCESS

Inventory released successfully.

### ORDER_NOT_FOUND

The specified order does not exist.

### ALREADY_CANCELLED

The order has already been cancelled and inventory has already been released.

### RESERVATION_NOT_FOUND

No reservation exists for the supplied order.

---