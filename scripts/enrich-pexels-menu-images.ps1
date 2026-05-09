$ErrorActionPreference = "Stop"

$repoRoot = Split-Path -Parent $PSScriptRoot
Set-Location $repoRoot

if (Test-Path ".env") {
    Get-Content ".env" | ForEach-Object {
        if ($_ -and -not $_.StartsWith("#")) {
            $parts = $_.Split("=", 2)
            if ($parts.Length -eq 2) {
                [Environment]::SetEnvironmentVariable($parts[0], $parts[1], "Process")
            }
        }
    }
}

if (-not $env:PEXELS_API_KEY) {
    throw "PEXELS_API_KEY is missing. Add it to .env before running this one-time enrichment."
}

$rows = docker exec platemate-postgres psql -U platemate -d platemate -At -F "`t" -c @"
select r.name, r.category, mi.name
from menu_item mi
join restaurant r on r.id = mi.restaurant_id
order by r.name, mi.sort_order, mi.name;
"@

$usedUrls = [System.Collections.Generic.HashSet[string]]::new()
$results = @()

function Get-Queries([string] $restaurant, [string] $category, [string] $item) {
    $itemLower = $item.ToLowerInvariant()
    $categoryLower = $category.ToLowerInvariant()

    if ($itemLower.Contains("schnitzel")) { return @("wiener schnitzel", "austrian schnitzel") }
    if ($itemLower.Contains("spätzle")) { return @("cheese spaetzle", "mac and cheese") }
    if ($itemLower.Contains("gröstl")) { return @("fried potatoes egg", "breakfast skillet") }
    if ($itemLower.Contains("apfelstrudel")) { return @("apple strudel", "apple pie dessert") }
    if ($itemLower.Contains("mixed salad")) { return @("fresh salad bowl", "green salad") }
    if ($itemLower.Contains("pizza margherita")) { return @("margherita pizza", "pizza basil") }
    if ($itemLower.Contains("pizza diavola")) { return @("pepperoni pizza", "spicy pizza") }
    if ($itemLower.Contains("penne")) { return @("penne arrabbiata", "tomato pasta") }
    if ($itemLower.Contains("caprese")) { return @("caprese salad", "tomato mozzarella") }
    if ($itemLower.Contains("tiramisu")) { return @("tiramisu", "italian dessert") }
    if ($itemLower.Contains("butter chicken")) { return @("butter chicken curry", "indian curry") }
    if ($itemLower.Contains("dal")) { return @("dal tadka", "lentil curry") }
    if ($itemLower.Contains("momos")) { return @("momo dumplings", "steamed dumplings") }
    if ($itemLower.Contains("naan")) { return @("garlic naan", "naan bread") }
    if ($itemLower.Contains("lassi")) { return @("mango lassi", "mango smoothie") }
    if ($itemLower.Contains("salmon maki")) { return @("salmon maki sushi", "salmon sushi") }
    if ($itemLower.Contains("veggie sushi")) { return @("vegetarian sushi", "sushi rolls") }
    if ($itemLower.Contains("teriyaki")) { return @("teriyaki chicken bowl", "chicken rice bowl") }
    if ($itemLower.Contains("edamame")) { return @("edamame", "soy beans") }
    if ($itemLower.Contains("miso")) { return @("miso soup", "japanese soup") }
    if ($itemLower.Contains("chicken tacos")) { return @("chicken tacos", "mexican tacos") }
    if ($itemLower.Contains("veggie tacos")) { return @("vegetarian tacos", "tacos") }
    if ($itemLower.Contains("burrito")) { return @("burrito bowl", "mexican rice bowl") }
    if ($itemLower.Contains("nachos")) { return @("nachos guacamole", "nachos") }
    if ($itemLower.Contains("churros")) { return @("churros", "fried dessert") }
    if ($itemLower.Contains("souvlaki")) { return @("chicken souvlaki", "grilled skewers") }
    if ($itemLower.Contains("falafel")) { return @("falafel wrap", "falafel pita") }
    if ($itemLower.Contains("hummus")) { return @("hummus plate", "hummus pita") }
    if ($itemLower.Contains("baklava")) { return @("baklava", "middle eastern dessert") }
    if ($itemLower.Contains("köfte") -or $itemLower.Contains("kofte")) { return @("kofte plate", "grilled meatballs") }
    if ($itemLower.Contains("green gaia")) { return @("vegan bowl", "quinoa bowl") }
    if ($itemLower.Contains("miso mushroom")) { return @("mushroom rice bowl", "miso mushrooms") }
    if ($itemLower.Contains("cauliflower")) { return @("crispy cauliflower", "roasted cauliflower") }
    if ($itemLower.Contains("chia")) { return @("chocolate chia pudding", "chia dessert") }
    if ($itemLower.Contains("lemonade")) { return @("ginger lemonade", "homemade lemonade") }
    if ($itemLower.Contains("adobo")) { return @("chicken adobo", "filipino chicken") }
    if ($itemLower.Contains("tocino")) { return @("pork rice bowl", "pork belly rice") }
    if ($itemLower.Contains("pancit")) { return @("pancit noodles", "stir fried noodles") }
    if ($itemLower.Contains("lumpia")) { return @("spring rolls", "fried rolls") }
    if ($itemLower.Contains("halo")) { return @("halo halo dessert", "shaved ice dessert") }

    return @("$item food", "$categoryLower food")
}

function Get-PexelsImage([string[]] $queries, [string] $salt) {
    foreach ($query in $queries) {
        foreach ($page in 1..4) {
            $encoded = [uri]::EscapeDataString($query)
            $uri = "https://api.pexels.com/v1/search?query=$encoded&orientation=landscape&per_page=12&page=$page"
            $response = Invoke-RestMethod -Uri $uri -Headers @{ Authorization = $env:PEXELS_API_KEY } -TimeoutSec 15
            if (-not $response.photos -or $response.photos.Count -eq 0) {
                continue
            }

            $start = [Math]::Abs(($salt + $query + $page).GetHashCode()) % $response.photos.Count
            for ($offset = 0; $offset -lt $response.photos.Count; $offset++) {
                $photo = $response.photos[($start + $offset) % $response.photos.Count]
                $url = $photo.src.landscape
                if (-not $url) {
                    $url = $photo.src.large2x
                }
                if ($url -and -not $usedUrls.Contains($url)) {
                    [void] $usedUrls.Add($url)
                    return $url
                }
            }
        }
    }
    return "/placeholders/menu-item.svg"
}

foreach ($row in $rows) {
    if (-not $row) {
        continue
    }
    $parts = $row.Split("`t")
    $restaurant = $parts[0]
    $category = $parts[1]
    $item = $parts[2]
    $queries = Get-Queries $restaurant $category $item
    $imageUrl = Get-PexelsImage $queries "$restaurant|$item"
    $results += [pscustomobject]@{
        restaurant = $restaurant
        item = $item
        imageUrl = $imageUrl
    }
    Write-Host "Mapped $restaurant / $item"
}

$outputPath = Join-Path $repoRoot "target\pexels-menu-images.json"
New-Item -ItemType Directory -Force -Path (Split-Path -Parent $outputPath) | Out-Null
$results | ConvertTo-Json -Depth 4 | Set-Content -Path $outputPath -Encoding UTF8
Write-Host "Wrote $outputPath"
