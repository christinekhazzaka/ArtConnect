# ArtConnect Pro Report

## Step 1: Understanding ArtConnect and Defining the Functional Scope

### Objective

The objective of this first step is to understand the provided ArtConnect Pro application, identify its main screens and features, define the target users, and describe the functional scope that will be supported by the database.

ArtConnect Pro is a JavaFX desktop application for managing a local art community. It helps organize information about artists, artworks, galleries, exhibitions, workshops, and community members.

---

## 1. Exploring the Provided Application

### Application Type

ArtConnect Pro is a Java application built with JavaFX. It uses a layered architecture:

- User interface layer: JavaFX screens and controllers
- Service layer: business operations
- DAO layer: data access interfaces
- Persistence layer: JDBC implementations
- Model layer: application entities

The original project skeleton was designed to run first with dummy data. In the current version, the service provider is connected to JDBC services, so the application is prepared to work with a MySQL database.

### Main Screens

The application contains the following main tabs:

| Screen | Purpose |
|---|---|
| Discover | Shows featured exhibitions and upcoming workshops |
| Artists | Manages artists and their information |
| Artworks | Manages artworks created by artists |
| Galleries | Manages galleries and their details |
| Exhibitions | Manages exhibitions hosted in galleries |
| Workshops | Manages workshops taught by artists |
| Community | Manages community members |

---

## 2. Functional Analysis

### Main Features Seen in the Interface

From the user's point of view, the application allows:

- Viewing featured exhibitions and workshops.
- Searching in the Discover page.
- Adding, updating, deleting, and searching artists.
- Filtering artists by discipline.
- Adding, updating, deleting, and searching artworks.
- Linking an artwork to an artist.
- Adding, updating, deleting, and searching galleries.
- Adding, updating, deleting, and searching exhibitions.
- Linking an exhibition to a gallery.
- Adding, updating, deleting, and searching workshops.
- Linking a workshop to an artist instructor.
- Adding, updating, deleting, and searching community members.
- Managing simple membership types such as BASIC and PREMIUM.

### User Roles

The application does not currently include authentication, but we can define simple roles conceptually.

| Role | Description |
|---|---|
| Organizer / Administrator | Manages artists, artworks, galleries, exhibitions, workshops, and members |
| Visitor / Community Member | Views exhibitions and workshops, and can be registered as a community member |
| Artist | Has a profile, creates artworks, and may teach workshops |

### Functional Scope for the Database

The database should store the main data used by the application:

- Artists
- Artworks
- Galleries
- Exhibitions
- Workshops
- Community members
- Bookings for workshops
- Reviews for artworks
- Disciplines and artwork tags

Some features are already visible in the interface, while others are planned because they exist in the model but are not fully available as screens.

### Feature Legend

| Symbol | Meaning |
|---|---|
| Present | Already available in the interface |
| Planned | Useful future feature or partially represented in the model |

### Use Case Diagram

```mermaid
flowchart LR
    Organizer[Organizer / Administrator]
    Visitor[Visitor / Community Member]
    Artist[Artist]

    subgraph ArtConnect[ArtConnect Pro]
        UC1[View featured exhibitions and workshops<br/>Present]
        UC2[Manage artists<br/>Present]
        UC3[Manage artworks<br/>Present]
        UC4[Manage galleries<br/>Present]
        UC5[Manage exhibitions<br/>Present]
        UC6[Manage workshops<br/>Present]
        UC7[Manage community members<br/>Present]
        UC8[Search and filter information<br/>Present]
        UC9[Book a workshop<br/>Planned]
        UC10[Review an artwork<br/>Planned]
    end

    Organizer --> UC2
    Organizer --> UC3
    Organizer --> UC4
    Organizer --> UC5
    Organizer --> UC6
    Organizer --> UC7
    Organizer --> UC8

    Visitor --> UC1
    Visitor --> UC8
    Visitor --> UC9
    Visitor --> UC10

    Artist --> UC2
    Artist --> UC3
    Artist --> UC6
```

---

## 3. Static Design

### General Architecture

The project follows a layered structure. The user interacts with JavaFX screens. Controllers call services. Services use DAO interfaces. JDBC DAO classes communicate with the database.

```mermaid
flowchart TD
    UI[JavaFX FXML Views]
    Controller[UI Controllers]
    Service[Service Interfaces]
    ServiceImpl[JDBC Service Implementations]
    DAO[DAO Interfaces]
    JdbcDAO[JDBC DAO Implementations]
    DB[(MySQL Database)]
    Model[Model Classes]

    UI --> Controller
    Controller --> Service
    Service --> ServiceImpl
    ServiceImpl --> DAO
    DAO --> JdbcDAO
    JdbcDAO --> DB
    Controller --> Model
    ServiceImpl --> Model
    JdbcDAO --> Model
```

### Main Model Class Diagram

```mermaid
classDiagram
    class Artist {
        String name
        String bio
        Integer birthYear
        String contactEmail
        String city
        boolean isActive
    }

    class Artwork {
        String title
        Integer creationYear
        String type
        String medium
        double price
        Status status
    }

    class Gallery {
        String name
        String address
        String ownerName
        String openingHours
        double rating
    }

    class Exhibition {
        String title
        LocalDate startDate
        LocalDate endDate
        String description
        int capacity
    }

    class Workshop {
        String title
        LocalDateTime date
        int durationMinutes
        int maxParticipants
        double price
        String location
        String level
    }

    class CommunityMember {
        String name
        String email
        Integer birthYear
        String phone
        String city
        String membershipType
    }

    class Booking {
        LocalDateTime bookingDate
        String paymentStatus
    }

    class Review {
        int rating
        String comment
        LocalDate reviewDate
    }

    class Discipline {
        String name
    }

    class ArtworkTag {
        String name
    }

    Artist "1" --> "many" Artwork : creates
    Artist "1" --> "many" Workshop : teaches
    Artist "many" --> "many" Discipline : has
    Artwork "many" --> "many" ArtworkTag : tagged with
    Gallery "1" --> "many" Exhibition : hosts
    Exhibition "many" --> "many" Artwork : displays
    CommunityMember "1" --> "many" Booking : makes
    Booking "many" --> "1" Workshop : reserves
    CommunityMember "1" --> "many" Review : writes
    Review "many" --> "1" Artwork : reviews
```

### Service and DAO Structure

```mermaid
classDiagram
    class ArtistService
    class ArtworkService
    class GalleryService
    class ExhibitionService
    class WorkshopService
    class CommunityService

    class ArtistDao
    class ArtworkDao
    class GalleryDao
    class ExhibitionDao
    class WorkshopDao
    class CommunityMemberDao

    class JdbcArtistDao
    class JdbcArtworkDao
    class JdbcGalleryDao
    class JdbcExhibitionDao
    class JdbcWorkshopDao
    class JdbcCommunityMemberDao

    ArtistService --> ArtistDao
    ArtworkService --> ArtworkDao
    GalleryService --> GalleryDao
    ExhibitionService --> ExhibitionDao
    WorkshopService --> WorkshopDao
    CommunityService --> CommunityMemberDao

    JdbcArtistDao ..|> ArtistDao
    JdbcArtworkDao ..|> ArtworkDao
    JdbcGalleryDao ..|> GalleryDao
    JdbcExhibitionDao ..|> ExhibitionDao
    JdbcWorkshopDao ..|> WorkshopDao
    JdbcCommunityMemberDao ..|> CommunityMemberDao
```

---

## Deliverable for Step 1

This section provides:

- A simple description of the application.
- The list of main screens.
- The list of main features.
- The main user roles.
- A use case diagram.
- A simple architecture diagram.
- Class diagrams for the model and persistence structure.
