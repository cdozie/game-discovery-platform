# Games DB Network

A full-stack video game catalog and social platform where users can discover games, build wishlists, rate and review titles, and submit new entries to a community-driven database. Built with Spring Boot and React.

## Features

**Game Catalog**
- Browse a searchable database of games aggregated from PlayStation Store, IGDB, and user submissions
- Full-text and fuzzy search with filters for platform, category, publisher, and source
- Game detail pages with cover art, metadata, ratings, and community discussion

**Wishlists & Tracking**
- Add games to your personal list with platform selection
- Track play status — Plan to Play, Playing, Completed, Dropped
- Rate games on a 0-10 scale

**Community**
- Threaded comment discussions on game pages
- Submit new games for review by moderators
- User profiles with display name, bio, avatar, and favorite platform

**Admin Portal**
- Review and approve/reject user-submitted games
- Moderate users with temporary or permanent bans
- Full CRUD management of the game catalog
- Set spotlight/featured games on the homepage
- Monitor catalog sync health and trigger manual syncs
- Audit log of all admin actions
- System job queue management

**Catalog Sync**
- Automated daily sync from PlayStation Store API
- Optional IGDB integration for PC/Xbox/Switch titles
- Multi-source deduplication with normalized title keys
- Batch processing with checkpoint-based resume

## Tech Stack

| Layer | Technology |
|-------|------------|
| Backend | Spring Boot 3.1.1, Java 21 |
| Frontend | React 18, TypeScript, Material UI 5 |
| Database | PostgreSQL with Flyway migrations |
| Cache | Redis (game interaction buffering, distributed locks) |
| Auth | JWT with HttpOnly cookies, Spring Security |
| APIs | PlayStation Store, IGDB (via Twitch OAuth) |

## Project Structure

```
├── src/main/java/              # Spring Boot backend
│   └── com/madre/excelweb/
│       ├── rest/               # REST controllers
│       ├── service/            # Business logic & sync services
│       ├── jpa/                # Entities & repositories
│       ├── security/           # JWT auth & Spring Security config
│       └── config/             # Bean configuration
├── src/main/resources/
│   ├── application.yml         # Default configuration
│   ├── application-local.yml   # Local dev overrides
│   └── db/migration/           # Flyway SQL migrations (V1–V28)
└── my-app/                     # React frontend
    └── src/
        ├── MainComponents/     # Pages (Home, Search, Game, Admin, etc.)
        ├── SignInComponents/   # Login & registration forms
        ├── GeneralComponents/  # Shared components & types
        └── api/                # Axios API client
```

## Getting Started

### Prerequisites

- Java 21
- Node.js & npm
- PostgreSQL 15+
- Redis

### 1. Configure credentials

Create a `.env` file in the project root (gitignored):

```env
SPRING_DATASOURCE_USERNAME=your_db_user
SPRING_DATASOURCE_PASSWORD=your_db_password
```

Or export them in your shell profile:

```bash
export SPRING_DATASOURCE_USERNAME=your_db_user
export SPRING_DATASOURCE_PASSWORD='your_db_password'
```

### 2. Start the backend

```bash
SPRING_PROFILES_ACTIVE=local ./gradlew bootRun
```

The backend runs on `http://localhost:8081`.

By default the local profile connects to a PostgreSQL database called `projects` on `localhost:5432`. Override with `SPRING_DATASOURCE_URL` if yours is different.

### 3. Start the frontend

```bash
cd my-app
npm install
npm start
```

The frontend runs on `http://localhost:3000/api/` and proxies API requests to the backend. If port 3000 is busy, use `PORT=3001 npm start`.

### 4. Create an admin user

Register a user through the UI, then set the bootstrap env var and restart the backend:

```bash
WEB_ADMIN_BOOTSTRAP_USERNAMES=your_username SPRING_PROFILES_ACTIVE=local ./gradlew bootRun
```

Matching accounts are promoted to `ADMIN` on startup.

## Configuration

All properties can be overridden via environment variables. Key settings in `application.yml`:

| Variable | Default | Description |
|----------|---------|-------------|
| `SERVER_PORT` | `8081` | Backend port |
| `SPRING_DATASOURCE_URL` | `jdbc:postgresql://localhost:5432/game_db` | Database URL |
| `SPRING_DATASOURCE_USERNAME` | — | Database user |
| `SPRING_DATASOURCE_PASSWORD` | — | Database password |
| `JWT_SECRET_KEY` | `change-me-...` | JWT signing key (**change in production**) |
| `JWT_EXPIRATION_MS` | `6000000` | Token expiration (ms) |
| `SEND_VERIFICATION_EMAIL` | `true` | Enable email verification delivery |
| `SPRING_MAIL_USERNAME` | — | SMTP username (Gmail) |
| `SPRING_MAIL_PASSWORD` | — | SMTP password |
| `PSN_SYNC_ENABLED` | `true` | PlayStation Store catalog sync |
| `IGDB_SYNC_ENABLED` | `false` | IGDB catalog sync |
| `IGDB_CLIENT_ID` | — | Twitch/IGDB client ID |
| `IGDB_CLIENT_SECRET` | — | Twitch/IGDB client secret |
| `MEDIA_STORAGE_PATH` | `./var/media` | Local file upload storage |

### Local profile notes

The `local` profile (`SPRING_PROFILES_ACTIVE=local`) differs from the default:
- Email verification is **disabled** — the register response returns a clickable verification URL directly
- PSN sync auto-starts on boot if the catalog is empty
- Frontend domain defaults to `http://localhost:3001`

## API Overview

| Endpoint | Description |
|----------|-------------|
| `POST /api/auth/register` | Register a new account |
| `POST /api/auth/login` | Log in (sets JWT cookie) |
| `GET /api/auth/games/random` | Random game selection |
| `GET /api/auth/games/search-page` | Paginated search with filters |
| `GET /api/auth/games/{id}` | Game details |
| `POST /api/auth/wishlist` | Add game to wishlist |
| `POST /api/auth/games/{id}/rating` | Rate a game |
| `GET /api/auth/games/{id}/comments` | Game comments |
| `POST /api/catalog/sync` | Trigger catalog sync (auth required) |
| `POST /api/catalog/submissions/` | Submit a new game |
| `GET /api/admin/dashboard` | Admin dashboard (admin only) |
| `PUT /api/account/profile` | Update user profile |

## License

This project is not currently licensed for redistribution.
