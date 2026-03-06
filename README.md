# 📌 La Selección - KickTime Backend

KickTime is a web platform for managing university soccer tournaments, using real-time availability and artificial intelligence to optimize match scheduling.
## 👤 Developers

- Sebastián Enrique Barros Barros
- Lina Janeth Sanchez Forero
- Julián Santiago Ramírez Urueña

## 📑 Content Table

1. [Project Architecture](#-project-architecture)
    - [Hexagonal Structure](#-hexagonal-structure)
2. [API Documentation](#-api-endpoints)
    - [Endpoints](#-api-endpoints)
    - [HTTP Status Codes](#-http-status-codes)
3. [Input & Output Data](#data-input--output)
4. [Technologies](#technologies)
    - [Backend & Core](#backend--core)
    - [Database](#database)
    - [DevOps & Infrastructure](#devops--infrastructure)
    - [CI/CD & Quality Assurance](#cicd--quality-assurance)
    - [Documentation & Testing](#documentation--testing)
    - [Design](#design)
    - [Communication & Project Management](#communication--project-management)
5. [Branch Strategy](#-branches-strategy--structure)
6. [Naming Conventions](#️-naming-conventions)
7. [System Architecture & Design](#-system-architecture--design)
8. [Getting Started](#-getting-started)
9. [Testing](#-testing)

---

## 🏢 Project Architecture

Kicktime follows a **Layered Monolithic Architecture**, where the entire system runs as a single deployable application but is internally organized into logical layers, each with a specific responsibility that communicates only with adjacent layers.

This approach keeps deployment simple while maintaining a clear separation of concerns inside the codebase.

- 🌐 **API Layer:** REST Controllers that handle incoming HTTP requests and expose the application endpoints.
- 🧠 **Service Layer:** Contains the business logic and orchestrates the application workflows.
- 📦 **Domain Layer:** Defines the core entities, DTOs, and enums that model the problem space.
- 🗄️ **Persistence Layer:** Manages database access through repository interfaces, abstracting data storage.
- ⚙️ **Configuration Layer:** Centralizes security settings and application-wide configuration.
- ✔️ **Simple Deployment:** The entire system runs as a single application, reducing operational complexity.
- ✔️ **Separation of Concerns:** Distinct boundaries between each layer prevent logic from leaking across responsibilities.
- ✔️ **Maintainability:** Each layer can be updated or refactored independently without affecting others.
- ✔️ **Modularity:** Functional modules keep related logic grouped, making the codebase easier to navigate and extend.

### 📂 Project Structure
```
📦 kicktime
│
├── 📂 backend
│   ├── 📂 api
│   │   ├── 📂 authentication
│   │   ├── 📂 match
│   │   ├── 📂 reservation
│   │   └── 📂 standings
│   ├── 📂 config
│   ├── 📂 domain
│   │   └── 📂 model
│   │       ├── 📂 dto
│   │       │   ├── 📂 request
│   │       │   └── 📂 response
│   │       └── 📂 enums
│   ├── 📂 services
│   │   ├── 📂 authentication
│   │   ├── 📂 match
│   │   ├── 📂 reservation
│   │   └── 📂 standings
│   └── 📂 repository
│
├── 📂 resources
├── 📂 test
└── 📂 target
```

## 📡 API Endpoints

### Data input & output

### 📟 HTTP Status Codes

Common status codes returned by the API.

| Code | Status | Description |
| :--- | :--- | :--- |
| `200` | **OK** | Request processed successfully. |
| `201` | **Created** | Resource (Route/Tracking) created successfully. |
| `400` | **Bad Request** | Invalid coordinates or missing parameters. |
| `401` | **Unauthorized** | Missing or invalid JWT token. |
| `404` | **Not Found** | Route or Trip ID does not exist. |
| `500` | **Internal Server Error** | Unexpected error (e.g., Google Maps API failure).

## Technologies

### Backend & Core

![Java](https://img.shields.io/badge/java-%23ED8B00.svg?style=for-the-badge&logo=openjdk&logoColor=white)
![Spring Boot](https://img.shields.io/badge/spring-%236DB33F.svg?style=for-the-badge&logo=spring&logoColor=white)
![Maven](https://img.shields.io/badge/Maven-C71A36?style=for-the-badge&logo=apache-maven&logoColor=white)

### Database

![Postgres](https://img.shields.io/badge/postgres-%23316192.svg?style=for-the-badge&logo=postgresql&logoColor=white)

### DevOps & Infrastructure

![Docker](https://img.shields.io/badge/docker-%230db7ed.svg?style=for-the-badge&logo=docker&logoColor=white)
![Azure](https://img.shields.io/badge/azure-%230072C6.svg?style=for-the-badge&logo=microsoftazure&logoColor=white)
![Vercel](https://img.shields.io/badge/vercel-%23000000.svg?style=for-the-badge&logo=vercel&logoColor=white)

### CI/CD & Quality Assurance

![GitHub Actions](https://img.shields.io/badge/github%20actions-%232671E5.svg?style=for-the-badge&logo=githubactions&logoColor=white)
![SonarQube](https://img.shields.io/badge/SonarQube-4E9BCD?style=for-the-badge&logo=sonarqube&logoColor=white)
![JaCoCo](https://img.shields.io/badge/JaCoCo-Coverage-green?style=for-the-badge)

### Documentation & Testing

![Swagger](https://img.shields.io/badge/-Swagger-%23Clojure?style=for-the-badge&logo=swagger&logoColor=white)
![Postman](https://img.shields.io/badge/Postman-FF6C37?style=for-the-badge&logo=postman&logoColor=white)

### Design

![Figma](https://img.shields.io/badge/figma-%23F24E1E.svg?style=for-the-badge&logo=figma&logoColor=white)

### Comunication & Project Management

![Microsoft Teams](https://img.shields.io/badge/Microsoft%20Teams-6264A7?style=for-the-badge&logo=microsoftteams&logoColor=white)
![Azure DevOps](https://img.shields.io/badge/Azure%20DevOps-0078D7?style=for-the-badge&logo=azuredevops&logoColor=white)

## 🌿 Branches Strategy & Structure

This module follows a strict branching strategy based on Gitflow to ensure the ordered versioning,code quality and continous integration.

| **Branch**              | **Purpose**                                      | **Receives from**        | **Sends to**         | **Notes**                                    |
| ----------------------- | ------------------------------------------------ | ------------------------ | -------------------- | -------------------------------------------- |
| `main`                  | 🏁 Stable code for preproduction or Production   | `release/*`, `hotfix/*`  | 🚀 Production        | 🔐 Protected with PR and successful CI       |
| `develop`               | 🧪 Main developing branch                        | `feature/*`              | `release/*`          | ↗️ Base for continuous deployment             |
| `feature/*`             | ✨ New functions or refactors to be implemented  | `develop`                | `develop`            | 🧹 Deleted after merge to develop            |
| `release/*`             | 📦 Release preparation & final polish            | `develop`                | `main` and `develop` | 🧪 Includes final QA. No new features added  |
| `bugfix/*` or `hotfix/*`| 🛠️ Critical fixes for production                 | `main`                   | `main` and `develop` | ⚡ Urgent patches. Highest priority          |

## 🏷️ Naming Conventions
## 🌿 Branch Naming
### ✨ Feature Branches
Used for new features or non-critical improvements.
**Format:**
`feature/[shortDescription]`
**Examples:**
- `feature/authenticationModule`
- `feature/securityService`

**Rules:**
* 🧩 **Case:** strictly *camelCase* (lowercase with hyphens).
* ✍️ **Descriptive:** Short and meaningful description.

### 📦 Release Branches
Used for preparing a new production release. Follows [Semantic Versioning](https://semver.org/).
**Format:**
`release/v[major].[minor].[patch]`
**Examples:**
- `release/v1.0.0`
- `release/v1.1.0-beta`

### 🚑 Hotfix Branches
Used for urgent fixes in the production environment.
**Format:**
`hotfix/[shortDescription]`
**Examples:**
- `hotfix/fixTokenExpiration`
- `hotfix/securityPatch`

### ✨ Commiting Structure

Commits follow this structure depending on the type of work:

**Feature**
`Feature - <FeatureName>: <Short description>`
> Example: `Feature - User Authentication: Implement JWT login flow`

**User Story**
`US - <HUCode> - <HUName>: <Short description>`
> Example: `US - 012 - User Profile Management: Add avatar upload endpoint`