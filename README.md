# Media Asset Management System

> A newsroom media tracking and asset management system built with Spring Boot, PostgreSQL, Flyway, and vanilla JavaScript.

🎯 **Built to replace spreadsheet-based media tracking with a centralized system for managing, searching, and maintaining newsroom footage metadata.**

The system manages news footage records and their relationships with reporters, cameramen, locations, importing staff, and ingesting staff. It provides JWT-based authentication, role-based authorization, PostgreSQL full-text search with Amharic support, optimistic locking for concurrent edits, structured validation errors, and a lightweight frontend served directly by Spring Boot.

## ✨ Features

* 📰 News footage metadata management
* 👥 Reporter, cameraman, location, and staff management
* 🔐 Stateless JWT authentication with Spring Security
* 🛡️ Role-based access control with Admin, Staff, and Viewer roles
* 🔎 Dynamic multi-filter news search
* 🔤 PostgreSQL full-text search for Amharic news titles
* ⚡ JPA optimistic locking for concurrent edits
* ✅ DTO-level and custom cross-field validation
* 🚨 Centralized and field-specific API error handling
* 📄 Pagination for search results
* 📊 Import and ingest activity summaries
* 🗃️ Flyway-managed database migrations
* 🖥️ Vanilla JavaScript frontend with no build tooling or frontend framework

## 🔐 Authentication & Authorization

Authentication is implemented using stateless JWT tokens through Spring Security, with BCrypt used for password hashing.

The application defines three roles:

| Role            | Access                                                         |
| --------------- | -------------------------------------------------------------- |
| 👑 **Admin**    | Manage reference data, staff, users, and system administration |
| 🧑‍💼 **Staff** | Create, edit, delete, and search news records                  |
| 👁️ **Viewer**  | Read-only news search and retrieval                            |

Authorization is enforced server-side using method-level `@PreAuthorize` checks.

The frontend also decodes the JWT to provide role-aware UI behavior, but frontend restrictions are **not treated as a security boundary**. The backend independently enforces every permission.

## 🔎 Search

The search system combines structured filtering with PostgreSQL full-text search.

Supported filters include:

* Reporter
* Cameraman
* Location
* Importing staff
* Ingesting staff
* News date
* Date range
* Active/inactive status
* Domestic/abroad location
* News title

### Full-Text Search

News titles are indexed using PostgreSQL `tsvector` and a GIN index.

Amharic content uses PostgreSQL's `simple` dictionary because the standard PostgreSQL language configurations do not provide an Amharic-specific text search configuration.

Search queries are processed using `websearch_to_tsquery`, allowing the frontend to provide an AND / OR / NOT query builder without requiring users to know PostgreSQL's raw search syntax.

## ⚡ Concurrency Control

The system uses JPA optimistic locking rather than pessimistic database locks.

The following entities use `@Version` fields:

* News
* Reporter
* Cameraman
* Location
* StaffMember

If two users edit the same record concurrently, the second update detects that the record version has changed and is rejected rather than silently overwriting the first user's changes.

The frontend surfaces this as a clear conflict message instead of exposing an opaque database exception.

## 🗄️ Database Design

PostgreSQL is used as the primary relational database.

Many-to-many relationships between news records and reporters, cameramen, and locations are represented using normalized junction tables with foreign-key constraints and appropriate cascade behavior.

The database also uses targeted indexes for frequently queried fields, including:

* Reporter relationships
* Cameraman relationships
* Location relationships
* Staff attribution
* News date

Schema evolution is managed through versioned Flyway migrations.

## 🖥️ Frontend

The frontend is intentionally dependency-free.

It uses:

* HTML
* CSS
* Vanilla JavaScript
* Fetch API

There is no React, Angular, Vue, Node.js build pipeline, bundler, or frontend package dependency tree.

The frontend is served directly from the Spring Boot application.

The interface includes:

* 🔑 JWT login
* 🔎 News search and filtering
* 📝 News creation and editing
* 🗑️ News deletion
* ⚡ Optimistic-lock conflict handling
* 📊 Import/ingest summaries
* 👥 Reporter management
* 📹 Cameraman management
* 📍 Location management
* 🧑‍💼 Staff management
* 🔐 Role-aware UI controls

## 🚨 Validation & Error Handling

The API uses standard Bean Validation together with domain-specific validation.

For example, a custom cross-field constraint requires every news item to identify at least one reporter or cameraman.

A centralized exception-handling layer converts application errors into consistent JSON responses, including:

* Validation failures
* Not-found errors
* Authentication failures
* Authorization failures
* Optimistic-lock conflicts
* Malformed JSON
* Invalid field types

The API can therefore return field-specific errors that the frontend can display directly against the relevant form input.

## 🏗️ Architecture

```text
┌─────────────────────────────────────┐
│          Vanilla JS Frontend        │
│             HTML / CSS / JS         │
└──────────────────┬──────────────────┘
                   │ HTTP / JSON
                   ▼
┌─────────────────────────────────────┐
│          Spring Security            │
│          JWT Authentication         │
└──────────────────┬──────────────────┘
                   ▼
┌─────────────────────────────────────┐
│             REST API                │
│     Controllers / DTOs / Errors     │
└──────────────────┬──────────────────┘
                   ▼
┌─────────────────────────────────────┐
│          Service Layer              │
│       Business Logic / Rules        │
└──────────────────┬──────────────────┘
                   ▼
┌─────────────────────────────────────┐
│        Data Access Layer            │
│   Spring Data JPA / Specifications  │
└──────────────────┬──────────────────┘
                   ▼
┌─────────────────────────────────────┐
│             PostgreSQL              │
│ Constraints / Indexes / FTS / Lock  │
└─────────────────────────────────────┘
```

## 🛠️ Tech Stack

**Backend**

* ☕ Java 21
* 🌱 Spring Boot
* 🔐 Spring Security
* 🗃️ Spring Data JPA
* 🔄 Hibernate
* ✅ Jakarta Bean Validation
* 🎫 JWT
* 🔒 BCrypt

**Database**

* 🐘 PostgreSQL
* 🔎 PostgreSQL Full-Text Search
* 📇 GIN indexes
* 🔗 Foreign keys and junction tables
* ⚡ Optimistic locking

**Database Migrations**

* 🦋 Flyway

**Frontend**

* 🌐 HTML5
* 🎨 CSS3
* ⚙️ Vanilla JavaScript
* 📡 Fetch API

**Development**

* 📦 Maven
* 🐳 Docker
* 🌿 Git

## 🗺️ Roadmap

* Application user management
* Role management
* Expanded administrative controls
* Additional integration and API testing
* Audit history
* Monitoring and observability
* CI/CD
* Production deployment configuration
