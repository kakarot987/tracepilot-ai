
param(
    [string]$BaseUrl = "http://localhost:8080",
    [string]$Output = "milestone-4-comparison.csv"
)

$incidentIds = @(
    "INC-DB-001",
    "INC-PAY-001",
    "INC-RATE-001",
    "INC-MEM-001"
)

$results = @()

foreach ($incidentId in $incidentIds) {
    foreach ($mode in @("single", "multi")) {

        $path = if ($mode -eq "single") {
            "/api/v1/incidents/$incidentId/investigations"
        }
        else {
            "/api/v1/incidents/$incidentId/workflows"
        }

        Write-Host "Running $mode for $incidentId"

        $watch = [System.Diagnostics.Stopwatch]::StartNew()
        $response = $null
        $httpStatus = 200
        $errorMessage = ""

        try {
            $response = Invoke-RestMethod `
                -Method Post `
                -Uri "$BaseUrl$path" `
                -TimeoutSec 360 `
                -ErrorAction Stop
        }
        catch {
            $httpStatus = if ($_.Exception.Response) {
                [int]$_.Exception.Response.StatusCode
            }
            else {
                0
            }

            $errorMessage = $_.Exception.Message
        }
        finally {
            $watch.Stop()
        }

        $runId = if ($null -ne $response) {
            if ($mode -eq "single") {
                $response.runId
            }
            else {
                $response.workflowId
            }
        }
        else {
            $null
        }

        $results += [pscustomobject]@{
            incidentId      = $incidentId
            mode            = $mode
            runId           = $runId
            status          = if ($response) {
                $response.status
            } else {
                "HTTP_ERROR"
            }
            httpStatus      = $httpStatus
            latencyMs       = $watch.ElapsedMilliseconds
            followUpsUsed   = if ($mode -eq "multi" -and $response) {
                $response.followUpsUsed
            } else {
                $null
            }
            error           = $errorMessage
        }
    }
}

$results | Export-Csv `
    -Path $Output `
    -NoTypeInformation `
    -Encoding UTF8

$results | Format-Table -AutoSize

Write-Host "Results saved to $Output"
