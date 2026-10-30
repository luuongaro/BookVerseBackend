# Fellow Readers

Fellow Readers lets authenticated users discover other active users, visit their
profiles, and select a user when adding a reading-group member.

## Profile API

All routes require authentication, including for ordinary `ROLE_USER` accounts.

| Method | Route | Response |
| --- | --- | --- |
| GET | `/api/users/profiles?username=Sarah&page=0&size=12` | Page of `{ idExternal, username }` |
| GET | `/api/users/profiles/{id}` | `{ idExternal, username }` |
| GET | `/api/users/profiles/{id}/stories?page=0&size=12` | Page of visible story summaries |

Username search is case insensitive, accepts an optional leading `@`, and matches
literal substrings. An empty search returns active users in username order.
Filtering happens before pagination; page size is bounded to 1–24.

Inactive, suspended, and banned users do not appear in discovery and their profiles
return 404. Profile responses contain no email, role, subscription, password, or
reading-history data. Hidden and deleted stories are filtered before pagination;
profile story summaries do not include chapter contents or draft chapter counts.
Existing account-management routes keep their authorization rules.

## Adding group members

`POST /api/group-members/group/{groupId}` now accepts either:

```json
{ "userId": "the-selected-users-external-uuid" }
```

or the existing email request. Supply exactly one identifier. The frontend
displays usernames and sends the selected UUID; knowing someone's email is not
required. Existing creator/admin/moderator permissions and active-account,
duplicate-membership, removal, and reactivation rules still apply.

## Rollout and verification

Deploy this backend before enabling the matching frontend. No schema migration is
required. The frontend uses `/users` for discovery and `/users/{id}` for profiles.

New tests cover real repository search/pagination, profile privacy, authenticated
access, target validation, email compatibility, and group-membership restrictions.
The existing develop test suite has unrelated compilation failures in older group
and review tests; validate the new tests in isolation until those contracts are
updated. The application itself compiles with Java 24.
