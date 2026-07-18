$PerformanceProfiles = [ordered]@{
    low = @{ ChargeIterations=30; SpendIterations=40; IdempotentIterations=15; ConflictIterations=15; MixedIterations=60 }
    medium = @{ ChargeIterations=60; SpendIterations=80; IdempotentIterations=30; ConflictIterations=30; MixedIterations=120 }
    high = @{ ChargeIterations=240; SpendIterations=320; IdempotentIterations=120; ConflictIterations=120; MixedIterations=480 }
}
