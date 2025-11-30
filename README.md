
# InvoiceDoc – Invoicing platform

InvoiceDoc is application to manage invoices and profiles.  
It lets an organisation:

- define **seller profiles** (company data + bank account)  
- manage **contractors** (clients / suppliers)  
- create **invoices** with line items  
- **generate PDF invoices**  
- *import invoice data from JSON* – including auto‑creation of seller profile + contractor for JSON

---

## 1. Technology stack

### Backend

- **Java + Spring Boot**
- **Spring Web / MVC**
- **Spring Security*  
  - JWT authentication
  - Method‑level security via `@PreAuthorize`
- **Spring Data JPA / Hibernate**
- **Database** – Relational DB supported by JPA: PostgreSQL
- ** OpenPDF ** for PDF generation
- **Jackson** for JSON and XML (via `jackson-dataformat-xml`)
- **Lombok** for boilerplate reduction (getters, builders, etc.)

### Frontend

- **nginx** bundler
- **Fetch API** for HTTP requests
- JWT stored in **`sessionStorage`**
- **vite** for dev use
---

## 2. Level of architecture

- Each **user** belongs to an **organisation**.
- Each organisation can have multiple:
  - **Seller profiles** – data used as seller on invoices
  - **Contractors** – buyers / clients
  - **Invoices** – issued for given seller profile + contractor
- **Import module** can:
  - accept JSON data
  - map it to internal DTO model
  - ensure contractor exists (create if needed)
  - create an **Invoice** using existing `InvoiceService`
  - PDF generation uses a dedicated `InvoicePdfService`.

---

## 3. Backend modules overview

### 3.1 Security

`SecurityConfig`:

- exposes `SecurityFilterChain`
- uses a custom `JwtAuthenticationFilter`
- endpoints:
  - `/v1/api/auth/login`, `/v1/api/auth/register` – **public**
  - everything else – **requires JWT**

`CurrentUserProvider`:

- reads authenticated user from security context
- exposes:
  - `id`
  - `organizationId`
  - `role` (`OWNER`, `ADMIN`, `ACCOUNTANT`)

### 3.2 Seller profiles

Entity: `SellerProfileEntity`

- belongs to an **organisation**
- fields (simplified):
  - `id`
  - `name`
  - `nipEncrypted`
  - `regon`
  - `krs`
  - `bankName`
  - `bankAccount`
  - `AddressEmbeddable address`
  - `defaultCurrency`
  - `defaultPaymentTermDays`
- API (simplified, prefix `/v1/api/seller-profiles`):
  - `GET /` – list seller profiles in current organisation (Owner / Admin )
  - `POST /` – create new seller profile (Owner / Admin)
  - `PUT /{id}` – update seller profile (Owner / Admin)
  - `DELETE /{id}` – delete seller profile (Owner / Admin)

Create DTO:

```java
public record SellerProfileCreateRequest(
    String name,
    String nip,
    String regon,
    String krs,
    String bankName,
    String bankAccount,
    AddressDto address,
    String defaultCurrency,
    Integer defaultPaymentTermDays
) {}
```

### 3.3 Contractors

Entity: `ContractorEntity`

- belongs to an **organisation**
- fields (simplified):
  - `id`
  - `type` – `COMPANY` or `PERSON`
  - `name`
  - `nipEncrypted`
  - `peselEncrypted`
  - `AddressEmbeddable address`
  - `email`
  - `phone`
  - `favorite`
- API (prefix `/v1/api/contractors`):
  - `GET /` – list contractors, optional `q` filter (Owner / Admin / Accountant)
  - `POST /` – create contractor (Owner / Admin)
  - `PUT /{id}` – update contractor (Owner / Admin)
  - `DELETE /{id}` – delete contractor (Owner / Admin)

Create DTO:

```java
public record ContractorCreateRequest(
    ContractorType type,
    String name,
    String nip,
    String pesel,
    AddressDto address,
    String email,
    String phone,
    Boolean favorite
) {}
```

### 3.4 Invoices

Entities:

- `InvoiceEntity`
- `InvoiceItemEntity`

Important fields in `InvoiceEntity`:

- multi‑tenant:
  - `OrganizationEntity organization`
  - `SellerProfileEntity sellerProfile`
  - `ContractorEntity contractor`
- seller snapshot:
  - `sellerName`
  - `sellerNipEncrypted`
  - `AddressEmbeddable sellerAddress`
  - `sellerBankAccount`
- buyer snapshot:
  - `buyerName`
  - `buyerNipEncrypted`
  - `buyerPeselEncrypted`
  - `AddressEmbeddable buyerAddress`
- invoice data:
  - `number`
  - `issueDate`
  - `saleDate`
  - `dueDate`
  - `paymentMethod` (`PaymentMethod` enum)
  - `currency`
  - `status` (`InvoiceStatus` enum)
  - `totalNet`, `totalVat`, `totalGross`
  - `notes`
  - `reverseCharge`
  - `splitPayment`
- items:
  - `List<InvoiceItemEntity> items`

`InvoiceItemEntity`:

- `InvoiceEntity invoice`
- `description`
- `quantity`
- `unit`
- `netUnitPrice`
- `vatRate`
- `netTotal`
- `vatAmount`
- `grossTotal`

Create DTOs:

```java
public record InvoiceItemCreateRequest(
    String description,
    BigDecimal quantity,
    String unit,
    BigDecimal netUnitPrice,
    String vatRate
) {}

public record InvoiceCreateRequest(
    Long sellerProfileId,
    Long contractorId,
    String buyerNameOverride,
    String buyerNipOverride,
    String buyerPeselOverride,
    LocalDate issueDate,
    LocalDate saleDate,
    LocalDate dueDate,
    PaymentMethod paymentMethod,
    String currency,
    String notes,
    Boolean reverseCharge,
    Boolean splitPayment,
    List<InvoiceItemCreateRequest> items
) {}
```

`InvoiceService`:

- validates ownership of seller profile + contractor
- generates number via `InvoiceNumberGeneratorService`
- snapshots seller + buyer data into `InvoiceEntity`
- calculates
  - `netTotal`, `vatAmount`, `grossTotal` per item
  - invoice totals
- saves invoice and returns `InvoiceResponse`

`InvoicePdfService`:

- uses `com.lowagie.text.*` to render A4 PDF:
  - header with invoice number
  - seller / buyer block with address + NIP + bank account
  - dates
  - items table
  - totals
  - notes section

Controller: `InvoiceController` (`/v1/api/invoices`):

- `POST /` – create invoice (Owner / Admin)
- `GET /` – list invoices for organisation (Owner / Admin / Accountant)
- `GET /{id}` – get invoice details (Owner / Admin / Accountant)
- `GET /{id}/pdf` – download PDF (Owner / Admin / Accountant)
- `DELETE /{id}` – delete invoice (Admin)

All endpoints secured via `@PreAuthorize`.

---

## 4. Import module

Package: `com.softwaremind.invoicedocbackend.importing`

### 4.1 Import DTO model

`InvoiceImportDto` (simplified – actual class is split into helper DTOs):

```java
public record InvoiceImportDto(
    ImportPartyDto seller,
    ImportPartyDto buyer,
    ImportInvoiceMetaDto invoice,
    List<ImportInvoiceItemDto> items,
    ImportExtraDto extra
) {}
```

- `ImportPartyDto` – company / person data with `AddressDto` and optional bank account
- `ImportInvoiceMetaDto` – dates, payment method, currency, etc.
- `ImportInvoiceItemDto` – description, quantity, netUnitPrice, vatRate
- `ImportExtraDto` – `notes`, `reverseCharge`, `splitPayment`

Mapper: `InvoiceImportMapper`

- converts `InvoiceImportDto` into standard `InvoiceCreateRequest`
- maps strings → enums (`PaymentMethod`)
- maps party data to internal DTOs

### 4.2 ImportService

Key responsibilities:

- **JSON**:
  - receives `InvoiceImportDto`
  - checks that selected `SellerProfile` belongs to current organisation
  - ensures a **Contractor** exists (by organisation + buyer name or NIP); if not:
    - creates `ContractorEntity` using `ContractorMapper`
- builds `InvoiceCreateRequest`
- delegates creation to `InvoiceService.createInvoice`


### 4.3 ImportController

Base path: `/api/import`

Endpoints:

- `POST /api/import/json?sellerProfileId={id}` (Owner / Admin)
  - Content‑Type: `application/json`
  - Body: `InvoiceImportDto`‑compatible JSON
  - Returns: `InvoiceResponse` (created invoice)
---

## 5. Frontend – invoices screen

Main file: `src/component/invoices/invoices.js`

Responsibilities:

- load **seller profiles**, **contractors** and **recent invoices** on init
- build a **create invoice** form:
  - selects for seller / buyer
  - date pickers
  - payment / currency / flags
  - dynamic line items with add/remove
  - summary totals
- client‑side validation
- POST `/v1/api/invoices` to create invoice
- list recent invoices with **“PDF”** button that calls:

```http
GET /v1/api/invoices/{id}/pdf
Authorization: Bearer <token>
```

and triggers browser download.

### 5.1 JSON import on UI

Invoices form includes an **“Import from JSON”** panel:

- `<input type="file" accept="application/json">`
- “Import JSON” button
- When clicked:
  1. Reads the file as text
  2. Parses JSON into object
  3. Validates presence of required top‑level keys:
     - `"sellerProfile"`
     - `"contractor"`
     - `"invoice"`
  4. Calls `/api/import/json?sellerProfileId={id}` with this JSON
  5. After success:
     - updates seller profiles list
     - updates contractors list
     - reloads invoices
     - displays success message

---

## 6. JSON import format

This is the main **JSON format** used by frontend and `/api/import/json`.

### 6.1 Top‑level structure

```jsonc
{
  "sellerProfile": {
    "name": "ABC Sp. z o.o.",
    "nip": "1234567890",
    "regon": "987654321",
    "krs": "0000123456",
    "bankName": "PKO BP",
    "bankAccount": "12 3456 7890 1234 5678 9012 3456",
    "address": {
      "street": "Street",
      "buildingNumber": "10A",
      "apartmentNumber": "5",
      "postalCode": "55-555",
      "city": "Warsaw",
      "country": "Poland"
    },
    "defaultCurrency": "PLN",
    "defaultPaymentTermDays": 14
  },

  "contractor": {
    "type": "COMPANY",
    "name": "Comapny sp. z o.o.",
    "nip": "1234567890",
    "pesel": null,
    "address": {
      "street": "Street",
      "buildingNumber": "10A",
      "apartmentNumber": null,
      "postalCode": "44-444",
      "city": "City",
      "country": "Poland"
    },
    "email": "kontakt@me.pl",
    "phone": "+48 123 456 789",
    "favorite": true
  },

  "invoice": {
    "issueDate": "2025-11-27",
    "saleDate": "2025-11-27",
    "dueDate": "2025-12-11",
    "paymentMethod": "BANK_TRANSFER",
    "currency": "PLN",
    "notes": "Notes belonging to invoice",
    "reverseCharge": false,
    "splitPayment": true,
    "items": [
      {
        "description": "Service X",
        "quantity": 1.0,
        "unit": "szt.",
        "netUnitPrice": 5000.00,
        "vatRate": "23"
      },
      {
        "description": "Service Y",
        "quantity": 5.0,
        "unit": "szt.",
        "netUnitPrice": 4000.00,
        "vatRate": "10"
      }
    ]
  }
}

```

All three top‑level objects are **required**.

---

### 6.2 `sellerProfile` object

```jsonc
"sellerProfile": {
  "name": "ABC Sp. z o.o.",          // required
  "nip": "1234567890",               // required, 10 digits
  "regon": "987654321",              // optional (nullable)
  "krs": "0000123456",               // optional (nullable)
  "bankName": "PKO BP",              // required
  "bankAccount": "12 3456 ...",      // required
  "address": {
    "street": "Street",              // required
    "buildingNumber": "10A",         // optional (nullable)
    "apartmentNumber": "5",          // optional (nullable)
    "postalCode": 55-555",           // required
    "city": "Warsaw",                // required
    "country": "Poland"              // required
  },
  "defaultCurrency": "PLN",          // required
  "defaultPaymentTermDays": 14       // required, positive integer
}
```

---

### 6.3 `contractor` object

```jsonc
"contractor": {
  "type": "COMPANY",                 // required: "COMPANY" | "PERSON"
  "name": "Comapny sp. z o.o.",      // required
  "nip": "1234567890",               // required for COMPANY, optional for PERSON
  "pesel": null,                     // optional, PERSON only (can be null)
  "address": {
    "street": "Street",              // required
    "buildingNumber": "10",          // optional (nullable)
    "apartmentNumber": null,         // optional (nullable)
    "postalCode": "44-444",          // required
    "city": "Kleszczów",             // required
    "country": "Poland"              // required
  },
  "email": "kontakt@me.pl",          // optional (nullable)
  "phone": "+48 123 456 789",        // optional (nullable)
  "favorite": true                   // optional, defaults to false
}
```

---

### 6.4 `invoice` object

```jsonc
"invoice": {
  "issueDate": "2025-11-27",         // required, ISO date (yyyy-MM-dd)
  "saleDate": "2025-11-27",          // required
  "dueDate": "2025-12-11",           // required
  "paymentMethod": "BANK_TRANSFER",  // required: BANK_TRANSFER | CASH | CARD | OTHER
  "currency": "PLN",                 // required (3 letters recommended)
  "notes": "Notes",                  // optional (nullable)
  "reverseCharge": false,            // required (boolean)
  "splitPayment": false,             // required (boolean)
  "items": [                         // required, non-empty
    {
      "description": "Service X",    // required
      "quantity": 1.0,               // required, > 0
      "unit": "szt.",                // optional (nullable)
      "netUnitPrice": 5000.00,       // required, >= 0
      "vatRate": "23"                // required, e.g. "23", "8", "0"
    },
    {
      "description": "Service Y",
      "quantity": 5.0,
      "unit": "szt.",
      "netUnitPrice": 4000.00,
      "vatRate": "10"
    }
  ]
}
```

---

### 6.5 Full example JSON

```json
{
  "sellerProfile": {
    "name": "ABC Sp. z o.o.",
    "nip": "1234567890",
    "regon": "",
    "krs": "",
    "bankName": "PKO BP",
    "bankAccount": "12 3456 7890 1234 5678 9012 3456",
    "address": {
      "street": "Street",
      "buildingNumber": "10",
      "apartmentNumber": "5",
      "postalCode": "55-555",
      "city": "Warsaw",
      "country": "Poland"
    },
    "defaultCurrency": "PLN",
    "defaultPaymentTermDays": 14
  },

  "contractor": {
    "type": "COMPANY",
    "name": "Comapany sp. z o.o.",
    "nip": "1234567890",
    "pesel": null,
    "address": {
      "street": "Street",
      "buildingNumber": "10A",
      "apartmentNumber": null,
      "postalCode": "44-444",
      "city": "Warsaw",
      "country": "Poland"
    },
    "email": "",
    "phone": "",
    "favorite": false
  },

  "invoice": {
    "issueDate": "2025-11-27",
    "saleDate": "2025-11-27",
    "dueDate": "2025-12-11",
    "paymentMethod": "BANK_TRANSFER",
    "currency": "PLN",
    "notes": "Notes belonging to invoice",
    "reverseCharge": false,
    "splitPayment": true,
    "items": [
      {
        "description": "Service X",
        "quantity": 1.0,
        "unit": "unit.",
        "netUnitPrice": 5000.00,
        "vatRate": "23"
      },
      {
        "description": "Service Y",
        "quantity": 5.0,
        "unit": "unit.",
        "netUnitPrice": 4000.00,
        "vatRate": "10"
      }
    ]
  }
}
```

---

## 7. Entity‑Relationship (ER) diagram

Below is a simplified ER diagram for the **Organisation → SellerProfile / Contractor → Invoice → InvoiceItem** flow.

```mermaid
erDiagram

    ORGANIZATION {
        bigint id PK
        string name
    }

    USER {
        bigint id PK
        string username
        string email
        string role  // OWNER / ADMIN / ACCOUNTANT
        bigint organization_id FK
    }

    SELLER_PROFILE {
        bigint id PK
        string name
        string nip_encrypted
        string regon
        string krs
        string bank_name
        string bank_account
        string street
        string building_number
        string apartment_number
        string postal_code
        string city
        string country
        string default_currency
        int default_payment_term_days
        bigint organization_id FK
    }

    CONTRACTOR {
        bigint id PK
        string type           // COMPANY / PERSON
        string name
        string nip_encrypted
        string pesel_encrypted
        string street
        string building_number
        string apartment_number
        string postal_code
        string city
        string country
        string email
        string phone
        bool favorite
        bigint organization_id FK
    }

    INVOICE {
        bigint id PK
        string number
        date issue_date
        date sale_date
        date due_date
        string payment_method   // enum
        string currency
        string status           // enum
        decimal total_net
        decimal total_vat
        decimal total_gross
        string notes
        bool reverse_charge
        bool split_payment

        // snapshot fields
        string seller_name
        string seller_nip_encrypted
        string seller_street
        string seller_building_number
        string seller_apartment_number
        string seller_postal_code
        string seller_city
        string seller_country
        string seller_bank_account

        string buyer_name
        string buyer_nip_encrypted
        string buyer_pesel_encrypted
        string buyer_street
        string buyer_building_number
        string buyer_apartment_number
        string buyer_postal_code
        string buyer_city
        string buyer_country

        bigint organization_id FK
        bigint seller_profile_id FK
        bigint contractor_id FK
    }

    INVOICE_ITEM {
        bigint id PK
        string description
        decimal quantity
        string unit
        decimal net_unit_price
        string vat_rate
        decimal net_total
        decimal vat_amount
        decimal gross_total
        bigint invoice_id FK
    }

    ORGANIZATION ||--o{ USER : "has"
    ORGANIZATION ||--o{ SELLER_PROFILE : "has"
    ORGANIZATION ||--o{ CONTRACTOR : "has"
    SELLER_PROFILE ||--o{ INVOICE : "issues"
    CONTRACTOR ||--o{ INVOICE : "is buyer on"
    ORGANIZATION ||--o{ INVOICE : "issues"
    INVOICE ||--o{ INVOICE_ITEM : "contains"
```

This ERD reflects the actual JPA mappings:

- `OrganizationEntity` – one‑to‑many with seller profiles, contractors, invoices.
- `SellerProfileEntity` – one‑to‑many with invoices (`sellerProfile` field).
- `ContractorEntity` – one‑to‑many with invoices (`contractor` field).
- `InvoiceEntity` – one‑to‑many with `InvoiceItemEntity` (`items`).

---

## 8. Running the project


```bash
docker compose up --build
```
---
## 👤 Maintainer

**Milosz Podsiadly**  
📧 [m.podsiadly99@gmail.com](mailto:m.podsiadly99@gmail.com)  
🔗 [GitHub – MiloszPodsiadly](https://github.com/MiloszPodsiadly)

---
## 🪪 License

Licensed under the [MIT License](https://opensource.org/licenses/MIT).