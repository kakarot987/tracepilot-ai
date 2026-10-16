
$uri = "http://localhost:8080/api/v1/incidents/analyze"

$body = @{
    title = "Payment service timeout"
    description = "HTTP 504 errors during peak traffic. Database connections are exhausted."
    serviceName = "payment-service"
} | ConvertTo-Json

function Invoke-BenchmarkRequest {
    param([int]$Run)

    $timer = [System.Diagnostics.Stopwatch]::StartNew()

    try {
        $response = Invoke-RestMethod `
            -Uri $uri `
            -Method POST `
            -ContentType "application/json" `
            -Body $body

        $timer.Stop()

        return [PSCustomObject]@{
            Run = $Run
            Success = $true
            LatencyMs = $timer.ElapsedMilliseconds
            Hypotheses = $response.hypotheses.Count
        }
    }
    catch {
        $timer.Stop()

        return [PSCustomObject]@{
            Run = $Run
            Success = $false
            LatencyMs = $timer.ElapsedMilliseconds
            Hypotheses = 0
        }
    }
}

Write-Host "Running warm-up request..."
$null = Invoke-BenchmarkRequest -Run 0

$results = @(
1..10 | ForEach-Object {
    Invoke-BenchmarkRequest -Run $_
}
)

$results | Format-Table

$successful = @(
$results | Where-Object { $_.Success }
)

if ($successful.Count -gt 0) {
    $latencies = @(
    $successful |
            Sort-Object LatencyMs |
            Select-Object -ExpandProperty LatencyMs
    )

    $p50Index = [math]::Ceiling(0.50 * $latencies.Count) - 1
    $p95Index = [math]::Ceiling(0.95 * $latencies.Count) - 1

    Write-Host "Successful:" $successful.Count
    Write-Host "Failed:" ($results.Count - $successful.Count)
    Write-Host "P50 latency (ms):" $latencies[$p50Index]
    Write-Host "P95 latency (ms):" $latencies[$p95Index]
    Write-Host "Average latency (ms):" (
    [math]::Round(
            ($latencies | Measure-Object -Average).Average,
            2
    )
    )
}

$results | Export-Csv `
    -Path "milestone-1-benchmark.csv" `
    -NoTypeInformation
