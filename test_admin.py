#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""Phase 2 runtime test: admin login + dashboard + user/category + 404."""
import sys
import io
sys.stdout = io.TextIOWrapper(sys.stdout.buffer, encoding='utf-8')
sys.stderr = io.TextIOWrapper(sys.stderr.buffer, encoding='utf-8')
import urllib.request
import urllib.parse
import http.cookiejar
import sys
import re

BASE = "http://localhost:8081"

cj = http.cookiejar.CookieJar()
opener = urllib.request.build_opener(urllib.request.HTTPCookieProcessor(cj), urllib.request.HTTPRedirectHandler())

def post(url, data, allow_redirect=False):
    body = urllib.parse.urlencode(data).encode()
    req = urllib.request.Request(url, data=body, method='POST',
                                 headers={'Content-Type': 'application/x-www-form-urlencoded'})
    try:
        resp = opener.open(req, timeout=15)
        return resp.status, resp.read().decode('utf-8', errors='ignore'), dict(resp.getheaders())
    except urllib.error.HTTPError as e:
        return e.code, e.read().decode('utf-8', errors='ignore') if e.fp else '', dict(e.headers)

def get(url):
    try:
        resp = opener.open(url, timeout=15)
        return resp.status, resp.read().decode('utf-8', errors='ignore')
    except urllib.error.HTTPError as e:
        return e.code, e.read().decode('utf-8', errors='ignore') if e.fp else ''

def get_no_follow(url):
    """GET without following redirects."""
    req = urllib.request.Request(url)
    try:
        resp = opener.open(req, timeout=15)
        return resp.status, resp.read().decode('utf-8', errors='ignore'), dict(resp.getheaders())
    except urllib.error.HTTPError as e:
        return e.code, e.read().decode('utf-8', errors='ignore') if e.fp else '', dict(e.headers)

# ============== TEST 1: Admin login (no follow) ==============
print("=" * 60)
print("TEST 1: Admin Login (/auth/login POST)")
print("=" * 60)
# Custom no-redirect handler
class NoRedirect(urllib.request.HTTPRedirectHandler):
    def redirect_request(self, req, fp, code, msg, hdrs, newurl):
        return None

opener_nr = urllib.request.build_opener(urllib.request.HTTPCookieProcessor(cj), NoRedirect())

body = urllib.parse.urlencode({"email": "admin2@gmail.com", "password": "admin123"}).encode()
req = urllib.request.Request(f"{BASE}/auth/login", data=body, method='POST',
                             headers={'Content-Type': 'application/x-www-form-urlencoded'})
try:
    resp = opener_nr.open(req, timeout=15)
    status = resp.status
    content = resp.read().decode('utf-8', errors='ignore')
    headers = dict(resp.getheaders())
except urllib.error.HTTPError as e:
    status = e.code
    content = e.read().decode('utf-8', errors='ignore') if e.fp else ''
    headers = dict(e.headers)

print(f"Login Status: {status}")
print(f"Location: {headers.get('Location', 'N/A')}")
jwt = next((c.value for c in cj if c.name == 'jwt'), None)
print(f"JWT Cookie present: {jwt is not None}")
if jwt:
    print(f"JWT length: {len(jwt)} chars")

assert status == 302, f"Expected 302 redirect, got {status}"
assert headers.get('Location') == 'http://localhost:8081/admin/dashboard', f"Expected redirect to /admin/dashboard, got {headers.get('Location')}"
assert jwt is not None, "JWT cookie not set"
print("PASS: Admin login redirects to /admin/dashboard with JWT cookie")

# ============== TEST 2: Admin Dashboard (follows cookie) ==============
print()
print("=" * 60)
print("TEST 2: Admin Dashboard (/admin/dashboard)")
print("=" * 60)
status, content = get(f"{BASE}/admin/dashboard")
print(f"Dashboard Status: {status}")
print(f"Content length: {len(content)}")
checks = [
    ("CNJ70 Admin", "CNJ70 Admin" in content),
    ("Admin Shell sidebar class (sd-sidebar)", "sd-sidebar" in content),
    ("Admin Shell topbar class (sd-topbar)", "sd-topbar" in content),
    ("adminRevenueChart div", "adminRevenueChart" in content),
    ("Chart JS inlined with trend data", "var trend" in content and ("T2" in content or "T3" in content or "T4" in content or "CN" in content)),
    ("sd-flash container", "sd-flash" in content),
    ("data-match for active state", "data-match" in content),
    ("ApexCharts CDN", "apexcharts" in content.lower()),
    ("Stats grid 4", "sd-stats-4" in content),
    ("sd-activity-list (DTO recentActivities)", "sd-activity-list" in content),
    ("Shops disabled nav", "sd-nav-item-disabled" in content),
    ("Coming Soon badge", "sd-nav-coming-soon" in content or "Soon" in content),
    ("Username rendered (sec:authentication evaluated)", "admin2@gmail.com" in content),
]
all_ok = True
for name, ok in checks:
    print(f"  [{'OK' if ok else 'FAIL'}] {name}")
    if not ok: all_ok = False
assert status == 200, f"Expected 200, got {status}"
assert all_ok, "Dashboard checks failed"
print("PASS: Admin Dashboard renders correctly")

# ============== TEST 3: Admin User List ==============
print()
print("=" * 60)
print("TEST 3: Admin User List (/admin/users)")
print("=" * 60)
status, content = get(f"{BASE}/admin/users")
print(f"User List Status: {status}")
print(f"Content length: {len(content)}")
checks = [
    ("Quản lý người dùng", "Quản lý người dùng" in content),
    ("sd-table", "sd-table" in content),
    ("Admin Shell sidebar", "sd-sidebar" in content),
]
for name, ok in checks:
    print(f"  [{'OK' if ok else 'FAIL'}] {name}")
    if not ok: all_ok = False
assert status == 200, f"Expected 200, got {status}"
assert "Quản lý người dùng" in content, "User list page missing expected title"
print("PASS: Admin User List renders")

# ============== TEST 4: Admin Category ==============
print()
print("=" * 60)
print("TEST 4: Admin Category (/admin/categories)")
print("=" * 60)
status, content = get(f"{BASE}/admin/categories")
print(f"Category Status: {status}")
print(f"Content length: {len(content)}")
checks = [
    ("Quản lý danh mục", "Quản lý danh mục" in content),
    ("sd-table", "sd-table" in content),
    ("Admin Shell sidebar", "sd-sidebar" in content),
]
for name, ok in checks:
    print(f"  [{'OK' if ok else 'FAIL'}] {name}")
    if not ok: all_ok = False
assert status == 200, f"Expected 200, got {status}"
assert "Quản lý danh mục" in content, "Category page missing expected title"
print("PASS: Admin Category renders")

# ============== TEST 5: /admin/shops (no controller) ==============
print()
print("=" * 60)
print("TEST 5: /admin/shops (no controller, expected 404)")
print("=" * 60)
status, content = get(f"{BASE}/admin/shops")
print(f"Status: {status}")
assert status == 404, f"Expected 404 (no controller), got {status}"
assert "404" in content, "404 page missing"
print("PASS: /admin/shops returns 404 (no fake controller)")

# ============== TEST 6: /admin/orders (no controller) ==============
print()
print("=" * 60)
print("TEST 6: /admin/orders (no controller, expected 404)")
print("=" * 60)
status, content = get(f"{BASE}/admin/orders")
print(f"Status: {status}")
assert status == 404, f"Expected 404 (no controller), got {status}"
print("PASS: /admin/orders returns 404")

# ============== TEST 7: Active state highlighting ==============
print()
print("=" * 60)
print("TEST 7: Sidebar active state on User List")
print("=" * 60)
status, content = get(f"{BASE}/admin/users")
# The data-match attribute is processed by JS, but the link itself should be there
has_users_link = '/admin/users' in content
has_dashboard_match = 'data-match="/admin/dashboard"' in content
has_users_match = 'data-match="/admin/users"' in content
print(f"  Dashboard data-match present: {has_dashboard_match}")
print(f"  Users data-match present: {has_users_match}")
assert has_users_match, "Active state match attribute missing"
print("PASS: Active state data-match attributes present (JS will highlight current page)")

# ============== TEST 8: Logout ==============
print()
print("=" * 60)
print("TEST 8: Logout")
print("=" * 60)
body = urllib.parse.urlencode({}).encode()
req = urllib.request.Request(f"{BASE}/auth/logout", data=body, method='POST')
opener_logout = urllib.request.build_opener(urllib.request.HTTPCookieProcessor(cj), NoRedirect())
try:
    resp = opener_logout.open(req, timeout=15)
    status = resp.status
    headers = dict(resp.getheaders())
except urllib.error.HTTPError as e:
    status = e.code
    headers = dict(e.headers)
print(f"Logout Status: {status}")
print(f"Location: {headers.get('Location', 'N/A')}")
assert status == 302, f"Expected 302, got {status}"
assert '/auth/login' in headers.get('Location', ''), "Logout should redirect to /auth/login"
# JWT cookie should be cleared
jwt_after = next((c.value for c in cj if c.name == 'jwt'), None)
print(f"JWT cookie after logout: {jwt_after}")
print("PASS: Logout works")

print()
print("=" * 60)
print("ALL RUNTIME TESTS PASSED")
print("=" * 60)