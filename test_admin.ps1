$baseUrl = "http://localhost:8081"
$cookieJar = New-Object System.Net.CookieContainer

$handler = New-Object System.Net.Http.HttpClientHandler
$handler.CookieContainer = $cookieJar
$handler.UseCookies = $true
$handler.AllowAutoRedirect = $false

$client = New-Object System.Net.Http.HttpClient($handler)

# Step 1: Login
$loginBody = "email=admin2@gmail.com&password=admin123"
$content = New-Object System.Net.Http.StringContent($loginBody, [System.Text.Encoding]::UTF8, "application/x-www-form-urlencoded")
$loginResp = $client.PostAsync("$baseUrl/auth/login", $content).Result
Write-Host "Login Status: $($loginResp.StatusCode)"
Write-Host "Location: $($loginResp.Headers.Location)"
Write-Host "Set-Cookie: $($loginResp.Headers.GetValues('Set-Cookie'))"

# Step 2: Access dashboard (follow redirect)
$dashResp = $client.GetAsync("$baseUrl/admin/dashboard").Result
Write-Host ""
Write-Host "=== DASHBOARD ==="
Write-Host "Dashboard Status: $($dashResp.StatusCode)"
$dashContent = $dashResp.Content.ReadAsStringAsync().Result
Write-Host "Dashboard length: $($dashContent.Length) chars"
Write-Host "Contains 'CNJ70 Admin': $($dashContent.Contains('CNJ70 Admin'))"
Write-Host "Contains 'sd-sidebar': $($dashContent.Contains('sd-sidebar'))"
Write-Host "Contains 'sd-topbar': $($dashContent.Contains('sd-topbar'))"
Write-Host "Contains 'adminRevenueChart': $($dashContent.Contains('adminRevenueChart'))"
Write-Host "Contains 'th:inline': $($dashContent.Contains('th:inline'))"
Write-Host "Contains 'sd-flash': $($dashContent.Contains('sd-flash'))"
Write-Host "Contains 'data-match': $($dashContent.Contains('data-match'))"

# Step 3: User list
$userResp = $client.GetAsync("$baseUrl/admin/users").Result
Write-Host ""
Write-Host "=== USER LIST ==="
Write-Host "User list Status: $($userResp.StatusCode)"
$userContent = $userResp.Content.ReadAsStringAsync().Result
Write-Host "Contains 'Quản lý người dùng': $($userContent.Contains('Quản lý người dùng'))"
Write-Host "Contains 'sd-table': $($userContent.Contains('sd-table'))"

# Step 4: Category list
$catResp = $client.GetAsync("$baseUrl/admin/categories").Result
Write-Host ""
Write-Host "=== CATEGORY ==="
Write-Host "Category Status: $($catResp.StatusCode)"
$catContent = $catResp.Content.ReadAsStringAsync().Result
Write-Host "Contains 'Quản lý danh mục': $($catContent.Contains('Quản lý danh mục'))"

# Step 5: /admin/shops - should be 404 (no controller)
$shopsResp = $client.GetAsync("$baseUrl/admin/shops").Result
Write-Host ""
Write-Host "=== /admin/shops (no controller) ==="
Write-Host "Shops Status: $($shopsResp.StatusCode)"

# Step 6: /admin/orders - should be 404
$ordersResp = $client.GetAsync("$baseUrl/admin/orders").Result
Write-Host ""
Write-Host "=== /admin/orders (no controller) ==="
Write-Host "Orders Status: $($ordersResp.StatusCode)"

# Step 7: Logout
$logoutContent = New-Object System.Net.Http.StringContent("", [System.Text.Encoding]::UTF8)
$logoutResp = $client.PostAsync("$baseUrl/auth/logout", $logoutContent).Result
Write-Host ""
Write-Host "=== LOGOUT ==="
Write-Host "Logout Status: $($logoutResp.StatusCode)"
Write-Host "Location: $($logoutResp.Headers.Location)"