# Pelycan API (Spring Boot 21)

Backend officiel Pelycan — remplace `Pelycan-servers` (Node).

## Stack

- Java 21, Spring Boot 3.4
- PostgreSQL (Flyway, schéma `pelycan`)
- Redis (cache actualités publiques)
- S3 (URLs présignées upload)
- Déploiement : image Docker → AWS App Runner ou ECS

## Démarrage local

1. Java 21 + Maven 3.9+
2. Infra locale :

```bash
cd pelycan-api
docker compose up -d
```

3. Variables (voir `.env.example`), puis :

```bash
mvn spring-boot:run
```

API : `http://localhost:4000`

- Health : `GET /actuator/health`
- Login admin seed : `admin@pelycan.local` / `Admin123!` (à changer en prod)

### Modules v0.3

| Domaine | Préfixe API |
|---------|-------------|
| Demandes logement | `/api/demandes`, `/api/logements/{id}/demandes` |
| Sécurité (Flutter) | `/api/security/*` (PATCH resolve/cancel, alert-contacts) |
| Contacts urgence | `/api/emergency-contacts` |
| Carte danger (admin) | `/api/alerts` |
| Infos médicales | `/api/user-alerts/medical-info` |
| FCM admins | `POST /api/notifications/register-token` + `FIREBASE_CREDENTIALS_PATH` |
| Demandes bailleur | `/api/demandes-ajout-logement` |

Variables Agora : `AGORA_APP_ID`, `AGORA_APP_CERTIFICATE`.  
Firebase (optionnel) : `FIREBASE_CREDENTIALS_PATH`, `FIREBASE_ADMIN_NOTIFY=true`.

## Endpoints (v0.3 — extraits)

| Méthode | Chemin | Auth |
|---------|--------|------|
| POST | `/api/demandes` | user/pro/admin |
| GET | `/api/demandes/user/{userId}` | JWT |
| PUT | `/api/demandes/{id}/status` | admin/pro/bailleur |
| GET | `/api/logements/{id}/demandes` | pro/admin/bailleur |
| POST | `/api/security/alert-contacts` | JWT |
| PATCH | `/api/security/user-alerts/{id}/resolve\|cancel` | JWT |
| CRUD | `/api/emergency-contacts` | JWT |
| POST | `/api/alerts/create`, `/voice-detection` | JWT |
| GET | `/api/alerts`, `/api/alerts/active` | admin |
| GET/PUT | `/api/user-alerts/medical-info` | JWT |
| PUT | `/api/user-alerts/{id}/location` | JWT |
| POST | `/api/notifications/register-token` | JWT (`fcmToken`, `platform` optionnel) |
| CRUD | `/api/demandes-ajout-logement` | JWT / admin pour liste & statut |

## Endpoints (v0.2)

| Méthode | Chemin | Auth |
|---------|--------|------|
| POST | `/api/auth/register` | — |
| POST | `/api/auth/login` | — |
| GET | `/api/auth/verify` | Bearer |
| GET | `/api/actualites` | — |
| GET/POST/PUT/DELETE | `/api/actualites/manage`, `/api/actualites/{id}` | admin |
| POST | `/api/uploads/presign` | JWT (S3 configuré) |

Contrat JSON aligné sur l’ancien Node (`success`, `data`, JWT claim `userId`).

## Clients

- **Flutter** : `--dart-define=API_URL=http://localhost:4000/api`
- **Pelycan-web** : `API_URL=http://localhost:4000/api`

## AWS (prod)

- **RDS** : `DATABASE_URL=jdbc:postgresql://...`
- **ElastiCache** : `REDIS_HOST=...`
- **S3** : `AWS_S3_BUCKET=...` + rôle IAM
- **Secrets Manager** : `JWT_SECRET`, mot de passe RDS
- **Firebase / Agora** : FCM admin sur `trigger-admin` si credentials Firebase présents ; Agora pour live et écoute détresse

## Build Docker

```bash
mvn -DskipTests package
docker build -t pelycan-api .
```

## Ancien code

`Pelycan-servers/` reste en archive de référence ; ne plus déployer. Domaines restants (groupes de parole complets, demandes ajout logement, SMS réels) à porter progressivement dans ce projet.
