# JWT Security Configuration

## Token Generation
- Access Token: 15 minutes TTL
- Refresh Token: 7 days TTL
- Secret: Base64-encoded key (JWT_SECRET env var)

## Token Payload
```json
{
  "sub": "user-uuid",
  "institutionId": "institution-uuid",
  "role": "TEACHER",
  "iat": 1234567890,
  "exp": 1234568790
}
```

## Security Filters
1. `JwtAuthenticationFilter` — Validates JWT, sets SecurityContext
2. `JwtRequestAttributeFilter` — Extracts userId, institutionId, role to request attributes

## Role Hierarchy
- INSTITUTION_ADMIN → full access within institution
- TEACHER → manage classes, students, assessments
- PARENT → view children's data
- STUDENT/OTHER_LEARNER → own data only
- LEARNER → NFE programs
- PROVIDER_ADMIN → NFE provider management
- PROVIDER_STAFF → NFE operations
