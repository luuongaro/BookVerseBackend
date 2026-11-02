# Reader profiles (Lectores)

Lectores lets authenticated users discover other active users, visit their
profiles, and select a user when adding a reading-group member.

## Profile API

All routes require authentication, including for ordinary `ROLE_USER` accounts.

| Method | Route | Response |
| --- | --- | --- |
| GET | `/api/users/profiles?username=Sarah&page=0&size=12` | Page of `{ idExternal, username }` |
| GET | `/api/users/profiles/{id}` | Public user identity |
| GET | `/api/users/profiles/{id}/stories?page=0&size=12` | Page of visible story summaries |
| GET | `/api/users/profiles/{id}/activity` | Want-to-read, in-progress, finished, created-story and active-group counts |
| GET | `/api/users/profiles/{id}/appearance` | Cover and avatar selection |
| PUT | `/api/users/profiles/me/appearance` | Update the authenticated user's cover and/or avatar |
| GET | `/api/users/profiles/{id}/favorites` | Page of visible favorite stories |
| GET | `/api/users/profiles/me/favorites/{storyId}` | Whether the authenticated user has favorited the story |
| PUT / DELETE | `/api/users/profiles/me/favorites/{storyId}` | Add/remove the authenticated user's favorite |

Username search is case insensitive, accepts an optional leading `@`, and matches
literal substrings. An empty search returns active users in username order.
The authenticated user is excluded before filtering, counting and pagination;
this also applies when searching by their own username. Administrative user lists
and direct access to one's own profile are unchanged.
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

Run `docs/sql/fellow-readers-profile.sql` before deploying the backend and matching
frontend. It creates appearance/favorite tables and enables RLS for direct Supabase
access; authenticated application access goes through the Java backend using its
database owner connection. Existing users, stories and reading states are preserved.
The frontend uses `/users` for discovery and `/users/{id}` for profiles.

Avatar and cover indexes are validated from 0 to 5. Their frontend asset ordering
must remain stable. Appearance updates accept either selection independently;
clients cannot supply a target user. Reading activity exposes aggregate counts,
without revealing the user's private reading history or group membership list.
Activity also returns `tipStoryId`: a visible, non-deleted story belonging to the
profile owner with at least one non-deleted published chapter, or null. The profile
uses this to show the same ARS 1,000 cafecito checkout as story details, without
introducing an author role. The button is hidden on the sender's own profile.
The existing `/api/tips` contract still determines the receiver from the story.
Checkout tests mock the provider; no real payments are made by the test suite.
Favorites are independent of library/reading status, and hidden/deleted stories
are filtered before pagination. The existing profile page is shared by personal
and visiting views; only the owner has appearance and favorite-removal controls.

New tests cover real repository search/pagination, profile privacy, authenticated
access, target validation, email compatibility, and group-membership restrictions.
The existing develop test suite has unrelated compilation failures in older group
and review tests; validate the new tests in isolation until those contracts are
updated. The application itself compiles with Java 24.
