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
- how stock reservations are performed

Instead, the Order Service communicates through well-defined interfaces.

This allows all team members to develop their modules independently while following a common agreement.

These contracts represent the communication agreements between services and will continue to be used as the project evolves.

By defining the contracts first, each service can be implemented independently without impacting other modules.

---

# 2. Business Context

The system models a retail ordering process.

### Example

Customer places an order for:

- SKU: `BEV-001`
- Quantity: `2`

Before confirming the order, the system must:

1. Verify the product exists.
2. Retrieve product information and price.
3. Verify enough stock is available.
4. Reserve stock.
5. Confirm or reject the order.

The Order Service owns the ordering workflow.

To complete its work it depends on:

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

### Why ProductInfo is Returned Directly

The method returns `ProductInfo` instead of `Optional`.

**Reason:**

A missing product is considered a business error rather than an expected result.

The caller expects a valid product.

If the SKU does not exist, `ProductNotFoundException` is thrown.

This leads to a cleaner and more explicit workflow.

**Example:**

Preferred:

```java
ProductInfo product =
    productLookup.findBySku("BEV-001");
```

Instead of:

```java
Optional<ProductInfo> product =
    productLookup.findBySku("BEV-001");
```

which would force every caller to handle `Optional`.

---

# 4. ProductInfo

## Purpose

Represents the product information required by the Order Service.

The Order Service does not require full Product entities.

It only needs:

- SKU
- Product name
- Selling price

Therefore `ProductInfo` acts as a lightweight data transfer object.

---

## Structure

```java
public record ProductInfo(
    String sku,
    String name,
    BigDecimal price
) {}
```

### Why BigDecimal is Used

Money values should not use `double`.

Example:

```java
0.1 + 0.2
```

may produce:

```java
0.30000000000000004
```

Using `BigDecimal` ensures monetary calculations remain accurate.

This is the standard approach used in enterprise retail and banking applications.

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

Provides inventory reservation capabilities.

The Order Service does not directly modify inventory.

All inventory operations must occur through this contract.

This ensures inventory rules remain controlled by the Inventory Service.

---

## Business Rules

The Inventory Service must enforce:

```text
Available Stock = On Hand - Reserved
```

Inventory must never:

- become negative
- exceed physical stock levels

Reservations occur only when sufficient stock exists.

---

## Interface

```java
public interface StockReserver {

    StockReservationResponse reserve(
        StockReservationRequest request
    );

    StockReleaseResponse release(
        String sku,
        int quantity
    );
}
```

---

# 7. StockReservationRequest

## Purpose

Represents a request to reserve stock.

---

## Structure

```java
public record StockReservationRequest(
    String sku,
    int quantity
) {}
```

### Example

```java
new StockReservationRequest(
    "BEV-001",
    2
);
```

---

# 8. StockReservationResponse

## Purpose

Represents the result of a stock reservation attempt.

### Why Not Use boolean

Using:

```java
boolean reserve(...)
```

only provides:

```java
true
```

or

```java
false
```

It does not explain:

- why reservation failed
- how much stock was available
- whether SKU exists
- whether quantity was invalid

A richer response provides meaningful business information.

---

## Example

### Success

```java
{
    status=SUCCESS,
    message="Stock reserved successfully"
}
```

### Failure

```java
{
    status=INSUFFICIENT_STOCK,
    message="Only 3 units available"
}
```

---

# 9. ReservationStatus

## Purpose

Represents the outcome of reservation operations.

---

## Values

```java
SUCCESS
INSUFFICIENT_STOCK
SKU_NOT_FOUND
INVALID_QUANTITY
```

### SUCCESS

Reservation completed.

### INSUFFICIENT_STOCK

Requested quantity exceeds available stock.

### SKU_NOT_FOUND

Inventory record does not exist.

### INVALID_QUANTITY

Quantity is zero or negative.

---

# 10. Stock Release

## Purpose

Releases previously reserved stock.

Used when:

- an order is cancelled
- a reservation must be rolled back

---

## Business Rule

Releasing stock should:

```text
reserved = reserved - quantity
```

and therefore:

```text
available = available + quantity
```

---

## Example

### Current Inventory

```text
On Hand = 10
Reserved = 4
Available = 6
```

### Release

```text
Quantity = 2
```

### Result

```text
On Hand = 10
Reserved = 2
Available = 8
```