$payload = @{
  vulnerabilities = @(
    @{ id = 'CVE-2025-0001'; severity = 'critical'; description='Example' },
    @{ id = 'CVE-2025-0002'; severity = 'medium'; description='Example2' }
  )
} | ConvertTo-Json

Invoke-RestMethod -Method Post -Uri http://localhost:4567/ingest/nuclei -Body $payload -ContentType 'application/json'
