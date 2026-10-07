# API reference

Auth: HTTP Basic (`Authorization: Basic base64(mobile:password)`), stateless, BCrypt-verified. Errors: `{"message": "..."}`.
Dates `YYYY-MM-DD`, times `HH:mm`. Quantity unit: bags.

| Method & path | Role | Purpose |
|---|---|---|
| POST /api/auth/register | public | Register farmer (name, mobile, password, address, village, district, state, aadhaar, farmerId, preferredCentreId, landAcres) |
| POST /api/auth/login | any (Basic) | Returns id, name, role, centreId |
| GET /api/centres?q= | public | Centres with `availableToday`, `queue`, status |
| GET /api/centres/{id} | public | One centre |
| GET /api/slots?centreId=&date= | public | Slots with `available` and `state` (AVAILABLE/LIMITED/FULL/CLOSED) |
| GET /api/slots/available?centreId=&date= | public | Only bookable slots |
| GET /api/slots/{id} | public | One slot |
| POST /api/bookings | farmer | Body: slotId, vehicleNumber, vehicleType, paddyType, quantity, contactNumber. Returns bookingId + token |
| GET /api/bookings/my | farmer | Own bookings |
| GET /api/bookings/{bookingId} | owner / staff of centre / admin | One booking |
| PUT /api/bookings/{bookingId}/cancel | owner (CONFIRMED, >2h before slot) / staff / admin | Cancel and free the seat |
| GET /api/farmers/me, PUT /api/farmers/me | farmer | Profile (+ approved limit, used quantity) |
| GET/POST /api/farmers/me/quantity-requests | farmer | Extra-quantity requests (body: quantity, reason) |
| GET /api/notifications, /unread-count; PUT /read-all | any | Notifications |
| GET /api/staff/slots?centreId= | staff/admin | Upcoming slots (staff: own centre only) |
| POST /api/staff/slots | staff/admin | centreId (admin), date, start, end, capacity, status |
| PUT /api/staff/slots/{id} | staff/admin | capacity (not below booked), status |
| DELETE /api/staff/slots/{id} | staff/admin | Only when no bookings |
| GET /api/staff/bookings?date=&centreId= | staff/admin | Queue for a day |
| PUT /api/staff/bookings/{bookingId}/status | staff/admin | Body `{status}`; flow CONFIRMED→ARRIVED→(WAITING)→UNLOADING→COMPLETED |
| GET /api/staff/stats?date=&centreId= | staff/admin | Dashboard numbers |
| GET /api/admin/dashboard | admin | Totals + chart data |
| GET /api/admin/farmers | admin | Farmers with limits |
| PUT /api/admin/farmers/{id}/max-quantity | admin | Body `{value}` – approved max bags |
| GET /api/admin/quantity-requests; PUT /{id} | admin | Body `{approve:true|false}` – approval adds requested bags |
| PUT /api/admin/users/{userId}/status | admin | ACTIVE / INACTIVE |
| GET/POST /api/admin/centres, PUT/DELETE /{id} | admin | Centre CRUD (DELETE = deactivate) |
| GET/POST /api/admin/staff | admin | Staff accounts bound to a centre |
| GET /api/admin/bookings?date=&status=&centreId= | admin | All bookings |
| GET /api/admin/reports?type=&from=&to= | admin | type = daily, centre, completed, utilization, farmer → `{columns, rows}` |

## How double booking is prevented
`CoreService.book` runs in one transaction: lock centre row → lock and re-read slot → validate (centre open, slot open, not started, not full, no duplicate, quantity within approved limit) → atomic `UPDATE slots SET booked_count=booked_count+1 WHERE id=? AND booked_count<capacity AND status='OPEN'` (must affect 1 row) → insert booking. Unique keys on `booking_id` and `(centre, date, token)` plus a `CHECK (booked_count <= capacity)` in `schema.sql` are extra safety nets.
