package com.saving.contract.service;

import com.saving.contract.repository.SavingContractRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

/**
 * Thread-safe contract number generator.
 *
 * Format: SC-{YEAR}-{6-digit-zero-padded-sequence}
 * Example: SC-2025-000001, SC-2025-000002, …
 *
 * The sequence resets to 1 each calendar year.
 */
@Service
@RequiredArgsConstructor
public class ContractNumberGenerator {

    private final SavingContractRepository contractRepository;

    /**
     * Generate the next unique contract number for the current year.
     * Synchronized to prevent duplicate numbers under concurrent requests.
     */
    public synchronized String generate() {
        int year   = LocalDate.now().getYear();
        String prefix = "SC-" + year + "-";

        Integer maxSeq = contractRepository.findMaxSequenceForYear(prefix);
        int nextSeq = (maxSeq == null ? 0 : maxSeq) + 1;

        return String.format("%s%06d", prefix, nextSeq);
    }
}
