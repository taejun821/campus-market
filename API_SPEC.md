# Campus Market API 명세서

> Base URL: `http://localhost:8081`
> 모든 응답은 `{ "success": true/false, "data": {...}, "message": null/"오류메시지" }` 형태로 반환됩니다.

---

## 공통

### 인증 방식
JWT Bearer 토큰을 사용합니다.
```
Authorization: Bearer {token}
```
로그인/회원가입 API를 제외한 모든 API에 필요합니다.

### 페이지네이션 (커서 기반)
목록 조회 API는 커서 기반 페이지네이션을 사용합니다.

```json
{
  "success": true,
  "data": {
    "items": [...],
    "nextCursor": 1716000000000,
    "hasNext": true
  }
}
```

| 필드 | 설명 |
|------|------|
| items | 현재 페이지 항목 목록 |
| nextCursor | 다음 페이지 요청 시 `cursor` 파라미터에 전달할 값 (다음 페이지 없으면 null) |
| hasNext | 다음 페이지 존재 여부 |

### 공통 에러 응답
| HTTP | 설명 |
|------|------|
| 400 | 요청 값 오류 (BadRequest) |
| 401 | 인증 토큰 없음 또는 만료 |
| 403 | 권한 없음 (타인의 게시물 수정/삭제 시도 등) |
| 404 | 리소스 없음 |
| 409 | 중복 (이미 가입된 이메일 등) |
| 500 | 서버 오류 |

---

## 1. 인증 (Auth)

### 1-1. 회원가입
`POST /api/auth/register`

> 인증 토큰 불필요

**Request Body**
```json
{
  "university": "홍익대학교",
  "region": "서울",
  "email": "example@hongik.ac.kr",
  "name": "홍길동",
  "password": "password123"
}
```

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| university | String | ✅ | 대학교명 |
| region | String | ✅ | 지역 (예: 서울, 경기, 인천, 부산 등) |
| email | String | ✅ | 학교 이메일 (허용 도메인만 가능) |
| name | String | ✅ | 사용자 이름 |
| password | String | ✅ | 비밀번호 (최소 8자) |

**Response** `201 Created`
```json
{
  "success": true,
  "data": {
    "uid": "uuid-string",
    "email": "example@hongik.ac.kr",
    "name": "홍길동",
    "university": "홍익대학교",
    "region": "서울",
    "createdAt": null,
    "token": "eyJhbGci..."
  },
  "message": null
}
```

---

### 1-2. 로그인
`POST /api/auth/login`

> 인증 토큰 불필요

**Request Body**
```json
{
  "email": "example@hongik.ac.kr",
  "password": "password123"
}
```

**Response** `200 OK`
```json
{
  "success": true,
  "data": {
    "uid": "uuid-string",
    "email": "example@hongik.ac.kr",
    "name": "홍길동",
    "university": "홍익대학교",
    "region": "서울",
    "createdAt": 1716000000000,
    "token": "eyJhbGci..."
  },
  "message": null
}
```

---

### 1-3. 비밀번호 변경
`PUT /api/auth/password`

> `Authorization` 헤더 필요

**Request Body**
```json
{
  "currentPassword": "password123",
  "newPassword": "newpassword456"
}
```

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| currentPassword | String | ✅ | 현재 비밀번호 (본인 확인용) |
| newPassword | String | ✅ | 새 비밀번호 (최소 8자, 현재 비밀번호와 달라야 함) |

**Response** `200 OK`
```json
{ "success": true, "data": null, "message": null }
```

---

### 1-4. 회원 탈퇴
`DELETE /api/auth/me`

> `Authorization` 헤더 필요
> 탈퇴 시 해당 유저의 중고거래·분실물 게시물도 함께 삭제됩니다.

**Request Body**
```json
{
  "password": "password123"
}
```

**Response** `200 OK`
```json
{ "success": true, "data": null, "message": null }
```

---

## 2. 프로필 (Profile)

> 모든 요청에 `Authorization` 헤더 필요

### 2-1. 내 프로필 조회
`GET /api/profile`

**Response** `200 OK`
```json
{
  "success": true,
  "data": {
    "uid": "uuid-string",
    "email": "example@hongik.ac.kr",
    "name": "홍길동",
    "university": "홍익대학교",
    "region": "서울",
    "createdAt": 1716000000000
  },
  "message": null
}
```

---

### 2-2. 내 프로필 수정
`PUT /api/profile`

> null인 필드는 수정하지 않음 (부분 수정 허용)

**Request Body** (변경할 필드만 포함)
```json
{
  "name": "새이름",
  "university": "새대학교",
  "region": "경기"
}
```

| 필드 | 타입 | 필수 | 제한 |
|------|------|------|------|
| name | String | ❌ | 최대 50자 |
| university | String | ❌ | 최대 100자 |
| region | String | ❌ | 최대 30자 |

**Response** `200 OK` — 수정된 프로필 반환

---

### 2-3. 타인 공개 프로필 조회
`GET /api/profile/{uid}`

**Response** `200 OK` — email 필드는 null (개인정보 보호)

---

## 3. 이미지 업로드 (Image)

> `Authorization` 헤더 필요

### 3-1. 이미지 업로드
`POST /api/images/upload`

**Content-Type:** `multipart/form-data`

| 파트 | 설명 |
|------|------|
| file | 이미지 파일 (jpeg / png / webp / gif, 최대 10MB) |

**Response** `200 OK`
```json
{
  "success": true,
  "data": {
    "url": "https://storage.googleapis.com/bucket/images/uuid.jpg"
  },
  "message": null
}
```

> 반환된 `url`을 게시물의 `imageUrls` 배열에 담아 전송하세요.

---

## 4. 중고거래 (Trade)

> 모든 요청에 `Authorization` 헤더 필요
> 목록 조회/검색은 로그인 유저와 **동일 지역** 게시물만 반환됩니다.

### 카테고리 목록
`전자기기` / `의류` / `도서` / `생활용품` / `식품` / `스포츠` / `기타`

---

### 4-1. 게시물 등록
`POST /api/trade`

**Request Body**
```json
{
  "title": "아이패드 팝니다",
  "description": "거의 새것, 사용 3개월",
  "price": 500000,
  "location": "신촌역 2번 출구",
  "category": "전자기기",
  "imageUrls": ["https://storage.googleapis.com/..."]
}
```

| 필드 | 타입 | 필수 | 제한 |
|------|------|------|------|
| title | String | ✅ | 최대 100자 |
| description | String | ✅ | 최대 2000자 |
| price | Long | ✅ | 0 이상 (무료 나눔 시 0) |
| location | String | ✅ | 최대 200자 (거래 희망 장소) |
| category | String | ✅ | 카테고리 목록 중 하나 |
| imageUrls | String[] | ❌ | 최대 10개 |

**Response** `201 Created`
```json
{
  "success": true,
  "data": {
    "id": "firestore-doc-id",
    "title": "아이패드 팝니다",
    "description": "거의 새것, 사용 3개월",
    "price": 500000,
    "location": "신촌역 2번 출구",
    "region": "서울",
    "category": "전자기기",
    "imageUrls": ["https://..."],
    "status": "SELLING",
    "userId": "uuid-string",
    "userName": "홍길동",
    "viewCount": 0,
    "likeCount": 0,
    "createdAt": null
  },
  "message": null
}
```

> `region`, `userName`은 서버가 로그인 유저 정보에서 자동으로 설정합니다.

---

### 4-2. 게시물 목록 조회 (지역 필터 + 페이지네이션)
`GET /api/trade`

**Query Parameters**

| 파라미터 | 타입 | 필수 | 설명 |
|---------|------|------|------|
| category | String | ❌ | 카테고리 필터 (예: `전자기기`) |
| cursor | Long | ❌ | 이전 응답의 `nextCursor` 값 (첫 페이지는 생략) |
| size | int | ❌ | 페이지 크기 (기본값: 20) |

**Response** `200 OK`
```json
{
  "success": true,
  "data": {
    "items": [...],
    "nextCursor": 1716000000000,
    "hasNext": true
  },
  "message": null
}
```

---

### 4-3. 게시물 검색
`GET /api/trade/search`

**Query Parameters**

| 파라미터 | 타입 | 필수 | 설명 |
|---------|------|------|------|
| keyword | String | ✅ | 제목 검색 키워드 |
| category | String | ❌ | 카테고리 필터 |

**Response** `200 OK` — 동일 지역 내 키워드 포함 게시물 목록

---

### 4-4. 내가 등록한 게시물 목록
`GET /api/trade/my`

**Response** `200 OK` — 내가 올린 모든 게시물 목록 (최신순)

---

### 4-5. 내가 좋아요한 게시물 목록
`GET /api/trade/liked`

**Response** `200 OK` — 내가 좋아요한 중고거래 게시물 목록

---

### 4-6. 게시물 단건 조회
`GET /api/trade/{id}`

> 조회할 때마다 `viewCount` 1 증가

**Response** `200 OK` — 게시물 상세 정보

---

### 4-7. 게시물 수정
`PUT /api/trade/{id}`

> 등록자 본인만 가능. 보내지 않은 필드(null)는 유지됩니다.

**Request Body** (변경할 필드만 포함)
```json
{
  "title": "수정된 제목",
  "price": 450000,
  "category": "기타"
}
```

| 필드 | 타입 | 필수 | 제한 |
|------|------|------|------|
| title | String | ❌ | 최대 100자 |
| description | String | ❌ | 최대 2000자 |
| price | Long | ❌ | 0 이상 |
| location | String | ❌ | 최대 200자 |
| category | String | ❌ | 카테고리 목록 중 하나 |
| imageUrls | String[] | ❌ | 최대 10개 |

**Response** `200 OK` — 수정된 게시물 반환

---

### 4-8. 게시물 삭제
`DELETE /api/trade/{id}`

> 등록자 본인만 가능

**Response** `200 OK`
```json
{ "success": true, "data": null, "message": null }
```

---

### 4-9. 거래 상태 변경
`PATCH /api/trade/{id}/status`

> 등록자 본인만 가능

**Request Body**
```json
{ "status": "RESERVED" }
```

| 값 | 설명 |
|----|------|
| SELLING | 판매중 |
| RESERVED | 예약중 |
| SOLD | 판매완료 |

**Response** `200 OK` — 변경된 게시물 반환

---

### 4-10. 좋아요 토글
`POST /api/trade/{id}/like`

> 이미 좋아요 → 취소 / 없으면 → 추가

**Response** `200 OK`
```json
{
  "success": true,
  "data": {
    "liked": true,
    "likeCount": 5
  },
  "message": null
}
```

---

## 5. 분실물 (Lost Item)

> 모든 요청에 `Authorization` 헤더 필요
> 목록 조회/검색은 로그인 유저와 **동일 지역** 게시물만 반환됩니다.

### 5-1. 분실물 등록
`POST /api/lost-items`

**Request Body**
```json
{
  "title": "에어팟 프로 분실",
  "description": "케이스 포함, 좌측 이어폰 스티커 있음",
  "location": "중앙도서관 3층",
  "lostDate": "2024-05-10",
  "imageUrls": ["https://storage.googleapis.com/..."]
}
```

| 필드 | 타입 | 필수 | 제한 |
|------|------|------|------|
| title | String | ✅ | 최대 100자 |
| description | String | ✅ | 최대 2000자 |
| location | String | ✅ | 최대 200자 |
| lostDate | String | ✅ | 분실 날짜 |
| imageUrls | String[] | ❌ | 최대 10개 |

**Response** `201 Created`
```json
{
  "success": true,
  "data": {
    "id": "firestore-doc-id",
    "title": "에어팟 프로 분실",
    "description": "케이스 포함, 좌측 이어폰 스티커 있음",
    "location": "중앙도서관 3층",
    "lostDate": "2024-05-10",
    "region": "서울",
    "imageUrls": ["https://..."],
    "status": "LOST",
    "userId": "uuid-string",
    "userName": "홍길동",
    "createdAt": null,
    "viewCount": 0,
    "likeCount": 0
  },
  "message": null
}
```

---

### 5-2. 분실물 목록 조회 (지역 필터 + 페이지네이션)
`GET /api/lost-items`

**Query Parameters**

| 파라미터 | 타입 | 필수 | 설명 |
|---------|------|------|------|
| cursor | Long | ❌ | 이전 응답의 `nextCursor` 값 (첫 페이지는 생략) |
| size | int | ❌ | 페이지 크기 (기본값: 20) |

**Response** `200 OK`
```json
{
  "success": true,
  "data": {
    "items": [...],
    "nextCursor": 1716000000000,
    "hasNext": true
  },
  "message": null
}
```

---

### 5-3. 분실물 검색
`GET /api/lost-items/search`

**Query Parameters**

| 파라미터 | 타입 | 필수 | 설명 |
|---------|------|------|------|
| keyword | String | ✅ | 제목 검색 키워드 |

**Response** `200 OK` — 동일 지역 내 키워드 포함 분실물 목록

---

### 5-4. 내가 등록한 분실물 목록
`GET /api/lost-items/my`

**Response** `200 OK` — 내가 올린 모든 분실물 목록 (최신순)

---

### 5-5. 내가 좋아요한 분실물 목록
`GET /api/lost-items/liked`

**Response** `200 OK` — 내가 좋아요한 분실물 목록

---

### 5-6. 분실물 단건 조회
`GET /api/lost-items/{id}`

> 조회할 때마다 `viewCount` 1 증가

**Response** `200 OK` — 분실물 상세 정보

---

### 5-7. 분실물 수정
`PUT /api/lost-items/{id}`

> 등록자 본인만 가능. 보내지 않은 필드(null)는 유지됩니다.

**Request Body** (변경할 필드만 포함)
```json
{
  "title": "수정된 제목",
  "location": "새로운 장소"
}
```

| 필드 | 타입 | 필수 | 제한 |
|------|------|------|------|
| title | String | ❌ | 최대 100자 |
| description | String | ❌ | 최대 2000자 |
| location | String | ❌ | 최대 200자 |
| lostDate | String | ❌ | |
| imageUrls | String[] | ❌ | 최대 10개 |

**Response** `200 OK` — 수정된 분실물 반환

---

### 5-8. 분실물 상태 변경
`PATCH /api/lost-items/{id}/status`

> 등록자 본인만 가능

**Request Body**
```json
{ "status": "FOUND" }
```

| 값 | 설명 |
|----|------|
| LOST | 분실중 |
| FOUND | 찾았음 |

**Response** `200 OK` — 변경된 분실물 반환

---

### 5-9. 분실물 삭제
`DELETE /api/lost-items/{id}`

> 등록자 본인만 가능

**Response** `200 OK`
```json
{ "success": true, "data": null, "message": null }
```

---

### 5-10. 좋아요 토글
`POST /api/lost-items/{id}/like`

**Response** `200 OK`
```json
{
  "success": true,
  "data": {
    "liked": false,
    "likeCount": 3
  },
  "message": null
}
```

---

## 6. 채팅 (Chat)

> 모든 요청에 `Authorization` 헤더 필요

### 6-1. 채팅방 조회 또는 생성
`POST /api/chat/rooms`

> 동일한 두 사용자 + 아이템 조합의 채팅방이 있으면 기존 방을 반환하고, 없으면 새로 생성합니다.

**Request Body**
```json
{
  "targetUserId": "상대방-uid",
  "itemId": "게시물-id",
  "itemType": "trade"
}
```

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| targetUserId | String | ✅ | 채팅 상대방 uid |
| itemId | String | ✅ | 게시물 ID |
| itemType | String | ✅ | `lost` 또는 `trade` |

**Response** `200 OK`
```json
{
  "success": true,
  "data": {
    "id": "uid1_uid2_itemId",
    "participants": ["uid1", "uid2"],
    "itemId": "게시물-id",
    "itemType": "trade",
    "lastMessage": null,
    "lastMessageAt": null,
    "createdAt": 1716000000000,
    "unreadCount": 0
  },
  "message": null
}
```

---

### 6-2. 내 채팅방 목록 조회
`GET /api/chat/rooms`

> `unreadCount`는 요청한 유저 기준 읽지 않은 메시지 수입니다.

**Response** `200 OK` — 내가 참여한 채팅방 목록 (최신순), 각 방에 `unreadCount` 포함

---

### 6-3. 채팅방 메시지 목록 조회
`GET /api/chat/rooms/{roomId}/messages`

> 참여자만 조회 가능. 커서 기반 페이지네이션 (오래된 메시지 추가 로드 지원)

**Query Parameters**

| 파라미터 | 타입 | 필수 | 설명 |
|---------|------|------|------|
| cursor | Long | ❌ | 이전 응답의 `nextCursor` 값 (없으면 최신 메시지부터) |
| size | int | ❌ | 페이지 크기 (기본값: 30) |

> cursor 없으면 최신 메시지 `size`개 반환, cursor 있으면 그보다 오래된 메시지 반환 (위로 스크롤 시 이전 내용 로드)
> 반환 순서는 항상 오래된 순 (화면 표시용)

**Response** `200 OK`
```json
{
  "success": true,
  "data": {
    "items": [
      {
        "id": "message-doc-id",
        "roomId": "uid1_uid2_itemId",
        "senderId": "uid1",
        "content": "안녕하세요, 아직 판매 중인가요?",
        "createdAt": 1716000000000
      }
    ],
    "nextCursor": 1716000000000,
    "hasNext": true
  },
  "message": null
}
```

---

### 6-4. 메시지 전송
`POST /api/chat/rooms/{roomId}/messages`

> 참여자만 전송 가능. 전송 후 채팅방 `lastMessage` 자동 업데이트, 상대방 `unreadCount` +1

**Request Body**
```json
{
  "content": "안녕하세요, 아직 판매 중인가요?"
}
```

| 필드 | 타입 | 필수 | 제한 |
|------|------|------|------|
| content | String | ✅ | 최대 1000자 |

**Response** `201 Created`
```json
{
  "success": true,
  "data": {
    "id": "message-doc-id",
    "roomId": "uid1_uid2_itemId",
    "senderId": "my-uid",
    "content": "안녕하세요, 아직 판매 중인가요?",
    "createdAt": 1716000000000
  },
  "message": null
}
```

---

### 6-5. 읽음 처리
`POST /api/chat/rooms/{roomId}/read`

> 채팅방을 열 때 호출. 내 `unreadCount`를 0으로 리셋합니다.
> 참여자만 가능.

**Response** `200 OK`
```json
{ "success": true, "data": null, "message": null }
```

---

## 7. 관리 (Admin)

> `Authorization` 헤더 필요

### 7-1. userName 마이그레이션
`POST /api/admin/migrate/user-name`

> 기존 게시물 중 `userName`이 없는 문서에 users 컬렉션에서 이름을 조회해 채워 넣습니다. 일회성 작업.

**Response** `200 OK`
```json
{
  "success": true,
  "data": {
    "trade_items": 5,
    "lost_items": 3
  },
  "message": null
}
```

> 숫자는 실제로 업데이트된 문서 수입니다.

---

## 주요 흐름 요약

### 게시물 이미지 포함 등록 흐름
1. `POST /api/images/upload` → 이미지 URL 획득
2. `POST /api/trade` 또는 `POST /api/lost-items` — `imageUrls` 배열에 URL 담아 전송

### 페이지네이션 사용 흐름
1. `GET /api/trade` → `data.items` 렌더링, `data.nextCursor` 저장
2. 스크롤 끝 도달 시 `GET /api/trade?cursor={nextCursor}` 재요청
3. `data.hasNext == false` 이면 더 이상 요청하지 않음

### 채팅 시작 흐름
1. 게시물 목록/단건 조회에서 `userId` 확인
2. `POST /api/chat/rooms` — `targetUserId`, `itemId`, `itemType` 전송
3. 반환된 `id`(roomId)로 `GET /api/chat/rooms/{roomId}/messages` 메시지 조회
4. `POST /api/chat/rooms/{roomId}/read` — 읽음 처리 (채팅방 진입 시 호출)
5. `POST /api/chat/rooms/{roomId}/messages` — 메시지 전송

### 읽음 처리 흐름
- 채팅방 목록(`GET /api/chat/rooms`)에서 `unreadCount > 0`인 방에 뱃지 표시
- 채팅방 진입 시 `POST /api/chat/rooms/{roomId}/read` 호출 → `unreadCount` 0으로 리셋

---

## Firestore 복합 인덱스 설정 필요 항목

> Firebase 콘솔 → Firestore → 인덱스 탭에서 생성해야 합니다.

| 컬렉션 | 필드 | 용도 |
|--------|------|------|
| trade_items | region ASC, createdAt DESC | 목록 조회 |
| trade_items | region ASC, category ASC, createdAt DESC | 카테고리 필터 목록 조회 |
| lost_items | region ASC, createdAt DESC | 목록 조회 |
| likes | type ASC, userId ASC | 좋아요한 목록 조회 |
